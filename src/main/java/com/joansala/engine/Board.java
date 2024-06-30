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
 * are used for communication between chess engines and user interfaces.
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
 *   state to and from standard notations used for communication and storage
 *   (e.g., FEN for Chess, Algebraic Notation for moves).
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
 *    - Implement methods like {@link #toMove(String)}, {@link #toCoordinates(int)},
 *      to return the encapsulated data and handle move conversions.
 *    - Implement methods like {@link #toBoard(String)} and {@link #toDiagram()}
 *      to convert between external notations (like FEN) and your internal
 *      game state representation.
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


    /**
     * Generates a compact string representation of the current board
     * state in a standard notation.
     *
     * This method adheres to a notation format commonly used for
     * communication between game engines and user interfaces (e.g., FEN
     * for Chess). This string representation can be used to reconstruct
     * an identical board state using the {@link #toBoard(String)} method.
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
    Board toBoard(String diagram);


    /**
     * Converts a move identifier to its coordinate representation.
     *
     * This method takes an internal move identifier and returns a
     * string describing a move performed on the board using a standard
     * notation (e.g., "e4" in Chess). This is used for displaying the
     * move to a human player and for communication with game engines
     * using protocols like UCI.
     *
     * (See {@link #toMove(String)} for the reverse operation).
     *
     * @param move The move identifier.
     * @return The move's coordinate representation.
     * @throws GameEngineException If the move identifier is invalid.
     */
    String toCoordinates(int move);


    /**
     * Converts a sequence of move identifiers to their notation
     * representation.
     *
     * This method takes an array of move identifiers and returns a string
     * describing the sequence of moves played on the board in a standard
     * notation format (e.g., for chess "e4 e5 Nf3 Nc6"). This notation
     * is often used for recording games or communicating moves between
     * players or engines.
     *
     * Notice that you can usually call {@link #toCoordinates} on each move
     * in the sequence, but for some games, such as draughts (checkers),
     * the notation representation may depend on the current state.
     *
     * (See {@link #toMoves(String)} for the reverse operation).
     * (See {@link #toCoordinates} for the conversion of a single move.
     *
     * This method does not validate the sequence of moves.
     *
     * @param moves The array of move identifiers.
     * @return The notation representation of the move sequence
     *        (e.g., for chess "e4 e5 Nf3 Nc6").
     * @throws GameEngineException If any move identifier is invalid.
     */
    String toNotation(int[] moves);


    /**
     * Converts a move coordinate representation to its move identifier.
     *
     * This method takes a string representing the location of a move on
     * the board (often in standard notation format) and returns the
     * corresponding internal move identifier used by the game. This can
     * be useful for processing user input or storing game history.
     *
     * (See {@link #toCoordinates(int)} for the reverse operation).
     *
     * @param coordinate The move's coordinate representation (e.g., "e4" in chess).
     * @return The move identifier.
     * @throws GameEngineException If the coordinate representation is invalid.
     */
    int toMove(String coordinate);


    /**
     * Converts a move sequence notation to an array of move identifiers.
     *
     * This method takes a strings representing a sequence of moves
     * played on the board in standard notation format and returns an
     * array of corresponding move identifiers used internally by the
     * game. This can be useful for replaying games or parsing move
     * sequences from external sources.
     *
     * This method does not validate the sequence of moves.
     *
     * (See {@link #toNotation(int[])} for the reverse operation).
     *
     * @param notation The notation representation of the move sequence
     *                (e.g., "e4 e5 Nf3 Nc6" in chess).
     * @return An array of move identifiers representing the move sequence.
     * @throws GameEngineException If a notation string is invalid.
     */
    int[] toMoves(String notation);


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