// start of PreLz5Decoder.java
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

import jp.gr.java_conf.dangan.io.CachedInputStream;


/**
 * -lz5- 解凍用 PreLzssDecoder。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: PreLz5Decoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PreLz5Decoder implements PreLzssDecoder {

    /** 辞書サイズ */
    private static final int DictionarySize = 4096;

    /** 最大一致長 */
    private static final int MaxMatch = 18;

    /** 最小一致長 */
    private static final int Threshold = 3;

    /**
     * -lz5- 形式の圧縮データを供給するストリーム
     */
    private InputStream in;

    /**
     * 現在処理位置。
     * larc の一致位置から lha の一致位置への変換に必要
     */
    private int position;

    /** Lzss圧縮情報のうち 一致位置(larcの一致位置) */
    private int matchPos;

    /** Lzss圧縮符号のうち 一致長 */
    private int matchLen;

    /** 8つのLzss圧縮、非圧縮を示すフラグをまとめたもの */
    private int flagByte;

    /** Lzss圧縮、非圧縮を示すフラグ */
    private int flagBit;

    /** positionのバックアップ用 */
    private int markPosition;

    /** matchOffsetのバックアップ用 */
    private int markMatchPos;

    /** matchLengthのバックアップ用 */
    private int markMatchLen;

    /** flagByteのバックアップ用。 */
    private int markFlagByte;

    /** flagCountのバックアップ用。 */
    private int markFlagBit;

    /**
     * -lz5- 解凍用 PreLzssDecoder を構築する。
     *
     * @param in 圧縮データを供給する入力ストリーム
     */
    public PreLz5Decoder(InputStream in) {
        if (in != null) {
            if (in instanceof CachedInputStream) {
                this.in = in;
            } else {
                this.in = new CachedInputStream(in);
            }

            this.position = 0;
            this.matchPos = 0;
            this.matchLen = 0;
            this.flagByte = 0;
            this.flagBit = 0x100;

            this.markPosition = 0;
            this.markMatchPos = 0;
            this.markMatchLen = 0;
            this.markFlagByte = 0;
            this.markFlagBit = 0;
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * -lz5- で圧縮された
     * 1byte の LZSS未圧縮のデータ、
     * もしくは圧縮コードのうち一致長を読み込む。<br>
     *
     * @return 1byte の 未圧縮のデータもしくは、圧縮された圧縮コードのうち一致長
     * @throws IOException  入出力エラーが発生した場合
     * @throws EOFException EndOfStreamに達した場合
     */
    @Override
    public int readCode() throws IOException {
        if (this.flagBit == 0x100) {
            this.flagByte = this.in.read();

            if (0 <= this.flagByte) {
                this.flagBit = 0x01;
            } else {
                throw new EOFException();
            }
        }

        if (0 != (this.flagByte & this.flagBit)) {
            this.flagBit <<= 1;
            this.position++;
            int ret = this.in.read();
            if (0 <= ret)
                return ret;
            else
                throw new EOFException();
        } else {
            this.flagBit <<= 1;
            int c1 = this.in.read();
            int c2 = this.in.read();

            if (0 <= c1) {
                this.matchPos = ((c2 & 0xF0) << 4) | c1;
                this.matchLen = c2 & 0x0F;
                return this.matchLen | 0x100;
            } else {
                throw new EOFException();
            }
        }
    }

    /**
     * -lz5- で圧縮された
     * 圧縮コードのうち一致位置を読み込む。<br>
     *
     * @return -lz5- で圧縮された圧縮コードのうち一致位置
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public int readOffset() throws IOException {
        int offset = (this.position - this.matchPos - 1 - PreLz5Decoder.MaxMatch) & (PreLz5Decoder.DictionarySize - 1);

        this.position += this.matchLen + PreLz5Decoder.Threshold;

        return offset;
    }

    /**
     * 接続された入力ストリームの現在位置にマークを設定し、
     * reset() メソッドでマークした時点の 読み込み位置に
     * 戻れるようにする。<br>
     *
     * @param readLimit マーク位置に戻れる限界のバイト数。
     *                  このバイト数を超えてデータを読み込んだ場合 reset()できなくなる可                  能性がある。<br>
     * @see PreLzssDecoder#mark(int)
     */
    @Override
    public void mark(int readLimit) {
        this.in.mark((readLimit * 9 + 7) / 8 + 2);
        this.markPosition = this.position;
        this.markMatchLen = this.matchLen;
        this.markMatchPos = this.matchPos;
        this.markFlagByte = this.flagByte;
        this.markFlagBit = this.flagBit;
    }

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。<br>
     *
     * @throws IOException <ol>
     *                     <li> mark() せずに reset() しようとした場合。</li>
     *                     <li> 接続された入力ストリームが markSupported()で false を返す場合。</li>
     *                     <li> 接続された入力ストリームで入出力エラーが発生した場合。</li>
     *                     </ol> の何れか。
     */
    @Override
    public void reset() throws IOException {
        // mark() していないのに reset() しようとした場合、
        // 接続されたストリームがmark/resetをサポートしない場合は
        // CachedInputStream が IOException を投げる。
        this.in.reset();

        this.position = this.markPosition;
        this.matchLen = this.markMatchLen;
        this.matchPos = this.markMatchPos;
        this.flagByte = this.markFlagByte;
        this.flagBit = this.markFlagBit;
    }

    /**
     * 接続されたストリームが mark() と reset()
     * をサポートするかを返す。
     *
     * @return 接続されたストリームが mark,reset をサポートするならtrue,
     * サポートしないなら false
     */
    @Override
    public boolean markSupported() {
        return this.in.markSupported();
    }

    /**
     * ブロックせずに読み出すことの出来る最低バイト数を得る。<br>
     * この値は保証される。
     *
     * @return ブロックしないで読み出せる最低バイト数。
     * @throws IOException 入出力エラーが発生した場合
     * @see PreLzssDecoder#available()
     */
    @Override
    public int available() throws IOException {
        return Math.max(in.available() * 8 / 9 - 2, 0);
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
    }

    /**
     * -lz5-形式の LZSS辞書のサイズを得る。
     *
     * @return -lz5-形式の LZSS辞書のサイズ
     */
    @Override
    public int getDictionarySize() {
        return PreLz5Decoder.DictionarySize;
    }

    /**
     * -lz5-形式の LZSSの最大一致長を得る。
     *
     * @return -lz5-形式の LZSSの最大一致長
     */
    @Override
    public int getMaxMatch() {
        return PreLz5Decoder.MaxMatch;
    }

    /**
     * -lz5-形式の LZSSの圧縮、非圧縮の閾値を得る。
     *
     * @return -lz5-形式の LZSSの圧縮、非圧縮の閾値
     */
    @Override
    public int getThreshold() {
        return PreLz5Decoder.Threshold;
    }

}
