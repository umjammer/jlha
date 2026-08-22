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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Properties;

import jp.gr.java_conf.dangan.io.GrowthByteBuffer;


/**
 * Utility class for outputting compressed data to a connected stream.<br>
 * Designed to have an interface similar to {@link java.util.zip.ZipOutputStream}.
 * Unlike Zip, LHA output is inherently 2-pass, so temporary storage holding data
 * of an entire entry is required until one entry is compressed.
 * If you do not wish to use such storage, use {@link LhaRetainedOutputStream} or
 * {@link LhaImmediateOutputStream}.
 *
 * <pre>
 * -- revision history --
 * $Log: LhaOutputStream.java,v $
 * Revision 1.1.2.2  2005/05/03 07:48:40  dangan
 * [bug fix]
 *     When compression method identifier -lhd- was specified, since compressed size does not drop below original size,
 *     it was always reset to -lh0-. As a result, directory information could not be stored.
 *
 * Revision 1.1.2.1  2005/04/29 02:14:28  dangan
 * [bug fix]
 *     When compression method identifier -lhd- was specified, since compressed size does not drop below original size,
 *     it was always reset to -lh0-. As a result, directory information could not be stored.
 *
 * Revision 1.1  2002/12/08 00:00:00  dangan
 * [maintenance]
 *     Updated to reflect the class name change from LhaConstants to CompressMethod.
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     Deprecated constructors taking String encode and added ones taking Properties.
 * [maintenance]
 *     Source cleanup.
 *     Eliminated tabs.
 *     Updated license text.
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1.2.2 $
 */
public class LhaOutputStream extends OutputStream {

    /**
     * Stream to output compressed data
     */
    private OutputStream out;

    /**
     * Class for CRC16 calculation
     */
    private CRC16 crc;

    /**
     * Temporary storage object
     */
    private Temporary temp;

    /**
     * Header of entry currently being compressed
     */
    private LhaHeader header;

    /**
     * Compression output stream for entry currently being compressed
     */
    private OutputStream tempOut;

    /**
     * Length of uncompressed data of entry currently being compressed
     */
    private long length;

    /**
     * Properties containing encoder generator expressions corresponding to each compression format
     */
    private Properties property;

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to {@code out}.<br>
     * Memory is used for temporary storage. Therefore, files whose compressed data exceeds available memory cannot be compressed.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of encoders corresponding to each compression format.
     *
     * @param out stream to output compressed data
     * @see LhaProperty#getProperties()
     */
    public LhaOutputStream(OutputStream out) {

        if (out != null) {

            Properties property = LhaProperty.getProperties();
            this.constructerHelper(out, new TemporaryBuffer(), property);

        } else {
            throw new NullPointerException("out");
        }
    }

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to {@code out}.<br>
     * Memory is used for temporary storage. Therefore, files whose compressed data exceeds available memory cannot be compressed.
     *
     * @param out      stream to output compressed data
     * @param property properties containing generator expressions of encoders corresponding to each compression format
     * @see LhaProperty
     */
    public LhaOutputStream(OutputStream out, Properties property) {

        if (out != null && property != null) {

            this.constructerHelper(out, new TemporaryBuffer(), property);

        } else if (out == null) {
            throw new NullPointerException("out");
        } else {
            throw new NullPointerException("property");
        }
    }

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to {@code out}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of encoders corresponding to each compression format.
     *
     * @param out  stream to output compressed data
     * @param file instance of {@link RandomAccessFile}.
     *             Must satisfy:
     *             <ul>
     *             <li>Must not be already closed.</li>
     *             <li>Must be instantiated with mode "rw" in constructor to enable reading and writing.</li>
     *             </ul>
     * @see LhaProperty#getProperties()
     */
    public LhaOutputStream(OutputStream out, RandomAccessFile file) {

        if (out != null && file != null) {

            Properties property = LhaProperty.getProperties();
            this.constructerHelper(out, new TemporaryFile(file), property);

        } else if (out == null) {
            throw new NullPointerException("out");
        } else {
            throw new NullPointerException("file");
        }
    }

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to {@code out}.
     *
     * @param out      stream to output compressed data
     * @param file     instance of {@link RandomAccessFile}.
     *                 Must satisfy:
     *                 <ul>
     *                 <li>Must not be already closed.</li>
     *                 <li>Must be instantiated with mode "rw" in constructor to enable reading and writing.</li>
     *                 </ul>
     * @param property properties containing generator expressions of encoders corresponding to each compression format
     * @see LhaProperty
     */
    public LhaOutputStream(OutputStream out, RandomAccessFile file, Properties property) {

        if (out != null && file != null && property != null) {

            this.constructerHelper(out, new TemporaryFile(file), property);

        } else if (out == null) {
            throw new NullPointerException("out");
        } else if (file == null) {
            throw new NullPointerException("file");
        } else {
            throw new NullPointerException("property");
        }
    }

    /**
     * Helper method responsible for constructor initialization.
     *
     * @param out      output stream to output data in LHA archive format
     * @param temp     temporary storage mechanism for compressed data
     * @param property properties containing generator expressions of encoders corresponding to each compression format
     */
    private void constructerHelper(OutputStream out, Temporary temp, Properties property) {
        this.out = out;
        this.temp = temp;
        this.property = property;

        this.crc = new CRC16();
        this.header = null;
        this.tempOut = null;
    }

    /**
     * Writes 1 byte of data to the current entry.
     *
     * @param data data to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(int data) throws IOException {
        if (this.tempOut != null) {
            if (this.header != null) {
                crc.update(data);
            }

            this.tempOut.write(data);
            this.length++;
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * Writes all contents of {@code buffer} to the current entry.
     *
     * @param buffer byte array containing data to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    /**
     * Writes {@code length} bytes of data starting from {@code index} of {@code buffer} to the current entry.
     *
     * @param buffer byte array containing data to write
     * @param index  start position of data to write in {@code buffer}
     * @param length number of bytes of data
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] buffer, int index, int length) throws IOException {
        if (this.tempOut != null) {
            if (this.header != null) {
                crc.update(buffer, index, length);
            }

            this.tempOut.write(buffer, index, length);
            this.length += length;
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * {@code flush} performs two operations.
     * First, it instructs data of the currently writing entry to be sent to temporary storage.
     * According to the contract of {@link PostLzssEncoder} and {@link LzssOutputStream},
     * this does not guarantee that the same data is output as when {@code flush()} is not called.
     * Second, it flushes the actual output destination.
     *
     * @throws IOException if an I/O error occurs
     * @see PostLzssEncoder#flush()
     * @see LzssOutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        if (this.tempOut != null) {
            this.tempOut.flush();
        }

        if (this.tempOut != this.out) {
            this.out.flush();
        }
    }

    /**
     * Outputs all data to destination and closes the stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (this.tempOut != null) {
            this.closeEntry();
        }

        // Output terminator
        this.out.write(0);
        this.out.close();
        this.out = null;

        this.temp.close();
        this.temp = null;

        this.property = null;
        this.crc = null;
        this.header = null;
    }

    /**
     * Sets up the stream to write a new entry.<br>
     * This method calls {@link #putNextEntryAlreadyCompressed(LhaHeader)} if the entry is already compressed,
     * or {@link #putNextEntryNotYetCompressed(LhaHeader)} if it is not yet compressed.<br>
     * Judged as not yet compressed if any of:
     * <ul>
     * <li>{@code header.getCompressedSize()}</li>
     * <li>{@code header.getOriginalSize()}</li>
     * <li>{@code header.getCRC()}</li>
     * </ul>
     * is {@link LhaHeader#UNKNOWN}.
     *
     * @param header instance of {@link LhaHeader} having information about the entry to write
     * @throws IOException if an I/O error occurs
     */
    public void putNextEntry(LhaHeader header) throws IOException {
        if (header.getCompressedSize() == LhaHeader.UNKNOWN || header.getOriginalSize() == LhaHeader.UNKNOWN
            || header.getCRC() == LhaHeader.UNKNOWN) {
            this.putNextEntryNotYetCompressed(header);
        } else {
            this.putNextEntryAlreadyCompressed(header);
        }
    }

    /**
     * Sets up the stream to write an already compressed entry.<br>
     * Since it is already compressed, it is output directly to the destination without passing through temporary storage.
     * Caller must guarantee that compressed data is correct.
     *
     * @param header instance of {@link LhaHeader} having information about the entry to write
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException <ol>
     *                                  <li>if {@code header.getOriginalSize()} returns {@link LhaHeader#UNKNOWN}</li>
     *                                  <li>if {@code header.getCompressedSize()} returns {@link LhaHeader#UNKNOWN}</li>
     *                                  <li>if {@code header.getCRC()} returns {@link LhaHeader#UNKNOWN}</li>
     *                                  </ol>
     */
    public void putNextEntryAlreadyCompressed(LhaHeader header) throws IOException {
        if (header.getOriginalSize() != LhaHeader.UNKNOWN && header.getCompressedSize() != LhaHeader.UNKNOWN
            && header.getCRC() != LhaHeader.UNKNOWN) {

            if (this.tempOut != null) {
                this.closeEntry();
            }

            String encoding = this.property.getProperty("lha.encoding");
            if (encoding == null) {
                encoding = LhaProperty.getProperty("lha.encoding");
            }
            this.out.write(header.getBytes(encoding));
            this.tempOut = out;

        } else if (header.getOriginalSize() == LhaHeader.UNKNOWN) {
            throw new IllegalArgumentException("OriginalSize must not \"LhaHeader.UNKNOWN\".");
        } else if (header.getCompressedSize() == LhaHeader.UNKNOWN) {
            throw new IllegalArgumentException("CompressedSize must not \"LhaHeader.UNKNOWN\".");
        } else {
            throw new IllegalArgumentException("CRC must not \"LhaHeader.UNKNOWN\".");
        }
    }

    /**
     * Sets up the stream to write an entry not yet compressed.<br>
     * Even if {@code OriginalSize}, {@code CompressedSize}, or {@code CRC} is specified in {@code header}, they are ignored.
     *
     * @param header instance of {@link LhaHeader} having information about the entry to write
     * @throws IOException if an I/O error occurs
     */
    public void putNextEntryNotYetCompressed(LhaHeader header) throws IOException {
        if (this.tempOut != null) {
            this.closeEntry();
        }

        this.crc.reset();
        this.length = 0;
        this.header = (LhaHeader) header.clone();
        this.tempOut = CompressMethod.connectEncoder(this.temp.getOutputStream(), header.getCompressMethod(), this.property);
    }

    /**
     * Closes the currently outputting entry and prepares the stream to output the next entry.
     * If compression fails (compressed size exceeds original size), decompresses and stores uncompressed.
     * If entry size is large, this processing takes considerable time.
     *
     * @throws IOException if an I/O error occurs
     */
    public void closeEntry() throws IOException {
        if (this.header != null) {
            this.tempOut.close();
            InputStream in;

            if (this.temp.length() < this.length) {
                this.header.setOriginalSize(this.length);
                this.header.setCompressedSize(this.temp.length());
                this.header.setCRC((int) crc.getValue());

                in = this.temp.getInputStream();
            } else {
                String method = this.header.getCompressMethod();

                this.header.setOriginalSize(this.length);
                this.header.setCompressedSize(this.length);
                this.header.setCRC((int) crc.getValue());
                if (!this.header.getCompressMethod().equalsIgnoreCase(CompressMethod.LHD)) {
                    this.header.setCompressMethod(CompressMethod.LH0);
                }

                in = this.temp.getInputStream();
                in = CompressMethod.connectDecoder(in, method, this.property, this.temp.length());
            }

            String encoding = this.property.getProperty("lha.encoding");
            if (encoding == null) {
                encoding = LhaProperty.getProperty("lha.encoding");
            }
            this.out.write(this.header.getBytes(encoding));

            byte[] buffer = new byte[8192];
            int length;
            while (0 <= (length = in.read(buffer))) {
                this.out.write(buffer, 0, length);
            }
        }
        this.header = null;
        this.tempOut = null;
    }

    /**
     * Provides a temporary storage mechanism for data.
     */
    private interface Temporary {

        /**
         * Gets an {@link InputStream} to retrieve data stored in the temporary storage mechanism.<br>
         * This data is identical to the data output to the {@link OutputStream} provided by the immediately preceding {@link #getOutputStream()}.<br>
         * {@link #getOutputStream()} must not be called until the {@link InputStream} obtained by {@link #getInputStream()} is closed.<br>
         * Also, {@link #getInputStream()} must not be called again until the {@link InputStream} obtained by {@link #getInputStream()} is closed.
         *
         * @return {@link InputStream} to retrieve data from temporary storage mechanism
         * @throws IOException if an I/O error occurs
         */
        InputStream getInputStream() throws IOException;

        /**
         * Gets an {@link OutputStream} to store data in the temporary storage mechanism.<br>
         * Stored data can be obtained from the {@link InputStream} obtained by immediately following {@link #getInputStream()}.<br>
         * {@link #getInputStream()} must not be called until the {@link OutputStream} obtained by {@link #getOutputStream()} is closed.
         * Also, {@link #getOutputStream()} must not be called again until the {@link OutputStream} obtained by {@link #getOutputStream()} is closed.
         *
         * @return {@link OutputStream} to store data in temporary storage mechanism
         * @throws IOException if an I/O error occurs
         */
        OutputStream getOutputStream() throws IOException;

        /**
         * Gets the amount of data stored in the temporary storage mechanism.
         * This is identical to the amount of data output to the {@link OutputStream} provided by the immediately preceding {@link #getOutputStream()}.
         *
         * @return amount of data stored in temporary storage mechanism
         */
        long length() throws IOException;

        /**
         * Releases all system resources used by the temporary storage mechanism.
         *
         * @throws IOException if an I/O error occurs
         */
        void close() throws IOException;
    }

    /**
     * Class that uses {@link RandomAccessFile} for temporary storage mechanism.
     */
    private static class TemporaryFile implements Temporary {

        /**
         * {@link RandomAccessFile} used for temporary storage mechanism
         */
        private RandomAccessFile tempfile;

        /**
         * Holds the amount of data output to the {@link OutputStream} provided by {@link #getOutputStream()}.
         */
        private long length;

        /**
         * Constructs a {@link TemporaryFile} using {@code file}.
         *
         * @param file instance of {@link RandomAccessFile}
         */
        public TemporaryFile(RandomAccessFile file) {
            if (file != null) {
                this.tempfile = file;
            } else {
                throw new NullPointerException("file");
            }
        }

        /**
         * Gets an {@link InputStream} to retrieve data stored in the temporary storage mechanism.<br>
         * This data is identical to the data output to the {@link OutputStream} provided by the immediately preceding {@link #getOutputStream()}.
         *
         * @return {@link InputStream} to retrieve data from temporary storage mechanism
         * @throws IOException if an I/O error occurs
         */
        @Override
        public InputStream getInputStream() throws IOException {
            return new TemporaryFileInputStream();
        }

        /**
         * Gets an {@link OutputStream} to store data in the temporary storage mechanism.<br>
         * Stored data can be obtained from the {@link InputStream} obtained by immediately following {@link #getInputStream()}.
         *
         * @return {@link OutputStream} to store data in temporary storage mechanism
         * @throws IOException if an I/O error occurs
         */
        @Override
        public OutputStream getOutputStream() throws IOException {
            return new TemporaryFileOutputStream();
        }

        /**
         * Gets the amount of data stored in the temporary storage mechanism.<br>
         * This is identical to the amount of data output to the {@link OutputStream} provided by the immediately preceding {@link #getOutputStream()}.
         *
         * @return amount of data stored in temporary storage mechanism
         */
        @Override
        public long length() {
            return this.length;
        }

        /**
         * Releases all system resources used by the temporary storage mechanism.
         * The {@link RandomAccessFile} provided in constructor is closed.
         *
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void close() throws IOException {
            this.tempfile.close();
            this.tempfile = null;
        }

        /**
         * Input stream for {@link TemporaryFile}
         */
        private class TemporaryFileInputStream extends InputStream {

            /**
             * Constructs an {@link InputStream} that reads data from {@link TemporaryFile}.<br>
             *
             * @throws IOException if an I/O error occurs
             */
            public TemporaryFileInputStream() throws IOException {
                TemporaryFile.this.tempfile.seek(0);
            }

            /**
             * Reads 1 byte of data from {@link TemporaryFile}.
             *
             * @return 1 byte of read data, or -1 if EndOfStream has already been reached
             * @throws IOException if an I/O error occurs
             */
            @Override
            public int read() throws IOException {
                long pos = TemporaryFile.this.tempfile.getFilePointer();
                long limit = TemporaryFile.this.length;

                if (pos < limit) {
                    return TemporaryFile.this.tempfile.read();
                } else {
                    return -1;
                }
            }

            /**
             * Reads data from {@link TemporaryFile} to fill {@code buffer}.
             *
             * @param buffer buffer to read data into
             * @return amount of read data, or -1 if EndOfStream has already been reached
             * @throws IOException if an I/O error occurs
             */
            @Override
            public int read(byte[] buffer) throws IOException {
                return this.read(buffer, 0, buffer.length);
            }

            /**
             * Reads {@code length} bytes of data from {@link TemporaryFile} into {@code buffer} starting at {@code index}.
             *
             * @param buffer buffer to read data into
             * @param index  start position of data reading in {@code buffer}
             * @param length amount of data to read
             * @return amount of read data, or -1 if EndOfStream has already been reached
             * @throws IOException if an I/O error occurs
             */
            @Override
            public int read(byte[] buffer, int index, int length) throws IOException {
                long pos = TemporaryFile.this.tempfile.getFilePointer();
                long limit = TemporaryFile.this.length;
                length = (int) (Math.min(pos + length, limit) - pos);

                if (pos < limit) {
                    return TemporaryFile.this.tempfile.read(buffer, index, length);
                } else {
                    return -1;
                }
            }
        }

        /**
         * Output stream for {@link TemporaryFile}
         */
        private class TemporaryFileOutputStream extends OutputStream {

            /**
             * Constructs an {@link OutputStream} that outputs data to {@link TemporaryFile}.
             *
             * @throws IOException if an I/O error occurs
             */
            public TemporaryFileOutputStream() throws IOException {
                TemporaryFile.this.tempfile.seek(0);
                TemporaryFile.this.length = 0;
            }

            /**
             * Writes 1 byte of data to {@link TemporaryFile}.
             *
             * @param data 1 byte of data to write
             * @throws IOException if an I/O error occurs
             */
            @Override
            public void write(int data) throws IOException {
                TemporaryFile.this.tempfile.write(data);
                TemporaryFile.this.length++;
            }

            /**
             * Writes all contents of {@code buffer} to {@link TemporaryFile}.
             *
             * @param buffer byte array containing data to write
             * @throws IOException if an I/O error occurs
             */
            @Override
            public void write(byte[] buffer) throws IOException {
                TemporaryFile.this.tempfile.write(buffer);
                TemporaryFile.this.length += buffer.length;
            }

            /**
             * Writes {@code length} bytes of contents of {@code buffer} starting from {@code index} to {@link TemporaryFile}.
             *
             * @param buffer byte array containing data to write
             * @param index  start position of data to write in {@code buffer}
             * @param length amount of data to write
             * @throws IOException if an I/O error occurs
             */
            @Override
            public void write(byte[] buffer, int index, int length) throws IOException {
                TemporaryFile.this.tempfile.write(buffer, index, length);
                TemporaryFile.this.length += length;
            }
        }
    }

    /**
     * Class that uses {@link GrowthByteBuffer} for temporary storage mechanism.
     */
    private static class TemporaryBuffer implements Temporary {

        /**
         * Buffer used for temporary storage mechanism
         */
        private GrowthByteBuffer tempBuffer;

        /**
         * Constructs a search mechanism using {@link GrowthByteBuffer}.
         */
        public TemporaryBuffer() {
            this.tempBuffer = new GrowthByteBuffer();
        }

        /**
         * Gets an {@link InputStream} to retrieve data stored in the temporary storage mechanism.<br>
         * This data is identical to the data output to the {@link OutputStream} provided by the immediately preceding {@link #getOutputStream()}.
         *
         * @return {@link InputStream} to retrieve data from temporary storage mechanism
         */
        @Override
        public InputStream getInputStream() {
            return new TemporaryBufferInputStream();
        }

        /**
         * Gets an {@link OutputStream} to store data in the temporary storage mechanism.<br>
         * Stored data can be obtained from the {@link InputStream} obtained by immediately following {@link #getInputStream()}.
         *
         * @return {@link OutputStream} to store data in temporary storage mechanism
         */
        @Override
        public OutputStream getOutputStream() {
            return new TemporaryBufferOutputStream();
        }

        /**
         * Gets the amount of data stored in the temporary storage mechanism.<br>
         * This is identical to the amount of data output to the {@link OutputStream} provided by the immediately preceding {@link #getOutputStream()}.
         *
         * @return amount of data stored in temporary storage mechanism
         */
        @Override
        public long length() {
            return this.tempBuffer.length();
        }

        /**
         * Releases all system resources used by the temporary storage mechanism.
         */
        @Override
        public void close() {
            this.tempBuffer = null;
        }

        /**
         * Input stream for {@link TemporaryBuffer}
         */
        private class TemporaryBufferInputStream extends InputStream {

            /**
             * Constructs an {@link InputStream} that reads data from {@link TemporaryBuffer}.
             */
            public TemporaryBufferInputStream() {
                TemporaryBuffer.this.tempBuffer.seek(0);
            }

            /**
             * Reads 1 byte of data from {@link TemporaryBuffer}.
             *
             * @return 1 byte of read data, or -1 if EndOfStream has already been reached
             */
            @Override
            public int read() {
                return TemporaryBuffer.this.tempBuffer.read();
            }

            /**
             * Reads data from {@link TemporaryBuffer} to fill {@code buffer}.
             *
             * @param buffer buffer to read data into
             * @return amount of read data, or -1 if EndOfStream has already been reached
             */
            @Override
            public int read(byte[] buffer) {
                return TemporaryBuffer.this.tempBuffer.read(buffer);
            }

            /**
             * Reads {@code length} bytes of data from {@link TemporaryBuffer} into {@code buffer} starting at {@code index}.
             *
             * @param buffer buffer to read data into
             * @param index  start position of data reading in {@code buffer}
             * @param length amount of data to read
             * @return amount of read data, or -1 if EndOfStream has already been reached
             */
            @Override
            public int read(byte[] buffer, int index, int length) {
                return TemporaryBuffer.this.tempBuffer.read(buffer, index, length);
            }
        }

        /**
         * Output stream for {@link TemporaryBuffer}
         */
        private class TemporaryBufferOutputStream extends OutputStream {

            /**
             * Constructs an {@link OutputStream} that outputs data to {@link TemporaryBuffer}.
             */
            public TemporaryBufferOutputStream() {
                TemporaryBuffer.this.tempBuffer.seek(0);
                TemporaryBuffer.this.tempBuffer.setLength(0);
            }

            /**
             * Writes 1 byte of data to {@link TemporaryBuffer}.
             *
             * @param data 1 byte of data to write
             */
            @Override
            public void write(int data) {
                TemporaryBuffer.this.tempBuffer.write(data);
            }

            /**
             * Writes all contents of {@code buffer} to {@link TemporaryBuffer}.
             *
             * @param buffer byte array containing data to write
             */
            @Override
            public void write(byte[] buffer) {
                TemporaryBuffer.this.tempBuffer.write(buffer);
            }

            /**
             * Writes {@code length} bytes of contents of {@code buffer} starting from {@code index} to {@link TemporaryBuffer}.
             *
             * @param buffer byte array containing data to write
             * @param index  start position of data to write in {@code buffer}
             * @param length amount of data to write
             */
            @Override
            public void write(byte[] buffer, int index, int length) {
                TemporaryBuffer.this.tempBuffer.write(buffer, index, length);
            }
        }
    }
}
