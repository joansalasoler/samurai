package com.joansala.util.bench;

/*
 * Copyright (c) 2021 Joan Sala Soler <contact@joansala.com>
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
import com.joansala.util.wrap.WrapGame;


/**
 * A decorated game that accumulates statistics.
 */
public final class BenchGame extends WrapGame {

    /** Statistics accumulator */
    private BenchStats stats;


    /**
     * Decorates a game object.
     */
    public BenchGame(BenchStats stats, Game game) {
        super(game);
        this.stats = stats;
    }


    /** {@inheritDoc} */
    @Override public void makeMove(int move) {
        stats.visits().increment();
        game.makeMove(move);
    }


    /** {@inheritDoc} */
    @Override public int score() {
        stats.heuristic().increment();
        stats.depth().aggregate(game.length());
        return game.score();
    }


    /** {@inheritDoc} */
    @Override public int outcome() {
        stats.terminal().increment();
        stats.depth().aggregate(game.length());
        return game.outcome();
    }
}
