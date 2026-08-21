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
 * Hash function used in prototype program ar940528 and LHa for Unix.<br>
 * It appears to have been referenced from the one used in gzip.
 *
 * <pre>
 * -- revision history --
 * $Log: HashDefault.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     Interface change to match {@link HashMethod} interface change.
 *     Removed argument check in constructor.
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     license comment update
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class HashDefault implements HashMethod {

    /**
     * Mask value for generating hash value.
     */
    private static final int HashMask = 0x7FFF;

    /**
     * Buffer for applying LZSS compression.
     * The first half is the dictionary area,
     * and the second half is the buffer containing data to be compressed.
     * Used only for reading to generate hash values in implementations of {@link HashMethod}.
     */
    private final byte[] textBuffer;

    /**
     * Constructs a hash function used in ar940528 and LHa for Unix.
     *
     * @param TextBuffer buffer for LZSS compression.
     *            Used for reading to generate hash values.
     */
    public HashDefault(byte[] TextBuffer) {
        this.textBuffer = TextBuffer;
    }

    /**
     * Hash function.<br>
     * Generates the hash value of the data pattern from {@code position} of {@link #textBuffer} passed to constructor.
     *
     * @param position start position of the data pattern
     * @return hash value
     */
    @Override
    public int hash(int position) {
        return (((this.textBuffer[position] << 5) ^ (this.textBuffer[position + 1] & 0xFF)) << 5
                ^ (this.textBuffer[position + 2] & 0xFF))
               & HashDefault.HashMask;
    }

    /**
     * Gets the number of bytes used by the hash function to generate a hash value.<br>
     * Since this hash function generates a hash value from 3 bytes of data using shift and XOR,
     * this method always returns 3.
     *
     * @return always 3
     */
    @Override
    public int hashRequires() {
        return 3;
    }

    /**
     * Gets the size of the hash table.<br>
     * Since this hash function generates hash values in the range 0x0000 to 0x7FFF,
     * this method always returns 0x8000 (32768).
     *
     * @return always 0x8000 (32768)
     */
    @Override
    public int tableSize() {
        return 0x8000;
    }
}
