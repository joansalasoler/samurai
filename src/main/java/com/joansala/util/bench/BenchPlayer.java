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
import com.joansala.uci.UCIClient;
import com.joansala.uci.UCIPlayer;


/**
 * An UCI player that accumulates statistics.
 */
public final class BenchPlayer extends UCIPlayer {

    /** Created instances of this class */
    private static int instances = 0;

    /** Player identifier */
    private final int identifier;

    /** Counts wins by this player */
    public final BenchCounter wins;

    /** Counts draws by this player */
    public final BenchCounter ties;


    /**
     * Create a new UCI player.
     */
    @Inject public BenchPlayer(UCIClient client) {
        super(client);
        identifier = ++instances;
        wins = new BenchCounter();
        ties = new BenchCounter();
    }


    /**
     * Numeric identifier of this player.
     */
    public int identifier() {
        return identifier;
    }
}
