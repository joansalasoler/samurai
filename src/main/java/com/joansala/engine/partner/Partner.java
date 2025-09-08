package com.joansala.engine.partner;

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
import com.joansala.engine.uct.UCT;
import com.joansala.engine.uct.UCTNode;
import com.joansala.scorers.MontecarloScorer;


/**
 * Cooperative Monte Carlo Tree Search (MCTS) using random playouts.
 *
 * This is a MCTS algorithm using random simulations designed for
 * single-player games. It simulates two players (north and south)
 * working together, but secretly trying to win. North always chooses
 * the worst possible move. South always chooses the best possible move.
 *
 * The algorithm requires a utility function (outcome) that treats
 * losses as draws. This is because in this single-player scenario, a
 * loss for one player is considered a loss for both players.
 *
 * This algorithm can be seen in action in the GGP module.
 */
public class Partner extends UCT {

    /** Factors the amount of exploration of the tree */
    public static final double DEFAULT_BIAS = 0.353;

    /** Heuristic evaluation with random playouts */
    private static MontecarloScorer scorer = new MontecarloScorer();


    /**
     * Create a new search engine.
     */
    public Partner() {
        super(DEFAULT_BIAS);
    }


    /**
     * Best child found so far for the given node.
     *
     * @param node      Parent node
     * @return          Child node
     */
    @Override
    protected UCTNode pickBestChild(UCTNode node) {
        UCTNode child = node.child();
        UCTNode bestChild = node.child();
        double bestScore = computeScore(bestChild);

        while ((child = child.sibling()) != null) {
            double score = computeScore(child);

            if (score < bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }

        return bestChild;
    }


    /**
     * Compute the selection score of a node.
     *
     * @param node      A node
     * @return          Score of the node
     */
    private double computeScore(UCTNode node) {
        final double bound = maxScore / Math.sqrt(node.count());
        final double score = node.score() + bound;

        return -node.turn() * score;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected int simulateMatch(Game game, int maxDepth) {
        return scorer.evaluate(game, maxDepth);
    }
}
