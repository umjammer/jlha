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

package jp.gr.java_conf.dangan.io;

import java.io.IOException;
import java.io.InputStream;


/**
 * Utility class for disconnecting between an input stream that supplies data
 * and an input stream that processes data.<br>
 * When disconnecting from a buffering stream such as {@link java.io.BufferedInputStream},
 * buffering past the disconnection position must be prevented using
 * {@link LimitedInputStream} or similar.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: DisconnectableInputStream.java,v $
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     tab removal
 *     license statement fix
 *     source maintenance
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class DisconnectableInputStream extends InputStream implements Disconnectable {

    /**
     * Connected input stream
     */
    private InputStream in;

    /**
     * Constructs an input stream that can be disconnected from {@code in}.
     *
     * @param in input stream
     */
    public DisconnectableInputStream(InputStream in) {
        if (in != null) {
            this.in = in;
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * Obtains the next byte of data from the connected input stream.
     *
     * @return the byte of data read, or -1 if EndOfStream has been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        return this.in.read();
    }

    /**
     * Reads data from the connected input stream to fill the byte array {@code buffer}.<br>
     * Note that data does not necessarily fill {@code buffer}.
     *
     * @param buffer byte array for storing read data
     * @return the amount of data read into {@code buffer} in bytes, or -1 if EndOfStream has already been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.in.read(buffer, 0, buffer.length);
    }

    /**
     * Reads {@code length} bytes of data into the byte array {@code buffer} starting at the position specified
     * by {@code index} from the connected input stream.<br>
     * Note that {@code length} bytes of data are not necessarily read.
     *
     * @param buffer byte array for storing read data
     * @param index  starting position in {@code buffer} to read data
     * @param length amount of data to read into {@code buffer}
     * @return the amount of data read into {@code buffer} in bytes, or -1 if EndOfStream has already been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        if (0 < length) {
            return this.in.read(buffer, index, length);
        } else {
            return 0;
        }
    }

    /**
     * Skips {@code length} bytes of data in the connected input stream.
     *
     * @param length number of bytes to skip.
     * @return actual number of bytes skipped.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long skip(long length) throws IOException {
        if (0 < length) {
            return this.in.skip(length);
        } else {
            return 0;
        }
    }

    /**
     * Sets a mark at the current position in the connected input stream,
     * allowing returning to the read position at the time of marking
     * using the {@link #reset()} method.
     *
     * @param readLimit the maximum number of bytes that can be read before the mark position becomes invalid.
     *                  If data exceeding this number of bytes is read, {@link #reset()} may not be possible.
     */
    @Override
    public void mark(int readLimit) {
        this.in.mark(readLimit);
    }

    /**
     * Sets the read position of the connected input stream to the position
     * when the {@link #mark(int)} method was last called.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void reset() throws IOException {
        this.in.reset();
    }

    /**
     * Gets whether the connected input stream supports {@link #mark(int)} and {@link #reset()}.
     *
     * @return {@code true} if the stream supports {@link #mark(int)} and {@link #reset()}; {@code false} otherwise.
     */
    public boolean markSupprted() {
        return this.in.markSupported();
    }

    /**
     * Gets the number of bytes that can be read from the connected input stream without blocking.
     *
     * @return the number of bytes that can be read without blocking.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        return this.in.available();
    }

    /**
     * Disconnects from the connected input stream.<br>
     * This method only calls {@link #disconnect()}.
     */
    @Override
    public void close() {
        this.disconnect();
    }

    /**
     * Disconnects from the connected input stream.
     */
    @Override
    public void disconnect() {
        this.in = null;
    }
}
