package com.joansala.cli.book;

/*
 * Samurai framework.
 * Copyright (C) 2021-2025 Joan Sala Soler <contact@joansala.com>
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
import picocli.CommandLine.*;

import com.google.inject.Inject;
import com.joansala.engine.Game;
import com.joansala.engine.doe.*;


/**
 * Propagates evaluation data from symmetric positions.
 */
@Command(
  name = "symmetrize",
  description = "Symmetrizes an opening book database",
  mixinStandardHelpOptions = true
)
public class SymmetrizeCommand implements Callable<Integer> {

    /** Game instance */
    private Game game;

    @Option(
      names = "--input",
      description = "Database storage folder"
    )
    private String inputPath = "book.db";


    /**
     * Create a new instance.
     */
    @Inject
    public SymmetrizeCommand(Game game) {
        this.game = game;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        final DOEStore store = new DOEStore(inputPath);
        final DOESymmetrizer symmetrizer = new DOESymmetrizer(store);

        // Ensures the store is properly closed

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down.");
            store.close();
            System.out.println("Done.");
        }));

        System.out.format("%nSymmetrizing book%n%s%n", horizontalRule('-'));
        symmetrizer.symmetrizeBook(game);
        System.out.println("Book symmetrized successfully.");

        return 0;
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
}