package com.joansala.engine.doe;

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


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sleepycat.je.*;
import com.sleepycat.persist.*;


/**
 * Store and retrieve nodes from a database.
 */
public class DOEStore implements AutoCloseable {

    /** Database entity store */
    private final EntityStore store;

    /** Database environment */
    private final Environment environment;

    /** Primary index for the store */
    private final PrimaryIndex<Long, DOENode> nodes;

    /** Secondary index for the store */
    private final SecondaryIndex<Long, Long, DOENode> hashes;


    /**
     * Create a new store instance.
     */
    public DOEStore(String path) throws DatabaseException {
        File home = getHomeFolder(path);
        environment = openEnvironment(home);
        store = openStore(environment);
        nodes = store.getPrimaryIndex(Long.class, DOENode.class);
        hashes = store.getSecondaryIndex(nodes, Long.class, "hash");
    }


    /**
     * Creates a database environment on the given folder.
     */
    private Environment openEnvironment(File home) throws DatabaseException {
        EnvironmentConfig config = new EnvironmentConfig();
        config.setTransactional(false);
        config.setAllowCreate(true);

        return new Environment(home, config);
    }


    /**
     * Opens the entity store on the given environment
     */
    private EntityStore openStore(Environment env) throws DatabaseException {
        StoreConfig config = new StoreConfig();
        config.setDeferredWrite(true);
        config.setTransactional(false);
        config.setAllowCreate(true);

        return new EntityStore(env, "graph", config);
    }


    /**
     * Get the environment's home folder.
     */
    private File getHomeFolder(String path) {
        File home = new File(path);

        if (home.exists() == false){
            home.mkdirs();
        }

        return home;
    }


    /**
     * Handle a database exception.
     */
    private void handleException(DatabaseException e) {
        throw new RuntimeException(e);
    }


    /**
     * Collection view of the nodes on this store.
     */
    public Collection<DOENode> values() {
        return nodes.map().values();
    }


    /**
     * Check if the store contains a node.
     */
    public boolean contains(Long id) {
        try {
            return nodes.contains(id);
        } catch (DatabaseException e) {
            handleException(e);
        }

        return false;
    }


    /**
     * Get a node from the store.
     */
    public DOENode read(Long id) {
        try {
            if (id != null) {
                return nodes.get(id);
            }
        } catch (DatabaseException e) {
            handleException(e);
        }

        return null;
    }


    /**
     * Save a node to the store.
     */
    public void write(DOENode node) {
        try {
            nodes.putNoReturn(node);
        } catch (DatabaseException e) {
            handleException(e);
        }
    }


    /**
     * Get nodes from the store with the given hash.
     */
    public List<DOENode> find(Long hash) {
        if (hash == null) {
            return Collections.emptyList();
        }

        List<DOENode> nodes = new ArrayList<>();
        EntityIndex<Long, DOENode> index = hashes.subIndex(hash);

        try (EntityCursor<DOENode> cursor = index.entities()) {
            for (DOENode node : cursor) {
                nodes.add(node);
            }
        } catch (DatabaseException e) {
            handleException(e);
        }

        return nodes;
    }


    /**
     * Close this store.
     */
    @Override
    public void close() {
        try {
            store.sync();
            store.close();
            environment.close();
        } catch (DatabaseException e) {
            handleException(e);
        }
    }


    /**
     * Number of stored nodes.
     */
    public long count() {
        try {
            return nodes.count();
        } catch (DatabaseException e) {
            handleException(e);
        }

        return 0;
    }
}
