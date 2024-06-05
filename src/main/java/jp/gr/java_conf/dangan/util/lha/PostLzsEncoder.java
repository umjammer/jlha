// start of PostLzsEncoder.java
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

import jp.gr.java_conf.dangan.io.BitOutputStream;
import jp.gr.java_conf.dangan.io.Bits;


/**
 * -lzs- 圧縮用 PostLzssEncoder。
 *
 * <pre>
 * -- revision history --
 * $Log: PostLzsEncoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/07/31 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     -lzs- の MaxMatch は 16 でなく 17 だったのを修正。
 * [maintenance]
 *     ソース整備
 *     タブの廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class PostLzsEncoder implements PostLzssEncoder {

    /** 辞書サイズ */
    private static final int DictionarySize = 2048;

    /** 最大一致長 */
    private static final int MaxMatch = 17;

    /** 最小一致長 */
    private static final int Threshold = 2;

    /** 一致位置のビット数 */
    private static final int PositionBits = Bits.len(PostLzsEncoder.DictionarySize - 1);

    /** 一致長のビット数 */
    private static final int LengthBits = Bits.len(PostLzsEncoder.MaxMatch - PostLzsEncoder.Threshold);

    /**
     * -lzs- 形式のデータを出力するビット出力ストリーム
     */
    private BitOutputStream out;

    /**
     * ストリーム内現在処理位置
     */
    private int position;

    /**
     * 現在処理中のLZSS圧縮コード
     */
    private int matchLength;

    /**
     * -lzs- 圧縮用 PostLzssEncoder を構築する。
     *
     * @param out -lzs- 形式の圧縮データを出力するストリーム
     */
    public PostLzsEncoder(OutputStream out) {
        if (out != null) {
            if (out instanceof BitOutputStream) {
                this.out = (BitOutputStream) out;
            } else {
                this.out = new BitOutputStream(out);
            }
            this.position = 0;
            this.matchLength = 0;
        } else {
            throw new NullPointerException("out");
        }
    }

    /**
     * 1byte の LZSS未圧縮のデータもしくは、
     * LZSS で圧縮された圧縮コードのうち一致長を書きこむ。<br>
     *
     * @param code 1byte の LZSS未圧縮のデータもしくは、LZSS で圧縮された圧縮コードのうち一致長
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void writeCode(int code) throws IOException {
        if (code < 0x100) {
            this.out.writeBit(1);
            this.out.writeBits(8, code);
            this.position++;
        } else {
            // close() 後の writeCode() で
            // NullPointerException を投げることを期待している。
            this.out.writeBit(0);
            this.matchLength = code - 0x100;
        }
    }

    /**
     * LZSS で圧縮された圧縮コードのうち一致位置を書きこむ。<br>
     *
     * @param offset LZSS で圧縮された圧縮コードのうち一致位置
     */
    @Override
    public void writeOffset(int offset) throws IOException {
        int pos = (this.position - offset - 1 - PostLzsEncoder.MaxMatch) & (PostLzsEncoder.DictionarySize - 1);

        this.position += this.matchLength + PostLzsEncoder.Threshold;

        this.out.writeBits(PostLzsEncoder.PositionBits, pos);
        this.out.writeBits(PostLzsEncoder.LengthBits, this.matchLength);
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
    }

    /**
     * -lzs-形式の LZSS辞書のサイズを得る。
     *
     * @return -lzs-形式の LZSS辞書のサイズ
     */
    @Override
    public int getDictionarySize() {
        return PostLzsEncoder.DictionarySize;
    }

    /**
     * -lzs-形式の LZSSの最大一致長を得る。
     *
     * @return -lzs-形式の LZSSの最大一致長
     */
    @Override
    public int getMaxMatch() {
        return PostLzsEncoder.MaxMatch;
    }

    /**
     * -lzs-形式の LZSSの圧縮、非圧縮の閾値を得る。
     *
     * @return -lzs-形式の LZSSの圧縮、非圧縮の閾値
     */
    @Override
    public int getThreshold() {
        return PostLzsEncoder.Threshold;
    }
}
