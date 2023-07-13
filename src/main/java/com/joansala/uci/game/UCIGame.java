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

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.joansala.engine.Game;
import com.joansala.util.wrap.WrapGame;


/**
 * This is the game object used by the UCI brain to generate moves.
 *
 * For now it only adds the option to add random noise to the results of
 * the heuristic evaluation function. This noise can be used to weaken
 * the engine's strength or increase the number of explored nodes.
 */
public final class UCIGame extends WrapGame {

    /** Random number generator */
    private Random random = ThreadLocalRandom.current();

    /** How many bits to shift to obtain the noise value */
    private int noiseShift = Long.SIZE;

    /** Noise level from zero to {@code} MAX_SCORE} */
    private int noiseLevel = 0;

    /** Randomization value for the current match */
    private long randomizer;


    /**
     * Decorates a game object.
     */
    public UCIGame(Game game) {
        super(game);
        setNoiseLevel(noiseLevel);
    }


    /**
     * Instructs this object a new match is about to start.
     */
    public void newMatch() {
        randomizer = nextRandomLong();
    }


    /**
     * Returns the current score noise level.
     */
    public int getNoiseLevel() {
        return noiseLevel;
    }


    /**
     * Sets the current score noise level.
     */
    public void setNoiseLevel(int level) {
        int v = Math.max(0, Math.min(level, game.infinity()));
        int n = Integer.SIZE - Integer.numberOfLeadingZeros(v);
        noiseShift = Long.SIZE - (1 + n);
        noiseLevel = level;
    }


    /**
     * Obtain a random noise score for the current state.
     */
    private int noise() {
        final long value = randomizer * hash();
        final int noise = (int) (value >> noiseShift);
        return noiseLevel > 0 ? noise : 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int score() {
        return noise() + game.score();
    }


    /**
     * Obtains a random number that will be used to add noise to
     * the heuristic evaluation function.
     */
    private long nextRandomLong() {
        int size = Integer.numberOfLeadingZeros(game.infinity());
        long minValue = 1L << (3 * Integer.SIZE - size);
        long range = Long.MAX_VALUE - minValue;
        return minValue + random.nextLong() % range;
    }
}
