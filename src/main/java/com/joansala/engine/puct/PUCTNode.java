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
import com.joansala.engine.uct.UCTNode;


/**
 * A game state on a search tree.
 */
public class PUCTNode extends UCTNode {

    /** Heuristic evaluation */
    private double bias = 0.0;


    /**
     * Create a new node for a game state.
     */
    protected PUCTNode(Game game, int move) {
        super(game, move);
    }


    /**
     * Set the expansion bias factor of this node.
     */
    protected void initBias(double value) {
        bias = value;
    }


    /**
     * Expansion bias factor.
     */
    public double bias() {
        return bias;
    }
}
