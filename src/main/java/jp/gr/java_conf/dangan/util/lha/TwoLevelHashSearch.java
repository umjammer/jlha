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

import jp.gr.java_conf.dangan.io.Bits;
import jp.gr.java_conf.dangan.lang.reflect.Factory;


/**
 * {@link LzssSearchMethod} accelerated using two-level hashing and singly linked lists.<br>
 * Referenced <a href="http://search.ieice.org/2000/pdf/e83-a_12_2689.pdf">Sadakane's paper</a>.
 *
 * <pre>
 * -- revision history --
 * $Log: TwoLevelHashSearch.java,v $
 * Revision 1.1  2002/12/10 22:06:40  dangan
 * [bug fix]
 *     Fixed bug in searchAndPut() where recent longest match could not be retrieved.
 *
 * Revision 1.0  2002/12/03 00:00:00  dangan
 * first edition
 * add to version control
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class TwoLevelHashSearch implements LzssSearchMethod {

    /**
     * LZSS dictionary size.
     */
    private final int DictionarySize;

    /**
     * Value used for LZSS compression.
     * Indicates maximum match length.
     */
    private final int MaxMatch;

    /**
     * Threshold used for LZSS compression.
     * If the match length is greater than or equal to this value, compression code is output.
     */
    private final int Threshold;

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
    private int DictionaryLimit;

    /**
     * Primary hash function
     */
    private final HashMethod primaryHash;

    /**
     * Primary hash table.
     * Index is primary hash value, content is index of secondary hash table.
     */
    private final int[] primaryHashTable;

    /**
     * Counts how many data patterns are registered in the primary hash table.
     */
    private final int[] primaryCount;

    /**
     * Number of bytes required to calculate secondary hash value.
     */
    private final int[] secondaryHashRequires;

    /**
     * Secondary hash table.
     * Index is value of primary hash table + secondary hash value,
     * content is start position of data pattern in {@link #TextBuffer}.
     */
    private int[] secondaryHashTable;

    /**
     * Dummy array swapped with {@link #secondaryHashTable} on each {@link #slide()}.
     * For reuse.
     */
    private int[] dummy;

    /**
     * Singly linked list holding start positions of data patterns with the same hash value.
     */
    private final int[] prev;

    /**
     * Constructs a {@link LzssSearchMethod} using two-level hashing.<br>
     * The default primary hash function is used.<br>
     *
     * @param DictionarySize dictionary size
     * @param MaxMatch       maximum match length
     * @param Threshold      threshold between compression and non-compression
     * @param TextBuffer     buffer for applying LZSS compression
     */
    public TwoLevelHashSearch(int DictionarySize, int MaxMatch, int Threshold, byte[] TextBuffer) {
        this(DictionarySize, MaxMatch, Threshold, TextBuffer, HashShort.class.getName());
    }

    /**
     * Constructs a {@link LzssSearchMethod} using two-level hashing.
     *
     * @param DictionarySize      dictionary size
     * @param MaxMatch            maximum match length
     * @param Threshold           threshold between compression and non-compression
     * @param TextBuffer          buffer for applying LZSS compression
     * @param HashMethodClassName class name providing hash function
     * @throws NoClassDefFoundError if the class specified by {@code HashMethodClassName} is not found.
     * @throws InstantiationError   if an instance cannot be created because the class specified by {@code HashMethodClassName} is an abstract class.
     * @throws NoSuchMethodError    if the class specified by {@code HashMethodClassName} does not have the constructor {@code HashMethod(byte[])}.
     */
    public TwoLevelHashSearch(int DictionarySize, int MaxMatch, int Threshold, byte[] TextBuffer, String HashMethodClassName) {

        this.DictionarySize = DictionarySize;
        this.MaxMatch = MaxMatch;
        this.Threshold = Threshold;
        this.TextBuffer = TextBuffer;
        this.DictionaryLimit = this.DictionarySize;

        try {
            this.primaryHash = (HashMethod) Factory.createInstance(HashMethodClassName,
                    new Object[] {
                            TextBuffer
                    });
        } catch (ClassNotFoundException exception) {
            throw new NoClassDefFoundError(exception.getMessage());
        } catch (InvocationTargetException exception) {
            throw new Error(exception.getTargetException().getMessage());
        } catch (NoSuchMethodException exception) {
            throw new NoSuchMethodError(exception.getMessage());
        } catch (InstantiationException exception) {
            throw new InstantiationError(exception.getMessage());
        }

        // Initialize hash tables
        this.primaryHashTable = new int[this.primaryHash.tableSize()];
        this.secondaryHashTable = new int[(this.primaryHash.tableSize() + this.DictionarySize / 4)];
        for (int i = 0; i < this.primaryHashTable.length; i++) {
            this.primaryHashTable[i] = i;
            this.secondaryHashTable[i] = -1;
        }

        // Create other arrays
        // Leverage the fact that primaryCount and secondaryHashRequires are zero-cleared upon array creation.
        this.primaryCount = new int[this.primaryHash.tableSize()];
        this.secondaryHashRequires = new int[this.primaryHash.tableSize()];
        this.dummy = new int[this.secondaryHashTable.length];

        // Initialize linked list
        this.prev = new int[this.DictionarySize];
        Arrays.fill(this.prev, -1);
    }

    /**
     * Registers the data pattern starting from {@code position}
     * into the search mechanism consisting of two-level hashing and linked lists.
     *
     * @param position start position of the data pattern in {@link #TextBuffer}
     */
    @Override
    public void put(int position) {
        int phash = this.primaryHash.hash(position);
        int base = this.primaryHashTable[phash];
        int shash = this.secondaryHash(position, this.secondaryHashRequires[phash]);

        this.primaryCount[phash]++;
        this.prev[position & (this.DictionarySize - 1)] = this.secondaryHashTable[base + shash];
        this.secondaryHashTable[base + shash] = position;
    }

    /**
     * Searches for the data pattern with the longest match to the data pattern
     * starting from {@code position} among data patterns registered in the
     * search mechanism consisting of two-level hashing and linked lists,
     * and simultaneously registers the data pattern starting from {@code position}
     * into the search mechanism consisting of two-level hashing and linked lists.
     *
     * @param position start position of data pattern in {@code TextBuffer}
     * @return If a match is found, a value holding information of the match position and match length
     *         generated by {@link LzssOutputStream#createSearchReturn(int, int)};
     *         if no match is found, {@link LzssOutputStream#NOMATCH}.
     * @see LzssOutputStream#createSearchReturn(int, int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int searchAndPut(int position) {
        int matchlen = this.Threshold - 1;
        int matchpos = position;
        int scanlimit = Math.max(this.DictionaryLimit, position - this.DictionarySize);

        int phash = this.primaryHash.hash(position);
        int base = this.primaryHashTable[phash];
        int requires = this.secondaryHashRequires[phash];
        int shash = this.secondaryHash(position, requires);
        int scanpos = this.secondaryHashTable[base + shash];

        byte[] buf = this.TextBuffer;
        int max = position + this.MaxMatch;
        int s;
        int p;
        int len;

        // Loop searching the linked list selected by secondary hash
        while (scanlimit <= scanpos) {
            if (buf[scanpos + matchlen] == buf[position + matchlen]) {
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
            }
            scanpos = this.prev[scanpos & (this.DictionarySize - 1)];
        }

        // When no match is found in the linked list narrowed down by secondary hash,
        // search all linked lists registered in primary hash
        int revbits = 1;
        int loopend = requires - Math.max(0, this.Threshold - this.primaryHash.hashRequires());
        int maxmatch = this.primaryHash.hashRequires() + requires - 1;
        for (int i = 1, send = 4; i <= loopend && matchlen <= maxmatch; i++, send <<= 2) {
            max += position + maxmatch;
            while (revbits < send) {
                scanpos = this.secondaryHashTable[base + (shash ^ revbits)];
                while (scanlimit <= scanpos) {
                    if (buf[scanpos] == buf[position]) {
                        s = scanpos + 1;
                        p = position + 1;
                        while (buf[s] == buf[p]) {
                            s++;
                            p++;
                            if (max <= p)
                                break;
                        }

                        len = p - position;
                        if (matchlen < len || (matchlen == len && matchpos < scanpos)) {
                            matchpos = scanpos;
                            matchlen = len;
                            if (max <= p) {
                                scanlimit = scanpos;
                                break;
                            }
                        }
                    }
                    scanpos = this.prev[scanpos & (this.DictionarySize - 1)];
                }
                revbits++;
            }
            maxmatch = this.primaryHash.hashRequires() + requires - i - 1;
        }

        // Register data pattern starting from position into search mechanism using two-level hashing and linked lists.
        this.primaryCount[phash]++;
        this.prev[position & (this.DictionarySize - 1)] = this.secondaryHashTable[base + shash];
        this.secondaryHashTable[base + shash] = position;

        // Return the longest match to caller.
        if (this.Threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * Searches data patterns registered in search mechanism using hashing and linked lists,
     * and gets the one with the longest match to the data pattern starting from {@code position}.<br>
     *
     * @param position   start position of the data pattern in {@link #TextBuffer}.
     * @param lastPutPos start position of the last registered data pattern.
     * @return If a match is found, a value holding information of the match position and match length
     *         generated by {@link LzssOutputStream#createSearchReturn(int, int)};
     *         if no match is found, {@link LzssOutputStream#NOMATCH}.
     * @see LzssOutputStream#createSearchReturn(int, int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int search(int position, int lastPutPos) {

        // Search data patterns not registered in search mechanism with hashing and linked lists
        // using simple sequential search.
        int matchlen = this.Threshold - 1;
        int matchpos = position;
        int scanpos = position - 1;
        int scanlimit = Math.max(this.DictionaryLimit, lastPutPos);

        byte[] buf = this.TextBuffer;
        int max = Math.min(this.TextBuffer.length, position + this.MaxMatch);
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

        // Search from search mechanism using two-level hashing and linked lists.
        int phashRequires = this.primaryHash.hashRequires();
        if (phashRequires < this.TextBuffer.length - position) {

            int phash = this.primaryHash.hash(position);
            int base = this.primaryHashTable[phash];
            int requires = this.secondaryHashRequires[phash];
            int shash;
            int start;
            if (phashRequires + requires < this.TextBuffer.length - position) {
                shash = this.secondaryHash(position, requires);
                start = 0;
            } else {
                int avail = this.TextBuffer.length - position - phashRequires;
                shash = this.secondaryHash(position, avail) << ((requires - avail) * 2);
                start = requires - avail;
            }
            int revbits = 0;
            @SuppressWarnings("unused")
            int loopend = requires - Math.max(0, this.Threshold - this.primaryHash.hashRequires());
            int maxmatch = this.MaxMatch;

            // Loop searching linked lists registered in primary level in order of priority
            for (int i = start, send = (1 << (i * 2)); i <= requires; i++, send <<= 2) {
                max += position + maxmatch;
                while (revbits < send) {
                    scanpos = this.secondaryHashTable[base + (shash ^ revbits)];
                    while (scanlimit <= scanpos) {
                        if (buf[scanpos] == buf[position]) {
                            s = scanpos + 1;
                            p = position + 1;
                            while (buf[s] == buf[p]) {
                                s++;
                                p++;
                                if (max <= p)
                                    break;
                            }

                            len = p - position;
                            if (matchlen < len || (matchlen == len && matchpos < scanpos)) {
                                matchpos = scanpos;
                                matchlen = len;
                                if (max <= p) {
                                    scanlimit = scanpos;
                                    break;
                                }
                            }
                        }
                        scanpos = this.prev[scanpos & (this.DictionarySize - 1)];
                    }
                    revbits++;
                }
                maxmatch = this.primaryHash.hashRequires() + requires - i - 1;
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

        // Update DictionaryLimit
        this.DictionaryLimit = Math.max(0, this.DictionaryLimit - this.DictionarySize);

        // Reconstruct secondaryHashTable based on values of primaryCount
        int secondaryIndex = 0;
        int dummyIndex = 0;
        for (int i = 0; i < this.primaryHashTable.length; i++) {
            this.primaryHashTable[i] = dummyIndex;
            int bits = this.secondaryHashRequires[i] * 2;

            if (1 << (5 + bits) <= this.primaryCount[i]) {
                for (int j = 0; j < (1 << bits); j++) {
                    this.divide(dummyIndex, secondaryIndex, this.primaryHash.hashRequires() + this.secondaryHashRequires[i]);
                    dummyIndex += 4;
                    secondaryIndex += 1;
                }
                this.secondaryHashRequires[i]++;

            } else if (0 < bits && this.primaryCount[i] < (1 << (2 + bits))) {
                for (int j = 0; j < (1 << (bits - 2)); j++) {
                    this.merge(dummyIndex, secondaryIndex);
                    dummyIndex += 1;
                    secondaryIndex += 4;
                }
                this.secondaryHashRequires[i]--;

            } else {
                for (int j = 0; j < (1 << bits); j++) {
                    int pos = this.secondaryHashTable[secondaryIndex++] - this.DictionarySize;
                    this.dummy[dummyIndex++] = (0 <= pos ? pos : -1);
                }
            }
            this.primaryCount[i] = 0;
        }
        int[] temp = this.secondaryHashTable;
        this.secondaryHashTable = this.dummy;
        this.dummy = temp;

        // Update linked list
        for (int i = 0; i < this.prev.length; i++) {
            int pos = this.prev[i] - this.DictionarySize;
            this.prev[i] = (0 <= pos ? pos : -1);
        }
    }

    /**
     * Gets the amount of data used when registering data into {@link LzssSearchMethod} with {@link #put(int)}.
     * In {@link TwoLevelHashSearch}, returns the sum of data amount required for {@code hash()}
     * by internal {@link HashMethod} implementation (return value of {@link HashMethod#hashRequires()})
     * and maximum number of bytes required for secondary hash.
     *
     * @return sum of bytes required for primary and secondary hash.
     */
    @Override
    public int putRequires() {
        return this.primaryHash.hashRequires() + Math.max(Bits.len(this.DictionarySize) - 5, 0) / 2;
    }

    /**
     * Secondary hash function
     *
     * @param position     start position of the data pattern in {@link #TextBuffer}
     * @param hashRequires number of bytes required to calculate secondary hash value
     */
    private int secondaryHash(int position, int hashRequires) {
        int hash = 0;
        int pos = position + this.primaryHash.hashRequires();

        while (0 < hashRequires--) {
            hash <<= 2;
            hash |= this.TextBuffer[pos++] & 0x03;
        }

        return hash;
    }

    /**
     * Splits secondary hash table and linked list.
     *
     * @param dbase  destination index in {@link #dummy}
     * @param sbase  source index in {@link #secondaryHashTable}
     * @param divoff split position
     */
    private void divide(int dbase, int sbase, int divoff) {
        int limit = this.DictionarySize;
        int position = this.secondaryHashTable[sbase];
        int[] current = {
                -1, -1, -1, -1
        };

        // Loop splitting linked list
        while (limit < position) {
            int shash = this.TextBuffer[position + divoff] & 0x03;
            if (0 < current[shash]) {
                this.prev[current[shash] & (this.DictionarySize - 1)] = position;
            } else {
                this.dummy[dbase + shash] = position - this.DictionarySize;
            }
            current[shash] = position;
            position = this.prev[position & (this.DictionarySize - 1)];
        }

        // Terminate linked list.
        for (int i = 0; i < current.length; i++) {
            if (0 < current[i]) {
                this.prev[current[i] & (this.DictionarySize - 1)] = -1;
            } else {
                this.dummy[dbase + i] = -1;
            }
        }
    }

    /**
     * Merges secondary hash table and linked list.
     *
     * @param dbase destination index in {@link #dummy}
     * @param sbase source index in {@link #secondaryHashTable}
     */
    private void merge(int dbase, int sbase) {
        int limit = this.DictionarySize;
        int position = -1;

        // Loop merging linked lists
        while (true) {
            int shash = 0;
            int max = this.secondaryHashTable[sbase];
            for (int i = 1; i < 4; i++) {
                if (max < this.secondaryHashTable[sbase + i]) {
                    shash = i;
                    max = this.secondaryHashTable[sbase + i];
                }
            }

            if (limit < max) {
                this.secondaryHashTable[sbase + shash] = this.prev[max & (this.DictionarySize - 1)];

                if (0 < position) {
                    this.prev[position & (this.DictionarySize - 1)] = max;
                } else {
                    this.dummy[dbase] = max - this.DictionarySize;
                }
                position = max;
            } else {
                break;
            }
        }

        // Terminate linked list.
        if (0 < position) {
            this.prev[position & (this.DictionarySize - 1)] = -1;
        } else {
            this.dummy[dbase] = -1;
        }
    }
}
