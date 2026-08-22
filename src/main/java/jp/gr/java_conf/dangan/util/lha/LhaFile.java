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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;


/**
 * Utility class for obtaining an {@link InputStream} to read entry data
 * from an LHA archive file.<br>
 * Designed to have an interface similar to {@link java.util.zip.ZipFile}.
 * Checksum/CRC verification using CRC16 is not performed.
 *
 * <pre>
 * -- revision history --
 * $Log: LhaFile.java,v $
 * Revision 1.1  2002/12/08 00:00:00  dangan
 * [maintenance]
 *     Updated to reflect the class name change from LhaConstants to CompressMethod.
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [improvement]
 *     Improved entry start position lookup in archives with many entries
 *     by managing entries using a Hashtable.
 * [change]
 *     Deprecated constructors taking String encode and added ones taking Properties.
 * [maintenance]
 *     Source cleanup.
 *     Eliminated tabs.
 *     Updated license text.
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class LhaFile implements Closeable {

    /**
     * Instance of {@link RandomAccessFile} holding data in LHA archive format.
     */
    private RandomAccessFile archive;

    /**
     * Object that last accessed {@code archive}.
     */
    private Object lastAccessObject;

    /**
     * List of {@link LhaHeader} holding header of each entry.
     * Entry with header at {@code headers.get(index)} starts from the position at {@code entryPoint.get(index)}.
     */
    private List<LhaHeader> headers;

    /**
     * List of start positions of each entry.
     * Entry with header at {@code headers.get(index)} starts from the position at {@code entryPoint.get(index)}.
     */
    private List<Long> entryPoint;

    /**
     * Hash table having entry name (stored file name) as key, and index of entry with that key name as value.
     * Elements are {@link Integer}.
     */
    private Map<String, Integer> hash;

    /**
     * For rescuing files with identical names.
     * List having indices of entries with duplicate names.
     * Elements are {@link Integer}.
     */
    private List<Integer> duplicate;

    /**
     * Properties containing decoder generator expressions corresponding to each compression format
     */
    private Properties property;

    /**
     * Constructs an {@link LhaFile} that reads archive data from the file specified by {@code filename}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of decoders corresponding to each compression format.
     *
     * @param filename name of LHA archive file
     * @throws IOException           if an I/O error occurs
     * @throws FileNotFoundException if file is not found
     * @throws SecurityException     if security manager denies read access to the file
     * @see LhaProperty#getProperties()
     */
    public LhaFile(String filename) throws IOException {
        Properties property = LhaProperty.getProperties();
        RandomAccessFile file = new RandomAccessFile(filename, "r");

        this.constructerHelper(file, property, false);
    }

    /**
     * Constructs an {@link LhaFile} that reads archive data from the file specified by {@code filename}.
     *
     * @param filename name of LHA archive file
     * @param property properties containing generator expressions of decoders corresponding to each compression format
     * @throws IOException                  if an I/O error occurs
     * @throws FileNotFoundException        if file is not found
     * @throws UnsupportedEncodingException if the encoding name obtained by {@code property.getProperty("lha.encoding")} is not supported
     * @throws SecurityException            if security manager denies read access to the file
     * @see LhaProperty
     */
    public LhaFile(String filename, Properties property) throws IOException {
        RandomAccessFile file = new RandomAccessFile(filename, "r");

        this.constructerHelper(file, property, false);
    }

    /**
     * Constructs an {@link LhaFile} that reads archive data from the file specified by {@code filename}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of decoders corresponding to each compression format.
     *
     * @param filename LHA archive file
     * @throws IOException           if an I/O error occurs
     * @throws FileNotFoundException if file is not found
     * @throws SecurityException     if security manager denies read access to the file
     * @see LhaProperty#getProperties()
     */
    public LhaFile(File filename) throws IOException {
        Properties property = LhaProperty.getProperties();
        RandomAccessFile file = new RandomAccessFile(filename, "r");

        this.constructerHelper(file, property, false);
    }

    /**
     * Constructs an {@link LhaFile} that reads archive data from the file specified by {@code filename}.
     *
     * @param filename LHA archive file
     * @param property properties containing generator expressions of decoders corresponding to each compression format
     * @throws IOException                  if an I/O error occurs
     * @throws FileNotFoundException        if file is not found
     * @throws UnsupportedEncodingException if the encoding name obtained by {@code property.getProperty("lha.encoding")} is not supported
     * @throws SecurityException            if security manager denies read access to the file
     * @see LhaProperty
     */
    public LhaFile(File filename, Properties property) throws IOException {
        RandomAccessFile file = new RandomAccessFile(filename, "r");

        this.constructerHelper(file, property, false);
    }

    /**
     * Constructs an {@link LhaFile} that reads archive data from the file specified by {@code file}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of decoders corresponding to each compression format.
     *
     * @param file LHA archive file
     * @throws IOException           if an I/O error occurs
     * @throws FileNotFoundException if file is not found
     * @throws SecurityException     if security manager denies read access to the file
     * @see LhaProperty#getProperties()
     */
    public LhaFile(RandomAccessFile file) throws IOException {
        Properties property = LhaProperty.getProperties();

        this.constructerHelper(file, property, false);
    }

    /**
     * Constructs an {@link LhaFile} that reads archive data from the file specified by {@code file}.<br>
     * Properties obtained with {@link LhaProperty#getProperties()} are used as properties having generator expressions of decoders corresponding to each compression format.
     *
     * @param file       LHA archive file
     * @param rescueMode if {@code true}, searches for entries in recovery mode to recover data from corrupted archives
     * @throws IOException           if an I/O error occurs
     * @throws FileNotFoundException if file is not found
     * @throws SecurityException     if security manager denies read access to the file
     * @see LhaProperty#getProperties()
     */
    public LhaFile(RandomAccessFile file, boolean rescueMode) throws IOException {
        Properties property = LhaProperty.getProperties();

        this.constructerHelper(file, property, rescueMode);
    }

    /**
     * Constructs an {@link LhaFile} that reads archive data from the file specified by {@code file}.
     *
     * @param file     LHA archive file
     * @param property properties containing generator expressions of decoders corresponding to each compression format
     * @throws IOException           if an I/O error occurs
     * @throws FileNotFoundException if file is not found
     * @throws SecurityException     if security manager denies read access to the file
     * @see LhaProperty
     */
    public LhaFile(RandomAccessFile file, Properties property) throws IOException {

        this.constructerHelper(file, property, false);
    }

    /**
     * Constructs an {@link LhaFile} that reads archive data from the file specified by {@code file}.
     *
     * @param file       LHA archive file
     * @param property   properties containing generator expressions of decoders corresponding to each compression format
     * @param rescueMode if {@code true}, searches for entries in recovery mode to recover data from corrupted archives
     * @throws IOException           if an I/O error occurs
     * @throws FileNotFoundException if file is not found
     * @throws SecurityException     if security manager denies read access to the file
     * @see LhaProperty
     */
    public LhaFile(RandomAccessFile file, Properties property, boolean rescueMode) throws IOException {

        this.constructerHelper(file, property, rescueMode);
    }

    /**
     * Scans {@code file} and constructs entry information.
     *
     * @param file       LHA archive file
     * @param property   properties containing generator expressions of decoders corresponding to each compression format
     * @param rescueMode if {@code true}, searches for entries in recovery mode to recover data from corrupted archives
     * @throws IOException                  if an I/O error occurs
     * @throws UnsupportedEncodingException if encode is not supported
     */
    private void constructerHelper(RandomAccessFile file, Properties property, boolean rescueMode) throws IOException {

        this.headers = new Vector<>();
        this.entryPoint = new Vector<>();

        file.seek(0);
        CachedRandomAccessFileInputStream archive = new CachedRandomAccessFileInputStream(file);

        byte[] HeaderData = LhaHeader.getFirstHeaderData(archive);
        while (null != HeaderData) {
            LhaHeader header = LhaHeader.createInstance(HeaderData, property);
            headers.add(header);
            entryPoint.add(archive.position());

            if (!rescueMode) {
                archive.skip(header.getCompressedSize());
                HeaderData = LhaHeader.getNextHeaderData(archive);
            } else {
                HeaderData = LhaHeader.getFirstHeaderData(archive);
            }
        }
        archive.close();

        this.hash = new Hashtable<>();
        this.duplicate = new Vector<>();
        for (int i = 0; i < this.headers.size(); i++) {
            LhaHeader header = headers.get(i);

            if (!this.hash.containsKey(header.getPath())) {
                this.hash.put(header.getPath(), i);
            } else {
                this.duplicate.add(i);
            }
        }

        this.archive = file;
        this.property = (Properties) property.clone();
    }

    /**
     * Gets an input stream to read contents of the entry specified by {@code header} while decompressing.
     *
     * @param header header
     * @return input stream to read contents of entry with header specified by {@code header},<br>
     * or {@code null} if entry is not found
     */
    public InputStream getInputStream(LhaHeader header) {
        int index = this.getIndex(header);
        if (0 <= index) {
            long start = this.entryPoint.get(index);
            long len = header.getCompressedSize();
            InputStream in = new RandomAccessFileInputStream(start, len);

            return CompressMethod.connectDecoder(in, header.getCompressMethod(), this.property, header.getOriginalSize());
        } else {
            return null;
        }
    }

    /**
     * Gets an input stream to read contents of the entry with name specified by {@code name} while decompressing.
     *
     * @param name name of entry
     * @return input stream to read contents of entry with name specified by {@code name} while decompressing,<br>
     * or {@code null} if entry is not found
     */
    public InputStream getInputStream(String name) {
        if (this.hash.containsKey(name)) {
            int index = this.hash.get(name);
            LhaHeader header = this.headers.get(index);
            long start = this.entryPoint.get(index);
            long len = header.getCompressedSize();
            InputStream in = new RandomAccessFileInputStream(start, len);

            return CompressMethod.connectDecoder(in, header.getCompressMethod(), this.property, header.getOriginalSize());
        } else {
            return null;
        }
    }

    /**
     * Returns an input stream to read contents of the entry specified by {@code header} without decompressing.
     *
     * @param header header
     * @return input stream to read contents of entry specified by {@code header} without decompressing,<br>
     * or {@code null} if entry is not found
     */
    public InputStream getInputStreamWithoutExtract(LhaHeader header) {
        int index = this.getIndex(header);
        if (0 <= index) {
            long start = this.entryPoint.get(index);
            long len = header.getCompressedSize();

            return new RandomAccessFileInputStream(start, len);
        } else {
            return null;
        }
    }

    /**
     * Returns an input stream to read contents of the entry with name specified by {@code name} without decompressing.
     *
     * @param name name of entry
     * @return input stream to read contents of entry with name specified by {@code name} without decompressing,<br>
     * or {@code null} if entry is not found
     */
    public InputStream getInputStreamWithoutExtract(String name) {
        if (this.hash.containsKey(name)) {
            int index = this.hash.get(name);
            LhaHeader header = this.headers.get(index);
            long start = this.entryPoint.get(index);
            long len = header.getCompressedSize();

            return new RandomAccessFileInputStream(start, len);
        } else {
            return null;
        }
    }

    /**
     * Gets the number of entries in this {@link LhaFile}.
     *
     * @return number of entries in file
     */
    public int size() {
        return this.headers.size();
    }

    /**
     * Gets an enumeration of {@link LhaHeader} of entries in this {@link LhaFile}.
     *
     * @return enumeration of {@link LhaHeader}
     * @throws IllegalStateException if {@link LhaFile} has been closed by {@link #close()}
     */
    public Enumeration<?> entries() {
        if (this.archive != null) {
            return new HeaderEnumeration();
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Gets an array enumerating entries in file.
     *
     * @return array enumerating entries in file
     */
    public LhaHeader[] getEntries() {
        LhaHeader[] headers = new LhaHeader[this.headers.size()];

        for (int i = 0; i < this.headers.size(); i++) {
            headers[i] = (LhaHeader) this.headers.get(i).clone();
        }

        return headers;
    }

    /**
     * Closes this LHA archive file.
     * At that time, all {@link InputStream}s issued by this {@link LhaFile} are forcibly closed.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        this.archive.close();
        this.archive = null;
        this.lastAccessObject = null;
        this.headers = null;
        this.entryPoint = null;
        this.hash = null;
        this.property = null;
        this.duplicate = null;
    }

    /**
     * Gets index of {@code target} in {@code headers}.
     *
     * @param target header
     * @return index of {@code target} in {@code headers},
     * or -1 if {@code target} is not in {@code headers}
     */
    private int getIndex(LhaHeader target) {
        int index = this.hash.get(target.getPath());

        LhaHeader header = this.headers.get(index);
        if (!LhaFile.equal(header, target)) {
            boolean match = false;
            for (int i = 0; i < this.duplicate.size() && !match; i++) {
                index = this.duplicate.get(i);
                header = this.headers.get(index);

                if (LhaFile.equal(header, target)) {
                    match = true;
                }
            }

            if (match) {
                return index;
            } else {
                return -1;
            }
        } else {
            return index;
        }
    }

    /**
     * Checks whether two {@link LhaHeader}s, {@code header1} and {@code header2}, are equivalent.
     *
     * @param header1 test target header 1
     * @param header2 test target header 2
     * @return {@code true} if {@code header1} and {@code header2} are equivalent, {@code false} otherwise
     */
    private static boolean equal(LhaHeader header1, LhaHeader header2) {
        return header1.getPath().equals(header2.getPath()) && header1.getCompressMethod().equals(header2.getCompressMethod())
                && header1.getLastModified().equals(header2.getLastModified())
                && header1.getCompressedSize() == header2.getCompressedSize()
                && header1.getOriginalSize() == header2.getOriginalSize() && header1.getCRC() == header2.getCRC()
                && header1.getOSID() == header2.getOSID() && header1.getHeaderLevel() == header2.getHeaderLevel();
    }

    /**
     * {@link InputStream} that obtains data within a certain section of {@link LhaFile}'s {@code archive}.
     * Includes synchronization processing to process multiple entries concurrently.
     */
    private class RandomAccessFileInputStream extends InputStream {

        /**
         * Current processing position in archive
         */
        private long position;

        /**
         * Read limit of this {@link InputStream} in archive
         */
        private final long end;

        /**
         * Mark position in archive
         */
        private long markPosition;

        /**
         * Constructor.
         *
         * @param start read start position
         * @param size  data size
         */
        public RandomAccessFileInputStream(long start, long size) {
            this.position = start;
            this.end = start + size;
            this.markPosition = -1;
        }

        /**
         * Reads 1 byte of data from current processing position in archive.
         *
         * @return 1 byte of read data,<br>
         * or -1 if read limit has already been reached
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read() throws IOException {
            synchronized (LhaFile.this.archive) {
                if (this.position < this.end) {
                    if (LhaFile.this.lastAccessObject != this)
                        LhaFile.this.archive.seek(this.position);

                    int data = LhaFile.this.archive.read();
                    if (0 <= data)
                        this.position++;
                    return data;
                } else {
                    return -1;
                }
            }
        }

        /**
         * Reads data from current processing position in archive to fill {@code buffer}.
         *
         * @param buffer buffer to store read data
         * @return number of bytes read,<br>
         * or -1 if read limit has already been reached
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read(byte[] buffer) throws IOException {
            return this.read(buffer, 0, buffer.length);
        }

        /**
         * Reads {@code length} bytes of data from current processing position in archive into region starting at {@code index} in {@code buffer}.
         *
         * @param buffer buffer to store read data
         * @param index  read start position in {@code buffer}
         * @param length number of bytes to read
         * @return number of bytes read,<br>
         * or -1 if read limit has already been reached
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read(byte[] buffer, int index, int length) throws IOException {
            synchronized (LhaFile.this.archive) {
                if (this.position < this.end) {
                    if (LhaFile.this.lastAccessObject != this) {
                        LhaFile.this.archive.seek(this.position);
                        LhaFile.this.lastAccessObject = this;
                    }

                    length = (int) Math.min(this.end - this.position, length);
                    length = LhaFile.this.archive.read(buffer, index, length);
                    if (0 <= length)
                        this.position += length;
                    return length;
                } else {
                    return -1;
                }
            }
        }

        /**
         * Skips {@code length} bytes of data.
         *
         * @param length number of bytes to skip
         * @return actual number of bytes skipped
         */
        @Override
        public long skip(long length) {
            synchronized (LhaFile.this.archive) {
                long skiplen = Math.min(this.end - this.position, length);
                this.position += skiplen;

                if (LhaFile.this.lastAccessObject == this)
                    LhaFile.this.lastAccessObject = null;

                return skiplen;
            }
        }

        /**
         * Returns whether this object supports mark/reset.
         *
         * @return this object supports mark/reset.<br>
         * Always {@code true}.
         */
        @Override
        public boolean markSupported() {
            return true;
        }

        /**
         * Sets a mark at current processing position so that the next reset can return to current processing position.
         *
         * @param readLimit valid limit of mark. Has no meaning in this object.
         */
        @Override
        public void mark(int readLimit) {
            this.markPosition = this.position;
        }

        /**
         * Returns to the last marked processing position.
         *
         * @throws IOException if not marked
         */
        @Override
        public void reset() throws IOException {
            synchronized (LhaFile.this.archive) {
                if (0 <= this.markPosition) {
                    this.position = this.markPosition;
                } else {
                    throw new IOException("not marked");
                }

                if (LhaFile.this.lastAccessObject == this)
                    LhaFile.this.lastAccessObject = null;
            }
        }

        /**
         * Gets the number of bytes that can be read from the connected input stream without blocking.<br>
         * In {@link RandomAccessFileInputStream}, reading always involves access to {@link RandomAccessFile}, so this method always returns 0.
         *
         * @return always 0
         */
        @Override
        public int available() {
            return 0;
        }

        /**
         * Closes this input stream and releases all resources used.<br>
         * This method does nothing.
         */
        @Override
        public void close() {
        }
    }

    /**
     * {@link InputStream} for header search on {@link RandomAccessFile}.<br>
     * Speed is improved by not performing synchronization and using buffering.
     */
    private static class CachedRandomAccessFileInputStream extends InputStream {

        /**
         * {@link RandomAccessFile} providing data
         */
        private RandomAccessFile archive;

        /**
         * Cache for storing data
         */
        private byte[] cache;

        /**
         * Current processing position in {@code cache}
         */
        private int cachePosition;

        /**
         * Read limit position in {@code cache}
         */
        private int cacheLimit;

        /**
         * Indicates whether mark position is within cache range.
         * Set to {@code true} when marked, and set to {@code false} when the next read from {@code in} into cache occurs.
         */
        private boolean markPositionIsInCache;

        /** Backup for cache */
        private byte[] markCache;

        /** Backup for cachePosition */
        private int markCachePosition;

        /** Backup for cacheLimit */
        private int markCacheLimit;

        /** Backup for position */
        private long markPosition;

        /**
         * Constructs a {@link CachedRandomAccessFileInputStream} speeded up using cache.
         *
         * @param file {@link RandomAccessFile} providing data
         */
        public CachedRandomAccessFileInputStream(RandomAccessFile file) {
            this.archive = file;

            this.cache = new byte[1024];
            this.cachePosition = 0;
            this.cacheLimit = 0;
        }

        /**
         * Reads 1 byte of data from current processing position in archive.
         *
         * @return 1 byte of read data,<br>
         * or -1 if read limit has already been reached
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read() throws IOException {
            if (this.cachePosition < this.cacheLimit) {
                return this.cache[this.cachePosition++] & 0xFF;
            } else {
                this.fillCache();

                if (this.cachePosition < this.cacheLimit) {
                    return this.cache[this.cachePosition++] & 0xFF;
                } else {
                    return -1;
                }
            }
        }

        /**
         * Reads data from current processing position in archive to fill {@code buffer}.
         *
         * @param buffer buffer to store read data
         * @return number of bytes read,<br>
         * or -1 if read limit has already been reached
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read(byte[] buffer) throws IOException {
            return this.read(buffer, 0, buffer.length);
        }

        /**
         * Reads {@code length} bytes of data from current processing position in archive into region starting at {@code index} in {@code buffer}.
         *
         * @param buffer buffer to store read data
         * @param index  read start position in {@code buffer}
         * @param length number of bytes to read
         * @return number of bytes read,<br>
         * or -1 if read limit has already been reached
         * @throws IOException if an I/O error occurs
         */
        @Override
        public int read(byte[] buffer, int index, int length) throws IOException {
            int requested = length;

            while (0 < length) {
                if (this.cacheLimit <= this.cachePosition) {
                    this.fillCache();
                    if (this.cacheLimit <= this.cachePosition) {
                        if (requested == length) {
                            return -1;
                        } else {
                            break;
                        }
                    }
                }

                int copylen = Math.min(length, this.cacheLimit - this.cachePosition);
                System.arraycopy(this.cache, this.cachePosition, buffer, index, copylen);

                index += copylen;
                length -= copylen;
                this.cachePosition += copylen;
            }
            return requested - length;
        }

        /**
         * Skips {@code length} bytes of data.
         *
         * @param length number of bytes to skip
         * @return actual number of bytes skipped
         */
        @Override
        public long skip(long length) throws IOException {
            long requested = length;

            if (this.cachePosition < this.cacheLimit) {
                long avail = (long) this.cacheLimit - this.cachePosition;
                long skiplen = Math.min(length, avail);

                length -= skiplen;
                this.cachePosition += (int) skiplen;
            }

            if (0 < length) {
                long avail = this.archive.length() - this.archive.getFilePointer();
                long skiplen = Math.min(avail, length);

                length -= skiplen;
                archive.seek(archive.getFilePointer() + skiplen);
            }

            return requested - length;
        }

        /**
         * Returns whether this object supports mark/reset.
         *
         * @return this object supports mark/reset.<br>
         * Always {@code true}.
         */
        @Override
        public boolean markSupported() {
            return true;
        }

        /**
         * Sets a mark at current processing position so that the next reset can return to current processing position.
         *
         * @param readLimit valid limit of mark. Has no meaning in this object.
         */
        @Override
        public void mark(int readLimit) {
            try {
                this.markPosition = this.archive.getFilePointer();
            } catch (IOException exception) {
                throw new Error("caught IOException( " + exception.getMessage() + " ) in mark()");
            }

            if (this.markCache == null) {
                this.markCache = this.cache.clone();
            } else {
                System.arraycopy(this.cache, 0, this.markCache, 0, this.cacheLimit);
            }

            this.markCacheLimit = this.cacheLimit;
            this.markCachePosition = this.cachePosition;
            this.markPositionIsInCache = true;
        }

        /**
         * Returns to the last marked processing position.
         *
         * @throws IOException if not marked
         */
        @Override
        public void reset() throws IOException {
            if (this.markPositionIsInCache) {
                this.cachePosition = this.markCachePosition;
            } else if (this.markCache == null) { // This condition indicates it is not yet marked. Utilizes markCache being set to null in constructor.
                throw new IOException("not marked.");
            } else {
                // If in cannot reset(), expecting the first line this.in.reset() to throw IOException.
                this.archive.seek(this.markPosition);

                System.arraycopy(this.markCache, 0, this.cache, 0, this.markCacheLimit);
                this.cacheLimit = this.markCacheLimit;
                this.cachePosition = this.markCachePosition;
            }
        }

        /**
         * Gets the number of bytes that can be read from the connected input stream without blocking.
         *
         * @return number of bytes that can be read without blocking
         */
        @Override
        public int available() {
            return this.cacheLimit - this.cachePosition;
        }

        /**
         * Closes this input stream and releases all resources used.
         */
        @Override
        public void close() {
            this.archive = null;

            this.cache = null;
            this.cachePosition = 0;
            this.cacheLimit = 0;

            this.markPositionIsInCache = false;
            this.markCache = null;
            this.markCachePosition = 0;
            this.markCacheLimit = 0;
            this.markPosition = 0;
        }

        /**
         * Gets current read position starting from the beginning of the file.
         *
         * @return current read position
         */
        public long position() throws IOException {
            long position = this.archive.getFilePointer();

            position -= this.cacheLimit - this.cachePosition;

            return position;
        }

        /**
         * Called when necessary to replenish data in cache buffer and ensure data always exists in cache buffer.<br>
         * If read up to EndOfStream, indicates this by not replenishing data.
         *
         * @throws IOException if an I/O error occurs
         */
        private void fillCache() throws IOException {
            this.markPositionIsInCache = false;
            this.cacheLimit = 0;
            this.cachePosition = 0;

            // Read data into cache
            int read = 0;
            while (0 <= read && this.cacheLimit < this.cache.length) {
                read = this.archive.read(this.cache, this.cacheLimit, this.cache.length - this.cacheLimit);

                if (0 < read)
                    this.cacheLimit += read;
            }
        }
    }

    /**
     * Enumeration returning all {@link LhaHeader}s in {@link LhaFile}.
     */
    private class HeaderEnumeration implements Enumeration<Object> {

        /**
         * Current processing position
         */
        private int index;

        /**
         * Constructs an enumeration returning all {@link LhaHeader}s in {@link LhaFile}.
         */
        public HeaderEnumeration() {
            this.index = 0;
        }

        /**
         * Gets whether elements still remain in enumeration.
         *
         * @return {@code true} if elements still remain in enumeration,
         * or {@code false} if none remain
         * @throws IllegalStateException if parent {@link LhaFile} is closed
         */
        @Override
        public boolean hasMoreElements() {
            if (LhaFile.this.archive != null) {
                return this.index < LhaFile.this.headers.size();
            } else {
                throw new IllegalStateException("file might be closed");
            }
        }

        /**
         * Gets the next element of enumeration.
         *
         * @return next element of enumeration
         * @throws IllegalStateException  if parent {@link LhaFile} is closed
         * @throws NoSuchElementException if no elements exist in enumeration
         */
        @Override
        public Object nextElement() {
            if (LhaFile.this.archive != null) {
                if (this.index < LhaFile.this.headers.size()) {
                    return LhaFile.this.headers.get(this.index++).clone();
                } else {
                    throw new NoSuchElementException();
                }
            } else {
                throw new IllegalStateException("file might be closed");
            }
        }
    }
}
