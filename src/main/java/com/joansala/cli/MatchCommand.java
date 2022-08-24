package com.joansala.cli;

/*
 * Samurai framework.
 * Copyright (C) 2021 Joan Sala Soler <contact@joansala.com>
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

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import com.google.inject.Inject;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.terminal.*;
import picocli.CommandLine.*;

import com.joansala.engine.Board;
import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import com.joansala.cli.util.ProcessConverter;
import com.joansala.uci.UCIPlayer;
import com.joansala.util.StopWatch;


/**
 * Executes the user interface to play against an engine.
 */
@Command(
  name = "match",
  description = "Play a match against the engine",
  mixinStandardHelpOptions = true
)
public class MatchCommand implements Callable<Integer> {

    /** Identifier for an undo move */
    private static final int UNDO_MOVE = Integer.MIN_VALUE;

    /** A stop watch to control time */
    private final StopWatch watch = new StopWatch();

    /** UCI player instance */
    private UCIPlayer player;

    /** Game being played */
    private Game game;

    /** Current board */
    private Board board;

    /** Turn of the human player */
    private int turn = Game.SOUTH;

    /** Milliseconds left on south's clock */
    private long southTime = 0;

    /** Milliseconds left on north's clock */
    private long northTime = 0;

    @Option(
      names = "--command",
      description = "Custom UCI engine command",
      converter = ProcessConverter.class,
      defaultValue = "<default>"
    )
    private Process service = null;

    @Option(
      names = "--depth",
      description = "Depth limit per move (plies)"
    )
    private int depth = Engine.DEFAULT_DEPTH;

    @Option(
      names = "--movetime",
      description = "Time limit per move (ms)"
    )
    private long moveTime = Engine.DEFAULT_MOVETIME;

    @Option(
      names = "--control",
      description = "Milliseconds per player"
    )
    private long controlTime = 0;

    @Option(
      names = "--increment",
      description = "Milliseconds per move"
    )
    private long incrementTime = 0;

    @Option(
      names = "--debug",
      description = "Log debug messages."
    )
    private boolean debug = false;

    @Option(
      names = "--uci-option",
      description = "Custom UCI option [name:value] (multiple)"
    )
    private String[] options = new String[0];


    /**
     * Creates a new service.
     */
    @Inject public MatchCommand(UCIPlayer player, Game game) {
        this.player = player;
        this.game = game;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        configureLoggers();
        player.setService(service);
        runMatch();
        return 0;
    }


    /**
     * Runs a match against an engine process.
     */
    public void runMatch() throws Exception {
        LineReader reader = newLineReader();
        PrintWriter writer = reader.getTerminal().writer();
        bindKeyMaps(reader, writer);

        this.southTime = controlTime;
        this.northTime = controlTime;

        try {
            initializePlayer();
            board = game.toBoard();

            printWelcome(writer);
            printBoard(writer);

            turn = askForTurn(reader);
            player.setTurn(-turn);

            while (player.isRunning() && !game.hasEnded()) {
                boolean isUserTurn = (turn == game.turn());

                try {
                    watch.reset();

                    if (isUserTurn == true) {
                        int move = askForMove(reader);

                        watch.stop();
                        updateClock(game, watch);
                        makeMove(game, move);
                    } else {
                        player.stopPondering();
                        int move = askForMove(writer);

                        watch.stop();
                        updateClock(game, watch);
                        makeMove(game, move);

                        player.startPondering(game);
                        printMove(writer, move);
                    }

                    board = game.toBoard();
                    printBoard(writer);
                } catch (UserInterruptException e) {
                    throw e;
                } catch (Exception e) {
                    writer.println(e.getMessage());
                    if (!isUserTurn) throw e;
                }
            }

            if (game.hasEnded()) {
                game.endMatch();
                printBoard(writer);
                printWinner(writer);
            }
        } catch (UserInterruptException e) {
            writer.println("Keyboard interrupt.");
        } catch (Exception e) {
            writer.println("Unhandled exception.");
            e.printStackTrace();
        } finally {
            if (player.isRunning()) {
                player.quitEngine();
            }

            writer.println("Bye.");
            writer.flush();
        }
    }


    /**
     * Adds a move to the given game.
     *
     * @param game          Game state
     * @param move          Move to perform
     */
    private void makeMove(Game game, int move) {
        if (game.isLegal(move) == false) {
            throw new IllegalArgumentException(
                "Sorry, that's not a legal move.");
        }

        game.ensureCapacity(1 + game.length());
        game.makeMove(move);
    }


    /**
     * Unmake last moves from human and machine.
     */
    private void unmakeMoves(Game game) {
        if (game.length() > 1) {
            game.unmakeMove();

            while (turn != game.turn()) {
                game.unmakeMove();
            }
        }
    }


    /**
     * Asks the user which move to perform.
     *
     * @param reader    Terminal reader
     * @return          Move identifier
     */
    private int askForMove(LineReader reader) throws Exception {
        String notation = reader.readLine("Your move? ").trim();
        return "undo".equals(notation) ? UNDO_MOVE : board.toMove(notation);
    }


    /**
     * Asks the engine which move to perform.
     *
     * @param writer    Terminal writer
     * @return          Move identifier
     */
    private int askForMove(PrintWriter writer) throws Exception {
        player.setSouthTime(southTime);
        player.setNorthTime(northTime);
        return player.startThinking(game);
    }


    /**
     * Asks the user if the computer should move first.
     *
     * @param reader    Terminal reader
     * @return          Turn of the user
     */
    private int askForTurn(LineReader reader) throws Exception {
        String reply = reader.readLine("Shall I move first? ");
        boolean isYes = reply != null && reply.matches("^\\s*y.*");
        return isYes ? Game.NORTH : Game.SOUTH;
    }


    /**
     * Update remaining time on a player's clock.
     */
    private void updateClock(Game game, StopWatch watch) {
        if (game.turn() == Game.SOUTH) {
            southTime += incrementTime;
            southTime -= watch.elapsed();
        } else {
            northTime += incrementTime;
            northTime -= watch.elapsed();
        }
    }


    /**
     * Prints a welcome message to the console.
     */
    private void printWelcome(PrintWriter writer) {
        String name = player.getClient().getName();
        Package pack = UCIPlayer.class.getPackage();
        String version = pack.getImplementationVersion();
        writer.format("UCI Match %s%n", version);
        writer.format("Playing against %s%n", name);
    }


    /**
     * Prints the current game board to the console.
     *
     * @param writer    Terminal writer
     */
    private void printBoard(PrintWriter writer) {
        if (controlTime > 0) printClock(writer);
        writer.format("%n%s%n%n", board);
        writer.flush();
    }


    /**
     * Print the current match clock.
     *
     * @param writer    Terminal writer
     */
    private void printClock(PrintWriter writer) {
        Duration st = Duration.ofMillis(southTime);
        Duration nt = Duration.ofMillis(northTime);

        writer.format(
            "%nClock: %02d:%02d:%02d - %02d:%02d:%02d%n",
            st.toHours(), st.toMinutesPart(), st.toSecondsPart(),
            nt.toHours(), nt.toMinutesPart(), nt.toSecondsPart()
        );

        writer.flush();
    }


    /**
     * Prints an engine move to the console.
     *
     * @param writer    Terminal writer
     */
    private void printMove(PrintWriter writer, int move) {
        String notation = board.toCoordinates(move);
        writer.format("My move is: %s%n", notation);
        writer.flush();
    }


    /**
     * Prints a welcome message to the console.
     */
    private void printWinner(PrintWriter writer) {
        final int winner = game.winner();

        if (winner == Game.DRAW) {
            writer.println("This match was drawn.");
        } else if (winner == turn) {
            writer.println("You won this match!");
        } else {
            writer.println("You lost this match.");
        }
    }


    /**
     * Configure the application loggers.
     */
    private void configureLoggers() {
        Logger logger = Logger.getLogger("com.joansala.uci");
        logger.setLevel(debug ? Level.ALL : Level.OFF);
    }


    /**
     * Initialize the computer player.
     */
    private void initializePlayer() throws Exception{
        player.startEngine();
        player.setDebug(debug);
        player.startNewGame();
        player.setDepth(depth);
        player.setMoveTime(moveTime);
        player.setIncrementTime(incrementTime);
        player.setTimeControl(controlTime > 0);

        for (String option : options) {
            String[] parts = option.split(":", 2);
            player.setUCIOption(parts[0], parts[1]);
        }
    }


    /**
     * Configure keystrokes to the reader.
     */
    private void bindKeyMaps(LineReader reader, PrintWriter writer) {
        KeyMap<Binding> map = reader.getKeyMaps().get(LineReader.MAIN);
        Map<String, Widget> widgets = reader.getWidgets();

        widgets.put("undo-moves", new UndoWidget(writer));
        map.bind(new Reference("undo-moves"), KeyMap.ctrl('U'));
    }


    /**
     * Creates a new terminal instance.
     *
     * @return          New terminal
     */
    private static Terminal newTerminal() throws IOException {
        return TerminalBuilder.builder().build();
    }


    /**
     * Creates a new terminal reader instance.
     *
     * @return          New reader
     */
    private static LineReader newLineReader() throws IOException {
        Terminal terminal = newTerminal();
        LineReaderBuilder builder = LineReaderBuilder.builder();
        return builder.terminal(terminal).build();
    }


    /**
     * This is bound to Ctrl + U to undo moves.
     */
    private class UndoWidget implements Widget {

        /** Output channel */
        private PrintWriter writer;

        /** Create a new instance */
        public UndoWidget(PrintWriter writer) {
            this.writer = writer;
        }

        /**
         * Undo moves until its the human player turn. If the human
         * player is not currently to move, this method does nothing.
         */
        public boolean apply() {
            if (turn == game.turn()) {
                unmakeMoves(game);
                board = game.toBoard();
                System.out.println();
                printBoard(writer);
                System.out.print("Your move? ");
            }

            return true;
        }
    }
}
