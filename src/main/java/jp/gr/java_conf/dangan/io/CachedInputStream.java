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

package jp.gr.java_conf.dangan.io;

import java.io.IOException;
import java.io.InputStream;


/**
 * キャッシュを使用して高速化するための入力ストリーム。<br>
 * BufferedInputStream とは read系メソッドが synchronized
 * されていないため、同期処理によるロスがない、mark/reset は
 * キャッシュ内の読み込み位置の移動で行えるときのみサポートであり、
 * それ以上は接続された入力ストリームの性能による、等の違いがある。
 *
 * <pre>
 * -- revision history --
 * $Log: CachedInputStream.java,v $
 * Revision 1.3  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.2  2002/11/02 00:00:00  dangan
 * [bug fix]
 *     available() でブロックせずに読み込める量よりも大きい値を返していた。
 *
 * Revision 1.1  2002/09/05 00:00:00  dangan
 * [change]
 *     EndOfStream に達した後の read( new byte[0] ) や
 *     read( byte[] buf, int off, 0 ) の戻り値を
 *     InputStream と同じく 0 になるようにした。
 *
 * Revision 1.0  2002/09/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     mark() で 接続された in に渡す readLimit の計算が甘かったため、
 *     要求された readLimit に達する前にマーク位置が破棄される事があった。
 *     read( buf, off, len ) 内の System.arraycopy の呼び出しで
 *     dst と src を逆にしていた。
 * [change]
 *     EndOfStream に達した後の read( new byte[0] ) や
 *     read( buf, off，0 )  が -1 を返すように修正。
 * [maintenance]
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.3 $
 */
public class CachedInputStream extends InputStream {

    /**
     * デフォルトのキャッシュサイズ
     */
    private static final int DefaultCacheSize = 1024;

    /**
     * データを供給する入力ストリーム
     */
    private InputStream in;

    /**
     * データを蓄えるためのキャッシュ
     */
    private byte[] cache;

    /**
     * cache内の現在処理位置
     */
    private int cachePosition;

    /**
     * cacheの読み込み限界位置
     */
    private int cacheLimit;

    /**
     * mark位置がキャッシュの範囲内にあるかを示す。
     * markされたとき true に設定され、
     * 次に in から キャッシュへの読み込みが
     * 行われたときに false に設定される。
     */
    private boolean markPositionIsInCache;

    /** cacheのバックアップ用 */
    private byte[] markCache;

    /** cachePositionのバックアップ用 */
    private int markCachePosition;

    /** cacheLimitのバックアップ用 */
    private int markCacheLimit;

    /**
     * デフォルトのサイズのキャッシュを持つ
     * CachedInputStreamを構築する。
     *
     * @param in キャッシュが必要な入力ストリーム
     * @throws IllegalArgumentException in が null だった場合
     */
    public CachedInputStream(InputStream in) {
        this(in, CachedInputStream.DefaultCacheSize);
    }

    /**
     * 指定されたサイズのキャッシュを持つ
     * CachedInputStreamを構築する。
     *
     * @param in        キャッシュが必要な入力ストリーム
     * @param cacheSize キャッシュのサイズ
     * @throws IllegalArgumentException cacheSize が 0以下であるか、in が null だった場合
     */
    public CachedInputStream(InputStream in, int cacheSize) {
        if (in != null && 0 < cacheSize) {
            this.in = in;

            this.cache = new byte[cacheSize];
            this.cachePosition = 0;
            this.cacheLimit = 0;

            this.markPositionIsInCache = false;
            this.markCache = null;
            this.markCachePosition = 0;
            this.markCacheLimit = 0;

        } else if (in == null) {
            throw new IllegalArgumentException("in must not be null.");
        } else {
            throw new IllegalArgumentException("cacheSize must be one or more.");
        }
    }

    /**
     * 接続されたストリームから 1バイトのデータを
     * 0〜255 にマップして読み込む。
     *
     * @return 読み出された 1バイトのデータを返す。既に EndOfStreamに達していた場合は -1を返す。
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    @Override
    public int read() throws IOException {
        if (this.cachePosition < this.cacheLimit) {
            return this.cache[this.cachePosition++] & 0xFF;
        } else {
            this.fillCache();

            if (this.cachePosition < this.cacheLimit) {
                return this.cache[this.cachePosition++] & 0xFF;
            } else {
                return -1;
            }
        }
    }

    /**
     * 接続されたストリームから bufferを満たすように
     * データを読み込む。<br>
     * このメソッドは buffer を満たすまでデータを読み込むか、
     * EndOfStreamに到達するまでブロックする。
     *
     * @param buffer 読み込んだデータを格納するためのバイト配列
     * @return buffer に読み込んだデータ量をバイト数で返す。既に EndOfStreamに達していた場合は -1を返す。
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    /**
     * 接続されたストリームから buffer に index で指定された
     * 位置へ length バイトデータを読み込む。<br>
     * このメソッドは length バイト読み込むか、
     * EndOfStreamに到達するまでブロックする。
     *
     * @param buffer 読み込んだデータを格納するためのバイト配列
     * @param index  buffer内のデータ読み込み開始位置
     * @param length bufferに読み込むデータ量
     * @return buffer に読み込んだデータ量をバイト数で返す。既に EndOfStreamに達していた場合は -1を返す。
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        int requested = length;

        while (0 < length) {
            if (this.cacheLimit <= this.cachePosition) {
                this.fillCache();
                if (this.cacheLimit <= this.cachePosition) {
                    if (requested == length) {
                        return -1;
                    } else {
                        break;
                    }
                }
            }

            int copylen = Math.min(length, this.cacheLimit - this.cachePosition);
            System.arraycopy(this.cache, this.cachePosition, buffer, index, copylen);

            index += copylen;
            length -= copylen;
            this.cachePosition += copylen;
        }

        return requested - length;
    }

    /**
     * 接続された入力ストリームのデータを length バイト読み飛ばす。<br>
     * このメソッドは length バイト読み飛ばすかEndOfStream に到達するまでブロックする。
     *
     * @param length 読み飛ばすバイト数。
     * @return 実際に読み飛ばされたバイト数。
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    @Override
    public long skip(long length) throws IOException {
        long requested = length;

        while (0 < length) {
            if (this.cacheLimit <= this.cachePosition) {
                this.fillCache();

                if (this.cacheLimit <= this.cachePosition) {
                    break;
                }
            }

            long skiplen = Math.min(length, this.cacheLimit - this.cachePosition);

            length -= skiplen;
            this.cachePosition += (int) skiplen;
        }

        return requested - length;
    }

    /**
     * 接続された入力ストリームの現在位置にマークを設定し、
     * reset() メソッドでマークした時点の 読み込み位置に
     * 戻れるようにする。<br>
     *
     * @param readLimit マーク位置に戻れる限界のバイト数。このバイト数を超えてデータを読み
     *                  込んだ場合 reset()できなくなる可能性がある。
     */
    @Override
    public void mark(int readLimit) {
        readLimit -= this.cacheLimit - this.cachePosition;
        readLimit = (readLimit / this.cache.length) * this.cache.length
                + (readLimit % this.cache.length == 0 ? 0 : this.cache.length);

        this.in.mark(readLimit);

        if (this.markCache == null) {
            this.markCache = this.cache.clone();
        } else {
            System.arraycopy(this.cache, 0, this.markCache, 0, this.cacheLimit);
        }

        this.markCacheLimit = this.cacheLimit;
        this.markCachePosition = this.cachePosition;
        this.markPositionIsInCache = true;
    }

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。
     *
     * @throws IOException <ol>
     *                     <li> CachedInputStream に mark がなされていない場合。</li>
     *                     <li> 接続された入力ストリームが markSupported()でfalse を返す場合。</li>
     *                     <li> 接続された入力ストリームで入出力エラーが発生した場合。</li>
     *                     </ol>
     *                     の何れか。
     */
    @Override
    public void reset() throws IOException {
        if (this.markPositionIsInCache) {
            this.cachePosition = this.markCachePosition;
        } else if (!this.in.markSupported()) {
            throw new IOException("not support mark()/reset().");
        } else if (this.markCache == null) { // この条件式は未だにマークされていないことを示す。コンストラクタで markCache が null に設定されるのを利用する。
            throw new IOException("not marked.");
        } else {
            // in が reset() できない場合は
            // 最初の行の this.in.reset() で
            // IOException を投げることを期待している。
            this.in.reset();
            System.arraycopy(this.markCache, 0, this.cache, 0, this.markCacheLimit);
            this.cacheLimit = this.markCacheLimit;
            this.cachePosition = this.markCachePosition;
        }
    }

    /**
     * 接続された入力ストリームが mark() と reset() をサポートするかを得る。
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
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    @Override
    public int available() throws IOException {
        return this.cacheLimit - this.cachePosition + (this.in.available() / this.cache.length) * this.cache.length;
    }

    /**
     * この入力ストリームを閉じ、使用していた
     * 全てのリソースを開放する。
     *
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        this.in.close();
        this.in = null;

        this.cache = null;
        this.cacheLimit = 0;
        this.cachePosition = 0;

        this.markCache = null;
        this.markCacheLimit = 0;
        this.markCachePosition = 0;
        this.markPositionIsInCache = false;
    }

    /**
     * 必要がある場合に、キャッシュ用バッファにデータを
     * 補填しキャッシュ用バッファに必ずデータが存在する
     * ことを保証するために呼ばれる。<br>
     * もし EndOfStream まで読み込まれている場合は データが
     * 補填されないことによって それを示す。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    private void fillCache() throws IOException {
        this.markPositionIsInCache = false;
        this.cacheLimit = 0;
        this.cachePosition = 0;

        // キャッシュにデータを読み込み
        int read = 0;
        while (0 <= read && this.cacheLimit < this.cache.length) {
            read = this.in.read(this.cache, this.cacheLimit, this.cache.length - this.cacheLimit);

            if (0 < read)
                this.cacheLimit += read;
        }
    }
}
