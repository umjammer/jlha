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

import java.io.IOException;
// import classes and interfaces
import java.io.OutputStream;

import jp.gr.java_conf.dangan.io.BitOutputStream;


/**
 * -lh1- 圧縮用の PostLzssEncoder。 <br>
 *
 * <pre>
 * -- revision history --
 * $Log: PostLh1Encoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/07/31 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PostLh1Encoder implements PostLzssEncoder {

    /** 辞書サイズ */
    private static final int DictionarySize = 4096;

    /** 最大一致長 */
    private static final int MaxMatch = 60;

    /** 最小一致長 */
    private static final int Threshold = 3;

    /**
     * -lh1- 形式の圧縮データの出力先の ビット出力ストリーム
     */
    private BitOutputStream out;

    /**
     * Code部圧縮用適応的ハフマン木
     */
    private DynamicHuffman huffman;

    /**
     * offset部の上位6bit圧縮用ハフマン符号の表
     */
    private int[] offHiCode;

    /**
     * offset部の上位6bit圧縮用ハフマン符号長の表
     */
    private int[] offHiLen;

    /**
     * -lh1- 圧縮用 PostLzssEncoder を構築する。
     *
     * @param out 圧縮データを受け取る出力ストリーム
     */
    public PostLh1Encoder(OutputStream out) {
        if (out != null) {
            if (out instanceof BitOutputStream) {
                this.out = (BitOutputStream) out;
            } else {
                this.out = new BitOutputStream(out);
            }
            this.huffman = new DynamicHuffman(314);
            this.offHiLen = PostLh1Encoder.createLenList();
            try {
                this.offHiCode = StaticHuffman.LenListToCodeList(this.offHiLen);
            } catch (BadHuffmanTableException exception) {
            }
        } else {
            throw new NullPointerException("out");
        }
    }

    /**
     * 1byte の LZSS未圧縮のデータもしくは、
     * LZSS で圧縮された圧縮コードのうち一致長を書きこむ。<br>
     *
     * @param code 1byte の LZSS未圧縮のデータもしくは、
     *             LZSS で圧縮された圧縮コードのうち一致長
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void writeCode(int code) throws IOException {
        int node = this.huffman.codeToNode(code);
        int hcode = 0;
        int hlen = 0;
        do {
            hcode >>>= 1;
            hlen++;
            if ((node & 1) != 0)
                hcode |= 0x80000000;

            node = this.huffman.parentNode(node);
        } while (node != DynamicHuffman.ROOT);

        this.out.writeBits(hlen, hcode >> (32 - hlen));
        this.huffman.update(code);
    }

    /**
     * LZSS で圧縮された圧縮コードのうち一致位置を書きこむ。<br>
     *
     * @param offset LZSS で圧縮された圧縮コードのうち一致位置
     */
    @Override
    public void writeOffset(int offset) throws IOException {
        int offHi = (offset >> 6);
        this.out.writeBits(this.offHiLen[offHi], this.offHiCode[offHi]);
        this.out.writeBits(6, offset);
    }

    /**
     * この PostLzssEncoder にバッファリングされている
     * 全ての 8ビット単位のデータを出力先の OutputStream に出力し、
     * 出力先の OutputStream を flush() する。<br>
     * このメソッドは圧縮率を変化させない。
     *
     * @throws IOException 入出力エラーが発生した場合
     * @see PostLzssEncoder#flush()
     * @see BitOutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    /**
     * この出力ストリームと、接続された出力ストリームを閉じ、
     * 使用していたリソースを解放する。<br>
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        this.out.close();

        this.out = null;
        this.huffman = null;
        this.offHiLen = null;
        this.offHiCode = null;
    }

    /**
     * -lh1-形式の LZSS辞書のサイズを得る。
     *
     * @return -lh1-形式の LZSS辞書のサイズ
     */
    @Override
    public int getDictionarySize() {
        return PostLh1Encoder.DictionarySize;
    }

    /**
     * -lh1-形式の LZSSの最大一致長を得る。
     *
     * @return -lz5-形式の LZSSの最大一致長
     */
    @Override
    public int getMaxMatch() {
        return PostLh1Encoder.MaxMatch;
    }

    /**
     * -lh1-形式の LZSSの圧縮、非圧縮の閾値を得る。
     *
     * @return -lh1-形式の LZSSの圧縮、非圧縮の閾値
     */
    @Override
    public int getThreshold() {
        return PostLh1Encoder.Threshold;
    }

    /**
     * -lh1- の offsetデコード用StaticHuffmanの
     * ハフマン符号長リストを生成する。
     *
     * @return -lh1- の offsetデコード用StaticHuffmanの ハフマン符号長リスト
     */
    private static int[] createLenList() {
        final int length = 64;
        int[] list = {
                3, 0x01, 0x04, 0x0C, 0x18, 0x30, 0
        };

        int[] LenList = new int[length];
        int index = 0;
        int len = list[index++];

        for (int i = 0; i < length; i++) {
            if (list[index] == i) {
                len++;
                index++;
            }
            LenList[i] = len;
        }
        return LenList;
    }
}
