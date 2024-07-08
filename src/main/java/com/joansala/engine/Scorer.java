package com.joansala.engine;

/*
 * Samurai framework.
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

/**
 * This functional interface defines a method to evaluate the current
 * state of a zero-sum game. Zero-sum games are games where one player's
 * gain is exactly balanced by the other player's loss.
 *
 * This method takes the current state of the game as input and returns
 * a score representing the estimated value of that state from the point
 * of view of the first player to move (White in Chess, South in Oware,
 * etc). A positive score favors that player while a negative score means
 * that the position is unfavorable for the player.
 *
 * The score returned is typically a heuristic value, meaning it's an
 * estimate of how good a position is, not necessarily the guaranteed
 * outcome of the game. A good heuristic function balances the accuracy
 * of a position's evaluation with a fast calculation.
 *
 * If this interface implements an heuristic evaluation (estimateed
 * outcome), the returned score should be a value between {@code -MAX_SCORE}
 * and {@code MAX_SCORE} (exclusive), where {@code MAX_SCORE} is an
 * arbitrarily chosen maximum score.
 *
 * If this interface implements a utility evaluation (guaranteed outcome)
 * it should usually return exactly {@code ±MAX_SCORE} for a guaranteed
 * win or loss, respectively.
 *
 * @see Game#score()
 * @see Game#outcome()
 * @see Game#infinity()
 *
 * @param <T>    Game type, must extend {@link Game}
 */
@FunctionalInterface
public interface Scorer<T extends Game> {

    /**
     * Evaluates the current state of the game.
     *
     * @param game  State to be evaluated.
     * @return      Heuristic or utilty evaluation score.
     */
    int evaluate(T game);

}