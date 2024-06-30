package com.joansala.uci.game;

/*
 * Copyright (C) 2023-2024 Joan Sala Soler <contact@joansala.com>
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
import com.joansala.engine.Leaves;
import com.joansala.engine.base.BaseLeaves;
import com.joansala.util.wrap.WrapLeaves;


/**
 * Decorates a leaves object.
 */
public final class UCILeaves extends WrapLeaves {

    /**
     * Create a new instance of this class.
     */
    public UCILeaves(Leaves<Game> leaves) {
        super(leaves);
    }


    /**
     * Checks if the wrapped object is a noop implementation.
     */
    public boolean isBaseLeaves() {
        return cast() instanceof BaseLeaves;
    }
}
