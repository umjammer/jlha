/*
 * BitDataBrokenException.java
 *
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


/**
 * An exception thrown when data for the requested number of bits
 * could not be obtained because the EndOfStream was reached.<br>
 * Note that since this is for {@link BitInputStream},
 * the data that can be held is up to 32 bits.<br>
 * Unlike {@link NotEnoughBitsException}, note that when this exception
 * is thrown, the read operation has actually been performed, so
 * the read position has changed from the point before throwing the
 * exception.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: BitDataBrokenException.java,v $
 * Revision 1.1  2002/12/07 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     tab removal
 *     license statement fix
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class BitDataBrokenException extends IOException {

    /**
     * The exception that caused the bit data to be
     * obtained only partially
     */
    private final Throwable cause;

    /**
     * Partial bit data
     */
    private final int bitData;

    /**
     * Number of valid bits in {@code bitData}
     */
    private final int bitCount;

    /**
     * Constructs a new {@link BitDataBrokenException}.
     *
     * @param cause the exception that caused the bit data to be obtained only partially
     * @param bitData the bit data that did not reach the requested number of bits
     * @param bitCount the number of bits in {@code bitData}
     */
    public BitDataBrokenException(Throwable cause, int bitData, int bitCount) {
        this.cause = cause;
        this.bitData = bitData;
        this.bitCount = bitCount;
    }

    /**
     * Gets the exception that caused the bit data to be
     * obtained only partially.
     *
     * @return the causing exception
     */
    @Override
    public Throwable getCause() {
        return this.cause;
    }

    /**
     * Gets the "broken" bit data that did not reach
     * the requested number of bits.
     *
     * @return the bit data
     */
    public int getBitData() {
        return this.bitData;
    }

    /**
     * Gets the number of valid bits in the bit data
     * obtained by {@link #getBitData()}.
     *
     * @return the number of valid bits in the bit data
     */
    public int getBitCount() {
        return this.bitCount;
    }
}
