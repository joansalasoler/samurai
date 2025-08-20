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

import com.joansala.book.uct.BookReader;
import com.joansala.book.uct.BookEntry;


/**
 * Dump an UCT opening book database in CSV format.
 */
@Command(
  name = "dump",
  description = "Dump an openings book in CSV format",
  mixinStandardHelpOptions = true
)
public class DumpCommand implements Callable<Integer> {

    @Option(
      names = "--roots-path",
      description = "Exported openings book path",
      required = true
    )
    private String rootsPath = null;


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        try (BookReader book = new BookReader(rootsPath)) {
            printHeader();

            book.entries().forEach(entry -> {
                printBookEntry(entry);
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return 0;
    }


    /**
     * Prints the CSV header row.
     */
    private void printHeader() {
        System.out.println("parent,hash,move,score,count");
    }


    /**
     * Prints a book entry as a CSV row.
     *
     * @param entry     Book entry
     */
    public void printBookEntry(BookEntry entry) {
        System.out.format(
            "%s,%s,%s,%s,%s%n",
            Long.toHexString(entry.getParent()),
            Long.toHexString(entry.getHash()),
            entry.getMove(),
            entry.getScore(),
            entry.getCount()
        );
    }
}
