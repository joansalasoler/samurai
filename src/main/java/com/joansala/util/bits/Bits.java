package com.joansala.util.bits;

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


/**
 * Bitwise operations.
 */
public final class Bits {

    /**
     * Check if none of the bits of a bitboard are set.
     *
     * @param bitboard      Bitboard to check
     * @return              True if no bit is set
     */
    public static final boolean empty(long bitboard) {
        return bitboard == 0L;
    }


    /**
     * Index of the rightmost bit that is set on a bitboard.
     *
     * @param bitboard      Bitboard
     * @return              Bit index
     */
    public static final int first(long bitboard) {
        return Long.numberOfTrailingZeros(bitboard);
    }


    /**
     * Index of the leftmost bit that is set on a bitboard.
     *
     * @param bitboard      Bitboard
     * @return              Bit index
     */
    public static final int last(long bitboard) {
        return Long.numberOfLeadingZeros(bitboard) ^ 63;
    }


    /**
     * Index of the next rightmost bit that is set on a bitboard.
     *
     * @param bitboard      Bitboard
     * @param index         Current bit index
     * @return              Bit index
     */
    public static final int next(long bitboard, int index) {
        return Long.numberOfTrailingZeros(bitboard & (-2L << index));
    }


    /**
     * Check if some bits are set on a bitboard.
     *
     * @param bitboard      Bitboard to check
     * @param bits          Bits to check
     * @return              If one or more bits are set
     */
    public static final boolean contains(long bitboard, long bits) {
        return (bitboard & bits) != 0L;
    }


    /**
     * Check if all the given bits are set on a bitboard.
     *
     * @param bitboard      Bitboard to check
     * @param bits          Bits to check
     * @return              If all the bits are set
     */
    public static final boolean includes(long bitboard, long bits) {
        return (bitboard & bits) == bits;
    }


    /**
     * Counts the number of stones on a bitboard.
     *
     * @param bitboard      Bitboard to count
     * @return              Number of stones
     */
    public static final int count(long bitboard) {
        return Long.bitCount(bitboard);
    }


    /**
     * Returns a bitboard with one bit set at the given index.
     *
     * @param index         Index of the bit to set
     * @return              Bitboard
     */
    public static final long bit(int index) {
        return 1L << index;
    }


    /**
     * Shifts a bitboard the given number of bits.
     *
     * This method shifts to the left or right according to the following
     * logic: if {@code n} is lower than 64 it shifts the bitboard {@code n}
     * bits to the left; otherwise it performs an unsigned shift {@code n % 64}
     * bits to the right.
     *
     * @param bitboard      Bitboard to shift
     * @param n             Number of bits to shift
     * @return              Shifted bitboard
     */
    public static final long shift(long bitboard, int n) {
        return (n < 64 ? bitboard << n : bitboard >>> n);
    }


    /**
     * Removes a single bit from a bitboard.
     *
     * @param bitboard      Bitboard
     * @param index         Bit index
     * @return              New bitboard
     */
    public static final long remove(long bitboard, int index) {
        final long mask = (1L << index) - 1;
        final long upper = (bitboard & ~mask) >>> 1;
        final long lower = (bitboard & mask);
        return upper | lower;
    }


    /**
     * Inserts a single bit on a bitboard.
     *
     * @param bitboard      Bitboard
     * @param index         Bit index
     * @return              New bitboard
     */
    public static final long insert(long bitboard, int index) {
        final long mask = (1L << index) - 1;
        final long upper = (bitboard & ~mask) << 1;
        final long lower = (bitboard & mask);
        return upper | lower;
    }


    /**
     * Rotate 180 degrees an 8x8 bitboard.
     *
     * @param bitboard      Bitboard
     * @return              New bitboard
     */
    public static final long rotate(long bitboard) {
        return Long.reverse(bitboard);
    }


    /**
     * Rotate 90 degrees clockwise an 8x8 bitboard.
     *
     * @param bitboard      Bitboard
     * @return              New bitboard
     */
    public static final long rotate90(long bitboard) {
        return mirrorX(transposeXY(bitboard));
    }


    /**
     * Rotate 270 degrees clockwise an 8x8 bitboard.
     *
     * @param bitboard      Bitboard
     * @return              New bitboard
     */
    public static final long rotate270(long bitboard) {
        return rotate(mirrorX(transposeXY(bitboard)));
    }


    /**
     * Mirrors an 8x8 bitboard vertically.
     *
     * @param bitboard      Bitboard
     * @return              New bitboard
     */
    public static final long mirrorX(long bitboard) {
        return Long.reverseBytes(bitboard);
    }


    /**
     * Mirrors an 8x8 bitboard horizontally.
     *
     * @param bitboard      Bitboard
     * @return              New bitboard
     */
    public static final long mirrorY(long bitboard) {
        final long K1 = 0x5555555555555555L;
        final long K2 = 0x3333333333333333L;
        final long K4 = 0x0f0f0f0f0f0f0f0fL;

        bitboard = ((bitboard >> 1) & K1) +  2 * (bitboard & K1);
        bitboard = ((bitboard >> 2) & K2) +  4 * (bitboard & K2);
        bitboard = ((bitboard >> 4) & K4) + 16 * (bitboard & K4);

        return bitboard;
    }


    /**
     * Transpose an 8x8 bitboard along the secondary diagonal.
     *
     * @param bitboard      Bitboard
     * @return              New bitboard
     */
    public static final long transposeYX(long bitboard) {
        return rotate(transposeXY(bitboard));
    }


    /**
     * Transpose an 8x8 bitboard along the main diagonal.
     *
     * @param bitboard      Bitboard
     * @return              New bitboard
     */
    public static final long transposeXY(long bitboard) {
        final long K1 = 0x00AA00AA00AA00AAL;
        final long K2 = 0x0000CCCC0000CCCCL;
        final long K4 = 0x00000000F0F0F0F0L;

        final long T1 = K1 & (bitboard ^ (bitboard >>> 7));
        bitboard = bitboard ^ T1 ^ (T1 << 7);

        final long T2 = K2 & (bitboard ^ (bitboard >>> 14));
        bitboard = bitboard ^ T2 ^ (T2 << 14);

        final long T4 = K4 & (bitboard ^ (bitboard >>> 28));
        bitboard = bitboard ^ T4 ^ (T4 << 28);

        return bitboard;
    }
}
