package com.joansala.uci.option;

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

import com.joansala.uci.UCIService;
import com.joansala.uci.game.UCICache;
import com.joansala.uci.util.CheckOption;


/**
 * If enabled the engine will use a transpositions table.
 */
public class UseCacheOption extends CheckOption {

    /** Transpositions table provided by the game module */
    private UCICache cache;

    /** Whether the game module provides a transpositions table */
    private boolean enabled = false;


    /**
     * Creates a new option instance.
     */
    public UseCacheOption() {
        super(true);
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEnabled() {
        return enabled;
    }


    /**
     * {@inheritDoc}
     */
    public void initialize(UCIService service) {
        this.cache = service.getCache();
        this.enabled = !cache.isBaseCache();
    }


    /**
     * {@inheritDoc}
     */
    public void handle(UCIService service, boolean active) {
        service.setCache(active ? cache : null);
        service.debug("Cache is now " + (active ? "enabled" : "disabled"));
    }
}
