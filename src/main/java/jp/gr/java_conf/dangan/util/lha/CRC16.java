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

import java.util.zip.Checksum;


/**
 * Class for calculating CRC16 values.
 * <p>
 * Constants, processing, and descriptions in this class are based on:
 *
 * <pre>
 * C-gengo ni yoru Algorithm Jiten (Algorithm Dictionary in C)
 *   Haruhiko Okumura, Gijutsu-Hyohron Co., Ltd.
 *   ISBN4-87408-414-1 C3055 2400 yen (at time of purchase)
 * </pre>
 *
 * <pre>
 * -- revision history --
 * $Log: CRC16.java,v $
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     Source cleanup.
 *     Eliminated tabs.
 *     Updated license text.
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class CRC16 implements Checksum {

    /**
     * Bit representation of the polynomial {@code x^16 + x^15 + x^2 + 1},
     * well known as CRC-ANSI or CRC-16.
     */
    public static final int CRC_ANSY_POLY = 0xA001;

    /**
     * Initial value of CRC used in LHA.
     * This is a value set arbitrarily by the author, and it is unknown
     * whether this value is specified as the initial value in CRC-ANSI.
     */
    public static final int CRC_ANSY_INIT = 0x0000;

    /**
     * Bit representation of the polynomial {@code x^16 + x^12 + x^5 + 1}
     * in the CCITT X.25 standard.
     */
    public static final int CCITT_POLY = 0x8408;

    /**
     * Initial value of CRC in the CCITT X.25 standard.
     */
    public static final int CCITT_INIT = 0xFFFF;

    /**
     * Default CRC polynomial in the sense that it is normally used in LHA.
     * Equivalent to {@link #CRC_ANSY_POLY}.
     */
    public static final int DefaultPOLY = CRC16.CRC_ANSY_POLY;

    /**
     * Default CRC initial value in the sense that it is normally used in LHA.
     * Equivalent to {@link #CRC_ANSY_INIT}.
     */
    public static final int DefaultINIT = CRC16.CRC_ANSY_INIT;

    /**
     * CRC16 value
     */
    private int crc;

    /**
     * Initial value of CRC
     */
    private final int init;

    /**
     * Table for updating CRC16 value
     */
    private final int[] crcTable;

    /**
     * Creates a {@link CRC16} with polynomial and initial value used in LHA.
     */
    public CRC16() {
        this(DefaultPOLY, DefaultINIT);
    }

    /**
     * Creates a {@link CRC16} with the polynomial specified by {@code poly}.
     * The initial value is {@link #CCITT_INIT} if {@code poly} is {@link #CCITT_POLY},
     * or {@link #DefaultINIT} otherwise.
     *
     * @param poly bit representation of the polynomial used for CRC16 calculation
     */
    public CRC16(int poly) {
        this(poly, (poly == CRC16.CCITT_POLY ? CRC16.CCITT_INIT : CRC16.DefaultINIT));
    }

    /**
     * Creates a {@link CRC16} with the polynomial specified by {@code poly}
     * and the initial value specified by {@code init}.
     *
     * @param poly bit representation of the polynomial used for CRC16 calculation
     * @param init initial value of CRC
     */
    public CRC16(int poly, int init) {
        this(CRC16.makeCrcTable(poly), init);
    }

    /**
     * Creates a {@link CRC16} with the CRC calculation table specified by {@code crcTable}
     * and the initial value specified by {@code init}.
     *
     * @param crcTable table used for CRC16 calculation
     * @param init initial value of CRC
     */
    public CRC16(int[] crcTable, int init) {
        final int BYTE_PATTERNS = 256;

        if (crcTable.length == BYTE_PATTERNS) {
            this.crcTable = crcTable;
            this.init = init;

            this.reset();
        } else {
            throw new IllegalArgumentException("crcTable.length must equals 256");
        }
    }

    /**
     * Updates the CRC value with 1-byte data specified by {@code byte8}.
     *
     * @param byte8 1-byte data to update CRC
     */
    @Override
    public void update(int byte8) {
        final int BYTE_BITS = 8;
        this.crc = (this.crc >> BYTE_BITS) ^ this.crcTable[(this.crc ^ byte8) & 0xFF];
    }

    /**
     * Updates the CRC value with the byte array specified by {@code buffer}.
     *
     * @param buffer byte array containing data to update CRC
     */
    @Override
    public void update(byte[] buffer) {
        this.update(buffer, 0, buffer.length);
    }

    /**
     * Updates the CRC value with the byte array specified by {@code buffer}.
     *
     * @param buffer byte array containing data to update CRC
     * @param index starting position of data
     * @param length number of bytes used to update checksum
     */
    @Override
    public void update(byte[] buffer, int index, int length) {
        final int BYTE_BITS = 8;

        while (0 < (index & 0x03) && 0 < length--) {
            this.crc = (this.crc >> BYTE_BITS) ^ this.crcTable[(this.crc ^ buffer[index++]) & 0xFF];
        }

        while (4 <= length) {
            int data = (buffer[index++] & 0xFF) | ((buffer[index++] & 0xFF) << 8) | ((buffer[index++] & 0xFF) << 16)
                       | (buffer[index++] << 24);

            this.crc = (this.crc >> BYTE_BITS) ^ this.crcTable[(this.crc ^ data) & 0xFF];
            data >>>= BYTE_BITS;
            this.crc = (this.crc >> BYTE_BITS) ^ this.crcTable[(this.crc ^ data) & 0xFF];
            data >>>= BYTE_BITS;
            this.crc = (this.crc >> BYTE_BITS) ^ this.crcTable[(this.crc ^ data) & 0xFF];
            data >>>= BYTE_BITS;
            this.crc = (this.crc >> BYTE_BITS) ^ this.crcTable[(this.crc ^ data) & 0xFF];
            length -= 4;
        }

        while (0 < length--) {
            this.crc = (this.crc >> BYTE_BITS) ^ this.crcTable[(this.crc ^ buffer[index++]) & 0xFF];
        }
    }

    /**
     * Resets the CRC value to the initial value.
     */
    @Override
    public void reset() {
        this.crc = this.init;
    }

    /**
     * Gets the CRC value.
     * The CRC value is a 2-byte value, mapped to 0x0000 to 0xFFFF.
     *
     * @return CRC value
     */
    @Override
    public long getValue() {
        return this.crc & 0xFFFF;
    }

    /**
     * Creates a table for CRC value calculation.
     *
     * @param poly polynomial for CRC calculation
     * @return CRC calculation table
     */
    public static int[] makeCrcTable(int poly) {
        final int BYTE_PATTERNS = 256;
        final int BYTE_BITS = 8;
        int[] crcTable = new int[BYTE_PATTERNS];

        for (int i = 0; i < BYTE_PATTERNS; i++) {
            crcTable[i] = i;

            for (int j = 0; j < BYTE_BITS; j++) {
                if ((crcTable[i] & 1) != 0) {
                    crcTable[i] = (crcTable[i] >> 1) ^ poly;
                } else {
                    crcTable[i] >>= 1;
                }
            }
        }

        return crcTable;
    }
}
