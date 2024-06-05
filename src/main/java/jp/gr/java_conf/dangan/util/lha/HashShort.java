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
 * データパタンの先頭2バイトから
 * 0 〜 4095 のハッシュ値を生成するハッシュ関数。
 *
 * <pre>
 * -- revision history --
 * $Log: HashShort.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     HashMethod のインタフェイス変更にあわせてインテフェイス変更。
 * [maintanance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class HashShort implements HashMethod {

    /**
     * LZSS圧縮を施すためのバッファ。
     * 前半は辞書領域、
     * 後半は圧縮を施すためのデータの入ったバッファ。
     * HashMethodの実装内では Hash値の生成のための読み込みにのみ使用する。
     */
    private final byte[] TextBuffer;

    /**
     * データパタンの先頭2バイトから 0x000 〜 0xFFF までの値を生成する
     * ハッシュ関数を構築する。
     *
     * @param TextBuffer LZSS圧縮用のバッファ。Hash値生成のため読み込み用に使用する。
     */
    public HashShort(byte[] TextBuffer) {
        this.TextBuffer = TextBuffer;
    }

    /**
     * ハッシュ関数。
     * コンストラクタで渡された TextBuffer の position からの
     * データパタンの hash値を生成する。
     *
     * @param position データパタンの開始位置
     * @return ハッシュ値
     */
    @Override
    public int hash(int position) {
        return ((((this.TextBuffer[position + 1] & 0x0F) << 8) | ((this.TextBuffer[position + 1] & 0xFF) >> 4))
                ^ ((this.TextBuffer[position] & 0xFF) << 2));
    }

    /**
     * ハッシュ関数がハッシュ値を生成するために使用するバイト数を得る。<br>
     * このハッシュ関数はデータパタンの先頭 2 バイトのデータから
     * ハッシュ値を生成するため、このメソッドは常に 2 を返す。
     *
     * @return 常に 2
     */
    @Override
    public int hashRequires() {
        return 2;
    }

    /**
     * ハッシュテーブルのサイズを得る。<br>
     * このハッシュ関数は 0x000 〜 0xFFF までのハッシュ値を生成するため
     * このメソッドは常に 0x1000(4096) を返す。
     *
     * @return 常に 0x1000(4096)
     */
    @Override
    public int tableSize() {
        return 0x1000;
    }
}
