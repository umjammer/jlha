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


/**
 * {@link PreLzssDecoder} for -lh2- decompression.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: PreLh2Decoder.java,v $
 * Revision 1.1  2002/12/06 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * [bug fix]
 *     Fixed inaccurate calculation of available().
 * [maintenance]
 *     source maintenance
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PreLh2Decoder implements PreLzssDecoder {

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
     * BitInputStream supplying -lh2- compressed data.
     */
    private BitInputStream in;

    /**
     * Dynamic Huffman tree for obtaining 1 byte of uncompressed LZSS data or
     * match length from LZSS compressed code.
     */
    private DynamicHuffman codeHuffman;

    /**
     * Dynamic Huffman tree for obtaining the upper 7-bit value of LZSS compressed code.
     */
    private DynamicHuffman offHiHuffman;

    /**
     * Current processing position (of decompressed data).
     */
    private int position;

    /**
     * Position where addLeaf() should be performed next.
     */
    private int nextPosition;

    /**
     * Match length.
     */
    private int matchLength;

    /** For backing up codeHuffman */
    private DynamicHuffman markCodeHuffman;

    /** For backing up offHiHuffman */
    private DynamicHuffman markOffHiHuffman;

    /** For backing up position */
    private int markPosition;

    /** For backing up nextPosition */
    private int markNextPosition;

    /** For backing up matchLength */
    private int markMatchLength;

    /**
     * Constructs a {@link PreLzssDecoder} for -lh2- decompression.<br>
     *
     * @param in input stream that supplies compressed data
     */
    public PreLh2Decoder(InputStream in) {
        if (in != null) {
            if (in instanceof BitInputStream) {
                this.in = (BitInputStream) in;
            } else {
                this.in = new BitInputStream(in);
            }
            this.codeHuffman = new DynamicHuffman(PreLh2Decoder.CodeSize);
            this.offHiHuffman = new DynamicHuffman(PreLh2Decoder.DictionarySize >> 6, 1);

            this.position = 0;
            this.nextPosition = 1 << 6;
            this.matchLength = 0;
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * Reads 1 byte of uncompressed LZSS data or
     * the match length of compressed code compressed with -lh2-.
     *
     * @return 1 byte of uncompressed data or the match length of compressed code
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached
     */
    @Override
    public int readCode() throws IOException {
        final int CodeMax = PreLh2Decoder.CodeSize - 1;

        int node = this.codeHuffman.childNode(DynamicHuffman.ROOT);
        while (0 <= node) {
            node = this.codeHuffman.childNode(node - (in.readBoolean() ? 1 : 0));
        }
        int code = ~node;
        this.codeHuffman.update(code);

        if (code < 0x100) {
            this.position++;
        } else {
            if (code == CodeMax) {
                try {
                    code += this.in.readBits(8);
                } catch (BitDataBrokenException exception) {
                    if (exception.getCause() instanceof EOFException)
                        throw (EOFException) exception.getCause();
                }
            }
            this.matchLength = code - 0x100 + PreLh2Decoder.Threshold;
        }
        return code;
    }

    /**
     * Reads the match offset of LZSS compressed code compressed with -lh2-.
     *
     * @return match offset of compressed code compressed with -lh2-
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached
     */
    @Override
    public int readOffset() throws IOException {
        if (this.nextPosition < PreLh2Decoder.DictionarySize) {
            while (this.nextPosition < this.position) {
                this.offHiHuffman.addLeaf(this.nextPosition >> 6);
                this.nextPosition += 64;

                if (PreLh2Decoder.DictionarySize <= this.nextPosition)
                    break;
            }
        }
        this.position += this.matchLength;

        int node = this.offHiHuffman.childNode(DynamicHuffman.ROOT);
        while (0 <= node) {
            node = this.offHiHuffman.childNode(node - (in.readBoolean() ? 1 : 0));
        }
        int offHi = ~node;
        this.offHiHuffman.update(offHi);

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
     * @see PreLzssDecoder#available()
     */
    @Override
    public void mark(int readLimit) {
        this.in.mark(readLimit * 18 / 8 + 4);
        this.markCodeHuffman = (DynamicHuffman) this.codeHuffman.clone();
        this.markOffHiHuffman = (DynamicHuffman) this.offHiHuffman.clone();
        this.markPosition = this.position;
        this.markNextPosition = this.nextPosition;
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
        // BitInputStream throws an IOException.
        this.in.reset();

        this.codeHuffman = (DynamicHuffman) this.markCodeHuffman.clone();
        this.offHiHuffman = (DynamicHuffman) this.markOffHiHuffman.clone();
        this.position = this.markPosition;
        this.nextPosition = this.markNextPosition;
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
        this.codeHuffman = null;
        this.offHiHuffman = null;
        this.markCodeHuffman = null;
        this.markOffHiHuffman = null;
    }

    /**
     * Gets the size of the -lh2- format LZSS dictionary.
     *
     * @return the size of the -lh2- format LZSS dictionary
     */
    @Override
    public int getDictionarySize() {
        return PreLh2Decoder.DictionarySize;
    }

    /**
     * Gets the maximum match length of the -lh2- format LZSS.
     *
     * @return the maximum match length of the -lh2- format LZSS
     */
    @Override
    public int getMaxMatch() {
        return PreLh2Decoder.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression of -lh2- format.
     *
     * @return the threshold for LZSS compression/non-compression of -lh2- format
     */
    @Override
    public int getThreshold() {
        return PreLh2Decoder.Threshold;
    }
}
