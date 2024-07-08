package com.joansala.engine;

import com.joansala.engine.base.BaseGame;
import com.joansala.except.GameEngineException;

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

/**
 * Represents the core logic of a zero-sum two-player board game.
 *
 * This interface is used by search algorithms like Negamax, UCT, and
 * Monte-Carlo Tree Search to decide the best move to perform on a given
 * position. It defines a state machine for managing the game state and
 * legality of moves. Adding moves to the game transitions the game state.
 * Calling `makeMove` updates the internal state to reflect the new move,
 * and `unmakeMove` reverts the state to the previous position.
 *
 * An implementation of this interface stores the performed moves and
 * provides methods for generating legal moves, evaluating the current
 * game state, and accessing the board representation. You can think of
 * it as a collection or a stack of moves performed on an initial board
 * state.
 *
 * ## Key Principles:
 *
 * - **State Management:** This interface treats the game as a state
 *   machine. `makeMove` transitions the state forward, and `unmakeMove`
 *   transitions it backward. This allows for efficient exploration of
 *   the game tree.
 * - **Move Representation:** Moves are represented by integer
 *   identifiers. The specific meaning of these identifiers depends on
 *   the game implementation. For Tic-Tac-Toe it may be a coordinate
 *   index on a unidimensional array representing a 3x3 grid.
 * - **Legal Move Iteration:** The game interface provides mechanisms to
 *   efficiently iterate through legal moves for the current player at
 *   each game state. Legal moves can be generated in stages, which can
 *   improve performance by avoiding the generation of all possible
 *   moves at once.
 * - **Utility Function:** The `score` and `outcome` methods provide
 *   approximate and exact evaluations of the current game state,
 *   respectively. Both functions are evaluated from the perspective of
 *   the first player to move (e.g. White in chess).
 *
 * ## Implementing a new Game:
 *
 * Here's a step-by-step guide on how to implement a new game engine
 * using this interface:
 *
 * 1. **Game Representation:** Choose a suitable internal representation
 *    (e.g., bitboards or arrays) and implement the `Board` interface for
 *    your game. Define constants and helper methods.
 *
 * 2. **Game Logic Implementation:** Implement the `Game` interface for
 *    your specific game.
 *
 *    - For most games, consider inheriting from the {@link BaseGame}
 *      abstract class. This class provides a standard implementation of
 *      common methods.
 *    - Implement the constructor to initialize the game state (starting
 *      position and turn) and allocate resources. Your {@code Game}
 *      implementation must be able to store as many moves as are
 *      possible in a match. If your game requires it, initialize the
 *      data structures to a convenient size that allows the engine to
 *      add and remove moves efficiently and then resize it when the
 *      engine invokes the method {@link #ensureCapacity(int)}.
 *    - Implement the methods {@link #makeMove(int)} and {@link #unmakeMove()}
 *      to update and revert the internal state based after a player's move.
 *    - Implement other methods like {@link #isLegal(int)}, {@link #hasEnded()},
 *      {@link #winner()}, and utility methods as needed by your specific
 *      game. If you are inheriting from {@link BaseGame}, you usually do
 *      not need to implement these methods from scratch.
 *
 * 3. **Legal Move Generation:** A good legal moves generator is
 *    crucial for an AI engine to explore the game tree and find the
 *    best move efficiently.
 *
 *    - Implement the method {@link #nextMove()} to iterate through
 *      legal moves for the current player and game state. Consider
 *      generating legal moves in stages and caching them if required.
 *    - To efficiently resume legal move generation after undoing a move,
 *      implement the following methods: {@link #getCursor()} and
 *      {@link #setCursor(int)}.
 *    - Implement the method {@link #legalMoves()}.
 *
 * 4. **Heuristic Evaluation:** A strong engine's heuristic evaluation
 *    must be both accurate and efficient, guiding the AI towards good
 *    moves quickly.
 *
 *    - Implement the methods {@link #score()} and {@link #outcome()}.
 *      Where {@link #score()} must return a value that judges how
 *      "good" a position is for a player.
 *
 * 5. **Game State Hashing:** Hashing the game state can significantly
 *    improve performance by allowing the engine to recognize and avoid
 *    redundant calculations.
 *
 *    - Implement the method {@link #hash()} or {@link #computeHash()}
 *      if you are inheriting from {@link BaseGame}. Consider using
 *      utility classes from `com.joansala.util.hash` package like
 *      ZobristHash, BinomialHash, or LehmerHash.
 *    - In your {@link #makeMove(int)} implementation, you may call
 *      {@link #hash()} to update the hash value after making the move.
 *      When {@link #unmakeMove()} is called, retrieve the hash value
 *      from a cache.
 *    - If you're using Zobrist Hashing, it's more efficient to compute
 *      the hash value incrementally within {@link #makeMove(int)}
 *      instead of calling {@link #hash()} after each invocation.
 *
 * ## Example: TicTacToeGame Implementation
 *
 * Refer to a concrete example like `TicTacToeGame` to see how to implement
 * the `Game` interface for a specific game. This example demonstrates
 * how to define move identifiers, manage the game state, perform move
 * generation, and evaluate the game state.
 */
public interface Game {

    /** Identifier for a drawn game */
    int DRAW = 0;

    /** Identifier for the first player to move (e.g., White in Chess) */
    int SOUTH = 1;

    /** Identifier for the second player to move (e.g., Black in Chess) */
    int NORTH = -SOUTH;

    /** Identifier for an invalid or non-existent move */
    int NULL_MOVE = -1;

    /** Default score assigned to a drawn game */
    int DRAW_SCORE = 0;


    /**
     * This is a convenience method intended to be used by wrapping
     * classes to perform type casting when needed. It should simply
     * return {@code this}.
     */
    Game cast();


    /**
     * Returns the total number of moves played in the current game.
     *
     * @return  The number of moves performed
     */
    int length();


    /**
     * Returns an array containing all the moves played in the current
     * game, or {@code null} if no moves have been made yet.
     *
     * @return  The moves performed for this game or {@code null} if
     *          no moves have been performed
     */
    int[] moves();


    /**
     * Returns the current player whose turn it is to move.
     *
     * For two-player games it will either be {@code SOUTH} or
     * {@code NORTH}.
     *
     * @return  Identifier of the player whose turn it is
     */
    int turn();


    /**
     * Returns the initial game state ({@code Board}) before any moves
     * were played.
     *
     * To get a board representing the current state of the game after
     * some moves where added to it with {@link #makeMove(int)} call
     * {@link #toBoard()} instead.
     *
     * @return Immutable representation of the game state before any move.
     */
    Board getBoard();


    /**
     * Returns a representation of the current game state ({@code Board}).
     *
     * To get a board representing the initial state of the game before
     * any moves where added to it call {@link #getBoard()} instead.
     *
     * @return A board instance representing the current game state.
     */
    Board toBoard();


    /**
     * Sets a new starting state ({@code Board}).
     *
     * Calling this method resets all game state information and history,
     * including played moves and the legal move generation state.
     *
     * @param board The new starting board.
     * @throws GameEngineException If the provided board is not valid for the game.
     */
    void setBoard(Board board);


    /**
     * This method is used for games where the final board state might
     * differ from the board during gameplay (e.g. Oware). This method
     * calculates and sets the final board for a match that ends on the
     * current position. For most games it should do nothing.
     */
    void endMatch();


    /**
     * Checks if the current game position represents an end state (win,
     * loss, or draw).
     *
     * @return  {@code true} if the game ended or
     *          {@code false} otherwise
     */
    boolean hasEnded();


    /**
     * Returns the identifier of the player who won the game based on
     * the current position.
     *
     * For two-player games it will return {@code SOUTH} if the first
     * player to move won, {@code NORTH} if the second player to move
     * won, or {@code DRAW} if the game is a draw or hasn't ended yet.
     *
     * @return  Game result identifier
     */
    int winner();


    /**
     * Provides an estimate (heuristic) of the current position's value.
     *
     * This score should be between the minimum and maximum values
     * returned by the {@link #outcome()} method. Positive values indicate
     * an advantage for the first player, negative values for the second
     * player. It should reflect how close the first player to move is to
     * winning the game, but it should never return an exact score.
     *
     * @see     Game#outcome
     * @return  The heuristic evaluation value
     */
    int score();


    /**
     * Provides a definitive evaluation of the current position,
     * considering it as an end state.
     *
     * Similar to {@link #score()}, the score is calculated from the
     * first player to move perspective. It should return the maximum
     * possible score if the first player wins, the minimum possible
     * score if the other player wins, or {@code DRAW_SCORE} if the game
     * is a draw or hasn't ended yet.
     *
     * This method can return a score different from the maximum possible
     * score to push the engine away from certain choices (like branches
     * that end in move repetitions). However, this also means the
     * engine will need to consider more options to find the best one.
     *
     * @see     Game#score
     * @see     Game#infinity
     * @return  The utility evaluation value
     */
    int outcome();


    /**
     * This method returns the score assigned to a drawn game.
     *
     * A positive contempt means that the engine will try to avoid draws
     * even if it means playing weaker moves, while a negative contempt
     * means that the algorithm will be more willing to accept a draw.
     *
     * @see     Game#score
     * @see     Game#outcome
     * @return  Contempt evaluation value
     */
    int contempt();


    /**
     * Maximum score to which a position can be evaluated.
     *
     * This is usually the maximum obtainable score, the same that is
     * returned by the {@link #outcome} method. Most search engines
     * will stop searching a branch if the evaluation score is equal or
     * greater than this value, but in certain situations they may use
     * a different value (e.g. when draw search is enabled on UCI).
     *
     * @see     Game#score
     * @see     Game#outcome
     * @return  Infinity evaluation value
     */
    int infinity();


    /**
     * Hash code for the current game position, used for indexing and
     * faster comparisons between positions
     *
     * It is crucial for the hash code of two distinct positions to be
     * unique or near-unique to avoid collisions, and for two equal
     * positions to always return the same hash code, even between
     * different game sessions.
     *
     * Consider using utility classes from `com.joansala.util.hash`
     * package (e.g., ZobristHash) for efficient hashing implementations.
     *
     * @return  The hash code for the current position
     */
    long hash();


    /**
     * Checks if a specific move is legal according to the game's rules
     * for the current game state.
     *
     * @see Game#makeMove(int)
     * @param move  A move identifier
     * @return      {@code true} if the move is legal
     */
    boolean isLegal(int move);


    /**
     * Performs the specified move on the internal game board.
     *
     * This method must be as efficient as possible and should not check
     * the legality of the move or assert that the game object has enough
     * capacity to store the move.
     *
     * @see Game#unmakeMove()
     * @param move  The move to perform on the internal board
     */
    void makeMove(int move);


    /**
     * Reverts the game state and legal move iterator to the state
     * before the previous {@link #makeMove(int)} call.
     *
     * This method efficiently undoes the most recent move, restoring
     * the game board and the internal state used for iterating through
     * legal moves. It's crucial for maintaining search efficiency as
     * this method is frequently called.
     *
     * **Important:** After calling `unmakeMove()`, subsequent calls to
     * {@link #nextMove()} should resume iterating legal moves from the
     * position before the undone move. This ensures the engine continues
     * searching from where it left off. The legal moves iterator should
     * not be reset to the beginning.
     *
     * @see Game#makeMove(int)
     * @see Game#nextMove()
     */
    void unmakeMove();


    /**
     * Undoes a specified number of moves from the current game state.
     *
     * @see Game#unmakeMove()
     * @param length Number of moves to undo
     */
    void unmakeMoves(int length);


    /**
     * Retrieves the next legal move for the current player's turn.
     *
     * This method iterates through legal moves based on the internal
     * game state, returning them one by one until {@link NULL_MOVE} is
     * returned, indicating no more moves are available. A good move
     * ordering significantly improves engine performance by prioritizing
     * promising moves first.
     *
     * This method is designed for efficient retrieval of legal moves
     * without the need to generate all moves at once, which can be
     * expensive. However, it's important to maintain the internal state
     * of the legal move iterator.
     *
     * Specifically: Calling {@link #unmakeMove()} resets the iterator
     * to the state it had before the previous {@link #makeMove(int)}
     * call. This ensures consistent behavior when retrieving legal moves
     * after undoing moves.
     *
     * @see Game#legalMoves
     * @see Game#unmakeMove()
     * @return A legal move identifier or {@link NULL_MOVE} if no more
     *         moves can be returned.
     */
    int nextMove();


    /**
     * Retrieves an array containing all legal moves that can be played
     * from the current game state.
     *
     * This method provides a snapshot of all legal moves without
     * altering the game state or the internal iterator used for
     * {@link #nextMove()}. A good move ordering significantly improves
     * engine performance by prioritizing promising moves first.
     *
     * **Important:** If you use {@link #nextMove()} to populate the
     * returned array, ensure you reset the legal moves iterator
     * (using {@link #setCursor(int)}) to its original position before
     * this method was called. This maintains consistent behavior for
     * subsequent calls to {@link #nextMove()}.
     *
     * @see Game#nextMove
     * @see Game#setCursor
     * @return A new array containing all legal moves.
     */
    int[] legalMoves();


    /**
     * Retrieves the current value of the move generation cursor.
     *
     * The move generation cursor is used to track the iteration through
     * legal moves. This method allows access to the current iteration
     * position within the legal move iteration process.
     *
     * @see Game#setCursor
     * @see Game#nextMove
     * @see Game#legalMoves
     *
     * @return  Current iteration cursor value
     */
    int getCursor();


    /**
     * Sets the move generation cursor to a new specified value.
     *
     * This allows control over the starting position for iterating
     * through legal moves using {@link #nextMove()}. It's important to
     * reset the cursor to the state it had before the corresponding
     * {@link #makeMove(int)} call to maintain proper iteration behavior.
     *
     * @see Game#setCursor
     * @see Game#nextMove
     * @see Game#legalMoves
     *
     * @param   New iteration cursor value
     */
    void setCursor(int cursor);


    /**
     * Converts an evaluation score to a domain-specific unit
     * (e.g., centipawns in Chess).
     *
     * This convenience method transforms the internal evaluation score
     * to a format suitable for external communication protocols
     * (e.g., UCI protocol for Chess). For Chess, centipawns (1/100th of
     * a pawn) are commonly used.
     *
     * The specific unit conversion depends on the game being implemented.
     * An implementation may choose to return the raw score unchanged if
     * no standard unit exists for the game. However, converting the
     * score to a human-understandable unit is generally recommended
     * for better readability. For example, Oware might convert the
     * score to 1/100th of a seed.
     *
     * @param score The evaluation score.
     * @return The score converted to the domain-specific unit.
     */
    int toCentiPawns(int score);


    /**
     * Increases the capacity of the game object if necessary.
     *
     * Calling this method ensures the game object can store at least
     * the specified number of game states. This method is called
     * frequently, so any implementation should be efficient and only
     * increase capacity when strictly necessary. It's a good idea to
     * start with a capacity of twice the search depth the engine can
     * reach and double it only when strictly needed.
     *
     * @see Game#makeMove
     * @param minCapacity Capacity specified in number of moves.
     * @throws IllegalArgumentException If {@code minCapacity} is above
     *      the maximum possible capacity for the object.
     */
    void ensureCapacity(int minCapacity);

}
