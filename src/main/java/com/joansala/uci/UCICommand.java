package com.joansala.uci;

/*
 * Copyright (C) 2014-2024 Joan Sala Soler <contact@joansala.com>
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

import java.util.function.BiConsumer;
import com.joansala.uci.util.Parameters;


/**
 * Functional interface for UCI commands.
 */
public interface UCICommand extends BiConsumer<UCIService, Parameters> {

    /**
     * Array of accepted parameter names.
     */
    default String[] parameterNames() {
        return new String[0];
    }
}
