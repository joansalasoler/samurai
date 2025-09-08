package com.joansala.test.engine;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

import com.joansala.engine.Board;
import com.joansala.engine.Game;
import com.joansala.util.suites.Suite;


/**
 *
 */
@DisplayName("Board interface contract")
public interface BoardContract {

    /**
     * Instantiate a new board object.
     */
    Board newBoard();


    /**
     * Instantiate a new game object.
     */
    Game newGame();


    @ParameterizedTest()
    @MethodSource("suites")
    @DisplayName("diagram to board peserves class type")
    default void ToBoardIsOfSameClassType(Suite suite) {
        Board instance = newBoard();
        String diagram = suite.diagram();
        Board board = instance.fromDiagram(diagram);
        assertEquals(instance.getClass(), board.getClass());
    }


    @ParameterizedTest()
    @MethodSource("suites")
    @DisplayName("diagram to board is commutative")
    default void ToBoardConversionIsCommutative(Suite suite) {
        Board instance = newBoard();
        String diagram = suite.diagram();
        Board board = instance.fromDiagram(diagram);
        String converted = board.toDiagram();
        assertEquals(diagram, converted);
    }


    @ParameterizedTest()
    @MethodSource("suites")
    @DisplayName("notation to moves is not a blank array")
    default void ToMovesIsNotBlank(Suite suite) {
        Board instance = newBoard();
        String notation = suite.notation();
        int[] moves = instance.parseNotation(notation);
        assertNotNull(moves, "moves array is null");
        assertTrue(moves.length > 0, "moves array is empty");
    }


    @ParameterizedTest()
    @MethodSource("suites")
    @DisplayName("notation to moves is commutative")
    default void ToMovesIsCommutative(Suite suite) {
        Board instance = newBoard();
        String notation = suite.notation();
        int[] moves = instance.parseNotation(notation);
        String converted = instance.toNotation(moves);
        int[] result = instance.parseNotation(converted);
        assertArrayEquals(moves, result);
    }


    @ParameterizedTest()
    @NullSource @EmptySource @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @DisplayName("notation to moves returns empty array")
    default void ToMovesReturnsEmptyArray(String notation) {
        Board instance = newBoard();
        int[] moves = instance.parseNotation(notation);
        assertTrue(moves.length == 0, "moves not empty");
    }


    @ParameterizedTest()
    @NullSource @EmptySource @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @DisplayName("diagram to board throws runtime exception")
    default void ToBoardThrowsRuntimeException(String notation) {
        Board instance = newBoard();
        assertThrows(RuntimeException.class, () -> {
            instance.fromDiagram(notation);
        });
    }


    @ParameterizedTest()
    @NullSource @EmptySource @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @DisplayName("notation to move throws runtime exception")
    default void ToMoveThrowsRuntimeException(String notation) {
        Board instance = newBoard();
        assertThrows(RuntimeException.class, () -> {
            instance.parseCoordinates(notation);
        });
    }


    @ParameterizedTest()
    @ValueSource(ints = { Game.NULL_MOVE, Integer.MIN_VALUE })
    @DisplayName("move to coordinates throws runtime exception")
    default void ToCoordinateThrowsRuntimeException(int move) {
        Board instance = newBoard();
        assertThrows(RuntimeException.class, () -> {
            instance.toCoordinates(move);
        });
    }


    @ParameterizedTest()
    @ValueSource(ints = { Game.NULL_MOVE, Integer.MIN_VALUE })
    @DisplayName("move to notation throws runtime exception")
    default void ToNotationThrowsRuntimeException(int move) {
        Board instance = newBoard();
        assertThrows(RuntimeException.class, () -> {
            int[] moves = { move };
            instance.toNotation(moves);
        });
    }


    @Test
    @DisplayName("symmetries returns non-null array")
    default void SymmetriesReturnsNonNullArray() {
        Board board = newBoard();
        Board[] symmetries = board.symmetries();
        assertNotNull(symmetries);
        assertTrue(symmetries.length > 0);
        assertEquals(board.hash(), symmetries[0].hash());
    }


    @Test
    @DisplayName("fromDiagram creates board of same type")
    default void fromDiagramCreatesBoardOfSameType() {
        Board board = newBoard();
        String diagram = board.toDiagram();
        Board newBoard = board.fromDiagram(diagram);
        assertEquals(board.getClass(), newBoard.getClass());
    }


    @ParameterizedTest()
    @MethodSource("suites")
    @DisplayName("board state matches game state for suite positions")
    default void BoardStateMatchesGameStateForSuites(Suite suite) {
        Board instance = newBoard();
        Board board = instance.fromDiagram(suite.diagram());
        Game game = newGame();
        game.setStartingBoard(board);
        Board current = game.getCurrentBoard();
        assertEquals(board.turn(), game.turn());
        assertEquals(current.turn(), game.turn());
        assertEquals(board.hash(), game.hash());
        assertEquals(current.hash(), game.hash());
    }
}
