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

import java.io.IOException;


/**
 * An exception thrown when data for the requested number of bits could not be obtained.<br>
 * Unlike {@link BitDataBrokenException}, note that when this exception is thrown,
 * the read operation has not actually been performed, so the read position
 * is the same as before throwing the exception.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: NotEnoughBitsException.java,v $
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     tab removal
 *     license statement fix
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class NotEnoughBitsException extends IOException {

    /**
     * Number of bits that can actually be read
     */
    private final int availableBits;

    /**
     * Constructs a {@link NotEnoughBitsException} indicating that {@code availableBits} are available.
     *
     * @param availableBits number of available bits
     */
    public NotEnoughBitsException(int availableBits) {
        super();
        this.availableBits = availableBits;
    }

    /**
     * Constructs a {@link NotEnoughBitsException} indicating that {@code availableBits} are available,
     * with a detailed message.
     *
     * @param message       detailed message
     * @param availableBits number of available bits
     */
    public NotEnoughBitsException(String message, int availableBits) {
        super(message);
        this.availableBits = availableBits;
    }

    /**
     * Gets the number of available bits.<br>
     * Returns the number of currently available bits in the method that threw this exception.
     *
     * @return number of available bits
     */
    public int getAvailableBits() {
        return this.availableBits;
    }
}
