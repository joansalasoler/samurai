package com.joansala.uci;

/*
 * Copyright (c) 2021 Joan Sala Soler <contact@joansala.com>
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

import com.google.inject.Inject;
import com.joansala.engine.*;
import static com.joansala.uci.UCI.*;
import static com.joansala.engine.Game.*;


/**
 * A simple UCI player.
 */
public class UCIPlayer {

    /** UCI client instance */
    private UCIClient client;

    /** Game board */
    private Board parser;

    /** Depth limit per move (plies) */
    private int depth = Engine.DEFAULT_DEPTH;

    /** Time limit per move (ms) */
    private long moveTime = Engine.DEFAULT_MOVETIME;

    /** Milliseconds left on south's clock */
    private long southTime = Integer.MIN_VALUE;

    /** Milliseconds left on north's clock */
    private long northTime = Integer.MIN_VALUE;

    /** Time increment per move (ms) */
    private long incrementTime = 0;

    /** If time control is enabled */
    private boolean hasTimeControl = false;


    /**
     * Create a new UCI player.
     */
    @Inject public UCIPlayer(UCIClient client) {
        this.parser = client.getBoard();
        this.client = client;
    }


    /**
     * Checks if the player process is alive.
     */
    public boolean isRunning() {
        return client.isRunning();
    }


    /**
     * UCI client of this player.
     */
    public UCIClient getClient() {
        return client;
    }


    /**
     * Sets the engine service.
     */
    public void setService(Process service) {
        client.setService(service);
    }


    /**
     * Depth limit for this player in plies.
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }


    /**
     * Time limit for this player in milliseconds.
     */
    public void setMoveTime(long moveTime) {
        this.moveTime = moveTime;
    }


    /**
     * Enable or disable time controls.
     */
    public void setTimeControl(boolean active) {
        this.hasTimeControl = active;
    }


    /**
     * Sets the time increment per move.
     */
    public void setIncrementTime(long milliseconds) {
        this.incrementTime = milliseconds;
    }


    /**
     * Time left on south's clock.
     */
    public void setSouthTime(long milliseconds) {
        this.southTime = milliseconds;
    }


    /**
     * Time left on north's clock.
     */
    public void setNorthTime(long milliseconds) {
        this.northTime = milliseconds;
    }


    /**
     * Sends an UCI option to the engine service.
     */
    public void setUCIOption(String name, String value) throws Exception {
        client.send(SETOPTION, NAME, name, VALUE, value);
    }


    /**
     * Enable or disable debug mode.
     */
    public void setDebug(boolean active) throws Exception {
        client.send(DEBUG, active ? ON : OFF);
    }


    /**
     * Turn on or off this engine draw search mode.
     */
    public void setDrawSearch(boolean active) throws Exception {
        setUCIOption(DRAW_SEARCH, active ? TRUE : FALSE);
    }


    /**
     * Turn this engine is playing.
     */
    public void setTurn(int turn) throws Exception {
        setUCIOption(ENGINE_TURN, turn == SOUTH ? SOUTH_TURN : NORTH_TURN);
    }


    /**
     * Asks the engine process to quit.
     */
    public void quitEngine() throws Exception {
        client.send(QUIT);
    }


    /**
     * Asks the engine process to start its UCI mode.
     */
    public void startEngine() throws Exception {
        client.send(UCI);

        while (!client.isUCIReady()) {
            client.receive();
        }
    }


    /**
     * Asks the engine process to get ready for a new game.
     */
    public void startNewGame() throws Exception {
        client.send(UCINEWGAME);
        client.send(ISREADY);

        while (!client.isReady()) {
            client.receive();
        }
    }


    /**
     * Asks the engine process to think for a move.
     *
     * @param game      Game to ponder
     * @return          Best move found
     */
    public int startThinking(Game game) throws Exception {
        client.send(toUCIPosition(game));

        if (hasTimeControl == false) {
            client.send(
                GO, MOVETIME, moveTime, DEPTH, depth
            );
        }  else {
            client.send(
                GO, WTIME, southTime, BTIME, northTime,
                WINC, incrementTime, BINC, incrementTime,
                DEPTH, depth
            );
        }

        while (client.isThinking()) {
            client.receive();
        }

        return client.getBestMove();
    }


    /**
     * Asks the engine process to start pondering a move.
     *
     * @param game      Game to ponder
     */
    public void startPondering(Game game) throws Exception {
        String position = toUCIPosition(game);
        int ponder = client.getPonderMove();

        if (ponder != Game.NULL_MOVE) {
            if (game.isLegal(ponder)) {
                int cursor = game.getCursor();
                game.ensureCapacity(1 + game.length());
                game.makeMove(ponder);
                position = toUCIPosition(game);
                game.unmakeMove();
                game.setCursor(cursor);
            }
        }

        client.send(position);
        client.send(GO, PONDER);
    }


    /**
     * Asks the engine process to stop pondering a move.
     */
    public void stopPondering() throws Exception {
        if (client.isPondering()) {
            client.send(STOP);

            while (client.isPondering()) {
                client.receive();
            }
        }
    }


    /**
     * Format a UCI position command for the given game state.
     *
     * @param game      Game state
     * @return          UCI command string
     */
    private String toUCIPosition(Game game) {
        String moves = parser.toNotation(game.moves());
        String params = moves.isEmpty() ? moves : " " + MOVES + " " + moves;
        return String.format("%s %s%s", POSITION, STARTPOS, params);
    }
}
