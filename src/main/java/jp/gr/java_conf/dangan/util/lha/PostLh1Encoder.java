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
// import classes and interfaces
import java.io.OutputStream;

import jp.gr.java_conf.dangan.io.BitOutputStream;


/**
 * {@link PostLzssEncoder} for -lh1- compression. <br>
 *
 * <pre>
 * -- revision history --
 * $Log: PostLh1Encoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/07/31 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     fix license statement
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PostLh1Encoder implements PostLzssEncoder {

    /** Dictionary size */
    private static final int DictionarySize = 4096;

    /** Maximum match length */
    private static final int MaxMatch = 60;

    /** Minimum match length */
    private static final int Threshold = 3;

    /**
     * Bit output stream to output -lh1- compressed data.
     */
    private BitOutputStream out;

    /**
     * Adaptive Huffman tree for compressing Code part.
     */
    private DynamicHuffman huffman;

    /**
     * Huffman code table for compressing the upper 6 bits of offset part.
     */
    private int[] offHiCode;

    /**
     * Huffman code length table for compressing the upper 6 bits of offset part.
     */
    private int[] offHiLen;

    /**
     * Constructs a {@link PostLzssEncoder} for -lh1- compression.
     *
     * @param out output stream that receives compressed data
     */
    public PostLh1Encoder(OutputStream out) {
        if (out != null) {
            if (out instanceof BitOutputStream) {
                this.out = (BitOutputStream) out;
            } else {
                this.out = new BitOutputStream(out);
            }
            this.huffman = new DynamicHuffman(314);
            this.offHiLen = PostLh1Encoder.createLenList();
            try {
                this.offHiCode = StaticHuffman.LenListToCodeList(this.offHiLen);
            } catch (BadHuffmanTableException exception) {
            }
        } else {
            throw new NullPointerException("out");
        }
    }

    /**
     * Writes 1 byte of uncompressed LZSS data or
     * the match length of LZSS compressed code.<br>
     *
     * @param code 1 byte of uncompressed LZSS data or
     *             the match length of LZSS compressed code
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCode(int code) throws IOException {
        int node = this.huffman.codeToNode(code);
        int hcode = 0;
        int hlen = 0;
        do {
            hcode >>>= 1;
            hlen++;
            if ((node & 1) != 0)
                hcode |= 0x80000000;

            node = this.huffman.parentNode(node);
        } while (node != DynamicHuffman.ROOT);

        this.out.writeBits(hlen, hcode >> (32 - hlen));
        this.huffman.update(code);
    }

    /**
     * Writes the match offset of LZSS compressed code.<br>
     *
     * @param offset the match offset of LZSS compressed code
     */
    @Override
    public void writeOffset(int offset) throws IOException {
        int offHi = (offset >> 6);
        this.out.writeBits(this.offHiLen[offHi], this.offHiCode[offHi]);
        this.out.writeBits(6, offset);
    }

    /**
     * Flushes all 8-bit unit data buffered in this {@link PostLzssEncoder}
     * to the output destination {@link OutputStream},
     * and calls {@code flush()} on the destination {@link OutputStream}.<br>
     * This method does not affect the compression ratio.
     *
     * @throws IOException if an I/O error occurs
     * @see PostLzssEncoder#flush()
     * @see BitOutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    /**
     * Closes this output stream and the connected output stream,
     * and releases any resources used.<br>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        this.out.close();

        this.out = null;
        this.huffman = null;
        this.offHiLen = null;
        this.offHiCode = null;
    }

    /**
     * Gets the size of the -lh1- format LZSS dictionary.
     *
     * @return the size of the -lh1- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PostLh1Encoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lh1- format LZSS.
     *
     * @return the maximum match length of the -lh1- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PostLh1Encoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lh1- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lh1- format
     */
    @Override
    public int getThreshold() {
        return PostLh1Encoder.Threshold;
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
