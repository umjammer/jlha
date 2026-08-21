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
import java.io.OutputStream;


/**
 * Utility class providing methods to access byte arrays, {@link InputStream},
 * and {@link OutputStream} in little-endian.
 *
 * <pre>
 * -- revision history --
 * $Log: LittleEndian.java,v $
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [change]
 *     removed writeByte(), readByte().
 * [maintenance]
 *     source maintenance
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class LittleEndian {

    /**
     * Default constructor. Cannot be used.
     */
    private LittleEndian() {
    }

    /**
     * Reads a 2-byte value in little-endian starting from the {@code index} position of {@code ByteArray}.
     * The read 2-byte value is mapped to 0x0000-0xFFFF.
     *
     * @param ByteArray byte array
     * @param index     starting position of data in {@code ByteArray}
     * @return the read 2-byte value
     * @throws ArrayIndexOutOfBoundsException if data starting from {@code index} is not within the range of {@code ByteArray}.
     */
    public static int readShort(byte[] ByteArray, int index) {
        return (ByteArray[index] & 0xFF) | ((ByteArray[index + 1] & 0xFF) << 8);
    }

    /**
     * Reads a 4-byte value in little-endian starting from the {@code index} position of {@code ByteArray}.
     *
     * @param ByteArray byte array
     * @param index     starting position of data in {@code ByteArray}
     * @return the read 4-byte value
     * @throws ArrayIndexOutOfBoundsException if data starting from {@code index} is not within the range of {@code ByteArray}.
     */
    public static int readInt(byte[] ByteArray, int index) {
        return (ByteArray[index] & 0xFF) | ((ByteArray[index + 1] & 0xFF) << 8) | ((ByteArray[index + 2] & 0xFF) << 16)
                | (ByteArray[index + 3] << 24);
    }

    /**
     * Reads an 8-byte value in little-endian starting from the {@code index} position of {@code ByteArray}.
     *
     * @param ByteArray byte array
     * @param index     starting position of data in {@code ByteArray}
     * @return the read 8-byte value
     * @throws ArrayIndexOutOfBoundsException if data starting from {@code index} is not within the range of {@code ByteArray}.
     */
    public static long readLong(byte[] ByteArray, int index) {
        return (LittleEndian.readInt(ByteArray, index) & 0xFFFFFFFFL)
                | ((long) LittleEndian.readInt(ByteArray, index + 4) << 32L);
    }

    /**
     * Reads a 2-byte value in little-endian from the input stream {@code in}.
     *
     * @param in input stream
     * @return the read 2-byte value
     * @throws EOFException if End Of Stream has already been reached or End Of Stream was reached while reading. Partial data is lost.
     * @throws IOException  if an I/O error occurs
     */
    public static int readShort(InputStream in) throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();

        if (0 <= byte1 && 0 <= byte2) {
            return (byte1 & 0xFF) | ((byte2 & 0xFF) << 8);
        } else {
            throw new EOFException();
        }
    }

    /**
     * Reads a 4-byte value in little-endian from the input stream {@code in}.
     *
     * @param in input stream
     * @return the read 4-byte value
     * @throws EOFException if End Of Stream has already been reached or End Of Stream was reached while reading. Partial data is lost.
     * @throws IOException  if an I/O error occurs
     */
    public static int readInt(InputStream in) throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();
        int byte3 = in.read();
        int byte4 = in.read();

        if (0 <= byte1 && 0 <= byte2 && 0 <= byte3 && 0 <= byte4) {
            return (byte1 & 0xFF) | ((byte2 & 0xFF) << 8) | ((byte3 & 0xFF) << 16) | (byte4 << 24);
        } else {
            throw new EOFException();
        }
    }

    /**
     * Reads an 8-byte value in little-endian from the input stream {@code in}.
     *
     * @param in input stream
     * @return the read 8-byte value
     * @throws EOFException if End Of Stream has already been reached or End Of Stream was reached while reading. Partial data is lost.
     * @throws IOException  if an I/O error occurs
     */
    public static long readLong(InputStream in) throws IOException {

        return (LittleEndian.readInt(in) & 0xFFFFFFFFL) | ((long) LittleEndian.readInt(in) << 32);

    }

    /**
     * Writes a 2-byte value in little-endian at the {@code index} position of {@code ByteArray}.
     *
     * @param ByteArray byte array
     * @param index     position to write data in {@code ByteArray}
     * @param value     2-byte value to write
     * @throws ArrayIndexOutOfBoundsException if data starting from {@code index} is not within the range of {@code ByteArray}.
     */
    public static void writeShort(byte[] ByteArray, int index, int value) {

        if (0 <= index && index + 1 < ByteArray.length) {
            ByteArray[index] = (byte) value;
            ByteArray[index + 1] = (byte) (value >> 8);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Writes a 4-byte value in little-endian at the {@code index} position of {@code ByteArray}.
     *
     * @param ByteArray byte array
     * @param index     position to write data in {@code ByteArray}
     * @param value     4-byte value to write
     * @throws ArrayIndexOutOfBoundsException if data starting from {@code index} is not within the range of {@code ByteArray}.
     */
    public static void writeInt(byte[] ByteArray, int index, int value) {

        if (0 <= index && index + 3 < ByteArray.length) {
            ByteArray[index] = (byte) value;
            ByteArray[index + 1] = (byte) (value >> 8);
            ByteArray[index + 2] = (byte) (value >> 16);
            ByteArray[index + 3] = (byte) (value >> 24);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Writes an 8-byte value in little-endian at the {@code index} position of {@code ByteArray}.
     *
     * @param ByteArray byte array
     * @param index     position to write data in {@code ByteArray}
     * @param value     8-byte value to write
     * @throws ArrayIndexOutOfBoundsException if data starting from {@code index} is not within the range of {@code ByteArray}.
     */
    public static void writeLong(byte[] ByteArray, int index, long value) {
        if (0 <= index && index + 7 < ByteArray.length) {
            LittleEndian.writeInt(ByteArray, index, (int) value);
            LittleEndian.writeInt(ByteArray, index + 4, (int) (value >> 32));
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Writes 2 bytes in little-endian to the output stream {@code out}.
     *
     * @param out   output stream
     * @param value 2-byte value to write
     * @throws IOException if an I/O error occurs
     */
    public static void writeShort(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    /**
     * Writes a 4-byte value in little-endian to the output stream {@code out}.
     *
     * @param out   output stream
     * @param value 4-byte value to write
     * @throws IOException if an I/O error occurs
     */
    public static void writeInt(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write(value >>> 24);
    }

    /**
     * Writes an 8-byte value in little-endian to the output stream {@code out}.
     *
     * @param out   output stream
     * @param value 8-byte value to write
     * @throws IOException if an I/O error occurs
     */
    public static void writeLong(OutputStream out, long value) throws IOException {
        int low = (int) value;
        int hi = (int) (value >> 32);

        out.write(low & 0xFF);
        out.write((low >> 8) & 0xFF);
        out.write((low >> 16) & 0xFF);
        out.write(low >>> 24);
        out.write(hi & 0xFF);
        out.write((hi >> 8) & 0xFF);
        out.write((hi >> 16) & 0xFF);
        out.write(hi >>> 24);
    }
}
