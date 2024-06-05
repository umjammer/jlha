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
import java.io.OutputStream;


/**
 * データを処理して出力する出力ストリームと
 * データをデバイスに出力するストリームとの
 * 接続を解除するためのユーティリティクラス。
 *
 * <pre>
 * -- revision history --
 * $Log: DisconnectableOutputStream.java,v $
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
public class DisconnectableOutputStream extends OutputStream implements Disconnectable {

    /**
     * 接続された出力ストリーム
     */
    private OutputStream out;

    /**
     * out との接続を解除可能な出力ストリームを構築する。
     *
     * @param out 出力ストリーム
     */
    public DisconnectableOutputStream(OutputStream out) {
        if (out != null) {
            this.out = out;
        } else {
            throw new NullPointerException("out");
        }
    }

    /**
     * 接続された出力ストリームに 1バイトのデータを出力する。
     *
     * @param data 書きこまれるべき 1バイトのデータ。一般的に上位3バイトは無視される。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void write(int data) throws IOException {
        this.out.write(data);
    }

    /**
     * 接続された出力ストリームに buffer内のデータを
     * 全て出力する。
     *
     * @param buffer 書きこまれるべきデータを格納したバイト配列。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void write(byte[] buffer) throws IOException {
        this.out.write(buffer, 0, buffer.length);
    }

    /**
     * 接続された出力ストリームに buffer内のデータを
     * indexで指定された位置から lengthバイト出力する。
     *
     * @param buffer 書きこまれるべきデータを格納したバイト配列。
     * @param index buffer内の書きこむべきデータの開始位置。
     * @param length 書きこむべきデータ量。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void write(byte[] buffer, int index, int length) throws IOException {
        this.out.write(buffer, index, length);
    }

    /**
     * 接続された出力ストリームに蓄えられたデータを全て出力する
     * ように指示する。
     *
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    /**
     * 接続された出力ストリームとの接続を解除する。<br>
     * このメソッドは disconnect() を呼び出すだけである。
     */
    @Override
    public void close() {
        this.disconnect();
    }

    /**
     * 接続された出力ストリームとの接続を解除する。
     */
    @Override
    public void disconnect() {
        this.out = null;
    }
}
