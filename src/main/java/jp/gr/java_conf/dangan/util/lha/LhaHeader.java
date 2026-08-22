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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import jp.gr.java_conf.dangan.io.LittleEndian;
import jp.gr.java_conf.dangan.util.MsdosDate;


/**
 * Handles LHA headers.<br>
 * This class is similar to {@link java.util.zip.ZipEntry} in the java.util.zip package,
 * but differs in that it has utility functions for header input and output.<br>
 * Note that this class is written to perform checks at {@link #getBytes()} time
 * that would better be done in set methods.
 *
 * <pre>
 * -- revision history --
 * $Log: LhaHeader.java,v $
 * Revision 1.2.2.3  2005/05/03 07:50:30  dangan
 * [bug fix]
 *     In exportLevel1Header(), skip size was not checked.
 *
 * Revision 1.2.2.2  2005/02/02 00:57:46  dangan
 * [bug fix]
 *     Fixed issue where files with size greater than or equal to 31-bit value
 *     were not handled correctly because file size was read as int in importLevelXHeader(byte[], String).
 *
 * Revision 1.2.2.1  2003/07/20 13:19:21  dangan
 * [bug fix]
 *     In exportDirNameExtHeader(String), src and dest order in System.arraycopy was reversed.
 *
 * Revision 1.2  2002/12/08 00:00:00  dangan
 * [maintenance]
 *     Updated to reflect the class name change from LhaConstants to CompressMethod.
 *
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [improvement]
 *     Supported 64-bit file size header.
 * [change]
 *     Changed to use LhaProperty.encoding instead of LhaUtil.DefaultEncoding.
 *     Renamed getNextHeaderData() to getFirstHeaderData().
 *     New getNextHeaderData() returns null if no header is found at the invoked position.
 *     Added createInstance() for callers extending LhaHeader.
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     setDate( null ) was allowed.
 *     setCompressMethod( null ) was allowed.
 *     In exportLevel2,3Header, Date values outside the 32-bit time_t range (including negative values) were allowed.
 * [change]
 *     In exportHeader, changed to throw IllegalStateException if header level is not 0, 1, 2, or 3.
 * [maintenance]
 *     Source cleanup.
 *     Eliminated tabs.
 *     Updated license text.
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.2.2.3 $
 */
public class LhaHeader implements Cloneable {

    /**
     * Value indicating unknown.
     * When {@link #getCRC()}, {@link #getCompressedSize()}, or {@link #getOriginalSize()} returns this value,
     * it indicates that the value is unknown because it has not been processed yet.
     */
    public static final int UNKNOWN = -1;

    /**
     * Value indicating there is no CRC value.
     * Means there is no CRC value in a level 0 header.
     */
    public static final int NO_CRC = -2;

    /**
     * Size before compression.
     * -1 means the size is unknown because it is before processing.
     */
    private long originalSize;

    /**
     * Last modified date and time.
     * Last modified date and time of the compressed file.
     */
    private Date lastModified;

    /**
     * Path name.
     * {@link File#separator} is used as the path delimiter.
     */
    private String path;

    /**
     * CRC16 value.
     * -1 means CRC16 value is unknown because it is before processing.
     * -2 means there is no CRC16 value in a level 0 header.
     */
    private int crc;

    /**
     * Compression method string.
     */
    private String method;

    /**
     * Size after compression.
     * -1 means the size is unknown because it is before processing.
     */
    private long compressedSize;

    /**
     * Header level.
     * One of 0, 1, 2, 3.
     */
    private int headerLevel;

    /**
     * OS that created the header.
     */
    private byte osid;

    /**
     * Stores extra data in the base header if present in level 0 or level 1 headers.
     */
    private byte[] extraData;

    /**
     * Stores DOS file attributes in level 0 headers.
     */
    private byte level0DosAttribute;

    /**
     * Stores extended headers having information not read by {@link LhaHeader}.
     */
    private Vector<byte[]> extraExtHeaders;

    /**
     * Initializes values of {@link LhaHeader}.
     */
    private LhaHeader() {
        this.method = CompressMethod.LH5;
        this.originalSize = LhaHeader.UNKNOWN;
        this.compressedSize = LhaHeader.UNKNOWN;
        this.lastModified = null;
        this.headerLevel = 2;
        this.path = "";
        this.crc = LhaHeader.UNKNOWN;
        this.osid = (byte) 'J';
        this.extraData = null;
        this.level0DosAttribute = 0x20;
        this.extraExtHeaders = null;
    }

    /**
     * Constructs an instance of {@link LhaHeader} with name {@code path}.<br>
     * Use {@link File#separator} as the path delimiter.<br>
     * If {@code path} is terminated by the path delimiter,
     * it is interpreted as a directory.
     *
     * @param path path name
     * @throws IllegalArgumentException if {@code path} is {@code null} or empty string
     */
    public LhaHeader(String path) {
        this(path, new Date(System.currentTimeMillis()));
    }

    /**
     * Constructs an instance of {@link LhaHeader} with name {@code path} and last modified date {@code date}.<br>
     * Use {@link File#separator} as the path delimiter.<br>
     * If {@code path} is terminated by the path delimiter,
     * it is interpreted as a directory.
     *
     * @param path path name
     * @param date last modified date
     * @throws IllegalArgumentException if {@code path} is {@code null} or empty string, or {@code date} is {@code null}
     */
    public LhaHeader(String path, Date date) {
        this();
        if (path != null && !path.isEmpty() && date != null) {
            if (path.endsWith(File.separator)) {
                this.method = CompressMethod.LHD;
            }

            this.path = path;
            this.lastModified = date;
        } else if (path == null) {
            throw new NullPointerException("path");
        } else if (path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty.");
        } else {
            throw new NullPointerException("date");
        }
    }

    /**
     * Constructs a new instance of {@link LhaHeader} from header data.<br>
     * The encoding used is {@link LhaProperty#encoding}.
     *
     * @param HeaderData header data
     * @throws IndexOutOfBoundsException if position where data is assumed to exist is outside the range of {@code HeaderData} because header data is corrupted
     * @throws IllegalArgumentException  if header level is not 0, 1, 2, or 3, or if {@code HeaderData} is {@code null}
     */
    public LhaHeader(byte[] HeaderData) {
        this();
        if (HeaderData != null) {
            try {
                this.importHeader(HeaderData, LhaProperty.encoding);
            } catch (UnsupportedEncodingException exception) {
                throw new Error("Java Runtime Environment not support " + LhaProperty.encoding + " encoding");
            }
        } else {
            throw new NullPointerException("HeaderData");
        }
    }

    /**
     * Constructs a new instance of {@link LhaHeader} from header data.
     *
     * @param HeaderData header data
     * @param encode     encoding used to interpret string information
     * @throws IndexOutOfBoundsException    if position where data is assumed to exist is outside the range of {@code HeaderData} because header data is corrupted
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     * @throws IllegalArgumentException     if header level is not 0, 1, 2, or 3, or if {@code HeaderData} is {@code null}
     */
    public LhaHeader(byte[] HeaderData, String encode) throws UnsupportedEncodingException {
        this();
        if (HeaderData != null && encode != null) {
            this.importHeader(HeaderData, encode); // throw UnsupportedEncodingException
        } else if (HeaderData == null) {
            throw new NullPointerException("HeaderData");
        } else {
            throw new NullPointerException("encode");
        }
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException exception) { // Ignore
            throw new Error("java.lang.Object is not support clone().");
        }
    }

    /**
     * Gets the string identifying the compression method of data.
     *
     * @return compression method string
     */
    public String getCompressMethod() {
        return this.method;
    }

    /**
     * Gets the size of data before compression.
     *
     * @return size before compression. An instance created by {@link #LhaHeader(String)} or
     * {@link #LhaHeader(String, Date)} returns {@link #UNKNOWN} (-1) because the size is unknown in initial state.
     * @see LhaHeader#UNKNOWN
     */
    public long getOriginalSize() {
        return this.originalSize;
    }

    /**
     * Gets the size of data after compression.
     *
     * @return size after compression. An instance created by {@link #LhaHeader(String)} or
     * {@link #LhaHeader(String, Date)} returns {@link #UNKNOWN} (-1) because the size is unknown in initial state.
     * @see LhaHeader#UNKNOWN
     */
    public long getCompressedSize() {
        return this.compressedSize;
    }

    /**
     * Gets the last modified date and time of the data.
     *
     * @return last modified date and time of the data
     */
    public Date getLastModified() {
        return new Date(this.lastModified.getTime());
    }

    /**
     * Gets the header level of this header.
     *
     * @return header level
     */
    public int getHeaderLevel() {
        return this.headerLevel;
    }

    /**
     * Gets the name of the data, or the path name if the data is a file.<br>
     * Even though it is a path name, it must not include drive letters like A: in Windows.<br>
     * {@link File#separator} is used as the path delimiter.
     *
     * @return name of the data, or path name.
     * @see File#separator
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Gets the CRC16 value of the data.
     *
     * @return CRC16 value of the data<br>
     * An instance created by {@link #LhaHeader(String)} or
     * {@link #LhaHeader(String, Date)} returns {@link #UNKNOWN} (-1)
     * because the CRC is unknown in initial state.<br>
     * If there is no CRC16 value field in a level 0 header,
     * returns {@link #NO_CRC} (-2).
     * @see LhaHeader#UNKNOWN
     * @see LhaHeader#NO_CRC
     */
    public int getCRC() {
        return this.crc;
    }

    /**
     * Gets the identifier of the OS that created this header.
     *
     * @return OS identifier
     */
    public byte getOSID() {
        return this.osid;
    }

    /**
     * Gets extra data in the base header that may be attached in level 0 or level 1 headers.
     *
     * @return extra data
     */
    protected byte[] getExtraData() {
        return this.extraData.clone();
    }

    /**
     * Gets DOS file attributes recorded in level 0 headers.
     *
     * @return DOS file attributes
     */
    protected byte getLevel0DosAttribute() {
        return this.level0DosAttribute;
    }

    /**
     * Gets the file name extracted from the path name.
     *
     * @return file name
     */
    private String getFileName() {
        return this.path.substring(this.path.lastIndexOf(File.separatorChar) + 1);
    }

    /**
     * Gets the directory name extracted from the path name.
     *
     * @return directory name
     */
    private String getDirName() {
        return this.path.substring(0, this.path.lastIndexOf(File.separatorChar) + 1);
    }

    /**
     * Generates header data using data of this {@link LhaHeader} and returns it as a byte array.<br>
     * Default encoding is used.
     *
     * @return header data stored in a byte array
     * @throws IllegalStateException <ol>
     *                               <li>if byte array of compression method string converted by {@code encode} is not 5 bytes
     *                               <li>if file name is too long to fit in header in level 0, 1, or 2
     *                               <li>if common extended header is too large to output in level 1 or 2, hence there is no place to store header CRC
     *                               <li>if {@link #NO_CRC} (-2), a special value indicating no CRC information in level 0 headers, is set for non-level 0 headers
     *                               <li>if {@code lastModified} is outside the range that can be represented in MS-DOS format in level 0 or 1
     *                               <li>if {@code lastModified} is outside the range that can be represented in 4-byte time_t in level 2 or 3
     *                               <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code originalSize}
     *                               <li>if {@code originalSize} is negative
     *                               <li>if {@code originalSize} cannot be represented as a 4-byte value in level 0, 1, or 3
     *                               <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code compressedSize}
     *                               <li>if {@code compressedSize} is negative
     *                               <li>if {@code compressedSize} cannot be represented as a 4-byte value in level 0, 1, or 3
     *                               <li>if file size header is required because {@code originalSize} or {@code compressedSize} exceeds 4-byte value in level 2, but file size header cannot be output because other extended headers are too large
     *                               <li>if {@link #UNKNOWN} (-1), a special value indicating unknown CRC16 value, is set in CRC
     *                               <li>if header level is other than 0, 1, 2, or 3
     *                               </ol>
     */
    public byte[] getBytes() {
        try {
            return this.exportHeader(LhaProperty.encoding);
        } catch (UnsupportedEncodingException exception) {
            throw new Error("Java Runtime Environment not support " + LhaProperty.encoding + " encoding");
        }
    }

    /**
     * Generates header data using data of this {@link LhaHeader} and returns it as a byte array.
     *
     * @param encode encoding used when outputting string information
     * @return header data stored in a byte array
     * @throws IllegalStateException        <ol>
     *                                      <li>if byte array of compression method string converted by {@code encode} is not 5 bytes
     *                                      <li>if file name is too long to fit in header in level 0, 1, or 2
     *                                      <li>if common extended header is too large to output in level 1 or 2, hence there is no place to store header CRC
     *                                      <li>if {@link #NO_CRC} (-2), a special value indicating no CRC information in level 0 headers, is set for non-level 0 headers
     *                                      <li>if {@code lastModified} is outside the range that can be represented in MS-DOS format in level 0 or 1
     *                                      <li>if {@code lastModified} is outside the range that can be represented in 4-byte time_t in level 2 or 3
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code originalSize}
     *                                      <li>if {@code originalSize} is negative
     *                                      <li>if {@code originalSize} cannot be represented as a 4-byte value in level 0, 1, or 3
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code compressedSize}
     *                                      <li>if {@code compressedSize} is negative
     *                                      <li>if {@code compressedSize} cannot be represented as a 4-byte value in level 0, 1, or 3
     *                                      <li>if file size header is required because {@code originalSize} or {@code compressedSize} exceeds 4-byte value in level 2, but file size header cannot be output because other extended headers are too large
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown CRC16 value, is set in CRC
     *                                      <li>if header level is other than 0, 1, 2, or 3
     *                                      </ol>
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    public byte[] getBytes(String encode) throws UnsupportedEncodingException {
        return this.exportHeader(encode);
    }

    /**
     * Sets the compression method string.
     *
     * @param method compression method string
     * @throws IllegalArgumentException if compression method string does not start with '-' or does not end with '-'
     */
    public void setCompressMethod(String method) {
        if (method == null) {
            throw new NullPointerException("method");
        } else if (!method.startsWith("-") || !method.endsWith("-")) {
            throw new IllegalArgumentException("method must starts with '-' and ends with '-'");
        } else {
            this.method = method;
        }
    }

    /**
     * Sets data size before compression.<br>
     * {@link #UNKNOWN} (-1) cannot be set because it is a special number indicating unknown size.<br>
     * Also, since only 4-byte values can be processed in level 0, 1, and 3,
     * setting a value that cannot be represented in 4 bytes will throw an exception at {@link #getBytes()} time.
     *
     * @param size data size before compression
     * @throws IllegalArgumentException if attempting to set {@link #UNKNOWN} (-1) to {@code size}
     * @see LhaHeader#UNKNOWN
     */
    public void setOriginalSize(long size) {
        if (size != LhaHeader.UNKNOWN) {
            this.originalSize = size;
        } else {
            throw new IllegalArgumentException("size must not LhaHeader.UNKNOWN( " + LhaHeader.UNKNOWN + " )");
        }
    }

    /**
     * Sets data size after compression.<br>
     * {@link #UNKNOWN} (-1) cannot be set because it is a special number indicating unknown size.<br>
     * Also, since only 4-byte values can be processed in level 0, 1, and 3,
     * setting a value that cannot be represented in 4 bytes will throw an exception at {@link #getBytes()} time.
     *
     * @param size data size after compression
     * @throws IllegalArgumentException if attempting to set {@link #UNKNOWN} to {@code size}
     * @see LhaHeader#UNKNOWN
     */
    public void setCompressedSize(long size) {
        if (size != LhaHeader.UNKNOWN) {
            this.compressedSize = size;
        } else {
            throw new IllegalArgumentException("size must not LhaHeader.UNKNOWN( " + LhaHeader.UNKNOWN + " )");
        }
    }

    /**
     * Sets last modified date and time of compressed data.<br>
     * If header level is 0 or 1, the date must be within the range that can be represented by {@link MsdosDate};
     * if header level is 2 or 3, it must be within the range that can be represented by 4-byte time_t.<br>
     * Note that this method does not throw an exception even if it is not within the range.
     * If it is not within the range, this method does not throw an exception, but an exception will be thrown at {@link #getBytes()} time.
     *
     * @param date last modified date and time
     * @throws IllegalArgumentException if attempting to set {@code null} to {@code date}
     */
    public void setLastModified(Date date) {
        if (date != null) {
            this.lastModified = date;
        } else {
            throw new NullPointerException("date");
        }
    }

    /**
     * Sets header level.<br>
     * Currently only 0, 1, 2, 3 can be set.<br>
     * Note that changing the header level changes the maximum length of path and the restricted range of {@code lastModified}.
     *
     * @param level header level
     */
    public void setHeaderLevel(int level) {
        this.headerLevel = level;
    }

    /**
     * Sets the name of the data, or the path of the data if the data is a file.<br>
     * {@link File#separator} is used as the path delimiter.<br>
     * Note that although there is a byte length restriction on {@code path} depending on the header level,
     * this method does not throw an exception even if the restriction is exceeded.
     * If the restriction is exceeded, this method does not throw an exception, but an exception will be thrown at {@link #getBytes()} time.
     *
     * @param path name of data, or file name
     * @throws IllegalArgumentException if {@code path} is empty string
     * @see File#separator
     */
    public void setPath(String path) {
        if (path == null) {
            throw new NullPointerException("path");
        } else if (path.isEmpty()) {
            throw new IllegalArgumentException("path must not empty.");
        } else {
            this.path = path;
        }
    }

    /**
     * Sets CRC16 value of data before compression.<br>
     * {@link #UNKNOWN} (-1) cannot be set because it is a special number indicating unknown size.<br>
     * {@link #NO_CRC} (-2) is a special value meaning that CRC value is not output in case of level 0 header.<br>
     * Note that setting {@link #NO_CRC} (-2) for other header levels will not throw an exception here,
     * but will throw an exception at {@link #getBytes()} time.<br>
     * The lower 2 bytes are valid, and the upper 2 bytes are ignored.
     *
     * @param crc CRC16 value of data before compression
     * @throws IllegalArgumentException if attempting to set {@link #UNKNOWN} to {@code crc}
     * @see LhaHeader#UNKNOWN
     * @see LhaHeader#NO_CRC
     */
    public void setCRC(int crc) {
        if (crc != LhaHeader.UNKNOWN) {
            this.crc = crc;
        } else {
            throw new IllegalArgumentException("crc must not LhaHeader.UNKNOWN( " + LhaHeader.UNKNOWN + " )");
        }
    }

    /**
     * If OS-specific information is included in this header,
     * sets the OS identifier as a clue to interpret the data.
     *
     * @param id OS identifier
     */
    public void setOSID(byte id) {
        this.osid = id;
    }

    /**
     * Sets extra information in base header used in level 0 and 1 headers.<br>
     * Note that although there is a restriction on the byte length of extra information,
     * this method does not throw an exception even if the restriction is exceeded.
     * If the restriction is exceeded, an exception will be thrown at {@link #getBytes()} time.
     *
     * @param data extra information, or {@code null} if not outputting extra information
     */
    protected void setExtraData(byte[] data) {
        this.extraData = data;
    }

    /**
     * Sets MS-DOS file attributes output in case of level 0 header.
     *
     * @param attribute MS-DOS file attributes
     */
    protected void setLevel0DosAttribute(byte attribute) {
        this.level0DosAttribute = attribute;
    }

    /**
     * Sets file name specified by {@code filename} to path name.
     *
     * @param filename file name
     */
    private void setFileName(String filename) {
        this.path = this.getDirName() + filename;
    }

    /**
     * Sets directory name specified by {@code dirname} to path name.
     *
     * @param dirname directory name
     */
    private void setDirName(String dirname) {
        this.path = dirname + this.getFileName();
    }

    /**
     * Interprets {@code HeaderData} as level 0 header data and sets values to this {@link LhaHeader}.
     *
     * @param HeaderData header data
     * @param encode     encoding used to interpret string information
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private void importLevel0Header(byte[] HeaderData, String encode) throws UnsupportedEncodingException {

        // Definition of header data positions
        final int HeaderSizeIndex = 0;
        int HeaderSize = (HeaderData[HeaderSizeIndex] & 0xFF) + 2;
        final int CompressMethodIndex = 2;
        final int CompressedSizeIndex = 7;
        final int OriginalSizeIndex = 11;
        final int LastModifiedIndex = 15;
        final int DosAttributeIndex = 19;
        final int HeaderLevelIndex = 20;
        final int PathLengthIndex = 21;
        int PathLength = HeaderData[PathLengthIndex] & 0xFF;
        final int PathIndex = 22;
        int CRCIndex = 22 + PathLength;
        int ExtraDataIndex = 24 + PathLength;
        int ExtraDataLength = HeaderSize - ExtraDataIndex;

        // Read header data
        this.method = new String(HeaderData, CompressMethodIndex, 5, encode);
        this.compressedSize = (LittleEndian.readInt(HeaderData, CompressedSizeIndex)) & 0xFFFFFFFFL;
        this.originalSize = (LittleEndian.readInt(HeaderData, OriginalSizeIndex)) & 0xFFFFFFFFL;
        this.lastModified = new MsdosDate(LittleEndian.readInt(HeaderData, LastModifiedIndex));
        this.level0DosAttribute = HeaderData[DosAttributeIndex];
        this.headerLevel = HeaderData[HeaderLevelIndex] & 0xFF;
        this.path = new String(HeaderData, PathIndex, PathLength, encode);
        this.path = this.path.replace('\\', File.separatorChar);

        if (CRCIndex + 2 <= HeaderSize) {
            this.crc = LittleEndian.readShort(HeaderData, CRCIndex);
            if (0 < ExtraDataLength) {
                this.extraData = new byte[ExtraDataLength];
                System.arraycopy(HeaderData, ExtraDataIndex, this.extraData, 0, ExtraDataLength);
            }
        } else {
            this.crc = LhaHeader.NO_CRC;
        }
    }

    /**
     * Interprets {@code HeaderData} as level 1 header data and sets values to this {@link LhaHeader}.
     *
     * @param HeaderData header data
     * @param encode     encoding used to interpret string information
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private void importLevel1Header(byte[] HeaderData, String encode) throws UnsupportedEncodingException {

        // Definition of base header data positions
        final int BaseHeaderSizeIndex = 0;
        int BaseHeaderSize = (HeaderData[BaseHeaderSizeIndex] & 0xFF) + 2;
        final int CompressMethodIndex = 2;
        final int SkipSizeIndex = 7;
        final int OriginalSizeIndex = 11;
        final int LastModifiedIndex = 15;
        final int HeaderLevelIndex = 20;
        final int FileNameLengthIndex = 21;
        int FileNameLength = HeaderData[FileNameLengthIndex] & 0xFF;
        final int FileNameIndex = 22;
        int CRCIndex = 22 + FileNameLength;
        int OSIDIndex = 24 + FileNameLength;
        int ExtraDataIndex = 25 + FileNameLength;
        int ExtraDataLength = BaseHeaderSize - ExtraDataIndex - 2;

        // Read base header data
        this.method = new String(HeaderData, CompressMethodIndex, 5, encode);
        this.compressedSize = (LittleEndian.readInt(HeaderData, SkipSizeIndex)) & 0xFFFFFFFFL;
        this.originalSize = (LittleEndian.readInt(HeaderData, OriginalSizeIndex)) & 0xFFFFFFFFL;
        this.lastModified = new MsdosDate(LittleEndian.readInt(HeaderData, LastModifiedIndex));
        this.headerLevel = HeaderData[HeaderLevelIndex] & 0xFF;
        this.path = new String(HeaderData, FileNameIndex, FileNameLength, encode);
        this.crc = LittleEndian.readShort(HeaderData, CRCIndex);
        this.osid = HeaderData[OSIDIndex];
        if (0 < ExtraDataLength) {
            this.extraData = new byte[ExtraDataLength];
            System.arraycopy(HeaderData, ExtraDataIndex, this.extraData, 0, ExtraDataLength);
        }

        // Read extended header data
        boolean hasFileSize = false;
        int index = BaseHeaderSize;
        int length = LittleEndian.readShort(HeaderData, index - 2);
        while (length != 0) {
            if (!hasFileSize) {
                this.compressedSize -= length;
            }

            this.importExtHeader(HeaderData, index, length - 2, encode);
            if (HeaderData[index] == (byte) 0x42) {
                hasFileSize = true;
            }

            index += length;
            length = LittleEndian.readShort(HeaderData, index - 2);
        }
    }

    /**
     * Interprets {@code HeaderData} as level 2 header data and sets values to this {@link LhaHeader}.
     *
     * @param HeaderData header data
     * @param encode     encoding used to interpret string information
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private void importLevel2Header(byte[] HeaderData, String encode) throws UnsupportedEncodingException {

        // Definition of base header data positions
        final int HeaderSizeIndex = 0;
        @SuppressWarnings("unused") int HeaderSize = LittleEndian.readShort(HeaderData, HeaderSizeIndex);
        final int CompressMethodIndex = 2;
        final int CompressedSizeIndex = 7;
        final int OriginalSizeIndex = 11;
        final int LastModifiedIndex = 15;
        final int HeaderLevelIndex = 20;
        final int CRCIndex = 21;
        final int OSIDIndex = 23;

        // Read base header data
        this.method = new String(HeaderData, CompressMethodIndex, 5, encode);
        this.compressedSize = (LittleEndian.readInt(HeaderData, CompressedSizeIndex)) & 0xFFFFFFFFL;
        this.originalSize = (LittleEndian.readInt(HeaderData, OriginalSizeIndex)) & 0xFFFFFFFFL;
        this.lastModified = new Date(LittleEndian.readInt(HeaderData, LastModifiedIndex) * 1000L);
        this.headerLevel = HeaderData[HeaderLevelIndex] & 0xFF;
        this.crc = LittleEndian.readShort(HeaderData, CRCIndex);
        this.osid = HeaderData[OSIDIndex];

        // Read extended header data
        final int BaseHeaderSize = 26;
        int index = BaseHeaderSize;
        int length = LittleEndian.readShort(HeaderData, index - 2);
        while (length != 0) {
            this.importExtHeader(HeaderData, index, length - 2, encode);
            index += length;
            length = LittleEndian.readShort(HeaderData, index - 2);
        }
    }

    /**
     * Interprets {@code HeaderData} as level 3 header data and sets values to this {@link LhaHeader}.
     *
     * @param HeaderData header data
     * @param encode     encoding used to interpret string information
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private void importLevel3Header(byte[] HeaderData, String encode) throws UnsupportedEncodingException {

        // Definition of base header data positions
        final int WordSizeIndex = 0;
        @SuppressWarnings("unused") int WordSize = LittleEndian.readShort(HeaderData, WordSizeIndex);
        final int CompressMethodIndex = 2;
        final int CompressedSizeIndex = 7;
        final int OriginalSizeIndex = 11;
        final int LastModifiedIndex = 15;
        final int HeaderLevelIndex = 20;
        final int CRCIndex = 21;
        final int OSIDIndex = 23;

        // Read base header data
        this.method = new String(HeaderData, CompressMethodIndex, 5, encode);
        this.compressedSize = (LittleEndian.readInt(HeaderData, CompressedSizeIndex)) & 0xFFFFFFFFL;
        this.originalSize = (LittleEndian.readInt(HeaderData, OriginalSizeIndex)) & 0xFFFFFFFFL;
        this.lastModified = new Date(LittleEndian.readInt(HeaderData, LastModifiedIndex) * 1000L);
        this.headerLevel = HeaderData[HeaderLevelIndex] & 0xFF;
        this.crc = LittleEndian.readShort(HeaderData, CRCIndex);
        this.osid = HeaderData[OSIDIndex];

        // Read extended header data
        final int BaseHeaderSize = 32;
        int index = BaseHeaderSize;
        int length = LittleEndian.readInt(HeaderData, index - 4);
        while (length != 0) {
            this.importExtHeader(HeaderData, index, length - 4, encode);
            index += length;
            length = LittleEndian.readInt(HeaderData, index - 4);
        }
    }

    /**
     * Interprets {@code HeaderData} as LHA header data and sets values to {@link LhaHeader}.
     *
     * @param HeaderData header data
     * @param encode     encoding used to interpret string information
     * @throws IndexOutOfBoundsException    if position where data is assumed to exist is outside the range of {@code HeaderData} because header data is corrupted
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     * @throws IllegalArgumentException     if header level is not 0, 1, 2, or 3
     */
    private void importHeader(byte[] HeaderData, String encode) throws UnsupportedEncodingException {
        final int HeaderLevelIndex = 20;

        switch (HeaderData[HeaderLevelIndex]) {
            case 0:
                this.importLevel0Header(HeaderData, encode);
                break;
            case 1:
                this.importLevel1Header(HeaderData, encode);
                break;
            case 2:
                this.importLevel2Header(HeaderData, encode);
                break;
            case 3:
                this.importLevel3Header(HeaderData, encode);
                break;

            default:
                throw new IllegalArgumentException("unknown header level \"" + HeaderData[HeaderLevelIndex] + "\".");
        }
    }

    /**
     * Reads common extended header from {@code HeaderData}.
     * If data other than CRC16 value for header inspection exists in common extended header,
     * this method simply registers the common extended header in {@code extraExtHeaders}.
     *
     * @param HeaderData header data
     * @param index      starting position of extended header in {@code HeaderData}
     * @param length     length of extended header
     */
    private void importCommonExtHeader(byte[] HeaderData, int index, int length) {
        // Comparison (3 < length) determines whether data is included other than
        // extended header ID (1 byte) and header CRC16 value (2 bytes).
        // If it has information other than CRC16 value, register in extraExtHeaders
        // to save that information.
        if (3 < length) {
            if (this.extraExtHeaders == null) {
                this.extraExtHeaders = new Vector<>();
            }
            byte[] ExtHeaderData = new byte[length];
            System.arraycopy(HeaderData, index, ExtHeaderData, 0, length);
            this.extraExtHeaders.addElement(ExtHeaderData);
        }
    }

    /**
     * Reads file name extended header from {@code HeaderData}.
     *
     * @param HeaderData header data
     * @param index      starting position of extended header in {@code HeaderData}
     * @param length     length of extended header
     * @param encode     encoding used to interpret string information
     * @throws UnsupportedEncodingException this exception is never thrown
     */
    private void importFileNameExtHeader(byte[] HeaderData,
                                         int index,
                                         int length,
                                         String encode) throws UnsupportedEncodingException {

        this.setFileName(new String(HeaderData, index + 1, length - 1, encode));
    }

    /**
     * Reads directory name extended header from {@code HeaderData}.
     *
     * @param HeaderData header data
     * @param index      starting position of extended header in {@code HeaderData}
     * @param length     length of extended header
     * @param encode     encoding used to interpret string information
     * @throws UnsupportedEncodingException this exception is never thrown
     */
    private void importDirNameExtHeader(byte[] HeaderData,
                                        int index,
                                        int length,
                                        String encode) throws UnsupportedEncodingException {

        final byte LhaFileSeparator = (byte) 0xFF;

        int off = 1;
        StringBuilder dir = new StringBuilder();
        while (off < length) {
            int len = 0;
            while (off + len < length) {
                if (HeaderData[index + off + len] != LhaFileSeparator) {
                    len++;
                } else {
                    break;
                }
            }

            if (off + len < length) {
                dir.append(new String(HeaderData, index + off, len, encode)).append(File.separator);
            } else {
                dir.append(new String(HeaderData, index + off, len, encode));
            }
            off += len + 1;
        }

        this.setDirName(dir.toString());
    }

    /**
     * Reads file size extended header from {@code HeaderData}.
     *
     * @param HeaderData header data
     * @param index      starting position of extended header in {@code HeaderData}
     * @param length     length of extended header
     */
    private void importFileSizeHeader(byte[] HeaderData, int index, int length) {
        if (length == 17) {
            this.compressedSize = LittleEndian.readLong(HeaderData, index + 1);
            this.originalSize = LittleEndian.readLong(HeaderData, index + 9);
        } else {
        }
    }

    /**
     * Reads extended header.
     * By overriding this method, it becomes possible to support various extended headers.
     * {@link LhaHeader} simply registers the extended header in {@code extraExtHeaders}, a private member.
     *
     * @param HeaderData header data
     * @param index      starting position of extended header in {@code HeaderData}
     * @param length     length of extended header
     * @param encode     encoding used to interpret string information
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    protected void importExtendHeader(byte[] HeaderData,
                                      int index,
                                      int length,
                                      String encode) throws UnsupportedEncodingException {
        if (this.extraExtHeaders == null) {
            this.extraExtHeaders = new Vector<>();
        }
        byte[] ExtHeaderData = new byte[length];
        System.arraycopy(HeaderData, index, ExtHeaderData, 0, length);
        this.extraExtHeaders.addElement(ExtHeaderData);
    }

    /**
     * Reads extended header of {@code length} bytes starting from {@code index} of {@code HeaderData}.
     *
     * @param HeaderData header data
     * @param index      starting position of extended header in {@code HeaderData}
     * @param length     length of extended header
     * @param encode     encoding used to interpret string information
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private void importExtHeader(byte[] HeaderData, int index, int length, String encode) throws UnsupportedEncodingException {
        final int ExtendHeaderIDIndex = 0;

        switch (HeaderData[index + ExtendHeaderIDIndex]) {
            case 0x00:
                this.importCommonExtHeader(HeaderData, index, length);
                break;
            case 0x01:
                this.importFileNameExtHeader(HeaderData, index, length, encode);
                break;
            case 0x02:
                this.importDirNameExtHeader(HeaderData, index, length, encode);
                break;
            case 0x42:
                this.importFileSizeHeader(HeaderData, index, length);
                break;
            default:
                this.importExtendHeader(HeaderData, index, length, encode);
        }
    }

    /**
     * Generates level 0 header data using information of this {@link LhaHeader}.<br>
     * At that time, if including {@code extraData} causes the header size to exceed the specified value,
     * {@code extraData} may not be included.
     *
     * @param encode encoding used when outputting string information
     * @throws IllegalStateException        <ol>
     *                                      <li>if byte array of compression method string converted by {@code encode} is not 5 bytes
     *                                      <li>if {@code path} is too large to fit in the maximum size of level 0 header
     *                                      <li>if {@code lastModified} is outside the range that can be represented in MS-DOS format
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code originalSize}
     *                                      <li>if {@code originalSize} is negative or cannot be represented as a 4-byte value
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code compressedSize}
     *                                      <li>if {@code compressedSize} is negative or cannot be represented as a 4-byte value
     *                                      </ol>
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private byte[] exportLevel0Header(String encode) throws UnsupportedEncodingException {

        // Preparation for header output
        final int LHarcHeaderSize = 100;
        int CRCLength = (this.crc == LhaHeader.NO_CRC || this.crc == LhaHeader.UNKNOWN ? 0 : 2);
        byte[] CompressMethod = this.method.getBytes(encode);
        MsdosDate dosDate;
        try {
            dosDate = this.lastModified instanceof MsdosDate ? (MsdosDate) this.lastModified : new MsdosDate(this.lastModified); // throw IllegalArgumentException
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(exception.toString());
        }
        byte[] PathData = this.path.replace(File.separatorChar, '\\').getBytes(encode);
        int HeaderLength = 22 + CRCLength + PathData.length;
        byte[] ExtraData;
        if (CRCLength != 0 && this.extraData != null && (HeaderLength + this.extraData.length <= LHarcHeaderSize)) {
            ExtraData = this.extraData;
        } else {
            ExtraData = new byte[0];
        }

        HeaderLength += ExtraData.length;

        // Header validity check
        if (CompressMethod.length != 5) {
            throw new IllegalStateException("CompressMethod doesn't follow Format.");
        }

        if (LHarcHeaderSize < HeaderLength) {
            throw new IllegalStateException("Header size too large.");
        }

        if (this.compressedSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("compressedSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.compressedSize) {
            throw new IllegalStateException("compressedSize must be 0xFFFFFFFF or less.");
        }

        if (this.compressedSize < 0) {
            throw new IllegalStateException("compressedSize must be 0 or more.");
        }

        if (this.originalSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("originalSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.originalSize) {
            throw new IllegalStateException("originalSize must be 0xFFFFFFFF or less.");
        }

        if (this.originalSize < 0) {
            throw new IllegalStateException("originalSize must be 0 or more.");
        }

        // Header output
        byte[] HeaderData;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // -2 because output header length does not include header length (1 byte) and checksum (1 byte) at head of header.
            out.write(HeaderLength - 2);
            out.write(0);
            out.write(CompressMethod);
            LittleEndian.writeInt(out, (int) this.compressedSize);
            LittleEndian.writeInt(out, (int) this.originalSize);
            LittleEndian.writeInt(out, dosDate.getMsdosTime());
            out.write(this.level0DosAttribute);
            out.write(this.headerLevel);
            out.write(PathData.length);
            out.write(PathData);
            if (this.crc != -1) {
                LittleEndian.writeShort(out, this.crc);
                out.write(ExtraData);
            }
            out.close();
            HeaderData = out.toByteArray();
        } catch (IOException exception) {
            throw new Error("caught the IOException ( " + exception.getMessage()
                    + " ) which should be never thrown by ByteArrayOutputStream.");
        }

        final int ChecksumIndex = 1;
        HeaderData[ChecksumIndex] = (byte) LhaHeader.calcHeaderChecksum(HeaderData);

        return HeaderData;
    }

    /**
     * Generates level 1 header data using information of this {@link LhaHeader}.<br>
     * At that time, if including {@code extraData} causes the header size to exceed the specified value,
     * {@code extraData} may not be included.
     * Also, extended headers with a size of 65534 bytes or more are ignored.
     *
     * @param encode encoding used when outputting string information
     * @throws IllegalStateException        <ol>
     *                                      <li>if byte array of compression method string converted by {@code encode} is not 5 bytes
     *                                      <li>if file name is too large to fit in base header or extended header
     *                                      <li>if common extended header is too large to output, hence there is no place to store header CRC
     *                                      <li>if {@link #NO_CRC} (-2), a special value indicating no CRC information in level 0 headers, is set in CRC
     *                                      <li>if {@code lastModified} is outside the range that can be represented in MS-DOS format
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code originalSize}
     *                                      <li>if {@code originalSize} is negative or cannot be represented as a 4-byte value
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code compressedSize}
     *                                      <li>if {@code compressedSize} is negative or cannot be represented as a 4-byte value
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown CRC16 value, is set in CRC
     *                                      </ol>
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private byte[] exportLevel1Header(String encode) throws UnsupportedEncodingException {

        // Preparation for header output
        final int LHarcHeaderSize = 100;
        boolean hasFileName = false; // Indicates whether it has file name information
        boolean hasCRC = false; // Indicates whether it has header CRC information
        byte[] CompressMethod = this.method.getBytes(encode);
        MsdosDate dosDate;
        try {
            if (this.lastModified instanceof MsdosDate) {
                dosDate = (MsdosDate) this.lastModified;
            } else {
                dosDate = new MsdosDate(this.lastModified);
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(exception.toString());
        }

        int HeaderLength = 27;
        byte[] ExtraData;
        if (this.extraData != null && (HeaderLength + this.extraData.length <= LHarcHeaderSize)) {
            ExtraData = this.extraData;
        } else {
            ExtraData = new byte[0];
        }
        HeaderLength += ExtraData.length;

        byte[] FileName = this.getFileName().getBytes(encode);
        if (LHarcHeaderSize < HeaderLength + FileName.length) {
            FileName = new byte[0];
        } else {
            hasFileName = true;
        }
        HeaderLength += FileName.length;

        byte[][] ExtendHeaders = this.exportExtHeaders(encode);
        long SkipSize = this.compressedSize;
        for (int i = 0; i < ExtendHeaders.length; i++) {
            if (ExtendHeaders[i].length == 0 || 65534 <= ExtendHeaders[i].length || (ExtendHeaders[i][0] == 1 && hasFileName)) {
                ExtendHeaders[i] = null;
            } else {
                if (ExtendHeaders[i][0] == 0x00) {
                    hasCRC = true;
                }
                if (ExtendHeaders[i][0] == 0x01) {
                    hasFileName = true;
                }

                SkipSize += ExtendHeaders[i].length + 2;
            }
        }

        // Header validity check
        if (CompressMethod.length != 5) {
            throw new IllegalStateException("CompressMethod doesn't follow Format.");
        }

        if (SkipSize != this.compressedSize && !hasCRC) {
            throw new IllegalStateException("no Header CRC field.");
        }

        if (!hasFileName) {
            throw new IllegalStateException("no Filename infomation.");
        }

        if (this.crc == LhaHeader.NO_CRC) {
            throw new IllegalStateException("no CRC value.");
        }

        if (this.crc == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CRC is UNKNOWN.");
        }

        if (this.compressedSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("compressedSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.compressedSize) {
            throw new IllegalStateException("compressedSize must be 0xFFFFFFFF or less.");
        }

        if (this.compressedSize < 0) {
            throw new IllegalStateException("compressedSize must be 0 or more.");
        }

        if (this.originalSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("originalSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.originalSize) {
            throw new IllegalStateException("originalSize must be 0xFFFFFFFF or less.");
        }

        if (this.originalSize < 0) {
            throw new IllegalStateException("originalSize must be 0 or more.");
        }

        if (0x0000000100000000L <= SkipSize) {
            throw new IllegalStateException("SkipSize must be 0xFFFFFFFF or less.");
        }

        // Header output
        byte[] HeaderData;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // -2 because output header length does not include header length (1 byte) and checksum (1 byte) at head of header.
            out.write(HeaderLength - 2);
            out.write(0);
            out.write(CompressMethod);
            LittleEndian.writeInt(out, (int) SkipSize);
            LittleEndian.writeInt(out, (int) this.originalSize);
            LittleEndian.writeInt(out, dosDate.getMsdosTime());
            out.write(0x20);
            out.write(this.headerLevel);
            out.write(FileName.length);
            out.write(FileName);
            LittleEndian.writeShort(out, this.crc);
            out.write(this.osid);
            out.write(ExtraData);

            for (byte[] extendHeader : ExtendHeaders) {
                if (extendHeader != null) {
                    LittleEndian.writeShort(out, extendHeader.length + 2);
                    out.write(extendHeader);
                }
            }
            LittleEndian.writeShort(out, 0);
            out.close();
            HeaderData = out.toByteArray();
        } catch (IOException exception) {
            throw new Error("caught the IOException ( " + exception.getMessage()
                    + " ) which should be never thrown by ByteArrayOutputStream.");
        }

        final int ChecksumIndex = 1;
        int CRCIndex = LhaHeader.getCRC16Position(HeaderData);
        HeaderData[ChecksumIndex] = (byte) LhaHeader.calcHeaderChecksum(HeaderData);
        if (hasCRC) {
            LittleEndian.writeShort(HeaderData, CRCIndex, LhaHeader.calcHeaderCRC16(HeaderData));
        }

        return HeaderData;
    }

    /**
     * Generates level 2 header data using information of this {@link LhaHeader}.<br>
     * Also, if all extended headers total 65536 bytes or more in size,
     * common extended header and file name extended header are stored with highest priority.
     * If these two extended headers alone become 65536 bytes or more, an exception is thrown.
     * After that, directory name extended header is prioritized, and subsequent extended headers
     * are prioritized and registered in the order output by {@link #exportExtendHeaders(String)},
     * stored so that all headers do not become 65536 bytes or more.
     *
     * @param encode encoding used when outputting string information
     * @throws IllegalStateException        <ol>
     *                                      <li>if byte array of compression method string converted by {@code encode} is not 5 bytes
     *                                      <li>if file name is too large to fit in extended header
     *                                      <li>if common extended header is too large to output, hence there is no place to store header CRC
     *                                      <li>if {@link #NO_CRC} (-2), a special value indicating no CRC information in level 0 headers, is set in CRC
     *                                      <li>if {@code lastModified} is outside the range that can be represented in 4-byte time_t
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code originalSize}
     *                                      <li>if {@code originalSize} is negative
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code compressedSize}
     *                                      <li>if {@code compressedSize} is negative
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown CRC16 value, is set in CRC
     *                                      <li>if file size header is required because {@code originalSize} or {@code compressedSize} exceeds 4-byte value, but file size header cannot be output because other extended headers are too large
     *                                      </ol>
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private byte[] exportLevel2Header(String encode) throws UnsupportedEncodingException {

        // Preparation for header output
        final int MaxHeaderLength = 65535;
        boolean hasFileName = false; // Indicates whether it has file name information
        boolean hasCRC = false; // Indicates whether it has header CRC information
        boolean needExtraByte = false; // Indicates whether to append an extra 1 byte to prevent the head of the header from being 0x00.
        boolean hasFileSize = false; // Indicates whether it has file size header.
        byte[] CompressMethod = this.method.getBytes(encode); // After Java 1.1 throw UnsupportedEncodingException
        int HeaderLength = 26;

        boolean needFileSize = (0x0000000100000000L <= this.compressedSize || 0x0000000100000000L <= this.originalSize);

        byte[][] ExtendHeaders = this.exportExtHeaders(encode);
        for (int i = 0; i < ExtendHeaders.length; i++) {
            if (ExtendHeaders[i].length == 0 || MaxHeaderLength <= HeaderLength + ExtendHeaders[i].length + 2) {
                ExtendHeaders[i] = null;
            } else {
                if (ExtendHeaders[i][0] == 0x00) {
                    hasCRC = true;
                }
                if (ExtendHeaders[i][0] == 0x01) {
                    hasFileName = true;
                }
                if (ExtendHeaders[i][0] == 0x42) {
                    hasFileSize = true;
                }

                HeaderLength += ExtendHeaders[i].length + 2;
            }
        }

        if ((HeaderLength & 0xFF) == 0) {
            HeaderLength++;
            needExtraByte = true;
        }

        // Header validity check
        if (CompressMethod.length != 5) {
            throw new IllegalStateException("CompressMethod doesn't follow Format.");
        }

        if (this.lastModified.getTime() < 0 || ((this.lastModified.getTime() / 1000L) & 0xFFFFFFFF00000000L) != 0) {
            throw new IllegalStateException("lastModified can not change to 4byte time_t format.");
        }

        if (!hasCRC) {
            throw new IllegalStateException("HeaderSize too large. can not contain CRC of the Header.");
        }

        if (!hasFileName) {
            throw new IllegalStateException("HeaderSize too large. can not contain Filename.");
        }

        if (needFileSize && !hasFileSize) {
            throw new IllegalStateException("HeaderSize too large. can not contain Filesize.");
        }

        if (this.crc == LhaHeader.NO_CRC) {
            throw new IllegalStateException("no CRC.");
        }

        if (this.crc == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CRC must not be UNKNOWN.");
        }

        if (this.compressedSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("compressedSize must not be UNKNOWN.");
        }

        if (this.compressedSize < 0) {
            throw new IllegalStateException("compressedSize must be 0 or more.");
        }

        if (this.originalSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("originalSize must not be UNKNOWN.");
        }

        if (this.originalSize < 0) {
            throw new IllegalStateException("originalSize must be 0 or more.");
        }

        // Header output
        byte[] HeaderData;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            LittleEndian.writeShort(out, HeaderLength);
            out.write(CompressMethod);
            LittleEndian.writeInt(out, (int) this.compressedSize);
            LittleEndian.writeInt(out, (int) this.originalSize);
            LittleEndian.writeInt(out, (int) (this.lastModified.getTime() / 1000L));
            out.write(0x20);
            out.write(this.headerLevel);
            LittleEndian.writeShort(out, this.crc);
            out.write(this.osid);

            for (byte[] extendHeader : ExtendHeaders) {
                if (extendHeader != null) {
                    LittleEndian.writeShort(out, extendHeader.length + 2);
                    out.write(extendHeader);
                }
            }
            LittleEndian.writeShort(out, 0);

            if (needExtraByte)
                out.write(0x00);

            out.close();
            HeaderData = out.toByteArray();
        } catch (IOException exception) {
            throw new Error("caught the IOException ( " + exception.getMessage()
                    + " ) which should be never thrown by ByteArrayOutputStream.");
        }

        int CRCIndex = LhaHeader.getCRC16Position(HeaderData);
        LittleEndian.writeShort(HeaderData, CRCIndex, LhaHeader.calcHeaderCRC16(HeaderData));

        return HeaderData;
    }

    /**
     * Generates level 3 header data using information of this {@link LhaHeader}.
     *
     * @param encode encoding used when outputting string information
     * @return header data stored in a byte array
     * @throws IllegalStateException        <ol>
     *                                      <li>if byte array of compression method string converted by {@code encode} is not 5 bytes
     *                                      <li>if common extended header is too large to output, hence there is no place to store header CRC
     *                                      <li>if {@link #NO_CRC} (-2), a special value indicating no CRC information in level 0 headers, is set in CRC
     *                                      <li>if {@code lastModified} is outside the range that can be represented in 4-byte time_t<br>
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code originalSize}<br>
     *                                      <li>if {@code originalSize} is negative or cannot be represented as a 4-byte value<br>
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code compressedSize}<br>
     *                                      <li>if {@code compressedSize} is negative or cannot be represented as a 4-byte value<br>
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown CRC16 value, is set in CRC<br>
     *                                      </ol>
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private byte[] exportLevel3Header(String encode) throws UnsupportedEncodingException {

        // Preparation for header output
        final int WordSize = 4;
        byte[] CompressMethod = this.method.getBytes(encode);
        int HeaderLength = 32;

        byte[][] ExtendHeaders = this.exportExtHeaders(encode);
        for (int i = 0; i < ExtendHeaders.length; i++) {
            if (ExtendHeaders[i].length == 0) {
                ExtendHeaders[i] = null;
            } else {
                HeaderLength += ExtendHeaders[i].length + 4;
            }
        }

        // Header validity check
        if (CompressMethod.length != 5) {
            throw new IllegalStateException("CompressMethod doesn't follow Format.");
        }

        if (this.lastModified.getTime() < 0 || ((this.lastModified.getTime() / 1000L) & 0xFFFFFFFF00000000L) != 0) {
            throw new IllegalStateException("lastModified can not change to 4byte time_t format.");
        }

        if (this.crc == LhaHeader.NO_CRC) {
            throw new IllegalStateException("no CRC value.");
        }

        if (this.crc == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CRC is UNKNOWN.");
        }

        if (this.compressedSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("compressedSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.compressedSize) {
            throw new IllegalStateException("compressedSize must be 0xFFFFFFFF or less.");
        }

        if (this.compressedSize < 0) {
            throw new IllegalStateException("compressedSize must be 0 or more.");
        }

        if (this.originalSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("originalSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.originalSize) {
            throw new IllegalStateException("originalSize must be 0xFFFFFFFF or less.");
        }

        if (this.originalSize < 0) {
            throw new IllegalStateException("originalSize must be 0 or more.");
        }

        // Header output
        byte[] HeaderData;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            LittleEndian.writeShort(out, WordSize);
            out.write(CompressMethod);
            LittleEndian.writeInt(out, (int) this.compressedSize);
            LittleEndian.writeInt(out, (int) this.originalSize);
            LittleEndian.writeInt(out, (int) (this.lastModified.getTime() / 1000L));
            out.write(0x20);
            out.write(this.headerLevel);
            LittleEndian.writeShort(out, this.crc);
            out.write(this.osid);
            LittleEndian.writeInt(out, HeaderLength);

            for (byte[] extendHeader : ExtendHeaders) {
                if (extendHeader != null) {
                    LittleEndian.writeInt(out, extendHeader.length + 4);
                    out.write(extendHeader);
                }
            }
            LittleEndian.writeInt(out, 0);

            out.close();
            HeaderData = out.toByteArray();
        } catch (IOException exception) {
            throw new Error("caught the IOException ( " + exception.getMessage()
                    + " ) which should be never thrown by ByteArrayOutputStream.");
        }

        int crcIndex = LhaHeader.getCRC16Position(HeaderData);
        LittleEndian.writeShort(HeaderData, crcIndex, LhaHeader.calcHeaderCRC16(HeaderData));

        return HeaderData;
    }

    /**
     * Generates header data using data of this {@link LhaHeader} and returns it as a byte array.
     *
     * @param encode encoding used when outputting string information
     * @throws IllegalStateException        <ol>
     *                                      <li>if byte array of compression method string converted by {@code encode} is not 5 bytes</li>
     *                                      <li>if file name is too long to fit in header in level 0, 1, or 2</li>
     *                                      <li>if common extended header is too large to output in level 1 or 2, hence there is no place to store header CRC</li>
     *                                      <li>if {@link #NO_CRC} (-2), a special value indicating no CRC information in level 0 headers, is set for non-level 0 headers</li>
     *                                      <li>if {@code lastModified} is outside the range that can be represented in MS-DOS format in level 0 or 1</li>
     *                                      <li>if {@code lastModified} is outside the range that can be represented in 4-byte time_t in level 2 or 3</li>
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code originalSize}</li>
     *                                      <li>if {@code originalSize} is negative</li>
     *                                      <li>if {@code originalSize} cannot be represented as a 4-byte value in level 0, 1, or 3</li>
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown size, is set in {@code compressedSize}</li>
     *                                      <li>if {@code compressedSize} is negative</li>
     *                                      <li>if {@code compressedSize} cannot be represented as a 4-byte value in level 0, 1, or 3</li>
     *                                      <li>if file size header is required because {@code originalSize} or {@code compressedSize} exceeds 4-byte value in level 2, but file size header cannot be output because other extended headers are too large</li>
     *                                      <li>if {@link #UNKNOWN} (-1), a special value indicating unknown CRC16 value, is set in CRC</li>
     *                                      <li>if header level is other than 0, 1, 2, or 3</li>
     *                                      </ol>
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private byte[] exportHeader(String encode) throws UnsupportedEncodingException {
        return switch (this.headerLevel) {
            case 0 -> this.exportLevel0Header(encode);
            case 1 -> this.exportLevel1Header(encode);
            case 2 -> this.exportLevel2Header(encode);
            case 3 -> this.exportLevel3Header(encode);
            default -> throw new IllegalStateException("unknown header level \"" + this.headerLevel + "\".");
        };
    }

    /**
     * Outputs common extended header as a byte array.
     * If common extended header information is registered in {@code extraExtHeaders},
     * this method returns that information, otherwise returns a 3-byte array initialized with 0.
     *
     * @return common extended header stored in a byte array
     */
    private byte[] exportCommonExtHeader() {
        if (this.extraExtHeaders != null) {
            for (int i = 0; i < this.extraExtHeaders.size(); i++) {
                byte[] extendHeaderData = this.extraExtHeaders.elementAt(i);

                if (extendHeaderData[0] == 0x00) {
                    return extendHeaderData;
                }
            }
        }

        return new byte[3];
    }

    /**
     * Outputs file name extended header as a byte array.
     * File name extended header is output even if empty.
     *
     * @param encode encoding used when outputting string information
     * @return file name extended header stored in a byte array
     */
    private byte[] exportFileNameExtHeader(String encode) throws UnsupportedEncodingException {
        byte[] fileName = this.getFileName().getBytes(encode); // After Java 1.1

        byte[] extendHeaderData = new byte[fileName.length + 1];
        extendHeaderData[0] = 0x01; // Set extended header ID
        System.arraycopy(fileName, 0, extendHeaderData, 1, fileName.length);
        return extendHeaderData;
    }

    /**
     * Outputs directory name extended header as a byte array.
     * In this method, directory name extended header is output even if empty,
     * but if directory name extended header is empty, it is removed at the {@link #exportExtHeaders(String)} stage.
     *
     * @param encode encoding used when outputting string information
     * @return directory name extended header stored in a byte array
     */
    private byte[] exportDirNameExtHeader(String encode) throws UnsupportedEncodingException {

        final byte LhaFileSeparator = (byte) 0xFF;
        String dir = this.getDirName();

        List<byte[]> vec = new ArrayList<>();
        int index = 0;
        int len = 0;
        int length = 0;
        while (index + len < dir.length()) {
            if (dir.charAt(index + len) == File.separatorChar) {
                byte[] src = dir.substring(index, index + len).getBytes(encode);
                byte[] array = new byte[src.length + 1];
                System.arraycopy(src, 0, array, 0, src.length);
                array[src.length] = LhaFileSeparator;
                length += array.length;
                vec.add(array);

                index += len + 1;
                len = 0;
            } else if (index + len + 1 < dir.length()) {
                byte[] array = dir.substring(index, index + len + 1).getBytes(encode);
                length += array.length;
                vec.add(array);

                index += len + 1;
                len = 0;
            } else {
                len++;
            }
        }

        byte[] extendHeaderData = new byte[length + 1];
        extendHeaderData[0] = 0x02; // Set extended header ID
        index = 1;
        for (byte[] array : vec) {
            System.arraycopy(array, 0, extendHeaderData, index, array.length);
            index += array.length;
        }

        return extendHeaderData;
    }

    /**
     * Outputs 64-bit file size header as a byte array.
     * This method outputs a byte array even if original size or compressed size can be represented as 32-bit values.
     * When not needed, {@link #exportExtHeaders(String)} suppresses output.
     *
     * @return 64-bit file size header
     */
    private byte[] exportFileSizeHeader() {
        byte[] extendHeaderData = new byte[17];

        extendHeaderData[0] = (byte) 0x42;
        LittleEndian.writeLong(extendHeaderData, 1, this.compressedSize);
        LittleEndian.writeLong(extendHeaderData, 9, this.originalSize);

        return extendHeaderData;
    }

    /**
     * Outputs extended headers as a byte array.
     * By overriding this method, it becomes possible to support various extended headers.
     * {@link LhaHeader} simply returns the extended header information registered in {@code extraExtHeaders}, a private member.
     * The output format stores the extended header identifier in the first byte, followed by extended header data,
     * without attaching the size of the next extended header.
     *
     * @param encode encoding used when outputting string information
     * @return an array of byte arrays, each storing one extended header
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    protected byte[][] exportExtendHeaders(String encode) throws UnsupportedEncodingException {
        if (this.extraExtHeaders != null) {
            byte[][] extendHeaders = new byte[this.extraExtHeaders.size()][];

            for (int i = 0; i < this.extraExtHeaders.size(); i++) {
                extendHeaders[i] = this.extraExtHeaders.elementAt(i);
            }

            return extendHeaders;
        } else {
            return new byte[0][];
        }
    }

    /**
     * Outputs extended headers as byte arrays.
     *
     * @param encode encoding used when outputting string information
     * @return an array of byte arrays of all extended headers, each storing one extended header
     * @throws UnsupportedEncodingException if the encoding specified by {@code encode} is not supported
     */
    private byte[][] exportExtHeaders(String encode) throws UnsupportedEncodingException {
        byte[] commonExtHeader = this.exportCommonExtHeader();
        byte[] fileNameExtHeader = this.exportFileNameExtHeader(encode);
        byte[] dirNameExtHeader = this.exportDirNameExtHeader(encode);

        byte[][] extraExtHeaders = this.exportExtendHeaders(encode);
        List<byte[]> headers = new ArrayList<>();

        headers.add(commonExtHeader);
        headers.add(fileNameExtHeader);
        if (1 < dirNameExtHeader.length) {
            headers.add(dirNameExtHeader);
        }

        if (this.headerLevel == 2 && (0x0000_0001_0000_0000L <= this.compressedSize || 0x0000_0001_0000_0000L <= this.originalSize)) {
            headers.add(this.exportFileSizeHeader());
        }

        for (byte[] extendHeaderData : extraExtHeaders) {
            if (0 < extendHeaderData.length && extendHeaderData[0] != 0x00 && extendHeaderData[0] != 0x01
                    && extendHeaderData[0] != 0x02) {
                headers.add(extendHeaderData);
            }
        }

        return headers.toArray(byte[][]::new);
    }

    /**
     * Checks whether header data is valid.
     *
     * @param HeaderData header data stored in a byte array
     * @return {@code true} if header data is valid, {@code false} otherwise
     */
    public static boolean checkHeaderData(byte[] HeaderData) {
        final int HeaderLevelIndex = 20;
        try {
            switch (HeaderData[HeaderLevelIndex] & 0xFF) {
                case 0:
                    return LhaHeader.verifyHeaderChecksum(HeaderData);

                case 1:
                    return LhaHeader.verifyHeaderChecksum(HeaderData)
                            && (LhaHeader.getCRC16Position(HeaderData) == -1 || LhaHeader.verifyHeaderCRC16(HeaderData));

                case 2:
                    return LhaHeader.verifyHeaderCRC16(HeaderData);

                case 3:
                    return LhaHeader.verifyHeaderCRC16(HeaderData);

            }
        } catch (ArrayIndexOutOfBoundsException ignore) {
        }
        return false;
    }

    /**
     * Gets the position where CRC value of header is stored.
     *
     * @param headerData header data stored in a byte array
     * @return position of CRC value of header, or -1 if header has no CRC value
     */
    private static int getCRC16Position(byte[] headerData) {
        final int HeaderLevelIndex = 20;
        int wordSize;
        int position;
        int length;

        switch (headerData[HeaderLevelIndex] & 0xFF) {
            case 1:
                wordSize = 2;
                position = length = (headerData[0] & 0xFF) + 2;
                break;
            case 2:
                wordSize = 2;
                position = length = 26;
                break;
            case 3:
                wordSize = 4;
                position = length = 32;
                break;
            default:
                return -1;
        }

        while (true) {
            if (0 < length && position < headerData.length) {
                length = 0;

                for (int i = 0; i < wordSize; i++) {
                    length = (length << 8 | (headerData[position - (1 + i)] & 0xFF));
                }

                if (headerData[position] == 0) {
                    return position + 1;
                }

                position += length;
            } else {
                return -1;
            }
        }
    }

    /**
     * Calculates checksum value from header data of level 0 and level 1 headers.
     *
     * @param headerData header data stored in a byte array
     * @return calculated header checksum value
     */
    private static int calcHeaderChecksum(byte[] headerData) {
        int length = headerData[0] & 0xFF;

        LhaChecksum checksum = new LhaChecksum();
        checksum.update(headerData, 2, length);

        return (int) checksum.getValue();
    }

    /**
     * Calculates CRC16 value from header data of level 1, level 2, and level 3 headers.
     *
     * @param HeaderData header data stored in a byte array
     * @return calculated header CRC16 value
     */
    private static int calcHeaderCRC16(byte[] HeaderData) {
        int position = LhaHeader.getCRC16Position(HeaderData);
        int crcValue = 0;
        if (position != -1) {
            crcValue = LittleEndian.readShort(HeaderData, position);
            LittleEndian.writeShort(HeaderData, position, 0);
        }

        CRC16 crc16 = new CRC16();
        crc16.update(HeaderData);

        if (position != -1) {
            LittleEndian.writeShort(HeaderData, position, crcValue);
        }

        return (int) crc16.getValue();
    }

    /**
     * Reads checksum value from header data of level 0 and level 1 headers.
     *
     * @param HeaderData header data stored in a byte array
     * @return checksum value recorded in header
     */
    private static int readHeaderChecksum(byte[] HeaderData) {
        return HeaderData[1] & 0xFF;
    }

    /**
     * Reads CRC16 value from header data of level 1, level 2, and level 3 headers.
     *
     * @param HeaderData header data stored in a byte array
     * @return CRC16 value recorded in header
     */
    private static int readHeaderCRC16(byte[] HeaderData) {
        int position = LhaHeader.getCRC16Position(HeaderData);
        if (position != -1) {
            return LittleEndian.readShort(HeaderData, position);
        } else {
            return -1;
        }
    }

    /**
     * Checks validity of header data using checksum value.
     *
     * @param HeaderData header data stored in a byte array
     * @return {@code true} if validity of header data is proven by checksum value, {@code false} otherwise
     */
    private static boolean verifyHeaderChecksum(byte[] HeaderData) {
        final int HeaderLevelIndex = 20;

        return switch (HeaderData[HeaderLevelIndex] & 0xFF) {
            case 0, 1 -> LhaHeader.readHeaderChecksum(HeaderData) == LhaHeader.calcHeaderChecksum(HeaderData);
            default -> false;
        };
    }

    /**
     * Checks validity of header data using CRC16 value.
     *
     * @param HeaderData header data stored in a byte array
     * @return {@code true} if validity of header data is proven by CRC16 value, {@code false} otherwise
     */
    private static boolean verifyHeaderCRC16(byte[] HeaderData) {
        final int HeaderLevelIndex = 20;

        return switch (HeaderData[HeaderLevelIndex] & 0xFF) {
            case 1, 2, 3 -> LhaHeader.readHeaderCRC16(HeaderData) == LhaHeader.calcHeaderCRC16(HeaderData);
            default -> false;
        };
    }

    /**
     * Reads the first header from the input stream.<br>
     * If data similar to a level 1 header or level 3 header exists, this method may attempt
     * to read the entire header beyond the limit of {@code in.mark(65536)}, and as a result,
     * it may fail to {@code reset()} and miss data in between.<br>
     * Also, depending on the implementation of mark/reset in {@link InputStream}, if data similar
     * to a header exists near the end of the stream, it may reach EndOfStream while attempting
     * to read the entire header, failing to {@code reset()} and missing data in between.
     *
     * @param in input stream to read header data from. Stream requires mark/reset support.
     * @return read header data, or {@code null} if EndOfStream is reached without finding a header
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if {@code in} does not support mark/reset
     */
    public static byte[] getFirstHeaderData(InputStream in) throws IOException {
        if (in.markSupported()) {
            try {
                int stock1 = -1;
                int stock2 = -1;
                int read;

                while (0 <= (read = in.read())) {
                    if (read == '-' && 0 < stock1) {
                        in.mark(65536); // 65536 guarantees only level 0, 2
                        LhaHeader.ensureSkip(in, 3);
                        if (in.read() == '-') {
                            LhaHeader.ensureSkip(in, 13);
                            int HeaderLevel = in.read();
                            in.reset();
                            byte[] HeaderData = switch (HeaderLevel) {
                                case 0 -> LhaHeader.readLevel0HeaderData(stock1, stock2, read, in);
                                case 1 -> LhaHeader.readLevel1HeaderData(stock1, stock2, read, in);
                                case 2 -> LhaHeader.readLevel2HeaderData(stock1, stock2, read, in);
                                case 3 -> LhaHeader.readLevel3HeaderData(stock1, stock2, read, in);
                                default -> null;
                            };

                            if (HeaderData != null && LhaHeader.checkHeaderData(HeaderData))
                                return HeaderData;
                        }
                        in.reset();
                    }
                    stock1 = stock2;
                    stock2 = read;
                }
            } catch (EOFException exception) { // Ignore
            }
            return null;
        } else {
            throw new IllegalArgumentException("InputStream needed mark()/reset() support.");
        }
    }

    /**
     * Reads the next header from the input stream.<br>
     * If data similar to a level 1 header or level 3 header exists, this method may attempt
     * to read the entire header beyond the limit of {@code in.mark(65536)}, and as a result,
     * it may fail to {@code reset()} and miss data in between.<br>
     * Also, if data similar to a header exists near the end of the stream, it may reach EndOfStream
     * while attempting to read the entire header, failing to {@code reset()} and missing data in between.
     *
     * @param in input stream to read header data from. Stream requires mark/reset support.
     * @return read header data, or {@code null} if EndOfStream is reached without finding a header
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if {@code in} does not support mark/reset
     */
    public static byte[] getNextHeaderData(InputStream in) throws IOException {
        if (in.markSupported()) {
            try {
                int first = in.read();
                if (0 < first) { // Negative value reaches EndOfStream, 0 reaches archive end
                    int second = in.read();
                    int third = in.read();
                    in.mark(65536); // 65536 guarantees only level 0, 2
                    LhaHeader.ensureSkip(in, 3);
                    int seventh = in.read();
                    if (third == '-' && seventh == '-') {
                        LhaHeader.ensureSkip(in, 13);
                        int HeaderLevel = in.read();
                        in.reset();
                        byte[] headerData = switch (HeaderLevel) {
                            case 0 -> LhaHeader.readLevel0HeaderData(first, second, third, in);
                            case 1 -> LhaHeader.readLevel1HeaderData(first, second, third, in);
                            case 2 -> LhaHeader.readLevel2HeaderData(first, second, third, in);
                            case 3 -> LhaHeader.readLevel3HeaderData(first, second, third, in);
                            default -> null;
                        };

                        if (headerData != null && LhaHeader.checkHeaderData(headerData)) {
                            return headerData;
                        }
                    }
                }
            } catch (EOFException exception) { // Ignore
            }
            return null;
        } else {
            throw new IllegalArgumentException("InputStream needed mark()/reset() support.");
        }
    }

    /**
     * Reads level 0 header from input stream.
     *
     * @param headerLength    length of header
     * @param headerChecksum  checksum of header
     * @param compressMethod1 compression method string
     * @param in              input stream to read header data from
     * @return read header data
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached while reading header
     */
    private static byte[] readLevel0HeaderData(int headerLength,
                                               int headerChecksum,
                                               int compressMethod1,
                                               InputStream in) throws IOException {
        byte[] headerData = new byte[headerLength + 2];
        headerData[0] = (byte) headerLength;
        headerData[1] = (byte) headerChecksum;
        headerData[2] = (byte) compressMethod1;
        int readed = 3;
        int length = 0;
        headerLength += 2;

        while (readed < headerLength && 0 <= length) {
            length = in.read(headerData, readed, headerLength - readed);
            readed += length;
        }

        if (readed == headerLength) {
            return headerData;
        } else {
            throw new EOFException();
        }
    }

    /**
     * Reads level 1 header from input stream.
     *
     * @param baseHeaderLength   length of base header
     * @param baseHeaderChecksum checksum of base header
     * @param compressMethod1    compression method string
     * @param in                 input stream to read header data from
     * @return read header data, or {@code null} if determined not to be a level 1 header
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached while reading header
     */
    private static byte[] readLevel1HeaderData(int baseHeaderLength,
                                               int baseHeaderChecksum,
                                               int compressMethod1,
                                               InputStream in) throws IOException {
        int headerLength = baseHeaderLength + 2;
        Vector<byte[]> headers = new Vector<>();
        byte[] headerData = new byte[headerLength];
        headerData[0] = (byte) baseHeaderLength;
        headerData[1] = (byte) baseHeaderChecksum;
        headerData[2] = (byte) compressMethod1;

        // Obtain header data
        int readed;
        int length = 0;
        do {
            if (headers.isEmpty()) {
                readed = 3;
            } else {
                readed = 0;
            }

            while (readed < headerLength && 0 <= length) {
                length = in.read(headerData, readed, headerLength - readed);
                readed += length;
            }

            if (readed == headerLength) {
                if (headers.isEmpty() && !LhaHeader.verifyHeaderChecksum(headerData)) {
                    return null;
                } else {
                    headers.addElement(headerData);
                }
            } else {
                throw new EOFException();
            }

            length = headerLength;
            headerLength = LittleEndian.readShort(headerData, headerLength - 2);
            headerData = new byte[headerLength];
        } while (0 < headerLength && readed == length);

        // Combine obtained header data into a single byte array
        headerLength = 0;
        for (int i = 0; i < headers.size(); i++) {
            headerLength += headers.elementAt(i).length;
        }

        headerData = new byte[headerLength];
        int position = 0;
        for (int i = 0; i < headers.size(); i++) {
            byte[] data = headers.elementAt(i);
            System.arraycopy(data, 0, headerData, position, data.length);

            position += data.length;
        }
        return headerData;
    }

    /**
     * Reads level 2 header from input stream.
     *
     * @param HeaderLengthLow lower byte of header length
     * @param HeaderLengthHi  upper byte of header length
     * @param CompressMethod1 compression method string
     * @param in              input stream to read header data from
     * @return read header data
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached while reading header
     */
    private static byte[] readLevel2HeaderData(int HeaderLengthLow,
                                               int HeaderLengthHi,
                                               int CompressMethod1,
                                               InputStream in) throws IOException {
        int headerLength = (HeaderLengthHi << 8) | HeaderLengthLow;
        byte[] headerData = new byte[headerLength];
        headerData[0] = (byte) HeaderLengthLow;
        headerData[1] = (byte) HeaderLengthHi;
        headerData[2] = (byte) CompressMethod1;

        int readed = 3;
        int length = 0;
        while (readed < headerLength && 0 <= length) {
            length = in.read(headerData, readed, headerLength - readed);
            readed += length;
        }

        if (readed == headerLength) {
            return headerData;
        } else {
            throw new EOFException();
        }
    }

    /**
     * Reads level 3 header from input stream.<br>
     * Unlike other read methods, this method assumes that {@code mark()} has been called in {@link #getNextHeaderData(InputStream)}.
     *
     * @param wordSizeLow     lower byte of word size used in header
     * @param wordSizeHi      upper byte of word size used in header
     * @param compressMethod1 compression method string
     * @param in              input stream to read header data from
     * @return read header data, or {@code null} if determined not to be a level 3 header
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached while reading header
     */
    private static byte[] readLevel3HeaderData(int wordSizeLow,
                                               int wordSizeHi,
                                               int compressMethod1,
                                               InputStream in) throws IOException {
        if (wordSizeLow == 0x04 && wordSizeHi == 0x00) {
            in.skipNBytes(21);
            int HeaderLength = LittleEndian.readInt(in);
            in.reset();

            byte[] headerData = new byte[HeaderLength];
            headerData[0] = (byte) wordSizeLow;
            headerData[1] = (byte) wordSizeHi;
            headerData[2] = (byte) compressMethod1;

            int readed = 3;
            int length = 0;
            while (readed < HeaderLength && 0 <= length) {
                length = in.read(headerData, readed, HeaderLength - readed);
                readed += length;
            }

            if (readed == HeaderLength) {
                return headerData;
            } else {
                throw new EOFException();
            }
        } else {
            return null;
        }
    }

    /**
     * Creates an instance of {@link LhaHeader} from {@code headerData} using the generator expression
     * bound to key "lha.header" in {@code property}.
     *
     * @param headerData byte array having header data
     * @param property   properties having generator expression as value of key "lha.header" such that {@link LhaProperty#parse(String, Hashtable, String)} can instantiate {@link LhaHeader}
     * @return instance of {@link LhaHeader}
     */
    public static LhaHeader createInstance(byte[] headerData, Properties property) {

        String encoding = property.getProperty("lha.encoding");
        if (encoding == null) {
            encoding = LhaProperty.getProperty("lha.encoding");
        }

        String packages = property.getProperty("lha.packages");
        if (packages == null) {
            packages = LhaProperty.getProperty("lha.packages");
        }

        String generator = property.getProperty("lha.header");
        if (generator == null) {
            generator = LhaProperty.getProperty("lha.header");
        }

        Hashtable<String, Object> substitute = new Hashtable<>();
        substitute.put("data", headerData);
        substitute.put("encoding", encoding);

        return (LhaHeader) LhaProperty.parse(generator, substitute, packages);
    }

    /**
     * Skips {@code len} bytes in {@link InputStream}.
     *
     * @param in  input stream
     * @param len length to skip
     * @throws IOException  if an I/O error occurs
     * @throws EOFException if EndOfStream is reached
     */
    private static void ensureSkip(InputStream in, long len) throws IOException {
        while (0 < len) {
            long skipLen = in.skip(len);
            if (skipLen <= 0) {
                if (0 <= in.read()) {
                    len--;
                } else {
                    throw new EOFException();
                }
            } else {
                len -= skipLen;
            }
        }
    }
}
