package com.joansala.cli.suite;

/*
 * Samurai framework.
 * Copyright (C) 2021 Joan Sala Soler <contact@joansala.com>
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

import java.util.Random;
import java.util.concurrent.Callable;
import com.google.inject.Inject;
import picocli.CommandLine.*;

import com.joansala.engine.Board;
import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import com.joansala.util.suites.Suite;


/**
 * Generates random game suites.
 */
@Command(
  name = "random",
  description = "Generate game suites from random playouts",
  mixinStandardHelpOptions = true
)
public class RandomCommand implements Callable<Integer> {

    /** Random number generator */
    private final Random random = new Random();

    /** Root game board */
    private final Board rootBoard;

    /** Current game state */
    private final Game game;

    @Option(
      names = "--depth",
      description = "Playout depth limit (plies)"
    )
    private int maxDepth = Engine.DEFAULT_DEPTH;

    @Option(
      names = "--endgame",
      description = "Play until the game ends"
    )
    private boolean endGame = false;

    @Option(
      names = "--size",
      description = "Number of suites to generate"
    )
    private int size = 100;


    /**
     * Creates a new trainer.
     */
    @Inject public RandomCommand(Game game) {
        this.game = game;
        this.rootBoard = game.getBoard();
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        for (int i = 0; i < size; i++) {
            Suite suite = generateSuite(rootBoard, game);
            System.out.println(suite);
        }

        return 0;
    }


    /**
     * Generate a new board by performing a random play.
     *
     * @param board         Root game board
     * @param game          A game object
     */
    private Suite generateSuite(Board board, Game game) {
        int depth = random.nextInt(maxDepth);
        game.setBoard(board);

        while ((endGame || depth < maxDepth) && !game.hasEnded()) {
            final int move = getRandomMove(game);
            game.ensureCapacity(1 + game.length());
            game.makeMove(move);
            depth++;
        }

        return Suite.fromGame(game);
    }


    /**
     * Pick a random legal move given a game state.
     *
     * @param game      Game state
     * @return          Chosen move
     */
    private int getRandomMove(Game game) {
        int count = 0;
        int move = Game.NULL_MOVE;
        int choice = Game.NULL_MOVE;

        while ((move = game.nextMove()) != Game.NULL_MOVE) {
            if (random.nextInt(++count) == 0) {
                choice = move;
            }
        }

        return choice;
    }
}
