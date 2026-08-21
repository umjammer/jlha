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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import jp.gr.java_conf.dangan.io.DisconnectableInputStream;
import jp.gr.java_conf.dangan.io.LimitedInputStream;


/**
 * Utility class for reading LHA archive data from a connected stream
 * and reading entries while extracting them.<br>
 * Designed to have an interface similar to {@link java.util.zip.ZipInputStream}.<br>
 * For corrupted archives, there is a possibility that uncorrupted entries following a corrupted entry
 * cannot be read normally.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: LhaInputStream.java,v $
 * Revision 1.1.2.1  2003/07/20 13:22:31  dangan
 * [bug fix]
 *     In getNextEntry(), passed this.in to CompressMethod.connectDecoder
 *     where this.limit should have been passed.
 *
 * Revision 1.1  2002/12/08 00:00:00  dangan
 * [maintenance]
 *     Updated to reflect the class name change from LhaConstants to CompressMethod.
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     Deprecated constructors taking String encode and added ones taking Properties.
 *     Prevented reading past archive termination.
 *     Changed available() behavior like java.util.zip.ZipInputStream to return 1
 *     if not reached entry EOF, and 0 if entry EOF is reached.
 * [maintenance]
 *     Source cleanup.
 *     Eliminated tabs.
 *     Updated license text.
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1.2.1 $
 */
public class LhaInputStream extends InputStream {

    /**
     * {@link InputStream} providing data in LHA archive format.
     */
    private InputStream source;

    /**
     * Indicates whether the first entry has already been read.
     */
    private boolean alreadyOpenedFirstEnrty;

    /**
     * Indicates whether end of archive has been reached.
     */
    private boolean reachedEndOfArchive;

    /**
     * {@link InputStream} providing decompressed data of one entry in LHA archive.
     */
    private InputStream in;

    /**
     * {@link LimitedInputStream} providing compressed data of one entry in LHA archive.
     * Used to skip when {@link #closeEntry()} is called.
     */
    private LimitedInputStream limit;

    /**
     * Set to {@code true} when reaching the end of the entry currently being processed.
     */
    private boolean reachedEndOfEntry;

    /** Backup for reachEndOfEntry */
    private boolean markReachedEndOfEntry;

    /**
     * Properties containing decoder generator expressions corresponding to each compression format
     */
    private Properties property;

    /**
     * Constructs an {@link InputStream} that reads LHA archive data from {@code in}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of decoders corresponding to each compression format.
     *
     * @param in input stream providing data in LHA archive format
     * @see LhaProperty#getProperties()
     */
    public LhaInputStream(InputStream in) {
        Properties property = LhaProperty.getProperties();

        try {
            this.constructerHelper(in, property);
        } catch (UnsupportedEncodingException exception) {
            throw new Error("Unsupported encoding \"" + property.getProperty("lha.encoding") + "\".");
        }
    }

    /**
     * Constructs an {@link InputStream} that reads LHA archive data from {@code in}.
     *
     * @param in       input stream providing data in LHA archive format
     * @param property properties containing generator expressions of decoders corresponding to each compression format
     * @throws UnsupportedEncodingException if the encoding name obtained by {@code property.getProperty("lha.encoding")} is not supported
     */
    public LhaInputStream(InputStream in, Properties property) throws UnsupportedEncodingException {

        this.constructerHelper(in, property);
    }

    /**
     * Helper method responsible for constructor initialization.
     *
     * @param in       input stream providing data in LHA archive format
     * @param property properties containing generator expressions of decoders corresponding to each compression format
     * @throws UnsupportedEncodingException if encode is not supported
     */
    private void constructerHelper(InputStream in, Properties property) throws UnsupportedEncodingException {

        if (in != null && property != null) {
            String encoding = property.getProperty("lha.encoding");
            if (encoding == null) {
                encoding = LhaProperty.getProperty("lha.encoding");
            }

            // Check encoding name
            encoding.getBytes(encoding);

            if (in.markSupported()) {
                this.source = in;
            } else {
                this.source = new BufferedInputStream(in);
            }

            this.in = null;
            this.limit = null;
            this.property = (Properties) property.clone();
            this.reachedEndOfEntry = false;
            this.reachedEndOfArchive = false;

        } else if (in == null) {
            throw new NullPointerException("in");
        } else {
            throw new NullPointerException("property");
        }
    }

    /**
     * Reads 1 byte of data from the current entry.
     *
     * @return 1 byte of read data, or -1 if end of entry has already been reached
     * @throws IOException if there is no entry currently being read or an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        if (this.in != null) {
            int ret = this.in.read();
            if (ret < 0) {
                this.reachedEndOfEntry = true;
            }
            return ret;
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * Reads data from the current entry to fill {@code buffer}.
     *
     * @param buffer buffer to read data into
     * @return amount of read data, or -1 if end of entry has already been reached
     * @throws IOException if there is no entry currently being read or an I/O error occurs
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    /**
     * Reads {@code length} bytes of data from the current entry into {@code buffer} starting at {@code index}.
     *
     * @param buffer buffer to read data into
     * @param index  start position of data reading in {@code buffer}
     * @param length amount of data to read
     * @return amount of read data, or -1 if end of entry has already been reached
     * @throws IOException if there is no entry currently being read or an I/O error occurs
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        if (this.in != null) {
            int ret = this.in.read(buffer, index, length);
            if (ret < 0) {
                this.reachedEndOfEntry = true;
            }
            return ret;
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * Skips {@code length} bytes of data in the current entry.
     *
     * @param length amount of data to skip
     * @return actual amount of data skipped
     * @throws IOException if there is no entry currently being read or an I/O error occurs
     */
    @Override
    public long skip(long length) throws IOException {
        if (this.in != null) {
            if (0 < length) {
                long len = this.in.skip(length - 1);
                int ret = this.in.read();
                if (ret < 0) {
                    this.reachedEndOfEntry = true;
                    return len;
                } else {
                    return len + 1;
                }
            } else {
                return 0;
            }
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * Sets a mark at the current position in the entry currently being read,
     * so that {@link #reset()} can return to the marked read position.
     *
     * @param readLimit limit of read amount before mark position becomes invalid. No guarantee that {@link #reset()} works if reading beyond this number of bytes.
     * @throws IllegalStateException if there is no entry currently being read
     */
    @Override
    public void mark(int readLimit) {
        if (this.in != null) {
            this.in.mark(readLimit);
            this.markReachedEndOfEntry = this.reachedEndOfEntry;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Sets the read position of the entry currently being read to the position where the
     * {@link #mark(int)} method was last called.
     *
     * @throws IOException if there is no entry currently being read or an I/O error occurs
     */
    @Override
    public void reset() throws IOException {
        if (this.in != null) {
            this.in.reset();
            this.reachedEndOfEntry = this.markReachedEndOfEntry;
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * Gets whether the connected input stream supports {@link #mark(int)} and {@link #reset()}.<br>
     * Since mark/reset is required when reading headers, if {@code in} passed in constructor returns
     * {@code false} for {@link InputStream#markSupported()}, this class wraps {@code in} with a
     * {@link BufferedInputStream} supporting mark/reset.
     * Therefore, this method always returns {@code true}.
     *
     * @return always {@code true}
     */
    @Override
    public boolean markSupported() {
        return this.source.markSupported();
    }

    /**
     * Gets whether the end of the entry currently being read has been reached.<br>
     * Note that it does not return the amount of data that can be read without blocking.
     *
     * @return 0 if end of entry currently being read is reached, 1 if not reached
     * @throws IOException if there is no entry currently being read or an I/O error occurs
     * @see java.util.zip.ZipInputStream#available()
     */
    @Override
    public int available() throws IOException {
        if (this.in != null) {
            return (this.reachedEndOfEntry ? 0 : 1);
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * Closes this input stream and releases all resources used.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (this.in != null) {
            this.in.close();
            this.limit = null;
            this.in = null;
        }

        this.source.close();
        this.source = null;
    }

    /**
     * Sets up the stream to read the next entry while decompressing it.
     *
     * @return {@link LhaHeader} having entry information, or {@code null} when next entry doesn't exist
     * @throws IOException if an I/O error occurs
     */
    public LhaHeader getNextEntry() throws IOException {
        if (!this.reachedEndOfArchive) {
            if (this.in != null) {
                this.closeEntry();
            }

            byte[] HeaderData;
            if (this.alreadyOpenedFirstEnrty) {
                HeaderData = LhaHeader.getNextHeaderData(this.source);
            } else {
                HeaderData = LhaHeader.getFirstHeaderData(this.source);
                this.alreadyOpenedFirstEnrty = true;
            }
            if (null != HeaderData) {
                LhaHeader header = LhaHeader.createInstance(HeaderData, this.property);
                this.in = new DisconnectableInputStream(this.source);
                this.limit = new LimitedInputStream(this.in, header.getCompressedSize());
                this.in = CompressMethod
                        .connectDecoder(this.limit, header.getCompressMethod(), this.property, header.getOriginalSize());

                this.reachedEndOfEntry = false;
                this.markReachedEndOfEntry = false;
                return header;
            } else {
                this.reachedEndOfArchive = true;
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Sets up the stream to read the next entry without extracting it.
     *
     * @return {@link LhaHeader} having entry information, or {@code null} when next entry doesn't exist
     * @throws IOException if an I/O error occurs
     */
    public LhaHeader getNextEntryWithoutExtract() throws IOException {

        if (!this.reachedEndOfArchive) {

            if (this.in != null) {
                this.closeEntry();
            }

            byte[] HeaderData;
            if (this.alreadyOpenedFirstEnrty) {
                HeaderData = LhaHeader.getNextHeaderData(this.source);
            } else {
                HeaderData = LhaHeader.getFirstHeaderData(this.source);
                this.alreadyOpenedFirstEnrty = true;
            }
            if (HeaderData != null) {

                LhaHeader header = LhaHeader.createInstance(HeaderData, this.property);
                this.in = new DisconnectableInputStream(this.source);
                this.limit = new LimitedInputStream(this.in, header.getCompressedSize());
                this.in = this.limit;

                this.reachedEndOfEntry = false;
                this.markReachedEndOfEntry = false;
                return header;
            } else {
                this.reachedEndOfArchive = true;
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Closes the entry currently being read and sets up the stream so that the next entry can be read.
     *
     * @throws IOException if an I/O error occurs
     */
    public void closeEntry() throws IOException {
        if (this.in != null) {
            while (0 <= this.limit.read()) {
                this.limit.skip(Long.MAX_VALUE);
            }

            this.in.close();
            this.in = null;
            this.limit = null;
        }
    }
}
