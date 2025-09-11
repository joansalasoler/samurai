package com.joansala.book.uct;

/*
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import com.joansala.engine.Roots;
import static com.joansala.engine.Game.*;


/**
 * UCT opening book implementation.
 *
 * This implementation uses confidence bounds to make safe move choices,
 * assuming the opponent will achieve their best possible outcome (UCB).
 * Making it suitable for conservative play where we want to avoid risky
 * moves even if they might have high average payoffs.
 */
public class UCTRoots implements Closeable, Roots<Game> {

    /** Reads the book data from a file */
    private final BookReader reader;

    /** Threshold score */
    private int threshold = Game.DRAW_SCORE;

    /** If no more book moves can be found */
    private boolean outOfBook = false;

    /** Maximum evaluation score */
    private int maxScore = Integer.MAX_VALUE;


    /**
     * Create a book for the given file.
     */
     public UCTRoots(File file) throws IOException {
         reader = new BookReader(file);
     }


    /**
     * Create a book for the given file path.
     */
    public UCTRoots(String path) throws IOException {
        reader = new BookReader(path);
    }


    /**
     * Minimum score a move must have to be playable.
     *
     * @param score     Threshold score
     */
    public void setThreshold(int score) {
        threshold = score;
    }


    /**
     * Sets the maximum score a position can possibly be evaluated.
     *
     * @see Engine#setInfinity(int)
     * @param score     Maximum score
     */
    public void setInfinity(int score) {
        maxScore = Math.max(score, 1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void newMatch() {
        outOfBook = false;
    }


    /**
     * Compute the selection score of a node.
     *
     * This method returns the negated Upper Confidence Bound (UCB) of
     * the opponent's score, representing the worst-case scenario from
     * the player to move perspective. Higher values indicate safer moves
     * where even the pessimistic outcome is acceptable.
     *
     * @param entry     Book entry
     * @return          Selection score
     */
    public double selectionScore(BookEntry entry) {
        final double bound = maxScore / Math.sqrt(entry.getCount());
        return -(entry.getScore() + bound);
    }


    /**
     * Filters entries to keep only the best based on selection score.
     *
     * @param entries       List of entries to filter
     * @return              Filtered list of best entries
     */
    private List<BookEntry> filterBestEntries(List<BookEntry> entries) {
        if (entries.isEmpty() == false) {
            BookEntry best = pickSecureEntry(entries);
            entries.removeIf(e -> selectionScore(e) < threshold);
            entries.removeIf(e -> e != best && isInferior(best, e));
        }

        return entries;
    }


    /**
     * Determines if two entries are statistically different.
     *
     * Checks if the second entry is statistically inferior to the first
     * entry using a t-test with 90% confidence level. This is a rough
     * approximation of the statistical test.
     *
     * @param entry     The reference entry (potentially superior)
     * @param other     The entry being compared (potentially inferior)
     * @return          If the other entry is inferior
     */
    private boolean isInferior(BookEntry entry, BookEntry other) {
        double ds = Math.abs(entry.getScore() - other.getScore());
        double se1 = maxScore / Math.sqrt(entry.getCount());
        double se2 = maxScore / Math.sqrt(other.getCount());
        double t = ds / Math.sqrt(se1 * se1 + se2 * se2);

        return t > 1.645;
    }


    /**
     * Lists all the book entries for a game state.
     *
     * @param game      Game state to query
     * @return          List of legal book entries
     */
    public List<BookEntry> findEntries(Game game) throws IOException {
        List<BookEntry> entries = readChildren(game);
        entries.removeIf(e -> !game.isLegal(e.getMove()));
        return entries;
    }


    /**
     * Lists all the book entries for a game state.
     *
     * @param game      Game state to query
     * @return          List of legal book entries
     */
    public List<BookEntry> findBestEntries(Game game) throws IOException {
        List<BookEntry> entries = findEntries(game);
        return filterBestEntries(entries);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int pickBestMove(Game game) throws IOException {
        if (outOfBook == true) {
            return NULL_MOVE;
        }

        List<BookEntry> entries = findEntries(game);

        if ((outOfBook = entries.isEmpty()) == false) {
            entries = filterBestEntries(entries);

            if ((outOfBook = entries.isEmpty()) == false) {
                return pickRandomEntry(entries).getMove();
            }
        }

        return NULL_MOVE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int pickPonderMove(Game game) throws IOException {
        List<BookEntry> entries = findEntries(game);

        if (entries.isEmpty() == false) {
            return pickMaxEntry(entries).getMove();
        }

        return NULL_MOVE;
    }


    /**
     * Picks the entry which provides the best move. This is the
     * entry for which its score's lowest bound is greater.
     *
     * @param entries       List of entries
     * @return              Best entry on the list
     */
    protected BookEntry pickSecureEntry(List<BookEntry> entries) {
        BookEntry bestEntry = entries.get(0);
        double bestScore = selectionScore(bestEntry);

        for (BookEntry entry : entries) {
            final double score = selectionScore(entry);

            if (score > bestScore) {
                bestScore = score;
                bestEntry = entry;
            }
        }

        return bestEntry;
    }


    /**
     * Picks the entry with the highest average score.
     *
     * @param entries       List of entries
     * @return              Best entry on the list
     */
    protected BookEntry pickMaxEntry(List<BookEntry> entries) {
        BookEntry bestEntry = entries.get(0);
        double bestScore = bestEntry.getScore();

        for (BookEntry entry : entries) {
            final double score = entry.getScore();

            if (score > bestScore) {
                bestScore = score;
                bestEntry = entry;
            }
        }

        return bestEntry;
    }


    /**
     * Picks a random element from a list of entries. This performs
     * a weighted random choice using entry counts as weights.
     *
     * @param entries       List of entries
     * @return              An entry on the list
     */
    protected BookEntry pickRandomEntry(List<BookEntry> entries) {
        double distance = count(entries) * Math.random();

        for (BookEntry entry : entries) {
            if ((distance -= entry.getCount()) < 0.0D) {
                return entry;
            }
        }

        return entries.get(0);
    }


    /**
     * Reads all the book entries for the given game state.
     *
     * @param game      Game state
     * @return          Book entries
     */
    protected List<BookEntry> readChildren(Game game) throws IOException {
        List<BookEntry> entries = reader.readChildren(game.hash());

        if (entries.isEmpty() == false) {
            long[] hashes = childHashes(game);
            Stream<Long> stream = Arrays.stream(hashes).boxed();
            Set<Long> childs = stream.collect(Collectors.toSet());
            entries.removeIf(e -> !childs.contains(e.getHash()));
        }

        return entries;
    }


    /**
     * Obtains the hash codes of each child state of a game state.
     *
     * @param game      Game state
     * @return          A new hash array
     */
    private long[] childHashes(Game game) {
        final int[] moves = game.legalMoves();
        final long[] hashes = new long[moves.length];

        int cursor = game.getCursor();
        game.ensureCapacity(1 + game.length());

        for (int i = 0; i < moves.length; i++) {
            game.makeMove(moves[i]);
            hashes[i] = game.hash();
            game.unmakeMove();
        }

        game.setCursor(cursor);

        return hashes;
    }


    /**
     * Sum expansion counts of a list of entries.
     *
     * @param entries       List of entries
     * @return              Sum counts
     */
    private long count(List<BookEntry> entries) {
        long count = 0L;

        for (BookEntry entry : entries) {
            count += entry.getCount();
        }

        return count;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
