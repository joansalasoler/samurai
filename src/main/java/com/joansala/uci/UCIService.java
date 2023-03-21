package com.joansala.uci;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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

import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringJoiner;
import com.google.inject.Inject;

import com.joansala.engine.base.BaseCache;
import com.joansala.engine.base.BaseLeaves;
import com.joansala.book.base.BaseRoots;
import com.joansala.uci.util.Parameters;
import com.joansala.uci.util.TimeManager;
import com.joansala.uci.option.*;
import com.joansala.uci.command.*;
import com.joansala.uci.game.*;
import com.joansala.engine.*;
import static com.joansala.uci.UCI.*;


/**
 * Universal Chess Interface protocol implementation.
 */
public class UCIService {

    /** Registered UCI commands */
    private Map<String, UCICommand> commands;

    /** Registered UCI options */
    private Map<String, UCIOption> options;

    /** Thread where the computations are performed */
    private UCIBrain brain;

    /** Game where searches are performed */
    private UCIGame game;

    /** Endgames database */
    private UCILeaves leaves;

    /** Transpositions table */
    private UCICache cache;

    /** Search time manager */
    private TimeManager timeManager;

    /** Search algorithm */
    private Engine engine;

    /** Default start board of the game */
    private Board board;

    /** Openings book */
    private Roots<Game> roots;

    /** Fallback openings book instance */
    private Roots<Game> baseRoots = new BaseRoots();

    /** Fallback endgames book instance */
    private Leaves<Game> baseLeaves = new BaseLeaves();

    /** Fallback transpositions table instance */
    private Cache<Game> baseCache = new BaseCache();;

    /** Whether to send debug messages */
    private boolean debug = false;

    /** If this service is running */
    private boolean alive = true;


    /**
     * Creates a new UCI service.
     *
     * @param game      Game instance
     * @param engine    Engine instance
     */
    @Inject
    public UCIService(Game game, Engine engine) {
        registerExceptionHandler();

        this.game = new UCIGame(game);
        this.timeManager = new TimeManager();
        this.board = game.getBoard();
        this.engine = engine;

        createOptions();
        createCommands();
        engine.setContempt(game.contempt());
        engine.setInfinity(game.infinity());
        cache = new UCICache(baseCache);
        leaves = new UCILeaves(baseLeaves);
        roots = baseRoots;

        brain = new UCIBrain(this);
        brain.setState(board, new int[0]);
        brain.start();
    }


    /**
     * Initialize the set of built-in commands.
     */
    private void createCommands() {
        commands = new HashMap<>();
        commands.put(DEBUG, new DebugCommand());
        commands.put(GO, new GoCommand());
        commands.put(ISREADY, new SynchCommand());
        commands.put(PONDERHIT, new PonderHitCommand());
        commands.put(POSITION, new PositionCommand());
        commands.put(QUIT, new QuitCommand());
        commands.put(SETOPTION, new SetOptionCommand());
        commands.put(STOP, new StopCommand());
        commands.put(UCI, new IdentifyCommand());
        commands.put(UCINEWGAME, new NewGameCommand());
    }


    /**
     * Initialize the set of built-in options.
     */
    private void createOptions() {
        options = new HashMap<>();
        options.put(HASH_SIZE, new HashOption());
        options.put(USE_CACHE, new UseCacheOption());
        options.put(USE_LEAVES, new UseLeavesOption());
        options.put(USE_ROOTS, new OwnBookOption());
        options.put(USE_PONDER, new PonderOption());
        options.put(DRAW_SEARCH, new DrawSearchOption());
        options.put(ENGINE_TURN, new EngineTurnOption());
        options.put(NOISE_LEVEL, new NoiseLevelOption());
    }


    /**
     * A map of registered UCI commands.
     */
    public Map<String, UCICommand> getCommands() {
        return commands;
    }


    /**
     * A map of registered UCI options.
     */
    public Map<String, UCIOption> getOptions() {
        return options;
    }


    /**
     * Brain thread of this service.
     */
    public UCIBrain getBrain() {
        return brain;
    }


    /**
     * Current game state.
     */
    public UCIGame getGame() {
        return game;
    }


    /**
     * Current engine tablebase instance.
     */
    public UCICache getCache() {
        return cache;
    }


    /**
     * Current endgames book instance.
     */
    public UCILeaves getLeaves() {
        return leaves;
    }


    /**
     * Time manager of this service.
     */
    public TimeManager getTimeManager() {
        return timeManager;
    }


    /**
     * Search engine of this service.
     */
    public Engine getEngine() {
        return engine;
    }


    /**
     * Default initial state of a game.
     */
    public Board getBoard() {
        return board;
    }


    /**
     * Current openings book instance.
     */
    public Roots<Game> getRoots() {
        return roots;
    }


    /**
     * Checks if debug mode is enabled.
     */
    public boolean getDebug() {
        return debug;
    }


    /**
     * Sets if debug mode is enabled.
     */
    public void setDebug(boolean active) {
        this.debug = active;
    }


    /**
     * Sets the openings book database to use.
     *
     * @param roots     A roots object or {@code null}
     */
    @Inject(optional=true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setRoots(Roots roots) {
        this.roots = (roots != null) ? roots : baseRoots;
    }


    /**
     * Sets the openings book database to use.
     *
     * @param roots     A roots object or {@code null}
     */
    @Inject(optional=true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setLeaves(Leaves leaves) {
        Leaves<Game> o = (leaves != null) ? leaves : baseLeaves;
        this.leaves = new UCILeaves(o);
    }


    /**
     * Sets the openings book database to use.
     *
     * @param roots     A roots object or {@code null}
     */
    @Inject(optional=true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setCache(Cache cache) {
        Cache<Game> o = (cache != null) ? cache : baseCache;
        this.cache = new UCICache(o);
    }


    /**
     * Synchronize the brain and check if it is still thinking.
     */
    public boolean isReady() {
        brain.synch();
        return !brain.isThinking();
    }


    /**
     * Run this service and start receiving commands.
     */
    public void start() {
        Scanner scanner = new Scanner(System.in);
        options.values().forEach(o -> o.initialize(this));

        while (alive && scanner.hasNext()) {
            String token = scanner.next();

            if (commands.containsKey(token) == false) {
                debug("Unknown command:", token);
                continue;
            }

            try {
                UCICommand command = commands.get(token);
                String args = scanner.nextLine();
                String[] names = command.parameterNames();
                command.accept(this, new Parameters(args, names));
            } catch (Exception e) {
                debug("Command failure:", e.getMessage());
            }
        }

        scanner.close();
        quit();
    }


    /**
     * Stop this service and its brain.
     */
    public void quit() {
        brain.stopThinking();
        brain.interrupt();
        alive = false;
    }


    /**
     * Sends a message to the client.
     */
    public void send(Object... values) {
        send(toMessage(values));
    }


    /**
     * Sends a message to the client.
     */
    public void send(Object message) {
        synchronized (System.out) {
            System.out.println(message);
        }
    }


    /**
     * Sends a debug string if debug is enabled.
     */
    public void debug(Object... values) {
        debug(toMessage(values));
    }


    /**
     * Sends a debug string if debug is enabled.
     */
    public void debug(Object message) {
        if (debug) {
            send(INFO, STRING, message);
        }
    }


    /**
     * Converts objects to a space separated string.
     */
    private String toMessage(Object... values) {
        StringJoiner message = new StringJoiner(" ");

        for (Object value : values) {
            message.add(String.valueOf(value));
        }

        return message.toString();
    }


    /**
     * Handles uncaught exceptions from this service and its brain
     * thread and prints its stack trace.
     */
    private void registerExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            System.exit(1);
        });
    }
}
