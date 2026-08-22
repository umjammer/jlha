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

package jp.gr.java_conf.dangan.io;

import java.io.IOException;
import java.io.InputStream;


/**
 * An input stream to improve speed using a cache.<br>
 * Unlike {@link java.io.BufferedInputStream}, read methods are not synchronized
 * so there is no synchronization overhead, and {@code mark}/{@code reset} is only supported
 * when it can be performed by moving the read position within the cache,
 * beyond which it depends on the performance of the connected input stream.
 *
 * <pre>
 * -- revision history --
 * $Log: CachedInputStream.java,v $
 * Revision 1.3  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.2  2002/11/02 00:00:00  dangan
 * [bug fix]
 *     available() returned a value larger than the amount that could be read without blocking.
 *
 * Revision 1.1  2002/09/05 00:00:00  dangan
 * [change]
 *     made return values of read(new byte[0]) and
 *     read(byte[] buf, int off, 0) after reaching EndOfStream 0,
 *     same as InputStream.
 *
 * Revision 1.0  2002/09/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     in mark(), because the calculation of readLimit passed to the connected in was loose,
 *     the mark position was sometimes discarded before reaching the requested readLimit.
 *     reversed dst and src in System.arraycopy call within read(buf, off, len).
 * [change]
 *     modified read(new byte[0]) and read(buf, off, 0) to return -1 after reaching EndOfStream.
 * [maintenance]
 *     tab removal
 *     license statement fix
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.3 $
 */
public class CachedInputStream extends InputStream {

    /**
     * Default cache size
     */
    private static final int DefaultCacheSize = 1024;

    /**
     * Input stream supplying data
     */
    private InputStream in;

    /**
     * Cache for storing data
     */
    private byte[] cache;

    /**
     * Current processing position in {@code cache}
     */
    private int cachePosition;

    /**
     * Read limit position of {@code cache}
     */
    private int cacheLimit;

    /**
     * Indicates whether the mark position is within the range of cache.
     * Set to {@code true} when marked, and set to {@code false} when the next read
     * from {@code in} into cache is performed.
     */
    private boolean markPositionIsInCache;

    /** For backing up {@code cache} */
    private byte[] markCache;

    /** For backing up {@code cachePosition} */
    private int markCachePosition;

    /** For backing up {@code cacheLimit} */
    private int markCacheLimit;

    /**
     * Constructs a {@link CachedInputStream} with the default cache size.
     *
     * @param in input stream that requires caching
     * @throws IllegalArgumentException if {@code in} is {@code null}
     */
    public CachedInputStream(InputStream in) {
        this(in, CachedInputStream.DefaultCacheSize);
    }

    /**
     * Constructs a {@link CachedInputStream} with the specified cache size.
     *
     * @param in        input stream that requires caching
     * @param cacheSize cache size
     * @throws IllegalArgumentException if {@code cacheSize} is less than or equal to 0, or {@code in} is {@code null}
     */
    public CachedInputStream(InputStream in, int cacheSize) {
        if (in != null && 0 < cacheSize) {
            this.in = in;

            this.cache = new byte[cacheSize];
            this.cachePosition = 0;
            this.cacheLimit = 0;

            this.markPositionIsInCache = false;
            this.markCache = null;
            this.markCachePosition = 0;
            this.markCacheLimit = 0;

        } else if (in == null) {
            throw new IllegalArgumentException("in must not be null.");
        } else {
            throw new IllegalArgumentException("cacheSize must be one or more.");
        }
    }

    /**
     * Reads 1 byte of data mapped to 0-255 from the connected stream.
     *
     * @return the 1 byte of data read, or -1 if EndOfStream has already been reached.
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    @Override
    public int read() throws IOException {
        if (this.cachePosition < this.cacheLimit) {
            return this.cache[this.cachePosition++] & 0xFF;
        } else {
            this.fillCache();

            if (this.cachePosition < this.cacheLimit) {
                return this.cache[this.cachePosition++] & 0xFF;
            } else {
                return -1;
            }
        }
    }

    /**
     * Reads data from the connected stream to fill {@code buffer}.<br>
     * This method blocks until data is read to fill {@code buffer} or EndOfStream is reached.
     *
     * @param buffer byte array for storing read data
     * @return the amount of data read into {@code buffer} in bytes, or -1 if EndOfStream has already been reached.
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    /**
     * Reads {@code length} bytes of data into {@code buffer} starting at the position specified
     * by {@code index} from the connected stream.<br>
     * This method blocks until {@code length} bytes are read or EndOfStream is reached.
     *
     * @param buffer byte array for storing read data
     * @param index  starting position in {@code buffer} to read data
     * @param length amount of data to read into {@code buffer}
     * @return the amount of data read into {@code buffer} in bytes, or -1 if EndOfStream has already been reached.
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        int requested = length;

        while (0 < length) {
            if (this.cacheLimit <= this.cachePosition) {
                this.fillCache();
                if (this.cacheLimit <= this.cachePosition) {
                    if (requested == length) {
                        return -1;
                    } else {
                        break;
                    }
                }
            }

            int copylen = Math.min(length, this.cacheLimit - this.cachePosition);
            System.arraycopy(this.cache, this.cachePosition, buffer, index, copylen);

            index += copylen;
            length -= copylen;
            this.cachePosition += copylen;
        }

        return requested - length;
    }

    /**
     * Skips {@code length} bytes of data in the connected input stream.<br>
     * This method blocks until {@code length} bytes are skipped or EndOfStream is reached.
     *
     * @param length number of bytes to skip.
     * @return actual number of bytes skipped.
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    @Override
    public long skip(long length) throws IOException {
        long requested = length;

        while (0 < length) {
            if (this.cacheLimit <= this.cachePosition) {
                this.fillCache();

                if (this.cacheLimit <= this.cachePosition) {
                    break;
                }
            }

            long skiplen = Math.min(length, this.cacheLimit - this.cachePosition);

            length -= skiplen;
            this.cachePosition += (int) skiplen;
        }

        return requested - length;
    }

    /**
     * Sets a mark at the current position in the connected input stream,
     * allowing returning to the read position at the time of marking
     * using the {@link #reset()} method.<br>
     *
     * @param readLimit the maximum number of bytes that can be read before the mark position becomes invalid.
     *                  If data exceeding this number of bytes is read, {@link #reset()} may not be possible.
     */
    @Override
    public void mark(int readLimit) {
        readLimit -= this.cacheLimit - this.cachePosition;
        readLimit = (readLimit / this.cache.length) * this.cache.length
                + (readLimit % this.cache.length == 0 ? 0 : this.cache.length);

        this.in.mark(readLimit);

        if (this.markCache == null) {
            this.markCache = this.cache.clone();
        } else {
            System.arraycopy(this.cache, 0, this.markCache, 0, this.cacheLimit);
        }

        this.markCacheLimit = this.cacheLimit;
        this.markCachePosition = this.cachePosition;
        this.markPositionIsInCache = true;
    }

    /**
     * Sets the read position of the connected input stream to the position
     * when the {@link #mark(int)} method was last called.
     *
     * @throws IOException one of the following:
     *                     <ol>
     *                     <li> When {@link CachedInputStream} has not been marked.</li>
     *                     <li> When the connected input stream returns {@code false} for {@link #markSupported()}.</li>
     *                     <li> When an I/O error occurs in the connected input stream.</li>
     *                     </ol>
     */
    @Override
    public void reset() throws IOException {
        if (this.markPositionIsInCache) {
            this.cachePosition = this.markCachePosition;
        } else if (!this.in.markSupported()) {
            throw new IOException("not support mark()/reset().");
        } else if (this.markCache == null) { // This condition indicates that it has not been marked yet. Takes advantage of markCache being set to null in the constructor.
            throw new IOException("not marked.");
        } else {
            // If in cannot reset(), it is expected that this.in.reset() in the first line throws an IOException.
            this.in.reset();
            System.arraycopy(this.markCache, 0, this.cache, 0, this.markCacheLimit);
            this.cacheLimit = this.markCacheLimit;
            this.cachePosition = this.markCachePosition;
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
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    @Override
    public int available() throws IOException {
        return this.cacheLimit - this.cachePosition + (this.in.available() / this.cache.length) * this.cache.length;
    }

    /**
     * Closes this input stream and releases all resources used.
     *
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    @Override
    public void close() throws IOException {
        this.in.close();
        this.in = null;

        this.cache = null;
        this.cacheLimit = 0;
        this.cachePosition = 0;

        this.markCache = null;
        this.markCacheLimit = 0;
        this.markCachePosition = 0;
        this.markPositionIsInCache = false;
    }

    /**
     * Called when necessary to replenish data into the cache buffer
     * to ensure that data always exists in the cache buffer.<br>
     * If EndOfStream has been reached, this is indicated by not replenishing data.
     *
     * @throws IOException if an I/O error occurs
     */
    private void fillCache() throws IOException {
        this.markPositionIsInCache = false;
        this.cacheLimit = 0;
        this.cachePosition = 0;

        // Read data into cache
        int read = 0;
        while (0 <= read && this.cacheLimit < this.cache.length) {
            read = this.in.read(this.cache, this.cacheLimit, this.cache.length - this.cacheLimit);

            if (0 < read)
                this.cacheLimit += read;
        }
    }
}
