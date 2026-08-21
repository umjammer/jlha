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

package jp.gr.java_conf.dangan.util.lha;


/**
 * Hash function that generates a hash value from 0 to 4095
 * from the first 2 bytes of a data pattern.
 *
 * <pre>
 * -- revision history --
 * $Log: HashShort.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     Interface change to match {@link HashMethod} interface change.
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     license comment update
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class HashShort implements HashMethod {

    /**
     * Buffer for applying LZSS compression.
     * The first half is the dictionary area,
     * and the second half is the buffer containing data to be compressed.
     * Used only for reading to generate hash values in implementations of {@link HashMethod}.
     */
    private final byte[] TextBuffer;

    /**
     * Constructs a hash function that generates a value from 0x000 to 0xFFF
     * from the first 2 bytes of a data pattern.
     *
     * @param TextBuffer buffer for LZSS compression. Used for reading to generate hash values.
     */
    public HashShort(byte[] TextBuffer) {
        this.TextBuffer = TextBuffer;
    }

    /**
     * Hash function.
     * Generates the hash value of the data pattern starting from {@code position}
     * of {@link #TextBuffer} passed to constructor.
     *
     * @param position start position of the data pattern
     * @return hash value
     */
    @Override
    public int hash(int position) {
        return ((((this.TextBuffer[position + 1] & 0x0F) << 8) | ((this.TextBuffer[position + 1] & 0xFF) >> 4))
                ^ ((this.TextBuffer[position] & 0xFF) << 2));
    }

    /**
     * Gets the number of bytes used by the hash function to generate a hash value.<br>
     * Since this hash function generates a hash value from the first 2 bytes of data
     * in the data pattern, this method always returns 2.
     *
     * @return always 2
     */
    @Override
    public int hashRequires() {
        return 2;
    }

    /**
     * Gets the size of the hash table.<br>
     * Since this hash function generates hash values in the range 0x000 to 0xFFF,
     * this method always returns 0x1000 (4096).
     *
     * @return always 0x1000 (4096)
     */
    @Override
    public int tableSize() {
        return 0x1000;
    }
}
