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
import java.io.OutputStream;


/**
 * Utility class for disconnecting between an output stream that processes and outputs data
 * and a stream that outputs data to a device.
 *
 * <pre>
 * -- revision history --
 * $Log: DisconnectableOutputStream.java,v $
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     tab removal
 *     license statement fix
 *     source maintenance
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class DisconnectableOutputStream extends OutputStream implements Disconnectable {

    /**
     * Connected output stream
     */
    private OutputStream out;

    /**
     * Constructs an output stream that can be disconnected from {@code out}.
     *
     * @param out output stream
     */
    public DisconnectableOutputStream(OutputStream out) {
        if (out != null) {
            this.out = out;
        } else {
            throw new NullPointerException("out");
        }
    }

    /**
     * Outputs 1 byte of data to the connected output stream.
     *
     * @param data 1 byte of data to be written. Generally, the upper 3 bytes are ignored.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(int data) throws IOException {
        this.out.write(data);
    }

    /**
     * Outputs all data in {@code buffer} to the connected output stream.
     *
     * @param buffer byte array containing data to be written.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] buffer) throws IOException {
        this.out.write(buffer, 0, buffer.length);
    }

    /**
     * Outputs {@code length} bytes of data from {@code buffer} starting from the position
     * specified by {@code index} to the connected output stream.
     *
     * @param buffer byte array containing data to be written.
     * @param index  starting position of data to write in {@code buffer}.
     * @param length amount of data to write.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] buffer, int index, int length) throws IOException {
        this.out.write(buffer, index, length);
    }

    /**
     * Instructs the connected output stream to output all stored data.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    /**
     * Disconnects from the connected output stream.<br>
     * This method only calls {@link #disconnect()}.
     */
    @Override
    public void close() {
        this.disconnect();
    }

    /**
     * Disconnects from the connected output stream.
     */
    @Override
    public void disconnect() {
        this.out = null;
    }
}
