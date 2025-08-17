package com.joansala.engine;

import com.joansala.engine.base.BaseBoard;
import com.joansala.except.GameEngineException;

/*
 * Samurai framework.
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


/**
 * Represents the immutable state of a game at a specific point in time.
 *
 * A {@code Board} object encapsulates the current state of a game. It
 * holds information such as the player whose turn it is to move, the
 * location of all game pieces, and any other data relevant to the gameplay.
 * It also provides methods for converting the game state between different
 * representations commonly used for communication and storage.
 *
 * For example, a chess {@code Board} might store the positions of the
 * pieces, the current player, the possibility of an en passant capture,
 * and the number of moves performed since the last pawn move or capture
 * (used for the fifty-move rule). It could also offer methods to convert
 * the board state into a Forsyth-Edwards Notation (FEN) string (used for
 * diagrams, see {@link #toDiagram()}) and Algebraic Notation (used for
 * move sequences, see {@link #toNotation(int[])}).
 *
 * Refer to the Universal Chess Interface (UCI) specification
 * (https://www.chessprogramming.org/UCI) for details on how to encode
 * your game state using standard notations and how these representations
 * are used for communication between game engines and user interfaces.
 *
 * ## Key Principles:
 *
 * - **Immutability:**  This interface enforces immutability for all boards.
 *   Methods returning state data must either return copies of the data or
 *   immutable objects.
 * - **Internal Representation:**  Internally, the game state should be
 *   stored in a fast and compact data structure suitable for efficient
 *   manipulation by your {@link Game} implementation (e.g., bitboards
 *   for Tic-Tac-Toe or Chess).
 * - **Standard Notations:**  Methods are provided for converting the game
 *   internal representations to and from standard notations used for
 *   communication (e.g., FEN, Algebraic Notation). These conversion
 *   methods enable interoperability with communication protocols like
 *   the built-in Universal Chess Inteface (UCI).
 *
 * ## Implementing a new game Board
 *
 * The {@code Board} interface provides a flexible framework for
 * implementing various games. Here's a step-by-step guide on how to
 * create a new game using this interface:
 *
 * 1. **Game Representation:** Define constants and helper methods to
 *    represent the game state.
 *
 *    - Define constants for the number of players, board dimensions,
 *      piece types, etc. Store these constants in a separate final class
 *      for better organization (refer to {@link TicTacToe} for an example).
 *    - Create helper methods to convert between different representations
 *      of the game state, such as converting from a 2D array representing
 *      the board to a single data structure suitable for storing the
 *      complete game state (e.g., a bitboard for Tic-Tac-Toe).
 *    - Consider using utility classes from `com.joansala.util` package
 *      (like `BitsetConverter`, `CoordinateConverter` or `DiagramConverter`)
 *      for common operations on game state representations.
 *
 * 2. **Board Implementation:** Implement the {@code Board} interface for
 *    your specific game.
 *
 *    - For most games, consider inheriting from the {@link BaseBoard}
 *      abstract class. This class provides a standard implementation of
 *      common methods.
 *    - The constructor should initialize the board with the starting game
 *      state (standard start position and turn; for Tic-Tac-Toe it's an
 *      empty board).
 *    - Implement methods like {@link #parseCoordinates(String)} and
 *      {@link #toCoordinates(int)} to handle move conversions.
 *    - Implement methods like {@link #fromDiagram(String)} and
 *      {@link #toDiagram()} to convert between external notations (like
 *      FEN) and your internal game state representation.
 *    - Override the {@link #toString()} method to provide a human-readable
 *      representation of the board state suitable for displaying on the
 *      console.
 *
 * ## Example: TicTacToeBoard Implementation
 *
 * Refer to the {@link TicTacToeBoard} class for a concrete example of
 * implementing the {@code Board} interface for Tic-Tac-Toe. It demonstrates
 * how to define constants for the game state, handle move conversions, and
 * create a human-readable string representation of the board. Tic-Tac-Toe
 * uses bitboards for a compact representation, but you may use arrays or
 * other data structures as well.
 */
public interface Board {

    /**
     * Gets the player whose turn it is to move.
     *
     * @return The player identifier.
     */
    int turn();


    /** Use {@link #fromDiagram(String)}. */
    @Deprecated
    default Board toBoard(String diagram) {
        return fromDiagram(diagram);
    }


    /** Use {@link #parseCoordinates(String)}. */
    @Deprecated
    default int toMove(String coordinates) {
        return parseCoordinates(coordinates);
    }


    /** Use {@link #parseNotation(String)}. */
    @Deprecated
    default int[] toMoves(String notation) {
        return parseNotation(notation);
    }


    /**
     * Generates a compact string representation of the current board
     * state in a standard notation.
     *
     * This method adheres to a notation format commonly used for
     * communication between game engines and user interfaces (e.g., FEN
     * for Chess). This string representation can be used to reconstruct
     * an identical board state using {@link #fromDiagram(String)}.
     *
     * It is used, for example, by the Universal Chess Interface (UCI)
     * implementation to communicate the state of an ongoing match to an
     * engine. In chess, this method might return a Forsyth-Edwards
     * Notation (FEN) string that encodes the piece placement, player
     * turn, castling rights, and en-passant target square.
     *
     * @return The diagram string representation of the board state.
     */
    String toDiagram();


    /**
     * Creates a new board instance from a diagram string.
     *
     * This method takes a diagram string representation of a board and
     * returns a new board instance that reflects the exact state encoded
     * in the string (see {@link #toDiagram()}).
     *
     * @param diagram The diagram string representation of a board.
     * @return A new board instance representing the provided diagram.
     * @throws GameEngineException If the diagram string is invalid.
     */
    Board fromDiagram(String diagram);


    /**
     * Converts a move identifier to its coordinates.
     *
     * This method transforms the game's internal numeric move representation
     * into a standardized string format that describes the move's location
     * or action on the board. This coordinate format is primarily used
     * for communication with external engines via protocols like UCI.
     * Examples include:
     *
     * - Chess: "e2e4" or "Nf3" (algebraic notation)
     * - Tic-Tac-Toe: "a1" or "c3" (grid coordinates)
     * - Checkers: "12-16" (square numbers)
     *
     * Use {@link #parseCoordinates(String)} to convert coordinates back
     * to move identifiers.
     *
     * @param move Internal move identifier (game-specific encoding)
     * @return The move's coordinates in standard notation (e.g., "e4", "a1")
     * @throws GameEngineException If the move identifier is invalid
     */
    String toCoordinates(int move);


    /**
     * Converts a sequence of move identifiers to game notation format.
     *
     * This method transforms an array of internal move identifiers into
     * a complete game notation string that represents the entire move
     * sequence. The output format follows game-specific standards:
     *
     * - Chess: "e4 e5 Nf3 Nc6" (algebraic notation)
     * - Tic-Tac-Toe: "a1 b2 c3" (coordinates)
     * - Checkers: "11-15 23-19 8-11" (dash notation with captures)
     *
     * Unlike {@link #toCoordinates(int)} which converts individual moves,
     * this method may produce different output for the same move depending
     * on the game state. For example, in checkers, multiple jumps in a
     * single turn are represented as one notation entry.
     *
     * This method does not validate move legality or sequence correctness,
     * it only converts valid identifiers to notation.
     *
     * Use {@link #parseNotation(String)} to convert notation back to
     * move arrays.
     *
     * @param moves Array of internal move identifiers
     * @return Complete game notation string (e.g., "1.e4 e5 2.Nf3 Nc6")
     * @throws GameEngineException If any move identifier is invalid
     * @see #toCoordinates(int) For single move coordinate conversion
     */
    String toNotation(int[] moves);


    /**
     * Converts human-readable move coordinates to move identifiers.
     *
     * This method converts standardized coordinate strings into the
     * game's internal numeric move representation. It accepts the same
     * coordinate formats produced by {@link #toCoordinates(int)}:
     *
     * - Chess: "e2e4", "Nf3", "O-O" (algebraic notations)
     * - Tic-Tac-Toe: "a1", "c3" (grid coordinates)
     * - Checkers: "12-16" (square-to-square notation)
     *
     * Use {@link #toCoordinates(int)} to convert move identifiers back
     * to coordinate strings.
     *
     * @param coordinates Move coordinates (e.g., "e4", "a1")
     * @return Internal move identifier corresponding to the coordinates
     * @throws GameEngineException If coordinates format is invalid
     */
    int parseCoordinates(String coordinates);


    /**
     * Converts complete game notation into an array of move identifiers.
     *
     * This method converts a full game notation string into an array of
     * internal move identifiers. It handles game-specific notation formats:
     *
     * - Chess: "e4 e5 Nf3 Nc6" (algebraic notation)
     * - Tic-Tac-Toe: "a1 b2 c3" (grid coordinates)
     * - Checkers: "11-15 23-19 8-11" (move sequences with captures)
     *
     * This method only validates notation syntax, not move legality. Use
     * with a {@link Game} instance to validate actual move sequences.
     *
     * Use {@link #toNotation(int[])} to convert move arrays back to
     * notation strings.
     *
     * @param notation Complete game notation string (e.g., "e4 e5 Nf3")
     * @return Array of internal move identifiers
     * @throws GameEngineException If notation format is invalid
     * @see #parseCoordinates(String) For single move coordinate parsing
     */
    int[] parseNotation(String notation);


    /**
     * Generates a human-readable string representation of the current
     * board state.
     *
     * This method provides a user-friendly representation of the game
     * state suitable for displaying on the console or a graphical user
     * interface. For example, an ASCII drawing of a board.
     *
     * @return A string representing the board state.
     */
    @Override
    String toString();

}