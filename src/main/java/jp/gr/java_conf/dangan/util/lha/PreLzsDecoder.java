// start of PreLzsDecoder.java
// TEXT_STYLE:CODE=Shift_JIS(Japanese):RET_CODE=CRLF

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

import java.io.EOFException;
// import exceptions
import java.io.IOException;
// import classes and interfaces
import java.io.InputStream;

import jp.gr.java_conf.dangan.io.BitDataBrokenException;
import jp.gr.java_conf.dangan.io.BitInputStream;
import jp.gr.java_conf.dangan.io.Bits;


/**
 * -lzs- 解凍用 PreLzssDecoder。
 *
 * <pre>
 * -- revision history --
 * $Log: PreLzsDecoder.java,v $
 * Revision 1.1  2002/12/06 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     -lzs- の MaxMatch が 17 であるべきが 16 となっていたのを修正。
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PreLzsDecoder implements PreLzssDecoder {

    /** 辞書サイズ */
    private static final int DictionarySize = 2048;

    /** 最大一致長 */
    private static final int MaxMatch = 17;

    /** 最小一致長 */
    private static final int Threshold = 2;

    /** 一致位置のビット数 */
    private static final int OffsetBits = Bits.len(PreLzsDecoder.DictionarySize - 1);

    /** 一致長のビット数 */
    private static final int LengthBits = Bits.len(PreLzsDecoder.MaxMatch - PreLzsDecoder.Threshold);

    /**
     * -lzs- 形式の圧縮データを供給する BitInputStream
     */
    private BitInputStream in;

    /**
     * 現在処理位置。
     * LzssInputStreamの内部状態を取得できないために存在する。
     * LzssInputStreamの内部クラスとして書けば、positionは必要無い。
     */
    private int position;

    /** 最も新しいLzssコードの一致位置 */
    private int matchOffset;

    /**
     * 最も新しいLzssコードの一致長
     * LzssInputStreamの内部状態を取得できないために存在する。
     * LzssInputStreamの内部クラスとして書けば、matchLengthは必要無い。
     */
    private int matchLength;

    /** matchPositionのバックアップ用 */
    private int markPosition;

    /** matchPositionのバックアップ用 */
    private int markMatchOffset;

    /** matchLengthのバックアップ用 */
    private int markMatchLength;

    /**
     * -lzs- 解凍用 PreLzssDecoder を構築する。
     *
     * @param in -lzs- 形式の圧縮データを供給する入力ストリーム
     */
    public PreLzsDecoder(InputStream in) {
        if (in != null) {
            if (in instanceof BitInputStream) {
                this.in = (BitInputStream) in;
            } else {
                this.in = new BitInputStream(in);
            }
            this.position = 0;
            this.matchOffset = 0;
            this.matchLength = 0;
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * -lzs- で圧縮された
     * 1byte の LZSS未圧縮のデータ、
     * もしくは圧縮コードのうち一致長を読み込む。
     *
     * @return 1byte の 未圧縮のデータもしくは、圧縮された圧縮コードのうち一致長
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public int readCode() throws IOException {
        try {
            if (this.in.readBoolean()) {
                this.position++;
                return this.in.readBits(8);
            } else {
                this.matchOffset = this.in.readBits(PreLzsDecoder.OffsetBits);
                this.matchLength = this.in.readBits(PreLzsDecoder.LengthBits);
                return this.matchLength | 0x100;
            }
        } catch (BitDataBrokenException exception) {
            if (exception.getCause() instanceof EOFException)
                throw (EOFException) exception.getCause();
            else
                throw exception;
        }
    }

    /**
     * -lzs- で圧縮された圧縮コードのうち
     * 一致位置を読み込む。<br>
     *
     * @return -lzs- で圧縮された圧縮コードのうち一致位置
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public int readOffset() throws IOException {
        int offset = (this.position - this.matchOffset - 1 - PreLzsDecoder.MaxMatch) & (PreLzsDecoder.DictionarySize - 1);

        this.position += this.matchLength + PreLzsDecoder.Threshold;
        return offset;
    }

    /**
     * 接続された入力ストリームの現在位置にマークを設定し、
     * reset() メソッドでマークした時点の 読み込み位置に
     * 戻れるようにする。<br>
     * InputStream の mark() と違い、readLimit で設定した
     * 限界バイト数より前にマーク位置が無効になる可能性が
     * ある事に注意すること。<br>
     *
     * @param readLimit マーク位置に戻れる限界のバイト数。
     *                  このバイト数を超えてデータを読み
     *                  込んだ場合 reset()できなくなる可
     *                  能性がある。<br>
     * @see PreLzssDecoder#mark(int)
     */
    @Override
    public void mark(int readLimit) {
        this.in.mark((readLimit * 9 + 7) / 8 + 1);
        this.markPosition = this.position;
        this.markMatchOffset = this.matchOffset;
        this.markMatchLength = this.matchLength;
    }

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。<br>
     *
     * @throws IOException <ol>
     *                     <li> mark() せずに reset() しようとした場合。</li>
     *                     <li> 接続された入力ストリームが markSupported()でfalse を返す場合。</li>
     *                     <li> 接続された入力ストリームで入出力エラーが発生した場合。</li>
     *                     </ol> の何れか。
     */
    @Override
    public void reset() throws IOException {
        // mark()しないで reset()しようとした場合、
        // 接続された InputStream が mark/resetをサポートしない場合は
        // BitInputStream の reset()によって IOExceptionが投げられる。
        this.in.reset();

        this.position = this.markPosition;
        this.matchOffset = this.markMatchOffset;
        this.matchLength = this.markMatchLength;
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
     * この最低バイト数は保証される。
     *
     * @return ブロックしないで読み出せる最低バイト数。
     * @throws IOException 入出力エラーが発生した場合
     * @see PreLzssDecoder#available()
     */
    @Override
    public int available() throws IOException {
        return Math.max(this.in.availableBits() / 9 - 2, 0);
    }

    /**
     * この出力とストリームと
     * 接続されていたストリームを閉じ、
     * 使用していたリソースを解放する。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        this.in.close();

        this.in = null;
    }

    /**
     * -lzs-形式の LZSS辞書のサイズを得る。
     *
     * @return -lzs-形式の LZSS辞書のサイズ
     */
    @Override
    public int getDictionarySize() {
        return PreLzsDecoder.DictionarySize;
    }

    /**
     * -lzs-形式の LZSSの最長一致長を得る。
     *
     * @return -lzs-形式の LZSSの最長一致長
     */
    @Override
    public int getMaxMatch() {
        return PreLzsDecoder.MaxMatch;
    }

    /**
     * -lzs-形式の LZSSの圧縮、非圧縮の閾値を得る。
     *
     * @return -lzs-形式の LZSSの圧縮、非圧縮の閾値
     */
    @Override
    public int getThreshold() {
        return PreLzsDecoder.Threshold;
    }
}
