/*
 * Copyright (C) 2001-2002  Michel Ishizuka  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY MICHEL ISHIZUKA ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MICHEL
 * ISHIZUKA BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jp.gr.java_conf.dangan.util.lha;


/**
 * Class handling dynamic Huffman coding.
 *
 * <pre>
 * -- revision history --
 * $Log: DynamicHuffman.java,v $
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     In addLeaf(), when the number of leaves increases from 1 to 2,
 *     it was assumed that the weight of the original leaf was 1.
 * [change]
 *     In constructor DynamicHuffman( int, int ), changed to pass the initial
 *     number of leaves instead of the initial size of the Huffman tree.
 * [maintenance]
 *     Source cleanup.
 *     Eliminated tabs.
 *     Updated license text.
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class DynamicHuffman implements Cloneable {

    /**
     * Indicates the root of the Huffman tree.
     */
    public static final int ROOT = 0;

    /**
     * Weight threshold to rebuild the Huffman tree.
     */
    private static final int MAX_WEIGHT = 0x8000;

    /**
     * Indicates the weight of the node at index.
     */
    private int[] weight;

    /**
     * Holds child node numbers for the node at index.
     * To utilize sibling property:
     * {@code child} is the node number of smaller node,
     * {@code child - 1} is the node number of larger node.
     * For a leaf node, bitwise inverted data is stored.
     */
    private int[] child;

    /**
     * Holds parent node numbers for the node at index.
     */
    private int[] parent;

    /**
     * Holds node numbers of leaves.
     */
    private int[] leafs;

    /**
     * Current size of the Huffman tree.
     */
    private int size;

    /**
     * Default constructor.
     * Cannot be used.
     */
    private DynamicHuffman() {
    }

    /**
     * Constructor.
     *
     * @param count number of leaves
     */
    public DynamicHuffman(int count) {
        this(count, count);
    }

    /**
     * Constructor.
     *
     * @param max maximum number of leaves
     * @param first initial number of leaves
     */
    public DynamicHuffman(int max, int first) {
        if (1 <= first && first <= max) {

            this.weight = new int[max * 2 - 1];
            this.child = new int[max * 2 - 1];
            this.parent = new int[max * 2 - 1];
            this.leafs = new int[max];
            this.size = Math.max(0, first * 2 - 1);

            // Generate leaves.
            int node = this.size - 1;
            for (int code = 0; code < first; code++, node--) {
                this.weight[node] = 1;
                this.child[node] = ~code;
                this.leafs[code] = node;
            }

            // Generate branches.
            int child = this.size - 1;
            while (0 <= node && node != child) {
                this.weight[node] = this.weight[child] + this.weight[child - 1];

                this.child[node] = child;
                this.parent[child] = this.parent[child - 1] = node;

                child -= 2;
                node--;
            }
        } else if (max < first) {
            throw new IllegalArgumentException("\"max\" must be larger than \"first\".");
        } else {
            throw new IllegalArgumentException("\"first\" must be one or more.");
        }
    }

    /**
     * Creates and returns a copy of this object with its current state.
     *
     * @return a copy having the current state of this object
     */
    @Override
    public Object clone() {
        DynamicHuffman clone = new DynamicHuffman();
        clone.weight = this.weight.clone();
        clone.child = this.child.clone();
        clone.parent = this.parent.clone();
        clone.leafs = this.leafs.clone();
        clone.size = this.size;
        return clone;
    }

    /**
     * Gets the node number from data.
     *
     * @param code data
     * @return node number of {@code code}
     */
    public int codeToNode(int code) {
        return this.leafs[code];
    }

    /**
     * If the node is not a leaf, gets the node number of its child node;
     * if the node is a leaf, gets all-bits-inverted value of the data held by the node.
     * Since the child node numbers use the sibling property,<br>
     * for node 0's child node, it is {@code childNode(node)}<br>
     * for node 1's child node, it is {@code childNode(node) - 1}<br>
     *
     * @param node node
     * @return node number of the child node of {@code node}
     */
    public int childNode(int node) {
        return this.child[node];
    }

    /**
     * Gets the node number of the parent node of {@code node}.
     *
     * @param node node
     * @return node number of the parent node of {@code node}
     */
    public int parentNode(int node) {
        return this.parent[node];
    }

    /**
     * Updates the Huffman tree so that the weight of {@code code} increases.
     *
     * @param code leaf to increase weight
     */
    public void update(int code) {
        if (this.weight[DynamicHuffman.ROOT] == DynamicHuffman.MAX_WEIGHT) {
            this.rebuildTree();
        }

        int node = this.leafs[code];
        while (DynamicHuffman.ROOT != node) {
            int swapNode = node;
            while (this.weight[swapNode - 1] == this.weight[node] && DynamicHuffman.ROOT < swapNode - 1) {
                swapNode--;
            }

            if (node != swapNode)
                this.swap(node, swapNode);

            this.weight[swapNode]++;
            node = this.parent[swapNode];
        }
        this.weight[DynamicHuffman.ROOT]++;
    }

    /**
     * Adds a leaf representing {@code code} to the Huffman tree.
     *
     * @param code code represented by the leaf
     * @exception IllegalStateException if leaf cannot be added because the Huffman tree is full
     */
    public void addLeaf(int code) {
        if (this.size < this.weight.length - 1) {
            int last = this.size - 1;
            int large = this.size;
            int small = this.size + 1;
            this.child[large] = this.child[last];
            this.child[small] = ~code;
            this.child[last] = small;
            this.weight[large] = this.weight[last];
            this.weight[small] = 0;
            this.leafs[~this.child[large]] = large;
            this.leafs[~this.child[small]] = small;
            this.parent[large] = this.parent[small] = last;
            this.size = small + 1;

            if (last == DynamicHuffman.ROOT) {
                this.weight[last] -= 1;
            }

            this.update(code);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Rebuilds the Huffman tree.
     * Called from {@link #update(int)} when the weight exceeds the private constant {@link #MAX_WEIGHT}.
     * Halves approximately the weight of all nodes.
     */
    private void rebuildTree() {
        int leafCount = 0;
        for (int i = 0; i < this.size; i++)
            if (this.child[i] < 0) {
                this.weight[leafCount] = (this.weight[i] + 1) / 2;
                this.child[leafCount] = this.child[i];
                leafCount++;
            }

        leafCount--;
        int position = this.size - 1;
        int leafPosition = this.size - 2;
        while (0 <= position) {
            while (leafPosition <= position) {
                this.leafs[~this.child[leafCount]] = position;
                this.weight[position] = this.weight[leafCount];
                this.child[position--] = this.child[leafCount--];
            }

            int weight = this.weight[leafPosition] + this.weight[leafPosition + 1];

            while (0 <= leafCount && this.weight[leafCount] <= weight) {
                this.leafs[~this.child[leafCount]] = position;
                this.weight[position] = this.weight[leafCount];
                this.child[position--] = this.child[leafCount--];
            }

            this.weight[position] = weight;
            this.child[position] = leafPosition + 1;
            this.parent[leafPosition] = this.parent[leafPosition + 1] = position;

            position--;
            leafPosition -= 2;
        }
    }

    /**
     * Swaps node at node number {@code i} and node at node number {@code j}.
     *
     * @param i node to swap
     * @param j node to swap
     */
    private void swap(int i, int j) {
        if (this.child[i] < 0) {
            this.leafs[~this.child[i]] = j;
        } else {
            this.parent[this.child[i]] = this.parent[this.child[i] - 1] = j;
        }

        if (this.child[j] < 0) {
            this.leafs[~this.child[j]] = i;
        } else {
            this.parent[this.child[j]] = this.parent[this.child[j] - 1] = i;
        }

        int temp = this.child[i];
        this.child[i] = this.child[j];
        this.child[j] = temp;

        temp = this.weight[i];
        this.weight[i] = this.weight[j];
        this.weight[j] = temp;
    }
}
