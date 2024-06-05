/*
 * Copyright (C) 2001  Michel Ishizuka  All rights reserved.
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
import java.io.IOException;


/**
 * LZSS圧縮コードを供給するインターフェイス。
 *
 * <pre>
 * -- revision history --
 * $Log: PreLzssDecoder.java,v $
 * Revision 1.0  2002/07/25 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public interface PreLzssDecoder {

    /**
     * 接続された入力ストリームの現在位置にマークを設定し、
     * reset() メソッドでマークした時点の読み込み位置に戻れるようにする。<br>
     * InputStream の mark() と違い、 readLimit で設定した
     * 限界バイト数より前にマーク位置が無効になってもかまわない。
     * ただし、readLimit を無視して無限に reset() 可能な
     * InputStream と接続している場合は readLimit にどのような値を設定されても
     * reset() で必ずマーク位置に復旧できなければならない。<br>
     *
     * @param readLimit マーク位置に戻れる限界のバイト数。
     *                  このバイト数を超えてデータを読み込んだ場合reset()できなくなる可能性がある。
     */
    void mark(int readLimit);

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。<br>
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    void reset() throws IOException;

    /**
     * 接続された入力ストリームが mark() と reset() を
     * サポートするかを得る。<br>
     *
     * @return ストリームが mark() と reset() をサポートする場合は true。サポートしない場合は false。<br>
     */
    boolean markSupported();

    /**
     * 接続された入力ストリームからブロックしないで
     * 読み込むことのできる最低バイト数を得る。<br>
     * この数値は完全である事を保障しなくてよい。
     * これは故意に作成されたデータ等ではブロックせずに
     * 読み込む事の出来る最低バイト数を得るには
     * 実際に読み込んでみる以外に方法がないためである。
     *
     * @return ブロックしないで読み出せる最低バイト数。
     * @throws IOException 入出力エラーが発生した場合
     */
    int available() throws IOException;

    /**
     * この入力ストリームを閉じ、使用していた
     * 全てのリソースを開放する。<br>
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    void close() throws IOException;

    /**
     * 1byte の LZSS未圧縮のデータもしくは、
     * LZSS で圧縮された圧縮コードのうち一致長を読み込む。<br>
     * 未圧縮データは 0〜255、
     * LZSS圧縮コード(一致長)は 256〜511 の値を取らなければならない。
     *
     * @return 1byte の LZSS未圧縮のデータもしくは、LZSS で圧縮された圧縮コードのうち一致長
     * @throws IOException  入出力エラーが発生した場合
     * @throws EOFException EndOfStreamに達した場合
     */
    int readCode() throws IOException;

    /**
     * LZSS で圧縮された圧縮コードのうち一致位置を読み込む。
     *
     * @return LZSS で圧縮された圧縮コードのうち一致位置
     * @throws IOException 入出力エラーが発生した場合
     */
    int readOffset() throws IOException;

    /**
     * このPreLzssDecoderが処理するLZSS辞書のサイズを得る。
     *
     * @return LZSS辞書のサイズ
     */
    int getDictionarySize();

    /**
     * このPreLzssDecoderが処理する最長一致長を得る。
     *
     * @return 最長一致長
     */
    int getMaxMatch();

    /**
     * このPreLzssDecoderが処理する圧縮、非圧縮の閾値を得る。
     *
     * @return LZSSの閾値
     */
    int getThreshold();
}
