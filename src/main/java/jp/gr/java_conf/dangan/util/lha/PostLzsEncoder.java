// start of PostLzsEncoder.java
// TEXT_STYLE:CODE=Shift_JIS(Japanese):RET_CODE=CRLF

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

// import exceptions

import java.io.IOException;
// import classes and interfaces
import java.io.OutputStream;

import jp.gr.java_conf.dangan.io.BitOutputStream;
import jp.gr.java_conf.dangan.io.Bits;


/**
 * {@link PostLzssEncoder} for -lzs- compression.
 *
 * <pre>
 * -- revision history --
 * $Log: PostLzsEncoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/07/31 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     Fixed MaxMatch for -lzs- to be 17 instead of 16.
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     fix license statement
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PostLzsEncoder implements PostLzssEncoder {

    /** Dictionary size */
    private static final int DictionarySize = 2048;

    /** Maximum match length */
    private static final int MaxMatch = 17;

    /** Minimum match length */
    private static final int Threshold = 2;

    /** Number of bits for match position */
    private static final int PositionBits = Bits.len(PostLzsEncoder.DictionarySize - 1);

    /** Number of bits for match length */
    private static final int LengthBits = Bits.len(PostLzsEncoder.MaxMatch - PostLzsEncoder.Threshold);

    /**
     * Bit output stream to output -lzs- format data.
     */
    private BitOutputStream out;

    /**
     * Current processing position in the stream.
     */
    private int position;

    /**
     * LZSS compressed code currently being processed.
     */
    private int matchLength;

    /**
     * Constructs a {@link PostLzssEncoder} for -lzs- compression.
     *
     * @param out stream to output -lzs- format compressed data
     */
    public PostLzsEncoder(OutputStream out) {
        if (out != null) {
            if (out instanceof BitOutputStream) {
                this.out = (BitOutputStream) out;
            } else {
                this.out = new BitOutputStream(out);
            }
            this.position = 0;
            this.matchLength = 0;
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
        if (code < 0x100) {
            this.out.writeBit(1);
            this.out.writeBits(8, code);
            this.position++;
        } else {
            // Expecting NullPointerException to be thrown on writeCode() after close().
            this.out.writeBit(0);
            this.matchLength = code - 0x100;
        }
    }

    /**
     * Writes the match offset of LZSS compressed code.<br>
     *
     * @param offset the match offset of LZSS compressed code
     */
    @Override
    public void writeOffset(int offset) throws IOException {
        int pos = (this.position - offset - 1 - PostLzsEncoder.MaxMatch) & (PostLzsEncoder.DictionarySize - 1);

        this.position += this.matchLength + PostLzsEncoder.Threshold;

        this.out.writeBits(PostLzsEncoder.PositionBits, pos);
        this.out.writeBits(PostLzsEncoder.LengthBits, this.matchLength);
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
    }

    /**
     * Gets the size of the -lzs- format LZSS dictionary.
     *
     * @return the size of the -lzs- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PostLzsEncoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lzs- format LZSS.
     *
     * @return the maximum match length of the -lzs- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PostLzsEncoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lzs- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lzs- format
     */
    @Override
    public int getThreshold() {
        return PostLzsEncoder.Threshold;
    }
}
