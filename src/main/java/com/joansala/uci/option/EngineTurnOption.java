package com.joansala.uci.option;

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

import java.util.Arrays;
import java.util.HashSet;
import com.joansala.engine.Engine;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.ComboOption;
import static com.joansala.engine.Game.*;
import static com.joansala.uci.UCI.*;


/**
 * Sets which turn the engine is playing.
 */
public class EngineTurnOption extends ComboOption {

    /**
     * Creates a new option instance.
     */
    public EngineTurnOption() {
        super(values(), "south");
    }


    /**
     * {@inheritDoc}
     */
    public void handle(UCIService service, String value) {
        Engine engine = service.getEngine();
        engine.setTurn(SOUTH_TURN.equals(value) ? SOUTH : NORTH);
        service.debug("Engine plays as " + value);
    }


    /**
     * Set of valid values.
     */
    private static HashSet<String> values() {
        String[] options = { SOUTH_TURN, NORTH_TURN };
        return new HashSet<String>(Arrays.asList(options));
    }
}
