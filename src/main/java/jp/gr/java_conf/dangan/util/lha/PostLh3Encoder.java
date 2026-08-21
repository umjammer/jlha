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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import jp.gr.java_conf.dangan.io.BitOutputStream;


/**
 * {@link PostLzssEncoder} for -lh3- compression.<br>
 *
 * <pre>
 * $Log: PostLh3Encoder.java,v $
 * Revision 1.2  2002/12/06 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [change]
 *     Changed not to flush the connected {@link OutputStream}
 *     unless flush() is called.
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/07/31 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     fix license statement
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.2 $
 */
public class PostLh3Encoder implements PostLzssEncoder {

    /** Dictionary size */
    private static final int DictionarySize = 8192;

    /** Maximum match length */
    private static final int MaxMatch = 256;

    /** Minimum match length */
    private static final int Threshold = 3;

    /**
     * Fixed Huffman code length for OffHi part.
     */
    private static final int[] ConstOffHiLen = PostLh3Encoder.createConstOffHiLen();

    /**
     * Size of the Huffman tree for the code part.
     * If the code part handles a value larger than this, extra bits are output to compensate.
     */
    private static final int CodeSize = 286;

    /**
     * Bit output stream to output -lh3- compressed data.
     */
    private BitOutputStream out;

    /**
     * Buffer to temporarily store data for static Huffman compression.
     */
    private byte[] buffer;

    /**
     * Number of code data in buffer.
     */
    private int blockSize;

    /**
     * Current processing position in buffer.
     */
    private int position;

    /**
     * Current bit being processed in flag byte.
     */
    private int flagBit;

    /**
     * Position of the current flag byte in buffer.
     */
    private int flagPos;

    /**
     * Frequency table of the code part.
     */
    private int[] codeFreq;

    /**
     * Frequency table of the offHi part.
     */
    private int[] offHiFreq;

    /**
     * Constructs a {@link PostLzssEncoder} for -lh3- compression.<br>
     * The default buffer size is used.
     *
     * @param out output stream that receives compressed data
     */
    public PostLh3Encoder(OutputStream out) {
        this(out, 16384);
    }

    /**
     * Constructs a {@link PostLzssEncoder} for -lh3- compression.
     *
     * @param out        output stream that receives compressed data
     * @param BufferSize buffer size for static Huffman compression
     * @throws IllegalArgumentException if {@code BufferSize} is too small
     */
    public PostLh3Encoder(OutputStream out, int BufferSize) {
        final int DictionarySizeByteLen = 2;
        final int MinCapacity = (DictionarySizeByteLen + 1) * 8 + 1;

        if (out != null && MinCapacity <= BufferSize) {

            if (out instanceof BitOutputStream) {
                this.out = (BitOutputStream) out;
            } else {
                this.out = new BitOutputStream(out);
            }
            this.codeFreq = new int[PostLh3Encoder.CodeSize];
            this.offHiFreq = new int[PostLh3Encoder.DictionarySize >> 6];
            this.buffer = new byte[BufferSize];
            this.blockSize = 0;
            this.position = 0;
            this.flagBit = 0;
            this.flagPos = 0;
        } else if (out == null) {
            throw new NullPointerException("out");
        } else {
            throw new IllegalArgumentException("BufferSize too small. BufferSize must be larger than " + MinCapacity);
        }
    }

    /**
     * Writes 1 byte of uncompressed LZSS data or
     * the match length of LZSS compressed code.
     *
     * @param code 1 byte of uncompressed LZSS data or the match length of LZSS compressed code
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCode(int code) throws IOException {
        final int CodeMax = PostLh3Encoder.CodeSize - 1;
        final int DictionarySizeByteLen = 2;
        final int Capacity = (DictionarySizeByteLen + 1) * 8 + 1;

        if (this.flagBit == 0) {
            if (this.buffer.length - this.position < Capacity || (65536 - 8) <= this.blockSize) {
                this.writeOut();
            }
            this.flagBit = 0x80;
            this.flagPos = this.position++;
            this.buffer[this.flagPos] = 0;
        }

        // Store data
        this.buffer[this.position++] = (byte) code;

        // Store upper 1 bit as a flag
        if (0x100 <= code)
            this.buffer[this.flagPos] |= this.flagBit;
        this.flagBit >>= 1;

        // Update frequency table
        this.codeFreq[Math.min(code, CodeMax)]++;

        // Update block size
        this.blockSize++;
    }

    /**
     * Writes the match offset of LZSS compressed code.
     *
     * @param offset the match offset of LZSS compressed code
     */
    @Override
    public void writeOffset(int offset) {
        // Store data
        this.buffer[this.position++] = (byte) (offset >> 8);
        this.buffer[this.position++] = (byte) offset;

        // Update frequency table
        this.offHiFreq[(offset >> 6)]++;
    }

    /**
     * Flushes all 8-bit unit data buffered in this {@link PostLzssEncoder}
     * to the output destination {@link OutputStream},
     * and calls {@code flush()} on the destination {@link OutputStream}.<br>
     * This method changes the compression ratio.
     *
     * @throws IOException if an I/O error occurs
     * @see PostLzssEncoder#flush()
     * @see BitOutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        this.writeOut();
        this.out.flush();
    }

    /**
     * Closes this output stream and the connected output stream,
     * and releases any resources used.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        this.writeOut();
        this.out.close();

        this.out = null;
        this.buffer = null;
        this.codeFreq = null;
        this.offHiFreq = null;
    }

    /**
     * Gets the size of the -lh3- format LZSS dictionary.
     *
     * @return the size of the -lh3- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PostLh3Encoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lh3- format LZSS.
     *
     * @return the maximum match length of the -lh3- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PostLh3Encoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lh3- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lh3- format
     */
    @Override
    public int getThreshold() {
        return PostLh3Encoder.Threshold;
    }

    /**
     * Outputs all buffered data to {@code this.out}.
     *
     * @throws IOException if an I/O error occurs
     */
    private void writeOut() throws IOException {
        final int CodeMax = PostLh3Encoder.CodeSize - 1;

        if (0 < this.blockSize) {
            // Output block size
            this.out.writeBits(16, this.blockSize);

            // Generate Huffman code table
            int[] codeLen = StaticHuffman.FreqListToLenList(this.codeFreq);
            int[] codeCode = StaticHuffman.LenListToCodeList(codeLen);
            int[] offHiLen = PostLh3Encoder.getBetterOffHiLen(this.offHiFreq, StaticHuffman.FreqListToLenList(this.offHiFreq));
            int[] offHiCode = StaticHuffman.LenListToCodeList(offHiLen);

            // Output Huffman code table for code part
            if (2 <= PostLh3Encoder.countNoZeroElement(this.codeFreq)) {
                this.writeCodeLenList(codeLen);
            } else {
                this.out.writeBits(15, 0x4210);
                this.out.writeBits(9, PostLh3Encoder.getNoZeroElementIndex(this.codeFreq));
            }

            // Output Huffman code table for offHi part
            if (offHiLen != PostLh3Encoder.ConstOffHiLen) {
                this.out.writeBit(1);

                if (2 <= PostLh3Encoder.countNoZeroElement(this.offHiFreq)) {
                    this.writeOffHiLenList(offHiLen);
                } else {
                    this.out.writeBits(12, 0x0111);
                    this.out.writeBits(7, PostLh3Encoder.getNoZeroElementIndex(this.offHiFreq));
                }
            } else {
                this.out.writeBit(0);
            }

            // Output Huffman code
            this.position = 0;
            this.flagBit = 0;
            for (int i = 0; i < blockSize; i++) {
                if (this.flagBit == 0) {
                    this.flagBit = 0x80;
                    this.flagPos = this.position++;
                }

                if (0 == (this.buffer[this.flagPos] & this.flagBit)) {
                    int code = this.buffer[this.position++] & 0xFF;
                    this.out.writeBits(codeLen[code], codeCode[code]);
                } else {
                    int code = (this.buffer[this.position++] & 0xFF) | 0x100;
                    int offset = ((this.buffer[this.position++] & 0xFF) << 8) | (this.buffer[this.position++] & 0xFF);
                    int offHi = offset >> 6;
                    if (code < CodeMax) {
                        this.out.writeBits(codeLen[code], codeCode[code]);
                    } else {
                        this.out.writeBits(codeLen[CodeMax], codeCode[CodeMax]);
                        this.out.writeBits(8, code - CodeMax);
                    }
                    this.out.writeBits(offHiLen[offHi], offHiCode[offHi]);
                    this.out.writeBits(6, offset);
                }
                this.flagBit >>= 1;
            }

            // Process for next block
            Arrays.fill(this.codeFreq, 0);

            Arrays.fill(this.offHiFreq, 0);

            this.blockSize = 0;
            this.position = 0;
            this.flagBit = 0;
        }
    }

    /**
     * Writes the list of Huffman code lengths for the code part while encoding them.
     *
     * @param codeLen list of Huffman code lengths for the code part
     * @throws IOException if an I/O error occurs
     */
    private void writeCodeLenList(int[] codeLen) throws IOException {
        for (int j : codeLen) {
            if (0 < j) {
                this.out.writeBits(5, 0x10 | (j - 1));
            } else {
                this.out.writeBit(0);
            }
        }
    }

    /**
     * Writes the list of Huffman code lengths for the OffHi part while encoding them.
     *
     * @param offHiLen list of Huffman code lengths for {@code CodeFreq}
     * @throws IOException if an I/O error occurs
     */
    private void writeOffHiLenList(int[] offHiLen) throws IOException {
        for (int j : offHiLen) {
            this.out.writeBits(4, j);
        }
    }

    /**
     * Gets the number of non-zero elements in an array.
     *
     * @param array array
     * @return the number of non-zero elements in the array
     */
    private static int countNoZeroElement(int[] array) {
        int count = 0;
        for (int j : array) {
            if (0 != j) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the index of the first non-zero element in an array.
     *
     * @param array array
     * @return the index of the first non-zero element in the array.
     * If all elements are 0, returns 0.
     */
    private static int getNoZeroElementIndex(int[] array) {
        for (int i = 0; i < array.length; i++) {
            if (0 != array[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Generates a Huffman code length list for decoding offHi part of -lh3-.
     *
     * @return Huffman code length list for decoding offHi part of -lh3-
     */
    private static int[] createConstOffHiLen() {
        final int length = PostLh3Encoder.DictionarySize >> 6;
        int[] list = {
                2, 0x01, 0x01, 0x03, 0x06, 0x0D, 0x1F, 0x4E, 0
        };

        int[] offHiLen = new int[length];
        int index = 0;
        int len = list[index++];

        for (int i = 0; i < length; i++) {
            while (list[index] == i) {
                len++;
                index++;
            }
            offHiLen[i] = len;
        }
        return offHiLen;
    }

    /**
     * Compares the Huffman code length list generated from {@code OffHiFreq}
     * with the fixed Huffman code length list, and returns the one with fewer output bits.
     *
     * @param OffHiFreq frequency table of the upper 6 bits of offset part
     * @param OffHiLen  Huffman code length list generated from {@code OffHiFreq}
     * @return the Huffman code length list with fewer output bits
     */
    private static int[] getBetterOffHiLen(int[] OffHiFreq, int[] OffHiLen) {
        boolean detect = false;
        for (int j : OffHiLen) {
            if (15 < j) { // 15 represents the maximum Huffman code length that can be written with writeOffHiLenList.
                detect = true;
            }
        }

        if (!detect) {
            int origTotal = 1;
            int consTotal = 1;

            if (2 <= PostLh3Encoder.countNoZeroElement(OffHiFreq)) {
                origTotal += 4 * (PostLh3Encoder.DictionarySize >> 6);
            } else {
                origTotal += 4 * 3 + 7;
            }
            for (int i = 0; i < OffHiFreq.length; i++) {
                origTotal += OffHiFreq[i] * OffHiLen[i];
                consTotal += OffHiFreq[i] * PostLh3Encoder.ConstOffHiLen[i];
            }

            if (origTotal < consTotal)
                return OffHiLen;
            else
                return PostLh3Encoder.ConstOffHiLen;
        } else {
            return PostLh3Encoder.ConstOffHiLen;
        }
    }
}
