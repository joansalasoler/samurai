package com.joansala.uci.game;

/*
 * Copyright (c) 2023 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Cache;
import com.joansala.engine.Game;
import com.joansala.engine.base.BaseCache;
import com.joansala.util.wrap.WrapCache;


/**
 * Decorates a cache object.
 */
public final class UCICache extends WrapCache {

    /**
     * Create a new instance of this class.
     */
    public UCICache(Cache<Game> cache) {
        super(cache);
    }


    /**
     * Checks if the wrapped object is a noop implementation.
     */
    public boolean isBaseCache() {
        return cast() instanceof BaseCache;
    }
}
