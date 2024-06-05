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
 * 接続された出力ストリームにビットデータを出力するための
 * 出力ストリームクラス。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: BitOutputStream.java,v $
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/09/11 00:00:00  dangan
 * add to version control
 * [change]
 *     close() 後の write系メソッドと flush() で
 *     例外を投げるように修正
 * [maintenance]
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class BitOutputStream extends OutputStream {

    /**
     * デフォルトキャッシュサイズ
     */
    private static final int DefaultCacheSize = 1024;

    /**
     * 接続された出力ストリーム
     */
    private OutputStream out;

    /**
     * 速度低下抑止用バイト配列
     */
    private byte[] cache;

    /**
     * cacheBuffer 内の現在処理位置
     */
    private int cachePosition;

    /**
     * ビットバッファ
     */
    private int bitBuffer;

    /**
     * bitBuffer の 有効ビット数
     */
    private int bitCount;

    /**
     * 出力ストリーム out へ データをビット単位で
     * 書きこめるようなストリームを構築する。<br>
     * キャッシュサイズにはデフォルト値が使用される。
     *
     * @param out 出力ストリーム
     */
    public BitOutputStream(OutputStream out) {
        this(out, BitOutputStream.DefaultCacheSize);
    }

    /**
     * 出力ストリーム out へ データをビット単位で
     * 書きこめるようなストリームを構築する。
     *
     * @param out       出力ストリーム
     * @param CacheSize キャッシュサイズ
     * @throws IllegalArgumentException CacheSize が 4未満の場合、または CacheSize が 4の倍数で無い場合。
     */
    public BitOutputStream(OutputStream out, int CacheSize) {
        if (out != null && 4 <= CacheSize && 0 == (CacheSize & 0x03)) {
            this.out = out;
            this.cache = new byte[CacheSize];
            this.cachePosition = 0;
            this.bitBuffer = 0;
            this.bitCount = 0;
        } else if (out == null) {
            throw new NullPointerException("out");
        } else if (CacheSize < 4) {
            throw new IllegalArgumentException("CacheSize must be 4 or more.");
        } else {
            throw new IllegalArgumentException("CacheSize must be multiple of 4.");
        }
    }

    /**
     * 接続された出力ストリームに 8ビットのデータを出力する。
     *
     * @param data 8ビットのデータ。上位24ビットは無視される。
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void write(int data) throws IOException {
        this.writeBits(8, data);
    }

    /**
     * 接続された出力ストリームにbufferの内容を連続した
     * 8ビットのデータとして出力する。
     *
     * @param buffer 出力すべきデータを格納したバイト配列
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void write(byte[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    /**
     * 接続された出力ストリームにbufferのindexから
     * lengthバイトの内容を連続した 8ビットのデータ
     * として出力する。
     *
     * @param buffer 出力すべきデータを格納したバイト配列
     * @param index  buffer内のデータ開始位置
     * @param length 出力するデータ量(バイト数)
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void write(byte[] buffer, int index, int length) throws IOException {
        if (this.bitCount % 8 == 0) {
            this.flush();
            this.out.write(buffer, index, length);
        } else {
            while (0 < length--)
                this.writeBits(8, buffer[index++]);
        }
    }

    /**
     * このビット出力ストリームにバッファリングされている
     * 8ビット単位のデータを全て出力先に出力する。<br>
     * 8ビットに満たないデータは出力されないことに注意。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void flush() throws IOException {
        while (8 <= this.bitCount) {
            this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 24);
            this.bitBuffer <<= 8;
            this.bitCount -= 8;
        }

        this.out.write(this.cache, 0, this.cachePosition);
        this.cachePosition = 0;
        this.out.flush();
    }

    /**
     * この出力ストリームと、接続された出力ストリームを閉じ、
     * 使用していたリソースを開放する。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        while (0 < this.bitCount) {
            this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 24);
            this.bitBuffer <<= 8;
            this.bitCount -= 8;
        }

        this.out.write(this.cache, 0, this.cachePosition);
        this.cachePosition = 0;
        this.out.flush();
        this.out.close();

        this.out = null;
        this.cache = null;
        this.cachePosition = 0;
        this.bitCount = 128;
        this.bitBuffer = 0;
    }

    /**
     * 接続された出力ストリームに1ビットのデータを出力する。
     *
     * @param data 1ビットのデータ。上位31ビットは無視される。
     * @throws IOException 入出力エラーが発生した場合
     */
    public void writeBit(int data) throws IOException {
        this.bitBuffer |= (data & 0x00000001) << 31 - this.bitCount;
        this.bitCount++;

        if (32 <= this.bitCount)
            this.writeOutBitBuffer();
    }

    /**
     * 真偽値を接続された出力ストリームに1ビットの
     * データとして出力する。<br>
     * true は 1、false は 0として出力する。
     * java.io.DataOutput の writeBoolean() とは
     * 互換性が無いので注意すること。<br>
     *
     * @param bool 真偽値
     * @throws IOException 入出力エラーが発生した場合
     */
    public void writeBoolean(boolean bool) throws IOException {
        if (bool)
            this.bitBuffer |= 1 << 31 - this.bitCount;

        this.bitCount++;

        if (32 <= this.bitCount)
            this.writeOutBitBuffer();
    }

    /**
     * 接続された出力ストリームにビットデータを出力する。
     *
     * @param count data の有効ビット数
     * @param data  ビットデータ
     * @throws IOException 入出力エラーが発生した場合
     */
    public void writeBits(int count, int data) throws IOException {
        while (0 < count) {
            int available = 32 - this.bitCount;
            if (count < available) {
                this.bitBuffer |= (data & (0xFFFFFFFF >>> 32 - count)) << available - count;
                this.bitCount += count;
                count = 0;
            } else {
                count -= available;
                this.bitBuffer |= data >> count & (0xFFFFFFFF >>> 32 - available);
                this.writeOutBitBuffer();
            }
        }
    }

    /**
     * ビットバッファに蓄えられたデータを全てキャッシュに
     * 出力し、キャッシュが満ちた場合はキャッシュのデータを
     * 接続された出力ストリームに出力する。<br>
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    private void writeOutBitBuffer() throws IOException {
        this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 24);
        this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 16);
        this.cache[this.cachePosition++] = (byte) (this.bitBuffer >> 8);
        this.cache[this.cachePosition++] = (byte) this.bitBuffer;

        this.bitBuffer = 0;
        this.bitCount = 0;

        if (this.cache.length <= this.cachePosition) {
            this.out.write(this.cache);
            this.cachePosition = 0;
        }
    }
}
