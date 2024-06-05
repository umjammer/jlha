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


/**
 * -lh2- 解凍用 PreLzssDecoder。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: PreLh2Decoder.java,v $
 * Revision 1.1  2002/12/06 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * [bug fix]
 *     available() の計算が甘かったのを修正。
 * [maintenance]
 *     ソース整備
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PreLh2Decoder implements PreLzssDecoder {

    /** 辞書サイズ */
    private static final int DictionarySize = 8192;

    /** 最大一致長 */
    private static final int MaxMatch = 256;

    /** 最小一致長 */
    private static final int Threshold = 3;

    /**
     * code部のハフマン木のサイズ
     * code部がこれ以上の値を扱う場合は余計なビットを出力して補う。
     */
    private static final int CodeSize = 286;

    /**
     * -lh2- の圧縮データを供給する BitInputStream
     */
    private BitInputStream in;

    /**
     * Lzss非圧縮データ 1byte か Lzss圧縮コードのうち一致長を
     * 得るための 動的ハフマン木
     */
    private DynamicHuffman codeHuffman;

    /**
     * Lzss圧縮コードの上位7bitの値を得るための動的ハフマン木
     */
    private DynamicHuffman offHiHuffman;

    /**
     * (解凍後のデータの)現在処理位置
     */
    private int position;

    /**
     * 次に addLeaf() すべき position
     */
    private int nextPosition;

    /**
     * 一致長
     */
    private int matchLength;

    /** codeHuffman のバックアップ用 */
    private DynamicHuffman markCodeHuffman;

    /** offHiHuffman のバックアップ用 */
    private DynamicHuffman markOffHiHuffman;

    /** position のバックアップ用 */
    private int markPosition;

    /** nextPosition のバックアップ用 */
    private int markNextPosition;

    /** matchLength のバックアップ用 */
    private int markMatchLength;

    /**
     * -lh2- 解凍用 PreLzssDecoder を構築する。<br>
     *
     * @param in 圧縮データを供給する入力ストリーム
     */
    public PreLh2Decoder(InputStream in) {
        if (in != null) {
            if (in instanceof BitInputStream) {
                this.in = (BitInputStream) in;
            } else {
                this.in = new BitInputStream(in);
            }
            this.codeHuffman = new DynamicHuffman(PreLh2Decoder.CodeSize);
            this.offHiHuffman = new DynamicHuffman(PreLh2Decoder.DictionarySize >> 6, 1);

            this.position = 0;
            this.nextPosition = 1 << 6;
            this.matchLength = 0;
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * -lh2- で圧縮された
     * 1byte のLZSS未圧縮のデータ、
     * もしくは圧縮コードのうち一致長を読み込む。
     *
     * @return 1byte の 未圧縮のデータもしくは、圧縮された圧縮コードのうち一致長
     * @exception IOException 入出力エラーが発生した場合
     * @exception EOFException EndOfStreamに達した場合
     */
    @Override
    public int readCode() throws IOException {
        final int CodeMax = PreLh2Decoder.CodeSize - 1;

        int node = this.codeHuffman.childNode(DynamicHuffman.ROOT);
        while (0 <= node) {
            node = this.codeHuffman.childNode(node - (in.readBoolean() ? 1 : 0));
        }
        int code = ~node;
        this.codeHuffman.update(code);

        if (code < 0x100) {
            this.position++;
        } else {
            if (code == CodeMax) {
                try {
                    code += this.in.readBits(8);
                } catch (BitDataBrokenException exception) {
                    if (exception.getCause() instanceof EOFException)
                        throw (EOFException) exception.getCause();
                }
            }
            this.matchLength = code - 0x100 + PreLh2Decoder.Threshold;
        }
        return code;
    }

    /**
     * -lh2- で圧縮された
     * LZSS圧縮コードのうち一致位置を読み込む。
     *
     * @return -lh2- で圧縮された圧縮コードのうち一致位置
     * @exception IOException 入出力エラーが発生した場合
     * @exception EOFException EndOfStreamに達した場合
     */
    @Override
    public int readOffset() throws IOException {
        if (this.nextPosition < PreLh2Decoder.DictionarySize) {
            while (this.nextPosition < this.position) {
                this.offHiHuffman.addLeaf(this.nextPosition >> 6);
                this.nextPosition += 64;

                if (PreLh2Decoder.DictionarySize <= this.nextPosition)
                    break;
            }
        }
        this.position += this.matchLength;

        int node = this.offHiHuffman.childNode(DynamicHuffman.ROOT);
        while (0 <= node) {
            node = this.offHiHuffman.childNode(node - (in.readBoolean() ? 1 : 0));
        }
        int offHi = ~node;
        this.offHiHuffman.update(offHi);

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
     *            このバイト数を超えてデータを読み込んだ場合 reset()できなくなる可能性がある。
     * @see PreLzssDecoder#available()
     */
    @Override
    public void mark(int readLimit) {
        this.in.mark(readLimit * 18 / 8 + 4);
        this.markCodeHuffman = (DynamicHuffman) this.codeHuffman.clone();
        this.markOffHiHuffman = (DynamicHuffman) this.offHiHuffman.clone();
        this.markPosition = this.position;
        this.markNextPosition = this.nextPosition;
        this.markMatchLength = this.matchLength;
    }

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。
     *
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void reset() throws IOException {
        // mark()しないで reset() しようとした場合、
        // readLimit を超えて reset() しようとした場合、
        // 接続された InputStream が markSupported() で false を返す場合は
        // BitInputStream が IOException を投げる。
        this.in.reset();

        this.codeHuffman = (DynamicHuffman) this.markCodeHuffman.clone();
        this.offHiHuffman = (DynamicHuffman) this.markOffHiHuffman.clone();
        this.position = this.markPosition;
        this.nextPosition = this.markNextPosition;
        this.matchLength = this.markMatchLength;
    }

    /**
     * 接続された入力ストリームが mark() と reset() を
     * サポートするかを得る。
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
     * @exception IOException 入出力エラーが発生した場合
     * @see PreLzssDecoder#available()
     */
    @Override
    public int available() throws IOException {
        return Math.max(this.in.availableBits() / 18 - 4, 0);
    }

    /**
     * このストリームを閉じ、使用していた全ての資源を解放する。
     *
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        this.in.close();

        this.in = null;
        this.codeHuffman = null;
        this.offHiHuffman = null;
        this.markCodeHuffman = null;
        this.markOffHiHuffman = null;
    }

    /**
     * -lh2-形式の LZSS辞書のサイズを得る。
     *
     * @return -lh2-形式の LZSS辞書のサイズ
     */
    @Override
    public int getDictionarySize() {
        return PreLh2Decoder.DictionarySize;
    }

    /**
     * -lh2-形式の LZSSの最大一致長を得る。
     *
     * @return -lh2-形式の LZSSの最大一致長
     */
    @Override
    public int getMaxMatch() {
        return PreLh2Decoder.MaxMatch;
    }

    /**
     * -lh2-形式の LZSSの圧縮、非圧縮の閾値を得る。
     *
     * @return -lh2-形式の LZSSの圧縮、非圧縮の閾値
     */
    @Override
    public int getThreshold() {
        return PreLh2Decoder.Threshold;
    }
}
