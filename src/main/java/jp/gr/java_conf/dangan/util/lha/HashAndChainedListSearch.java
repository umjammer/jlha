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
 * Speed-optimized implementation of {@link LzssSearchMethod} using hash and singly linked list.<br>
 * Since it also speeds up search by aborting, it is not guaranteed to always find the longest match.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: HashAndChainedListSearch.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     Interface change to match {@link LzssSearchMethod} interface change
 * [improvement]
 *     Changed implementation to match TEST5 of ar940528.
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     license comment update
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class HashAndChainedListSearch implements LzssSearchMethod {

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
    private final byte[] TextBuffer;

    /**
     * Dictionary limit position.
     * When there is no data in the dictionary area in the first half of {@link #TextBuffer},
     * this prevents compression from using undefined data (0 in Java) in the dictionary area.
     */
    private int dictionaryLimit;

    /**
     * Hash method.
     */
    private final HashMethod hashMethod;

    /**
     * Hash table.
     * Index is hash value, content is position in {@link #TextBuffer}.
     */
    private final int[] hashTable;

    /**
     * Flag set when the length of the linked list of data patterns
     * having the same hash value exceeds a certain threshold.
     * <p>
     * Since {@code boolean[]} is somehow slower, 16 flags are handled together as {@code char[]}.
     * When handling, use the utility methods
     * {@link #isTooBig(int)}, {@link #setTooBigFlag(int)}, and {@link #clearTooBigFlag(int)}.
     */
    private final char[] tooBigFlag;

    /**
     * Singly linked list holding start positions of data patterns
     * having the same hash value.
     */
    private final int[] prev;

    /**
     * Upper limit on the number of search attempts.
     * No more searches than this number will be performed.
     */
    private final int searchLimitCount;

    /**
     * Constructs a {@link LzssSearchMethod} using hash and linked list.<br>
     * Default hash method and search attempt limit are used.
     *
     * @param DictionarySize dictionary size
     * @param MaxMatch maximum match length
     * @param Threshold threshold between compression and non-compression
     * @param TextBuffer buffer for applying LZSS compression
     */
    public HashAndChainedListSearch(int DictionarySize, int MaxMatch, int Threshold, byte[] TextBuffer) {
        this(DictionarySize, MaxMatch, Threshold, TextBuffer, HashDefault.class.getName(), 256);
    }

    /**
     * Constructs a {@link LzssSearchMethod} using hash and linked list.<br>
     * Default hash method is used.
     *
     * @param DictionarySize dictionary size
     * @param MaxMatch maximum match length
     * @param Threshold threshold between compression and non-compression
     * @param TextBuffer buffer for applying LZSS compression
     * @param SearchLimitCount upper limit on search attempts
     *
     * @exception IllegalArgumentException if {@code searchLimitCount} is 0 or less
     */
    public HashAndChainedListSearch(int DictionarySize, int MaxMatch, int Threshold, byte[] TextBuffer, int SearchLimitCount) {
        this(DictionarySize, MaxMatch, Threshold, TextBuffer, HashDefault.class.getName(), SearchLimitCount);
    }

    /**
     * Constructs a {@link LzssSearchMethod} using hash and linked list.<br>
     * Default search attempt limit is used.
     *
     * @param DictionarySize dictionary size
     * @param MaxMatch maximum match length
     * @param Threshold threshold between compression and non-compression
     * @param TextBuffer buffer for applying LZSS compression
     * @param HashMethodClassName class name providing the {@link HashMethod}
     * @exception NoClassDefFoundError if the class specified by {@code HashMethodClassName} is not found.
     * @exception InstantiationError if an instance cannot be created because the class specified by {@code HashMethodClassName} is an abstract class.
     * @exception NoSuchMethodError if the class specified by {@code HashMethodClassName} does not have the constructor {@code HashMethod(byte[])}.
     */
    public HashAndChainedListSearch(int DictionarySize,
            int MaxMatch,
            int Threshold,
            byte[] TextBuffer,
            String HashMethodClassName) {
        this(DictionarySize, MaxMatch, Threshold, TextBuffer, HashMethodClassName, 256);
    }

    /**
     * Constructs a {@link LzssSearchMethod} using hash and linked list.
     *
     * @param dictionarySize dictionary size
     * @param maxMatch maximum match length
     * @param threshold threshold between compression and non-compression
     * @param textBuffer buffer for applying LZSS compression
     * @param hashMethodClassName class name providing the {@link HashMethod}
     * @param searchLimitCount upper limit on search attempts
     * @exception IllegalArgumentException if {@code searchLimitCount} is 0 or less
     * @exception NoClassDefFoundError if the class specified by {@code hashMethodClassName} is not found.
     * @exception InstantiationError if an instance cannot be created because the class specified by {@code hashMethodClassName} is an abstract class.
     * @exception NoSuchMethodError if the class specified by {@code hashMethodClassName} does not have the constructor {@code HashMethod(byte[])}.
     */
    public HashAndChainedListSearch(int dictionarySize,
            int maxMatch,
            int threshold,
            byte[] textBuffer,
            String hashMethodClassName,
            int searchLimitCount) {

        if (0 < searchLimitCount) {

            this.dictionarySize = dictionarySize;
            this.maxMatch = maxMatch;
            this.threshold = threshold;
            this.TextBuffer = textBuffer;
            this.dictionaryLimit = this.dictionarySize;
            this.searchLimitCount = searchLimitCount;

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
            Arrays.fill(this.hashTable, -1);

            // Initialize linked list
            this.prev = new int[this.dictionarySize];
            Arrays.fill(this.prev, -1);

            this.tooBigFlag = new char[this.hashMethod.tableSize() >> 4];

        } else {
            throw new IllegalArgumentException("searchLimitCount must be 1 or more.");
        }
    }

    /**
     * Registers the data pattern starting from {@code position}
     * into the search mechanism consisting of hash and linked list.
     *
     * @param position start position of the data pattern in {@link #TextBuffer}
     */
    @Override
    public void put(int position) {
        int hash = this.hashMethod.hash(position);
        this.prev[position & (this.dictionarySize - 1)] = this.hashTable[hash];
        this.hashTable[hash] = position;
    }

    /**
     * Searches for the data pattern with the longest match to the data pattern
     * starting from {@code position} among the data patterns registered in the
     * search mechanism consisting of hash and linked list, and simultaneously registers
     * the data pattern starting from {@code position} into the search mechanism
     * consisting of hash and linked list.
     *
     * @param position start position of the data pattern in {@link #TextBuffer}.
     *
     * @return If a match is found, a value holding information of the match position and match length
     *         generated by {@link LzssOutputStream#createSearchReturn};
     *         if no match is found, {@link LzssOutputStream#NOMATCH}.
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int searchAndPut(int position) {

        int matchlen = this.threshold - 1;
        int matchpos = position;
        int maxmatch;
        int scanlimit = Math.max(this.dictionaryLimit, position - this.dictionarySize);

        // If the length of the linked list is too long, use offset
        // to use a hash value with a shorter linked list.
        int poshash = this.hashMethod.hash(position);
        int offhash = poshash;
        int offset = 0;
        while (this.isTooBig(offhash) && offset < this.maxMatch - this.hashMethod.hashRequires()) {
            offset++;
            offhash = this.hashMethod.hash(position + offset);
        }

        // Main loop
        // Traverse at most 2 linked lists starting from offhash and poshash.
        byte[] buf = this.TextBuffer;
        int max = position + this.maxMatch;
        int s;
        int p;
        int len;
        while (true) {
            int scanpos = this.hashTable[offhash];
            int searchCount = this.searchLimitCount;

            while (scanlimit <= scanpos - offset && 0 < --searchCount) {
                if (buf[scanpos + matchlen - offset] == buf[position + matchlen]) {
                    s = scanpos - offset;
                    p = position;
                    while (buf[s] == buf[p]) {
                        s++;
                        p++;
                        if (max <= p)
                            break;
                    }

                    len = p - position;
                    if (matchlen < len) {
                        matchpos = scanpos - offset;
                        matchlen = len;
                        if (max <= p)
                            break;
                    }
                }
                scanpos = this.prev[scanpos & (this.dictionarySize - 1)];
            }

            if (searchCount <= 0) {
                this.setTooBigFlag(offhash);
            } else if (scanpos < scanlimit) {
                this.clearTooBigFlag(offhash);
            }

            if (0 < offset && matchlen < this.hashMethod.hashRequires() + offset) {
                offset = 0;
                maxmatch = this.hashMethod.hashRequires() + offset - 1;
                max = position + maxmatch;
                offhash = poshash;
            } else {
                break;
            }
        }

        // Register the data pattern starting from position
        // into the search mechanism using hash and linked list.
        this.prev[position & (this.dictionarySize - 1)] = this.hashTable[poshash];
        this.hashTable[poshash] = position;

        // Return the longest match to caller.
        if (this.threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * Searches registered data patterns in the search mechanism using hash and linked list
     * and gets the one with the longest match to the data pattern starting from {@code position}.
     *
     * @param position start position of the data pattern in {@link #TextBuffer}.
     * @param lastPutPos start position of the last registered data pattern.
     *
     * @return If a match is found, a value holding information of the match position and match length
     *         generated by {@link LzssOutputStream#createSearchReturn};
     *         if no match is found, {@link LzssOutputStream#NOMATCH}.
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int search(int position, int lastPutPos) {

        // Search data patterns not registered in the search mechanism
        // using hash and linked list by simple sequential search.
        int matchlen = this.threshold - 1;
        int matchpos = position;
        int scanpos = position - 1;
        int scanlimit = Math.max(this.dictionaryLimit, lastPutPos);

        byte[] buf = this.TextBuffer;
        int max = Math.min(this.TextBuffer.length, position + this.maxMatch);
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

        // Search from the search mechanism using hash and linked list.
        if (this.hashMethod.hashRequires() < this.TextBuffer.length - position) {
            int maxmatch;
            scanlimit = Math.max(this.dictionaryLimit, position - this.dictionarySize);

            // If the length of the linked list is too long, use offset
            // to use a hash value with a shorter linked list.
            int poshash = this.hashMethod.hash(position);
            int offhash = poshash;
            int offset = 0;
            while (this.isTooBig(offhash) && offset < this.maxMatch - this.hashMethod.hashRequires()) {
                offset++;
                offhash = this.hashMethod.hash(position + offset);
            }

            // Main loop
            // Traverse at most 2 linked lists starting from offhash and poshash.
            while (true) {
                int searchCount = this.searchLimitCount;
                scanpos = this.hashTable[offhash];

                while (scanlimit <= scanpos - offset && 0 < --searchCount) {
                    if (buf[scanpos + matchlen - offset] == buf[position + matchlen]) {
                        s = scanpos - offset;
                        p = position;
                        while (buf[s] == buf[p]) {
                            s++;
                            p++;
                            if (max <= p)
                                break;
                        }

                        len = p - position;
                        if (matchlen < len) {
                            matchpos = scanpos - offset;
                            matchlen = len;
                            if (max <= p)
                                break;
                        }
                    }
                    scanpos = this.prev[scanpos & (this.dictionarySize - 1)];
                }

                if (searchCount <= 0) {
                    this.setTooBigFlag(offhash);
                } else if (scanpos < scanlimit) {
                    this.clearTooBigFlag(offhash);
                }

                if (0 < offset && matchlen < this.hashMethod.hashRequires() + offset) {
                    offset = 0;
                    maxmatch = this.hashMethod.hashRequires() + offset - 1;
                    max = Math.min(this.TextBuffer.length, position + maxmatch);
                    offhash = poshash;
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
     * When sliding data in {@link #TextBuffer} up to {@code position} forward,
     * performs the process of shifting data in {@link LzssSearchMethod} forward
     * accordingly so as not to contradict data in {@link #TextBuffer}.
     */
    @Override
    public void slide() {
        this.dictionaryLimit = Math.max(0, this.dictionaryLimit - this.dictionarySize);

        for (int i = 0; i < this.hashTable.length; i++) {
            int pos = this.hashTable[i] - this.dictionarySize;
            this.hashTable[i] = (0 <= pos ? pos : -1);
        }

        for (int i = 0; i < this.prev.length; i++) {
            int pos = this.prev[i] - this.dictionarySize;
            this.prev[i] = (0 <= pos ? pos : -1);
        }
    }

    /**
     * Gets the amount of data used when registering data into {@link LzssSearchMethod} with {@link #put(int)}.
     * In {@link HashAndChainedListSearch}, returns the amount of data required for {@code hash()}
     * by the internally used {@link HashMethod} implementation
     * (return value of {@link HashMethod#hashRequires()}).
     *
     * @return amount of data required for {@code hash()} by the internally used {@link HashMethod} implementation
     */
    @Override
    public int putRequires() {
        return this.hashMethod.hashRequires();
    }

    /**
     * {@link #searchAndPut(int)} providing options for better LZSS compression.
     * For example, between match length 3, match position 4 and match length 4, match position 1024,
     * match length 3, match position 4 + 1 uncompressed character may result in fewer output bits.
     * To handle such cases, lists the match position closest to {@code position} for each match length.
     *
     * @param position start position of the data pattern to search
     * @param matchposs array for storing and returning the list of match positions<br>
     *            {@code matchpos[0]} stores the match position of match length {@code threshold},<br>
     *            {@code matchpos[1]} stores the match position of match length {@code threshold + 1}.<br>
     *            If no match is found, {@link LzssOutputStream#NOMATCH} is stored.
     * @return If a match is found, a SearchReturn generated by {@link LzssOutputStream#createSearchReturn(int, int)} is returned.<br>
     *         If no match is found, {@link LzssOutputStream#NOMATCH} is returned.
     */
    public int searchAndPut(int position, int[] matchposs) {
        int matchlen = this.threshold - 1;
        int matchpos = position;
        int maxmatch = this.maxMatch;
        int scanlimit = Math.max(this.dictionaryLimit, position - this.dictionarySize);
        int searchCount = this.searchLimitCount;

        Arrays.fill(matchposs, LzssOutputStream.NOMATCH);

        int scanpos = this.hashTable[this.hashMethod.hash(position)];

        while (scanlimit < scanpos && 0 < searchCount--) {
            if (this.TextBuffer[scanpos + matchlen] == this.TextBuffer[position + matchlen]) {
                int len = 0;
                while (this.TextBuffer[scanpos + len] == this.TextBuffer[position + len]) {
                    if (maxmatch <= ++len)
                        break;
                }

                if (matchlen < len) {
                    int i = matchlen + 1 - this.threshold;
                    int end = Math.min(len + 1 - this.threshold, matchposs.length);
                    while (i < end)
                        matchposs[i++] = scanpos;

                    matchpos = scanpos;
                    matchlen = len;
                    if (maxmatch <= len)
                        break;
                }
            }
            scanpos = this.prev[scanpos & (this.dictionarySize - 1)];
        }

        this.put(position);

        if (matchpos < position)
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        else
            return LzssOutputStream.NOMATCH;
    }

    /**
     * Gets whether the linked list for {@code hash} exceeds the threshold.
     *
     * @param hash hash value
     * @return {@code true} if the linked list length exceeds the threshold;
     *         {@code false} otherwise
     */
    private boolean isTooBig(int hash) {
        return 0 != (this.tooBigFlag[hash >> 4] & (1 << (hash & 0x0F)));
    }

    /**
     * Sets the flag indicating that the linked list for {@code hash} exceeded the threshold.
     *
     * @param hash hash value to set the too big flag for
     */
    private void setTooBigFlag(int hash) {
        this.tooBigFlag[hash >> 4] |= 1 << (hash & 0x0F);
    }

    /**
     * Clears the flag indicating that the linked list for {@code hash} exceeds the threshold.
     *
     * @param hash hash value to clear the too big flag for
     */
    private void clearTooBigFlag(int hash) {
        this.tooBigFlag[hash >> 4] &= ~(1 << (hash & 0x0F));
    }
}
