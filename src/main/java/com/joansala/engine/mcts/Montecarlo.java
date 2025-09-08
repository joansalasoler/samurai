package com.joansala.engine.mcts;

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
import com.joansala.scorers.MontecarloScorer;


/**
 * Monte Carlo Tree Search (MCTS) using random playouts.
 *
 * This engine uses random simulations (playouts) to evaluate moves
 * instead of an heuristic function. This can be advantageous if
 * designing an heuristic function is difficult or there is a need to
 * estimate the value of a position without prior human knowledge.
 */
public class Montecarlo extends UCT {

    /** Factors the amount of exploration of the tree */
    public static final double DEFAULT_BIAS = 0.707;

    /** Heuristic evaluation with random playouts */
    private static MontecarloScorer scorer = new MontecarloScorer();


    /**
     * Create a new search engine.
     */
    public Montecarlo() {
        super(DEFAULT_BIAS);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected int simulateMatch(Game game, int maxDepth) {
        return scorer.evaluate(game, maxDepth);
    }
}
