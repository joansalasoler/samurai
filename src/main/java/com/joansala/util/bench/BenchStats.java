package com.joansala.util.bench;

/*
 * Copyright (c) 2021 Joan Sala Soler <contact@joansala.com>
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

import com.google.inject.Singleton;
import com.joansala.util.StopWatch;


/**
 * Utility class to accumulate statistics.
 */
@Singleton
public final class BenchStats {

    /** Elapsed time stop-watch */
    private StopWatch watch = new StopWatch();

    /** Searched states (root states) */
    private BenchCounter moves = new BenchCounter();

    /** Visited states (moves made) */
    private BenchCounter visits = new BenchCounter();

    /** Exact evaluations */
    private BenchCounter terminal = new BenchCounter();

    /** Heuristic evaluations */
    private BenchCounter heuristic = new BenchCounter();

    /** Average reached depth */
    private BenchAverage depth = new BenchAverage();

    /** Cache probes */
    private BenchCounter cache = new BenchCounter();

    /** Cache probes */
    private BenchCounter leaves = new BenchCounter();

    /** Average search time per move */
    private BenchAverage movetime = new BenchAverage();


    /**
     * Stop watch.
     */
    public StopWatch watch() {
        return watch;
    }


    /**
     * Heuristic evalutation counter.
     */
    public BenchCounter heuristic() {
        return heuristic;
    }


    /**
     * Terminal evalutation counter.
     */
    public BenchCounter terminal() {
        return terminal;
    }


    /**
     * Root nodes counter.
     */
    public BenchCounter moves() {
        return moves;
    }


    /**
     * Node visits counter.
     */
    public BenchCounter visits() {
        return visits;
    }


    /**
     * Movetime average statistics.
     */
    public BenchAverage movetime() {
        return movetime;
    }


    /**
     * Depth average statistics.
     */
    public BenchAverage depth() {
        return depth;
    }


    /**
     * Cache probes counter.
     */
    public BenchCounter cache() {
        return cache;
    }


    /**
     * Leaves probes counter.
     */
    public BenchCounter leaves() {
        return leaves;
    }


    /**
     * Number of visited nodes per second.
     */
    public double visitsPerSecond() {
        return visits.count() / watch.seconds();
    }


    /**
     * Average branching factor approximation.
     */
    public double branchingFactor() {
        long count = terminal.count() + heuristic.count();
        return Math.pow(count / moves.count(), 1 / depth.average());
    }
}
