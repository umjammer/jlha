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

/**
 * 特別な検索機構を用いない
 * LzssSearchMethod の最もシンプルな実装。<br>
 * 検索機構を用いないため、
 * 他の検索機構を用いる実装と比べると遅いが、
 * メモリ消費量も非常に少ない。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: SimpleSearch.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     LzssSearchMethod のインタフェイス変更にあわせてインタフェイス変更。
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class SimpleSearch implements LzssSearchMethod {

    /**
     * LZSS辞書サイズ。
     */
    private final int DictionarySize;

    /**
     * LZSS圧縮に使用される値。
     * 最大一致長を示す。
     */
    private final int MaxMatch;

    /**
     * LZSS圧縮に使用される閾値。
     * 一致長が この値以上であれば、圧縮コードを出力する。
     */
    private final int Threshold;

    /**
     * LZSS圧縮を施すためのバッファ。
     * position を境に 前半は辞書領域、
     * 後半は圧縮を施すためのデータの入ったバッファ。
     * LzssSearchMethodの実装内では読み込みのみ許される。
     */
    private final byte[] TextBuffer;

    /**
     * 辞書の限界位置。
     * TextBuffer前半の辞書領域にデータが無い場合に
     * 辞書領域にある不定のデータ(Javaでは0)を使用
     * して圧縮が行われるのを抑止する。
     */
    private int DictionaryLimit;

    /**
     * 特別な検索機構を用いないシンプルな
     * LzssSearchMethod を構築する。<br>
     *
     * @param DictionarySize 辞書サイズ
     * @param MaxMatch       最大一致長
     * @param Threshold      圧縮、非圧縮の閾値
     * @param TextBuffer     LZSS圧縮を施すためのバッファ
     */
    public SimpleSearch(int DictionarySize, int MaxMatch, int Threshold, byte[] TextBuffer) {

        this.DictionarySize = DictionarySize;
        this.MaxMatch = MaxMatch;
        this.Threshold = Threshold;
        this.TextBuffer = TextBuffer;
        this.DictionaryLimit = this.DictionarySize;
    }

    /**
     * SimpleSearch は検索機構を使用しないため
     * このメソッドは何もしない。
     *
     * @param position TextBuffer内のデータパタンの開始位置
     */
    @Override
    public void put(int position) {
    }

    /**
     * TextBuffer 内の辞書領域にあるデータパタンから
     * position から始まるデータパタンと
     * 最長の一致を持つものを検索する。
     *
     * @param position TextBuffer内のデータパタンの開始位置。
     * @return 一致が見つかった場合は LzssOutputStream.createSearchReturn
     * によって生成された一致位置と一致長の情報を持つ値、 一致が見つからなかった場合は
     * LzssOutputStream.NOMATCH。
     * @see LzssOutputStream#createSearchReturn(int, int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int searchAndPut(int position) {
        int matchlen = this.Threshold - 1;
        int matchpos = position;
        int scanlimit = Math.max(this.DictionaryLimit, position - this.DictionarySize);
        int scanpos = position - 1;

        byte[] buf = this.TextBuffer;
        int max = position + this.MaxMatch;
        int p;
        int s;
        int len;
        while (scanlimit < scanpos) {
            s = scanpos;
            p = position;
            while (buf[s] == buf[p]) {
                s++;
                p++;
                if (max <= p)
                    break;
            }

            len = p - position;
            if (matchlen < len) {
                matchpos = scanpos;
                matchlen = len;
                if (this.MaxMatch == len)
                    break;
            }
            scanpos--;
        }

        if (this.Threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * TextBuffer 内の辞書領域にあるデータパタンから
     * position から始まるデータパタンと
     * 最長の一致を持つものを検索する。
     *
     * @param position   TextBuffer内のデータパタンの開始位置。
     * @param lastPutPos 最後に登録したデータパタンの開始位置。
     * @return 一致が見つかった場合は
     * LzssOutputStream.createSearchReturn
     * によって生成された一致位置と一致長の情報を持つ値、
     * 一致が見つからなかった場合は
     * LzssOutputStream.NOMATCH。
     * @see LzssOutputStream#createSearchReturn(int, int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int search(int position, int lastPutPos) {
        int matchlen = this.Threshold - 1;
        int matchpos = position;
        int scanlimit = Math.max(this.DictionaryLimit, position - this.DictionarySize);
        int scanpos = position - 1;

        byte[] buf = this.TextBuffer;
        int max = Math.min(position + this.MaxMatch, this.TextBuffer.length);
        int p;
        int s;
        int len;
        while (scanlimit < scanpos) {
            s = scanpos;
            p = position;
            while (buf[s] == buf[p]) {
                s++;
                p++;
                if (max <= p)
                    break;
            }

            len = p - position;
            if (matchlen < len) {
                matchpos = scanpos;
                matchlen = len;
                if (this.MaxMatch == len)
                    break;
            }
            scanpos--;
        }

        if (this.Threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * LzssOutputStream が slide() でTextBuffer内のデータを
     * DictionarySize だけ移動させる際に検索機構内のデータを
     * それらと矛盾無く移動させる処理を行う。
     */
    @Override
    public void slide() {
        this.DictionaryLimit = Math.max(0, this.DictionaryLimit - this.DictionarySize);
    }

    /**
     * SimpleSearch は検索機構を使用しないため常に 0 を返す。
     *
     * @return 常に 0
     */
    @Override
    public int putRequires() {
        return 0;
    }
}
