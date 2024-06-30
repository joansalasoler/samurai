package com.joansala.cli.test;

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

import java.util.concurrent.Callable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.google.inject.Inject;
import com.google.inject.Injector;
import picocli.CommandLine.*;

import com.joansala.engine.Board;
import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import com.joansala.cli.util.ProcessConverter;
import com.joansala.util.bench.BenchPlayer;
import com.joansala.util.RoundRobin;
import static com.joansala.engine.Game.*;


/**
 * Runs a tournament between UCI engines.
 */
@Command(
  name = "battle",
  description = "Tournament between engines",
  mixinStandardHelpOptions = true
)
public class BattleCommand implements Callable<Integer> {

    /** Converts command strings to processes */
    private ProcessConverter converter = new ProcessConverter();

    /** Game instance */
    private Game game;

    /** Game board instance */
    private Board board;

    /** Class injector instance */
    private Injector injector;

    /** UCI player instances */
    private BenchPlayer[] players;


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
      names = "--command",
      description = "Custom UCI engine commands (multiple).",
      defaultValue = ProcessConverter.DEFAULT
    )
    private String[] commands = null;

    @Option(
      names = "--matches",
      description = "Number of matches to play."
    )
    private int matches = 0;

    @Option(
      names = "--rounds",
      description = "Number of rounds to play."
    )
    private int rounds = 1;

    @Option(
      names = "--suite",
      description = "Print games instead of a report"
    )
    private boolean suite = false;

    @Option(
      names = "--watch",
      description = "Print the board after each move"
    )
    private boolean watch = false;

    @Option(
      names = "--debug",
      description = "Log debug messages."
    )
    private boolean debug = false;


    /**
     * Creates a new service.
     */
    @Inject public BattleCommand(Injector injector) {
        this.board = injector.getInstance(Board.class);
        this.game = injector.getInstance(Game.class);
        this.injector = injector;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        configureLoggers();
        players = createPlayers(commands);
        runTournament();

        for (BenchPlayer player : players) {
            player.quitEngine();
        }

        return 0;
    }


    /**
     * Runs the tournament.
     */
    public void runTournament() throws Exception {
        RoundRobin<BenchPlayer> pairer = new RoundRobin<>(players);

        if (matches <= 0 && rounds >= 1) {
            int length = players.length;
            int size = length + length % 2;
            int tables = length / 2;
            matches = Math.max(1, rounds * tables * (size - 1));
        }

        if (suite == false) {
            System.out.format("%s%n", formatSetup());
            System.out.format("%s%n", formatPlayers());
            System.out.println("Playing tournament");
            System.out.println(horizontalRule('-'));
        }

        for (int match = 1; match <= matches; match++) {
            BenchPlayer south = pairer.next();
            BenchPlayer north = pairer.next();
            BenchPlayer player = north;

            if (suite == false) {
                int s = south.identifier();
                int n = north.identifier();
                System.out.format("%3d. %2d-%-2d ", match, s, n);
            }

            // Play a match till its completion

            south.setDebug(debug);
            south.startNewGame();
            south.setTurn(SOUTH);

            north.setDebug(debug);
            north.startNewGame();
            north.setTurn(NORTH);

            game.setBoard(board);

            if (watch) printBoard(game);

            while (!game.hasEnded()) {
                player.startPondering(game);
                player = (game.turn() == SOUTH) ? south : north;
                player.stopPondering();
                int move = player.startThinking(game);

                game.ensureCapacity(1 + game.length());
                game.makeMove(move);

                if (watch) printBoard(game);
            }

            // Count and print match results

            final int winner = game.winner();

            south.stopPondering();
            south.wins.test(winner == SOUTH);
            south.ties.test(winner == DRAW);

            north.stopPondering();
            north.wins.test(winner == NORTH);
            north.ties.test(winner == DRAW);

            if (suite == false) {
                String notation = board.toNotation(game.moves());
                System.out.format("%s ", ellipsis(notation, 42));
                System.out.format("%s%n", formatWinner(winner));
            } else {
                String diagram = board.toDiagram();
                String notation = board.toNotation(game.moves());
                System.out.format("%s moves %s\n", diagram, notation);
            }
        }

        if (suite == false) {
            System.out.format("%n%s", formatResults());
        }
    }


    /**
     * Initializes the UCI players for each service.
     */
    private BenchPlayer[] createPlayers(String[] commands) throws Exception {
        Process[] services = createServices(commands);
        BenchPlayer[] players = new BenchPlayer[services.length];

        for (int i = 0; i < services.length; i++) {
            BenchPlayer player = injector.getInstance(BenchPlayer.class);

            player.setDepth(depth);
            player.setMoveTime(moveTime);
            player.setService(services[i]);
            player.startEngine();

            players[i] = player;
        }

        return players;
    }


    /**
     * Creates service processes for the given commands. This method
     * returns at least two services.
     */
    private Process[] createServices(String[] commands) throws Exception {
        int size = Math.max(2, commands.length);
        Process[] services = new Process[size];

        for (int i = 0; i < size; i++) {
            services[i] = (i < commands.length) ?
                converter.convert(commands[i]) :
                converter.convert(commands[0]);
        }

        return services;
    }


    /**
     * Configure the application loggers.
     */
    private void configureLoggers() {
        Logger logger = Logger.getLogger("com.joansala.uci");
        logger.setLevel(debug ? Level.ALL : Level.OFF);
    }


    /**
     * Prints the board of the given game.
     */
    private void printBoard(Game game) {
        System.out.println("\n " + game.toBoard());
    }


    /**
     * Formats the current setting into a string.
     */
    private String formatSetup() {
        return String.format(
            "Tournament setup%n" +
            "%s%n" +
            "Players:       %,37d players%n" +
            "Matches:       %,37d matches%n" +
            "Rounds:        %,37d rounds%n" +
            "Depth limit:   %,37d plies%n" +
            "Time per move: %,37d ms%n",
            horizontalRule('-'),
            players.length,
            matches,
            rounds,
            depth,
            moveTime
        );
    }


    /**
     * Formats the players of the tournament.
     */
    private String formatPlayers() {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("Tournament players%n"));
        builder.append(String.format("%s%n", horizontalRule('-')));

        for (BenchPlayer player : players) {
            builder.append(formatPlayer(player));
        }

        return builder.toString();
    }


    /**
     * Formats the results of the tournament.
     */
    private String formatResults() {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("Tournament results%n"));
        builder.append(String.format("%s%n", horizontalRule('-')));

        int ranking = 1;
        Arrays.sort(players, classifier);

        for (BenchPlayer player : players) {
            builder.append(String.format("%3d. ", ranking++));
            builder.append(formatResult(player));
        }

        return builder.toString();
    }


    /**
     * Formats the results of a player.
     */
    private String formatResult(BenchPlayer player) {
        return String.format(
            "%2d %s %5.1f%% (%2d) drew %5.1f%% (%2d) won%n",
            player.identifier(),
            ellipsis(player.getClient().getName(), 18),
            player.ties.percentage(),
            player.ties.success(),
            player.wins.percentage(),
            player.wins.success()
        );
    }


    /**
     * Formats a player.
     */
    private String formatPlayer(BenchPlayer player) {
        return String.format(
            "%3d. %s%n",
            player.identifier(),
            ellipsis(player.getClient().getName(), 54)
        );
    }


    /**
     * Formats the result of a single game.
     */
    private String formatWinner(int winner) {
        return (winner == SOUTH) ? "(1-0)" :
               (winner == NORTH) ? "(0-1)" : "(½-½)";
    }


    /**
     * String representation truncated to 60 characters.
     *
     * @param o         An object
     * @param size      Maximum string size
     * @return          A string
     */
    private static String ellipsis(Object o, int size) {
        final String v = String.valueOf(o);
        return v.replaceAll("(?<=^.{" + size + "}).*$", "…");
    }


    /**
     * Returns an horizontal rule of exactly 60 characters.
     *
     * @param c         Rule character
     * @return          A new string
     */
    private static String horizontalRule(char c) {
        return new String(new char[60]).replace('\0', c);
    }


    /**
     * Compare number of wins and ties of two players.
     */
    private Comparator<BenchPlayer> classifier = (first, second) -> {
        return -10 * first.wins.compareTo(second.wins) +
                -1 * first.ties.compareTo(second.ties);
    };
}
