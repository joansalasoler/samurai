package com.joansala.util.bits;

/*
 * Samurai framework.
 * Copyright (C) 2022-2024 Joan Sala Soler <contact@joansala.com>
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


import java.io.*;


/**
 * Hash table implementation for bit words of variable length.
 */
public class BitsetMap {

    /** How many words to store on each entry */
    public final int entrySize;

    /** How many bits does each entry have */
    public final int wordSize;

    /** A mask with exactly {@code wordSize} bits set */
    public final long wordMask;

    /** Contains the hash table data */
    private final long[] entries;


    /**
     * Creates a new hash table that can store {@code capacity} words
     * of {@code wordSize} bits each.
     *
     * @param wordSize      Length in bits of each entry
     * @param capacity      Number of entries on this store
     */
    public BitsetMap(int wordSize, long capacity) {
        this.wordSize = wordSize;
        this.wordMask = (1 << wordSize) - 1L;
        this.entrySize = Long.SIZE / wordSize;
        this.entries = new long[1 + (int) (capacity / entrySize)];
    }


    /**
     * Obtain the word to which the given hash is mapped.
     *
     * @param hash      Hash of the entry
     * @return          Value of the entry
     */
    public long get(long hash) {
        final int slot = getSlot(hash);
        final int position = getPosition(hash);
        return wordMask & (entries[slot] >>> position);
    }


    /**
     * Associates a hash with a value.
     *
     * @param hash      Hash of the entry
     * @param value     Value of the entry
     */
    public void put(long hash, long value) {
        final int slot = getSlot(hash);
        final int position = getPosition(hash);
        entries[slot] &= ~(wordMask << position);
        entries[slot] |= ((wordMask & value) << position);
    }


    /**
     * Index of the {@code entries} array slot where the given hash
     * code must be associated.
     *
     * @param hash      Hash of an entry
     * @return          Index of an entry
     */
    private int getSlot(long hash) {
        return (int) (hash / entrySize);
    }


    /**
     * Position where a word must be stored inside an entry.
     *
     * @param hash      Hash of an entry
     * @return          Position of an entry
     */
    private int getPosition(long hash) {
        return (int) (wordSize * (hash % entrySize));
    }


    /**
     * Reads the contents of this bit map from a file.
     *
     * @param stream        Data input stream
     */
    public void readFromFile(DataInputStream stream) throws IOException {
        try {
            for (int i = 0; i < entries.length; i++) {
                entries[i] = stream.readLong();
            }
        } catch (EOFException e) {
            // Allow reading a partial table.
        }

        stream.close();
    }


    /**
     * Writes the contents of this bit map to a file.
     *
     * @param stream        Data output stream
     */
    public void writeToFile(DataOutputStream stream) throws IOException {
        for (int i = 0; i < entries.length; i++) {
            stream.writeLong(entries[i]);
        }

        stream.close();
    }
}
