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

import java.io.IOException;


/**
 * Interface for processing LZSS compression codes.
 *
 * <pre>
 * -- revision history --
 * $Log: PostLzssEncoder.java,v $
 * Revision 1.0  2002/07/25 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     fix license statement
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public interface PostLzssEncoder {

    /**
     * Flushes outputtable data buffered in this {@link PostLzssEncoder}
     * to the output destination {@link java.io.OutputStream},
     * and calls {@code flush()} on the destination {@link java.io.OutputStream}.<br>
     * Similar to {@link java.io.OutputStream#flush()}, but it is not
     * guaranteed that output when {@code flush()} is not called and when {@code flush()} is called will be the same.<br>
     * That is, the following two codes, which would be expected to output the same data in {@link java.io.OutputStream#flush()},
     * may output different data in {@link PostLzssEncoder}.
     *
     * <pre>
     * (1)
     *   PostLzssEncoder out = new ImplementedPostLzssEncoder();
     *   out.writeCode( 0 );
     *   out.writeCode( 0 );
     *   out.writeCode( 0 );
     *   out.close();
     *
     * (2)
     *   PostLzssEncoder out = new ImplementedPostLzssEncoder();
     *   out.writeCode( 0 );
     *   out.flush();
     *   out.writeCode( 0 );
     *   out.flush();
     *   out.writeCode( 0 );
     *   out.close();
     * </pre>
     *
     * @throws IOException if an I/O error occurs
     */
    void flush() throws IOException;

    /**
     * Closes this output stream and the connected output stream,
     * and releases any resources used.<br>
     *
     * @throws IOException if an I/O error occurs
     */
    void close() throws IOException;

    /**
     * Writes 1 byte of uncompressed LZSS data or
     * the match length of LZSS compressed code.<br>
     * Use 0 to 255 for uncompressed data,
     * and 256 to 510 for LZSS compressed codes (match length).
     *
     * @param code 1 byte of uncompressed LZSS data or the match length of LZSS compressed code
     * @throws IOException if an I/O error occurs
     */
    void writeCode(int code) throws IOException;

    /**
     * Writes the match offset of LZSS compressed code.<br>
     *
     * @param offset the match offset of LZSS compressed code
     * @throws IOException if an I/O error occurs
     */
    void writeOffset(int offset) throws IOException;

    /**
     * Gets the size of the LZSS dictionary processed by this {@link PostLzssEncoder}.
     *
     * @return the size of the LZSS dictionary
     */
    int getDictionarySize();

    /**
     * Gets the maximum match length processed by this {@link PostLzssEncoder}.
     *
     * @return the maximum match length
     */
    int getMaxMatch();

    /**
     * Gets the compression/non-compression threshold processed by this {@link PostLzssEncoder}.
     *
     * @return the compression threshold
     */
    int getThreshold();
}
