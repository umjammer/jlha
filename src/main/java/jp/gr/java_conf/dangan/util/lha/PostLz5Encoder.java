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

import java.io.IOException;
// import classes and interfaces
import java.io.OutputStream;


/**
 * {@link PostLzssEncoder} for -lz5- compression.
 *
 * <pre>
 * -- revision history --
 * $Log: PostLz5Encoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/07/31 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     Fixed MaxMatch for -lz5- to be 18 instead of 16.
 *     Fixed failure to output data that could be output in flush().
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
public class PostLz5Encoder implements PostLzssEncoder {

    /** Dictionary size */
    private static final int DictionarySize = 4096;

    /** Maximum match length */
    private static final int MaxMatch = 18;

    /** Minimum match length */
    private static final int Threshold = 3;

    /**
     * Stream to output -lz5- compressed data.
     */
    private OutputStream out;

    /** Buffer for temporarily storing compressed data. */
    private byte[] buf;

    /** Current processing position in buf. */
    private int index;

    /** Position of flag in buf indicating LZSS compression/non-compression. */
    private int flagIndex;

    /** Flag indicating LZSS compression/non-compression. */
    private int flagBit;

    /**
     * Current processing position in the stream.
     * Needed to convert from LHA offset to LARC offset.
     */
    private int position;

    /**
     * Constructs a {@link PostLzssEncoder} for -lz5- compression.<br>
     *
     * @param out output stream to output compressed data
     */
    public PostLz5Encoder(OutputStream out) {
        if (out != null) {
            this.out = out;
            this.position = 0;
            this.buf = new byte[1024];
            this.index = 0;
            this.flagIndex = 0;
            this.flagBit = 0x100;
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
        if (this.flagBit == 0x100) {
            if (this.buf.length - (2 * 8 + 1) < this.index) {
                this.out.write(this.buf, 0, this.index);
                this.index = 0;
            }
            this.flagBit = 0x01;
            this.flagIndex = this.index++;
            this.buf[this.flagIndex] = 0;
        }

        if (code < 0x100) {
            this.buf[this.flagIndex] |= this.flagBit;
            this.buf[this.index++] = (byte) code;
            this.position++;
        } else {
            this.buf[this.index++] = (byte) code;
        }
        this.flagBit <<= 1;
    }

    /**
     * Writes the match offset of LZSS compressed code.
     *
     * @param offset the match offset of LZSS compressed code
     */
    @Override
    public void writeOffset(int offset) {
        int pos = (this.position - offset - 1 - PostLz5Encoder.MaxMatch) & (PostLz5Encoder.DictionarySize - 1);

        int matchlen = this.buf[--this.index] & 0x0F;
        this.buf[this.index++] = (byte) pos;
        this.buf[this.index++] = (byte) (((pos >> 4) & 0xF0) | matchlen);

        this.position += matchlen + PostLz5Encoder.Threshold;
    }

    /**
     * Flushes outputtable data buffered in this {@link PostLzssEncoder}
     * to the output destination {@link OutputStream},
     * and calls {@code flush()} on the destination {@link OutputStream}.<br>
     * This method leaves unoutputtable data of at most 15 bytes buffered
     * without outputting.
     * This method does not affect the compression ratio.
     *
     * @throws IOException if an I/O error occurs
     * @see PostLzssEncoder#flush()
     */
    @Override
    public void flush() throws IOException {
        if (this.flagBit == 0x100) {
            this.out.write(this.buf, 0, this.index);
            this.out.flush();

            this.index = 0;
            this.flagBit = 0x01;
            this.flagIndex = this.index++;
            this.buf[this.flagIndex] = 0;
        } else {
            this.out.write(this.buf, 0, this.flagIndex);
            this.out.flush();

            System.arraycopy(this.buf, this.flagIndex, this.buf, 0, this.index - this.flagIndex);
            this.index -= this.flagIndex;
            this.flagIndex = 0;
        }
    }

    /**
     * Closes this output stream and the connected output stream,
     * and releases any resources used.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        this.out.write(this.buf, 0, this.index);
        this.out.close();

        this.out = null;
        this.buf = null;
    }

    /**
     * Gets the size of the -lz5- format LZSS dictionary.
     *
     * @return the size of the -lz5- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PostLz5Encoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lz5- format LZSS.
     *
     * @return the maximum match length of the -lz5- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PostLz5Encoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lz5- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lz5- format
     */
    @Override
    public int getThreshold() {
        return PostLz5Encoder.Threshold;
    }
}
