// start of PreLz5Decoder.java
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

import jp.gr.java_conf.dangan.io.CachedInputStream;


/**
 * {@link PreLzssDecoder} for -lz5- decompression.
 *
 * <pre>
 * -- revision history --
 * $Log: PreLz5Decoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     Fixed MaxMatch for -lz5- to be 18 instead of 16.
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
public class PreLz5Decoder implements PreLzssDecoder {

    /** Dictionary size */
    private static final int DictionarySize = 4096;

    /** Maximum match length */
    private static final int MaxMatch = 18;

    /** Minimum match length */
    private static final int Threshold = 3;

    /**
     * Input stream supplying -lz5- compressed data.
     */
    private InputStream in;

    /** Flag indicating LZSS compression/non-compression. */
    private int flagByte;

    /** Mask for obtaining 1 bit indicating LZSS compression/non-compression. */
    private int flagBit;

    /**
     * Current processing position in the stream.
     * Needed to convert from LARC offset to LHA offset.
     */
    private int position;

    /**
     * Match length of LZSS compressed code currently being processed.
     */
    private int matchLength;

    /** For backing up flagByte */
    private int markFlagByte;

    /** For backing up flagBit */
    private int markFlagBit;

    /** For backing up position */
    private int markPosition;

    /** For backing up matchLength */
    private int markMatchLength;

    /**
     * Constructs a {@link PreLzssDecoder} for -lz5- decompression.<br>
     *
     * @param in input stream that supplies compressed data
     */
    public PreLz5Decoder(InputStream in) {
        if (in != null) {
            this.in = in;
            this.position = 0;
            this.matchLength = 0;
            this.flagByte = 0;
            this.flagBit = 0;
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * Reads 1 byte of uncompressed LZSS data or
     * the match length of compressed code compressed with -lz5-.
     *
     * @return 1 byte of uncompressed data or the match length of compressed code
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached
     */
    @Override
    public int readCode() throws IOException {
        int code;
        if (this.flagBit == 0) {
            this.flagByte = this.in.read();

            if (this.flagByte < 0) {
                return -1;
            }

            this.flagBit = 0x01;
        }

        if ((this.flagByte & this.flagBit) == 0) {
            this.flagBit <<= 1;
            this.flagBit &= 0xFF;

            int code1 = this.in.read();
            int code2 = this.in.read();

            if (0 <= code1 && 0 <= code2) {
                this.matchLength = (code2 & 0x0F) + PreLz5Decoder.Threshold;
                code = (code2 & 0xF0) << 4 | code1;
            } else {
                throw new EOFException();
            }
        } else {
            this.flagBit <<= 1;
            this.flagBit &= 0xFF;

            code = this.in.read();

            if (code < 0) {
                throw new EOFException();
            }

            this.position++;
        }
        return code;
    }

    /**
     * Reads the match offset of LZSS compressed code compressed with -lz5-.
     *
     * @return match offset of compressed code compressed with -lz5-
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached
     */
    @Override
    public int readOffset() throws IOException {
        int offset = (this.position - this.readCode() - 1 - PreLz5Decoder.MaxMatch)
                & (PreLz5Decoder.DictionarySize - 1);
        this.position += this.matchLength;

        return offset;
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
        this.in.mark((readLimit * 9 + 7) / 8 + 2);
        this.markFlagByte = this.flagByte;
        this.markFlagBit = this.flagBit;
        this.markPosition = this.position;
        this.markMatchLength = this.matchLength;
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
        // this.in.reset() throws an IOException.
        this.in.reset();

        this.flagByte = this.markFlagByte;
        this.flagBit = this.markFlagBit;
        this.position = this.markPosition;
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
     * This value is guaranteed.
     *
     * @return the minimum number of bytes that can be read without blocking
     * @throws IOException if an I/O error occurs
     * @see PreLzssDecoder#available()
     */
    @Override
    public int available() throws IOException {
        return Math.max(in.available() * 8 / 9 - 2, 0);
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
    }

    /**
     * Gets the size of the -lz5- format LZSS dictionary.
     *
     * @return the size of the -lz5- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PreLz5Decoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lz5- format LZSS.
     *
     * @return the maximum match length of the -lz5- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PreLz5Decoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lz5- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lz5- format
     */
    @Override
    public int getThreshold() {
        return PreLz5Decoder.Threshold;
    }

}
