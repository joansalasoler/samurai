package com.joansala.engine.mtd;

/*
 * Copyright (C) 2014-2024 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.*;
import com.joansala.engine.negamax.Negamax;


/**
 * Implements a game engine using a MTD(f) framework.
 */
public class MTDf extends Negamax {

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int computeBestMove(Game game) {
        // If the game ended on that position return a null move
        // and set the best score accordingly

        if (game.hasEnded()) {
            bestScore = -(game.outcome() * game.turn());
            return Game.NULL_MOVE;
        }

        // Get ready for the move computation

        scheduleCountDown(moveTime);
        game.ensureCapacity(MAX_DEPTH + game.length());
        cache.discharge();

        // Compute all legal moves for the game

        int[] rootMoves = game.legalMoves();
        bestScore = Game.DRAW_SCORE;
        scoreDepth = 0;

        // Check for a hash move and reorder moves accordingly

        if (cache.find(game) && cache.getMove() != Game.NULL_MOVE) {
            final int hashMove = cache.getMove();

            for (int index = 0; index < 6; index++) {
                if (rootMoves[index] == hashMove) {
                    System.arraycopy(rootMoves, 0, rootMoves, 1, index);
                    bestScore = cache.getScore();
                    rootMoves[0] = hashMove;
                    break;
                }
            }
        }

        // Iterative deepening search for a best move

        int score;
        int depth = MIN_DEPTH;
        int lastScore = maxScore;
        int lastMove = Game.NULL_MOVE;
        int bestMove = rootMoves[0];

        while (!aborted() || depth == MIN_DEPTH) {
            int upper = maxScore;
            int lower = minScore;

            while (!aborted() && lower < upper) {
                int scoreGuess = Math.max(bestScore, lower + 1);
                int alpha = scoreGuess - 1;
                int beta = scoreGuess;

                for (int move : rootMoves) {
                    game.makeMove(move);
                    score = search(game, alpha, beta, depth);
                    game.unmakeMove();

                    if (aborted() && depth > MIN_DEPTH) {
                        bestMove = lastMove;
                        bestScore = lastScore;
                        break;
                    }

                    if (score < beta) {
                        bestMove = move;
                        bestScore = score;
                        beta = score;
                    } else if (score == beta) {
                        bestScore = score;
                    }
                }

                // Update score bounds

                if (bestScore < scoreGuess) {
                    upper = bestScore;
                } else {
                    lower = bestScore;
                }
            }

            // Stop if an exact score or time was exhausted

            if (!aborted() || depth == MIN_DEPTH) {
                scoreDepth = depth;
            }

            if (Math.abs(bestScore) == maxScore) {
                break;
            }

            if (aborted() || depth >= maxDepth) {
                break;
            }

            // Create a report of the current search results

            if (depth > MIN_DEPTH) {
                if (bestMove != lastMove || bestScore != lastScore) {
                    invokeConsumers(game, bestMove, bestScore);
                } else if (depth == 2 + MIN_DEPTH) {
                    invokeConsumers(game, bestMove, bestScore);
                }
            }

            lastMove = bestMove;
            lastScore = bestScore;
            depth += 2;
        }

        cache.store(game, bestScore, bestMove, scoreDepth, Flag.EXACT);
        invokeConsumers(game, bestMove, bestScore);
        cancelCountDown();

        return bestMove;
    }
}