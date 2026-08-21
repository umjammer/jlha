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
import java.io.OutputStream;


/**
 * An output stream class for outputting bit data to a connected output stream.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: BitOutputStream.java,v $
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/09/11 00:00:00  dangan
 * add to version control
 * [change]
 *     modified write methods and flush() to throw an exception
 *     after close()
 * [maintenance]
 *     tab removal
 *     license statement fix
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class BitOutputStream extends OutputStream {

    /**
     * Default cache size
     */
    private static final int DefaultCacheSize = 1024;

    /**
     * Connected output stream
     */
    private OutputStream out;

    /**
     * Byte array for caching to prevent performance degradation
     */
    private byte[] cache;

    /**
     * Current processing position in {@code cache}
     */
    private int cachePosition;

    /**
     * Bit buffer
     */
    private int bitBuffer;

    /**
     * Number of valid bits in {@code bitBuffer}
     */
    private int bitCount;

    /**
     * Constructs a stream that can write data to the output stream {@code out} bit by bit.<br>
     * The default value is used for the cache size.
     *
     * @param out output stream
     */
    public BitOutputStream(OutputStream out) {
        this(out, BitOutputStream.DefaultCacheSize);
    }

    /**
     * Constructs a stream that can write data to the output stream {@code out} bit by bit.
     *
     * @param out       output stream
     * @param CacheSize cache size
     * @throws IllegalArgumentException if {@code CacheSize} is less than 4, or {@code CacheSize} is not a multiple of 4.
     */
    public BitOutputStream(OutputStream out, int CacheSize) {
        if (out != null && 4 <= CacheSize && 0 == (CacheSize & 0x03)) {
            this.out = out;
            this.cache = new byte[CacheSize];
            this.cachePosition = 0;
            this.bitBuffer = 0;
            this.bitCount = 0;
        } else if (out == null) {
            throw new NullPointerException("out");
        } else if (CacheSize < 4) {
            throw new IllegalArgumentException("CacheSize must be 4 or more.");
        } else {
            throw new IllegalArgumentException("CacheSize must be multiple of 4.");
        }
    }

    /**
     * Outputs 8-bit data to the connected output stream.
     *
     * @param data 8-bit data. The upper 24 bits are ignored.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(int data) throws IOException {
        this.writeBits(8, data);
    }

    /**
     * Outputs the contents of {@code buffer} as continuous 8-bit data
     * to the connected output stream.
     *
     * @param buffer byte array containing data to be output
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    /**
     * Outputs {@code length} bytes of data from {@code index} of {@code buffer}
     * as continuous 8-bit data to the connected output stream.
     *
     * @param buffer byte array containing data to be output
     * @param index  starting position of data in {@code buffer}
     * @param length amount of data to output (number of bytes)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] buffer, int index, int length) throws IOException {
        if (this.bitCount % 8 == 0) {
            this.flush();
            this.out.write(buffer, index, length);
        } else {
            while (0 < length--)
                this.writeBits(8, buffer[index++]);
        }
    }

    /**
     * Flushes all 8-bit unit data buffered in this bit output stream
     * to the output destination.<br>
     * Note that data of less than 8 bits is not output.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        while (8 <= this.bitCount) {
            this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 24);
            this.bitBuffer <<= 8;
            this.bitCount -= 8;
        }

        this.out.write(this.cache, 0, this.cachePosition);
        this.cachePosition = 0;
        this.out.flush();
    }

    /**
     * Closes this output stream and the connected output stream,
     * and releases any resources used.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        while (0 < this.bitCount) {
            this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 24);
            this.bitBuffer <<= 8;
            this.bitCount -= 8;
        }

        this.out.write(this.cache, 0, this.cachePosition);
        this.cachePosition = 0;
        this.out.flush();
        this.out.close();

        this.out = null;
        this.cache = null;
        this.cachePosition = 0;
        this.bitCount = 128;
        this.bitBuffer = 0;
    }

    /**
     * Outputs 1 bit of data to the connected output stream.
     *
     * @param data 1-bit data. The upper 31 bits are ignored.
     * @throws IOException if an I/O error occurs
     */
    public void writeBit(int data) throws IOException {
        this.bitBuffer |= (data & 0x00000001) << 31 - this.bitCount;
        this.bitCount++;

        if (32 <= this.bitCount)
            this.writeOutBitBuffer();
    }

    /**
     * Outputs a boolean value as 1-bit data to the connected output stream.<br>
     * {@code true} is output as 1, and {@code false} as 0.
     * Note that there is no compatibility with {@link java.io.DataOutput#writeBoolean(boolean)}.<br>
     *
     * @param bool boolean value
     * @throws IOException if an I/O error occurs
     */
    public void writeBoolean(boolean bool) throws IOException {
        if (bool)
            this.bitBuffer |= 1 << 31 - this.bitCount;

        this.bitCount++;

        if (32 <= this.bitCount)
            this.writeOutBitBuffer();
    }

    /**
     * Outputs bit data to the connected output stream.
     *
     * @param count number of valid bits in {@code data}
     * @param data  bit data
     * @throws IOException if an I/O error occurs
     */
    public void writeBits(int count, int data) throws IOException {
        while (0 < count) {
            int available = 32 - this.bitCount;
            if (count < available) {
                this.bitBuffer |= (data & (0xFFFFFFFF >>> 32 - count)) << available - count;
                this.bitCount += count;
                count = 0;
            } else {
                count -= available;
                this.bitBuffer |= data >> count & (0xFFFFFFFF >>> 32 - available);
                this.writeOutBitBuffer();
            }
        }
    }

    /**
     * Outputs all data stored in the bit buffer to the cache,
     * and outputs the data in the cache to the connected output stream
     * if the cache is full.<br>
     *
     * @throws IOException if an I/O error occurs
     */
    private void writeOutBitBuffer() throws IOException {
        this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 24);
        this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 16);
        this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 8);
        this.cache[this.cachePosition++] = (byte) this.bitBuffer;

        this.bitBuffer = 0;
        this.bitCount = 0;

        if (this.cache.length <= this.cachePosition) {
            this.out.write(this.cache);
            this.cachePosition = 0;
        }
    }
}
