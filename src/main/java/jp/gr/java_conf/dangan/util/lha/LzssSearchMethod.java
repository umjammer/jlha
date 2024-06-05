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
 * LzssOutputStream で使用される
 * 最長一致検索を提供するインターフェイス。
 *
 * <br>
 * コンストラクタの形式は
 *
 * <pre>
 * LzssSearchMethod( int    DictionarySize,
 *                   int    MaxMatch,
 *                   int    Threshold,
 *                   byte[] TextBuffer )
 *
 * <strong>パラメータ:</strong>
 *   DictionarySize - LZSSの辞書サイズ
 *   MaxMatch       - LZSSの最大一致長
 *   Threshold      - LZSSの圧縮/非圧縮の閾値
 *   TextBuffer     - LZSS圧縮を施すデータの入ったバッファ
 * </pre>
 *
 * のような形式に則ること。<br>
 * また、追加の引数をとりたい場合は
 *
 * <pre>
 * LzssSearchMethod( int    DictionarySize,
 *                   int    MaxMatch,
 *                   int    Threshold,
 *                   byte[] TextBuffer,
 *                   Object ExtraArgument1,
 *                   Object ExtraArgument2 )
 * </pre>
 *
 * のような形式を用いる。<br>
 * なお、コンストラクタの引数チェックは追加の引数がある場合について行えばよい。
 * <br>
 *
 * <pre>
 * -- revision history --
 * $Log: LzssSearchMethod.java,v $
 * Revision 1.1  2002/12/04 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [change]
 *     slide() で引数を取らずに
 *     スライド幅を常に DictionarySize とするように変更。
 *     putLength を putRequires に変更
 * [maintenance]
 *     タブ廃止
 *     ライセンス文の修正
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public interface LzssSearchMethod {

    /**
     * position から始まるデータパタンを
     * LzssSearchMethod の持つ検索機構に登録する。<br>
     * LzssOutputStream は 線形に、重複無く、
     * put または searchAndPut を呼び出す。
     *
     * @param position TextBuffer内のデータパタンの開始位置
     */
    void put(int position);

    /**
     * 検索機構に登録されたデータパタンから
     * position から始まるデータパタンと
     * 最長の一致を持つものを検索し、
     * 同時に position から始まるデータパタンを
     * LzssSearchMethod の持つ検索機構に登録する。<br>
     * LzssOutputStream は 線形に、重複無く、
     * put または searchAndPut を呼び出す。
     *
     * @param position TextBuffer内のデータパタンの開始位置
     * @return 一致が見つかった場合は
     *         LzssOutputStream.createSearchReturn
     *         によって生成された一致位置と一致長の情報を持つ値、
     *         一致が見つからなかった場合は
     *         LzssOutputStream.NOMATCH。
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    int searchAndPut(int position);

    /**
     * 検索機構に登録されたデータパタンから
     * position から始まるデータパタンと
     * 最長の一致を持つものを検索する。<br>
     * このメソッドは LzssOutputStream の
     * flush() を実装するためだけに提供される。<br>
     * TextBuffer.length &lt position + MaxMatch となるような
     * position にも対応すること。
     *
     * @param position TextBuffer内のデータパタンの開始位置
     * @param lastPutPos 最後に登録したデータパタンの開始位置
     * @return 一致が見つかった場合は
     *         LzssOutputStream.createSearchReturn
     *         によって生成された一致位置と一致長の情報を持つ値、
     *         一致が見つからなかった場合は
     *         LzssOutputStream.NOMATCH。
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    int search(int position, int lastPutPos);

    /**
     * LzssOutputStream が slide() でTextBuffer内のデータを
     * DictionarySize だけ移動させる際に検索機構内のデータを
     * それらと矛盾無く移動させる処理を行う。
     */
    void slide();

    /**
     * put() または searchAndPut() を使用して
     * データパタンを検索機構に登録する時に
     * 必要とするデータ量を得る。<br>
     *
     * @return put() または searchAndPut() で検索機構に登録するデータ量
     */
    int putRequires();
}
