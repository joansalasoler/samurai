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

import java.util.concurrent.Callable;
import picocli.CommandLine.*;
import com.joansala.engine.doe.*;


/**
 * Exports a book built using the train command.
 */
@Command(
  name = "export",
  description = "Exports an opening book database",
  mixinStandardHelpOptions = true
)
public class ExportCommand implements Callable<Integer> {

    @Option(
      names = "--input",
      description = "Database storage folder"
    )
    private String inputPath = "book.db";

    @Option(
      names = "--output",
      description = "Path to export the opening book",
      required = true
    )
    private String outputPath = null;

    @Option(
      names = "--min-count",
      description = "Minimum expansions of exported entries"
    )
    private long minCount = 1;


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        final DOEStore store = new DOEStore(inputPath);
        final DOEExporter exporter = new DOEExporter(store);

        // Ensures the store is properly closed

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down.");
            store.close();
            System.out.println("Done.");
        }));

        long count = exporter.export(outputPath, minCount);

        System.out.format("%nExporting book%n%s%n", horizontalRule('-'));
        System.out.format("Entries: %d%n", count);

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
