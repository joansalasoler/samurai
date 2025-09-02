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
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.joansala.util.Settings;
import static com.joansala.book.uct.BookEntry.*;


/**
 * Reads a game book from a file.
 */
public class BookReader implements Closeable {

    /** Book format signature */
    private final String signature;

    /** Additional book information */
    private final Map<String, String> headers;

    /** File from where the book is read */
    protected final RandomAccessFile file;

    /** File offset of the book entries */
    protected final long offset;

    /** Number of book entries */
    protected final long size;


    /**
     * Open a book for the given file.
     */
    public BookReader(File f) throws IOException {
        file = new RandomAccessFile(f, "r");
        signature = readSignature();
        headers = readHeaders();
        offset = file.getFilePointer();
        size = (file.length() - offset) / ENTRY_SIZE;
    }


    /**
     * Open a book for the given file path.
     */
     public BookReader(String path) throws IOException {
         this(Settings.getFile(path));
     }


    /**
     * Book format signature identifier.
     *
     * @return          Signature string
     */
    public String getSignature() {
        return signature;
    }


    /**
     * Map view of the book headers.
     *
     * @return          Headers map
     */
    public Map<String, String> getHeaders() {
        return headers;
    }


    /**
     * Reads a book entry for the given game state.
     *
     * @param parent    Game state parent hash
     * @param child     Game state child hash
     * @return          Book entry or {@code null}
     */
    public BookEntry readEntry(long parent, long child) throws IOException {
        for (BookEntry entry : readChildren(parent)) {
            if (entry.getHash() == child) {
                return entry;
            }
        }
        return null;
    }


    /**
     * Reads all book entries for the given parent hash.
     *
     * @param parent    Game state parent hash
     * @return          List of book entries
     */
    public List<BookEntry> readChildren(long parent) throws IOException {
        List<BookEntry> entries = new LinkedList<>();

        if (seekParent(parent)) {
            while (file.getFilePointer() < file.length()) {
                BookEntry entry = new BookEntry();
                entry.readData(file);

                if (entry.getParent() == parent) {
                    entries.add(entry);
                } else {
                    break;
                }
            }
        }

        return entries;
    }


    /**
     * Reads the book headers and returns them.
     *
     * @return          New book headers map
     */
    private Map<String, String> readHeaders() throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line = file.readLine();

        while (line != null && !line.trim().isEmpty()) {
            String[] parts = line.split("[:]", 2);
            headers.put(parts[0].trim(), parts[1].trim());
            line = file.readLine();
        }

        return headers;
    }


    /**
     * Reads the book signature and returns it.
     *
     * @return          Signature string
     */
    private String readSignature() throws IOException {
        return file.readLine();
    }


    /**
     * Seeks the first entry with the given parent hash.
     *
     * @param parent    Parent node hash code
     * @return          Whether any entry with this parent was found
     */
    private boolean seekParent(long parent) throws IOException {
        long ceiling = size - 1;
        long floor = 0;
        long result = -1;

        while (floor <= ceiling) {
            long middle = (floor + ceiling) / 2;
            long position = offset + middle * ENTRY_SIZE;

            file.seek(position);
            long hash = file.readLong();

            if (hash == parent) {
                result = middle;
                ceiling = middle - 1;
            } else if (hash < parent) {
                floor = middle + 1;
            } else {
                ceiling = middle - 1;
            }
        }

        if (result != -1) {
            file.seek(offset + result * ENTRY_SIZE);
            return true;
        }

        return false;
    }


    /**
     * Returns all entries in the book.
     *
     * @return          List of book entries
     */
    public List<BookEntry> entries() throws IOException {
        List<BookEntry> entries = new LinkedList<>();
        file.seek(offset);

        for (long i = 0; i < size; i++) {
            BookEntry entry = new BookEntry();
            entry.readData(file);
            entries.add(entry);
        }

        return entries;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        file.close();
    }
}
