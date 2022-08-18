package com.joansala.cli.suite;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;
import com.google.inject.Inject;
import picocli.CommandLine.*;

import com.joansala.engine.Board;
import com.joansala.engine.Game;
import com.joansala.util.suites.SuiteReader;


/**
 * Play suites on the board and print it after each move.
 */
@Command(
  name = "show",
  description = "Play suites on the board",
  mixinStandardHelpOptions = true
)
public class ShowCommand implements Callable<Integer> {

    /** Game board instance */
    private Board parser;

    /** Game instance */
    private Game game;

    @Option(
      names = "--file",
      description = "A suite file"
    )
    private File file;


    /**
     * Creates a new trainer.
     */
    @Inject public ShowCommand(Game game) {
        this.game = game;
        this.parser = game.getBoard();
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        InputStream input = new FileInputStream(file);

        try (SuiteReader reader = new SuiteReader(input)) {
            reader.stream().forEach((suite) -> {
                Board board = parser.toBoard(suite.diagram());
                int[] moves = board.toMoves(suite.notation());

                game.setBoard(board);
                game.ensureCapacity(1 + moves.length);

                System.out.format("%s%n", suite.diagram());
                System.out.format("%n%s%nStart position%n", board);

                for (int move : moves) {
                    board = game.toBoard();
                    game.makeMove(move);

                    System.out.format(
                        "%n%s%nAfter move: %s%n",
                        game.toBoard(),
                        board.toCoordinates(move)
                    );
                }

                if (game.hasEnded()) {
                    game.endMatch();
                    board = game.toBoard();
                    System.out.format("%n%s%nEnd position%n", board);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return 0;
    }
}
