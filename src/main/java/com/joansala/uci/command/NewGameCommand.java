package com.joansala.uci.command;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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
import com.joansala.engine.Game;
import com.joansala.engine.Roots;
import com.joansala.uci.UCICommand;
import com.joansala.uci.UCIService;
import com.joansala.uci.game.UCIGame;
import com.joansala.uci.util.Parameters;
import com.joansala.uci.util.TimeManager;


/**
 * Tells the service that next positions are from a different game.
 * If the engine is still thinking does nothing.
 */
public class NewGameCommand implements UCICommand {
    public void accept(UCIService service, Parameters params) {
        if (service.isReady() == false) {
            throw new IllegalStateException(
                "Engine is not ready");
        }

        UCIGame game = service.getGame();
        Engine engine = service.getEngine();
        Roots<Game> roots = service.getRoots();
        TimeManager timeManager = service.getTimeManager();

        if (roots instanceof Roots) {
            timeManager.newMatch();
            engine.newMatch();
            roots.newMatch();
            game.newMatch();
        } else {
            timeManager.newMatch();
            engine.newMatch();
            game.newMatch();
        }
    }
}
