package com.joansala.engine.puct;

/*
 * Copyright (C) 2024 Joan Sala Soler <contact@joansala.com>
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
import com.joansala.engine.uct.*;


/**
 * Monte Carlo Tree Search (MCTS) with PUCB.
 *
 * This class implements the core MCTS algorithm using Upper Confidence
 * Bounds (UCB1) and predictors for node selection. It extends the
 * {@link UCT} algorithm by using the heuristic evaluation of noves to
 * compute their expansion prioriy.
 */
public class PUCT extends UCT {

    /**
     * {@inheritDoc}
     */
    @Override
    protected UCTNode createNode(Game game, int move) {
        return new PUCTNode(game, move);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected UCTNode pickLeadChild(UCTNode parent) {
        UCTNode child = parent.child();
        UCTNode bestNode = parent.child();
        double factor = Math.sqrt(parent.count());
        double bestScore = computePriority(child, factor);

        while ((child = child.sibling()) != null) {
            double score = computePriority(child, factor);

            if (score < bestScore) {
                bestScore = score;
                bestNode = child;
            }
        }

        return bestNode;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected double evaluate(UCTNode node, Game game, int depth) {
        return evaluate((PUCTNode) node, game, depth);
    }


    /**
     * Evaluate a node and set its prior node expansion bias.
     */
    private double evaluate(PUCTNode node, Game game, int depth) {
        final double score = super.evaluate(node, game, depth);
        node.initBias(exploreFactor * Math.abs(score));
        return score;
    }


    /**
     * Computes the expansion priority of an edge (PUCB).
     *
     * @param child      Child node
     * @param factor     Parent factor
     * @return           Expansion priority
     */
    private double computePriority(UCTNode child, double factor) {
        final PUCTNode node = (PUCTNode) child;
        final double E = factor / child.count();
        return child.score() - E * node.bias();
    }
}
