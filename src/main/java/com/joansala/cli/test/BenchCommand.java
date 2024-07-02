package com.joansala.cli.test;

/*
 * Samurai framework.
 * Copyright (C) 2021-2024 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.StringJoiner;
import com.google.inject.Inject;
import com.google.inject.Injector;
import picocli.CommandLine.*;

import com.joansala.cli.util.EngineType;
import com.joansala.util.suites.Suite;
import com.joansala.util.suites.SuiteReader;
import com.joansala.engine.*;
import com.joansala.util.bench.*;

/**
 * Runs an engine benchmark.
 */
@Command(
  name = "bench",
  description = "Runs an engine benchmark",
  mixinStandardHelpOptions = true
)
public class BenchCommand implements Callable<Integer> {

    /** Dependency injector */
    private Injector injector;

    /** Statistics accumulator */
    private BenchStats stats;

    /** Game board instance */
    private Board parser;

    /** Engine to benchmark */
    private Engine engine;

    /** Decorated game instance */
    private BenchGame game;

    /** Decorated cache instance */
    private BenchCache cache;

    /** Decorated leaves instance */
    private BenchLeaves leaves;


    @Option(
      names = "--engine",
      description = "Custom engine (${COMPLETION-CANDIDATES})"
    )
    private EngineType engineType = null;

    @Option(
      names = "--depth",
      description = "Depth limit per move (plies)"
    )
    private int depth = Engine.DEFAULT_DEPTH;

    @Option(
      names = "--movetime",
      description = "Time limit per move (ms)"
    )
    private long moveTime = Engine.DEFAULT_MOVETIME;

    @Option(
      names = "--file",
      description = "Benchmark suite file.",
      required = true
    )
    private File file;


    /**
     * Creates a new service.
     */
    @Inject public BenchCommand(Injector injector) {
        this.injector = injector;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        stats = injector.getInstance(BenchStats.class);
        game = injector.getInstance(BenchGame.class);
        parser = injector.getInstance(Board.class);
        cache = getInstanceOrNull(injector, BenchCache.class);
        leaves = getInstanceOrNull(injector, BenchLeaves.class);
        engine = getEngineInstance(engineType);

        setupEngine();
        runBenchmark();
        return 0;
    }


    /**
     * Obtain an engine instance.
     */
    private Engine getEngineInstance(EngineType engineType) {
        return (engineType instanceof EngineType) ?
            injector.getInstance(engineType.getType()) :
            injector.getInstance(Engine.class);
    }


    /**
     * Runs the benchmark on each position of the benchmark suites.
     */
    public void runBenchmark() throws IOException {
        System.out.format("%s%n", formatSetup());
        System.out.format("Running tests%n%s%n", horizontalRule('-'));
        InputStream input = new FileInputStream(file);

        try (SuiteReader reader = new SuiteReader(input)) {
            reader.stream().forEach((suite) -> {
                String format = formatSuite(suite);
                System.out.format("%s%n", ellipsis(format, 59));

                Board board = parser.toBoard(suite.diagram());
                int[] moves = board.toMoves(suite.notation());

                game.ensureCapacity(moves.length);
                game.setBoard(board);

                engine.newMatch();
                benchmark(engine, game);

                for (int move : moves) {
                    if (game.hasEnded() == false) {
                        game.makeMove(move);
                        benchmark(engine, game);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();

            String diagram = game.toBoard().toDiagram();
            System.err.println("Benchmark exception:");
            System.err.println("\tat " + diagram);

            System.exit(1);
        }

        System.out.format("%n%s", formatStats());
    }


    /**
     * Configures the engine to be benchmarked.
     */
    private void setupEngine() {
        engine.setContempt(game.contempt());
        engine.setInfinity(game.infinity());
        engine.setMoveTime(moveTime);
        engine.setDepth(depth);

        if (engine instanceof HasCache) {
            ((HasCache) engine).setCache(cache);
        }

        if (engine instanceof HasLeaves) {
            ((HasLeaves) engine).setLeaves(leaves);
        }
    }


    /**
     * Benchmark a game state using an engine. Statistics are accumulated
     * on this object's {@code BenchStats} instance.
     *
     * @param engine    Engine instance
     * @param game      Game instance
     */
    private void benchmark(Engine engine, Game game) {
        long start = stats.watch().elapsed();

        stats.depth().offset(game.length());
        stats.moves().increment();
        stats.watch().start();
        engine.computeBestMove(game);
        stats.watch().stop();

        long elapsed = stats.watch().elapsed() - start;
        stats.movetime().aggregate(elapsed);
    }


    /**
     * Obtain an instance of the given class if it was bound.
     *
     * @param injector      Object injector
     * @param class         Object type
     * @return              An object or {@code null}
     */
    private <T> T getInstanceOrNull(Injector injector, Class<T> type) {
        T instance = null;

        try {
            instance = injector.getInstance(type);
        } catch (Exception e) {}

        return instance;
    }

    /**
     * Formats the current engine setting into a string.
     *
     * @return       A string
     */
    private String formatSetup() {
        Game game = this.game;
        Cache<Game> cache = this.cache;
        Leaves<Game> leaves = this.leaves;

        if (game instanceof BenchGame) {
            game = this.game.cast();
        }

        if (cache instanceof BenchCache) {
            cache = this.cache.cast();
        }

        if (leaves instanceof BenchLeaves) {
            leaves = this.leaves.cast();
        }

        return String.format(
            "Engine setup%n" +
            "%s%n" +
            "Time per move: %,39d ms%n" +
            "Depth limit:   %,39d plies%n" +
            "Cache size:    %,39d bytes%n" +
            "Engine class:  %45s%n" +
            "Game class:    %45s%n" +
            "Cache class:   %45s%n" +
            "Leaves class:  %45s%n",
            horizontalRule('-'),
            engine.getMoveTime(),
            engine.getDepth(),
            cache == null ? 0 : cache.size(),
            ellipsis(className(engine), 44),
            ellipsis(className(game), 44),
            ellipsis(className(cache), 44),
            ellipsis(className(leaves), 44)
        );
    }


    /**
     * Formats the current statistics into a string.
     *
     * @return       A string
     */
    private String formatStats() {
        return String.format(
            "Benchmark results%n" +
            "%s%n" +
            "Average branching factor: %,28.2f nodes%n" +
            "Average evaluation depth: %,28.2f plies%n" +
            "Average time per move:    %,28.2f ms   %n" +
            "Maximum time per move:      %,26d ms   %n" +
            "Node visits per second:   %,28.0f nps%n" +
            "Node visits count:          %,26d nodes%n" +
            "Heuristic evaluations:      %,26d nodes%n" +
            "Terminal evaluations:       %,26d nodes%n" +
            "Cache hit ratio:          %,28.2f %%%n" +
            "Endgames book hit ratio:  %,28.2f %%%n",
            horizontalRule('-'),
            stats.branchingFactor(),
            stats.depth().average(),
            stats.movetime().average(),
            stats.movetime().maximum(),
            stats.visitsPerSecond(),
            stats.visits().count(),
            stats.heuristic().count(),
            stats.terminal().count(),
            stats.cache().percentage(),
            stats.leaves().percentage()
        );
    }


    /**
     * String representation of a game suite.
     */
    private static String formatSuite(Suite suite) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add(suite.notation());
        joiner.add(suite.diagram());
        return joiner.toString();
    }


    /**
     * String representation truncated to 60 characters.
     *
     * @param o         An object
     * @param size      Maximum string size
     * @return          A string
     */
    private static String ellipsis(Object o, int size) {
        final String v = String.valueOf(o);
        return v.replaceAll("(?<=^.{" + size + "}).*$", "…");
    }


    /**
     * Returns an horizontal rule of exactly 60 characters.
     *
     * @param c         Rule character
     * @return          A new string
     */
    private static String horizontalRule(char c) {
        return new String(new char[60]).replace('\0', c);
    }


    /**
     * Class name of an object.
     *
     * @param o         An object
     * @return          Class name
     */
    private static String className(Object o) {
        return o == null ? "-" : o.getClass().getName();
    }
}
