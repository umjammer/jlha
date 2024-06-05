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
 * -lh2- 圧縮用 PostLzssEncoder。 <br>
 *
 * <pre>
 * -- revision history --
 * $Log: PostLh2Encoder.java,v $
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
public class PostLh2Encoder implements PostLzssEncoder {

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
     * -lh2- 形式の圧縮データの出力先の ビット出力ストリーム
     */
    private BitOutputStream out;

    /**
     * code部圧縮用適応的ハフマン木
     */
    private DynamicHuffman codeHuffman;

    /**
     * offHi部圧縮用適応的ハフマン木
     */
    private DynamicHuffman offHiHuffman;

    /**
     * 出力したオリジナルのサイズをカウントする
     */
    private int position;

    /**
     * 次に葉を加える動作をする位置
     */
    private int nextPosition;

    /**
     * 現在処理中の一致長
     */
    private int matchLength;

    /**
     * -lh2- 圧縮用 PostLzssEncoder を構築する。
     *
     * @param out 圧縮データを受け取る出力ストリーム
     */
    public PostLh2Encoder(OutputStream out) {
        if (out != null) {
            if (out instanceof BitOutputStream) {
                this.out = (BitOutputStream) out;
            } else {
                this.out = new BitOutputStream(out);
            }
            this.codeHuffman = new DynamicHuffman(PostLh2Encoder.CodeSize);
            this.offHiHuffman = new DynamicHuffman(PostLh2Encoder.DictionarySize >> 6, 1);
            this.position = 0;
            this.nextPosition = 1 << 6;
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
        final int CodeMax = PostLh2Encoder.CodeSize - 1;

        int node = this.codeHuffman.codeToNode(Math.min(code, CodeMax));
        int hcode = 0;
        int hlen = 0;
        do {
            hcode >>>= 1;
            hlen++;
            if ((node & 1) != 0)
                hcode |= 0x80000000;

            node = this.codeHuffman.parentNode(node);
        } while (node != DynamicHuffman.ROOT);

        this.out.writeBits(hlen, hcode >>> (32 - hlen));

        if (code < 0x100) {
            this.position++;
        } else {
            this.matchLength = (code & 0xFF) + PostLh2Encoder.Threshold;

            if (CodeMax <= code) {
                this.out.writeBits(8, code - CodeMax);
                code = CodeMax; // updateするコードをCodeMaxにする。
            }
        }
        this.codeHuffman.update(code);
    }

    /**
     * LZSS で圧縮された圧縮コードのうち一致位置を書きこむ。<br>
     *
     * @param offset LZSS で圧縮された圧縮コードのうち一致位置
     */
    @Override
    public void writeOffset(int offset) throws IOException {
        if (this.nextPosition < PostLh2Encoder.DictionarySize) {
            while (this.nextPosition < this.position) {
                this.offHiHuffman.addLeaf(this.nextPosition >> 6);
                this.nextPosition += 64;

                if (PostLh2Encoder.DictionarySize <= this.nextPosition)
                    break;
            }
        }

        int node = this.offHiHuffman.codeToNode(offset >> 6);
        int hcode = 0;
        int hlen = 0;
        while (node != DynamicHuffman.ROOT) {
            hcode >>>= 1;
            hlen++;
            if ((node & 1) != 0)
                hcode |= 0x80000000;

            node = this.offHiHuffman.parentNode(node);
        }

        this.out.writeBits(hlen, hcode >> (32 - hlen));
        this.out.writeBits(6, offset);
        this.offHiHuffman.update(offset >> 6);

        this.position += this.matchLength;
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
     * 使用していたリソースを開放する。<br>
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        this.out.close();

        this.out = null;
        this.codeHuffman = null;
        this.offHiHuffman = null;
    }

    /**
     * -lh2-形式の LZSS辞書のサイズを得る。
     *
     * @return -lh2-形式の LZSS辞書のサイズ
     */
    @Override
    public int getDictionarySize() {
        return PostLh2Encoder.DictionarySize;
    }

    /**
     * -lh2-形式の LZSSの最大一致長を得る。
     *
     * @return -lh2-形式の LZSSの最大一致長
     */
    @Override
    public int getMaxMatch() {
        return PostLh2Encoder.MaxMatch;
    }

    /**
     * -lh2-形式の LZSSの圧縮、非圧縮の閾値を得る。
     *
     * @return -lh2-形式の LZSSの圧縮、非圧縮の閾値
     */
    @Override
    public int getThreshold() {
        return PostLh2Encoder.Threshold;
    }
}
