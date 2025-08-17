package com.joansala.engine;

/*
 * Copyright (C) 2014-2025 Joan Sala Soler <contact@joansala.com>
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

import java.util.function.Consumer;


/**
 * Represents an algorithm that computes the best move or score for
 * a given game state.
 *
 * An {@code Engine} is responsible for analyzing a game position and
 * determining the optimal move for the current player. It provides the
 * core search functionality used by AI "brains" like {@link UCIBrain}.
 *
 * The engine operates on {@link Game} objects, which provide the game
 * state and legal move generation capabilities. It returns move
 * identifiers that correspond to the internal representation used by
 * the specific game implementation.
 *
 * ## Key Principles:
 *
 * - **Algorithm Agnostic:** This interface can be implemented by any
 *   search algorithm. Whether using traditional minimax-based approaches
 *   or modern Monte Carlo methods, all engines share the same contract.
 * - **Configurable Search:** Engines support various search parameters
 *   including time and depth limits, and evaluation tuning.
 * - **Real-time Feedback:** Through the consumer pattern, engines can
 *   provide live search statistics and progress updates.
 * - **Tournament Ready:** Features like pondering, match management, and
 *   UCI protocol compatibility make engines suitable for competitive play.
 *
 * ## Engine Responsibilities:
 *
 * Engines should focus on pure move computation and analysis:
 *
 * - **Position Analysis:** Evaluate positions and compute optimal moves
 * - **Search Management:** Respect time/depth limits and handle interruptions
 * - **State Preservation:** Maintain search data between calls for
 *   efficiency, especially during pondering scenarios
 * - **Progress Reporting:** Provide search statistics to consumers
 * - **Endgame Knowledge:** May use endgame tablebases during search
 *
 * Engines should NOT handle:
 *
 * - Opening book lookups (managed by callers like {@link UCIBrain})
 * - Automatic pondering (managed by callers like {@link UCIBrain})
 * - Printing search information (send {@link Report} instead)
 * - Game-specific logic (use {@link Game} methods instead)
 * - Thread management beyond search abortion
 *
 * ## Search Parameters:
 *
 * - **Depth:** For most algorithms (Negamax, Alpha-Beta), this represents
 *   the maximum number of plies the algorithm is allowed to search.
 * - **Move Time:** Maximum time allowed for move computation, enabling
 *   time-controlled games and responsive user interfaces.
 * - **Contempt Factor:** Adjusts the evaluation of drawn positions,
 *   allowing engines to play more aggressively or defensively based on
 *   the game context.
 * - **Infinity Score:** Defines the bounds for minimax-style evaluations,
 *   improving search efficiency when set to the maximum possible game
 *   score. See {@link Game#infinity} for details.
 *
 * ## Implementing a new Engine:
 *
 * Here's a step-by-step guide for implementing an engine:
 *
 * 1. **Engine Implementation:** For most engines, consider inheriting
 *    from the {@link BaseEngine} abstract class. This class provides a
 *    standard implementation of common methods including parameter
 *    management, consumer handling, and search abortion mechanisms.
 *
 * 2. **Choose Search Algorithm:** Decide on the search algorithm (Negamax,
 *    MCTS, etc.) and understand how it maps to the interface parameters.
 *
 * 3. **Core Search Implementation:** Implement {@link #computeBestMove(Game)}
 *    as your main search routine. This method should:
 *
 *    - Respect the configured time and depth limits
 *    - Return {@code Game.NULL_MOVE} for terminal positions
 *    - Handle search abortion gracefully using {@link BaseEngine#aborted()}
 *
 * 4. **Parameter Handling:** If inheriting from {@link BaseEngine}, the
 *    getters and setters for search parameters are already implemented.
 *    Focus on interpreting them appropriately for your algorithm.
 *
 * 5. **Asynchronous Control:** {@link BaseEngine} provides built-in
 *    support for {@link #abortComputation()} and timed search abortion.
 *    Use the {@link BaseEngine#aborted()} method to check for abortion
 *    during your search loops.
 *
 * 6. **Match Management:** {@link BaseEngine} handles basic match setup.
 *    Override {@link #newMatch()} if you need to reset algorithm-specific
 *    state like transposition tables or learning parameters.
 *
 * 7. **Progress Reporting:** External classes (like UCI protocol
 *    implementations) attach consumers to the engine using
 *    {@link #attachConsumer(Consumer)}. The engine is responsible for
 *    creating {@link Report} objects and invoking attached consumers
 *    at regular intervals or when important search information changes.
 *
 * ## Example Usage:
 *
 * ```java
 * Engine engine = new Negamax();
 * engine.setMoveTime(5000); // Maximum search time limit
 * engine.setDepth(8);       // Maximum recursion depth
 * engine.setContempt(10);   // Slight draw aversion
 *
 * Game game = new ChessGame();
 * int bestMove = engine.computeBestMove(game);
 * ```
 *
 * ## Game State Management During Search:
 *
 * Engines should use {@link Game#makeMove(int)} and {@link Game#unmakeMove()}
 * to efficiently navigate the game tree during search. Game implementations
 * maintain history of board states and moves, making this approach much
 * more efficient than creating new {@link Game} objects for each position.
 *
 * It is important to always restore the original game state before
 * returning. Every {@link Game#makeMove(int)} must be paired with
 * {@link Game#unmakeMove()} or {@link Game#unmakeMoves(int)}. This ensures
 * callers get back the exact same game state they passed in. Please note
 * although that while the board position is restored, the move generation
 * cursor remains at its current position after {@link Game#unmakeMove()}.
 *
 * ## Example Implementations
 *
 * Refer to concrete examples that inherit from {@link BaseEngine} to see
 * how to implement specific search algorithms. These examples demonstrate
 * how to focus on the core search logic while leveraging the base class
 * for common functionality. The most simple amongst them being the
 * {@link Mindless} engine, that chooses moves at random.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public interface Engine {

    /** Default depth limit per move */
    int DEFAULT_DEPTH = 127;

    /** Default time limit per move */
    long DEFAULT_MOVETIME = 3600;


    /**
     * Returns the maximum search depth limit.
     *
     * @return The current depth limit
     * @see #setDepth(int)
     */
    int getDepth();


    /**
     * Returns the maximum time allowed for move computation.
     *
     * @return The current time limit in milliseconds
     * @see #setMoveTime(long)
     */
    long getMoveTime();


    /**
     * Returns the current contempt factor.
     *
     * @return The current contempt factor
     * @see #setContempt(int)
     */
    int getContempt();


    /**
     * Returns the current infinity score.
     *
     * @return The current infinity score
     * @see #setInfinity(int)
     */
    int getInfinity();


    /**
     * Returns the move this engine would like to ponder on.
     *
     * Pondering allows the engine to think during the opponent's turn
     * by predicting the most likely next move. This is a common feature
     * in game engines that can significantly improve playing strength
     * in time-controlled games.
     *
     * When pondering is enabled, brain implementations like {@link UCIBrain}
     * may apply this move to the game state and then invoke the method
     * {@link #computeBestMove(Game)} to search the resulting position.
     * However, the brain may choose to ponder on a different move instead
     * (e.g., a move from the openings book).
     *
     * @param game The current game state
     * @return The predicted opponent move to ponder on, or
     *         {@code Game.NULL_MOVE} if no prediction is available
     */
    int getPonderMove(Game game);


    /**
     * Sets the maximum search depth for subsequent computations.
     *
     * For most algorithms, this represents the maximum number of plies
     * (half-moves) to search ahead. This depth limit should never be
     * exceeded during search, as exceeding it may cause the {@link Game}
     * to exceed its allocated capacity and throw an exception.
     *
     * @param depth The new depth limit (must be positive)
     * @see Game#ensureCapacity(int)
     */
    void setDepth(int depth);


    /**
     * Sets the maximum search time for subsequent computations.
     *
     * This time limit is used to ensure responsive gameplay and is
     * compatible with the UCI protocol's 'movetime' parameter. The
     * engine should make its best effort to return a result within this
     * time frame, though brief overruns may occur during search completion.
     *
     * @param delay The new time limit in milliseconds (must be positive)
     */
    void setMoveTime(long delay);


    /**
     * Sets the contempt factor for draw evaluation.
     *
     * The contempt factor adjusts how the engine evaluates drawn positions.
     * A positive contempt makes the engine avoid draws (playing more
     * aggressively), while a negative contempt makes it seek draws when
     * behind. This is particularly useful in tournament play where the
     * engine's rating relative to its opponent matters.
     *
     * The contempt score must always be within the range ±infinity for
     * proper engine operation.
     *
     * @param score The contempt factor for draw positions
     * @see #setInfinity(int)
     */
    void setContempt(int score);


    /**
     * Sets the infinity score for evaluation bounds.
     *
     * The infinity score represents the maximum evaluation score that
     * the game can produce. Setting this correctly helps minimax-based
     * algorithms distinguish between mate scores and regular position
     * evaluations, improving search efficiency.
     *
     * @param score The infinity value as a positive integer
     */
    void setInfinity(int score);


    /**
     * Sets which player the engine is playing as.
     *
     * This method informs the engine of its player identity for the
     * current match, which may affect evaluation perspectives, or other
     * player-specific optimizations. This does not modify the {@link Game}
     * state or change whose turn it is to move.
     *
     * @param turn The player identifier ({@code Game.SOUTH} or
     *             {@code Game.NORTH})
     */
    void setTurn(int turn);


    /**
     * Attaches a progress consumer to the engine.
     *
     * Consumers receive real-time updates about the search progress,
     * including statistics like nodes searched, current depth, best move,
     * and evaluation scores. This enables live analysis displays and
     * progress monitoring without coupling the engine to specific UI
     * implementations.
     *
     * Consumers are guaranteed to run to completion before the engine
     * continues with normal operations, ensuring thread safety for the
     * reporting mechanism.
     *
     * @param consumer The progress consumer to attach
     * @see #computeBestMove(Game)
     */
    void attachConsumer(Consumer<Report> consumer);


    /**
     * Detaches a progress consumer from the engine.
     *
     * Removes a previously attached consumer, stopping it from receiving
     * further search progress updates.
     *
     * @param consumer The progress consumer to detach
     */
    void detachConsumer(Consumer<Report> consumer);


    /**
     * Signals the start of a new match.
     *
     * This method informs the engine that subsequent positions will be
     * from a different game/match, allowing it to reset match-specific
     * state such as:
     *
     * - Transposition table entries
     * - Learning algorithm parameters
     * - Time management settings
     *
     * This method should be called before starting each new game to
     * ensure optimal engine performance.
     */
    void newMatch();


    /**
     * Computes the best achievable score for the current game state.
     *
     * This method performs a search to determine the theoretical best
     * score obtainable from the given position, assuming optimal play
     * from both sides. The score is evaluated from the perspective of
     * the player to move ({@code Game.SOUTH} or {@code Game.NORTH}).
     *
     * Most implementations will call {@link #computeBestMove(Game)}
     * internally and return the evaluation score of the best move found,
     * rather than the move itself. This method is used for position
     * analysis when only the score matters, not the actual move to play.
     *
     * @param game The game state to evaluate
     * @return The best achievable score for the current position
     * @see #computeBestMove(Game)
     */
    int computeBestScore(Game game);


    /**
     * Computes the best move for the current game position.
     *
     * This is the primary method of the engine, performing a search
     * to find the optimal move for the current player. The search
     * respects the configured time and depth limits and can be
     * interrupted using {@link #abortComputation()}.
     *
     * This method may be called repeatedly for the same or related
     * positions (e.g., during pondering), so engines should preserve
     * useful search information between calls for optimal performance.
     *
     * Engines should send progress reports to attached consumers multiple
     * times during execution to provide real-time search feedback.
     *
     * @param game The game state to analyze (should not be modified)
     * @return The best move found, or {@code Game.NULL_MOVE} if the
     *         game has already ended or no legal moves are available
     */
    int computeBestMove(Game game);


    /**
     * Immediately aborts the current search operation.
     *
     * This method forces any ongoing move computation to terminate
     * and return the best result found so far. This is essential for
     * responsive user interfaces and time-controlled games.
     *
     * @see #computeBestMove(Game)
     * @see #abortComputation(long)
     */
    void abortComputation();


    /**
     * Schedules the current search to abort after a specified delay.
     *
     * This method allows for graceful search termination, giving the
     * engine time to complete its current iteration or reach a stable
     * search state before stopping.
     *
     * @param delay The delay in milliseconds before aborting the search
     * @see #abortComputation()
     * @see #setMoveTime(long)
     */
    void abortComputation(long delay);
}
