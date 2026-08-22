// start of PreLzsDecoder.java
// TEXT_STYLE:CODE=Shift_JIS(Japanese):RET_CODE=CRLF

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
// import exceptions
import java.io.IOException;
// import classes and interfaces
import java.io.InputStream;

import jp.gr.java_conf.dangan.io.BitDataBrokenException;
import jp.gr.java_conf.dangan.io.BitInputStream;
import jp.gr.java_conf.dangan.io.Bits;


/**
 * {@link PreLzssDecoder} for -lzs- decompression.
 *
 * <pre>
 * -- revision history --
 * $Log: PreLzsDecoder.java,v $
 * Revision 1.1  2002/12/06 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     Fixed MaxMatch to be 17 instead of 16.
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     fix license statement
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PreLzsDecoder implements PreLzssDecoder {

    /** Dictionary size */
    private static final int DictionarySize = 2048;

    /** Maximum match length */
    private static final int MaxMatch = 17;

    /** Minimum match length */
    private static final int Threshold = 2;

    /** Number of bits for match position */
    private static final int OffsetBits = Bits.len(PreLzsDecoder.DictionarySize - 1);

    /** Number of bits for match length */
    private static final int LengthBits = Bits.len(PreLzsDecoder.MaxMatch - PreLzsDecoder.Threshold);

    /**
     * BitInputStream supplying -lzs- compressed data.
     */
    private BitInputStream in;

    /**
     * Current processing position.
     * Exists because internal state of LzssInputStream cannot be obtained.
     * If written as an inner class of LzssInputStream, position is not needed.
     */
    private int position;

    /** Match position of the most recent LZSS code */
    private int matchOffset;

    /**
     * Match length of the most recent LZSS code.
     * Exists because internal state of LzssInputStream cannot be obtained.
     * If written as an inner class of LzssInputStream, matchLength is not needed.
     */
    private int matchLength;

    /** For backing up matchPosition */
    private int markPosition;

    /** For backing up matchOffset */
    private int markMatchOffset;

    /** For backing up matchLength */
    private int markMatchLength;

    /**
     * Constructs a {@link PreLzssDecoder} for -lzs- decompression.
     *
     * @param in input stream supplying -lzs- format compressed data
     */
    public PreLzsDecoder(InputStream in) {
        if (in != null) {
            if (in instanceof BitInputStream) {
                this.in = (BitInputStream) in;
            } else {
                this.in = new BitInputStream(in);
            }
            this.position = 0;
            this.matchOffset = 0;
            this.matchLength = 0;
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * Reads 1 byte of uncompressed LZSS data or
     * the match length of compressed code compressed with -lzs-.
     *
     * @return 1 byte of uncompressed data or the match length of compressed code
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int readCode() throws IOException {
        try {
            if (this.in.readBoolean()) {
                this.position++;
                return this.in.readBits(8);
            } else {
                this.matchOffset = this.in.readBits(PreLzsDecoder.OffsetBits);
                this.matchLength = this.in.readBits(PreLzsDecoder.LengthBits);
                return this.matchLength | 0x100;
            }
        } catch (BitDataBrokenException exception) {
            if (exception.getCause() instanceof EOFException)
                throw (EOFException) exception.getCause();
            else
                throw exception;
        }
    }

    /**
     * Reads the match position of LZSS compressed code compressed with -lzs-.<br>
     *
     * @return match position of compressed code compressed with -lzs-
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int readOffset() throws IOException {
        int offset = (this.position - this.matchOffset - 1 - PreLzsDecoder.MaxMatch) & (PreLzsDecoder.DictionarySize - 1);

        this.position += this.matchLength + PreLzsDecoder.Threshold;
        return offset;
    }

    /**
     * Sets a mark at the current position in the connected input stream,
     * allowing return to the read position at the time of marking using {@link #reset()}.<br>
     * Note that unlike {@link InputStream#mark(int)}, the mark position
     * may become invalid before the limit specified by {@code readLimit}.<br>
     *
     * @param readLimit limit of bytes that can be read while still retaining the mark position.
     *                  If data exceeds this number of bytes, reset() may no longer be possible.<br>
     * @see PreLzssDecoder#mark(int)
     */
    @Override
    public void mark(int readLimit) {
        this.in.mark((readLimit * 9 + 7) / 8 + 1);
        this.markPosition = this.position;
        this.markMatchOffset = this.matchOffset;
        this.markMatchLength = this.matchLength;
    }

    /**
     * Sets the read position of the connected input stream to the position
     * where {@link #mark(int)} was last called.<br>
     *
     * @throws IOException <ol>
     *                     <li>if attempting to {@code reset()} without {@code mark()}.</li>
     *                     <li>if the connected input stream returns {@code false} for {@code markSupported()}.</li>
     *                     <li>if an I/O error occurs in the connected input stream.</li>
     *                     </ol>
     *                     any of these.
     */
    @Override
    public void reset() throws IOException {
        // If attempting to reset() without mark(),
        // or if the connected InputStream returns false for markSupported(),
        // BitInputStream's reset() throws an IOException.
        this.in.reset();

        this.position = this.markPosition;
        this.matchOffset = this.markMatchOffset;
        this.matchLength = this.markMatchLength;
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
     * Unlike {@link InputStream#available()}, this minimum number of bytes is guaranteed.
     *
     * @return the minimum number of bytes that can be read without blocking
     * @throws IOException if an I/O error occurs
     * @see PreLzssDecoder#available()
     */
    @Override
    public int available() throws IOException {
        return Math.max(this.in.availableBits() / 9 - 2, 0);
    }

    /**
     * Closes this stream and connected stream,
     * and releases any resources used.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        this.in.close();

        this.in = null;
    }

    /**
     * Gets the size of the -lzs- format LZSS dictionary.
     *
     * @return the size of the -lzs- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PreLzsDecoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lzs- format LZSS.
     *
     * @return the maximum match length of the -lzs- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PreLzsDecoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lzs- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lzs- format
     */
    @Override
    public int getThreshold() {
        return PreLzsDecoder.Threshold;
    }
}
