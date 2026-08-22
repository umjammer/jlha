/*
 * Copyright (C) 2002  Michel Ishizuka  All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import jp.gr.java_conf.dangan.lang.reflect.Factory;


/**
 * Implementation of {@link LzssSearchMethod} using hash and binary tree.<br>
 *
 * <pre>
 * Reference:
 * Data Compression Handbook [Revised 2nd Edition]
 *        Written by M. Nelson / J.-L. Gailly
 *        Translated by Takeshi Hagiwara / Suguru Yamaguchi
 *                  ISBN4-8101-8605-9
 *                             5728 yen (excluding tax, price at time of purchase)
 * </pre>
 *
 * was used as reference.<br>
 * With a binary tree, while the longest match can be found,
 * it is not guaranteed to find the closest match.
 * Therefore, in compression methods like -lh5- that exploit
 * the tendency of LZSS match positions to be clustered nearby,
 * the compression ratio may be somewhat degraded.
 *
 * <pre>
 * -- revision history --
 * $Log: HashAndBinaryTreeSearch.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     Interface change to match {@link LzssSearchMethod} interface change
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     license comment update
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class HashAndBinaryTreeSearch implements LzssSearchMethod {

    /**
     * Value indicating that it is not used.
     * If {@code parent[node]} is {@link #UNUSED}, {@code node} is an unused node.
     * If {@code small[node]} or {@code large[node]} is {@link #UNUSED},
     * it indicates that {@code node} does not have a child node on that side.
     */
    private static final int UNUSED = -1;

    /**
     * Value indicating the root of the binary tree.
     * If {@code parent[node]} is {@link #ROOT_NODE}, {@code node} is the root of the binary tree.
     */
    private static final int ROOT_NODE = -2;

    /**
     * LZSS dictionary size.
     */
    private final int dictionarySize;

    /**
     * Value used for LZSS compression.
     * Indicates maximum match length.
     */
    private final int maxMatch;

    /**
     * Threshold used for LZSS compression.
     * If the match length is greater than or equal to this value, compression code is output.
     */
    private final int threshold;

    /**
     * Buffer for applying LZSS compression.
     * The first half is the dictionary area,
     * and the second half is the buffer containing data to be compressed.
     * Only read access is permitted within implementations of {@link LzssSearchMethod}.
     */
    private final byte[] textBuffer;

    /**
     * Dictionary limit position.
     * When there is no data in the dictionary area in the first half of {@code TextBuffer},
     * this prevents compression from using undefined data (0 in Java) in the dictionary area.
     */
    private int dictionaryLimit;

    /**
     * Hash method
     */
    private final HashMethod hashMethod;

    /**
     * Hash table
     * Index is hash value, content is the start position of the data pattern
     * of the binary tree root having each hash value.
     */
    private final int[] hashTable;

    /**
     * Indicates the start position of the parent's data pattern.
     * Index is node number, content is start position of the parent node's data pattern.
     */
    private final int[] parent;

    /**
     * Indicates the start position of the small child's data pattern.
     * Index is node number, content is start position of the small child node's data pattern.
     */
    private final int[] small;

    /**
     * Indicates the start position of the large child's data pattern.
     * Index is node number, content is start position of the large child node's data pattern.
     */
    private final int[] large;

//    /**
//     * Buffer for slide
//     */
//     private int[] dummy;

    /**
     * Constructs a search mechanism using hash and binary tree.<br>
     * The default hash method is used.
     *
     * @param dictionarySize dictionary size
     * @param maxMatch maximum match length
     * @param threshold threshold between compression and non-compression
     * @param textBuffer buffer for applying LZSS compression
     */
    public HashAndBinaryTreeSearch(int dictionarySize, int maxMatch, int threshold, byte[] textBuffer) {

        this(dictionarySize, maxMatch, threshold, textBuffer, HashShort.class.getName());
    }

    /**
     * Constructs a {@link LzssSearchMethod} using hash and binary tree.
     *
     * @param dictionarySize dictionary size
     * @param maxMatch maximum match length
     * @param threshold threshold between compression and non-compression
     * @param textBuffer buffer for applying LZSS compression
     * @param hashMethodClassName class name providing the {@link HashMethod}
     *
     * @exception NoClassDefFoundError if the class specified by {@code hashMethodClassName} is not found.
     * @exception InstantiationError if an instance cannot be created because the class specified by {@code hashMethodClassName} is an abstract class.
     * @exception NoSuchMethodError if the class specified by {@code hashMethodClassName} does not have the constructor {@code HashMethod(byte[])}.
     */
    public HashAndBinaryTreeSearch(int dictionarySize,
            int maxMatch,
            int threshold,
            byte[] textBuffer,
            String hashMethodClassName) {

        this.dictionarySize = dictionarySize;
        this.maxMatch = maxMatch;
        this.threshold = threshold;
        this.textBuffer = textBuffer;
        this.dictionaryLimit = this.dictionarySize;

        try {
            this.hashMethod = (HashMethod) Factory.createInstance(hashMethodClassName, (Object) textBuffer);
        } catch (ClassNotFoundException exception) {
            throw new NoClassDefFoundError(exception.getMessage());
        } catch (InvocationTargetException exception) {
            throw new Error(exception.getTargetException().getMessage());
        } catch (NoSuchMethodException exception) {
            throw new NoSuchMethodError(exception.getMessage());
        } catch (InstantiationException exception) {
            throw new InstantiationError(exception.getMessage());
        }

        // Initialize hash table
        this.hashTable = new int[this.hashMethod.tableSize()];
        Arrays.fill(this.hashTable, HashAndBinaryTreeSearch.UNUSED);

        // Initialize binary tree
        this.parent = new int[dictionarySize];
        this.large = new int[dictionarySize];
        this.small = new int[dictionarySize];
        Arrays.fill(this.parent, HashAndBinaryTreeSearch.UNUSED);
    }

    /**
     * Registers the data pattern starting from {@code position}
     * into the search mechanism using hash and binary tree.
     *
     * @param position start position of the data pattern in {@link #textBuffer}
     */
    @Override
    public void put(int position) {

        // Delete the oldest data pattern from the binary tree
        this.deleteNode(position - this.dictionarySize);

        // Search for the position to insert position into the binary tree
        int hash = this.hashMethod.hash(position);
        int parentpos = this.hashTable[hash];
        int scanpos = this.hashTable[hash];

        byte[] buf = this.textBuffer;
        int max = position + this.maxMatch;
        int p = 0;
        int s;
        while (scanpos != HashAndBinaryTreeSearch.UNUSED) {

            s = scanpos;
            p = position;
            while (buf[s] == buf[p]) {
                s++;
                p++;
                if (max <= p) {
                    // Full match found
                    this.replaceNode(scanpos, position);
                    return;
                }
            }

            parentpos = scanpos;
            scanpos = (buf[s] < buf[p] ? this.large[scanpos & (this.dictionarySize - 1)]
                                       : this.small[scanpos & (this.dictionarySize - 1)]);
        }

        // Register the data pattern starting from position into the binary tree
        if (this.hashTable[hash] != HashAndBinaryTreeSearch.UNUSED) {
            this.addNode(parentpos, position, p - position);
        } else {
            this.hashTable[hash] = position;
            int node = position & (this.dictionarySize - 1);
            this.parent[node] = HashAndBinaryTreeSearch.ROOT_NODE;
            this.small[node] = HashAndBinaryTreeSearch.UNUSED;
            this.large[node] = HashAndBinaryTreeSearch.UNUSED;
        }
    }

    /**
     * Searches for the data pattern with the longest match to the data pattern
     * starting from {@code position} among the data patterns registered in the
     * search mechanism using hash and binary tree, and simultaneously registers
     * the data pattern starting from {@code position} into the search mechanism
     * using hash and binary tree.
     *
     * @param position start position of the data pattern in {@link #textBuffer}.
     * @return If a match is found, a value holding information of the match position and match length
     *         generated by {@link LzssOutputStream#createSearchReturn};
     *         if no match is found, {@link LzssOutputStream#NOMATCH}.
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int searchAndPut(int position) {

        // Delete the oldest data pattern from the binary tree
        this.deleteNode(position - this.dictionarySize);

        // Search for the longest match from the binary tree
        int hash = this.hashMethod.hash(position);
        int matchlen = -1;
        int matchpos = this.hashTable[hash];
        int parentpos = this.hashTable[hash];
        int scanpos = this.hashTable[hash];

        byte[] buf = this.textBuffer;
        int max = position + this.maxMatch;
        int p;
        int s;
        int len = 0;
        while (scanpos != HashAndBinaryTreeSearch.UNUSED) {
            s = scanpos;
            p = position;
            while (buf[s] == buf[p]) {
                s++;
                p++;
                if (max <= p) {
                    // Full match found
                    this.replaceNode(matchpos, position);
                    return LzssOutputStream.createSearchReturn(matchlen, matchpos);
                }
            }

            len = p - position;
            if (matchlen < len) {
                matchpos = scanpos;
                matchlen = len;
            } else if (matchlen == len && matchpos < scanpos) {
                matchpos = scanpos;
            }

            parentpos = scanpos;
            scanpos = (buf[s] < buf[p] ? this.large[scanpos & (this.dictionarySize - 1)]
                                       : this.small[scanpos & (this.dictionarySize - 1)]);
        }

        // Register the data pattern starting from position into the binary tree
        if (this.hashTable[hash] != HashAndBinaryTreeSearch.UNUSED) {
            this.addNode(parentpos, position, len);
        } else {
            this.hashTable[hash] = position;
            int node = position & (this.dictionarySize - 1);
            this.parent[node] = HashAndBinaryTreeSearch.ROOT_NODE;
            this.small[node] = HashAndBinaryTreeSearch.UNUSED;
            this.large[node] = HashAndBinaryTreeSearch.UNUSED;
        }

        // Compare with the farthest data pattern
        // deleted at the beginning of the method
        scanpos = position - this.dictionarySize;
        if (this.dictionaryLimit <= scanpos) {
            len = 0;
            while (this.textBuffer[scanpos + len] == this.textBuffer[position + len]) {
                if (this.maxMatch <= ++len)
                    break;
            }

            if (matchlen < len) {
                matchpos = scanpos;
                matchlen = len;
            }
        }

        // Return the longest match to caller.
        if (this.threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * Searches registered data patterns in the search mechanism using hash and binary tree
     * and gets the one with the longest match to the data pattern starting from {@code position}.<br>
     * For {@code position} where {@code textBuffer.length < position + maxMatch},
     * the binary tree is not fully traversed, so the longest match is not guaranteed to be obtained.
     *
     * @param position start position of the data pattern in {@link #textBuffer}.
     * @param lastPutPos start position of the last registered data pattern.
     * @return If a match is found, a value holding information of the match position and match length
     *         generated by {@link LzssOutputStream#createSearchReturn};
     *         if no match is found, {@link LzssOutputStream#NOMATCH}.
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int search(int position, int lastPutPos) {

        // Search data patterns not registered in the binary tree
        // using simple sequential search.
        int matchlen = this.threshold - 1;
        int matchpos = position;
        int scanpos = position - 1;
        int scanlimit = Math.max(this.dictionaryLimit, lastPutPos);

        byte[] buf = this.textBuffer;
        int max = Math.min(this.textBuffer.length, position + this.maxMatch);
        int s;
        int p;
        int len = 0;
        while (scanlimit < scanpos) {
            s = scanpos;
            p = position;
            while (buf[s] == buf[p]) {
                s++;
                p++;
                if (max <= p)
                    break;
            }

            if (matchlen < len) {
                matchpos = scanpos;
                matchlen = len;
            }
            scanpos--;
        }

        // Search the binary tree
        if (this.hashMethod.hashRequires() <= this.textBuffer.length - position) {
            int hash = this.hashMethod.hash(position);
            scanpos = this.hashTable[hash];
            scanlimit = Math.max(this.dictionaryLimit, position - this.dictionarySize);
            while (scanpos != HashAndBinaryTreeSearch.UNUSED) {
                s = scanpos;
                p = position;
                while (buf[s] == buf[p]) {
                    s++;
                    p++;
                    if (max <= p)
                        break;
                }

                if (p < max) {
                    len = p - position;
                    if (scanlimit <= scanpos) {
                        if (matchlen < len) {
                            matchpos = scanpos;
                            matchlen = len;
                        } else if (matchlen == len && matchpos < scanpos) {
                            matchpos = scanpos;
                        }
                    }
                    scanpos = (buf[s] < buf[p] ? this.large[scanpos & (this.dictionarySize - 1)]
                                               : this.small[scanpos & (this.dictionarySize - 1)]);
                } else {
                    break;
                }
            }
        }

        // Return the longest match to caller.
        if (this.threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * When sliding data in {@link #textBuffer} up to {@code position} forward,
     * performs the process of shifting data constituting the search mechanism using hash and binary tree
     * forward accordingly so as not to contradict data in {@link #textBuffer}.
     */
    @Override
    public void slide() {
        this.dictionaryLimit = Math.max(0, this.dictionaryLimit - this.dictionarySize);

        this.slideTree(this.hashTable);
        this.slideTree(this.parent);
        this.slideTree(this.small);
        this.slideTree(this.large);
    }

    /**
     * Gets the amount of data required when registering a data pattern
     * into the binary tree using {@link #put(int)} or {@link #searchAndPut(int)}.<br>
     * The binary tree requires all ({@link #maxMatch} bytes) data that make up the data pattern upon registration.
     *
     * @return {@link #maxMatch} specified in constructor
     */
    @Override
    public int putRequires() {
        return this.maxMatch;
    }

    /**
     * Registers the data pattern starting from {@code position} into the binary tree
     * as a child of the data pattern at {@code parentpos}.<br>
     * The data patterns at {@code parentpos} and {@code position} match by {@code len} bytes.
     * The node at {@code position} must be put into the {@link #UNUSED} state in advance by {@link #deleteNode(int)}, etc.
     *
     * @param parentpos start position in {@link #textBuffer} of the parent's data pattern
     * @param position start position in {@link #textBuffer} of the newly added data pattern
     * @param len match length between the parent's data pattern and the newly added data pattern
     */
    private void addNode(int parentpos, int position, int len) {
        int parentnode = parentpos & (this.dictionarySize - 1);
        int node = position & (this.dictionarySize - 1);

        if (this.textBuffer[parentpos + len] < this.textBuffer[position + len]) {
            this.large[parentnode] = position;
        } else {
            this.small[parentnode] = position;
        }
        this.parent[node] = parentpos;
        this.small[node] = HashAndBinaryTreeSearch.UNUSED;
        this.large[node] = HashAndBinaryTreeSearch.UNUSED;
    }

    /**
     * Deletes the data pattern starting from {@code position} from the binary tree.
     *
     * @param position start position of the data pattern to delete
     */
    private void deleteNode(int position) {
        int node = position & (this.dictionarySize - 1);

        if (this.parent[node] != HashAndBinaryTreeSearch.UNUSED) {
            if (this.small[node] == HashAndBinaryTreeSearch.UNUSED && this.large[node] == HashAndBinaryTreeSearch.UNUSED) {
                this.contractNode(position, HashAndBinaryTreeSearch.UNUSED);
            } else if (this.small[node] == HashAndBinaryTreeSearch.UNUSED) {
                this.contractNode(position, this.large[node]);
            } else if (this.large[node] == HashAndBinaryTreeSearch.UNUSED) {
                this.contractNode(position, this.small[node]);
            } else {
                int replace = this.findNext(position);
                this.deleteNode(replace);
                this.replaceNode(position, replace);
            }
        }
    }

    /**
     * Replaces {@code oldpos}, which has only {@code newpos} as a child, with {@code newpos}.
     * {@code oldpos} is deleted from the binary tree.
     *
     * @param oldpos start position of the data pattern to delete
     * @param newpos start position of the data pattern replacing {@code oldpos}
     */
    private void contractNode(int oldpos, int newpos) {
        int oldnode = oldpos & (this.dictionarySize - 1);
        int newnode = newpos & (this.dictionarySize - 1);
        int parentpos = this.parent[oldnode];
        int parentnode = parentpos & (this.dictionarySize - 1);

        if (parentpos != HashAndBinaryTreeSearch.ROOT_NODE) {
            if (oldpos == this.small[parentnode]) {
                this.small[parentnode] = newpos;
            } else {
                this.large[parentnode] = newpos;
            }
        } else {
            this.hashTable[this.hashMethod.hash(oldpos)] = newpos;
        }

        if (newpos != HashAndBinaryTreeSearch.UNUSED) {
            this.parent[newnode] = parentpos;
        }

        this.parent[oldnode] = HashAndBinaryTreeSearch.UNUSED;
    }

    /**
     * Replaces {@code oldpos} with new data pattern {@code newpos} not contained in the binary tree.
     * If {@code newpos} is contained in the binary tree, it must be removed from the binary tree
     * in advance by e.g. calling {@code deleteNode(newpos)}.
     * {@code oldpos} is deleted from the binary tree.
     *
     * @param oldpos start position of the data pattern to delete
     * @param newpos start position of the data pattern replacing {@code oldpos}
     */
    private void replaceNode(int oldpos, int newpos) {
        int oldnode = oldpos & (this.dictionarySize - 1);
        int newnode = newpos & (this.dictionarySize - 1);
        int parentpos = this.parent[oldnode];
        int parentnode = parentpos & (this.dictionarySize - 1);

        if (parentpos != HashAndBinaryTreeSearch.ROOT_NODE) {
            if (oldpos == this.small[parentnode]) {
                this.small[parentnode] = newpos;
            } else {
                this.large[parentnode] = newpos;
            }
        } else {
            this.hashTable[this.hashMethod.hash(oldpos)] = newpos;
        }

        this.parent[newnode] = parentpos;
        this.small[newnode] = this.small[oldnode];
        this.large[newnode] = this.large[oldnode];
        if (this.small[newnode] != HashAndBinaryTreeSearch.UNUSED) {
            this.parent[this.small[newnode] & (this.dictionarySize - 1)] = newpos;
        }
        if (this.large[newnode] != HashAndBinaryTreeSearch.UNUSED) {
            this.parent[this.large[newnode] & (this.dictionarySize - 1)] = newpos;
        }

        this.parent[oldnode] = HashAndBinaryTreeSearch.UNUSED;
        this.large[oldnode] = HashAndBinaryTreeSearch.UNUSED;
        this.small[oldnode] = HashAndBinaryTreeSearch.UNUSED;
    }

    /**
     * When {@code deleteNode(position)} is called and both {@code small} and {@code large} leaves are found,
     * searches for the start position of the data pattern that should replace the data pattern starting from {@code position}.
     *
     * @param position start position of the data pattern to be replaced
     *
     * @return start position of the data pattern that should replace the data pattern starting from {@code position}
     */
    private int findNext(int position) {
        int node = position & (this.dictionarySize - 1);

        position = this.small[node];
        node = position & (this.dictionarySize - 1);
        while (HashAndBinaryTreeSearch.UNUSED != this.large[node]) {
            position = this.large[node];
            node = position & (this.dictionarySize - 1);
        }

        return position;
    }

    /**
     * Used to shift each element of the binary tree during {@link #slide()}.
     *
     * @param array array to traverse
     */
    private void slideTree(int[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = (0 <= array[i] ? array[i] - this.dictionarySize : array[i]);
        }
    }
}
