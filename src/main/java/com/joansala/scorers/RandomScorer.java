package com.joansala.scorers;

/*
 * Samurai framework.
 * Copyright (C) 2025 Joan Sala Soler <contact@joansala.com>
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

import java.util.concurrent.ThreadLocalRandom;
import com.joansala.engine.Game;
import com.joansala.engine.Scorer;


/**
 * An evaluation function that assigns random scores to game positions.
 *
 * Random evaluation can produce surprisingly decent gameplay when used
 * with sufficient search depth in algorithms like Minimax or MCTS. This
 * occurs because random evaluation naturally biases the search toward
 * positions with higher mobility, which are often strategically superior.
 */
public final class RandomScorer implements Scorer<Game> {

    /** Random number generator */
    private ThreadLocalRandom random = ThreadLocalRandom.current();


    /**
     * {@inheritDoc}
     */
    public final int evaluate(Game game) {
        final int maxScore = game.infinity() - 10;
        return random.nextInt(-maxScore, maxScore + 1);
    }
}
