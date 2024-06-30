package com.joansala.uci.command;

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

import com.joansala.engine.Engine;
import com.joansala.uci.UCIBrain;
import com.joansala.uci.UCICommand;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.Parameters;
import com.joansala.uci.util.TimeManager;


/**
 * Instructs the engine to switch from ponder to normal search.
 */
public class PonderHitCommand implements UCICommand {

    /**
     * {@inheritDoc}
     */
    public void accept(UCIService service, Parameters params) {
        UCIBrain brain = service.getBrain();
        Engine engine = service.getEngine();
        TimeManager manager = service.getTimeManager();

        int turn = brain.getSearchTurn();
        long moveTime = manager.getMoveTimeAdvice(turn);
        engine.abortComputation(moveTime);
    }
}
