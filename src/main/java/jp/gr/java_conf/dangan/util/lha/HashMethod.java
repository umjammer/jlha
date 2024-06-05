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
 * ハッシュ関数を提供するインターフェイス。
 * <br>
 * コンストラクタの形式は
 *
 * <pre>
 * HashMethod( byte[] TextBuffer )
 *
 * <strong>パラメータ:</strong>
 *   TextBuffer     - LZSS圧縮を施すデータの入ったバッファ
 * </pre>
 *
 * のような形式に則ること。<br>
 * また、追加の引数をとりたい場合は
 *
 * <pre>
 * HashMethod( byte[] TextBuffer,
 *             Object ExtraData1,
 *             Object ExtraData2 )
 * </pre>
 *
 * のような形式を用いる。<br>
 * なお、コンストラクタの引数チェックは追加の引数がある場合について行えばよい。
 *
 * <pre>
 * -- revision history --
 * $Log: HashMethod.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version cotrol
 * [change]
 *     requiredSize() を hashRequires() に名前変更。
 *     size() を tableSize() 名前変更。
 * [maintanance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public interface HashMethod {

    /**
     * ハッシュ関数。
     * コンストラクタで渡された TextBuffer 内の
     * position からのデータパタンの hash値を生成する。
     *
     * @param position データパタンの開始位置
     * @return ハッシュ値
     */
    int hash(int position);

    /**
     * ハッシュ関数が
     * ハッシュ値を生成するために使用するバイト数を得る。
     *
     * @return ハッシュ関数がハッシュ値を
     *         生成するために使用するバイト数
     */
    int hashRequires();

    /**
     * この HashMethod を使った場合の
     * HashTable のサイズを得る。
     *
     * @return この HashMethod を使った場合の HashTable のサイズ
     */
    int tableSize();
}
