package com.joansala.engine.base;

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


import java.util.logging.Logger;
import com.google.inject.AbstractModule;


/**
 * Base game module.
 */
public abstract class BaseModule extends AbstractModule {

    /** Module logger instance */
    protected static Logger logger = Logger.getLogger("com.joansala");


    /**
     * Module service command line parameters.
     */
    public String[] getServiceParameters() {
        String main = getClass().getName();
        String[] params = { main, "service" };
        return params;
    }
}
