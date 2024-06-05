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

package jp.gr.java_conf.dangan.io;

import java.io.IOException;
import java.io.InputStream;


/**
 * データを供給する入力ストリームと データを処理する
 * 入力ストリームとの接続を解除するためのユーティリティクラス。<br>
 * java.io.BufferedInputStream 等のバッファリングするストリーム
 * との接続を解除する場合は
 * jp.gr.java_conf.dangan.io.LimitedInputStream 等を使用して
 * 接続解除位置を過ぎたバッファリングを抑止する必要がある。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: DisconnectableInputStream.java,v $
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     タブ廃止
 *     ライセンス文の修正
 *     ソース整備
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class DisconnectableInputStream extends InputStream implements Disconnectable {

    /**
     * 接続された入力ストリーム
     */
    private InputStream in;

    /**
     * in との接続を解除可能な入力ストリームを構築する。
     *
     * @param in 入力ストリーム
     */
    public DisconnectableInputStream(InputStream in) {
        if (in != null) {
            this.in = in;
        } else {
            throw new NullPointerException("in");
        }
    }

    /**
     * 接続された入力ストリームから 次の1バイトのデータを得る。
     *
     * @return 読み込まれた1バイトのデータ。EndOfStreamに達した場合は -1 を返す。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int read() throws IOException {
        return this.in.read();
    }

    /**
     * 接続された入力ストリームから バイト配列 buffer を
     * 満たすようにデータを読み込む。<br>
     * データは必ずしも buffer を満たすとは限らないことに注意。
     *
     * @param buffer 読み込まれたデータを格納するためのバイト配列
     * @return buffer に読み込んだデータ量をバイト数で返す。既に EndOfStream に達していた場合は -1 を返す。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.in.read(buffer, 0, buffer.length);
    }

    /**
     * 接続された入力ストリームから バイト配列 buffer の
     * index で指定された位置から length バイトのデータを読み込む。<br>
     * データは必ずしも length バイト読み込まれるとは限らないことに注意。
     *
     * @param buffer 読み込まれたデータを格納するためのバイト配列
     * @param index buffer内のデータ読み込み開始位置
     * @param length bufferに読み込むデータ量
     * @return buffer に読み込んだデータ量をバイト数で返す。既に EndOfStream に達していた場合は -1 を返す。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        if (0 < length) {
            return this.in.read(buffer, index, length);
        } else {
            return 0;
        }
    }

    /**
     * 接続された入力ストリームのデータを length バイト
     * 読み飛ばす。
     *
     * @param length 読み飛ばすバイト数。
     * @return 実際に読み飛ばされたバイト数。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public long skip(long length) throws IOException {
        if (0 < length) {
            return this.in.skip(length);
        } else {
            return 0;
        }
    }

    /**
     * 接続された入力ストリームの現在位置にマークを設定し、
     * reset() メソッドでマークした時点の 読み込み位置に
     * 戻れるようにする。
     *
     * @param readLimit マーク位置に戻れる限界のバイト数。
     *            このバイト数を超えてデータを読み込んだ場合 reset()できなくなる可能性がある。
     */
    @Override
    public void mark(int readLimit) {
        this.in.mark(readLimit);
    }

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。
     *
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void reset() throws IOException {
        this.in.reset();
    }

    /**
     * 接続された入力ストリームが mark() と reset() を
     * サポートするかを得る。
     *
     * @return ストリームが mark() と reset() をサポートする場合は{@code true}。サポートしない場合は{@code false}。
     */
    public boolean markSupprted() {
        return this.in.markSupported();
    }

    /**
     * 接続された入力ストリームからブロックしないで
     * 読み込むことのできるバイト数を得る。
     *
     * @return ブロックしないで読み出せるバイト数。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int available() throws IOException {
        return this.in.available();
    }

    /**
     * 接続された入力ストリームとの接続を解除する。<br>
     * このメソッドは disconnect() を呼ぶだけである。
     */
    @Override
    public void close() {
        this.disconnect();
    }

    /**
     * 接続された入力ストリームとの接続を解除する。
     */
    @Override
    public void disconnect() {
        this.in = null;
    }
}
