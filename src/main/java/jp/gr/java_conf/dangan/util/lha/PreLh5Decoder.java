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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import jp.gr.java_conf.dangan.io.BitDataBrokenException;
import jp.gr.java_conf.dangan.io.Bits;
import jp.gr.java_conf.dangan.io.NotEnoughBitsException;


/**
 * {@link PreLzssDecoder} for -lh4-, -lh5-, -lh6-, and -lh7- decompression.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: PreLh5Decoder.java,v $
 * Revision 1.3  2002/12/08 00:00:00  dangan
 * [bug fix]
 *     In readCode, EOFException was not thrown when EndOfStream was reached
 *     while reading Huffman code.
 *
 * Revision 1.2  2002/12/08 00:00:00  dangan
 * [change]
 *     Changed class name from PreLh5DecoderFast to PreLh5Decoder.
 *
 * Revision 1.1  2002/12/06 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     Import source from latest BitInputStream and PreLh5Decoder.
 *     source maintenance
 *     tab removal
 *     fix license statement
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.3 $
 */
public class PreLh5Decoder implements PreLzssDecoder {

    /**
     * Connected input stream.
     */
    private InputStream in;

    /**
     * Byte array for mitigating performance degradation.
     */
    private byte[] cache;

    /**
     * Number of valid bytes in cache.
     */
    private int cacheLimit;

    /**
     * Current processing position in cache.
     */
    private int cachePosition;

    /**
     * Bit buffer.
     */
    private int bitBuffer;

    /**
     * Number of valid bits in bitBuffer.
     */
    private int bitCount;

    /**
     * Indicates the remaining size of the block currently being processed.
     */
    private int blockSize;

    /**
     * Table of Huffman code lengths for the code part.
     */
    private int[] codeLen;

    /**
     * Table for decoding the code part.
     * If positive, indicates an index in codeTree.
     * If negative, bitwise NOT of the code.
     */
    private short[] codeTable;

    /**
     * Number of bits required to look up codeTable.
     */
    private final int codeTableBits;

    /**
     * Tree for decoding data that does not fit in codeTable.
     * If positive, indicates an index in codeTree.
     * If negative, bitwise NOT of the code.
     */
    private short[][] codeTree;

    /**
     * Table of Huffman code lengths for the offLen part.
     */
    private int[] offLenLen;

    /**
     * Table for decoding the offLen part.
     * If positive, indicates an index in offLenTree.
     * If negative, bitwise NOT of the offLen.
     */
    private short[] offLenTable;

    /**
     * Number of bits required to look up offLenTable.
     */
    private final int offLenTableBits;

    /**
     * Tree for decoding data that does not fit in offLenTable.
     * If positive, indicates an index in offLenTree.
     * If negative, bitwise NOT of the offLen.
     */
    private short[][] offLenTree;

    /**
     * LZSS dictionary size.
     */
    private final int DictionarySize;

    /**
     * LZSS maximum match length.
     */
    private final int MaxMatch;

    /**
     * LZSS compression/non-compression threshold.
     */
    private final int Threshold;

    /**
     * Indicates whether the mark position is within the cache.
     * Set to {@code true} when marked, and set to {@code false}
     * when next read from {@code in} into the cache occurs.
     */
    private boolean markPositionIsInCache;

    /** For backing up cache */
    private byte[] markCache;

    /** For backing up cacheLimit */
    private int markCacheLimit;

    /** For backing up cachePosition */
    private int markCachePosition;

    /** For backing up bitBuffer */
    private int markBitBuffer;

    /** For backing up bitCount */
    private int markBitCount;

    /** For backing up blockSize */
    private int markBlockSize;

    /** For backing up codeLen */
    private int[] markCodeLen;

    /** For backing up codeTable */
    private short[] markCodeTable;

    /** For backing up codeTree */
    private short[][] markCodeTree;

    /** For backing up offLenLen */
    private int[] markOffLenLen;

    /** For backing up offLenTable */
    private short[] markOffLenTable;

    /** For backing up offLenTree */
    private short[][] markOffLenTree;

    /**
     * Constructs a {@link PreLzssDecoder} for -lh5- decompression.<br>
     * Default values are used for table sizes.
     *
     * @param in input stream that supplies -lh5- format compressed data
     */
    public PreLh5Decoder(InputStream in) {
        this(in, CompressMethod.LH5, 12, 8);
    }

    /**
     * Constructs a {@link PreLzssDecoder} for -lh4-, -lh5-, -lh6-, and -lh7- decompression.<br>
     * Default values are used for table sizes.
     *
     * @param in     input stream that supplies compressed data
     * @param method compression method identifier: <ul>
     *               <li> {@link CompressMethod#LH4} </li>
     *               <li> {@link CompressMethod#LH5} </li>
     *               <li> {@link CompressMethod#LH6} </li>
     *               <li> {@link CompressMethod#LH7} </li>
     *               </ul> specify one of these.
     * @throws IllegalArgumentException if {@code method} is other than the above
     */
    public PreLh5Decoder(InputStream in, String method) {

        this(in, method, 12, 8);
    }

    /**
     * Constructs a {@link PreLzssDecoder} for -lh4-, -lh5-, -lh6-, and -lh7- decompression.
     *
     * @param in              input stream that supplies compressed data
     * @param method          compression method identifier: <ul>
     *                        <li> {@link CompressMethod#LH4} </li>
     *                        <li> {@link CompressMethod#LH5} </li>
     *                        <li> {@link CompressMethod#LH6} </li>
     *                        <li> {@link CompressMethod#LH7} </li>
     *                        </ul> specify one of these.
     * @param CodeTableBits   specifies the size of the table used to decode the code part in bit length.
     *                        Specifying 12 generates a lookup table of 4096.
     * @param OffLenTableBits specifies the size of the table used to decode the offLen part in bit length.
     *                        Specifying 8 generates a lookup table of 256.
     * @throws IllegalArgumentException <ul>
     *                                  <li> (1) if {@code method} is other than the above </li>
     *                                  <li> (2) if {@code CodeTableBits} or </li>
     *                                  <li> {@code OffLenTableBits} is 0 or less </li>
     *                                  </ul> any of these
     */
    public PreLh5Decoder(InputStream in, String method, int CodeTableBits, int OffLenTableBits) {
        if (CompressMethod.LH4.equals(method) || CompressMethod.LH5.equals(method) || CompressMethod.LH6.equals(method)
                || CompressMethod.LH7.equals(method)) {

            this.DictionarySize = CompressMethod.toDictionarySize(method);
            this.MaxMatch = CompressMethod.toMaxMatch(method);
            this.Threshold = CompressMethod.toThreshold(method);

            if (in != null && 0 < CodeTableBits && 0 < OffLenTableBits) {
                this.in = in;
                this.cache = new byte[1024];
                this.cacheLimit = 0;
                this.cachePosition = 0;
                this.bitBuffer = 0;
                this.bitCount = 0;
                this.blockSize = 0;
                this.codeTableBits = CodeTableBits;
                this.offLenTableBits = OffLenTableBits;

                this.markPositionIsInCache = false;
                this.markCache = null;
                this.markCacheLimit = 0;
                this.markCachePosition = 0;
                this.markBitBuffer = 0;
                this.markBitCount = 0;

            } else if (in == null) {
                throw new NullPointerException("in");
            } else if (CodeTableBits <= 0) {
                throw new IllegalArgumentException("CodeTableBits too small. CodeTableBits must be larger than 1.");
            } else {
                throw new IllegalArgumentException("OffHiTableBits too small. OffHiTableBits must be larger than 1.");
            }
        } else if (null == method) {
            throw new NullPointerException("method");
        } else {
            throw new IllegalArgumentException("Unknown compress method " + method);
        }
    }

    /**
     * Reads 1 byte of uncompressed LZSS data or
     * the match length of compressed code compressed with -lh5- family compression method.
     *
     * @return 1 byte of uncompressed data, or the match length of compressed code
     * @throws IOException              if an I/O error occurs
     * @throws EOFException             if EndOfStream is reached
     * @throws BadHuffmanTableException if the Huffman code length table for constructing the Huffman tree is invalid
     */
    @Override
    public int readCode() throws IOException {
        if (this.blockSize <= 0) {
            this.readBlockHead();
        }
        this.blockSize--;

        if (this.bitCount < 16) {
            if (2 <= this.cacheLimit - this.cachePosition) {
                this.bitBuffer |= ((this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount))
                        | ((this.cache[this.cachePosition++] & 0xFF) << (16 - this.bitCount));
                this.bitCount += 16;
            } else {
                this.fillBitBuffer();

                int node = this.codeTable[this.bitBuffer >>> (32 - this.codeTableBits)];
                if (0 <= node) {
                    int bits = this.bitBuffer << this.codeTableBits;
                    do {
                        node = this.codeTree[bits >>> 31][node];
                        bits <<= 1;
                    } while (0 <= node);
                }
                int len = this.codeLen[~node];
                if (len <= this.bitCount) {
                    this.bitBuffer <<= len;
                    this.bitCount -= len;

                    return ~node;
                } else {
                    this.bitCount = 0;
                    this.bitBuffer = 0;
                    throw new EOFException();
                }
            }
        }

        int node = this.codeTable[this.bitBuffer >>> (32 - this.codeTableBits)];
        if (0 <= node) {
            int bits = this.bitBuffer << this.codeTableBits;
            do {
                node = this.codeTree[bits >>> 31][node];
                bits <<= 1;
            } while (0 <= node);
        }
        int len = this.codeLen[~node];
        this.bitBuffer <<= len;
        this.bitCount -= len;

        return ~node;
    }

    /**
     * Reads the match offset of LZSS compressed code compressed with -lh5- family compression method.
     *
     * @return match offset of compressed code compressed with -lh5- family
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int readOffset() throws IOException {
        if (this.bitCount < 16) {
            if (2 <= this.cacheLimit - this.cachePosition) {
                this.bitBuffer |= ((this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount))
                        | ((this.cache[this.cachePosition++] & 0xFF) << (16 - this.bitCount));
                this.bitCount += 16;
            } else {
                this.fillBitBuffer();
            }
        }

        int node = this.offLenTable[this.bitBuffer >>> (32 - this.offLenTableBits)];
        if (0 <= node) {
            int bits = this.bitBuffer << this.offLenTableBits;
            do {
                node = this.offLenTree[bits >>> 31][node];
                bits <<= 1;
            } while (0 <= node);
        }
        int offlen = ~node;
        int len = this.offLenLen[offlen];
        this.bitBuffer <<= len;
        this.bitCount -= len;

        offlen--;
        if (0 <= offlen) {
            return (1 << offlen) | this.readBits(offlen);
        } else {
            return 0;
        }
    }

    /**
     * Sets a mark at the current position in the connected input stream,
     * allowing return to the read position at the time of marking using {@link #reset()}.<br>
     * Note that unlike {@link InputStream#mark(int)}, the mark position
     * may become invalid before the limit specified by {@code readLimit}.<br>
     *
     * @param readLimit limit of bytes that can be read while still retaining the mark position.
     *                  If data exceeds this number of bytes, reset() may no longer be possible.
     * @see PreLzssDecoder#mark(int)
     */
    @Override
    public void mark(int readLimit) {

        // Calculate readLimit considering the worst case of Huffman encoding
        if (this.blockSize < readLimit) {
            readLimit = readLimit * StaticHuffman.LimitLen / 8;
            readLimit += 272; // block head
        } else {
            readLimit = readLimit * StaticHuffman.LimitLen / 8;
        }

        // Calculate readLimit for BitInputStream cache.
        readLimit -= this.cacheLimit - this.cachePosition;
        readLimit -= this.bitCount / 8;
        readLimit += 4;
        readLimit = (readLimit + this.cache.length - 1) / this.cache.length * this.cache.length;

        // mark processing
        this.in.mark(readLimit);

        if (this.markCache == null) {
            this.markCache = this.cache.clone();
        } else {
            System.arraycopy(this.cache, 0, this.markCache, 0, this.cacheLimit);
        }
        this.markCacheLimit = this.cacheLimit;
        this.markCachePosition = this.cachePosition;
        this.markBitBuffer = this.bitBuffer;
        this.markBitCount = this.bitCount;
        this.markPositionIsInCache = true;

        this.markBlockSize = this.blockSize;
        this.markCodeLen = this.codeLen;
        this.markCodeTable = this.codeTable;
        this.markCodeTree = this.codeTree;
        this.markOffLenLen = this.offLenLen;
        this.markOffLenTable = this.offLenTable;
        this.markOffLenTree = this.offLenTree;
    }

    /**
     * Sets the read position of the connected input stream to the position
     * where {@link #mark(int)} was last called.
     *
     * @throws IOException <ul>
     *                     <li> (1) if attempting to {@code reset()} without {@code mark()}.</li>
     *                     <li> (2) if the connected input stream returns {@code false} for {@code markSupported()}.</li>
     *                     <li> (3) if an I/O error occurs in the connected input stream.</li>
     *                     </ul>
     *                     any of these.
     */
    @Override
    public void reset() throws IOException {
        if (this.markPositionIsInCache) {
            this.cachePosition = this.markCachePosition;
            this.bitBuffer = this.markBitBuffer;
            this.bitCount = this.markBitCount;

            this.blockSize = this.markBlockSize;
            this.codeLen = this.markCodeLen;
            this.codeTable = this.markCodeTable;
            this.codeTree = this.markCodeTree;
            this.offLenLen = this.markOffLenLen;
            this.offLenTable = this.markOffLenTable;
            this.offLenTree = this.markOffLenTree;
        } else if (!this.in.markSupported()) {
            throw new IOException("not support mark()/reset().");
        } else if (this.markCache == null) { // This condition indicates that mark has not yet been set. Utilizes that markCache is set to null in constructor.
            throw new IOException("not marked.");
        } else {
            // If in cannot reset(),
            // this.in.reset() on the first line is expected to throw an IOException.
            this.in.reset();
            System.arraycopy(this.markCache, 0, this.cache, 0, this.markCacheLimit);
            this.cacheLimit = this.markCacheLimit;
            this.cachePosition = this.markCachePosition;
            this.bitBuffer = this.markBitBuffer;
            this.bitCount = this.markBitCount;

            this.blockSize = this.markBlockSize;
            this.codeLen = this.markCodeLen;
            this.codeTable = this.markCodeTable;
            this.codeTree = this.markCodeTree;
            this.offLenLen = this.markOffLenLen;
            this.offLenTable = this.markOffLenTable;
            this.offLenTree = this.markOffLenTree;
        }
    }

    /**
     * Gets whether the connected input stream supports {@code mark()} and {@code reset()}.
     *
     * @return {@code true} if the stream supports {@code mark()} and {@code reset()}; {@code false} otherwise.
     */
    @Override
    public boolean markSupported() {
        return this.in.markSupported();
    }

    /**
     * Gets the minimum number of bytes that can be read without blocking.<br>
     * Note that unlike {@link InputStream#available()}, this minimum number of bytes is not necessarily guaranteed.
     *
     * @return the minimum number of bytes that can be read without blocking
     * @throws IOException if an I/O error occurs
     * @see PreLzssDecoder#available()
     */
    @Override
    public int available() throws IOException {
        int avail = ((this.cacheLimit - this.cachePosition) + this.in.available() / this.cache.length * this.cache.length);
        avail += this.bitCount - 32;
        avail = avail / StaticHuffman.LimitLen;
        if (this.blockSize < avail) {
            avail -= 272;
        }
        return Math.max(avail, 0);
    }

    /**
     * Closes this stream and releases all resources used.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        this.in.close();
        this.in = null;

        this.cache = null;
        this.cacheLimit = 0;
        this.cachePosition = 0;
        this.bitBuffer = 0;
        this.bitCount = 0;

        this.markCache = null;
        this.markCacheLimit = 0;
        this.markCachePosition = 0;
        this.markBitBuffer = 0;
        this.markBitCount = 0;
        this.markPositionIsInCache = false;

        this.blockSize = 0;
        this.codeLen = null;
        this.codeTable = null;
        this.codeTree = null;
        this.offLenLen = null;
        this.offLenTable = null;
        this.offLenTree = null;

        this.markBlockSize = 0;
        this.markCodeLen = null;
        this.markCodeTable = null;
        this.markCodeTree = null;
        this.markOffLenLen = null;
        this.markOffLenTable = null;
        this.markOffLenTree = null;
    }

    /**
     * Gets the LZSS dictionary size handled by this {@link PreLh5Decoder}.
     *
     * @return the LZSS dictionary size handled by this {@link PreLh5Decoder}
     */
    @Override
    public int getDictionarySize() {
        return this.DictionarySize;
    }

    /**
     * Gets the maximum match length of the LZSS handled by this {@link PreLh5Decoder}.
     *
     * @return the maximum match length of the LZSS handled by this {@link PreLh5Decoder}
     */
    @Override
    public int getMaxMatch() {
        return this.MaxMatch;
    }

    /**
     * Gets the compression/non-compression threshold handled by this {@link PreLh5Decoder}.
     *
     * @return the compression/non-compression threshold handled by this {@link PreLh5Decoder}
     */
    @Override
    public int getThreshold() {
        return this.Threshold;
    }

    /**
     * Reads the block size and Huffman code length list at the beginning of a Huffman block.
     *
     * @throws IOException              if an I/O error occurs
     * @throws EOFException             if EndOfStream is reached
     * @throws BadHuffmanTableException if the Huffman code length table for constructing the Huffman tree is invalid and the Huffman decoder cannot be generated
     * @throws BitDataBrokenException   if data reading is interrupted unexpectedly and data of the requested number of bits cannot be obtained
     * @throws NotEnoughBitsException   if data reading is interrupted unexpectedly and data of the requested number of bits cannot be obtained
     */
    private void readBlockHead() throws IOException {
        // Read block size
        // For normal data, EndOfStream is reached at this point.
        try {
            this.blockSize = this.readBits(16);
        } catch (BitDataBrokenException exception) {
            if (exception.getCause() instanceof EOFException) {
                throw (EOFException) exception.getCause();
            } else {
                throw exception;
            }
        }

        // Process codeLen part
        int[] codeLenLen = this.readCodeLenLen();
        short[] codeLenTable;
        if (null != codeLenLen) {
            codeLenTable = StaticHuffman.createTable(codeLenLen);
        } else {
            codeLenTable = new short[] {
                    (short) this.readBits(5)
            };
            codeLenLen = new int[codeLenTable[0] + 1];
        }

        // Process code part
        this.codeLen = this.readCodeLen(codeLenTable, codeLenLen);
        if (null != this.codeLen) {
            short[][] tableAndTree = StaticHuffman.createTableAndTree(this.codeLen, this.codeTableBits);
            this.codeTable = tableAndTree[0];
            this.codeTree = new short[][] {
                    tableAndTree[1], tableAndTree[2]
            };
        } else {
            int code = this.readBits(9);
            this.codeLen = new int[256 + this.MaxMatch - this.Threshold + 1];
            this.codeTable = new short[1 << this.codeTableBits];
            Arrays.fill(this.codeTable, ((short) ~code));
            this.codeTree = new short[][] {
                    new short[0], new short[0]
            };
        }

        // Process offLen part
        this.offLenLen = this.readOffLenLen();
        if (null != this.offLenLen) {
            short[][] tableAndTree = StaticHuffman.createTableAndTree(this.offLenLen, this.offLenTableBits);
            this.offLenTable = tableAndTree[0];
            this.offLenTree = new short[][] {
                    tableAndTree[1], tableAndTree[2]
            };
        } else {
            int offLen = this.readBits(Bits.len(Bits.len(this.DictionarySize)));
            this.offLenLen = new int[Bits.len(this.DictionarySize)];
            this.offLenTable = new short[1 << this.offLenTableBits];
            Arrays.fill(this.offLenTable, ((short) ~offLen));
            this.offLenTree = new short[][] {
                    new short[0], new short[0]
            };
        }
    }

    /**
     * Reads the list of Huffman code lengths for decoding
     * the Huffman codes of the list of Code Huffman code lengths.
     *
     * @return list of Huffman code lengths, or {@code null} if there is no code length list
     * @throws IOException            if an I/O error occurs
     * @throws EOFException           if EndOfStream is reached
     * @throws BitDataBrokenException if data reading is interrupted unexpectedly and data of the requested number of bits cannot be obtained
     */
    private int[] readCodeLenLen() throws IOException {
        int listlen = this.readBits(5);
        if (0 < listlen) {
            int[] codeLenLen = new int[listlen];
            int index = 0;

            while (index < listlen) {
                int codelenlen = this.readBits(3);
                if (codelenlen == 0x07) {
                    while (this.readBoolean())
                        codelenlen++;
                }
                codeLenLen[index++] = codelenlen;

                if (index == 3) {
                    index += this.readBits(2);
                }
            }
            return codeLenLen;
        } else {
            return null;
        }
    }

    /**
     * Reads the list of Code Huffman code lengths while decoding them.
     *
     * @return list of Huffman code lengths, or {@code null} if there is no code length list
     * @throws IOException            if an I/O error occurs
     * @throws EOFException           if EndOfStream is reached
     * @throws BitDataBrokenException if data reading is interrupted unexpectedly and data of the requested number of bits cannot be obtained
     * @throws NotEnoughBitsException if data reading is interrupted unexpectedly and data of the requested number of bits cannot be obtained
     */
    private int[] readCodeLen(short[] codeLenTable, int[] codeLenLen) throws IOException {

        int codeLenTableBits = Bits.len(codeLenTable.length - 1);

        int listlen = this.readBits(9);
        if (0 < listlen) {
            int[] codeLen = new int[listlen];
            int index = 0;

            while (index < listlen) {
                this.fillBitBuffer();
                int bits = (0 < codeLenTableBits ? this.bitBuffer >>> (32 - codeLenTableBits) : 0);
                int codelen = codeLenTable[bits];
                int len = codeLenLen[codelen];
                this.bitBuffer <<= len;
                this.bitCount -= len;

                if (codelen == 0)
                    index++;
                else if (codelen == 1)
                    index += this.readBits(4) + 3;
                else if (codelen == 2)
                    index += this.readBits(9) + 20;
                else
                    codeLen[index++] = codelen - 2;
            }
            return codeLen;
        } else {
            return null;
        }
    }

    /**
     * Reads the list of Huffman code lengths for {@code offLen}.
     *
     * @return list of Huffman code lengths, or {@code null} if there is no code length list
     * @throws IOException            if an I/O error occurs
     * @throws EOFException           if EndOfStream is reached
     * @throws BitDataBrokenException if data reading is interrupted unexpectedly and data of the requested number of bits cannot be obtained
     */
    private int[] readOffLenLen() throws IOException {
        int listlen = this.readBits(Bits.len(Bits.len(this.DictionarySize)));
        if (0 < listlen) {
            int[] offLenLen = new int[listlen];
            int index = 0;

            while (index < listlen) {
                int offlenlen = this.readBits(3);
                if (offlenlen == 0x07) {
                    while (this.readBoolean())
                        offlenlen++;
                }
                offLenLen[index++] = offlenlen;
            }
            return offLenLen;
        } else {
            return null;
        }
    }

    /**
     * Reads 1-bit data from the connected input stream as a boolean value.
     *
     * @return {@code true} if the read 1-bit data is 1; {@code false} if 0.
     * @throws EOFException if EndOfStream has already been reached
     * @throws IOException  if an I/O error occurs in the connected input stream
     */
    private boolean readBoolean() throws IOException {
        if (0 < this.bitCount) {
            boolean bool = (this.bitBuffer < 0);
            this.bitBuffer <<= 1;
            this.bitCount -= 1;
            return bool;
        } else {
            this.fillBitBuffer();
            boolean bool = (this.bitBuffer < 0);
            this.bitBuffer <<= 1;
            this.bitCount -= 1;
            return bool;
        }
    }

    /**
     * Reads {@code count} bits of data from the connected input stream.
     * As can be seen from the {@code int} return value, the maximum valid number of bits
     * that can be read is 32 bits, but {@code count} is not checked for values of 32 or more,
     * so if a larger value is specified, the excess bit data is discarded.<br>
     * For example, calling {@code readBits(33)} first discards 1 bit of data
     * and returns the subsequent 32 bits of data.<br>
     * Also note that if {@code count} is 0 or less, no read operation is performed,
     * so the return value is always 0, and {@link EOFException} is not thrown even if EndOfStream has been reached.
     *
     * @param count number of bits to read
     * @return the read bit data
     * @throws IOException            if an I/O error occurs in the connected input stream
     * @throws EOFException           if EndOfStream has already been reached
     * @throws BitDataBrokenException if reading data of the requested number of bits failed due to reaching EndOfStream midway
     */
    private int readBits(int count) throws IOException {
        if (0 < count) {
            if (count <= this.bitCount) {
                int bits = this.bitBuffer >>> (32 - count);
                this.bitBuffer <<= count;
                this.bitCount -= count;
                return bits;
            } else {
                int requested = count;
                int bits = 0;
                try {
                    this.fillBitBuffer();
                    while (this.bitCount < count) {
                        count -= this.bitCount;
                        if (count < 32) {
                            bits |= (this.bitBuffer >>> (32 - this.bitCount)) << count;
                        }
                        this.bitBuffer = 0;
                        this.bitCount = 0;
                        this.fillBitBuffer();
                    }
                    bits |= this.bitBuffer >>> (32 - count);
                    this.bitBuffer <<= count;
                    this.bitCount -= count;
                    return bits;
                } catch (LocalEOFException exception) {
                    if (exception.thrownBy(this) && count < requested) {
                        throw new BitDataBrokenException(exception, bits >>> count, requested - count);
                    } else {
                        throw exception;
                    }
                }
            }
        } else {
            return 0;
        }
    }

    /**
     * Gets the number of bits stored in this {@link BitInputStream}.
     *
     * @return the number of bits stored in this {@link BitInputStream}
     */
    private int cachedBits() {
        return this.bitCount + ((this.cacheLimit - this.cachePosition) << 3);
    }

    /**
     * Fills {@code bitBuffer} with data.
     * Guarantees that 25 bits of data are secured in {@code bitBuffer},
     * except near EndOfStream.
     *
     * @throws IOException       if an I/O error occurs
     * @throws LocalEOFException if EndOfStream is reached with {@code bitBuffer} empty
     */
    private void fillBitBuffer() throws IOException {
        if (32 <= this.cachedBits()) {
            if (this.bitCount <= 24) {
                if (this.bitCount <= 16) {
                    if (this.bitCount <= 8) {
                        if (this.bitCount <= 0) {
                            this.bitBuffer = this.cache[this.cachePosition++] << 24;
                            this.bitCount = 8;
                        }
                        this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                        this.bitCount += 8;
                    }
                    this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                    this.bitCount += 8;
                }
                this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                this.bitCount += 8;
            }
        } else if (this.bitCount < 25) {
            if (this.bitCount == 0) {
                this.bitBuffer = 0;
            }

            int count = Math.min((32 - this.bitCount) >> 3, this.cacheLimit - this.cachePosition);
            while (0 < count--) {
                this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                this.bitCount += 8;
            }
            this.fillCache();
            if (this.cachePosition < this.cacheLimit) {
                count = Math.min((32 - this.bitCount) >> 3, this.cacheLimit - this.cachePosition);
                while (0 < count--) {
                    this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                    this.bitCount += 8;
                }
            } else if (this.bitCount <= 0) {
                throw new LocalEOFException(this);
            }
        }
    }

    /**
     * Reads data into cache when cache becomes empty.
     *
     * @throws IOException if an I/O error occurs
     */
    private void fillCache() throws IOException {
        this.markPositionIsInCache = false;
        this.cacheLimit = 0;
        this.cachePosition = 0;

        // Read data into cache
        int read = 0;
        while (0 <= read && this.cacheLimit < this.cache.length) {
            read = this.in.read(this.cache, this.cacheLimit, this.cache.length - this.cacheLimit);

            if (0 < read)
                this.cacheLimit += read;
        }
    }

    /**
     * Defines a local EOFException because using {@link EOFException}
     * for EndOfStream detection in {@link BitInputStream} is problematic.
     */
    private static class LocalEOFException extends EOFException {

        /**
         * The object that threw this exception.
         */
        private final Object owner;

        /**
         * Constructor.
         *
         * @param object the object that threw this exception
         */
        public LocalEOFException(Object object) {
            super();
            this.owner = object;
        }

        /**
         * Gets whether this exception was thrown by {@code object}.
         *
         * @param object object
         * @return {@code true} if this exception was thrown by {@code object}; {@code false} otherwise
         */
        public boolean thrownBy(Object object) {
            return this.owner == object;
        }
    }
}
