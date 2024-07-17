package com.joansala.cli.suite;

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
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Injector;

import picocli.CommandLine.*;

import com.joansala.cli.util.EngineType;
import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import com.joansala.util.suites.Suite;
import com.joansala.util.suites.SuiteReader;


/**
 *
 */
@Command(
  name = "score",
  description = "Compute the scores for a set of suites",
  mixinStandardHelpOptions = true
)
public class ScoreCommand implements Callable<Integer> {

    /** Dependency injector */
    private Injector injector;

    @Option(
      names = "--file",
      description = "A suite file"
    )
    private File file;

    @Option(
      names = "--engine",
      description = "Custom engine (multiple)"
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
      names = "--threads",
      description = "Size of the evaluation thread pool"
    )
    private int poolSize = Runtime.getRuntime().availableProcessors();


    /**
     *
     */
    @Inject public ScoreCommand(Injector injector) {
        this.injector = injector;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        computeScores();
        return 0;
    }


    /**
     *
     */
    public void computeScores() throws IOException {
        InputStream input = new FileInputStream(file);
        Executor executor = new Executor(poolSize);
        BlockingQueue<Evaluator> tasks = new ArrayBlockingQueue<>(poolSize);

        for (int i = 0; i < poolSize; i++) {
            tasks.add(new Evaluator());
        }

        try (SuiteReader reader = new SuiteReader(input)) {
            reader.stream().forEach((suite) -> executor.submit(() -> {
                try{
                    Evaluator task = tasks.take();
                    task.evaluateSuite(suite);
                    tasks.put(task);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException(e);
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
            executor.shutdown();
            System.exit(1);
        }

        executor.shutdown();
    }


    /**
     * Configures the engine to be benchmarked.
     */
    private void setupEngine(Engine engine, Game game) {
        engine.setContempt(game.contempt());
        engine.setInfinity(game.infinity());
        engine.setMoveTime(moveTime);
        engine.setDepth(depth);
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
     * Evaluates a game states using an engine.
     */
    private class Evaluator {

        private Game game;
        private Engine engine;


        private Evaluator() {
            game = injector.getInstance(Game.class);
            engine = getEngineInstance(engineType);
            setupEngine(engine, game);
        }


        /**
         * Compute the best score for a state.
         */
        private void evaluateSuite(Suite suite) {
            engine.newMatch();
            suite.setupGame(game);
            String diagram = game.toBoard().toDiagram();
            int score = game.turn() * engine.computeBestScore(game);
            System.out.format("%s, %d%n", diagram, score);
        }
    }


    /**
     * Executes tasks in a fixed thread pool.
     */
    public class Executor {

        private final Semaphore semaphore;
        private final ExecutorService executor;


        private Executor(int poolSize) {
            executor = Executors.newFixedThreadPool(poolSize);
            semaphore = new Semaphore(poolSize);
        }


        /**
         * Submit a task for execution.
         */
        public void submit(Runnable task) {
            try {
                semaphore.acquire();
                executor.submit(() -> {
                    task.run();
                    semaphore.release();
                });
            } catch (InterruptedException e) {
                semaphore.release();
            }
        }


        /**
         * Shutdown this executor and await termination.
         */
        public void shutdown() {
            try {
                executor.shutdown();

                long waitTime = 2 * poolSize * moveTime;
                TimeUnit timeUnit = TimeUnit.MILLISECONDS;

                if (!executor.awaitTermination(waitTime, timeUnit)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }
}
