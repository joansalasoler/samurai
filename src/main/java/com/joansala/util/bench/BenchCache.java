package com.joansala.util.bench;

/*
 * Copyright (C) 2021-2024 Joan Sala Soler <contact@joansala.com>
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

import com.google.inject.Inject;
import com.joansala.engine.Cache;
import com.joansala.engine.Game;
import com.joansala.util.wrap.WrapCache;


/**
 * A decorated cache that accumulates statistics.
 */
public final class BenchCache extends WrapCache {

    /** Statistics accumulator */
    private BenchStats stats;


    /**
     * Decorates a cache object.
     */
    @Inject
    @SuppressWarnings({"rawtypes", "unchecked"})
    public BenchCache(BenchStats stats, Cache cache) {
        super(cache);
        this.stats = stats;
    }


    /** {@inheritDoc} */
    @Override public boolean find(Game game) {
        return stats.cache().test(cache.find(game.cast()));
    }
}
