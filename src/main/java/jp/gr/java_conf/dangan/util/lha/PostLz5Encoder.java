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
// import classes and interfaces
import java.io.OutputStream;


/**
 * -lz5- 圧縮用 PostLzssEncoder。
 *
 * <pre>
 * -- revision history --
 * $Log: PostLz5Encoder.java,v $
 * Revision 1.1  2002/12/01 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/07/31 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     -lz5- の MaxMatch は 16 でなく 18 だった。
 *     flush() で出力できるデータを出力していなかったのを修正。
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
public class PostLz5Encoder implements PostLzssEncoder {

    /** 辞書サイズ */
    private static final int DictionarySize = 4096;

    /** 最大一致長 */
    private static final int MaxMatch = 18;

    /** 最小一致長 */
    private static final int Threshold = 3;

    /**
     * -lz5- 圧縮データを出力するストリーム
     */
    private OutputStream out;

    /** 圧縮データの一時格納用バッファ */
    private byte[] buf;

    /** buf内の現在処理位置 */
    private int index;

    /** buf内の Lzss圧縮、非圧縮を示すフラグの位置を示す */
    private int flagIndex;

    /** Lzss圧縮、非圧縮を示すフラグ */
    private int flagBit;

    /**
     * ストリーム内現在処理位置
     * lha の offset から larc の offset への変換に必要
     */
    private int position;

    /**
     * -lz5- 圧縮用 PostLzssEncoder を構築する。<br>
     *
     * @param out 圧縮データを出力する出力ストリーム
     */
    public PostLz5Encoder(OutputStream out) {
        if (out != null) {
            this.out = out;
            this.position = 0;
            this.buf = new byte[1024];
            this.index = 0;
            this.flagIndex = 0;
            this.flagBit = 0x100;
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
        if (this.flagBit == 0x100) {
            if (this.buf.length - (2 * 8 + 1) < this.index) {
                this.out.write(this.buf, 0, this.index);
                this.index = 0;
            }
            this.flagBit = 0x01;
            this.flagIndex = this.index++;
            this.buf[this.flagIndex] = 0;
        }

        if (code < 0x100) {
            this.buf[this.flagIndex] |= this.flagBit;
            this.buf[this.index++] = (byte) code;
            this.position++;
        } else {
            this.buf[this.index++] = (byte) code;
        }
        this.flagBit <<= 1;
    }

    /**
     * LZSS で圧縮された圧縮コードのうち一致位置を書きこむ。
     *
     * @param offset LZSS で圧縮された圧縮コードのうち一致位置
     */
    @Override
    public void writeOffset(int offset) {
        int pos = (this.position - offset - 1 - PostLz5Encoder.MaxMatch) & (PostLz5Encoder.DictionarySize - 1);

        int matchlen = this.buf[--this.index] & 0x0F;
        this.buf[this.index++] = (byte) pos;
        this.buf[this.index++] = (byte) (((pos >> 4) & 0xF0) | matchlen);

        this.position += matchlen + PostLz5Encoder.Threshold;
    }

    /**
     * この PostLzssEncoder にバッファリングされている
     * 出力可能なデータを出力先の OutputStream に出力し、
     * 出力先の OutputStream を flush() する。<br>
     * このメソッドは出力不可能な 最大15バイトのデータを
     * バッファリングしたまま 出力しない。
     * このメソッドは圧縮率を変化させない。
     *
     * @throws IOException 入出力エラーが発生した場合
     * @see PostLzssEncoder#flush()
     */
    @Override
    public void flush() throws IOException {
        if (this.flagBit == 0x100) {
            this.out.write(this.buf, 0, this.index);
            this.out.flush();

            this.index = 0;
            this.flagBit = 0x01;
            this.flagIndex = this.index++;
            this.buf[this.flagIndex] = 0;
        } else {
            this.out.write(this.buf, 0, this.flagIndex);
            this.out.flush();

            System.arraycopy(this.buf, this.flagIndex, this.buf, 0, this.index - this.flagIndex);
            this.index -= this.flagIndex;
            this.flagIndex = 0;
        }
    }

    /**
     * この出力ストリームと、接続された出力ストリームを閉じ、
     * 使用していたリソースを開放する。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        this.out.write(this.buf, 0, this.index);
        this.out.close();

        this.out = null;
        this.buf = null;
    }

    /**
     * -lz5-形式の LZSS辞書のサイズを得る。
     *
     * @return -lz5-形式の LZSS辞書のサイズ
     */
    @Override
    public int getDictionarySize() {
        return PostLz5Encoder.DictionarySize;
    }

    /**
     * -lz5-形式の LZSSの最長一致長を得る。
     *
     * @return -lz5-形式の LZSSの最長一致長
     */
    @Override
    public int getMaxMatch() {
        return PostLz5Encoder.MaxMatch;
    }

    /**
     * -lz5-形式の LZSSの圧縮、非圧縮の閾値を得る。
     *
     * @return -lz5-形式の LZSSの圧縮、非圧縮の閾値
     */
    @Override
    public int getThreshold() {
        return PostLz5Encoder.Threshold;
    }
}
