package com.joansala.engine.uct;

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
import java.util.function.Consumer;

import com.joansala.util.StopWatch;
import com.joansala.engine.*;
import com.joansala.engine.base.*;


/**
 * Best-first search using Upper Confidence Bounds (UCT).
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class UCT extends BaseEngine implements HasLeaves {

    /** Factors the amount of exploration of the tree */
    public static final double DEFAULT_BIAS = 0.353;

    /** Minimum elapsed time between reports */
    private static final int REPORT_INTERVAL = 450;

    /** Prune when less than this amount of memory is available */
    private static final int PRUNE_MEMORY_LIMIT = 2 << 21;

    /** Number of pruning iterations to run at once */
    private static final int PRUNE_ITERATIONS = 20;

    /** Mesures elapsed time between reports */
    private final StopWatch watch = new StopWatch();

    /** Fallback empty endgames instance */
    private final Leaves<Game> baseLeaves = new BaseLeaves();

    /** Current computation root node */
    protected UCTNode root;

    /** Best child found so far */
    protected UCTNode bestChild;

    /** Endgame database */
    protected Leaves<Game> leaves = null;

    /** Exploration bias parameter */
    private double biasFactor = DEFAULT_BIAS;

    /** Exploration priority multiplier */
    private double bias = DEFAULT_BIAS * maxScore;

    /** Path of traversed nodes during expansion */
    private int[] queue = new int[MAX_DEPTH];

    /** Traversed nodes queue offset */
    private int offset = 0;

    /** Next slot on the traversed nodes queue */
    private int index = 0;


    /**
     * Create a new search engine.
     */
    public UCT() {
        this(DEFAULT_BIAS);
    }


    /**
     * Create a new search engine.
     */
    protected UCT(double biasFactor) {
        super();
        leaves = baseLeaves;
        setExplorationBias(biasFactor);
    }


    /**
     * {@inheritDoc}
     */
    public Leaves<Game> getLeaves() {
        return leaves;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int getPonderMove(Game game) {
        if (root == null) {
            return Game.NULL_MOVE;
        }

        UCTNode node = null;
        long hash = game.hash();

        if ((node = findNode(root, hash, 1)) != null) {
            if (node.expanded() && !node.terminal()) {
                if ((node = pickBestChild(node)) != null) {
                    return node.move();
                }
            }
        }

        return Game.NULL_MOVE;
    }


    /**
     * Sets the maximum score a position can obtain.
     *
     * @param score     Infinite value as a positive integer
     */
    @Override
    public synchronized void setInfinity(int score) {
        super.setInfinity(score);
        bias = biasFactor * maxScore;
    }


    /**
     * Preference for exploring suboptimal moves.
     *
     * @param factor    Exploration parameter
     */
    public synchronized void setExplorationBias(double factor) {
        biasFactor = factor;
        bias = biasFactor * maxScore;
    }


    /**
     * Sets an endgames database to use by the engine.
     *
     * @param leaves    Leaves instance or {@code null}
     */
    @Override
    @Inject(optional=true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized void setLeaves(Leaves leaves) {
        this.leaves = (leaves != null) ? leaves : baseLeaves;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void newMatch() {
        super.newMatch();
        root = null;
        System.gc();
    }


    /**
     * Computes the best move for a game and returns its score.
     *
     * @param game      Game instance
     * @return          Average outcome score
     */
    public synchronized int computeBestScore(Game game) {
        computeBestMove(game);
        return (int) -bestChild.score();
    }


    /**
     * Computes a best move for the current position of a game.
     *
     * <p>Note that the search is performed on the provided game object,
     * thus, the game object will change during the computation and its
     * capacity may be increased. The provided game object must not be
     * manipulated while a computation is ongoing.</p>
     *
     * @param game  The game for which a best move must be computed
     * @return      The best move found for the current game position
     *              or {@code Game.NULL_MOVE} if the game already ended
     */
    @Override
    public synchronized int computeBestMove(Game game) {
        if (game.hasEnded()) {
            return Game.NULL_MOVE;
        }

        scheduleCountDown(moveTime);
        game.ensureCapacity(MAX_DEPTH + game.length());
        root = rootNode(game);

        double bestScore = Game.DRAW_SCORE;

        watch.reset();
        watch.start();

        if (root.parent() != null) {
            root.parent().detachFromTree();
            System.gc();
        }

        offset = game.length();

        while (!aborted() || root.child() == null) {
            clearMovesQueue();
            expand(root, game, maxDepth);
            pruneGarbage(root);

            // Report search information periodically

            if (watch.current() >= REPORT_INTERVAL) {
                watch.reset();
                UCTNode child = pickBestChild(root);
                double change = Math.abs(child.score() - bestScore);

                if (child != bestChild || change > 5.0) {
                    bestChild = child;
                    bestScore = child.score();
                    invokeConsumers(game);
                }
            }
        }

        game.unmakeMoves(game.length() - offset);
        bestChild = pickBestChild(root);
        invokeConsumers(game);
        cancelCountDown();

        return bestChild.move();
    }


    /**
     * Computes the expansion priority of an edge. Guides the selection
     * using Upper Confidence Bounds (UCB1).
     *
     * @param child      Child node
     * @param factor     Parent factor
     *
     * @return           Expansion priority
     */
    protected double computePriority(UCTNode child, double factor) {
        final double E = Math.sqrt(factor / child.count());
        final double priority = child.score() - E * bias;

        return priority;
    }


    /**
     * Compute the selection score of a node.
     *
     * @param node      A node
     * @return          Score of the node
     */
    protected double computeScore(UCTNode node) {
        final double bound = maxScore / Math.sqrt(node.count());
        final double score = node.score() + bound;

        return score;
    }


    /**
     * Best child found so far for the given node.
     *
     * @param node      Parent node
     * @return          Child node
     */
    protected UCTNode pickBestChild(UCTNode node) {
        UCTNode child = node.child();
        UCTNode bestChild = node.child();
        double bestScore = computeScore(bestChild);

        while ((child = child.sibling()) != null) {
            double score = computeScore(child);

            if (score < bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }

        return bestChild;
    }


    /**
     * Pick the child node with the highest expansion priority.
     *
     * @param node      Parent node
     * @return          A child node
     */
    protected UCTNode pickLeadChild(UCTNode parent) {
        UCTNode child = parent.child();
        UCTNode bestNode = parent.child();
        double factor = Math.log(parent.count());
        double bestScore = computePriority(child, factor);

        while ((child = child.sibling()) != null) {
            double score = computePriority(child, factor);

            if (score < bestScore) {
                bestScore = score;
                bestNode = child;
            }
        }

        return bestNode;
    }


    /**
     * Pick the expanded child with the worst score.
     *
     * @param node      Parent node
     * @return          Child node
     */
    private UCTNode pickFutileChild(UCTNode node) {
        UCTNode child = node.child();
        UCTNode futileNode = node.child();

        while ((child = child.sibling()) != null) {
            if (child.score() > futileNode.score()) {
                if (child.expanded() == true) {
                    futileNode = child;
                }
            }
        }

        return futileNode;
    }


    /**
     * Obtains a tree node for the given game position.
     *
     * @param game      Game state
     * @return          A root node
     */
    private UCTNode rootNode(Game game) {
        final long hash = game.hash();
        UCTNode node = root;

        if (node != null) {
            if (node.parent() != null) {
                node = node.parent();
            }

            if ((node = findNode(node, hash, 2)) != null) {
                return node;
            }
        }

        UCTNode root = new UCTNode(game, Game.NULL_MOVE);
        root.initScore(0.0);

        return root;
    }


    /**
     * Recursive lookup for a node given its hash code.
     *
     * @param node      Root node
     * @param hash      Hash of the searched node
     * @param depth     Maximum recursion depth
     *
     * @return          Matching node or null
     */
    private UCTNode findNode(UCTNode node, long hash, int depth) {
        if (node.hash() == hash) {
            return node;
        }

        UCTNode match = null;

        if (depth > 0 && (node = node.child()) != null) {
            match = findNode(node, hash, depth - 1);

            while (match == null && (node = node.sibling()) != null) {
                match = findNode(node, hash, depth - 1);
            }
        }

        return match;
    }


    /**
     * Scores the current game position for the given node.
     *
     * @param node      Tree node to evaluate
     * @param game      Game state for the node
     * @param depth     Maximum search depth
     *
     * @return          Score of the game
     */
    private int score(UCTNode node, Game game, int depth) {
        int score;

        if (node.terminal()) {
            score = game.outcome();
        } else if (leaves.find(game)) {
            score = leaves.getScore();
        } else {
            score = simulateMatch(game, depth);
        }

        if (score == Game.DRAW_SCORE) {
            score = contempt * turn;
        }

        return score * game.turn();
    }


    /**
     * Simulates a match and return its final score.
     *
     * @param game      Initial game state
     * @param depth     Maximum simulation depth
     * @return          Outcome of the simulation
     */
    protected int simulateMatch(Game game, int maxDepth) {
        return game.score();
    }


    /**
     * Evaluate a node and return its score.
     *
     * @param node      Node to evaluate
     * @param game      Game state for the node
     * @param depth     Maximum search depth
     *
     * @return          Score of the node
     */
    private double evaluate(UCTNode node, Game game, int depth) {
        final double score = score(node, game, depth);
        node.initScore(score);

        return score;
    }


    /**
     * Expands a node with a new child and returns its score.
     *
     * @param node      Node to expand
     * @param game      Game state
     * @param move      Move to perform
     *
     * @return          New child node
     */
    private UCTNode appendChild(UCTNode parent, Game game, int move) {
        final UCTNode node = new UCTNode(game, move);
        parent.pushChild(node);

        return node;
    }


    /**
     * Expands the most prioritary tree node.
     *
     * @param node      State node
     * @param game      Game state
     * @param depth     Depth limit
     */
    private double expand(UCTNode node, Game game, int depth) {
        final UCTNode child;
        final double score;

        if (node.terminal() || depth == 0) {
            score = node.score();
            node.increaseCount();

            return score;
        }

        int move = Game.NULL_MOVE;

        if (node.expanded() == false) {
            dequeueMoves(game);
            move = node.nextMove(game);
        }

        if (move != Game.NULL_MOVE) {
            game.makeMove(move);
            child = appendChild(node, game, move);
            score = -evaluate(child, game, depth - 1);
        } else {
            child = pickLeadChild(node);
            queueMove(child.move());
            score = -expand(child, game, depth - 1);
        }

        if (child.terminal() == false) {
            node.updateScore(score);
        } else if (score == maxScore) {
            node.settleScore(score);
        } else if (score == -maxScore && node.expanded()) {
            node.proveScore(score);
        } else {
            node.updateScore(score);
        }

        return score;
    }


    /**
     * Notifies registered consumers of a state change.
     *
     * @param game          Game state before a search
     * @param bestMove      Best move found so far
     */
    protected void invokeConsumers(Game game) {
        Report report = new UCTReport(this, game, root);

        for (Consumer<Report> consumer : consumers) {
            consumer.accept(report);
        }
    }


    /**
     * If the free memory is below a certain threshold prunes one or more
     * nodes from the tree so they can be garbage collected.
     *
     * @param root  Root of the tree
     */
    private void pruneGarbage(UCTNode root) {
        if (Runtime.getRuntime().freeMemory() < PRUNE_MEMORY_LIMIT) {
            for (int i = 0; i < PRUNE_ITERATIONS; i++) {
                pruneChilds(root, root);

                if (root.parent() != null) {
                    pruneChilds(root.parent(), root);
                }
            }

            System.gc();
        }
    }


    /**
     * Prunes one ore more leafs from each subtree of a parent node.
     *
     * @param parent    Parent node to prune
     * @param ignore    Do not prune this child
     */
    private void pruneChilds(UCTNode parent, UCTNode ignore) {
        UCTNode node = parent.child();

        do {
            if (node.expanded() && node != ignore) {
                while (node.expanded()) {
                    node = pickFutileChild(node);
                }

                if (node.parent() != ignore) {
                    node.parent().detachChildren();
                }
            }
        } while ((node = node.sibling()) != null);
    }


    /**
     * Perform the queued traveled path on a game object.
     *
     * @param game      Game where to perform the moves
     */
    private void dequeueMoves(Game game) {
        int[] moves = game.moves();
        int floor = floor(moves, queue, offset);
        int length = offset + index;

        game.unmakeMoves(moves.length - floor);

        for (int i = floor; i < length; i++) {
            game.makeMove(queue[i - offset]);
        }
    }


    /**
     * Obtain the index of the first move that differs between a
     * list of moves and a traveled path queue.
     *
     * @param moves     Game moves
     * @param queue     Moves queue
     * @param offset    First moves index to compare
     */
    private int floor(int[] moves, int[] queue, int offset) {
        int i = offset;
        int ceil = Math.min(moves.length - offset, index);
        while (i < ceil && moves[i] == queue[i - offset]) i++;
        return i;
    }


    /**
     * Adds a move to the traveled path queue.
     *
     * @param move      Move to append to the queue
     */
    private void queueMove(int move) {
        queue[index++] = move;
    }


    /**
     * Remove all nodes from the traveled path queue.
     */
    private void clearMovesQueue() {
        index = 0;
    }
}
