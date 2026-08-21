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
 * An input stream with a limit on the amount of data that can be read.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: LimitedInputStream.java,v $
 * Revision 1.1.2.1  2003/07/20 17:03:37  dangan
 * [maintenance]
 *     imported source from latest LimitedInputStream.
 *
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [change]
 *     made return values of read(new byte[0]) and
 *     read(byte[] buf, int off, 0) after reaching EndOfStream 0,
 *     same as InputStream
 * [maintenance]
 *     tab removal
 *     license statement fix
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1.2.1 $
 */
public class LimitedInputStream extends InputStream {

    /**
     * Connected input stream
     */
    private InputStream in;

    /**
     * Current read position
     */
    private long position;

    /**
     * Read limit
     */
    private final long limit;

    /**
     * Mark position
     */
    private long markPosition;

    /**
     * Constructs an input stream that limits the amount of data that can be read from {@code in}.<br>
     *
     * @param in    input stream
     * @param limit number of bytes that can be read
     * @throws IllegalArgumentException if {@code limit} is negative
     */
    public LimitedInputStream(InputStream in, long limit) {
        if (in != null && 0 <= limit) {
            this.in = in;
            this.position = 0;
            this.limit = limit;
            this.markPosition = -1;
        } else if (in == null) {
            throw new NullPointerException("in");
        } else {
            throw new IllegalArgumentException("limit must be 0 or more.");
        }
    }

    /**
     * Reads 1 byte of data from the connected input stream.
     *
     * @return the 1 byte of data read, or -1 if EndOfStream or the limit has been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        if (this.position < this.limit) {
            int ret = this.in.read();
            if (0 <= ret) {
                this.position++;
            }
            return ret;
        } else {
            return -1;
        }
    }

    /**
     * Reads data from the connected input stream to fill {@code buffer}.<br>
     * Note that data does not necessarily fill {@code buffer}.
     *
     * @param buffer byte array for storing read data
     * @return the amount of data read into {@code buffer} in bytes, or -1 if EndOfStream or the limit has been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        if (0 < buffer.length) {
            int ret;
            if (buffer.length < this.limit - this.position) {
                ret = this.in.read(buffer);
            } else if (this.position < this.limit) {
                ret = this.in.read(buffer, 0, (int) (this.limit - this.position));
            } else {
                return -1;
            }
            if (0 < ret) {
                this.position += ret;
            }
            return ret;
        } else {
            return 0;
        }
    }

    /**
     * Reads {@code length} bytes of data into the byte array {@code buffer} starting at the position
     * specified by {@code index} from the connected input stream.<br>
     * Note that {@code length} bytes of data are not necessarily read.
     *
     * @param buffer byte array for storing read data
     * @param index  starting position in {@code buffer} to read data
     * @param length amount of data to read into {@code buffer}
     * @return the amount of data read into {@code buffer} in bytes, or -1 if EndOfStream or the limit has been reached.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {

        if (0 < length) {
            if (this.limit <= this.position) {
                return -1;
            } else if (this.limit - this.position < length) {
                length = (int) (this.limit - this.position);
            }
            int ret = this.in.read(buffer, index, length);
            if (0 < ret) {
                this.position += ret;
            }
            return ret;
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
            if (this.limit <= this.position) {
                return 0;
            } else if (this.limit - this.position < length) {
                length = this.limit - this.position;
            }
            length = this.in.skip(length);
            if (0 < length) {
                this.position += length;
            }
            return length;
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
        this.markPosition = this.position;
    }

    /**
     * Sets the read position of the connected input stream to the position
     * when the {@link #mark(int)} method was last called.
     *
     * @throws IOException one of the following:
     *                     <ol>
     *                     <li> When {@link LimitedInputStream} has not been marked.</li>
     *                     <li> When the connected input stream returns {@code false} for {@link #markSupported()}.</li>
     *                     <li> When an I/O error occurs in the connected input stream.</li>
     *                     </ol>
     */
    @Override
    public void reset() throws IOException {
        if (!this.in.markSupported()) {
            throw new IOException("not support mark()/reset().");
        } else if (this.markPosition < 0) { // Takes advantage of markPosition being set to -1 in the constructor.
            throw new IOException("not marked.");
        } else {
            this.in.reset();
            this.position = this.markPosition;
        }
    }

    /**
     * Gets whether the connected input stream supports {@link #mark(int)} and {@link #reset()}.
     *
     * @return {@code true} if the stream supports {@link #mark(int)} and {@link #reset()}; {@code false} otherwise.
     */
    @Override
    public boolean markSupported() {
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
        return (int) Math.min(this.in.available(), this.limit - this.position);
    }

    /**
     * Closes this input stream and releases all resources used.<br>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        this.in.close();
        this.in = null;
    }
}
