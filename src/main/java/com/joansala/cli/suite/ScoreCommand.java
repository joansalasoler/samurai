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
import java.util.concurrent.Callable;
import com.google.inject.Inject;
import com.google.inject.Injector;

import picocli.CommandLine.*;

import com.joansala.cli.util.EngineType;
import com.joansala.engine.Engine;
import com.joansala.engine.Game;
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

    /** Game instance */
    private Game game;

    /** Engine to benchmark */
    private Engine engine;

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
        game = injector.getInstance(Game.class);
        engine = getEngineInstance(engineType);

        setupEngine(engine);
        computeScores();

        return 0;
    }


    /**
     *
     */
    public void computeScores() throws IOException {
        InputStream input = new FileInputStream(file);

        try (SuiteReader reader = new SuiteReader(input)) {
            reader.stream().forEach((suite) -> {
                engine.newMatch();
                suite.setupGame(game);
                String diagram = game.toBoard().toDiagram();
                int score = engine.computeBestScore(game);
                System.out.format("%d, %s%n", score, diagram);
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
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
     * Configures the engine to be benchmarked.
     */
    private void setupEngine(Engine engine) {
        engine.setContempt(game.contempt());
        engine.setInfinity(game.infinity());
        engine.setMoveTime(moveTime);
        engine.setDepth(depth);
    }
}
