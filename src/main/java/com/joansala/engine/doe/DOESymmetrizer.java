package com.joansala.engine.doe;

/*
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

import java.io.IOException;
import com.joansala.engine.Board;
import com.joansala.engine.Game;
import com.joansala.engine.doe.DOE;


/**
 * Symmetrizes an openings book by propagating evaluation data between
 * symmetric positions to improve book coverage and reliability.
 *
 * This class traverses the openings book tree and ensures that
 * symmetric positions share the most reliable evaluation data based
 * on visit counts. It also expands children from symmetric positions
 * when needed to maintain complete coverage.
 */
public class DOESymmetrizer {

    /** Store to symmetrize */
    private DOEStore store;


    /**
     * Creates a new symmetrizer for the given DOE store.
     *
     * @param store     DOE database store to symmetrize
     */
    public DOESymmetrizer(DOEStore store) {
        this.store = store;
    }


    /**
     * Recursively expand symmetries for all nodes in the tree.
     *
     * @param game
     */
    public void symmetrizeBook(Game game) throws IOException {
        game.ensureCapacity(DOE.MAX_DEPTH);
        DOENode root = store.read(1L);
        propagateSymmetries(root, game);
    }


    /**
     * Propagates symmetry information through the tree starting from
     * the given node. Expands children from symmetric positions if needed
     * and recursively processes all child nodes.
     *
     * @param node  The root node to start propagation from
     * @param game  The game state used for move navigation
     */
    protected void propagateSymmetries(DOENode node, Game game) {
        if (node.isTerminal() || game.length() == DOE.MAX_DEPTH) {
            return;
        }

        // If the node has no children, check if any symmetric position
        // has children and expand them to ensure complete coverage

        if (node.hasChildren() == false) {
            Board board = game.getCurrentBoard();
            DOENode symmetry = findBestSymmetry(node, board);

            if (symmetry.hasChildren()) {
                updateFromSymmetry(node, symmetry);
                expandChildren(node, game);
            }
        }

        // Process all children nodes recursively to propagate symmetry
        // information throughout the entire subtree

        if (node.hasChildren() == true) {
            DOENode child = store.read(node.child);
            processSymmetries(child, game);

            while ((child = store.read(child.sibling)) != null) {
                processSymmetries(child, game);
            }
        }
    }


    /**
     * Recursively processes child nodes to propagate symmetry information.
     *
     * At each node, checks for symmetric positions and updates the node's
     * evaluation data from the best available symmetry.
     *
     * @param node  The node to process for symmetries
     * @param game  The game state used for move navigation
     */
    private void processSymmetries(DOENode node, Game game) {
        game.makeMove(node.move);

        Board board = game.getCurrentBoard();
        DOENode symmetry = findBestSymmetry(node, board);
        updateFromSymmetry(node, symmetry);
        propagateSymmetries(node, game);

        game.unmakeMove();
    }


    /**
     * Updates a node's evaluation data from a symmetric position that
     * has more reliable statistics.
     *
     * @param node      The node to update
     * @param symmetry  The symmetric node to copy data from
     */
    private void updateFromSymmetry(DOENode node, DOENode symmetry) {
        node.evaluated = true;
        node.score = symmetry.score;
        node.count = symmetry.count;
        store.write(node);
    }


    /**
     * Expands all the children of a node.
     *
     * @param node      Parent node
     * @param game      Game state
     *
     * @return          Expanded nodes
     */
    private void expandChildren(DOENode parent, Game game) {
        for (int move : game.legalMoves()) {
            game.makeMove(move);
            DOENode child = new DOENode(game, move);
            store.write(child);
            parent.pushChild(child);
            game.unmakeMove();
        }

        parent.expanded = true;
        store.write(parent);
    }


    /**
     * Finds the most reliable evaluation from all symmetric positions
     * that can be found in the store.
     *
     * @param board     Board for which to find symmetries
     * @return          Evaluated node with the highest visit count
     *                  among all symmetric positions
     */
    private DOENode findBestSymmetry(DOENode node, Board board) {
        DOENode bestNode = node;

        for (Board symmetry : board.symmetries()) {
            long hash = symmetry.hash();

            for (DOENode symNode : store.find(hash)) {
                if (!symNode.isEvaluated()) {
                    continue;
                } else if (symNode.hasMoreSimulations(bestNode)) {
                    bestNode = symNode;
                }
            }
        }

        return bestNode;
    }
}
