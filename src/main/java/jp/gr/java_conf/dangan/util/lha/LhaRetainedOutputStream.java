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

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Properties;
import java.util.Vector;


/**
 * Utility class for outputting compressed data to a connected {@link RandomAccessFile}.<br>
 * Designed to have an interface similar to {@link java.util.zip.ZipOutputStream}.<br>
 * Automatically handles processing when compression fails (when compressed size exceeds uncompressed size).
 * When implementing progress reporting, hiding such processing inside the class will cause progress reporting
 * to become unresponsive for several seconds or even tens of minutes (for example, when dealing with gigabyte-class data).
 * If you want to avoid such situations, use {@link LhaImmediateOutputStream}.<br>
 * Also, in JDK 1.1 and earlier, {@link RandomAccessFile} does not have {@code setLength}, so the file size cannot be truncated
 * even if there is other data following the archive data.
 * This problem can be avoided by always opening a new file of size 0.
 *
 * <pre>
 * -- revision history --
 * $Log: LhaRetainedOutputStream.java,v $
 * Revision 1.2  2002/12/11 02:25:14  dangan
 * [bug fix]
 *     Fixed parts that failed to compile on JDK 1.2.
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
 * @version $Revision: 1.2 $
 */
public class LhaRetainedOutputStream extends OutputStream {

    /**
     * Archive file
     */
    private RandomAccessFile archive;

    /**
     * Compression output stream
     */
    private OutputStream out;

    /**
     * Compression output stream
     */
    private RandomAccessFileOutputStream rafo;

    /**
     * Header currently being compressed
     */
    private LhaHeader header;

    /**
     * Encoding used for header output
     */
    private String encoding;

    /**
     * Header position
     */
    private long headerpos;

    /**
     * For CRC value calculation
     */
    private CRC16 crc;

    /**
     * Properties containing encoder generator expressions corresponding to each compression format
     */
    private Properties property;

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to the file {@code filename}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of encoders corresponding to each compression format.
     *
     * @param filename name of file to write compressed data to
     * @throws FileNotFoundException if the file given by {@code filename} is not found
     * @throws SecurityException     if security manager denies access to the file
     * @see LhaProperty#getProperties()
     */
    public LhaRetainedOutputStream(String filename) throws FileNotFoundException {

        if (filename != null) {
            RandomAccessFile file = new RandomAccessFile(filename, "rw");
            Properties property = LhaProperty.getProperties();

            this.constructerHelper(file, property);
        } else {
            throw new NullPointerException("filename");
        }
    }

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to the file {@code filename}.
     *
     * @param filename name of file to write compressed data to
     * @param property properties containing generator expressions of encoders corresponding to each compression format
     * @throws FileNotFoundException if the file given by {@code filename} is not found
     * @throws SecurityException     if security manager denies access to the file
     * @see LhaProperty
     */
    public LhaRetainedOutputStream(String filename, Properties property) throws FileNotFoundException {

        if (filename != null) {
            RandomAccessFile file = new RandomAccessFile(filename, "rw");
            this.constructerHelper(file, property);
        } else {
            throw new NullPointerException("filename");
        }
    }

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to the file {@code filename}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of encoders corresponding to each compression format.
     *
     * @param filename name of file to write compressed data to
     * @throws FileNotFoundException if the file given by {@code filename} is not found
     * @throws SecurityException     if security manager denies access to the file
     * @throws IOException           exists only for compilation in JDK 1.2
     * @see LhaProperty#getProperties()
     */
    public LhaRetainedOutputStream(File filename) throws IOException {

        if (filename != null) {
            RandomAccessFile file = new RandomAccessFile(filename, "rw");
            Properties property = LhaProperty.getProperties();

            this.constructerHelper(file, property);
        } else {
            throw new NullPointerException("filename");
        }
    }

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to the file {@code filename}.
     *
     * @param filename name of file to write compressed data to
     * @param property properties containing generator expressions of encoders corresponding to each compression format
     * @throws FileNotFoundException if the file given by {@code filename} is not found
     * @throws SecurityException     if security manager denies access to the file
     * @throws IOException           exists only for compilation in JDK 1.2
     * @see LhaProperty
     */
    public LhaRetainedOutputStream(File filename, Properties property) throws IOException {

        if (filename != null) {
            RandomAccessFile file = new RandomAccessFile(filename, "rw");
            this.constructerHelper(file, property);
        } else {
            throw new NullPointerException("filename");
        }
    }

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to {@code file}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of encoders corresponding to each compression format.
     *
     * @param file instance of {@link RandomAccessFile}.<ul>
     *             <li>Must not be already closed.
     *             <li>Must be instantiated with mode "rw" in constructor to enable reading and writing.
     *             </ul>
     * @see LhaProperty#getProperties()
     */
    public LhaRetainedOutputStream(RandomAccessFile file) {

        if (file != null) {
            Properties property = LhaProperty.getProperties();
            this.constructerHelper(file, property);
        } else {
            throw new NullPointerException("out");
        }
    }

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to {@code file}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of encoders corresponding to each compression format.
     *
     * @param file     instance of {@link RandomAccessFile}.<ul>
     *                 <li>Must not be already closed.
     *                 <li>Must be instantiated with mode "rw" in constructor to enable reading and writing.
     *                 </ul>
     * @param property properties containing generator expressions of encoders corresponding to each compression format
     * @see LhaProperty
     */
    public LhaRetainedOutputStream(RandomAccessFile file, Properties property) {

        if (file != null && property != null) {

            this.constructerHelper(file, property);

        } else if (file == null) {
            throw new NullPointerException("null");
        } else {
            throw new NullPointerException("property");
        }
    }

    /**
     * Helper method responsible for constructor initialization.
     *
     * @param file     instance of {@link RandomAccessFile}.<ul>
     *                 <li>Must not be already closed.
     *                 <li>Must be instantiated with mode "rw" in constructor to enable reading and writing.
     *                 </ul>
     * @param property properties containing generator expressions of encoders corresponding to each compression format
     */
    private void constructerHelper(RandomAccessFile file, Properties property) {

        this.archive = file;

        this.out = null;
        this.header = null;
        this.headerpos = -1;
        this.crc = new CRC16();
        this.property = property;
    }

    /**
     * Writes 1 byte of data to the current entry.
     *
     * @param data data to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(int data) throws IOException {
        if (this.out != null) {
            if (this.header != null) {
                crc.update(data);
            }

            this.out.write(data);
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
        if (this.out != null) {
            if (this.header != null) {
                crc.update(buffer, index, length);
            }

            this.out.write(buffer, index, length);
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * Forces data of the currently writing entry to be written to destination.
     * According to the contract of {@link PostLzssEncoder} and {@link LzssOutputStream},
     * this outputs different data than when {@link #flush()} is not called.
     * (In most cases, it simply reduces the compression ratio.)
     *
     * @throws IOException if an I/O error occurs
     * @see PostLzssEncoder#flush()
     * @see LzssOutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        if (this.out != null) {
            this.out.flush();
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * Outputs all data to destination and closes the stream.<br>
     * Also releases all resources used.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (this.out != null) {
            this.closeEntry();
        }

        // Output terminator
        this.archive.write(0);
        try {
            this.archive.setLength(this.archive.getFilePointer());
        } catch (NoSuchMethodError error) {
        }

        this.archive.close();
        this.archive = null;
        this.header = null;
        this.crc = null;
        this.property = null;
        this.rafo = null;
    }

    /**
     * Sets up the stream to write a new entry.<br>
     * This method calls {@link #putNextEntryAlreadyCompressed(LhaHeader)} if the entry is already compressed,
     * or {@link #putNextEntryNotYetCompressed(LhaHeader)} if it is not yet compressed.<br>
     * Judged as not yet compressed if any of:
     * <ul>
     * <li>{@code header.getCompressedSize()}
     * <li>{@code header.getCRC()}
     * </ul>
     * is {@link LhaHeader#UNKNOWN}.<br>
     * {@code header} must have an accurate {@code OriginalSize} specified.
     *
     * @param header instance of {@link LhaHeader} having information about the entry to write
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if {@code header.getOriginalSize()} returns {@link LhaHeader#UNKNOWN}
     */
    public void putNextEntry(LhaHeader header) throws IOException {
        if (header.getCompressedSize() == LhaHeader.UNKNOWN || header.getCRC() == LhaHeader.UNKNOWN) {
            this.putNextEntryNotYetCompressed(header);
        } else {
            this.putNextEntryAlreadyCompressed(header);
        }
    }

    /**
     * Sets up the stream to write an already compressed entry.<br>
     * Caller must guarantee that compressed data is correct.
     *
     * @param header instance of {@link LhaHeader} having information about the entry to write
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException <ol>
     *                                  <li>if {@code header.getOriginalSize()} returns {@link LhaHeader#UNKNOWN}
     *                                  <li>if {@code header.getCompressedSize()} returns {@link LhaHeader#UNKNOWN}
     *                                  <li>if {@code header.getCRC()} returns {@link LhaHeader#UNKNOWN}
     *                                  </ol>
     */
    public void putNextEntryAlreadyCompressed(LhaHeader header) throws IOException {
        if (header.getOriginalSize() != LhaHeader.UNKNOWN && header.getCompressedSize() != LhaHeader.UNKNOWN
                && header.getCRC() != LhaHeader.UNKNOWN) {

            if (this.out != null) {
                this.closeEntry();
            }

            this.headerpos = this.archive.getFilePointer();

            this.encoding = this.property.getProperty("lha.encoding");
            if (this.encoding == null) {
                encoding = LhaProperty.getProperty("lha.encoding");
            }

            this.archive.write(header.getBytes(encoding));
            this.out = new RandomAccessFileOutputStream(this.archive, header.getCompressedSize());

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
     * {@code header} must have an accurate {@code OriginalSize} specified.<br>
     * Even if {@code CompressedSize} or {@code CRC} is specified in {@code header}, they are ignored.
     *
     * @param header instance of {@link LhaHeader} having information about the entry to write
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if {@code header.getOriginalSize()} returns {@link LhaHeader#UNKNOWN}
     */
    public void putNextEntryNotYetCompressed(LhaHeader header) throws IOException {
        if (header.getOriginalSize() != LhaHeader.UNKNOWN) {
            if (this.out != null) {
                this.closeEntry();
            }

            this.crc.reset();
            this.headerpos = this.archive.getFilePointer();
            this.header = (LhaHeader) header.clone();
            this.header.setCompressedSize(0);
            this.header.setCRC(0);

            this.encoding = this.property.getProperty("lha.encoding");
            if (this.encoding == null) {
                this.encoding = LhaProperty.getProperty("lha.encoding");
            }

            this.archive.write(this.header.getBytes(encoding));
            this.rafo = new RandomAccessFileOutputStream(this.archive, header.getOriginalSize());
            this.out = CompressMethod.connectEncoder(this.rafo, header.getCompressMethod(), this.property);

        } else {
            throw new IllegalArgumentException("OriginalSize must not \"LhaHeader.UNKNOWN\".");
        }
    }

    /**
     * Closes the currently outputting entry and prepares the stream to output the next entry.<br>
     * If compression fails (compressed size exceeds uncompressed size), decompresses and stores uncompressed.
     * If entry size is large, this processing takes considerable time.
     *
     * @throws IOException if an I/O error occurs
     */
    public void closeEntry() throws IOException {
        if (this.header != null) {
            this.out.close();

            if (!this.rafo.cache.isEmpty()) {
                RandomAccessFileInputStream rafi;
                InputStream in;
                long pos = this.rafo.start;
                rafi = new RandomAccessFileInputStream(this.archive, this.rafo);
                in = CompressMethod
                        .connectDecoder(rafi, header.getCompressMethod(), this.property, this.header.getOriginalSize());

                byte[] buffer = new byte[8192];
                int length;
                while (0 <= (length = in.read(buffer))) {
                    rafi.cache(pos + length);
                    this.archive.seek(pos);
                    this.archive.write(buffer, 0, length);
                    pos += length;
                }
                in.close();

                this.header.setCompressMethod(CompressMethod.LH0);
            }

            long pos = this.archive.getFilePointer();
            long size = (pos - this.headerpos - this.header.getBytes(this.encoding).length);
            this.header.setCompressedSize(size);
            if (this.header.getCRC() != LhaHeader.NO_CRC) {
                this.header.setCRC((int) this.crc.getValue());
            }

            this.archive.seek(this.headerpos);
            this.archive.write(this.header.getBytes(this.encoding));
            this.archive.seek(pos);
        }
        this.header = null;
        this.out = null;
    }

    /**
     * Wrapper class to adapt {@link RandomAccessFile} to the {@link OutputStream} interface.
     */
    private static class RandomAccessFileOutputStream extends OutputStream {

        /**
         * Destination {@link RandomAccessFile}
         */
        private RandomAccessFile archive;

        /**
         * Cache when attempting to write beyond storage limit
         */
        private final Cache cache;

        /**
         * Storage start position
         */
        private final long start;

        /**
         * Current processing position
         */
        private long pos;

        /**
         * Storage limit
         */
        private final long limit;

        /**
         * Constructs an {@link OutputStream} wrapping {@link RandomAccessFile}.
         *
         * @param archive destination {@link RandomAccessFile}
         * @param length  output limit length
         * @throws IOException if an I/O error occurs
         */
        public RandomAccessFileOutputStream(RandomAccessFile archive, long length) throws IOException {
            this.archive = archive;
            this.start = this.archive.getFilePointer();
            this.pos = this.start;
            this.limit = this.start + length;
            this.cache = new Cache();
        }

        /**
         * Writes 1 byte to connected {@link RandomAccessFile}.
         *
         * @param data 1 byte of data to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(int data) throws IOException {
            if (this.pos < this.limit && this.cache.isEmpty()) {
                this.pos++;
                this.archive.write(data);
            } else {
                this.cache.add(new byte[] {
                        (byte) data
                });
            }
        }

        /**
         * Writes all contents of {@code buffer} to connected {@link RandomAccessFile}.
         *
         * @param buffer byte array containing data to write
         * @throws IOException  if an I/O error occurs
         * @throws EOFException if attempting to write beyond the length passed to constructor
         */
        @Override
        public void write(byte[] buffer) throws IOException {
            this.write(buffer, 0, buffer.length);
        }

        /**
         * Writes {@code length} bytes of contents of {@code buffer} starting from {@code index} to connected {@link RandomAccessFile}.
         *
         * @param buffer byte array containing data to write
         * @param index  start position of data to write in {@code buffer}
         * @param length amount of data to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(byte[] buffer, int index, int length) throws IOException {

            if (this.pos + length <= this.limit && this.cache.isEmpty()) {
                this.pos += length;
                this.archive.write(buffer, index, length);
            } else if (this.pos < this.limit && this.cache.isEmpty()) {
                int len = (int) (this.limit - this.pos);
                this.pos += len;
                this.archive.write(buffer, index, len);
                this.cache.add(buffer, index + len, length - len);
            } else {
                this.cache.add(buffer, index, length);
            }
        }

        /**
         * Closes this stream and releases resources used.
         */
        @Override
        public void close() {
            this.archive = null;
        }
    }

    /**
     * Wrapper class that applies the {@link InputStream} interface to {@link RandomAccessFile}.
     * Necessary for the process of decompressing and re-storing uncompressed when compressed size exceeds uncompressed size.
     */
    private static class RandomAccessFileInputStream extends InputStream {

        /**
         * Source {@link RandomAccessFile}
         */
        private RandomAccessFile archive;

        /**
         * Front cache.
         * Cache for when writing overtakes reading.
         */
        private Cache front;

        /**
         * Rear cache.
         * Cache for data exceeding write limit.
         */
        private Cache rear;

        /**
         * Current processing position
         */
        private long pos;

        /**
         * Read limit
         */
        private final long limit;

        /**
         * Constructs an {@link InputStream} wrapping {@link RandomAccessFile}.
         *
         * @param archive {@link RandomAccessFile} providing data
         * @param out     {@link RandomAccessFileOutputStream} that received compressed data immediately before
         */
        public RandomAccessFileInputStream(RandomAccessFile archive, RandomAccessFileOutputStream out) {
            this.archive = archive;
            this.pos = out.start;
            this.limit = out.pos;
            this.front = new Cache();
            this.rear = out.cache;
        }

        /**
         * Reads 1 byte of data from cache or {@link RandomAccessFile}.
         *
         * @return 1 byte of read data,<br>
         * or -1 if there is no data to read
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read() throws IOException {
            int return_value = this.front.read();
            if (return_value < 0) {
                if (this.pos < this.limit) {
                    this.archive.seek(this.pos++);
                    return_value = this.archive.read();
                } else {
                    return_value = this.rear.read();
                }
            }

            return return_value;
        }

        /**
         * Reads data from cache or {@link RandomAccessFile} to fill {@code buffer}.
         *
         * @param buffer buffer to store read data
         * @return actual amount of data read
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read(byte[] buffer) throws IOException {
            return this.read(buffer, 0, buffer.length);
        }

        /**
         * Reads {@code length} bytes of data from cache or {@link RandomAccessFile} into {@code buffer} starting at {@code index}.
         *
         * @param buffer buffer to store read data
         * @param index  start position of reading in {@code buffer}
         * @param length amount of data to read
         * @return actual amount of data read
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read(byte[] buffer, int index, int length) throws IOException {

            int count = 0;
            int ret = this.front.read(buffer, index, length);
            if (0 <= ret) {
                count += ret;
            }

            this.archive.seek(this.pos);
            ret = Math.min(length - count, Math.max((int) (this.limit - this.pos), 0));
            this.archive.readFully(buffer, index + count, ret);
            if (0 <= ret) {
                this.pos += ret;
                count += ret;
            }

            ret = this.rear.read(buffer, index + count, length - count);
            if (0 <= ret) {
                count += ret;
            }

            if (0 < count) {
                return count;
            } else {
                return -1;
            }
        }

        /**
         * Closes this stream and releases resources used.
         */
        @Override
        public void close() {
            this.front = null;
            this.rear = null;
            this.archive = null;
        }

        /**
         * If not yet read up to {@code pos}, adds data from current read position to {@code pos} into front cache.
         *
         * @param pos write position in archive
         */
        public void cache(long pos) throws IOException {
            int length = (int) Math.min(this.limit - this.pos, pos - this.pos);

            byte[] buffer = new byte[length];
            if (0 < length) {
                this.archive.seek(this.pos);
                this.archive.readFully(buffer);
                this.front.add(buffer);

                this.pos += length;
            }
        }
    }

    /**
     * Used to cache data when writing beyond write limit or writing beyond read position.
     */
    private static class Cache {

        /**
         * {@link Vector} of {@code byte[]}.
         * Each element is discarded as soon as it is completely read.
         */
        private final Vector<byte[]> cache;

        /**
         * Element currently being read
         */
        private byte[] current;

        /**
         * Current processing position in {@code current}
         */
        private int position;

        /**
         * Constructs a temporary storage mechanism for data.
         */
        public Cache() {
            this.current = null;
            this.position = 0;
            this.cache = new Vector<>();
        }

        /**
         * Reads 1 byte of data from cache mapped to 0-255.
         *
         * @return 1 byte of read data,<br>
         * or -1 if cache is empty and there is no data
         */
        public int read() {
            if (null != this.current) {
                int ret = this.current[this.position++] & 0xFF;

                if (this.current.length <= this.position) {
                    if (!this.cache.isEmpty()) {
                        this.current = this.cache.firstElement();
                        this.cache.removeElementAt(0);
                    } else {
                        this.current = null;
                    }
                    this.position = 0;
                }

                return ret;
            } else {
                return -1;
            }
        }

        /**
         * Reads {@code length} bytes from cache into {@code buffer} starting at {@code index}.
         *
         * @param buffer buffer holding read data
         * @param index  start position of reading in {@code buffer}
         * @param length amount of data to read
         * @return actual amount of data read,<br>
         * or -1 if cache is empty and there is no data
         */
        public int read(byte[] buffer, int index, int length) {
            int count = 0;

            while (null != this.current && count < length) {
                int copylen = Math.min(this.current.length - this.position, length - count);
                System.arraycopy(this.current, this.position, buffer, index + count, copylen);

                this.position += copylen;
                count += copylen;

                if (this.current.length <= this.position) {
                    if (!this.cache.isEmpty()) {
                        this.current = this.cache.firstElement();
                        this.cache.removeElementAt(0);
                    } else {
                        this.current = null;
                    }
                    this.position = 0;
                }
            }

            if (count == 0) {
                return -1;
            } else {
                return count;
            }
        }

        /**
         * Adds data to cache.
         *
         * @param buffer buffer storing data
         */
        public void add(byte[] buffer) {
            if (this.current == null) {
                this.current = buffer;
            } else {
                this.cache.addElement(buffer);
            }
        }

        /**
         * Adds data to cache.
         *
         * @param buffer buffer storing data
         * @param index  data start position in {@code buffer}
         * @param length amount of data stored
         */
        public void add(byte[] buffer, int index, int length) {
            byte[] buf = new byte[length];
            System.arraycopy(buffer, index, buf, 0, length);

            if (this.current == null) {
                this.current = buf;
            } else {
                this.cache.addElement(buf);
            }
        }

        /**
         * Gets whether this cache is empty.
         *
         * @return {@code true} if this cache is empty,
         * or {@code false} if not empty
         */
        public boolean isEmpty() {
            return this.current == null;
        }
    }
}
