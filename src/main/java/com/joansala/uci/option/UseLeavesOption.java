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
import com.joansala.uci.game.UCILeaves;
import com.joansala.uci.util.CheckOption;


/**
 * If enabled the engine will use an endgames database.
 */
public class UseLeavesOption extends CheckOption {

    /** Endgames database provided by the game module */
    private UCILeaves leaves;

    /** Whether the game module provides endgames */
    private boolean enabled = false;


    /**
     * Creates a new option instance.
     */
    public UseLeavesOption() {
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
        this.leaves = service.getLeaves();
        this.enabled = !leaves.isBaseLeaves();
    }


    /**
     * {@inheritDoc}
     */
    public void handle(UCIService service, boolean active) {
        service.setLeaves(active ? leaves : null);
        service.debug("Leaves are now " + (active ? "enabled" : "disabled"));
    }
}
