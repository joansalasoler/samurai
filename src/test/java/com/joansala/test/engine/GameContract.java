package com.joansala.test.engine;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.joansala.engine.Board;
import com.joansala.engine.Game;
import com.joansala.util.suites.Suite;


/**
 *
 */
@DisplayName("Game interface contract")
public interface GameContract {

    /**
     * Instantiate a new game object.
     */
    Game newInstance();


    @Test()
    @DisplayName("moves can be iterated in random order")
    default void SameMoveReturnedAfterSetCursor() {
        List<Move> nodes = new LinkedList<>();

        Game game = newInstance();
        int cursor = game.getCursor();
        int cmove = Game.NULL_MOVE;

        while ((cmove = game.nextMove()) != Game.NULL_MOVE) {
            nodes.add(new Move(cmove, cursor));
            cursor = game.getCursor();
        }

        Collections.shuffle(nodes, new Random(1));

        for (Move node : nodes) {
            game.setCursor(node.cursor);
            assertEquals(node.move, game.nextMove());
        }
    }


    @Test
    @DisplayName("unmake move preserves game state")
    default void UnmakeMovePreservesGameState() {
        Game game = newInstance();
        int originalLength = game.length();
        int originalTurn = game.turn();
        long originalHash = game.hash();
        int[] legalMoves = game.legalMoves();

        if (legalMoves.length > 0) {
            game.makeMove(legalMoves[0]);
            game.unmakeMove();
            assertEquals(originalLength, game.length());
            assertEquals(originalTurn, game.turn());
            assertEquals(originalHash, game.hash());
        }
    }


    @Test
    @DisplayName("winner returns valid values")
    default void WinnerReturnsValidValues() {
        Game game = newInstance();
        int winner = game.winner();
        assertTrue(winner == Game.SOUTH || winner == Game.NORTH || winner == Game.DRAW);
    }


    @Test
    @DisplayName("score and outcome are within infinity bounds")
    default void ScoreAndOutcomeAreWithinInfinityBounds() {
        Game game = newInstance();
        int infinity = game.infinity();
        int score = game.score();
        int outcome = game.outcome();

        assertTrue(Math.abs(score) <= infinity,
            () -> "Score " + Math.abs(score) + " must be ≤ " + infinity);

        assertTrue(Math.abs(outcome) <= infinity,
            () -> "Outcome " + Math.abs(score) + " must be ≤ " + infinity);
    }


    @Test
    @DisplayName("legalMoves returns non-null array")
    default void LegalMovesReturnsNonNullArray() {
        Game game = newInstance();
        int[] moves = game.legalMoves();
        assertNotNull(moves);
    }


    @Test
    @DisplayName("isLegal validates all legal moves")
    default void IsLegalValidatesAllLegalMoves() {
        Game game = newInstance();
        int[] legalMoves = game.legalMoves();

        for (int move : legalMoves) {
            assertTrue(game.isLegal(move));
        }
    }


    @Test
    @DisplayName("cast returns same instance")
    default void CastReturnsSameInstance() {
        Game game = newInstance();
        assertSame(game, game.cast());
    }


    /**
     * Encapsulates information about a move.
     */
    public class Move {
        public int move;
        public int cursor;

        public Move(int move, int cursor) {
            this.move = move;
            this.cursor = cursor;
        }
    }
}
