package com.joansala.util.hash;

/*
 * Samurai framework.
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


/**
 * Minimal perfect hashing of integer arrays.
 *
 * Uses Lehmer codes to compute the hash of each array. Requires that
 * the arrays to hash represent a permutation without replacement, that is,
 * each element is a distinct number from zero to {@code count}. Partial
 * permutations are also supported.
 */
public class LehmerHash implements HashFunction {

    /** Binomial coefficients */
    protected final long[] binomials;

    /** Length of the integer array */
    protected final int length;

    /** Number of distinct elements */
    protected final int count;


    /**
     * Instantiates this object.
     *
     * @param count     Number of distinct elements
     * @param length    Fixed length of the array
     */
    public LehmerHash(int count, int length) {
        this.binomials = new long[length];
        this.length = length;
        this.count = count;
        initialize();
    }


    /**
     * Precomputes the binomial coefficients table.
     */
    private void initialize() {
        for (int i = 0; i < length; i++) {
            final int n = count - i - 1;
            final int k = length - i - 1;
            final long c = factorial(k);
            binomials[i] = c * binomial(n, k);
        }
    }


    /**
     * Computes the factorial of an integer.
     *
     * @param n     An integer number
     * @return      Factorial of the number
     */
    private static long factorial(int n) {
        long value = 1L;

        for (int i = 1; i <= n; ++i) {
            value = i * value;
        }

        return value;
    }


    /**
     * Computes the binomial coefficient C(n, k).
     *
     * @param n     Number of objects
     * @param k     Number of choices
     * @return      Binomial coefficient
     */
    private static long binomial(int n, int k) {
        long value = 1L;

        if (k > n - k) {
            k = n - k;
        }

        for (int i = 0; i < k; i++) {
            value *= (n - i);
            value /= (i + 1);
        }

        return value;
    }


    /**
     * {@inheritDoc}
     */
    public long hash(Object state) {
        return hash((int[]) state);
    }


    /**
     * Compute the hash of an integer array.
     *
     * @param state     An array
     * @return          Hash value
     */
    public long hash(int[] state) {
        long counted = 1L << state[0];
        long hash = state[0] * binomials[0];

        for (int i = 1; i < length; i++) {
            final long bit = 1L << state[i];
            final int lower = Long.bitCount(counted & (bit - 1));
            hash += (state[i] - lower) * binomials[i];
            counted ^= bit;
        }

        return hash;
    }


    /**
     * Convert a hash into its array representation.
     *
     * @param hash      Hash value
     * @return          A new array
     */
    public int[] unhash(long hash) {
        int[] state = new int[length];

        for (int i = 0; i < length; i++) {
            final long base = binomials[i];
            state[i] = (int) (hash / base);
            hash = hash % base;
        }

        for (int i = length - 1; i >= 0; i--) {
            for (int n = i + 1; n < length; n++) {
                if (state[n] >= state[i]) {
                    state[n]++;
                }
            }
        }

        return state;
    }
}
