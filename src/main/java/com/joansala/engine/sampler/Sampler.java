package com.joansala.engine.sampler;

/*
 * Copyright (C) 2021-2025 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Game;
import com.joansala.engine.base.BaseEngine;
import com.joansala.scorers.MontecarloScorer;


/**
 * An engine that chooses the best move by random sampling.
 *
 * This is not a MCTS algorithm, but uses a simplified Monte-Carlo
 * method to estimate the outcome of a game. It is useful as a baseline
 * to compare the performance of other algorithms. At each node, it
 * plays randomly for a fixed number of moves and averages the outcome.
 */
public class Sampler extends BaseEngine {

    /** Heuristic evaluation with random playouts */
    private static MontecarloScorer scorer = new MontecarloScorer();

    /** Best score found so far */
    private int bestScore = Integer.MAX_VALUE;


    /**
     * Create a new search engine.
     */
    public Sampler() {
        super();
    }


    /**
     * Computes the best move for a game and returns its score.
     *
     * @param game      Game instance
     * @return          Average outcome score
     */
    public synchronized int computeBestScore(Game game) {
        computeBestMove(game);
        return -bestScore;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int computeBestMove(Game game) {
        if (game.hasEnded()) {
            bestScore = -(game.outcome() * game.turn());
            return Game.NULL_MOVE;
        }

        scheduleCountDown(moveTime);
        game.ensureCapacity(MAX_DEPTH + game.length());

        int[] moves = game.legalMoves();
        double[] outcomes = new double[moves.length];
        long count = 1L;

        // Play random maches and average their outcome

        while (!aborted() || count <= 1L) {
            for (int i = 0; i < moves.length; i++) {
                final double outcome = outcomes[i];
                final int move = moves[i];

                game.makeMove(move);
                int score = scorer.evaluate(game, maxDepth - 1);
                game.unmakeMove();

                int value = score * game.turn();
                outcomes[i] += (value - outcome) / count;
            }

            if (count++ == Long.MAX_VALUE) {
                break;
            }
        }

        // Pick the move with the best average outcome

        double bestOutcome = outcomes[0];
        int bestMove = moves[0];

        for (int i = 1; i < moves.length; i++) {
            if (outcomes[i] > bestOutcome) {
                bestOutcome = outcomes[i];
                bestMove = moves[i];
            }
        }

        bestScore = (int) bestOutcome;
        cancelCountDown();

        return bestMove;
    }
}
