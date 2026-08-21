/*
 * Copyright (C) 2001  Michel Ishizuka  All rights reserved.
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


/**
 * Interface for supplying LZSS compression codes.
 *
 * <pre>
 * -- revision history --
 * $Log: PreLzssDecoder.java,v $
 * Revision 1.0  2002/07/25 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     fix license statement
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public interface PreLzssDecoder {

    /**
     * Sets a mark at the current position in the connected input stream,
     * allowing return to the read position at the time of marking using the {@link #reset()} method.<br>
     * Unlike {@link java.io.InputStream#mark(int)}, the mark position may become invalid
     * before the limit specified by {@code readLimit}.
     * However, if connected to an {@link java.io.InputStream} that can be reset indefinitely ignoring {@code readLimit},
     * it must always be possible to restore the marked position with {@link #reset()} regardless of the value set in {@code readLimit}.<br>
     *
     * @param readLimit limit of bytes that can be read while still retaining the mark position.
     *                  If data exceeds this number of bytes, reset() may no longer be possible.
     */
    void mark(int readLimit);

    /**
     * Sets the read position of the connected input stream to the position
     * where {@link #mark(int)} was last called.<br>
     *
     * @throws IOException if an I/O error occurs
     */
    void reset() throws IOException;

    /**
     * Gets whether the connected input stream supports {@code mark()} and {@code reset()}.<br>
     *
     * @return {@code true} if the stream supports {@code mark()} and {@code reset()}; {@code false} otherwise.<br>
     */
    boolean markSupported();

    /**
     * Gets the minimum number of bytes that can be read without blocking from the connected input stream.<br>
     * This number does not need to be guaranteed to be exact.
     * This is because for intentionally crafted data, there is no way to know the minimum number of bytes
     * that can be read without blocking other than actually attempting to read.
     *
     * @return the minimum number of bytes that can be read without blocking
     * @throws IOException if an I/O error occurs
     */
    int available() throws IOException;

    /**
     * Closes this input stream and releases any resources used.<br>
     *
     * @throws IOException if an I/O error occurs
     */
    void close() throws IOException;

    /**
     * Reads 1 byte of uncompressed LZSS data or
     * the match length of LZSS compressed code.<br>
     * Uncompressed data must take values 0 to 255,
     * and LZSS compressed codes (match length) must take values 256 to 511.
     *
     * @return 1 byte of uncompressed LZSS data or the match length of LZSS compressed code
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached
     */
    int readCode() throws IOException;

    /**
     * Reads the match offset of LZSS compressed code.
     *
     * @return the match offset of LZSS compressed code
     * @throws IOException if an I/O error occurs
     */
    int readOffset() throws IOException;

    /**
     * Gets the size of the LZSS dictionary processed by this {@link PreLzssDecoder}.
     *
     * @return the size of the LZSS dictionary
     */
    int getDictionarySize();

    /**
     * Gets the maximum match length processed by this {@link PreLzssDecoder}.
     *
     * @return the maximum match length
     */
    int getMaxMatch();

    /**
     * Gets the compression/non-compression threshold processed by this {@link PreLzssDecoder}.
     *
     * @return the LZSS threshold
     */
    int getThreshold();
}
