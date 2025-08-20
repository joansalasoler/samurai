package com.joansala.test.engine;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

import com.joansala.engine.Engine;
import com.joansala.test.mock.*;


/**
 *
 */
@DisplayName("Engine interface contract")
public interface EngineContract {

    /**
     * Instantiate a new engine object.
     */
    Engine newInstance();


    @Test()
    @DisplayName("game state is the same after search")
    default void ComputeBestMoveKeepsGameState() {
        Engine engine = newInstance();
        MockGame game = new MockGame();

        for (int move : new int[] { 0, 1, 3,  2,  0, 1, 4, 1, 1 }) {
            game.makeMove(move);
        }

        int[] moves = game.moves();
        engine.computeBestMove(game);
        assertArrayEquals(moves, game.moves());
    }


    @Test
    @DisplayName("default values are reasonable")
    default void DefaultValuesAreReasonable() {
        Engine engine = newInstance();
        assertTrue(engine.getDepth() > 0);
        assertTrue(engine.getMoveTime() > 0);
        assertTrue(engine.getInfinity() > 0);
    }


    @Test
    @DisplayName("setMoveTime enforces minimum value")
    default void SetMoveTimeEnforcesMinimumValue() {
        Engine engine = newInstance();
        engine.setMoveTime(0);
        assertTrue(engine.getMoveTime() >= 1);
    }


    @ParameterizedTest
    @ValueSource(ints = {-100, 0, 100})
    @DisplayName("setContempt accepts any integer")
    default void SetContemptAcceptsAnyInteger(int contempt) {
        Engine engine = newInstance();
        engine.setContempt(contempt);
        assertEquals(contempt, engine.getContempt());
    }


    @Test
    @DisplayName("setInfinity enforces positive value")
    default void SetInfinityEnforcesPositiveValue() {
        Engine engine = newInstance();
        engine.setInfinity(0);
        assertTrue(engine.getInfinity() > 0);
    }
}
