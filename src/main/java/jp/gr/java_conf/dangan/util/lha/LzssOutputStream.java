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
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

import jp.gr.java_conf.dangan.lang.reflect.Factory;


/**
 * Output stream for compression that outputs to a specified {@link PostLzssEncoder}
 * while compressing data with LZSS.
 *
 * <pre>
 * -- revision history --
 * $Log: LzssOutputStream.java,v $
 * Revision 1.2  2002/12/06 00:00:00  dangan
 * [change]
 *     Modified flush() to output all write()'d data to the connected PostLzssEncoder.
 * [maintenance]
 *     Fixed slide width to always be DictionarySize bytes.
 *
 * Revision 1.1  2002/10/20 00:00:00  dangan
 * [bug fix]
 *     When flushed in the initial state or flushed consecutively
 *     (when encode() is called while lastsearchret is NEEDSEARCH),
 *     the immediately following 1 byte was corrupted.
 *     Fixed performing searchAndPut that corrupted the search mechanism
 *     because putLength() was not considered during flush().
 *     Fixed MaxMatch bytes of data at the end of TextBuffer not being output during flush().
 *
 * Revision 1.0  2002/07/25 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     Fixed getMatchLen() which used searchret >>> 22 instead of searchret >> 22.
 * [maintenance]
 *     Replaced LhaUtil.createInstance() with Factory.createInstance().
 *     source maintenance
 *     tab removal
 *     license comment update
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.2 $
 */
public class LzssOutputStream extends OutputStream {

    /**
     * Value to register in {@link #lastsearchret}.
     * Indicates that processing of {@code searchAndPut} is required.
     */
    private static final int NEEDSEARCH = 0;

    /**
     * When {@code searchret} is this value,
     * it indicates that as a result of the search, no match equal to or greater than the threshold was found.
     */
    public static final int NOMATCH = -1;

    /**
     * Output stream to which LZSS compression codes are emitted.
     */
    private PostLzssEncoder encoder;

    /**
     * LZSS dictionary size.
     */
    private final int DictionarySize;

    /**
     * Threshold used for LZSS compression.
     * If the match length is greater than or equal to this value, compression code is output.
     */
    private final int Threshold;

    /**
     * Value used for LZSS compression.
     * Indicates maximum match length.
     */
    private final int MaxMatch;

    /**
     * Buffer for applying LZSS compression.
     * The first half is the dictionary area,
     * and the second half is the buffer containing data to be compressed.
     */
    private byte[] TextBuffer;

    /**
     * Dictionary limit position.
     * When there is no data in the dictionary area in the first half of {@link #TextBuffer},
     * this prevents compression from using undefined data (0 in Java) in the dictionary area.
     */
    private int DictionaryLimit;

    /**
     * Write completed position in {@link #TextBuffer}.
     * Position written by {@link #write(int)}.
     * <p>
     * The relationship among the three is {@code putPos <= searchedPos <= writtenPos}.
     */
    private int writtenPos;

    /**
     * {@code put()} completed position in {@link #TextBuffer}.
     * Position where registration into the search mechanism has completed
     * via {@link LzssSearchMethod#put(int)} or {@link LzssSearchMethod#searchAndPut(int)}.
     */
    private int putPos;

    /**
     * Current search position in {@link #TextBuffer}.
     * Position where the next search should be performed
     * via {@link LzssSearchMethod#search(int, int)} or {@link LzssSearchMethod#searchAndPut(int)}.
     */
    private int searchPos;

    /**
     * Preserves the last {@code searchret} of the previous encode.
     * In the constructor, {@link #NEEDSEARCH}, which indicates an invalid number, is set.
     */
    private int lastsearchret;

    /**
     * Class that manages searching.
     */
    private LzssSearchMethod method;

    /**
     * Constructs an output stream that compresses data written by {@link #write(int)}
     * with LZSS and outputs the compressed data to {@code encoder}.
     *
     * @param encoder LZSS compressed data output stream
     */
    public LzssOutputStream(PostLzssEncoder encoder) {
        this(encoder, HashAndChainedListSearch.class.getName(), new Object[0]);
    }

    /**
     * Constructs an output stream that compresses data written by {@link #write(int)}
     * with LZSS and outputs the compressed data to {@code encoder}.
     *
     * @param encoder                   LZSS compressed data output stream
     * @param LzssSearchMethodClassName class name including package name indicating the implementation of {@link LzssSearchMethod}
     * @throws NoClassDefFoundError if the class specified by {@code LzssSearchMethodClassName} is not found.
     * @throws InstantiationError   if an instance cannot be created because the class specified by {@code LzssSearchMethodClassName} is an abstract class.
     * @throws NoSuchMethodError    if the class specified by {@code LzssSearchMethodClassName} does not have the constructor {@code LzssSearchMethod(int, int, int, byte[], int)}.
     */
    public LzssOutputStream(PostLzssEncoder encoder, String LzssSearchMethodClassName) {
        this(encoder, LzssSearchMethodClassName, new Object[0]);
    }

    /**
     * Constructs an output stream that compresses data written by {@link #write(int)}
     * with LZSS and outputs the compressed data to {@code encoder}.
     *
     * @param encoder                   LZSS compressed data output stream
     * @param LzssSearchMethodClassName class name including package name indicating the implementation of {@link LzssSearchMethod}
     * @param LzssSearchMethodExtraArguments extra arguments for the {@link LzssSearchMethod} constructor
     * @throws NoClassDefFoundError if the class specified by {@code LzssSearchMethodClassName} is not found.
     * @throws InstantiationError   if an instance cannot be created because the class specified by {@code LzssSearchMethodClassName} is an abstract class.
     * @throws NoSuchMethodError    if the class specified by {@code LzssSearchMethodClassName} does not have the constructor {@code LzssSearchMethod(int, int, int, byte[])}.
     */
    public LzssOutputStream(PostLzssEncoder encoder,
                            String LzssSearchMethodClassName,
                            Object[] LzssSearchMethodExtraArguments) {

        this.DictionarySize = encoder.getDictionarySize();
        this.MaxMatch = encoder.getMaxMatch();
        this.Threshold = encoder.getThreshold();

        this.encoder = encoder;
        this.TextBuffer = new byte[this.DictionarySize * 2 + this.MaxMatch];
        this.writtenPos = this.DictionarySize;
        this.putPos = this.DictionarySize;
        this.searchPos = this.DictionarySize;
        this.DictionaryLimit = this.DictionarySize;
        this.lastsearchret = LzssOutputStream.NEEDSEARCH;

        Object[] arguments = new Object[LzssSearchMethodExtraArguments.length + 4];
        arguments[0] = this.DictionarySize;
        arguments[1] = this.MaxMatch;
        arguments[2] = this.Threshold;
        arguments[3] = this.TextBuffer;
        for (int i = 0; i < LzssSearchMethodExtraArguments.length; i++) {
            arguments[4 + i] = LzssSearchMethodExtraArguments[i];
        }

        try {
            this.method = (LzssSearchMethod) Factory.createInstance(LzssSearchMethodClassName, arguments);
        } catch (ClassNotFoundException exception) {
            throw new NoClassDefFoundError(exception.getMessage());
        } catch (InvocationTargetException exception) {
            throw new Error(exception.getTargetException().getMessage());
        } catch (NoSuchMethodException exception) {
            throw new NoSuchMethodError(exception.getMessage());
        } catch (InstantiationException exception) {
            throw new InstantiationError(exception.getMessage());
        }
    }

    /**
     * Outputs 1 byte of data to the compression mechanism.<br>
     * Data is actually passed to {@link PostLzssEncoder} only when {@link #TextBuffer}
     * is filled or when output is explicitly instructed by {@link #flush()}.
     *
     * @param data 1 byte of data
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(int data) throws IOException {
        this.TextBuffer[this.writtenPos++] = (byte) data;

        if (this.TextBuffer.length <= this.writtenPos) {
            this.encode(false);
            this.slide();
        }
    }

    /**
     * Outputs all data in {@code buffer} to the compression mechanism.<br>
     * Data is actually passed to {@link PostLzssEncoder} only when {@link #TextBuffer}
     * is filled or when output is explicitly instructed by {@link #flush()}.
     *
     * @param buffer buffer storing data
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    /**
     * Outputs {@code length} bytes of data from {@code index} in {@code buffer} to the compression mechanism.<br>
     * Data is actually passed to {@link PostLzssEncoder} only when {@link #TextBuffer}
     * is filled or when output is explicitly instructed by {@link #flush()}.
     *
     * @param buffer buffer storing data
     * @param index  start position of data in {@code buffer}
     * @param length length of data in {@code buffer}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] buffer, int index, int length) throws IOException {
        int pos = index;
        int end = index + length;

        while (pos < end) {
            int space = TextBuffer.length - writtenPos;
            if (end - pos < space) {
                System.arraycopy(buffer, pos, this.TextBuffer, this.writtenPos, end - pos);
                this.writtenPos += end - pos;
                pos = end;
            } else {
                System.arraycopy(buffer, pos, this.TextBuffer, this.writtenPos, space);
                this.writtenPos += space;
                pos += space;
                this.encode(false);
                this.slide();
            }
        }
    }

    /**
     * Outputs all data written to the compression mechanism to the connected
     * {@link PostLzssEncoder} and flushes the connected {@link PostLzssEncoder}.<br>
     * At this time, near the end of the output data, search uses {@link LzssSearchMethod#search(int, int)},
     * which decreases compression speed.
     * Also, the compression ratio changes compared to when not calling {@link #flush()}.
     * This is because near the flushed position, data patterns shorter than {@link #MaxMatch}
     * are used for pattern searching, resulting in incomplete search results.
     * While this change in compression ratio usually only causes a slight degradation,
     * code such as the following will not perform any LZ compression at all:
     *
     * <pre>
     *  public void wrongCompress( InputStream in, LzssOutputSteam out ){
     *      int r;
     *      while( 0 <= r = in.read() ){
     *          out.write( r );
     *          out.flush();
     *      }
     *  }
     * </pre>
     * <p>
     * In addition, since this method calls {@link PostLzssEncoder#flush()},
     * the output data may differ compared to when not calling {@link #flush()}.
     *
     * @throws IOException if an I/O error occurs
     * @see PostLzssEncoder#flush()
     */
    @Override
    public void flush() throws IOException {
        this.encode(false);
        if (this.DictionarySize * 2 <= this.putPos) {
            this.slide();
            if (this.searchPos < this.writtenPos) {
                this.encode(false);
            }
        }
        this.encoder.flush();
    }

    /**
     * Outputs all data accumulated in this class to the connected
     * {@link PostLzssEncoder}, closes this output stream and the connected stream,
     * and releases the resources used.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        while (this.DictionarySize <= this.writtenPos) {
            this.encode(true);
            if (this.writtenPos <= this.searchPos) {
                break;
            } else {
                this.slide();
            }
        }

        this.encoder.close();
        this.encoder = null;

        this.TextBuffer = null;
        this.method = null;
    }

    /**
     * Compresses data accumulated in {@link #TextBuffer} and outputs to private field {@code this.encoder}.
     *
     * @param last terminal position of data in {@link #TextBuffer} whose output is complete + 1
     * @throws IOException if an I/O error occurs
     */
    private void encode(boolean last) throws IOException {

        int end = Math.min(this.TextBuffer.length - this.MaxMatch, this.writtenPos - (last ? 0 : this.method.putRequires()));
        if (this.searchPos < end) {

            // Pre-processing
            if (this.lastsearchret == LzssOutputStream.NEEDSEARCH) {

                // Register unregistered data patterns into the search mechanism
                while (this.putPos < this.searchPos - 1) {
                    this.method.put(++this.putPos);

                    // If we only put() data patterns that couldn't be put() in the immediately preceding flush(), return
                    if (this.DictionarySize * 2 <= this.putPos) {
                        return;
                    }
                }

                // Since lastsearchret is NEEDSEARCH, search using searchAndPut.
                this.lastsearchret = this.method.searchAndPut(this.searchPos);
            }

            int searchret = this.lastsearchret;
            int matchlen = LzssOutputStream.getMatchLen(searchret);
            int matchpos = LzssOutputStream.getMatchPos(searchret);
            if (this.writtenPos - this.searchPos < matchlen) {
                matchlen = this.writtenPos - this.searchPos;
            }

            // Main loop
            while (true) {
                int lastmatchlen = matchlen;
                int lastmatchoff = this.searchPos - matchpos - 1;

                searchret = this.method.searchAndPut(++this.searchPos);
                matchlen = LzssOutputStream.getMatchLen(searchret);
                matchpos = LzssOutputStream.getMatchPos(searchret);
                if (this.writtenPos - this.searchPos < matchlen) {
                    matchlen = this.writtenPos - this.searchPos;
                }

                if (lastmatchlen < matchlen || lastmatchlen < this.Threshold) {
                    this.encoder.writeCode(0xFF & this.TextBuffer[this.searchPos - 1]);
                    if (end <= this.searchPos) {
                        this.putPos = this.searchPos;
                        this.lastsearchret = searchret;
                        break;
                    }
                } else {
                    this.encoder.writeCode(256 + lastmatchlen - this.Threshold);
                    this.encoder.writeOffset(lastmatchoff);

                    lastmatchlen--;
                    if (this.searchPos + lastmatchlen < end) {
                        while (0 < --lastmatchlen) {
                            this.method.put(++this.searchPos);
                        }

                        searchret = this.method.searchAndPut(++this.searchPos);
                        matchlen = LzssOutputStream.getMatchLen(searchret);
                        matchpos = LzssOutputStream.getMatchPos(searchret);
                        if (this.writtenPos - this.searchPos < matchlen) {
                            matchlen = this.writtenPos - this.searchPos;
                        }
                    } else if (end < this.searchPos + lastmatchlen) {
                        this.putPos = this.searchPos;
                        while (this.putPos < end) {
                            this.method.put(++this.putPos);
                        }
                        this.searchPos += lastmatchlen;
                        this.lastsearchret = LzssOutputStream.NEEDSEARCH;
                        break;
                    } else {
                        this.putPos = this.searchPos;
                        while (this.putPos < end - 1) {
                            this.method.put(++this.putPos);
                        }
                        this.putPos++;
                        this.searchPos += lastmatchlen;
                        this.lastsearchret = this.method.searchAndPut(this.searchPos);
                        break;
                    }
                }
            }
        }

        // Exclusively for flush()
        // Advance only searchPos while keeping putPos as is.
        end = Math.min(this.TextBuffer.length - this.MaxMatch, this.writtenPos);
        if (!last && this.searchPos < end) {
            if (this.lastsearchret == LzssOutputStream.NEEDSEARCH) {
                this.lastsearchret = this.method.search(this.searchPos, this.putPos);
            }
            int searchret = this.lastsearchret;
            int matchlen = LzssOutputStream.getMatchLen(searchret);
            int matchpos = LzssOutputStream.getMatchPos(searchret);
            if (this.writtenPos - this.searchPos < matchlen) {
                matchlen = this.writtenPos - this.searchPos;
            }

            while (this.searchPos < end) {
                int lastmatchlen = matchlen;
                int lastmatchoff = this.searchPos - matchpos - 1;

                searchret = this.method.search(++this.searchPos, this.putPos);
                matchlen = LzssOutputStream.getMatchLen(searchret);
                matchpos = LzssOutputStream.getMatchPos(searchret);
                if (this.writtenPos - this.searchPos < matchlen) {
                    matchlen = this.writtenPos - this.searchPos;
                }

                if (lastmatchlen < matchlen || lastmatchlen < this.Threshold) {
                    this.encoder.writeCode(0xFF & this.TextBuffer[this.searchPos - 1]);
                } else {
                    this.encoder.writeCode(256 + lastmatchlen - this.Threshold);
                    this.encoder.writeOffset(lastmatchoff);

                    this.searchPos += lastmatchlen - 1;
                    searchret = this.method.search(this.searchPos, this.putPos);
                    matchlen = LzssOutputStream.getMatchLen(searchret);
                    matchpos = LzssOutputStream.getMatchPos(searchret);
                    if (this.writtenPos - this.searchPos < matchlen) {
                        matchlen = this.writtenPos - this.searchPos;
                    }
                }
            }
            this.lastsearchret = LzssOutputStream.NEEDSEARCH;
        }
    }

    /**
     * Shifts data in {@link #TextBuffer} up to position forward.
     */
    private void slide() {
        this.DictionaryLimit = Math.max(0, this.DictionaryLimit - this.DictionarySize);

        this.method.slide();

        if (this.lastsearchret != LzssOutputStream.NEEDSEARCH) {
            int matchlen = LzssOutputStream.getMatchLen(this.lastsearchret);
            int matchpos = LzssOutputStream.getMatchPos(this.lastsearchret);
            this.lastsearchret = LzssOutputStream.createSearchReturn(matchlen, matchpos - this.DictionarySize);
        }

        this.writtenPos -= this.DictionarySize;
        this.searchPos -= this.DictionarySize;
        this.putPos -= this.DictionarySize;
        for (int i = this.DictionaryLimit; i < this.writtenPos; i++)
            this.TextBuffer[i] = this.TextBuffer[i + this.DictionarySize];
    }

    /**
     * Generates the return value of search.
     * Although search returns the match position, it is very convenient to return
     * the match length at the same time. Since both match position and match length
     * require few bits, they are exchanged using type {@code int}.
     * This function guarantees unified processing for that purpose.
     * When extracting the match position or match length from the value generated by this function,
     * use {@link #getMatchLen(int)} and {@link #getMatchPos(int)}.
     *
     * @param matchlen match length
     * @param matchpos match position
     * @return return value of search containing match length and match position information
     */
    public static int createSearchReturn(int matchlen, int matchpos) {
        return matchlen << 22 | matchpos;
    }

    /**
     * Extracts the match length from the search return value generated by {@link #createSearchReturn(int, int)}.
     *
     * @param searchret return value of search
     * @return match length
     */
    public static int getMatchLen(int searchret) {
        return searchret >> 22;
    }

    /**
     * Extracts the match position from the search return value generated by {@link #createSearchReturn(int, int)}.
     *
     * @param searchret return value of search
     * @return match position
     */
    public static int getMatchPos(int searchret) {
        if (0 <= searchret)
            return searchret & 0x3FFFFF;
        else
            return -1;
    }
}
