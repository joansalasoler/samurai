package com.joansala.cli.book;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;
import com.google.inject.Inject;
import picocli.CommandLine.*;

import com.joansala.engine.Board;
import com.joansala.engine.Game;
import com.joansala.book.uct.BookReader;
import com.joansala.book.uct.BookEntry;
import com.joansala.util.suites.SuiteReader;


/**
 * Query an UCT opening book database.
 */
@Command(
  name = "query",
  description = "Query an opening book database",
  mixinStandardHelpOptions = true
)
public class QueryCommand implements Callable<Integer> {

    /** Game board instance */
    private Board parser;

    /** Game instance */
    private Game game;

    @Option(
      names = "--suite",
      description = "Game suite file."
    )
    private File suiteFile;

    @Option(
      names = "--roots",
      description = "Exported openings book path",
      required = true
    )
    private String rootsPath = null;


    /**
     * Create a new instance.
     */
    @Inject public QueryCommand(Game game) {
        this.game = game;
        this.parser = game.getBoard();
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        InputStream input = new FileInputStream(suiteFile);

        try (BookReader book = new BookReader(rootsPath)) {
            try (SuiteReader reader = new SuiteReader(input)) {
                reader.stream().forEach((suite) -> {
                    Board board = parser.toBoard(suite.diagram());
                    int[] moves = board.toMoves(suite.notation());

                    game.setBoard(board);
                    game.ensureCapacity(1 + moves.length);

                    for (int move : moves) {
                        if (game.hasEnded() == false) {
                            game.makeMove(move);
                        }
                    }

                    BookEntry entry = null;
                    long parent = game.hash();

                    for (int move : game.legalMoves()) {
                        Board state = game.toBoard();
                        game.makeMove(move);

                        try {
                            entry = book.readEntry(parent, game.hash());
                            printBookEntry(state, entry);
                        } catch (Exception e) {}

                        game.unmakeMove();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return 0;
    }



    /**
     * Show the values of a book entry on the standard output.
     *
     * @param board     Board sate for the entry
     * @param entry     Book entry
     */
    public void printBookEntry(Board board, BookEntry entry) {
        System.out.format(
            "move = %s, score = %.2f, value = %.2f, count = %d%n",
            board.toCoordinates(entry.getMove()),
            computeScore(game, entry),
            entry.getScore(),
            entry.getCount()
        );
    }


    /**
     * Compute the selection score of a node. This method returns an
     * upper confidence bound on the score of the entry.
     *
     * @param game      Game instance
     * @param node      A book entry
     * @return          Score of the node
     */
    private double computeScore(Game game, BookEntry entry) {
        final double bound = game.infinity() / Math.sqrt(entry.getCount());
        final double score = entry.getScore() + bound;

        return score;
    }
}
