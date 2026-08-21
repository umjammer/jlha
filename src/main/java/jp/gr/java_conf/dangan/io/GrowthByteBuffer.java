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

/**
 * An automatically expanding buffer.<br>
 * Used as an in-memory version of {@link java.io.RandomAccessFile}.
 * However, it is not suitable for handling very huge data.
 * Not thread-safe.
 * Not compatible with JDK 1.4+ {@link java.nio.ByteBuffer}.
 *
 * <pre>
 * -- revision history --
 * $Log: GrowthByteBuffer.java,v $
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     fixed incorrect calculation of buffer increase in grow().
 * [change]
 *     made return values of read(new byte[0]) and
 *     read(byte[] buf, int off, 0) after reaching read limit 0,
 *     same as InputStream
 * [maintenance]
 *     source maintenance
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class GrowthByteBuffer {

    /**
     * Default size of a single buffer
     */
    private static final int DefaultBufferSize = 16384;

    /**
     * Buffer.
     * An array of byte arrays, all having the same size as {@code buffer[0].length}.
     */
    private byte[][] buffer;

    /**
     * Current processing position.
     * {@code position} may also be after {@code limit}.
     */
    private int position;

    /**
     * Current read limit.
     * Data after this is undefined.
     * Note that data at this position can be read.
     */
    private int limit;

    /**
     * Constructs a buffer that automatically expands in size.<br>
     * The default value is used for the buffer size.
     */
    public GrowthByteBuffer() {
        this(GrowthByteBuffer.DefaultBufferSize);
    }

    /**
     * Constructs a buffer that automatically expands in size.
     *
     * @param BufferSize buffer size
     */
    public GrowthByteBuffer(int BufferSize) {
        if (0 < BufferSize) {
            this.buffer = new byte[16][];
            this.buffer[0] = new byte[BufferSize];
            this.position = 0;
            this.limit = -1;
        } else {
            throw new IllegalArgumentException("BufferSize most be 1 or more.");
        }
    }

    /**
     * Writes 1 byte of data at the current position.
     *
     * @param data 1 byte of data
     */
    public void write(int data) {
        this.grow(this.position);

        this.buffer[this.position / this.buffer[0].length][this.position % this.buffer[0].length] = (byte) data;

        this.position++;
    }

    /**
     * Writes the contents of {@code buffer} at the current position.
     *
     * @param buffer buffer containing data to write
     */
    public void write(byte[] buffer) {
        this.write(buffer, 0, buffer.length);
    }

    /**
     * Writes {@code length} bytes of data from {@code index} of {@code buffer} at the current position.
     *
     * @param buffer buffer containing data to write
     * @param index  starting position of data to write in {@code buffer}
     * @param length amount of data to write
     */
    public void write(byte[] buffer, int index, int length) {
        this.grow(this.position + length - 1);

        while (0 < length) {
            int copylen = Math.min((this.position / this.buffer[0].length + 1) * this.buffer[0].length, this.position + length)
                          - this.position;

            System.arraycopy(buffer,
                             index,
                             this.buffer[this.position / this.buffer[0].length],
                             this.position % this.buffer[0].length,
                             copylen);

            this.position += copylen;
            index += copylen;
            length -= copylen;
        }
    }

    /**
     * Reads 1 byte of data from the current position.
     *
     * @return the 1 byte of data read.<br>
     *         -1 if attempting to read beyond the read limit
     */
    public int read() {
        if (this.position <= this.limit) {
            return this.buffer[this.position / this.buffer[0].length][this.position++ % this.buffer[0].length] & 0xFF;
        } else {
            return -1;
        }
    }

    /**
     * Reads data from the current position to fill {@code buffer}.
     *
     * @param buffer buffer to read data into
     * @return the amount of data actually read<br>
     *         -1 if attempting to read beyond the read limit
     */
    public int read(byte[] buffer) {
        return this.read(buffer, 0, buffer.length);
    }

    /**
     * Reads {@code length} data from the current position into {@code index} of {@code buffer}.
     *
     * @param buffer buffer to read data into
     * @param index  position in {@code buffer} to read data into
     * @param length amount of data to read
     * @return the amount of data actually read<br>
     *         -1 if attempting to read beyond the read limit
     */
    public int read(byte[] buffer, int index, int length) {
        if (this.position <= this.limit) {
            int len = 0;
            while (0 < length) {
                int copylen = Math.min(Math.min((this.position / this.buffer[0].length + 1) * this.buffer[0].length,
                                                this.position + length),
                                       this.limit + 1)
                              - this.position;
                if (0 < copylen) {
                    System.arraycopy(this.buffer[this.position / this.buffer[0].length],
                                     this.position % this.buffer[0].length,
                                     buffer,
                                     index,
                                     copylen);

                    this.position += copylen;
                    index += copylen;
                    len += copylen;
                    length -= copylen;
                } else {
                    break;
                }
            }
            return len;
        } else if (0 < length) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * Gets the current read limit.
     *
     * @return current read limit
     */
    public int length() {
        return this.limit + 1;
    }

    /**
     * Sets the read limit position.
     *
     * @param length new read limit position
     */
    public void setLength(int length) {
        length--;
        if (this.limit < length) {
            this.grow(length);
        } else {
            this.limit = length;
        }
    }

    /**
     * Gets the current position.
     *
     * @return current position
     */
    public int position() {
        return this.position;
    }

    /**
     * Sets the current position.
     * Same as {@link java.io.RandomAccessFile}, the buffer does not increase
     * immediately after setting a value exceeding the read limit with {@link #setPosition(int)}.
     * The buffer increases for the first time when written later by {@code write}.
     *
     * @param position new current position
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Sets the current position.
     * Same as {@link java.io.RandomAccessFile}, the buffer does not increase
     * immediately after setting a value exceeding the read limit with {@link #seek(int)}.
     * The buffer increases for the first time when written later by {@code write}.
     *
     * @param position new current position
     */
    public void seek(int position) {
        this.setPosition(position);
    }

    /**
     * Sets a new read limit {@code limit} and expands the buffer up to {@code limit}.
     *
     * @param limit new read limit
     */
    private void grow(int limit) {
        if (this.limit < limit) {
            int last = 0;
            while (last < this.buffer.length && this.buffer[last] != null)
                last++;

            limit++;
            if (last * this.buffer[0].length < limit) {
                int need = (limit / this.buffer[0].length) + (limit % this.buffer[0].length == 0 ? 0 : 1);

                if (this.buffer.length < need) {
                    byte[][] old = this.buffer;
                    this.buffer = new byte[Math.max(old.length * 2, need)][];

                    for (int i = 0; i < last; i++)
                        this.buffer[i] = old[i];
                }
                for (int i = last; i < need; i++)
                    this.buffer[i] = new byte[this.buffer[0].length];
            }

            this.limit = limit - 1;
        }
    }
}
