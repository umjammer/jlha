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
import java.io.IOException;
import java.io.InputStream;

import jp.gr.java_conf.dangan.io.BitDataBrokenException;
import jp.gr.java_conf.dangan.io.BitInputStream;
import jp.gr.java_conf.dangan.io.Bits;
import jp.gr.java_conf.dangan.io.NotEnoughBitsException;


/**
 * -lh1- 解凍用の PreLzssDecoder。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: PreLh1Decoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     available の計算が甘かったのを修正。
 * [maintenance]
 *     ソース整備
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PreLh1Decoder implements PreLzssDecoder {

    /** 辞書サイズ */
    private static final int DictionarySize = 4096;

    /** 最大一致長 */
    private static final int MaxMatch = 60;

    /** 最小一致長 */
    private static final int Threshold = 3;

    /**
     * -lh1- の圧縮データを供給する BitInputStream
     */
    BitInputStream in;

    /**
     * code部復号用の動的ハフマン木
     */
    DynamicHuffman huffman;

    /**
     * オフセット部の上位6bit復号用
     * ハフマン符号長リスト。
     */
    int[] offHiLen;

    /**
     * オフセット部の上位6bit復号用テーブル。
     */
    short[] offHiTable;

    /**
     * オフセット部の上位6bit復号用テーブルを引くのに必要なbit数。
     */
    int offHiTableBits;

    /** huffman のバックアップ用 */
    DynamicHuffman markHuffman;

    /**
     * -lh1- 解凍用 PreLzssDecoder を構築する。
     *
     * @param in -lh1- で圧縮されたデータを供給する入力ストリーム
     */
    public PreLh1Decoder(InputStream in) {
        if (in != null) {
            if (in instanceof BitInputStream) {
                this.in = (BitInputStream) in;
            } else {
                this.in = new BitInputStream(in);
            }
            this.huffman = new DynamicHuffman(314);
            this.markHuffman = null;

            this.offHiLen = PreLh1Decoder.createLenList();
            try {
                this.offHiTable = StaticHuffman.createTable(this.offHiLen);
            } catch (BadHuffmanTableException exception) {
            }
            this.offHiTableBits = Bits.len(this.offHiTable.length - 1);
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * -lh1- で圧縮された
     * 1byte のLZSS未圧縮のデータ、
     * もしくは圧縮コードのうち一致長を読み込む。
     *
     * @return 1byte の 未圧縮のデータもしくは、圧縮された圧縮コードのうち一致長
     * @throws IOException  入出力エラーが発生した場合
     * @throws EOFException EndOfStreamに達した場合
     */
    @Override
    public int readCode() throws IOException {
        int node = this.huffman.childNode(DynamicHuffman.ROOT);
        while (0 <= node) {
            node = this.huffman.childNode(node - (in.readBoolean() ? 1 : 0));
        }
        int code = ~node;
        this.huffman.update(code);
        return code;
    }

    /**
     * -lh1- で圧縮された
     * LZSS圧縮コードのうち一致位置を読み込む。
     *
     * @return -lh1- で圧縮された圧縮コードのうち一致位置
     * @throws IOException            入出力エラーが発生した場合。
     * @throws EOFException           データが途中までしかないため予期せぬ EndOfStream に到達した場合。
     * @throws BitDataBrokenException データが途中までしかないため予期せぬ EndOfStream に到達したか、他の入出力エラーが発生した。
     * @throws NotEnoughBitsException データが途中までしかないため予期せぬ EndOfStream に到達したか、他の入出力エラーが発生した。
     */
    @Override
    public int readOffset() throws IOException {
        // offHiをあらわすのに最短の場合は 0 の 3bit で
        // offHiTableBits は 8bitで 両者の差は 5bit。
        // そのため、下位6bitを読み込む事を加味すると
        // 正常なデータでは peekBits が
        // NotEnoughBitsException を投げることは無い。
        int offHi = this.offHiTable[this.in.peekBits(this.offHiTableBits)];
        this.in.skipBits(this.offHiLen[offHi]);

        return (offHi << 6) | this.in.readBits(6);
    }

    /**
     * 接続された入力ストリームの現在位置にマークを設定し、
     * reset() メソッドでマークした時点の 読み込み位置に
     * 戻れるようにする。<br>
     * InputStream の mark() と違い、readLimit で設定した
     * 限界バイト数より前にマーク位置が無効になる可能性が
     * ある事に注意すること。
     *
     * @param readLimit マーク位置に戻れる限界のバイト数。
     *                  このバイト数を超えてデータを読み込んだ場合 reset()できなくなる可能性がある。
     * @see PreLzssDecoder#mark(int)
     */
    @Override
    public void mark(int readLimit) {
        this.in.mark(readLimit * 18 / 8 + 4);
        this.markHuffman = (DynamicHuffman) this.huffman.clone();
    }

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void reset() throws IOException {
        // mark()しないで reset() しようとした場合、
        // readLimit を超えて reset() しようとした場合、
        // 接続された InputStream が markSupported() で false を返す場合は
        // BitInputStream が IOException を投げる。
        this.in.reset();

        this.huffman = (DynamicHuffman) this.markHuffman.clone();
    }

    /**
     * 接続された入力ストリームが mark() と reset() をサポートするかを得る。
     *
     * @return ストリームが mark() と reset() をサポートする場合は true。サポートしない場合は false。
     */
    @Override
    public boolean markSupported() {
        return this.in.markSupported();
    }

    /**
     * ブロックせずに読み出すことの出来る最低バイト数を得る。<br>
     * InputStream の available() と違い、
     * この最低バイト数は必ずしも保障されていない事に注意すること。
     *
     * @return ブロックしないで読み出せる最低バイト数。
     * @throws IOException 入出力エラーが発生した場合
     * @see PreLzssDecoder#available()
     */
    @Override
    public int available() throws IOException {
        return Math.max(this.in.availableBits() / 18 - 4, 0);
    }

    /**
     * このストリームを閉じ、使用していた全ての資源を解放する。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        this.in.close();

        this.in = null;
        this.huffman = null;
        this.markHuffman = null;

        this.offHiLen = null;
        this.offHiTable = null;
        this.offHiTableBits = 0;
    }

    /**
     * -lh1-形式のLZSS辞書のサイズを得る。
     *
     * @return -lh1-形式のLZSS辞書のサイズ
     */
    @Override
    public int getDictionarySize() {
        return PreLh1Decoder.DictionarySize;
    }

    /**
     * -lh1-形式のLZSSの最大一致長を得る。
     *
     * @return -lh1-形式のLZSSの最大一致長
     */
    @Override
    public int getMaxMatch() {
        return PreLh1Decoder.MaxMatch;
    }

    /**
     * -lh1-形式のLZSSの圧縮、非圧縮の閾値を得る。
     *
     * @return -lh1-形式のLZSSの圧縮、非圧縮の閾値
     */
    @Override
    public int getThreshold() {
        return PreLh1Decoder.Threshold;
    }

    /**
     * -lh1- の offsetデコード用StaticHuffmanの
     * ハフマン符号長リストを生成する。
     *
     * @return -lh1- の offsetデコード用StaticHuffmanの
     * ハフマン符号長リスト
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
