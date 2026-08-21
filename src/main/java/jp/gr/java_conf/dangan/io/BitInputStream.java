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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;


/**
 * Utility class for bit input.
 *
 * <pre>
 * -- revision history --
 * $Log: BitInputStream.java,v $
 * Revision 1.5  2002/12/07 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.4  2002/11/15 00:00:00  dangan
 * [improvement]
 *     modified prefetchBits() to guarantee reading 32 bits
 * [change]
 *     renamed methods
 *     prefetchBit     -> peekBit
 *     prefetchBoolean -> peekBoolean
 *     prefetchBits    -> peekBits
 *
 * Revision 1.3  2002/11/02 00:00:00  dangan
 * [bug fix]
 *     available() and availableBits() returned values larger than
 *     the amount that could be read without blocking.
 *
 * Revision 1.2  2002/09/05 00:00:00  dangan
 * [change]
 *     made return values of read(new byte[0]) and
 *     read(byte[] buf, int off, 0) after reaching EndOfStream 0,
 *     same as InputStream
 *
 * Revision 1.1  2002/09/04 00:00:00  dangan
 * [bug fix]
 *     skip(len) and skipBits(len) were not handled correctly
 *     when len is less than 0.
 *
 * Revision 1.0  2002/09/03 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     in mark(), because the calculation of readLimit passed to the connected in was loose,
 *     the mark position was sometimes discarded before reaching the requested readLimit.
 *     skip() and skip(0) after reaching EndOfStream returned -1.
 * [maintenance]
 *     tab removal
 *     license statement fix
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.5 $
 */
public class BitInputStream extends InputStream {

    private static final Logger logger = System.getLogger(BitInputStream.class.getName());

    /**
     * Default cache size
     */
    private static final int DefaultCacheSize = 1024;

    /**
     * Connected input stream
     */
    private InputStream in;

    /**
     * Byte array for caching to prevent performance degradation
     */
    private byte[] cache;

    /**
     * Number of valid bytes in {@code cache}
     */
    private int cacheLimit;

    /**
     * Current processing position in {@code cache}
     */
    private int cachePosition;

    /**
     * Bit buffer.
     * Bit data is stored from the most significant bit for {@code bitCount}.
     */
    private int bitBuffer;

    /**
     * Number of valid bits in {@code bitBuffer}
     */
    private int bitCount;

    /**
     * Indicates whether the mark position is within the range of cache.
     * Set to {@code true} when marked, and set to {@code false} when the next read
     * from {@code in} into cache is performed.
     */
    private boolean markPositionIsInCache;

    /** For backing up {@code cache} */
    private byte[] markCache;

    /** For backing up {@code cacheLimit} */
    private int markCacheLimit;

    /** For backing up {@code cachePosition} */
    private int markCachePosition;

    /** For backing up {@code bitBuffer} */
    private int markBitBuffer;

    /** For backing up {@code bitCount} */
    private int markBitCount;

    /**
     * Constructs a stream that can read data from the input stream {@code in} bit by bit.
     *
     * @param in input stream
     */
    public BitInputStream(InputStream in) {
        this(in, BitInputStream.DefaultCacheSize);
    }

    /**
     * Constructs a stream that can read data from the input stream {@code in} bit by bit.
     *
     * @param in        input stream
     * @param cacheSize buffer size
     */
    public BitInputStream(InputStream in, int cacheSize) {
        if (in != null && 4 <= cacheSize) {
            this.in = in;
            this.cache = new byte[cacheSize];
            this.cacheLimit = 0;
            this.cachePosition = 0;
            this.bitBuffer = 0;
            this.bitCount = 0;

            this.markPositionIsInCache = false;
            this.markCache = null;
            this.markCacheLimit = 0;
            this.markCachePosition = 0;
            this.markBitBuffer = 0;
            this.markBitCount = 0;
        } else if (in == null) {
            throw new NullPointerException("in");
        } else {
            throw new IllegalArgumentException("cacheSize must be 4 or more.");
        }
    }

    /**
     * Reads 8 bits of data from the connected stream.
     *
     * @return the 8-bit data read, or -1 if EndOfStream has already been reached
     * @throws IOException            if an I/O error occurs in the connected input stream
     * @throws BitDataBrokenException if reading data of the requested number of bits failed because EndOfStream was reached.
     */
    @Override
    public int read() throws IOException {
        try {
            return this.readBits(8);
        } catch (LocalEOFException exception) {
            if (exception.thrownBy(this))
                return -1;
            else
                throw exception;
        }
    }

    /**
     * Reads data from the connected input stream to fill the byte array {@code buffer}.<br>
     * Note that data does not necessarily fill {@code buffer}.
     *
     * @param buffer byte array for storing read data
     * @return the amount of data read into {@code buffer} in bytes, or -1 if EndOfStream has already been reached.
     * @throws IOException            if an I/O error occurs in the connected input stream
     * @throws BitDataBrokenException if reading data of the requested number of bits failed because EndOfStream was reached.
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    /**
     * Reads {@code length} bytes of data starting from the position specified
     * by {@code index} into the byte array {@code buffer} from the connected input stream.<br>
     * This method blocks until {@code length} bytes are read or EndOfStream is reached.
     * Note that {@code length} bytes of data are not necessarily read.<br>
     *
     * @param buffer byte array for storing read data
     * @param index  starting position in {@code buffer} to read data
     * @param length amount of data to read into {@code buffer}
     * @return the amount of data read into {@code buffer} in bytes, or -1 if EndOfStream has already been reached.
     * @throws IOException            if an I/O error occurs in the connected input stream
     * @throws BitDataBrokenException if reading data of the requested number of bits failed because EndOfStream was reached.
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        int requested = length;
        try {
            while (0 < length) {
                buffer[index++] = (byte) this.readBits(8);
                length--;
            }
            return requested;
        } catch (LocalEOFException exception) {
            if (exception.thrownBy(this)) {
                if (requested != length)
                    return requested - length;
                else
                    return -1;
            } else {
                throw exception;
            }
        } catch (BitDataBrokenException exception) {
            if (exception.getCause() instanceof LocalEOFException
                    && ((LocalEOFException) exception.getCause()).thrownBy(this)) {
                this.bitBuffer >>>= exception.getBitCount();
                this.bitCount += exception.getBitCount();
                this.bitBuffer |= exception.getBitData() << (32 - exception.getBitCount());

                return requested - length;
            } else {
                throw exception;
            }
        }
    }

    /**
     * Skips {@code length} bytes of data in the connected input stream.<br>
     * This method blocks until {@code length} bytes are skipped or EndOfStream is reached.
     * Note that {@code length} bytes of data are not necessarily skipped.
     *
     * @param length number of bytes to skip.
     * @return actual number of bytes skipped.
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    @Override
    public long skip(long length) throws IOException {
        length = (0 < length ? length : 0);
        long requested = length;
        try {
            while (0 < length) {
                this.readBits(8);
                length--;
            }
            return requested;
        } catch (LocalEOFException exception) {
            return requested - length;
        } catch (BitDataBrokenException exception) {
            if (exception.getCause() instanceof LocalEOFException
                    && ((LocalEOFException) exception.getCause()).thrownBy(this)) {
                this.bitBuffer >>>= exception.getBitCount();
                this.bitCount += exception.getBitCount();
                this.bitBuffer |= exception.getBitData() << (32 - exception.getBitCount());
                return requested - length;
            } else {
                throw exception;
            }
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
        readLimit -= this.cacheLimit - this.cachePosition;
        readLimit -= this.bitCount / 8;
        readLimit += 4;
        readLimit = ((readLimit / this.cache.length) * this.cache.length
                + (readLimit % this.cache.length == 0 ? 0 : this.cache.length));

        this.in.mark(readLimit);

        if (this.markCache == null) {
            this.markCache = this.cache.clone();
        } else {
            System.arraycopy(this.cache, 0, this.markCache, 0, this.cacheLimit);
        }
        this.markCacheLimit = this.cacheLimit;
        this.markCachePosition = this.cachePosition;
        this.markBitBuffer = this.bitBuffer;
        this.markBitCount = this.bitCount;
        this.markPositionIsInCache = true;
    }

    /**
     * Sets the read position of the connected input stream to the position
     * when the {@link #mark(int)} method was last called.
     *
     * @throws IOException <ul>
     *                     <li> When {@link BitInputStream} has not been marked.</li>
     *                     <li> When the connected input stream returns {@code false} for {@link #markSupported()}.</li>
     *                     <li> When an I/O error occurs in the connected input stream.</li>
     *                     </ul>
     */
    @Override
    public void reset() throws IOException {
        if (this.markPositionIsInCache) {
            this.cachePosition = this.markCachePosition;
            this.bitBuffer = this.markBitBuffer;
            this.bitCount = this.markBitCount;
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
            this.bitBuffer = this.markBitBuffer;
            this.bitCount = this.markBitCount;
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
        return this.availableBits() / 8;
    }

    /**
     * Closes this input stream and releases any resources used.
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
        this.bitBuffer = 0;
        this.bitCount = 0;

        this.markCache = null;
        this.markCacheLimit = 0;
        this.markCachePosition = 0;
        this.markBitBuffer = 0;
        this.markBitCount = 0;
        this.markPositionIsInCache = false;
    }

    /**
     * Reads 1 bit of data from the connected input stream.
     *
     * @return the 1-bit data read, or -1 if EndOfStream has already been reached.
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    public int readBit() throws IOException {
        if (0 < this.bitCount) {
            int bit = this.bitBuffer >>> 31;
            this.bitBuffer <<= 1;
            this.bitCount -= 1;
            return bit;
        } else {
            try {
                this.fillBitBuffer();
                int bit = this.bitBuffer >>> 31;
                this.bitBuffer <<= 1;
                this.bitCount -= 1;
                return bit;
            } catch (LocalEOFException exception) {
                if (exception.thrownBy(this)) {
                    return -1;
                } else {
                    throw exception;
                }
            }
        }
    }

    /**
     * Reads 1 bit of data from the connected input stream as a boolean value.
     *
     * @return {@code true} if the read 1-bit data is 1; {@code false} if 0.
     * @throws EOFException if EndOfStream has already been reached
     * @throws IOException  if an I/O error occurs in the connected input stream
     */
    public boolean readBoolean() throws IOException {
        if (0 < this.bitCount) {
            boolean bool = (this.bitBuffer < 0);
            this.bitBuffer <<= 1;
            this.bitCount -= 1;
            return bool;
        } else {
            this.fillBitBuffer();
            boolean bool = (this.bitBuffer < 0);
            this.bitBuffer <<= 1;
            this.bitCount -= 1;
            return bool;
        }
    }

    /**
     * Reads {@code count} bits of data from the connected input stream.
     * As can be seen from the return value being an {@code int},
     * the maximum valid number of bits that can be read is 32 bits,
     * but because {@code count} is not checked even if a value of 32 or more is set,
     * bit data beyond that will be discarded.<br>
     * For example, when {@code readBits(33)} is called, 1 bit of data is first discarded,
     * and the following 32 bits of data are returned.<br>
     * Also note that when called with a number less than or equal to 0 for {@code count},
     * since no read operation is involved, the return value is always 0,
     * and {@link EOFException} is not thrown even if EndOfStream is reached.
     *
     * @param count number of bits of data to read
     * @return read bit data.
     * @throws IOException            if an I/O error occurs in the connected input stream
     * @throws EOFException           if EndOfStream has already been reached
     * @throws BitDataBrokenException if reading data of the requested number of bits failed because EndOfStream was reached during reading.
     */
    public int readBits(int count) throws IOException {
        if (0 < count) {
            if (count <= this.bitCount) {
                int bits = this.bitBuffer >>> (32 - count);
                this.bitBuffer <<= count;
                this.bitCount -= count;
                return bits;
            } else {
                int requested = count;
                int bits = 0;
                try {
                    this.fillBitBuffer();
                    while (this.bitCount < count) {
                        count -= this.bitCount;
                        if (count < 32) {
                            bits |= (this.bitBuffer >>> (32 - this.bitCount)) << count;
                        }
                        this.bitBuffer = 0;
                        this.bitCount = 0;
                        this.fillBitBuffer();
                    }
                    bits |= this.bitBuffer >>> (32 - count);
                    this.bitBuffer <<= count;
                    this.bitCount -= count;
                    return bits;
                } catch (LocalEOFException exception) {
                    if (exception.thrownBy(this) && count < requested) {
                        throw new BitDataBrokenException(exception, bits >>> count, requested - count);
                    } else {
                        throw exception;
                    }
                }
            }
        } else {
            return 0;
        }
    }

    /**
     * Skips {@code count} bits of data in the connected stream.
     *
     * @param count number of bits to skip
     * @return actual number of bits skipped
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    public int skipBits(int count) throws IOException {
        count = Math.max(count, 0);


        if (count < this.bitCount) {
            this.bitBuffer <<= count;
            this.bitCount -= count;
            return count;
        } else {
            int requested = count;
            count -= this.bitCount;
            this.bitCount = 0;
            this.bitBuffer = 0;
            try {
                while ((this.cacheLimit - this.cachePosition) * 8 <= count) {
                    count -= (this.cacheLimit - this.cachePosition) * 8;
                    this.cachePosition = this.cacheLimit;
                    this.fillCache();
                    if (this.cacheLimit == this.cachePosition) {
                        throw new LocalEOFException(this);
                    }
                }
                this.cachePosition += (count >> 3);
                count = count & 0x07;
                if (0 < count) {
                    this.bitCount = 8 - count;
                    this.bitBuffer = this.cache[this.cachePosition++] << (24 + count);
                    count = 0;
                }
            } catch (LocalEOFException exception) {
                logger.log(Level.ERROR, exception.getMessage(), exception);
            }
            return requested - count;
        }
    }

    /**
     * Peeks 1 bit of data without changing the read position.
     *
     * @return the 1-bit data read,<br>
     *         or -1 if EndOfStream has already been reached.
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    public int peekBit() throws IOException {
        if (0 < this.bitCount) {
            return this.bitBuffer >>> 31;
        } else {
            try {
                this.fillBitBuffer();
                return this.bitBuffer >>> 31;
            } catch (LocalEOFException exception) {
                if (exception.thrownBy(this)) {
                    return -1;
                } else {
                    throw exception;
                }
            }
        }
    }

    /**
     * Peeks 1 bit of data as a boolean value without changing the read position.
     *
     * @return {@code true} if the read 1-bit data is 1; {@code false} if 0.
     * @throws EOFException if EndOfStream has already been reached
     * @throws IOException  if an I/O error occurs in the connected input stream
     */
    public boolean peekBoolean() throws IOException {
        if (0 < this.bitCount) {
            return this.bitBuffer < 0;
        } else {
            this.fillBitBuffer();
            return this.bitBuffer < 0;
        }
    }

    /**
     * Peeks {@code count} bits of data without changing the read position.<br>
     * As can be seen from the return value being an {@code int},
     * the maximum valid number of bits is 32 bits.<br>
     * Except near EndOfStream, the number of bits guaranteed to be peekable is 32 bits
     * (because the size of the bit buffer is 32 bits).<br>
     * If peeking 32 bits or more is indispensable, repeat {@link #mark(int)}, {@link #readBits(int)},
     * and {@link #reset()}, or give up using this class.<br>
     *
     * @param count number of bits to read
     * @return peeked {@code count} bits of bit data
     * @throws EOFException           if EndOfStream has already been reached
     * @throws IOException            if an I/O error occurs in the connected input stream
     * @throws NotEnoughBitsException if {@code count} is out of the peekable range
     */
    public int peekBits(int count) throws IOException {
        if (0 < count) {
            if (count <= this.bitCount) {
                return this.bitBuffer >>> (32 - count);
            } else {
                this.fillBitBuffer();
                if (count <= this.bitCount) {
                    return this.bitBuffer >>> (32 - count);
                } else if (count <= this.cachedBits()) {
                    if (count <= 32) {
                        int bits = this.bitBuffer;
                        bits |= (this.cache[this.cachePosition] & 0xFF) >> (this.bitCount - 24);
                        return bits >>> (32 - count);
                    } else if (count - 32 < this.bitCount) {
                        int bits = this.bitBuffer << (count - 32);
                        int bcnt = this.bitCount - (count - 32);
                        int pos = this.cachePosition;
                        while (bcnt < 25) {
                            bits |= (this.cache[pos++] & 0xFF) << (24 - bcnt);
                            bcnt += 8;
                        }
                        if (bcnt < 32) {
                            bits |= (this.cache[pos] & 0xFF) >> (bcnt - 24);
                        }
                        return bits;
                    } else {
                        count -= this.bitCount;
                        count -= 32;
                        int pos = this.cachePosition + (count >> 3);
                        count &= 0x07;
                        if (0 < count) {
                            return (this.cache[pos] << (24 + count)) | ((this.cache[pos + 1] & 0xFF) << (16 + count))
                                    | ((this.cache[pos + 2] & 0xFF) << (8 + count)) | ((this.cache[pos + 3] & 0xFF) << count)
                                    | ((this.cache[pos + 4] & 0xFF) >> (8 - count));
                        } else {
                            return (this.cache[pos] << 24) | ((this.cache[pos + 1] & 0xFF) << 16)
                                    | ((this.cache[pos + 2] & 0xFF) << 8) | (this.cache[pos + 3] & 0xFF);
                        }
                    }
                } else {
                    throw new NotEnoughBitsException(this.cachedBits());
                }
            }
        } else {
            return 0;
        }
    }

    /**
     * Gets the number of bits that can be read from the connected input stream without blocking.
     *
     * @return the number of bits that can be read without blocking.
     * @throws IOException if an I/O error occurs in the connected input stream
     */
    public int availableBits() throws IOException {
        int avail = (this.cacheLimit - this.cachePosition) + this.in.available() / this.cache.length * this.cache.length;
        avail += this.bitCount - 32;

        return Math.max(avail, 0);
    }

    /**
     * Gets the number of bits stored in this {@link BitInputStream}.
     *
     * @return the number of bits stored in this {@link BitInputStream}.
     */
    private int cachedBits() {
        return this.bitCount + ((this.cacheLimit - this.cachePosition) << 3);
    }

    /**
     * Fills {@code bitBuffer} with data.
     * Except near EndOfStream, it is guaranteed that 25 bits of data
     * are secured in {@code bitBuffer}.
     *
     * @throws IOException       if an I/O error occurs
     * @throws LocalEOFException if EndOfStream is reached while {@code bitBuffer} is empty
     */
    private void fillBitBuffer() throws IOException {
        if (32 <= this.cachedBits()) {
            if (this.bitCount <= 24) {
                if (this.bitCount <= 16) {
                    if (this.bitCount <= 8) {
                        if (this.bitCount <= 0) {
                            this.bitBuffer = this.cache[this.cachePosition++] << 24;
                            this.bitCount = 8;
                        }
                        this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                        this.bitCount += 8;
                    }
                    this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                    this.bitCount += 8;
                }
                this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                this.bitCount += 8;
            }
        } else if (this.bitCount < 25) {
            if (this.bitCount == 0) {
                this.bitBuffer = 0;
            }

            int count = Math.min((32 - this.bitCount) >> 3, this.cacheLimit - this.cachePosition);
            while (0 < count--) {
                this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                this.bitCount += 8;
            }
            this.fillCache();
            if (this.cachePosition < this.cacheLimit) {
                count = Math.min((32 - this.bitCount) >> 3, this.cacheLimit - this.cachePosition);
                while (0 < count--) {
                    this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                    this.bitCount += 8;
                }
            } else if (this.bitCount <= 0) {
                throw new LocalEOFException(this);
            }
        }
    }

    /**
     * Reads data into {@code cache} when {@code cache} becomes empty.
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

    /**
     * Since using {@link EOFException} for detecting EndOfStream inside {@link BitInputStream}
     * is somewhat problematic, define a local {@link EOFException}.
     */
    private static class LocalEOFException extends EOFException {

        /**
         * The object that threw this exception
         */
        private final Object owner;

        /**
         * Constructor.
         *
         * @param object the object that threw this exception
         */
        public LocalEOFException(Object object) {
            super();
            this.owner = object;
        }

        /**
         * Gets whether this exception was thrown by {@code object}.
         *
         * @param object object
         * @return {@code true} if this exception was thrown by {@code object}; {@code false} otherwise
         */
        public boolean thrownBy(Object object) {
            return this.owner == object;
        }
    }
}
