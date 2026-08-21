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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Properties;


/**
 * Utility class for outputting compressed data to a connected {@link RandomAccessFile}.<br>
 * Designed to have an interface similar to {@link java.util.zip.ZipOutputStream}.<br>
 * Processing when compression fails (when compressed size exceeds original size)
 * must be handled manually.
 * The following shows such code.
 *
 * <pre>
 * LhaCompressFiles( String arcfile, File[] files ){
 *   LhaImmediateOutputStream lio = new LhaImmediateOutputStream( arcfile );
 *
 *   for( int i = 0 ; i &lt; files.length ; i++ ){
 *     RandomAccessFile raf = new RandomAccessFile( files[i] );
 *     LhaHeader header = new LhaHeader( files[i].getName() );
 *     header.setLastModified( new Date( files.lastModified() ) );
 *     header.setOriginalSize( files.length() );
 *     byte[] buffer  = new byte[8192];
 *     int    length;
 *
 *     while( 0 &lt;= ( length = raf.read( buffer ) ) ){
 *         lio.write( buffer, 0, length );
 *     }
 * <strong>
 *     if( !lio.closeEntry() ){
 *       header.setCompressMethod( CompressMethod.LH0 );
 *       lio.putNextEntry( lhaheader );
 *       raf.seek( 0 );
 *       while( 0 &lt;= ( length = raf.read( buffer ) ) ){
 *           lio.write( buffer, 0, length );
 *       }
 *       lio.closeEntry();
 *     }
 * </strong>
 *   lio.close();
 * }
 * </pre>
 * <p>
 * When implementing progress reporting, hiding such processing inside the class will cause progress reporting
 * to become unresponsive for several seconds or even tens of minutes (for example, when dealing with gigabyte-class data).
 * This class is designed to avoid such situations that occur in {@link LhaRetainedOutputStream}.<br>
 * Also, in JDK 1.1 and earlier, {@link RandomAccessFile} does not have {@code setLength}, so the file size cannot be truncated
 * even if there is other data following the archive data.<br>
 * This problem can be avoided by always opening a new file of size 0.
 *
 * <pre>
 * -- revision history --
 * $Log: LhaImmediateOutputStream.java,v $
 * Revision 1.2  2002/12/11 02:25:06  dangan
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
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.2 $
 */
public class LhaImmediateOutputStream extends OutputStream {

    private static final Logger logger = System.getLogger(LhaImmediateOutputStream.class.getName());

    /**
     * Archive file
     */
    private RandomAccessFile archive;

    /**
     * Compression output stream
     */
    private OutputStream out;

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
    private long headerPos;

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
    public LhaImmediateOutputStream(String filename) throws FileNotFoundException {

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
    public LhaImmediateOutputStream(String filename, Properties property) throws FileNotFoundException {

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
    public LhaImmediateOutputStream(File filename) throws IOException {

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
    public LhaImmediateOutputStream(File filename, Properties property) throws IOException {

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
     * @param file instance of {@link RandomAccessFile}.
     *             Must satisfy:
     *             <ul>
     *             <li>Must not be already closed.</li>
     *             <li>Must be instantiated with mode "rw" in constructor to enable reading and writing.</li>
     *             </ul>
     * @see LhaProperty#getProperties()
     */
    public LhaImmediateOutputStream(RandomAccessFile file) {

        if (file != null) {
            Properties property = LhaProperty.getProperties();
            this.constructerHelper(file, property);
        } else {
            throw new NullPointerException("out");
        }
    }

    /**
     * Constructs an {@link OutputStream} that outputs compressed data to {@code file}.<br>
     *
     * @param file     instance of {@link RandomAccessFile}.
     *                 Must satisfy:
     *                 <ul>
     *                 <li>Must not be already closed.</li>
     *                 <li>Must be instantiated with mode "rw" in constructor to enable reading and writing.</li>
     *                 </ul>
     * @param property properties containing generator expressions of encoders corresponding to each compression format
     * @see LhaProperty
     */
    public LhaImmediateOutputStream(RandomAccessFile file, Properties property) {

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
     * @param file     instance of {@link RandomAccessFile}.
     *                 Must satisfy:
     *                 <ul>
     *                 <li>Must not be already closed.</li>
     *                 <li>Must be instantiated with mode "rw" in constructor to enable reading and writing.</li>
     *                 </ul>
     * @param property properties containing generator expressions of encoders corresponding to each compression format
     */
    private void constructerHelper(RandomAccessFile file, Properties property) {

        this.archive = file;

        this.out = null;
        this.header = null;
        this.headerPos = -1;
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
            logger.log(Level.ERROR, error.getMessage(), error);
        }
        this.archive.close();
        this.archive = null;

        this.crc = null;
        this.property = null;
        this.encoding = null;
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
     *                                  <li>if {@code header.getOriginalSize()} returns {@link LhaHeader#UNKNOWN}</li>
     *                                  <li>if {@code header.getCompressedSize()} returns {@link LhaHeader#UNKNOWN}</li>
     *                                  <li>if {@code header.getCRC()} returns {@link LhaHeader#UNKNOWN}</li>
     *                                  </ol>
     * @throws IllegalStateException    if previous entry has not been closed by {@link #closeEntry()}
     */
    public void putNextEntryAlreadyCompressed(LhaHeader header) throws IOException {

        if (this.out == null) {

            if (header.getOriginalSize() != LhaHeader.UNKNOWN && header.getCompressedSize() != LhaHeader.UNKNOWN
                    && header.getCRC() != LhaHeader.UNKNOWN) {

                this.headerPos = this.archive.getFilePointer();

                this.encoding = this.property.getProperty("lha.encoding");
                if (this.encoding == null) {
                    this.encoding = LhaProperty.getProperty("lha.encoding");
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

        } else {
            throw new IllegalStateException("entry is not closed.");
        }
    }

    /**
     * Sets up the stream to write an entry not yet compressed.
     * Even if {@code CompressedSize} or {@code CRC} is specified in {@code header}, they are ignored.
     * The {@code header} passed to this method must have the accurate original size specified using {@link LhaHeader#setOriginalSize(long)}.
     *
     * @param header instance of {@link LhaHeader} having information about the entry to write
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if {@code header.getOriginalSize()} returns {@link LhaHeader#UNKNOWN}
     * @throws IllegalStateException    if previous entry has not been closed by {@link #closeEntry()}
     */
    public void putNextEntryNotYetCompressed(LhaHeader header) throws IOException {

        if (out == null) {

            if (header.getOriginalSize() != LhaHeader.UNKNOWN) {

                this.crc.reset();
                this.headerPos = this.archive.getFilePointer();
                this.header = (LhaHeader) header.clone();
                this.header.setCompressedSize(0);
                this.header.setCRC(0);

                this.encoding = this.property.getProperty("lha.encoding");
                if (this.encoding == null) {
                    this.encoding = LhaProperty.getProperty("lha.encoding");
                }

                this.archive.write(this.header.getBytes(encoding));
                this.out = new RandomAccessFileOutputStream(this.archive, header.getOriginalSize());
                this.out = CompressMethod.connectEncoder(this.out, header.getCompressMethod(), this.property);

            } else {
                throw new IllegalArgumentException("OriginalSize must not \"LhaHeader.UNKNOWN\".");
            }

        } else {
            throw new IllegalStateException("entry is not closed.");
        }
    }

    /**
     * Closes the currently outputting entry and prepares the stream to output the next entry.<br>
     * When closing an entry opened with {@link #putNextEntryNotYetCompressed(LhaHeader)},
     * if compression fails (compressed size exceeds original size), this method deletes the entire entry from destination {@link RandomAccessFile}.<br>
     * Since this deletion simply rewinds the file pointer to the entry start position, in JDK 1.1 and earlier where {@link RandomAccessFile} lacks {@code setLength()},
     * unless the entry is re-output uncompressed (or with another compression method), incomplete data from the failed compression will remain after the archive end.
     *
     * @return {@code true} if entry was output, {@code false} if entry was deleted because compressed size became larger than before compression.
     * Also returns {@code true} if there was no entry currently being processed.
     * @throws IOException if an I/O error occurs
     */
    public boolean closeEntry() throws IOException {
        if (this.out != null) {

            this.out.close();
            if (this.header != null) {

                long pos = this.archive.getFilePointer();
                long size = (pos - this.headerPos - this.header.getBytes(this.encoding).length);

                this.header.setCompressedSize(size);
                if (this.header.getCRC() != LhaHeader.NO_CRC) {
                    this.header.setCRC((int) this.crc.getValue());
                }

                this.archive.seek(this.headerPos);
                if (this.header.getCompressMethod().equals(CompressMethod.LH0)
                        || this.header.getCompressMethod().equals(CompressMethod.LHD)
                        || this.header.getCompressMethod().equals(CompressMethod.LZ4)
                        || this.header.getCompressedSize() < this.header.getOriginalSize()) {

                    this.archive.write(this.header.getBytes(this.encoding));
                    this.archive.seek(pos);
                    this.header = null;
                    this.out = null;
                    return true;
                } else {
                    this.header = null;
                    this.out = null;
                    return false;
                }
            } else {
                this.out = null;
                return true;
            }
        } else {
            return true;
        }
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
            this.pos = this.archive.getFilePointer();
            this.limit = this.pos + length;
        }

        /**
         * Writes 1 byte to connected {@link RandomAccessFile}.<br>
         * Does nothing if attempting to write beyond the limit passed to constructor.
         *
         * @param data 1 byte of data to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(int data) throws IOException {
            if (this.pos < this.limit) {
                this.pos++;
                this.archive.write(data);
            }
        }

        /**
         * Writes all contents of {@code buffer} to connected {@link RandomAccessFile}.
         * Does nothing if attempting to write beyond the limit passed to constructor.
         *
         * @param buffer byte array containing data to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(byte[] buffer) throws IOException {
            this.write(buffer, 0, buffer.length);
        }

        /**
         * Writes {@code length} bytes of contents of {@code buffer} starting from {@code index} to connected {@link RandomAccessFile}.
         * Does nothing if attempting to write beyond the limit passed to constructor.
         *
         * @param buffer byte array containing data to write
         * @param index  start position of data to write in {@code buffer}
         * @param length amount of data to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(byte[] buffer, int index, int length) throws IOException {

            if (this.limit < this.pos + length) {
                length = (int) Math.max(this.limit - this.pos, 0);
            }
            this.archive.write(buffer, index, length);
            this.pos += length;
        }

        /**
         * Closes this stream and releases resources used.
         */
        @Override
        public void close() {
            this.archive = null;
        }
    }
}
