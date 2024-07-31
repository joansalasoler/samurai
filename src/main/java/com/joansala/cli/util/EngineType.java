package com.joansala.cli.util;

/*
 * Samurai framework.
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


import com.joansala.engine.Engine;
import com.joansala.engine.mcts.Montecarlo;
import com.joansala.engine.mindless.Mindless;
import com.joansala.engine.mtd.MTDf;
import com.joansala.engine.negamax.Negamax;
import com.joansala.engine.partner.Partner;
import com.joansala.engine.puct.PUCT;
import com.joansala.engine.sampler.Sampler;
import com.joansala.engine.uct.UCT;


/**
 * Enumeration of engines that can be used for training.
 */
public enum EngineType {

    /** An engine that chooses random moves */
    MINDLESS(Mindless.class),

    /** An UCT engine with random simulations */
    MONTECARLO(Montecarlo.class),

    /** A minimax engine wiht zero-window searches */
    MTDF(MTDf.class),

    /** A minimax engine with alpha-beta pruning */
    NEGAMAX(Negamax.class),

    /** An engine that picks bad moves if playing as north */
    PARTNER(Partner.class),

    /** UCT engine with heuristic evaluations (PUCB) */
    PUCT(PUCT.class),

    /** An engine with pure random simulations */
    SAMPLER(Sampler.class),

    /** UCT engine with heuristic evaluations (UCB1) */
    UCT(UCT.class);

    /** Engine type class */
    private Class<Engine> type;


    /**
     * Instantiates an enum value.
     */
    @SuppressWarnings("unchecked")
    <T extends Engine> EngineType(Class<T> type) {
        this.type = (Class<Engine>) type;
    }


    /**
     * Get the engine type class.
     */
    public Class<Engine> getType() {
        return type;
    }
}
