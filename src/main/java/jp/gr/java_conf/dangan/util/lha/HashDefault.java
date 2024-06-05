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
 * 試作プログラム ar940528 や LHa for Unix で使用されているハッシュ関数。<br>
 * gzip で使用されているを参考にしたようだ。
 *
 * <pre>
 * -- revision history --
 * $Log: HashDefault.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     HashMethod のインタフェイス変更にあわせてインテフェイス変更。
 *     コンストラクタで引数チェックを削除。
 * [maintanance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class HashDefault implements HashMethod {

    /**
     * ハッシュ値を生成するためのマスク値
     */
    private static final int HashMask = 0x7FFF;

    /**
     * LZSS圧縮を施すためのバッファ。
     * 前半は辞書領域、
     * 後半は圧縮を施すためのデータの入ったバッファ。
     * HashMethodの実装内では Hash値の生成のための読み込みにのみ使用する。
     */
    private final byte[] TextBuffer;

    /**
     * ar940528 や LHa for Unix で使用されているハッシュ関数を構築する。
     *
     * @param TextBuffer LZSS圧縮用のバッファ。
     *            Hash値生成のため読み込み用に使用する。
     */
    public HashDefault(byte[] TextBuffer) {
        this.TextBuffer = TextBuffer;
    }

    /**
     * ハッシュ関数。<br>
     * コンストラクタで渡された TextBuffer の position からの
     * データパタンの hash値を生成する。
     *
     * @param position データパタンの開始位置
     * @return ハッシュ値
     */
    @Override
    public int hash(int position) {
        return (((this.TextBuffer[position] << 5) ^ (this.TextBuffer[position + 1] & 0xFF)) << 5
                ^ (this.TextBuffer[position + 2] & 0xFF))
               & HashDefault.HashMask;
    }

    /**
     * ハッシュ関数がハッシュ値を生成するために使用するバイト数を得る。<br>
     * このハッシュ関数は 3バイトのデータから
     * シフトとXORを使用してハッシュ値を生成するため、
     * このメソッドは常に 3 を返す。
     *
     * @return 常に 3
     */
    @Override
    public int hashRequires() {
        return 3;
    }

    /**
     * ハッシュテーブルのサイズを得る。<br>
     * このハッシュ関数は 0x0000 〜 0x7FFF のハッシュ値を生成するため、
     * このメソッドは常に 0x8000(32768) を返す。
     *
     * @return 常に 0x8000(32768)
     */
    @Override
    public int tableSize() {
        return 0x8000;
    }
}
