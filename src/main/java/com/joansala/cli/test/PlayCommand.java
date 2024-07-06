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

/**
 * Compute best moves for the given game suites. Does the same as
 * {@code BenchCommand} but without counting statistics.
 */
@Command(
  name = "play",
  description = "Play the given game suites",
  mixinStandardHelpOptions = true
)
public class PlayCommand implements Callable<Integer> {

    /** Dependency injector */
    private Injector injector;

    /** Game board instance */
    private Board parser;

    /** Engine to benchmark */
    private Engine engine;

    /** Decorated game instance */
    private Game game;

    @Option(
      names = "--engine",
      description = "Custom engine (multiple)"
    )
    private EngineType[] engineTypes = null;

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
    @Inject public PlayCommand(Injector injector) {
        this.injector = injector;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        game = injector.getInstance(Game.class);
        parser = injector.getInstance(Board.class);

        if (engineTypes != null && engineTypes.length > 0) {
            for (EngineType engineType : engineTypes) {
                engine = getEngineInstance(engineType);
                setupEngine();
                runBenchmark();
            }
        } else {
            engine = getEngineInstance(null);
            setupEngine();
            runBenchmark();
        }

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
                engine.computeBestMove(game);

                for (int move : moves) {
                    if (game.hasEnded() == false) {
                        game.makeMove(move);
                        engine.computeBestMove(game);
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

        System.out.println();
    }


    /**
     * Configures the engine to be benchmarked.
     */
    private void setupEngine() {
        engine.setContempt(game.contempt());
        engine.setInfinity(game.infinity());
        engine.setMoveTime(moveTime);
        engine.setDepth(depth);
    }


    /**
     * Formats the current engine setting into a string.
     *
     * @return       A string
     */
    private String formatSetup() {
        return String.format(
            "Engine setup%n" +
            "%s%n" +
            "Time per move: %,39d ms%n" +
            "Depth limit:   %,39d plies%n" +
            "Engine class:  %45s%n" +
            "Game class:    %45s%n",
            horizontalRule('-'),
            engine.getMoveTime(),
            engine.getDepth(),
            ellipsis(className(engine), 44),
            ellipsis(className(game), 44)
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
