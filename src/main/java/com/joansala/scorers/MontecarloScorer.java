package com.joansala.scorers;

/*
 * Samurai framework.
 * Copyright (C) 2025 Joan Sala Soler <contact@joansala.com>
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

import java.util.SplittableRandom;
import com.joansala.engine.Game;
import com.joansala.engine.Scorer;


/**
 * Estimates the value of a game state using a single random playout.
 *
 * This function simulates a sequence of random moves from the current
 * position to the end of the game and returns the outcome as a heuristic
 * evaluation. It provides a simple way to estimate the value of a position
 * without any prior knowledge or handcrafted evaluation functions.
 *
 * To obtain a more reliable estimate, this function can be invoked multiple
 * times and the results averaged.
 */
public final class MontecarloScorer implements Scorer<Game> {

    /** Default depth limit per simulation */
    private static final int DEFAULT_DEPTH = 127;

    /** Random number generator */
    private SplittableRandom random = new SplittableRandom();


    /**
     * {@inheritDoc}
     */
    public final int evaluate(Game game) {
        return simulateMatch(game, DEFAULT_DEPTH);
    }


    /**
     * Evaluates the current state of the game.
     *
     * @param game      State to be evaluated.
     * @param maxDepth  Maximum depth of the simulation.
     * @return          Heuristic or utilty evaluation score.
     */
    public final int evaluate(Game game, int maxDepth) {
        return simulateMatch(game, maxDepth);
    }


    /**
     * Simulates a match and return its final score.
     *
     * @param game      Initial game state
     * @param depth     Maximum simulation depth
     * @return          Outcome of the simulation
     */
    private int simulateMatch(Game game, int maxDepth) {
        int depth = 0;

        while (depth < maxDepth && !game.hasEnded()) {
            final int move = getRandomMove(game);
            game.makeMove(move);
            depth++;
        }

        final int score = game.outcome();
        game.unmakeMoves(depth);

        return score;
    }


    /**
     * Selects a random move from the list of possible moves.
     *
     * Chooses a move using a variant of reservoir-sampling that works
     * even without knowing the list length. It ensures each element
     * has an equal chance of being chosen.
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
