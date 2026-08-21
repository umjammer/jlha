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
 * {@link PostLzssEncoder} for -lh2- compression. <br>
 *
 * <pre>
 * -- revision history --
 * $Log: PostLh2Encoder.java,v $
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
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PostLh2Encoder implements PostLzssEncoder {

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
     * Bit output stream to output -lh2- compressed data.
     */
    private BitOutputStream out;

    /**
     * Adaptive Huffman tree for compressing code part.
     */
    private DynamicHuffman codeHuffman;

    /**
     * Adaptive Huffman tree for compressing offHi part.
     */
    private DynamicHuffman offHiHuffman;

    /**
     * Counts the size of the original data output.
     */
    private int position;

    /**
     * Position where the next leaf should be added.
     */
    private int nextPosition;

    /**
     * Match length currently being processed.
     */
    private int matchLength;

    /**
     * Constructs a {@link PostLzssEncoder} for -lh2- compression.
     *
     * @param out output stream that receives compressed data
     */
    public PostLh2Encoder(OutputStream out) {
        if (out != null) {
            if (out instanceof BitOutputStream) {
                this.out = (BitOutputStream) out;
            } else {
                this.out = new BitOutputStream(out);
            }
            this.codeHuffman = new DynamicHuffman(PostLh2Encoder.CodeSize);
            this.offHiHuffman = new DynamicHuffman(PostLh2Encoder.DictionarySize >> 6, 1);
            this.position = 0;
            this.nextPosition = 1 << 6;
        } else {
            throw new NullPointerException("out");
        }
    }

    /**
     * Writes 1 byte of uncompressed LZSS data or
     * the match length of LZSS compressed code.<br>
     *
     * @param code 1 byte of uncompressed LZSS data or the match length of LZSS compressed code
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCode(int code) throws IOException {
        final int CodeMax = PostLh2Encoder.CodeSize - 1;

        int node = this.codeHuffman.codeToNode(Math.min(code, CodeMax));
        int hcode = 0;
        int hlen = 0;
        do {
            hcode >>>= 1;
            hlen++;
            if ((node & 1) != 0)
                hcode |= 0x80000000;

            node = this.codeHuffman.parentNode(node);
        } while (node != DynamicHuffman.ROOT);

        this.out.writeBits(hlen, hcode >>> (32 - hlen));

        if (code < 0x100) {
            this.position++;
        } else {
            this.matchLength = (code & 0xFF) + PostLh2Encoder.Threshold;

            if (CodeMax <= code) {
                this.out.writeBits(8, code - CodeMax);
                code = CodeMax; // Set code to update to CodeMax.
            }
        }
        this.codeHuffman.update(code);
    }

    /**
     * Writes the match offset of LZSS compressed code.<br>
     *
     * @param offset the match offset of LZSS compressed code
     */
    @Override
    public void writeOffset(int offset) throws IOException {
        if (this.nextPosition < PostLh2Encoder.DictionarySize) {
            while (this.nextPosition < this.position) {
                this.offHiHuffman.addLeaf(this.nextPosition >> 6);
                this.nextPosition += 64;

                if (PostLh2Encoder.DictionarySize <= this.nextPosition)
                    break;
            }
        }

        int node = this.offHiHuffman.codeToNode(offset >> 6);
        int hcode = 0;
        int hlen = 0;
        while (node != DynamicHuffman.ROOT) {
            hcode >>>= 1;
            hlen++;
            if ((node & 1) != 0)
                hcode |= 0x80000000;

            node = this.offHiHuffman.parentNode(node);
        }

        this.out.writeBits(hlen, hcode >> (32 - hlen));
        this.out.writeBits(6, offset);
        this.offHiHuffman.update(offset >> 6);

        this.position += this.matchLength;
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
        this.codeHuffman = null;
        this.offHiHuffman = null;
    }

    /**
     * Gets the size of the -lh2- format LZSS dictionary.
     *
     * @return the size of the -lh2- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PostLh2Encoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lh2- format LZSS.
     *
     * @return the maximum match length of the -lh2- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PostLh2Encoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lh2- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lh2- format
     */
    @Override
    public int getThreshold() {
        return PostLh2Encoder.Threshold;
    }
}
