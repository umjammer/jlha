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

import jp.gr.java_conf.dangan.io.BitDataBrokenException;
import jp.gr.java_conf.dangan.io.BitInputStream;
import jp.gr.java_conf.dangan.io.Bits;
import jp.gr.java_conf.dangan.io.NotEnoughBitsException;


/**
 * {@link PreLzssDecoder} for -lh1- decompression.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: PreLh1Decoder.java,v $
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
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PreLh1Decoder implements PreLzssDecoder {

    /** Dictionary size */
    private static final int DictionarySize = 4096;

    /** Maximum match length */
    private static final int MaxMatch = 60;

    /** Minimum match length */
    private static final int Threshold = 3;

    /**
     * BitInputStream supplying -lh1- compressed data.
     */
    BitInputStream in;

    /**
     * Dynamic Huffman tree for decoding code part.
     */
    DynamicHuffman huffman;

    /**
     * Huffman code length list for decoding the upper 6 bits of offset part.
     */
    int[] offHiLen;

    /**
     * Table for decoding the upper 6 bits of offset part.
     */
    short[] offHiTable;

    /**
     * Number of bits required to look up the table for decoding the upper 6 bits of offset part.
     */
    int offHiTableBits;

    /** For backing up huffman */
    DynamicHuffman markHuffman;

    /**
     * Constructs a {@link PreLzssDecoder} for -lh1- decompression.
     *
     * @param in input stream that supplies data compressed with -lh1-
     */
    public PreLh1Decoder(InputStream in) {
        if (in != null) {
            if (in instanceof BitInputStream) {
                this.in = (BitInputStream) in;
            } else {
                this.in = new BitInputStream(in);
            }
            this.huffman = new DynamicHuffman(314);
            this.markHuffman = null;

            this.offHiLen = PreLh1Decoder.createLenList();
            try {
                this.offHiTable = StaticHuffman.createTable(this.offHiLen);
            } catch (BadHuffmanTableException exception) {
            }
            this.offHiTableBits = Bits.len(this.offHiTable.length - 1);
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * Reads 1 byte of uncompressed LZSS data or
     * the match length of compressed code compressed with -lh1-.
     *
     * @return 1 byte of uncompressed data or the match length of compressed code
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached
     */
    @Override
    public int readCode() throws IOException {
        int node = this.huffman.childNode(DynamicHuffman.ROOT);
        while (0 <= node) {
            node = this.huffman.childNode(node - (in.readBoolean() ? 1 : 0));
        }
        int code = ~node;
        this.huffman.update(code);
        return code;
    }

    /**
     * Reads the match offset of LZSS compressed code compressed with -lh1-.
     *
     * @return match offset of compressed code compressed with -lh1-
     * @throws IOException            if an I/O error occurs
     * @throws EOFException           if unexpected EndOfStream is reached because the data is incomplete
     * @throws BitDataBrokenException if unexpected EndOfStream is reached because the data is incomplete, or other I/O error occurs
     * @throws NotEnoughBitsException if unexpected EndOfStream is reached because the data is incomplete, or other I/O error occurs
     */
    @Override
    public int readOffset() throws IOException {
        // In the shortest case, offHi is represented by 3 bits of 0,
        // and offHiTableBits is 8 bits, so the difference between them is 5 bits.
        // Therefore, taking into account that the lower 6 bits are read,
        // peekBits will not throw NotEnoughBitsException for normal data.
        int offHi = this.offHiTable[this.in.peekBits(this.offHiTableBits)];
        this.in.skipBits(this.offHiLen[offHi]);

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
        this.in.mark(readLimit * 18 / 8 + 4);
        this.markHuffman = (DynamicHuffman) this.huffman.clone();
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

        this.huffman = (DynamicHuffman) this.markHuffman.clone();
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
        return Math.max(this.in.availableBits() / 18 - 4, 0);
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
        this.huffman = null;
        this.markHuffman = null;

        this.offHiLen = null;
        this.offHiTable = null;
        this.offHiTableBits = 0;
    }

    /**
     * Gets the size of the -lh1- format LZSS dictionary.
     *
     * @return the size of the -lh1- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PreLh1Decoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lh1- format LZSS.
     *
     * @return the maximum match length of the -lh1- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PreLh1Decoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lh1- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lh1- format
     */
    @Override
    public int getThreshold() {
        return PreLh1Decoder.Threshold;
    }

    /**
     * Generates a Huffman code length list for {@link StaticHuffman}
     * for decoding offset of -lh1-.
     *
     * @return Huffman code length list for {@link StaticHuffman} for decoding offset of -lh1-
     */
    private static int[] createLenList() {
        final int length = 64;
        int[] list = {
                3, 0x01, 0x04, 0x0C, 0x18, 0x30, 0
        };

        int[] LenList = new int[length];
        int index = 0;
        int len = list[index++];

        for (int i = 0; i < length; i++) {
            if (list[index] == i) {
                len++;
                index++;
            }
            LenList[i] = len;
        }
        return LenList;
    }
}
