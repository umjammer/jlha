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
 * Class for calculating simple 1-byte checksum values used in LHA.
 *
 * <pre>
 * -- revision history --
 * $Log: LhaChecksum.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     Source cleanup.
 *     Eliminated tabs.
 *     Updated license text.
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class LhaChecksum implements Checksum {

    /**
     * Checksum value
     */
    private int checksum;

    /**
     * Constructs a new checksum object.
     */
    public LhaChecksum() {
        super();
        this.reset();
    }

    /**
     * Updates the checksum value with the 1-byte data specified by {@code byte8}.
     *
     * @param byte8 1-byte data to update checksum
     */
    @Override
    public void update(int byte8) {
        this.checksum += byte8;
    }

    /**
     * Updates the checksum value with the byte array specified by {@code buffer}.
     * This method is equivalent to {@code update(buffer, 0, buffer.length)}.
     *
     * @param buffer byte array containing data to update checksum
     */
    @Override
    public void update(byte[] buffer) {
        this.update(buffer, 0, buffer.length);
    }

    /**
     * Updates the checksum value with the byte array specified by {@code buffer}.
     *
     * @param buffer byte array containing data to update checksum
     * @param index starting position of data
     * @param length number of bytes used to update checksum
     */
    @Override
    public void update(byte[] buffer, int index, int length) {
        while (0 < length--)
            this.checksum += buffer[index++];
    }

    /**
     * Resets the checksum value to the initial value.
     */
    @Override
    public void reset() {
        this.checksum = 0;
    }

    /**
     * Gets the checksum value.
     * The checksum value is a 1-byte value, mapped to 0x00 to 0xFF.
     *
     * @return checksum value
     */
    @Override
    public long getValue() {
        return this.checksum & 0xFF;
    }
}
