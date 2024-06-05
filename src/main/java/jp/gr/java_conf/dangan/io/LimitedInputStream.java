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
 * 読み込み可能なデータ量が制限された入力ストリーム。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: LimitedInputStream.java,v $
 * Revision 1.1.2.1  2003/07/20 17:03:37  dangan
 * [maintenance]
 *     最新の LimitedInputStream からソースを取り込む。
 *
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [change]
 *     EndOfStream に達した後の read( new byte[0] ) や
 *     read( byte[] buf, int off, 0 ) の戻り値を
 *     InputStream と同じく 0 になるようにした
 * [maintenance]
 *     タブ廃止
 *     ライセンス文の修正
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1.2.1 $
 */
public class LimitedInputStream extends InputStream {

    /**
     * 接続された入力ストリーム
     */
    private InputStream in;

    /**
     * 現在読み込み位置
     */
    private long position;

    /**
     * 読み込み限界
     */
    private final long limit;

    /**
     * マーク位置
     */
    private long markPosition;

    /**
     * in からの読み込み可能なデータ量を制限した
     * 入力ストリームを構築する。<br>
     *
     * @param in 入力ストリーム
     * @param limit 読み込み可能バイト数
     *
     * @exception IllegalArgumentException
     *                limit が負数である場合
     */
    public LimitedInputStream(InputStream in, long limit) {
        if (in != null && 0 <= limit) {
            this.in = in;
            this.position = 0;
            this.limit = limit;
            this.markPosition = -1;
        } else if (in == null) {
            throw new NullPointerException("in");
        } else {
            throw new IllegalArgumentException("limit must be 0 or more.");
        }
    }

    /**
     * 接続された入力ストリームから 1バイトのデータを読み込む。
     *
     * @return 読み込まれた 1バイトのデータ既にEndOfStream に達していたか、制限に達した場合は -1 を返す。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int read() throws IOException {
        if (this.position < this.limit) {
            int ret = this.in.read();
            if (0 <= ret) {
                this.position++;
            }
            return ret;
        } else {
            return -1;
        }
    }

    /**
     * 接続された入力ストリームから buffer を満たすように
     * データを読み込む。<br>
     * データは必ずしも buffer を満たすとは限らないことに注意。
     *
     * @param buffer 読み込んだデータを格納するためのバイト配列
     * @return buffer に読み込んだデータ量をバイト数で返す。既にEndOfStream に達していたか、制限に達した場合は -1 を返す。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        if (0 < buffer.length) {
            int ret;
            if (buffer.length < this.limit - this.position) {
                ret = this.in.read(buffer);
            } else if (this.position < this.limit) {
                ret = this.in.read(buffer, 0, (int) (this.limit - this.position));
            } else {
                return -1;
            }
            if (0 < ret) {
                this.position += ret;
            }
            return ret;
        } else {
            return 0;
        }
    }

    /**
     * 接続された入力ストリームから バイト配列 buffer の
     * index で指定された位置から length バイトのデータを
     * 読み込む。<br>
     * データは必ずしも length バイト読み込まれるとは限ら
     * ないことに注意。
     *
     * @param buffer 読み込まれたデータを格納するためのバイト配列
     * @param index buffer内のデータ読み込み開始位置
     * @param length bufferに読み込むデータ量
     * @return buffer に読み込んだデータ量をバイト数で返す。既にEndOfStream に達していたか、制限に達した場合は -1 を返す。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {

        if (0 < length) {
            if (this.limit <= this.position) {
                return -1;
            } else if (this.limit - this.position < length) {
                length = (int) (this.limit - this.position);
            }
            int ret = this.in.read(buffer, index, length);
            if (0 < ret) {
                this.position += ret;
            }
            return ret;
        } else {
            return 0;
        }
    }

    /**
     * 接続された入力ストリームのデータを length バイト読み飛ばす。
     *
     * @param length 読み飛ばすバイト数。
     * @return 実際に読み飛ばされたバイト数。
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public long skip(long length) throws IOException {

        if (0 < length) {
            if (this.limit <= this.position) {
                return 0;
            } else if (this.limit - this.position < length) {
                length = this.limit - this.position;
            }
            length = this.in.skip(length);
            if (0 < length) {
                this.position += length;
            }
            return length;
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
        this.markPosition = this.position;
    }

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。
     *
     * @exception IOException <ol>
     *                <li>LimitedInputStream に mark がなされていない場合。</li>
     *                <li>接続された入力ストリームが markSupported()でfalse を返す場合。</li>
     *                <li>接続された入力ストリームで入出力エラーが発生した場合。</li>
     *                </ol>
     *                の何れか。
     */
    @Override
    public void reset() throws IOException {
        if (!this.in.markSupported()) {
            throw new IOException("not support mark()/reset().");
        } else if (this.markPosition < 0) { // コンストラクタで MarkPosition が -1 に設定されるのを利用する。
            throw new IOException("not marked.");
        } else {
            this.in.reset();
            this.position = this.markPosition;
        }
    }

    /**
     * 接続された入力ストリームが mark() と reset() を
     * サポートするかを得る。
     *
     * @return ストリームが mark() と reset() をサポートする場合は true。サポートしない場合は false。
     */
    @Override
    public boolean markSupported() {
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
        return (int) Math.min(this.in.available(), this.limit - this.position);
    }

    /**
     * この入力ストリームを閉じ、使用していた
     * 全てのリソースを開放する。<br>
     *
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        this.in.close();
        this.in = null;
    }
}
