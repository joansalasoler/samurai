package com.joansala.test.engine.mcts;

import org.junit.jupiter.api.*;
import com.joansala.engine.Engine;
import com.joansala.engine.mcts.Montecarlo;
import com.joansala.test.engine.EngineContract;


@DisplayName("Montecarlo engine")
public class MontecarloTest implements EngineContract {

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine newInstance() {
        return new Montecarlo();
    }
}
