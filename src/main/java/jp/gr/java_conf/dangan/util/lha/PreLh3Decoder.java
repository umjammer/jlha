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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import jp.gr.java_conf.dangan.io.BitDataBrokenException;
import jp.gr.java_conf.dangan.io.BitInputStream;
import jp.gr.java_conf.dangan.io.NotEnoughBitsException;


/**
 * {@link PreLzssDecoder} for -lh3- decompression.
 *
 * <pre>
 * -- revision history --
 * $Log: PreLh3Decoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     Fixed inaccurate calculation of available.
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     fix license statement
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PreLh3Decoder implements PreLzssDecoder {

    /** Dictionary size */
    private static final int DictionarySize = 8192;

    /** Maximum match length */
    private static final int MaxMatch = 256;

    /** Minimum match length */
    private static final int Threshold = 3;

    /**
     * Size of the Huffman tree for the code part.
     * If the code part handles a value larger than this, extra bits are output to compensate.
     */
    private static final int CodeSize = 286;

    /**
     * BitInputStream supplying -lh3- compressed data.
     */
    private BitInputStream in;

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
     * Table of Huffman code lengths for the offHi part.
     */
    private int[] offHiLen;

    /**
     * Table for decoding the offHi part.
     * If positive, indicates an index of offHi.
     * If negative, bitwise NOT of the code.
     */
    private short[] offHiTable;

    /**
     * Number of bits required to look up offHiTable.
     */
    private final int offHiTableBits;

    /**
     * Tree for decoding data that does not fit in offHiTable.
     * If positive, indicates an index of offHi.
     * If negative, bitwise NOT of the code.
     */
    private short[][] offHiTree;

    /** For backing up blockSize */
    private int markBlockSize;

    /** For backing up codeLen */
    private int[] markCodeLen;

    /** For backing up codeTable */
    private short[] markCodeTable;

    /** For backing up codeTree */
    private short[][] markCodeTree;

    /** For backing up offHiLen */
    private int[] markOffHiLen;

    /** For backing up offHiTable */
    private short[] markOffHiTable;

    /** For backing up offHiTree */
    private short[][] markOffHiTree;

    /**
     * Constructs a {@link PreLzssDecoder} for -lh3- decompression.<br>
     * Default values are used for table sizes.
     *
     * @param in input stream that supplies compressed data
     */
    public PreLh3Decoder(InputStream in) {
        this(in, 12, 8);
    }

    /**
     * Constructs a {@link PreLzssDecoder} for -lh3- decompression.
     *
     * @param in             input stream that supplies compressed data
     * @param CodeTableBits  specifies the size of the table used to decode the code part in bit length.
     *                       Specifying 12 generates a lookup table of 4096.
     * @param OffHiTableBits specifies the size of the table used to decode the offHi part in bit length.
     *                       Specifying 8 generates a lookup table of 256.
     * @throws IllegalArgumentException if {@code CodeTableBits} or {@code OffHiTableBits} is 0 or less
     */
    public PreLh3Decoder(InputStream in, int CodeTableBits, int OffHiTableBits) {
        if (in != null && 0 < CodeTableBits && 0 < OffHiTableBits) {
            if (in instanceof BitInputStream) {
                this.in = (BitInputStream) in;
            } else {
                this.in = new BitInputStream(in);
            }
            this.blockSize = 0;
            this.codeTableBits = CodeTableBits;
            this.offHiTableBits = OffHiTableBits;
        } else if (in == null) {
            throw new NullPointerException("in");
        } else if (CodeTableBits <= 0) {
            throw new IllegalArgumentException("CodeTableBits too small. CodeTableBits must be larger than 1.");
        } else {
            throw new IllegalArgumentException("OffHiTableBits too small. OffHiTableBits must be larger than 1.");
        }
    }

    /**
     * Reads 1 byte of uncompressed LZSS data or
     * the match length of compressed code compressed with -lh3-.
     *
     * @return 1 byte of uncompressed data or
     * the match length of compressed code
     * @throws IOException              if an I/O error occurs
     * @throws EOFException             if EndOfStream is reached
     * @throws BadHuffmanTableException if the Huffman code length table for constructing the Huffman tree is invalid and the Huffman decoder cannot be generated
     */
    @Override
    public int readCode() throws IOException {
        if (this.blockSize <= 0) {
            this.readBlockHead();
        }
        this.blockSize--;

        int code;
        try {
            int node = this.codeTable[this.in.peekBits(this.codeTableBits)];
            if (node < 0) {
                code = ~node;
                this.in.skipBits(this.codeLen[code]);
            } else {
                this.in.skipBits(this.codeTableBits);
                do {
                    node = this.codeTree[this.in.readBit()][node];
                } while (0 <= node);
                code = ~node;
            }
        } catch (NotEnoughBitsException exception) {
            int avail = exception.getAvailableBits();
            int bits = this.in.peekBits(avail);
            bits = bits << (this.codeTableBits - avail);
            int node = this.codeTable[bits];

            if (node < 0) {
                code = ~node;
                if (this.in.skipBits(this.codeLen[code]) < this.codeLen[code]) {
                    throw new EOFException();
                }
            } else {
                this.in.skipBits(avail);
                throw new EOFException();
            }
        } catch (ArrayIndexOutOfBoundsException exception) {
            throw new EOFException();
        }

        final int CodeMax = PreLh3Decoder.CodeSize - 1;
        if (code == CodeMax) {
            code += this.in.readBits(8);
        }
        return code;
    }

    /**
     * Reads the match offset of LZSS compressed code compressed with -lh3-.
     *
     * @return match offset of compressed code compressed with -lh3-
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int readOffset() throws IOException {
        int offHi;
        try {
            int node = this.offHiTable[this.in.peekBits(this.offHiTableBits)];
            if (node < 0) {
                offHi = ~node;
                this.in.skipBits(this.offHiLen[offHi]);
            } else {
                this.in.skipBits(this.offHiTableBits);
                do {
                    node = this.offHiTree[this.in.readBit()][node];
                } while (0 <= node);
                offHi = ~node;
            }
        } catch (NotEnoughBitsException exception) {
            int avail = exception.getAvailableBits();
            int bits = this.in.peekBits(avail);
            bits = bits << (this.offHiTableBits - avail);
            int node = this.offHiTable[bits];

            if (node < 0) {
                offHi = ~node;
                if (this.offHiLen[offHi] <= avail) {
                    this.in.skipBits(this.offHiLen[offHi]);
                } else {
                    this.in.skipBits(avail);
                    throw new EOFException();
                }
            } else {
                this.in.skipBits(avail);
                throw new EOFException();
            }
        } catch (ArrayIndexOutOfBoundsException exception) {
            throw new EOFException();
        }

        return (offHi << 6) | this.in.readBits(6);
    }

    /**
     * Sets a mark at the current position in the connected input stream,
     * allowing return to the read position at the time of marking using {@link #reset()}.<br>
     * Note that unlike {@link InputStream#mark(int)}, the mark position
     * may become invalid before the limit specified by {@code readLimit}.
     *
     * @param readLimit limit of bytes that can be read while still retaining the mark position.
     *                  If data exceeds this number of bytes, reset() may no longer be possible.
     * @see PreLzssDecoder#mark(int)
     */
    @Override
    public void mark(int readLimit) {
        readLimit = readLimit * StaticHuffman.LimitLen / 8;
        if (this.blockSize < readLimit) {
            readLimit += 245;
        }
        this.in.mark(readLimit);

        this.markBlockSize = this.blockSize;
        this.markCodeLen = this.codeLen;
        this.markCodeTable = this.codeTable;
        this.markCodeTree = this.codeTree;
        this.markOffHiLen = this.offHiLen;
        this.markOffHiTable = this.offHiTable;
        this.markOffHiTree = this.offHiTree;
    }

    /**
     * Sets the read position of the connected input stream to the position
     * where {@link #mark(int)} was last called.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void reset() throws IOException {
        // If attempting to reset() without mark(),
        // if attempting to reset() beyond readLimit,
        // or if the connected InputStream returns false for markSupported(),
        // BitInputStream throws an IOException.
        this.in.reset();

        this.blockSize = this.markBlockSize;
        this.codeLen = this.markCodeLen;
        this.codeTable = this.markCodeTable;
        this.codeTree = this.markCodeTree;
        this.offHiLen = this.markOffHiLen;
        this.offHiTable = this.markOffHiTable;
        this.offHiTree = this.markOffHiTree;
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
        int avail = this.in.available() * 8 / StaticHuffman.LimitLen;
        if (this.blockSize < avail) {
            avail -= 245;
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

        this.blockSize = 0;
        this.codeLen = null;
        this.codeTable = null;
        this.codeTree = null;
        this.offHiLen = null;
        this.offHiTable = null;
        this.offHiTree = null;

        this.markBlockSize = 0;
        this.markCodeLen = null;
        this.markCodeTable = null;
        this.markCodeTree = null;
        this.markOffHiLen = null;
        this.markOffHiTable = null;
        this.markOffHiTree = null;
    }

    /**
     * Gets the size of the -lh3- format LZSS dictionary.
     *
     * @return the size of the -lh3- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PreLh3Decoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lh3- format LZSS.
     *
     * @return the maximum match length of the -lh3- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PreLh3Decoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lh3- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lh3- format
     */
    @Override
    public int getThreshold() {
        return PreLh3Decoder.Threshold;
    }

    /**
     * Reads the block size and Huffman code length list at the beginning of a Huffman block.
     *
     * @throws IOException              if an I/O error occurs
     * @throws EOFException             if EndOfStream is reached
     * @throws BadHuffmanTableException if the Huffman code length table for constructing the Huffman tree is invalid and the Huffman decoder cannot be generated
     * @throws BitDataBrokenException   if data reading is interrupted unexpectedly and data of the requested number of bits cannot be obtained
     */
    private void readBlockHead() throws IOException {
        // Read block size
        // For normal data, EndOfStream is reached at this point.
        try {
            this.blockSize = this.in.readBits(16);
        } catch (BitDataBrokenException exception) {
            if (exception.getCause() instanceof EOFException) {
                throw (EOFException) exception.getCause();
            } else {
                throw exception;
            }
        }

        // Process code part
        this.codeLen = this.readCodeLen();
        if (1 < this.codeLen.length) {
            short[][] tableAndTree = StaticHuffman.createTableAndTree(this.codeLen, this.codeTableBits);
            this.codeTable = tableAndTree[0];
            this.codeTree = new short[][] {
                    tableAndTree[1], tableAndTree[2]
            };
        } else {
            int code = this.codeLen[0];
            this.codeLen = new int[PreLh3Decoder.CodeSize];
            this.codeTable = new short[1 << this.codeTableBits];
            Arrays.fill(this.codeTable, ((short) ~code));
            this.codeTree = new short[][] {
                    new short[0], new short[0]
            };
        }

        // Process offHi part
        this.offHiLen = this.readOffHiLen();
        if (1 < this.offHiLen.length) {
            short[][] tableAndTree = StaticHuffman.createTableAndTree(this.offHiLen, this.offHiTableBits);
            this.offHiTable = tableAndTree[0];
            this.offHiTree = new short[][] {
                    tableAndTree[1], tableAndTree[2]
            };
        } else {
            int offHi = this.offHiLen[0];
            this.offHiLen = new int[PreLh3Decoder.DictionarySize >> 6];
            this.offHiTable = new short[1 << this.offHiTableBits];
            Arrays.fill(this.offHiTable, ((short) ~offHi));
            this.offHiTree = new short[][] {
                    new short[0], new short[0]
            };
        }
    }

    /**
     * Reads the list of Huffman code lengths for the code part.
     *
     * @return list of Huffman code lengths, or a single code of length 1
     * @throws IOException            if an I/O error occurs
     * @throws EOFException           if EndOfStream is reached
     * @throws BitDataBrokenException if data reading is interrupted unexpectedly and data of the requested number of bits cannot be obtained
     */
    private int[] readCodeLen() throws IOException {
        int[] codeLen = new int[PreLh3Decoder.CodeSize];

        for (int i = 0; i < codeLen.length; i++) {
            if (this.in.readBoolean())
                codeLen[i] = this.in.readBits(4) + 1;

            if (i == 2 && codeLen[0] == 1 && codeLen[1] == 1 && codeLen[2] == 1) {
                return new int[] {
                        this.in.readBits(9)
                };
            }
        }
        return codeLen;
    }

    /**
     * Reads the list of Huffman code lengths for the offHi part.
     *
     * @return list of Huffman code lengths, or a single code of length 1
     * @throws IOException            if an I/O error occurs
     * @throws EOFException           if EndOfStream is reached
     * @throws BitDataBrokenException if data reading is interrupted unexpectedly and data of the requested number of bits cannot be obtained
     */
    private int[] readOffHiLen() throws IOException {
        if (this.in.readBoolean()) {
            int[] offHiLen = new int[PreLh3Decoder.DictionarySize >> 6];

            for (int i = 0; i < offHiLen.length; i++) {
                offHiLen[i] = this.in.readBits(4);

                if (i == 2 && offHiLen[0] == 1 && offHiLen[1] == 1 && offHiLen[2] == 1) {
                    return new int[] {
                            this.in.readBits(7)
                    };
                }
            }
            return offHiLen;
        } else {
            return PreLh3Decoder.createConstOffHiLen();
        }
    }

    /**
     * Generates a Huffman code length list for {@link StaticHuffman} for decoding offHi part of -lh3-.
     *
     * @return Huffman code length list for {@link StaticHuffman} for decoding offHi part of -lh3-
     */
    private static int[] createConstOffHiLen() {
        final int length = PreLh3Decoder.DictionarySize >> 6;
        int[] list = {
                2, 0x01, 0x01, 0x03, 0x06, 0x0D, 0x1F, 0x4E, 0
        };

        int[] LenList = new int[length];
        int index = 0;
        int len = list[index++];

        for (int i = 0; i < length; i++) {
            while (list[index] == i) {
                len++;
                index++;
            }
            LenList[i] = len;
        }
        return LenList;
    }
}
