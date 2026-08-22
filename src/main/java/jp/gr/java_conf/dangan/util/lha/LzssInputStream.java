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
import java.io.IOException;
import java.io.InputStream;


/**
 * Input stream that supplies data while decompressing LZSS compressed data.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: LzssInputStream.java,v $
 * Revision 1.1  2002/12/08 00:00:00  dangan
 * [bug fix]
 *     Fixed inaccurate calculation of readLimit passed to mark
 *     of connected PreLzssDecoder in mark().
 *
 * Revision 1.0  2002/07/25 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     Fixed spelling mistake in available().
 *     Fixed condition error for invoking decode() in skip().
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     license comment update
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class LzssInputStream extends InputStream {

    /**
     * Input stream that returns LZSS compression codes.
     */
    private PreLzssDecoder decoder;

    /**
     * threshold used for LZSS compression.
     * If the match length is greater than or equal to this value, compression code is output.
     */
    private final int threshold;

    /**
     * Value used for LZSS compression.
     * Indicates maximum match length.
     */
    private final int maxMatch;

    /**
     * Data size after decompression.
     */
    private final long length;

    /**
     * Buffer for expanding LZSS compression.
     */
    private byte[] textBuffer;

    /**
     * Current read position.
     * Indicates the position read out externally by {@link #read()}.
     */
    private long textPosition;

    /**
     * Current decoded position.
     * Indicates the position obtained by decompressing LZSS compression codes.
     */
    private long textDecoded;

    /** For backing up {@link #textBuffer} */
    private byte[] markTextBuffer;

    /** For backing up {@link #textPosition} */
    private long MarkTextPosition;

    /** For backing up {@link #textDecoded} */
    private long MarkTextDecoded;

    /**
     * Constructs an input stream that receives LZSS compressed data input from {@code decoder}
     * and provides decompressed data.
     * A {@link LzssInputStream} created from this constructor cannot correctly restore
     * data where reading past the last decompressed data of -lh1-, etc. is not guaranteed
     * to immediately reach EndOfStream (garbage data may be appended after the end).
     *
     * @param decoder LZSS compressed data supply stream
     */
    public LzssInputStream(PreLzssDecoder decoder) {
        this(decoder, Long.MAX_VALUE);
    }

    /**
     * Constructs an input stream that receives LZSS compressed data input from {@code decoder}
     * and provides decompressed data.
     *
     * @param decoder LZSS compressed data supply stream
     * @param length size after decompression
     */
    public LzssInputStream(PreLzssDecoder decoder, long length) {
        this.maxMatch = decoder.getMaxMatch();
        this.threshold = decoder.getThreshold();
        this.length = length;

        this.decoder = decoder;
        this.textBuffer = new byte[decoder.getDictionarySize()];
        this.textPosition = 0;
        this.textDecoded = 0;

        if (this.decoder instanceof PreLz5Decoder)
            this.initLz5TextBuffer();
    }

    /**
     * Decompresses compressed data from the {@link PreLzssDecoder} specified in constructor
     * and supplies 1 byte of data.
     *
     * @return 1 byte of decompressed data
     *
     * @exception IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        if (this.textDecoded <= this.textPosition) {
            try {
                this.decode();
            } catch (EOFException exception) {
                if (this.textDecoded <= this.textPosition)
                    return -1;
            }
        }

        return this.textBuffer[(int) this.textPosition++ & (this.textBuffer.length - 1)] & 0xFF;
    }

    /**
     * Decompresses compressed data from the {@link PreLzssDecoder} specified in constructor
     * and reads decompressed data to fill {@code buffer}.
     *
     * @param buffer buffer into which data is read
     * @return amount of data read
     * @exception IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    /**
     * Decompresses compressed data from the {@link PreLzssDecoder} specified in constructor
     * and reads {@code length} bytes of data into {@code buffer} starting from {@code index}.
     *
     * @param buffer buffer into which data is read
     * @param index start position in {@code buffer} to read data into
     * @param length amount of data to read
     * @return amount of data read
     * @exception IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        int position = index;
        int end = index + length;
        try {
            while (position < end) {
                if (this.textDecoded <= this.textPosition)
                    this.decode();

                position = this.copyTextBufferToBuffer(buffer, position, end);
            }
        } catch (EOFException exception) {
            position = this.copyTextBufferToBuffer(buffer, position, end);

            if (position == index)
                return -1;
        }

        return position - index;
    }

    /**
     * Skips {@code length} bytes of decompressed data.
     *
     * @param length amount of data to skip (in bytes)
     * @return actual number of bytes skipped
     * @exception IOException if an I/O error occurs
     */
    @Override
    public long skip(long length) throws IOException {
        long end = this.textPosition + length;
        try {
            while (this.textPosition < end) {
                if (this.textDecoded <= this.textPosition)
                    this.decode();

                this.textPosition = Math.min(end, this.textDecoded);
            }
        } catch (EOFException exception) {
            this.textPosition = Math.min(end, this.textDecoded);
        }

        return length - (end - this.textPosition);
    }

    /**
     * Sets a mark at the current position of the connected input stream
     * so that it can return to the read position at the time of marking with {@link #reset()}.<br>
     * Unlike {@link InputStream#mark(int)}, the mark position may become invalid
     * before the limit byte count set by {@code readLimit}.
     * However, if connected to an {@link InputStream} that ignores {@code readLimit}
     * and can reset indefinitely, it is guaranteed that the mark position can always
     * be restored by {@link #reset()} regardless of what value is set for {@code readLimit}.
     *
     * @param readLimit maximum byte limit that can be read before mark position becomes invalid.
     *            If data exceeding this byte count is read, {@link #reset()} may not be possible.
     * @see PreLzssDecoder#mark(int)
     */
    @Override
    public void mark(int readLimit) {
        readLimit -= (int) (this.textDecoded - this.textPosition);
        int Size = this.textBuffer.length - this.maxMatch;
        readLimit = (readLimit + Size - 1) / Size * Size;
        this.decoder.mark(Math.max(readLimit, 0));

        if (this.markTextBuffer == null) {
            this.markTextBuffer = this.textBuffer.clone();
        } else {
            System.arraycopy(this.textBuffer, 0, this.markTextBuffer, 0, this.textBuffer.length);
        }
        this.MarkTextPosition = this.textPosition;
        this.MarkTextDecoded = this.textDecoded;
    }

    /**
     * Sets the read position of the connected input stream to the position
     * when the {@link #mark(int)} method was last called.
     *
     * @exception IOException if an I/O error occurs
     */
    @Override
    public void reset() throws IOException {
        if (this.markTextBuffer == null) {
            throw new IOException("not marked.");
        } else if (this.textDecoded - this.MarkTextPosition <= this.textBuffer.length) {
            this.textPosition = this.MarkTextPosition;
        } else if (this.decoder.markSupported()) {
            // reset
            this.decoder.reset();
            System.arraycopy(this.markTextBuffer, 0, this.textBuffer, 0, this.textBuffer.length);
            this.textPosition = this.MarkTextPosition;
            this.textDecoded = this.MarkTextDecoded;
        } else {
            throw new IOException("mark/reset not supported.");
        }
    }

    /**
     * Gets whether the connected input stream supports {@link #mark(int)} and {@link #reset()}.
     *
     * @return {@code true} if the stream supports {@link #mark(int)} and {@link #reset()};
     *         {@code false} otherwise.
     */
    @Override
    public boolean markSupported() {
        return this.decoder.markSupported();
    }

    /**
     * Gets the number of bytes that can be read from the connected input stream without blocking.<br>
     *
     * @return number of bytes that can be read without blocking.<br>
     *
     * @exception IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        return (int) (this.textDecoded - this.textPosition) + this.decoder.available();
    }

    /**
     * Closes this input stream and releases all resources used.<br>
     *
     * @exception IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        this.decoder.close();
        this.decoder = null;
        this.textBuffer = null;
        this.markTextBuffer = null;
    }

    /**
     * Reads compressed data from private field {@code this.decoder},
     * decompresses it, and writes data into {@link #textBuffer}.
     *
     * @exception IOException if an I/O error occurs
     * @exception EOFException if end of stream is reached
     */
    private void decode() throws IOException {
        if (this.textDecoded < this.length) {
            int textMask = this.textBuffer.length - 1;
            int textStart = (int) this.textDecoded & textMask;
            int textPos = textStart;
            int textLimit = (int) (Math.min(this.textPosition + this.textBuffer.length - this.maxMatch, this.length)
                                   - this.textDecoded)
                            + textStart;
            try {
                while (textPos < textLimit) {
                    int code = this.decoder.readCode();

                    if (code < 0x100) {
                        this.textBuffer[textMask & textPos++] = (byte) code;
                    } else {
                        int matchLength = (code & 0xFF) + this.threshold;
                        int matchPosition = textPos - this.decoder.readOffset() - 1;

                        while (0 < matchLength--)
                            this.textBuffer[textMask & textPos++] = this.textBuffer[textMask & matchPosition++];
                    }
                }
            } finally {
                this.textDecoded += textPos - textStart;
            }
        } else {
            throw new EOFException();
        }
    }

    /**
     * Transfers data from private field {@code this.textBuffer} to {@code buffer}.
     *
     * @param buffer buffer to copy contents of {@link #textBuffer} into
     * @param position current write position in {@code buffer}
     * @param end write end position in {@code buffer}
     * @return position in {@code buffer} where the next write should occur
     */
    private int copyTextBufferToBuffer(byte[] buffer, int position, int end) {
        if ((this.textPosition & ~(this.textBuffer.length - 1)) < (this.textDecoded & ~(this.textBuffer.length - 1))) {
            int length = Math.min(this.textBuffer.length - ((int) this.textPosition & this.textBuffer.length - 1),
                                  end - position);

            System.arraycopy(this.textBuffer, (int) this.textPosition & this.textBuffer.length - 1, buffer, position, length);

            this.textPosition += length;
            position += length;
        }

        if (this.textPosition < this.textDecoded) {
            int length = Math.min((int) (this.textDecoded - this.textPosition), end - position);

            System.arraycopy(this.textBuffer, (int) this.textPosition & this.textBuffer.length - 1, buffer, position, length);

            this.textPosition += length;
            position += length;
        }

        return position;
    }

    /**
     * Initializes {@link #textBuffer} for -lz5-.
     */
    private void initLz5TextBuffer() {
        int position = 18;
        for (int i = 0; i < 256; i++)
            for (int j = 0; j < 13; j++)
                this.textBuffer[position++] = (byte) i;

        for (int i = 0; i < 256; i++)
            this.textBuffer[position++] = (byte) i;

        for (int i = 0; i < 256; i++)
            this.textBuffer[position++] = (byte) (255 - i);

        for (int i = 0; i < 128; i++)
            this.textBuffer[position++] = 0;

        while (position < this.textBuffer.length)
            this.textBuffer[position++] = (byte) ' ';
    }
}
