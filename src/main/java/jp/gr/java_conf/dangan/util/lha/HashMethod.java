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
 * Interface that provides hash functions.
 * <br>
 * The constructor form must conform to a format like:
 *
 * <pre>
 * HashMethod( byte[] TextBuffer )
 *
 * <strong>Parameters:</strong>
 *   TextBuffer     - Buffer containing data to be LZSS compressed
 * </pre>
 *
 * Furthermore, if additional arguments are needed, use a format like:
 *
 * <pre>
 * HashMethod( byte[] TextBuffer,
 *             Object ExtraData1,
 *             Object ExtraData2 )
 * </pre>
 *
 * Note that argument checking in the constructor only needs to be performed when there are additional arguments.
 *
 * <pre>
 * -- revision history --
 * $Log: HashMethod.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version cotrol
 * [change]
 *     Renamed requiredSize() to hashRequires().
 *     Renamed size() to tableSize().
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     license comment update
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public interface HashMethod {

    /**
     * Hash function.
     * Generates the hash value of the data pattern starting from {@code position}
     * in the {@code TextBuffer} passed to the constructor.
     *
     * @param position start position of the data pattern
     * @return hash value
     */
    int hash(int position);

    /**
     * Gets the number of bytes used by the hash function to generate a hash value.
     *
     * @return number of bytes used by the hash function to generate a hash value
     */
    int hashRequires();

    /**
     * Gets the size of the {@code HashTable} when using this {@link HashMethod}.
     *
     * @return size of the {@code HashTable} when using this {@link HashMethod}
     */
    int tableSize();
}
