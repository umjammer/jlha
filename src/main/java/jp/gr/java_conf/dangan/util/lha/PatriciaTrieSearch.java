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

import java.math.BigInteger;
import java.util.Arrays;

import jp.gr.java_conf.dangan.io.Bits;


/**
 * Implementation of {@link LzssSearchMethod} using PATRICIA Trie.
 *
 * <pre>
 * -- revision history --
 * $Log: PatriciaTrieSearch.java,v $
 * Revision 1.2  2002/12/10 22:28:55  dangan
 * [bug fix]
 *     Fixed put( DictionarySize * 2 )
 *     and searchAndPut( DictionarySize * 2 ) not being supported.
 *
 * Revision 1.1  2002/12/04 00:00:00  dangan
 * [change]
 *     Interface change to match {@link LzssSearchMethod} interface change.
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/08/15 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     Fixed forgetting to link to linked list from hashtable in contractNode.
 *     Fixed accessing array with PatriciaTrieSearch.ROOT_NODE(-1).
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     license comment update
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.2 $
 */
public class PatriciaTrieSearch implements LzssSearchMethod {

    /**
     * Value indicating that it is not used.
     * If {@code parent[node]} is {@link #UNUSED}, {@code node} is an unused node.
     * If {@code prev[node]} or {@code next[node]} is {@link #UNUSED},
     * it indicates that there is no sibling node on that side.
     */
    private static final int UNUSED = 0;

    /**
     * LZSS dictionary size
     */
    private final int DictionarySize;

    /**
     * Value used for LZSS compression.
     * Indicates maximum match length.
     */
    private final int MaxMatch;

    /**
     * Threshold for LZSS compression/non-compression.
     * If the match length is greater than or equal to this value, compression code is output.
     */
    private final int Threshold;

    /**
     * Buffer for applying LZSS compression.
     * Only read access is permitted within implementations of {@link LzssSearchMethod}.
     */
    private final byte[] TextBuffer;

    /**
     * Dictionary limit position.
     * When there is no data in the dictionary area in the first half of {@link #TextBuffer},
     * this prevents compression from using undefined data (0 in Java) in the dictionary area.
     */
    private int DictionaryLimit;

    /**
     * Indicates parent node number.
     * {@code parent[node]} indicates the number of the parent node of {@code node}.
     */
    private final int[] parent;

    /**
     * Indicates hash value of children.
     * {@code hashTable[hash(node, ch)]} indicates the hash value of the child node of {@code node} for character {@code ch}.
     */
    private final int[] hashTable;

    /**
     * Part of doubly linked list chained from {@link #hashTable}.
     * Indicates node number of previous node having the same hash value.
     * {@code prev[node]} is the node number of the node positioned immediately before {@code node} in the linked list with the same hash value.
     * If {@code prev[node]} is negative, it indicates bitwise inverted hash value.
     */
    private final int[] prev;

    /**
     * Part of doubly linked list chained from {@link #hashTable}.
     * Indicates node number of next node having the same hash value.
     * {@code next[node]} is the node number of the node positioned immediately after {@code node} in the linked list with the same hash value.
     * <p>
     * For non-leaf nodes, {@code next} and {@link #avail} form a stack (singly linked list) of unused nodes.
     * <p>
     * Furthermore, for leaf nodes deleted due to full match, forms a singly linked list to leaf nodes existing within PATRICIA Trie.
     */
    private final int[] next;

    /**
     * Indicates start position of data pattern in {@link #TextBuffer} of the node.
     * {@code position[node]} indicates start position of the data pattern of {@code node}.
     */
    private final int[] position;

    /**
     * Indicates branch position of the node.
     * {@code level[node]} indicates the position where child nodes of {@code node} branch.
     */
    private final int[] level;

    /**
     * Indicates the number of child nodes of the node.
     * {@code childnum[node]} indicates the number of child nodes of {@code node}.
     */
    private final int[] childnum;

    /**
     * Stack pointer of unused node stack formed by {@link #next}.
     */
    private int avail;

    /**
     * Shift value used during hashing
     */
    private final int shift;

    /**
     * Longest match position in PATRICIA Trie obtained in the last {@link #searchAndPut(int)} or {@link #put(int)}
     */
    private int lastMatchPos;

    /**
     * Longest match length in PATRICIA Trie obtained in the last {@link #searchAndPut(int)} or {@link #put(int)}
     */
    private int lastMatchLen;

    /**
     * Constructor.
     * Constructs a search mechanism using PATRICIA Trie.
     *
     * @param DictionarySize dictionary size
     * @param MaxMatch maximum match length
     * @param Threshold threshold between compression and non-compression
     * @param TextBuffer buffer for applying LZSS compression
     */
    public PatriciaTrieSearch(int DictionarySize, int MaxMatch, int Threshold, byte[] TextBuffer) {

        this.DictionarySize = DictionarySize;
        this.MaxMatch = MaxMatch;
        this.Threshold = Threshold;
        this.TextBuffer = TextBuffer;
        this.DictionaryLimit = this.DictionarySize;

        this.parent = new int[this.DictionarySize * 2];
        this.prev = new int[this.DictionarySize * 2];
        this.next = new int[this.DictionarySize * 2];
        this.position = new int[this.DictionarySize];
        this.level = new int[this.DictionarySize];
        this.childnum = new int[this.DictionarySize];
        this.hashTable = new int[PatriciaTrieSearch.generateProbablePrime(this.DictionarySize + (this.DictionarySize >> 2))];

        for (int i = 2; i < this.DictionarySize; i++) {
            this.next[i] = i - 1;
        }
        this.avail = this.DictionarySize - 1;

        for (int i = 0; i < this.DictionarySize * 2; i++) {
            this.parent[i] = PatriciaTrieSearch.UNUSED;
        }

        Arrays.fill(this.hashTable, PatriciaTrieSearch.UNUSED);

        this.shift = Bits.len(this.DictionarySize) - 8;

        this.lastMatchLen = 0;
        this.lastMatchPos = 0;
    }

    /**
     * Registers the data pattern starting from {@code position}
     * into the PATRICIA Trie.
     *
     * @param position start position of the data pattern in {@link #TextBuffer}
     */
    @Override
    public void put(int position) {

        // Delete the oldest data pattern from PATRICIA Trie
        int posnode = (position & (this.DictionarySize - 1)) + this.DictionarySize;
        this.deleteNode(posnode);

        // Search longest match from PATRICIA Trie
        int matchnode;
        int matchpos;
        int scannode;
        int matchlen;
        if (3 < this.lastMatchLen) {

            // If the previous match length is greater than threshold,
            // search for match of lastMatchLen - 1 from leaves.
            scannode = (this.lastMatchPos + 1) | this.DictionarySize;

            // Processing when scannode was removed from PATRICIA Trie due to longest match
            while (this.parent[scannode] == PatriciaTrieSearch.UNUSED) {
                scannode = this.next[scannode];
            }

            // Traverse from leaves to parents sequentially
            // to find a node with level <= lastMatchLen - 1.
            int node = this.parent[scannode];
            this.lastMatchLen--;
            while (0 < node && this.lastMatchLen <= this.level[node]) {
                scannode = node;
                node = this.parent[node];
            }

            // Traverse further to parents and update position.
            while (0 < node) {
                this.position[node] = position;
                node = this.parent[node];
            }

            matchlen = this.lastMatchLen;
        } else {

            // Traverse PATRICIA Trie from root.
            scannode = this.child(this.TextBuffer[position] - 128, this.TextBuffer[position + 1] & 0xFF);
            matchlen = 2;

            if (scannode == PatriciaTrieSearch.UNUSED) {
                // Add position to root.
                this.attachNode(this.TextBuffer[position] - 128, posnode, this.TextBuffer[position + 1] & 0xFF);
                this.lastMatchLen = matchlen;
                return;
            }
        }

        while (true) {
            int max;
            if (scannode < this.DictionarySize) {
                max = this.level[scannode];
                matchnode = scannode;
                matchpos = this.position[scannode];
            } else {
                max = this.MaxMatch;
                matchnode = scannode;
                matchpos = (position <= scannode ? scannode - this.DictionarySize : scannode);
            }

            while (matchlen < max && (this.TextBuffer[matchpos + matchlen] == this.TextBuffer[position + matchlen])) {
                matchlen++;
            }

            if (matchlen == max && matchlen < this.MaxMatch) {
                this.position[scannode] = position;
                scannode = this.child(scannode, this.TextBuffer[position + matchlen] & 0xFF);

                if (scannode == PatriciaTrieSearch.UNUSED) {
                    this.attachNode(matchnode, posnode, this.TextBuffer[position + matchlen] & 0xFF);
                    break;
                } else {
                    matchlen++;
                }
            } else if (matchlen < max) {
                // Branch matchnode and position.
                this.splitNode(matchnode, matchpos, posnode, position, matchlen);
                break;
            } else {
                // Full match found, replace node.
                this.replaceNode(matchnode, posnode);
                this.next[matchnode] = position;
                break;
            }
        }

        // Save search result
        this.lastMatchLen = matchlen;
        this.lastMatchPos = matchpos;
    }

    /**
     * Searches for the data pattern with the longest match to the data pattern
     * starting from {@code position} among the data patterns registered in the
     * PATRICIA Trie, and simultaneously registers the data pattern starting
     * from {@code position} into the PATRICIA Trie.
     *
     * @param position start position of the data pattern in {@link #TextBuffer}.
     * @return If a match is found, a value holding information of the match position and match length
     *         generated by {@link LzssOutputStream#createSearchReturn(int, int)};
     *         if no match is found, {@link LzssOutputStream#NOMATCH}.
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int searchAndPut(int position) {

        // Delete the oldest data pattern from PATRICIA Trie
        int posnode = (position & (this.DictionarySize - 1)) + this.DictionarySize;
        this.deleteNode(posnode);

        // Search longest match from PATRICIA Trie
        int matchnode;
        int matchpos = position;
        int scannode;
        int matchlen;
        if (3 < this.lastMatchLen) {

            // If the previous match length is greater than threshold,
            // search for match of lastMatchLen - 1 from leaves.
            scannode = (this.lastMatchPos + 1) | this.DictionarySize;

            // Processing when scannode was removed from PATRICIA Trie due to longest match
            while (this.parent[scannode] == PatriciaTrieSearch.UNUSED) {
                scannode = this.next[scannode];
            }

            // Traverse from leaves to parents sequentially
            // to find a node with level <= lastMatchLen - 1.
            int node = this.parent[scannode];
            this.lastMatchLen--;
            while (0 < node && this.lastMatchLen <= this.level[node]) {
                scannode = node;
                node = this.parent[node];
            }

            // Traverse further to parents and update position.
            while (0 < node) {
                this.position[node] = position;
                node = this.parent[node];
            }

            matchlen = this.lastMatchLen;
        } else {
            // Traverse PATRICIA Trie from root.
            scannode = this.child(this.TextBuffer[position] - 128, this.TextBuffer[position + 1] & 0xFF);
            matchlen = 2;
        }

        // scannode == UNUSED only when lastMatchLen is smaller than threshold.
        if (scannode != PatriciaTrieSearch.UNUSED) {
            while (true) {
                int max;
                if (scannode < this.DictionarySize) {
                    max = this.level[scannode];
                    matchnode = scannode;
                    matchpos = this.position[scannode];
                } else {
                    max = this.MaxMatch;
                    matchnode = scannode;
                    matchpos = (position <= scannode ? scannode - this.DictionarySize : scannode);
                }

                while (matchlen < max && (this.TextBuffer[matchpos + matchlen] == this.TextBuffer[position + matchlen])) {
                    matchlen++;
                }

                if (matchlen == max && matchlen < this.MaxMatch) {
                    this.position[scannode] = position;
                    scannode = this.child(scannode, this.TextBuffer[position + matchlen] & 0xFF);

                    if (scannode == PatriciaTrieSearch.UNUSED) {
                        // Add position to matchnode.
                        this.attachNode(matchnode, posnode, this.TextBuffer[position + matchlen] & 0xFF);
                        break;
                    } else {
                        matchlen++;
                    }
                } else if (matchlen < max) {
                    // Branch matchnode and position.
                    this.splitNode(matchnode, matchpos, posnode, position, matchlen);
                    break;
                } else {
                    // Full match found, replace node.
                    this.replaceNode(matchnode, posnode);
                    this.next[matchnode] = position;
                    break;
                }
            }
        } else { // if( scannode != PatriciaTrieSearch.UNUSED )
                 // Add position to root.
            this.attachNode(this.TextBuffer[position] - 128, posnode, this.TextBuffer[position + 1] & 0xFF);
            matchlen = 0;
        }

        // Save search result
        this.lastMatchLen = matchlen;
        this.lastMatchPos = matchpos;

        // Also check data pattern deleted from PATRICIA Trie at beginning of method.
        scannode = position - this.DictionarySize;
        if (this.DictionaryLimit <= scannode) {
            int len = 0;
            while (this.TextBuffer[scannode + len] == this.TextBuffer[position + len])
                if (this.MaxMatch <= ++len)
                    break;

            if (matchlen < len) {
                matchpos = scannode;
                matchlen = len;
            }
        }

        // Return the longest match to caller.
        if (this.Threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * Searches registered data patterns in PATRICIA Trie and gets the one
     * with the longest match to the data pattern starting from {@code position}.<br>
     *
     * @param position start position of the data pattern in {@link #TextBuffer}.
     * @param lastPutPos start position of the last registered data pattern.
     * @return If a match is found, a value holding information of the match position and match length
     *         generated by {@link LzssOutputStream#createSearchReturn(int, int)};
     *         if no match is found, {@link LzssOutputStream#NOMATCH}.
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int search(int position, int lastPutPos) {

        // Search data patterns not registered in PATRICIA Trie
        // using simple sequential search.
        int scanlimit = Math.max(this.DictionaryLimit, lastPutPos);
        int scanpos = position - 1;
        int matchlen = 0;
        int matchpos = 0;

        byte[] buf = this.TextBuffer;
        int max = Math.min(this.TextBuffer.length, position + this.MaxMatch);
        int s;
        int p;
        int len;
        while (scanlimit < scanpos) {
            s = scanpos;
            p = position;
            while (buf[s] == buf[p]) {
                s++;
                p++;
                if (max <= p)
                    break;
            }

            len = p - position;
            if (matchlen < len) {
                matchpos = scanpos;
                matchlen = len;
                if (max <= p)
                    break;
            }
            scanpos--;
        }

        // Search PATRICIA Trie
        if (2 < this.TextBuffer.length - position) {
            int matchnode = this.child(this.TextBuffer[position] - 128, this.TextBuffer[position + 1] & 0xFF);
            scanlimit = Math.max(this.DictionaryLimit, position - this.DictionarySize);
            len = 2;
            while (matchnode != PatriciaTrieSearch.UNUSED) {
                int maxlen;
                if (matchnode < this.DictionarySize) {
                    maxlen = this.level[matchnode];
                    scanpos = this.position[matchnode];
                } else {
                    maxlen = this.MaxMatch;
                    scanpos = (lastPutPos < matchnode ? matchnode - this.DictionarySize : matchnode);
                }

                if (scanlimit <= scanpos) {
                    max = Math.min(this.TextBuffer.length, position + maxlen);
                    s = scanpos + len;
                    p = position + len;
                    if (p < max) {
                        while (buf[s] == buf[p]) {
                            s++;
                            p++;
                            if (max <= p)
                                break;
                        }
                    }

                    len = p - position;
                    if (matchlen < len) {
                        matchpos = scanpos;
                        matchlen = len;
                    }

                    if (len == maxlen && matchlen < this.MaxMatch) {
                        if (position + len < this.TextBuffer.length) {
                            matchnode = this.child(matchnode, this.TextBuffer[position + len] & 0xFF);

                            if (matchnode != PatriciaTrieSearch.UNUSED) {
                                len++;
                            }
                        } else {
                            break;
                        }
                    } else { // Match less than maxlen found or full match found
                        break;
                    }
                } else { // if( scanlimit <= scanpos ) Matched pattern exceeded search limit.
                    break;
                }
            }
        }

        // Return the longest match to caller.
        if (this.Threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * When sliding data in {@link #TextBuffer} up to {@code position} forward,
     * performs processing to shift data in {@link LzssSearchMethod} forward
     * accordingly so as not to contradict data in {@link #TextBuffer}.
     */
    @Override
    public void slide() {
        this.DictionaryLimit = Math.max(0, this.DictionaryLimit - this.DictionarySize);
        this.lastMatchPos -= this.DictionarySize;

        for (int i = 0; i < this.position.length; i++) {
            int pos = this.position[i] - this.DictionarySize;
            this.position[i] = Math.max(0, pos);
        }
    }

    /**
     * Gets the amount of data used when registering data into {@link LzssSearchMethod} with {@link #put(int)}.
     * {@link PatriciaTrieSearch} always returns {@link #MaxMatch}.
     *
     * @return always {@link #MaxMatch}
     */
    @Override
    public int putRequires() {
        return this.MaxMatch;
    }

    /**
     * Branches {@code oldnode} at {@code splitLen}.
     * A new node is created at the position where {@code oldnode} was,
     * and the new node has {@code oldnode} and {@code position} as children.
     *
     * @param oldnode node to branch
     * @param oldpos start position of data pattern pointed to by {@code oldnode}
     * @param posnode node for {@code position}
     * @param position start position of data pattern in {@link #TextBuffer}
     * @param splitLen branch position within data pattern
     */
    private void splitNode(int oldnode, int oldpos, int posnode, int position, int splitLen) {
        // Obtain new node from stack.
        int newnode = this.avail;
        this.avail = this.next[newnode];

        this.replaceNode(oldnode, newnode);
        this.level[newnode] = splitLen;
        this.position[newnode] = position;
        this.childnum[newnode] = 0;

        this.attachNode(newnode, oldnode, this.TextBuffer[oldpos + splitLen] & 0xFF);
        this.attachNode(newnode, posnode, this.TextBuffer[position + splitLen] & 0xFF);
    }

    /**
     * Deletes leaf {@code node} from PATRICIA Trie.
     * If necessary, also promotes parent node of {@code node}.
     *
     * @param node leaf node to delete
     */
    private void deleteNode(int node) {
        if (this.parent[node] != PatriciaTrieSearch.UNUSED) {
            int parent = this.parent[node];
            int prev = this.prev[node];
            int next = this.next[node];

            this.parent[node] = PatriciaTrieSearch.UNUSED;
            this.prev[node] = PatriciaTrieSearch.UNUSED;
            this.next[node] = PatriciaTrieSearch.UNUSED;

            if (0 <= prev) {
                this.next[prev] = next;
            } else {
                this.hashTable[~prev] = next;
            }
            this.prev[next] = prev;

            if (0 < parent) { // Conditional expression that is true when parent is not the root of PATRICIA Trie
                this.childnum[parent]--;

                if (this.childnum[parent] <= 1) {
                    this.contractNode(this.child(parent, this.TextBuffer[this.position[parent] + this.level[parent]] & 0xFF));
                }
            }
        }
    }

    /**
     * Adds {@code childnode} to {@code parentnode}.
     *
     * @param parentnode target parent node to add {@code childnode} to
     * @param childnode node to add to {@code parentnode}
     * @param ch current processing position in {@link #TextBuffer}.
     *            Used to determine leaf {@code position}.
     */
    private void attachNode(int parentnode, int childnode, int ch) {
        int hash = this.hash(parentnode, ch);
        int node = this.hashTable[hash];
        this.hashTable[hash] = childnode;
        this.parent[childnode] = parentnode;
        this.prev[childnode] = ~hash;
        this.next[childnode] = node;
        this.prev[node] = childnode;

        if (0 < parentnode) {
            this.childnum[parentnode]++;
        }
    }

    /**
     * Swaps {@code oldnode} and {@code newnode}.
     * {@code newnode} maintains relationship with child nodes.
     * {@code oldnode} is replaced and removed from PATRICIA Trie.
     *
     * @param oldnode node replaced and deleted from Trie
     * @param newnode node connected to the position where {@code oldnode} was
     */
    private void replaceNode(int oldnode, int newnode) {
        this.parent[newnode] = this.parent[oldnode];
        this.prev[newnode] = this.prev[oldnode];
        this.next[newnode] = this.next[oldnode];

        this.prev[this.next[newnode]] = newnode;

        if (this.prev[newnode] < 0) {
            this.hashTable[~this.prev[newnode]] = newnode;
        } else {
            this.next[this.prev[newnode]] = newnode;
        }

        this.parent[oldnode] = PatriciaTrieSearch.UNUSED;
        this.prev[oldnode] = PatriciaTrieSearch.UNUSED;
        this.next[oldnode] = PatriciaTrieSearch.UNUSED;
    }

    /**
     * Promotes {@code node} whose siblings are gone.
     * Parent node of {@code node} is deleted from PATRICIA Trie,
     * and {@code node} is connected to that position instead.
     * Determination of whether siblings are gone is made by caller.
     *
     * @param node node to promote
     */
    private void contractNode(int node) {
        int parentnode = this.parent[node];

        this.prev[this.next[node]] = this.prev[node];
        if (0 <= this.prev[node]) {
            this.next[this.prev[node]] = this.next[node];
        } else {
            this.hashTable[~this.prev[node]] = this.next[node];
        }
        this.replaceNode(parentnode, node);

        // Return unused parentnode to stack.
        this.next[parentnode] = this.avail;
        this.avail = parentnode;
    }

    /**
     * Shifts each element of Trie during {@link #slide()}.
     *
     * @param src copy source
     * @param dst copy destination
     * @param width shift width
     */
    @SuppressWarnings("unused")
    private void slideTree(int[] src, int[] dst, int width) {
        for (int i = 0; i < this.DictionarySize; i++)
            dst[i] = (src[i] < this.DictionarySize ? src[i]
                                                   : ((src[i] - width) & (this.DictionarySize - 1)) + this.DictionarySize);

        for (int i = this.DictionarySize; i < src.length; i++)
            dst[((i - width) & (this.DictionarySize - 1))
                + this.DictionarySize] = (src[i] < this.DictionarySize ? src[i] : ((src[i] - width) & (this.DictionarySize - 1))
                                                                                  + this.DictionarySize);
    }

    /**
     * Gets child branched with {@code ch} from {@code parent}.
     * Returns {@link #UNUSED} if there is no node.
     *
     * @param parent parent node
     * @param ch branch character
     * @return child node
     */
    private int child(int parent, int ch) {
        int node = this.hashTable[this.hash(parent, ch)];

        // this.parent[ PatriciaTrieSearch.UNUSED ] = parent;
        while (node != PatriciaTrieSearch.UNUSED && this.parent[node] != parent) {
            node = this.next[node];
        }

        return node;
    }

    /**
     * Gets hash value from {@code node} and {@code ch}.
     *
     * @param node node
     * @param ch branch character
     * @return hash value
     */
    private int hash(int node, int ch) {
        return (node + (ch << this.shift) + 256) % this.hashTable.length;
    }

    /**
     * Generates the smallest prime (or probable prime) greater than or equal to {@code num}.
     * The probability that the return value is not prime is 1/256 or less.
     *
     * @param num generates prime greater than or equal to this value.
     *
     * @return generated prime (or probable prime)
     */
    private static int generateProbablePrime(int num) {
        num = num + ((num & 1) == 0 ? 1 : 0);

        while (!(new BigInteger(Integer.toString(num))).isProbablePrime(8)) {
            num += 2;
            num = num + ((num % 3) == 0 ? 2 : 0);
            num = num + ((num % 5) == 0 ? 2 : 0);
            num = num + ((num % 7) == 0 ? 2 : 0);
        }
        return num;
    }
}

// /**
//  * Checks the entire Trie.
//  *
//  * @param pos current processing position.
//  *
//  * @exception RuntimeException if Trie is broken.
//  */
// private void checkTrie( int pos ){
//     for( int i = -256 ; i < 0 ; i++ ){
//         this.checkNode( i, pos );
//     }
//
//     for( int i = 1 ; i < this.DictionarySize ; i++ ){
//         if( this.parent[ i ] != PatriciaTrieSearch.UNUSED ){
//             this.checkNode( i, pos );
//         }
//     }
// }
//
// /**
//  * Checks non-leaf Node.
//  *
//  * Check items are:
//  * (1) Parent-child relationship
//  * (2) No contradiction in position.
//  * (3) No contradiction in level.
//  * (4) node has this.childnum[node] children.
//  * 4 items above.
//  *
//  * @param node node to check
//  * @param pos  current processing position
//  * @exception RuntimeException if any of the above checks fail.
//  */
// private void checkNode(int node, int pos) {
//
//     int nlevel;
//     int npos;
//     if (node < 0) {
//         nlevel = 0;
//         npos = this.TextBuffer.length;
//     } else {
//         nlevel = this.level[node];
//         npos = this.position[node];
//     }
//
//     int childcount = 0;
//     for (int i = 0 ; i < 256; i++) {
//         int child = this.child(node, i);
//
//         if (child != PatriciaTrieSearch.UNUSED) {
//             childcount++;
//
//             if (this.parent[ child ] != node) {
//                 System.out.println("unlink::parent<->child");
//                 this.writeNode( node );
//                 this.writeNode( child );
//                 throw new RuntimeException("Trie Broken");
//             }
//
//             if (child < this.DictionarySize){
//                 if (this.level[child] <= nlevel){
//                     System.out.println("broken hierarchy::level");
//                     this.writeNode( node );
//                     this.writeNode( child );
//                     throw new RuntimeException( "Trie Broken" );
//                 }
//
//                 if( npos < this.position[ child ] ){
//                     System.out.println( "broken hierarchy::position" );
//                     this.writeNode( node );
//                     this.writeNode( child );
//                     throw new RuntimeException( "Trie Broken" );
//                 }
//                 // this.checkTrie( child, pos );
//             }else{
//                 int childpos = ( pos <= child ? child - this.DictionarySize : child );
//                 if( npos < childpos ){
//                     System.out.println( "broken hierarchy::position" );
//                     this.writeNode( node );
//                     this.writeNode( child );
//                     throw new RuntimeException( "Trie Broken" );
//                 }
//             }
//         }
//     }
//
//     if( 0 < node && node < this.DictionarySize ){
//         if( this.childnum[ node ] != childcount ){
//             System.out.println( "broken hierarchy::childnum" );
//             this.writeNode( node );
//             throw new RuntimeException( "Trie Broken" );
//         }
//     }
// }
//
// /**
//  * Outputs node information.
//  *
//  * @param node node to output information for
//  */
// private void writeNode( int node ){
//     if( 0 < node ){
//       System.out.println( "this.parent[" + node + "]  ::" + this.parent[ node ] );
//       System.out.println( "this.prev[" + node + "]    ::" + this.prev[ node ] );
//       System.out.println( "this.next[" + node + "]    ::" + this.next[ node ] );
//       if( node < this.DictionarySize ){
//           System.out.println( "this.childnum[" + node + "]::" + this.childnum[ node ] );
//           System.out.println( "this.position[" + node + "]::" + this.position[ node ] );
//           System.out.println( "this.level[" + node + "]   ::" + this.level[ node ] );
//       }
//   } else if(node < 0) {
//       System.out.println( "ROOT_NODE                  ::" + node );
//   } else {
//       System.out.println( "UNUSED                     ::" + node );
//   }
// }
