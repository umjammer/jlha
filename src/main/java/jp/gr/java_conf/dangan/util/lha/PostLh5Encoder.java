// start of PostLh5Encoder.java
// TEXT_STYLE:CODE=Shift_JIS(Japanese):RET_CODE=CRLF

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

// import exceptions

import java.io.IOException;
// import classes and interfaces
import java.io.OutputStream;
import java.util.Arrays;

import jp.gr.java_conf.dangan.io.BitOutputStream;
import jp.gr.java_conf.dangan.io.Bits;


/**
 * {@link PostLzssEncoder} for -lh4-, -lh5-, -lh6-, and -lh7- compression.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: PostLh5Encoder.java,v $
 * Revision 1.4  2002/12/08 00:00:00  dangan
 * [change]
 *     Changed class name from PostLh5EncoderCombo to PostLh5Encoder.
 *
 * Revision 1.3  2002/12/06 00:00:00  dangan
 * [maintenance]
 *     source maintenance
 *
 * Revision 1.2  2002/12/01 00:00:00  dangan
 * [change]
 *     Changed not to flush the connected {@link OutputStream}
 *     unless flush() is called.
 *
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [bug fix]
 *     In writeOutGroup, this.offLenFreq was used where the local
 *     variable offLenFreq should have been used.
 * [maintenance]
 *     Abolished instance fields buffer, codeFreq, offLenFreq
 *     inherited from PostLh5Encoder.
 *     source maintenance
 *
 * Revision 1.0  2002/07/31 00:00:00  dangan
 * add to version control
 * [improvement]
 *     Reduced the number of patterns to process by introducing DivideNum.
 * [maintenance]
 *     source maintenance
 *     tab removal
 *     fix license statement
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.4 $
 */
public class PostLh5Encoder implements PostLzssEncoder {

    /**
     * Bit output stream to output -lh4-, -lh5-, -lh6-, -lh7- compressed data.
     */
    private BitOutputStream out;

    /**
     * LZSS dictionary size.
     */
    private final int DictionarySize;

    /**
     * LZSS maximum match length.
     */
    private final int MaxMatch;

    /**
     * LZSS compression/non-compression threshold.
     */
    private final int Threshold;

    /**
     * Number of bytes required to indicate dictionary size.
     */
    private final int DictionarySizeByteLen;

    /**
     * Current processing position in {@code this.block[this.currentBlock]}.
     */
    private int position;

    /**
     * Flag in flag byte indicating compression/non-compression.
     */
    private int flagBit;

    /**
     * Position of the flag byte in {@code this.block[this.currentBlock]}.
     */
    private int flagPos;

    /**
     * Indicates the Huffman block currently being processed.
     */
    private int currentBlock;

    /**
     * Buffers for storing Huffman codes.
     */
    private byte[][] block;

    /**
     * Number of code data in each block.
     */
    private final int[] blockSize;

    /**
     * Frequency tables holding code part frequency tables of corresponding blocks.
     */
    private int[][] blockCodeFreq;

    /**
     * Frequency tables holding offLen part frequency tables of corresponding blocks.
     */
    private int[][] blockOffLenFreq;

    /**
     * Array of patterns for dividing all blocks into several groups.
     */
    private int[][] pattern;

    /**
     * Array of groups combining multiple blocks.
     * {@code this.group[0]} has the group containing all blocks,
     * {@code this.group[1]} and {@code this.group[2]} have groups missing the last and first block of all blocks respectively,
     * and so on, constructed in a pyramid shape.
     */
    private int[][] group;

    /**
     * Constructs a {@link PostLzssEncoder} for -lh5- compression.<br>
     * The default buffer size is used.
     *
     * @param out {@link OutputStream} that receives compressed data
     */
    public PostLh5Encoder(OutputStream out) {
        this(out, CompressMethod.LH5);
    }

    /**
     * Constructs a {@link PostLzssEncoder} for -lh4-, -lh5-, -lh6-, and -lh7- compression.<br>
     * The default buffer size is used.
     *
     * @param out    {@link OutputStream} that receives compressed data
     * @param method string indicating compression method: <ul>
     *               <li> {@link CompressMethod#LH4} </li>
     *               <li> {@link CompressMethod#LH5} </li>
     *               <li> {@link CompressMethod#LH6} </li>
     *               <li> {@link CompressMethod#LH7} </li>
     *               </ul> specify one of these.
     * @throws IllegalArgumentException if {@code method} is other than the above
     */
    public PostLh5Encoder(OutputStream out, String method) {
        this(out, method, 16384);
    }

    /**
     * Constructs a {@link PostLzssEncoder} for -lh4-, -lh5-, -lh6-, and -lh7- compression.<br>
     *
     * @param out        {@link OutputStream} that receives compressed data
     * @param method     string indicating compression method: <ul>
     *                   <li> {@link CompressMethod#LH4} </li>
     *                   <li> {@link CompressMethod#LH5} </li>
     *                   <li> {@link CompressMethod#LH6} </li>
     *                   <li> {@link CompressMethod#LH7} </li>
     *                   </ul> specify one of these.
     * @param BufferSize size of the buffer to store LZSS compressed data
     * @throws IllegalArgumentException <ol>
     *                                  <li> (1) if {@code method} is other than the above</li>
     *                                  <li> (2) if {@code BufferSize} is too small</li>
     *                                  </ol> either case.
     */
    public PostLh5Encoder(OutputStream out, String method, int BufferSize) {
        this(out, method, 1, BufferSize, 0);
    }

    /**
     * Constructs a {@link PostLzssEncoder} for -lh4-, -lh5-, -lh6-, and -lh7- compression.<br>
     * Combines {@code BlockNum} blocks, each of {@code BlockSize} bytes,
     * and outputs them in the configuration that produces the fewest output bits.
     * All patterns obtained by dividing all blocks into {@code DivideNum + 1} are tried.
     *
     * @param out       {@link OutputStream} that receives compressed data
     * @param method    string indicating compression method: <ul>
     *                  <li> {@link CompressMethod#LH4} </li>
     *                  <li> {@link CompressMethod#LH5} </li>
     *                  <li> {@link CompressMethod#LH6} </li>
     *                  <li> {@link CompressMethod#LH7} </li>
     *                  </ul> specify one of these.
     * @param BlockNum  number of blocks
     * @param BlockSize number of bytes in 1 block
     * @param DivideNum maximum number of divisions
     * @throws IllegalArgumentException <ol>
     *                                  <li> if {@code CompressMethod} is other than the above</li>
     *                                  <li> if {@code BlockNum} is 0 or less</li>
     *                                  <li> if {@code BlockSize} is too small</li>
     *                                  <li> if {@code DivideNum} is less than 0, or {@code BlockNum} or more</li>
     *                                  </ol> any of these.
     */
    public PostLh5Encoder(OutputStream out, String method, int BlockNum, int BlockSize, int DivideNum) {

        if (CompressMethod.LH4.equals(method) || CompressMethod.LH5.equals(method) || CompressMethod.LH6.equals(method)
                || CompressMethod.LH7.equals(method)) {

            this.DictionarySize = CompressMethod.toDictionarySize(method);
            this.MaxMatch = CompressMethod.toMaxMatch(method);
            this.Threshold = CompressMethod.toThreshold(method);
            this.DictionarySizeByteLen = (Bits.len(this.DictionarySize - 1) + 7) / 8;

            int MinCapacity = (DictionarySizeByteLen + 1) * 8 + 1;

            if (out != null && 0 < BlockNum && 0 <= DivideNum && DivideNum < BlockNum && MinCapacity <= BlockSize) {

                if (out instanceof BitOutputStream) {
                    this.out = (BitOutputStream) out;
                } else {
                    this.out = new BitOutputStream(out);
                }

                this.currentBlock = 0;
                this.block = new byte[BlockNum][];
                this.blockSize = new int[BlockNum];
                this.blockCodeFreq = new int[BlockNum][];
                this.blockOffLenFreq = new int[BlockNum][];

                int codeFreqSize = 256 + this.MaxMatch - this.Threshold + 1;
                int offLenFreqSize = Bits.len(this.DictionarySize);
                for (int i = 0; i < BlockNum; i++) {
                    this.block[i] = new byte[BlockSize];
                    this.blockCodeFreq[i] = new int[codeFreqSize];
                    this.blockOffLenFreq[i] = new int[offLenFreqSize];
                }

                this.group = PostLh5Encoder.createGroup(BlockNum, DivideNum);
                this.pattern = PostLh5Encoder.createPattern(BlockNum, DivideNum);

                this.position = 0;
                this.flagBit = 0;
                this.flagPos = 0;

            } else if (out == null) {
                throw new NullPointerException("out");
            } else if (BlockNum <= 0) {
                throw new IllegalArgumentException("BlockNum too small. BlockNum must be 1 or more.");
            } else if (DivideNum < 0 || BlockNum <= DivideNum) {
                throw new IllegalArgumentException("DivideNum out of bounds( 0 to BlockNum - 1(" + (BlockNum - 1) + ") ).");
            } else {
                throw new IllegalArgumentException("BlockSize too small. BlockSize must be larger than " + MinCapacity);
            }

        } else if (method == null) {
            throw new NullPointerException("method");
        } else {
            throw new IllegalArgumentException("Unknown compress method. " + method);
        }
    }

    /**
     * Writes 1 byte of uncompressed LZSS data or
     * the match length of LZSS compressed code.
     *
     * @param code 1 byte of uncompressed LZSS data or the match length of LZSS compressed code
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCode(int code) throws IOException {
        int need = ((0x100 <= code ? this.DictionarySizeByteLen + 1 : 1) + (this.flagBit == 0 ? 1 : 0));

        if (this.block[this.currentBlock].length - this.position < need || 65535 <= this.blockSize[this.currentBlock]) {

            this.currentBlock++;
            if (this.block.length <= this.currentBlock) {
                this.writeOut();
            } else {
                this.position = 0;
            }

            this.flagBit = 0x80;
            this.flagPos = this.position++;
            this.block[this.currentBlock][this.flagPos] = 0;
        } else if (this.flagBit == 0) {
            this.flagBit = 0x80;
            this.flagPos = this.position++;
            this.block[this.currentBlock][this.flagPos] = 0;
        }

        // Store data
        this.block[this.currentBlock][this.position++] = (byte) code;

        // Store upper 1 bit as a flag
        if (0x100 <= code) {
            this.block[this.currentBlock][this.flagPos] |= this.flagBit;
        }
        this.flagBit >>= 1;

        // Update frequency table
        this.blockCodeFreq[this.currentBlock][code]++;

        // Update block size
        this.blockSize[this.currentBlock]++;
    }

    /**
     * Writes the match offset of LZSS compressed code.<br>
     *
     * @param offset the match offset of LZSS compressed code
     */
    @Override
    public void writeOffset(int offset) {
        // Store data
        int shift = (this.DictionarySizeByteLen - 1) << 3;
        while (0 <= shift) {
            this.block[this.currentBlock][this.position++] = (byte) (offset >> shift);
            shift -= 8;
        }

        // Update frequency table
        this.blockOffLenFreq[this.currentBlock][Bits.len(offset)]++;
    }

    /**
     * Flushes all 8-bit unit data buffered in this {@link PostLzssEncoder}
     * to the output destination {@link OutputStream},
     * and calls {@code flush()} on the destination {@link OutputStream}.<br>
     * This method changes the compression ratio.
     *
     * @throws IOException if an I/O error occurs
     * @see PostLzssEncoder#flush()
     * @see BitOutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        this.writeOut();
        this.out.flush();
    }

    /**
     * Closes this output stream and the connected output stream,
     * and releases any resources used.<br>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        this.writeOut();
        this.out.close();

        this.out = null;
        this.block = null;
        this.blockCodeFreq = null;
        this.blockOffLenFreq = null;
        this.group = null;
        this.pattern = null;
    }

    /**
     * Gets the LZSS dictionary size handled by this {@link PostLh5Encoder}.
     *
     * @return the LZSS dictionary size handled by this {@link PostLh5Encoder}
     */
    @Override
    public int getDictionarySize() {
        return this.DictionarySize;
    }

    /**
     * Gets the maximum match length handled by this {@link PostLh5Encoder}.
     *
     * @return the maximum match length handled by this {@link PostLh5Encoder}
     */
    @Override
    public int getMaxMatch() {
        return this.MaxMatch;
    }

    /**
     * Gets the threshold for LZSS compression/non-compression handled by this {@link PostLh5Encoder}.
     *
     * @return the threshold for LZSS compression/non-compression handled by this {@link PostLh5Encoder}
     */
    @Override
    public int getThreshold() {
        return this.Threshold;
    }

    /**
     * Outputs all buffered data to {@code this.out}.
     *
     * @throws IOException if an I/O error occurs
     */
    private void writeOut() throws IOException {
        if (1 < this.block.length) {
            this.writeOutBestPattern();
        } else {
            this.writeOutGroup(new int[] {
                    0
            });
            this.currentBlock = 0;
        }

        this.position = 0;
        this.flagBit = 0;
    }

    /**
     * Outputs all buffered data to {@code this.out} in the best configuration.<br>
     *
     * @throws IOException if an I/O error occurs
     */
    private void writeOutBestPattern() throws IOException {
        int[] bestPattern = null;
        int[] groupHuffLen = new int[this.group.length];

        // ----
        // Calculate the number of bits when outputting group.
        for (int i = 0; i < this.group.length; i++) {
            if (this.group != null) {
                int blockSize = 0;
                for (int j = 0; j < this.group[i].length; j++) {
                    blockSize += this.blockSize[this.group[i][j]];
                }
                if (0 < blockSize && blockSize < 65536) {
                    groupHuffLen[i] = PostLh5Encoder
                            .calcHuffmanCodeLength(this.DictionarySize,
                                     PostLh5Encoder.margeArrays(this.group[i], this.blockCodeFreq),
                                     PostLh5Encoder.margeArrays(this.group[i], this.blockOffLenFreq));
                } else if (0 == blockSize) {
                    groupHuffLen[i] = 0;
                } else {
                    groupHuffLen[i] = -1;
                }
            } else {
                groupHuffLen[i] = -1;
            }
        }

        // ----
        // Find the pattern with the minimum number of output bits by brute force.
        int smallest = Integer.MAX_VALUE;
        for (int[] ints : this.pattern) {
            int length = 0;

            for (int anInt : ints) {
                if (0 <= groupHuffLen[anInt]) {
                    length += groupHuffLen[anInt];
                } else {
                    length = Integer.MAX_VALUE;
                    break;
                }
            }
            if (length < smallest) {
                bestPattern = ints;
                smallest = length;
            }
        }

        // ----
        // Output in the pattern with the smallest number of output bits.
        // If all patterns have a group with a block size of 65536 or more,
        // output in block units.
        if (bestPattern != null) {
            for (int j : bestPattern) {
                this.writeOutGroup(this.group[j]);
            }
        } else {
            for (int i = 0; i < this.block.length; i++) {
                this.writeOutGroup(new int[] {
                        i
                });
            }
        }

        this.currentBlock = 0;
    }

    /**
     * Huffman-encodes the blocks specified by {@code group} and outputs them to {@code this.out}.<br>
     *
     * @param group array holding block numbers to output
     * @throws IOException if an I/O error occurs
     */
    private void writeOutGroup(int[] group) throws IOException {
        int[] codeFreq = PostLh5Encoder.margeArrays(group, this.blockCodeFreq);
        int[] offLenFreq = PostLh5Encoder.margeArrays(group, this.blockOffLenFreq);

        int blockSize = 0;
        for (int item : group) {
            blockSize += this.blockSize[item];
        }

        if (0 < blockSize) {
            // ----
            // Output block size
            this.out.writeBits(16, blockSize);

            // ----
            // Generate Huffman code table
            int[] codeLen = StaticHuffman.FreqListToLenList(codeFreq);
            int[] codeCode = StaticHuffman.LenListToCodeList(codeLen);
            int[] offLenLen = StaticHuffman.FreqListToLenList(offLenFreq);
            int[] offLenCode = StaticHuffman.LenListToCodeList(offLenLen);

            // ----
            // Output Huffman code table for code part
            if (2 <= PostLh5Encoder.countNoZeroElement(codeFreq)) {
                int[] codeLenFreq = PostLh5Encoder.createCodeLenFreq(codeLen);
                int[] codeLenLen = StaticHuffman.FreqListToLenList(codeLenFreq);
                int[] codeLenCode = StaticHuffman.LenListToCodeList(codeLenLen);

                if (2 <= PostLh5Encoder.countNoZeroElement(codeLenFreq)) {
                    this.writeCodeLenLen(codeLenLen);
                } else {
                    this.out.writeBits(5, 0);
                    this.out.writeBits(5, PostLh5Encoder.getNoZeroElementIndex(codeLenFreq));
                }
                this.writeCodeLen(codeLen, codeLenLen, codeLenCode);
            } else {
                this.out.writeBits(10, 0);
                this.out.writeBits(18, PostLh5Encoder.getNoZeroElementIndex(codeFreq));
            }

            // ----
            // Output Huffman code table for offLen part
            if (2 <= PostLh5Encoder.countNoZeroElement(offLenFreq)) {
                this.writeOffLenLen(offLenLen);
            } else {
                int len = Bits.len(Bits.len(this.DictionarySize));
                this.out.writeBits(len, 0);
                this.out.writeBits(len, PostLh5Encoder.getNoZeroElementIndex(offLenFreq));
            }

            // ----
            // Output Huffman code
            for (int value : group) {
                this.position = 0;
                this.flagBit = 0;
                byte[] buffer = this.block[value];

                for (int j = 0; j < this.blockSize[value]; j++) {
                    if (this.flagBit == 0) {
                        this.flagBit = 0x80;
                        this.flagPos = this.position++;
                    }
                    if (0 == (buffer[this.flagPos] & this.flagBit)) {
                        int code = buffer[this.position++] & 0xFF;
                        this.out.writeBits(codeLen[code], codeCode[code]);
                    } else {
                        int code = (buffer[this.position++] & 0xFF) | 0x100;
                        int offset = 0;
                        for (int k = 0; k < this.DictionarySizeByteLen; k++) {
                            offset = (offset << 8) | (buffer[this.position++] & 0xFF);
                        }
                        int offlen = Bits.len(offset);
                        this.out.writeBits(codeLen[code], codeCode[code]);
                        this.out.writeBits(offLenLen[offlen], offLenCode[offlen]);
                        if (1 < offlen)
                            this.out.writeBits(offlen - 1, offset);
                    }
                    this.flagBit >>= 1;
                }
            }

            // ----
            // Process for next block
            for (int k : group) {
                this.blockSize[k] = 0;

                codeFreq = this.blockCodeFreq[k];
                Arrays.fill(codeFreq, 0);

                offLenFreq = this.blockOffLenFreq[k];
                Arrays.fill(offLenFreq, 0);
            }
        }
    }

    /**
     * Writes the list of Huffman code lengths of {@code codeLenLen}.
     *
     * @param codeLenLen list of Huffman code lengths of {@code codeLenFreq}
     * @throws IOException if an I/O error occurs
     */
    private void writeCodeLenLen(int[] codeLenLen) throws IOException {
        int end = codeLenLen.length;
        while (0 < end && codeLenLen[end - 1] == 0) {
            end--;
        }

        this.out.writeBits(5, end);
        int index = 0;
        while (index < end) {
            int len = codeLenLen[index++];
            if (len <= 6)
                this.out.writeBits(3, len);
            else
                this.out.writeBits(len - 3, (1 << (len - 3)) - 2);

            if (index == 3) {
                while (codeLenLen[index] == 0 && index < 6) {
                    index++;
                }
                this.out.writeBits(2, (index - 3) & 0x03);
            }
        }
    }

    /**
     * Writes the list of Huffman code lengths for the code part while
     * encoding them with Huffman and run-length.
     *
     * @param codeLen     list of Huffman code lengths of {@code codeFreq}
     * @param codeLenLen  list of Huffman code lengths of {@code codeLenFreq}
     * @param codeLenCode list of Huffman codes of {@code codeLenFreq}
     * @throws IOException if an I/O error occurs
     */
    private void writeCodeLen(int[] codeLen, int[] codeLenLen, int[] codeLenCode) throws IOException {
        int end = codeLen.length;
        while (0 < end && codeLen[end - 1] == 0) {
            end--;
        }

        this.out.writeBits(9, end);
        int index = 0;
        while (index < end) {
            int len = codeLen[index++];

            if (0 < len) {
                this.out.writeBits(codeLenLen[len + 2], codeLenCode[len + 2]);
            } else {
                int count = 1;
                while (codeLen[index] == 0 && index < end) {
                    count++;
                    index++;
                }

                if (count <= 2) {
                    for (int i = 0; i < count; i++)
                        this.out.writeBits(codeLenLen[0], codeLenCode[0]);
                } else if (count <= 18) {
                    this.out.writeBits(codeLenLen[1], codeLenCode[1]);
                    this.out.writeBits(4, count - 3);
                } else if (count == 19) {
                    this.out.writeBits(codeLenLen[0], codeLenCode[0]);
                    this.out.writeBits(codeLenLen[1], codeLenCode[1]);
                    this.out.writeBits(4, 0x0F);
                } else {
                    this.out.writeBits(codeLenLen[2], codeLenCode[2]);
                    this.out.writeBits(9, count - 20);
                }
            }
        }
    }

    /**
     * Writes the list of Huffman code lengths of {@code offLenLen}.
     *
     * @param offLenLen list of Huffman code lengths of {@code offLenFreq}
     * @throws IOException if an I/O error occurs
     */
    private void writeOffLenLen(int[] offLenLen) throws IOException {
        int end = offLenLen.length;
        while (0 < end && offLenLen[end - 1] == 0) {
            end--;
        }

        int len = Bits.len(Bits.len(this.DictionarySize));
        this.out.writeBits(len, end);
        int index = 0;
        while (index < end) {
            len = offLenLen[index++];
            if (len <= 6)
                this.out.writeBits(3, len);
            else
                this.out.writeBits(len - 3, (1 << (len - 3)) - 2);
        }
    }

    /**
     * Gets the number of non-zero elements in an array.
     *
     * @param array array
     * @return the number of non-zero elements in the array
     */
    private static int countNoZeroElement(int[] array) {
        int count = 0;
        for (int j : array) {
            if (0 != j) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the index of the first non-zero element in an array.
     *
     * @param array array
     * @return the index of the first non-zero element in the array.
     * If all elements are 0, returns 0.
     */
    private static int getNoZeroElementIndex(int[] array) {
        for (int i = 0; i < array.length; i++) {
            if (0 != array[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Concatenates the arrays specified by {@code indexes} from {@code arrays}.
     *
     * @param indexes array of indices indicating the target arrays in {@code arrays}
     * @param arrays  list containing target arrays
     */
    private static int[] margeArrays(int[] indexes, int[][] arrays) {
        if (1 < indexes.length) {
            int[] array = new int[arrays[0].length];

            for (int index : indexes) {
                int[] src = arrays[index];

                for (int j = 0; j < src.length; j++) {
                    array[j] += src[j];
                }
            }
            return array;
        } else {
            return arrays[indexes[0]];
        }
    }

    /**
     * Creates a frequency table for encoding {@code codeLen} using run-length and Huffman.<br>
     * The created frequency table includes special elements:
     * {@code codeLenFreq[0]} indicates that there is 1 element of value 0 to skip,
     * {@code codeLenFreq[1]} indicates that there are 3 to 18 elements of value 0 to skip by looking at the subsequent 5-bit data,
     * {@code codeLenFreq[2]} indicates that there are 20 or more elements of value 0 to skip by looking at the subsequent 9-bit data.
     * The standard frequencies are placed at positions offset by +2.
     *
     * @param codeLen list of Huffman code lengths of {@code codeFreq}
     * @return frequency table of {@code codeLen}
     */
    private static int[] createCodeLenFreq(int[] codeLen) {
        int[] codeLenFreq = new int[StaticHuffman.LimitLen + 3];

        int end = codeLen.length;
        while (0 < end && codeLen[end - 1] == 0) {
            end--;
        }

        int index = 0;
        while (index < end) {
            int len = codeLen[index++];

            if (0 < len) {
                codeLenFreq[len + 2]++;
            } else {
                int count = 1;
                while (codeLen[index] == 0 && index < end) {
                    count++;
                    index++;
                }

                if (count <= 2) {
                    codeLenFreq[0] += count;
                } else if (count <= 18) {
                    codeLenFreq[1]++;
                } else if (count == 19) {
                    codeLenFreq[0]++;
                    codeLenFreq[1]++;
                } else {
                    codeLenFreq[2]++;
                }
            }
        }
        return codeLenFreq;
    }

    /**
     * Gets the number of bits when outputting Huffman codes with the specified frequency information.
     *
     * @param DictionarySize LZSS dictionary size
     * @param codeFreq       frequency information of code part
     * @param offLenFreq     length frequency information of offset part
     * @return the number of bits when outputting Huffman codes with this frequency information
     */
    private static int calcHuffmanCodeLength(int DictionarySize, int[] codeFreq, int[] offLenFreq) {

        // ----
        // Initialization
        int length = 0;
        @SuppressWarnings("unused")
        int[] codeLen, codeCode, offLenLen;
        try {
            codeLen = StaticHuffman.FreqListToLenList(codeFreq);
            codeCode = StaticHuffman.LenListToCodeList(codeLen);
            offLenLen = StaticHuffman.FreqListToLenList(offLenFreq);
        } catch (BadHuffmanTableException exception) { // Never happens
            throw new Error("caught the BadHuffmanTableException which should be never thrown.");
        }

        // ----
        // Calculate the length of the Huffman frequency table for code part.
        length += 16;
        if (2 <= PostLh5Encoder.countNoZeroElement(codeFreq)) {
            int[] codeLenFreq = PostLh5Encoder.createCodeLenFreq(codeLen);
            int[] codeLenLen = StaticHuffman.FreqListToLenList(codeLenFreq);
            if (2 <= PostLh5Encoder.countNoZeroElement(codeLenFreq)) {
                length += PostLh5Encoder.calcCodeLenLen(codeLenLen);
            } else {
                length += 5;
                length += 5;
            }
            length += PostLh5Encoder.calcCodeLen(codeLen, codeLenLen);
        } else {
            length += 10;
            length += 18;
        }

        // ----
        // Calculate the length of the Huffman frequency table for offLen part.
        if (2 <= PostLh5Encoder.countNoZeroElement(offLenFreq)) {
            length += PostLh5Encoder.calcOffLenLen(DictionarySize, offLenLen);
        } else {
            int len = Bits.len(Bits.len(DictionarySize));
            length += len;
            length += len;
        }

        // ----
        // Calculate the length of LZSS compressed data further Huffman-encoded.
        for (int i = 0; i < codeFreq.length; i++) {
            length += codeFreq[i] * codeLen[i];
        }
        for (int i = 0; i < offLenFreq.length; i++) {
            length += offLenFreq[i] * (offLenLen[i] + i - 1);
        }
        return length;
    }

    /**
     * Gets the number of bits when outputting the specified Huffman code length table.
     *
     * @param codeLenLen table of Huffman code lengths for code part further Huffman-encoded
     * @return the number of bits when outputting the specified Huffman code length table
     */
    private static int calcCodeLenLen(int[] codeLenLen) {
        int length = 0;
        int end = codeLenLen.length;
        while (0 < end && codeLenLen[end - 1] == 0) {
            end--;
        }

        length += 5;

        int index = 0;
        while (index < end) {
            int len = codeLenLen[index++];
            if (len <= 6)
                length += len;
            else
                length += len - 3;

            if (index == 3) {
                while (codeLenLen[index] == 0 && index < 6) {
                    index++;
                }
                length += 2;
            }
        }
        return length;
    }

    /**
     * Gets the number of bits when outputting the specified Huffman code length table.
     *
     * @param codeLen    table of Huffman code lengths for code part
     * @param codeLenLen table of Huffman code lengths for code part further Huffman-encoded
     * @return the number of bits when outputting the specified Huffman code length table
     */
    private static int calcCodeLen(int[] codeLen, int[] codeLenLen) {
        int length = 0;
        int end = codeLen.length;
        while (0 < end && codeLen[end - 1] == 0) {
            end--;
        }

        length += 9;

        int index = 0;
        while (index < end) {
            int len = codeLen[index++];

            if (0 < len) {
                length += codeLenLen[len + 2];
            } else {
                int count = 1;
                while (codeLen[index] == 0 && index < end) {
                    count++;
                    index++;
                }

                if (count <= 2) {
                    for (int i = 0; i < count; i++)
                        length += codeLenLen[0];
                } else if (count <= 18) {
                    length += codeLenLen[1];
                    length += 4;
                } else if (count == 19) {
                    length += codeLenLen[0];
                    length += codeLenLen[1];
                    length += 4;
                } else {
                    length += codeLenLen[2];
                    length += 9;
                }
            }
        }
        return length;
    }

    /**
     * Gets the number of bits when outputting the specified Huffman code length table.
     *
     * @param DictionarySize LZSS dictionary size
     * @param offLenLen      table of Huffman code lengths of offset part lengths
     * @return the number of bits when outputting the specified Huffman code length table
     */
    private static int calcOffLenLen(int DictionarySize, int[] offLenLen) {
        int length = 0;
        int end = offLenLen.length;
        while (0 < end && offLenLen[end - 1] == 0) {
            end--;
        }

        length += Bits.len(Bits.len(DictionarySize));

        int index = 0;
        while (index < end) {
            int len = offLenLen[index++];
            if (len <= 6)
                length += 3;
            else
                length += len - 3;
        }
        return length;
    }

    /**
     * Returns a list of groups formed by grouping {@code BlockNum} blocks into contiguous blocks.
     *
     * <pre>
     * group = new int[] {
     *     0, 1, 2
     * }
     * </pre>
     * <p>
     * indicates a group consisting of {@code block[0]}, {@code block[1]}, and {@code block[2]}.
     * Also, groups are generated with regularity in a pyramid shape:
     * {@code group[0]} is a group consisting of all blocks,
     * {@code group[1]} and {@code group[2]} are groups missing the last and first block of all blocks respectively,
     * and so on. Note that {@code createPattern} relies on this regularity when modifying this method.
     * Also note that unused groups contain {@code null}.
     *
     * @param BlockNum  number of blocks
     * @param DivideNum maximum number of divisions
     * @return list of generated groups
     */
    private static int[][] createGroup(int BlockNum, int DivideNum) {
        int[][] group = new int[(BlockNum + 1) * BlockNum / 2][];

        if (DivideNum == 0) {
            // ----
            // Generate only group containing all blocks
            group[0] = new int[BlockNum];
            for (int i = 0; i < BlockNum; i++) {
                group[0][i] = i;
            }
        } else if (2 < BlockNum && DivideNum == 1) {
            // ----
            // Generate only first and last groups of the same size.
            int index = 0;
            for (int size = BlockNum; 0 < size; size--) {
                group[index] = new int[size];
                for (int i = 0; i < size; i++) {
                    group[index][i] = i;
                }
                if (size < BlockNum) {
                    index += BlockNum - size;
                    group[index] = new int[size];
                    for (int i = 0; i < size; i++) {
                        group[index][i] = i + BlockNum - size;
                    }
                }
                index++;
            }
        } else {
            // ----
            // Generate all groups.
            int index = 0;
            for (int size = BlockNum; 0 < size; size--) {
                for (int start = 0; size + start <= BlockNum; start++) {
                    group[index] = new int[size];

                    for (int i = 0; i < size; i++) {
                        group[index][i] = start + i;
                    }
                    index++;
                }
            }
        }
        return group;
    }

    /**
     * Generates a table of patterns when dividing {@code BlockNum} blocks into at most {@code DivideNum + 1} regions.
     * A single pattern is indicated by enumerating indices to the group array generated by {@code createGroup}.
     *
     * <pre>
     * pattern = new int[] {
     *     1, 3
     * };
     * </pre>
     * <p>
     * Such a pattern indicates division between {@code group[1]} and {@code group[3]}.
     *
     * @param BlockNum  number of blocks
     * @param DivideNum maximum number of divisions
     * @return list of generated patterns
     */
    private static int[][] createPattern(int BlockNum, int DivideNum) {
        int index = 0;
        int patternNum = PostLh5Encoder.calcPatternNum(BlockNum, DivideNum);
        int[][] pattern = new int[patternNum][];

        for (int div = 0; div < Math.min(BlockNum, DivideNum + 1); div++) {
            // Array holding division positions.
            // Value in the array, e.g. 0, means dividing between Block[0] and Block[1].
            int[] divPos = new int[div];
            for (int i = 0; i < divPos.length; i++) {
                divPos[i] = i;
            }

            // Loop to generate patterns with the same number of divisions.
            // 'more' indicates that more patterns can still be generated with this number of divisions.
            boolean more;
            do {
                pattern[index] = new int[div + 1];

                int start = 0;
                for (int i = 0; i < divPos.length; i++) {
                    int len = (divPos[i] - start) + 1;
                    int num = BlockNum - len;
                    pattern[index][i] = (num + 1) * num / 2 + start;
                    start += len;
                }
                int num = BlockNum - (BlockNum - start);
                pattern[index][divPos.length] = (num + 1) * num / 2 + start;
                index++;

                // Move division position. If division position can be moved,
                // it can be determined that there are still patterns that can be output with this number of divisions.
                more = false;
                int move = divPos.length - 1;
                int range = BlockNum - 2;
                while (0 <= move && !more) {
                    if (divPos[move] < range) {
                        divPos[move]++;
                        if (move < divPos.length - 1) {
                            for (int i = move; i < divPos.length - 1; i++)
                                divPos[i + 1] = divPos[i] + 1;
                        }
                        more = true;
                    }
                    range = divPos[move] - 1;
                    move--;
                }
            } while (more);
        }
        return pattern;
    }

    /**
     * Gets how many patterns can be formed when dividing {@code BlockNum} blocks
     * into at most {@code DivideNum + 1} contiguous regions.
     *
     * @param BlockNum  number of blocks
     * @param DivideNum number of divisions
     * @return number of patterns
     */
    private static int calcPatternNum(int BlockNum, int DivideNum) {
        int patternNum = 0;
        for (int div = 0; div <= DivideNum; div++) {
            int count = (div <= (BlockNum / 2) ? div : BlockNum - 1 - div);

            int numerator = 1;
            for (int i = 1; i <= count; i++) {
                numerator *= (BlockNum - i);
            }

            int denominator = 1;
            for (int i = 1; i <= count; i++) {
                denominator *= i;
            }

            patternNum += numerator / denominator;
        }
        return patternNum;
    }
}
