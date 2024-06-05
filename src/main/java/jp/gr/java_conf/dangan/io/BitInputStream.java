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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;


/**
 * ビット入力のためのユーティリティクラス。
 *
 * <pre>
 * -- revision history --
 * $Log: BitInputStream.java,v $
 * Revision 1.5  2002/12/07 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.4  2002/11/15 00:00:00  dangan
 * [improvement]
 *     prefetchBits() が  32bit の読み込みを保証するように修正
 * [change]
 *     メソッド名の変更
 *     prefetchBit     -> peekBit
 *     prefetchBoolean -> peekBoolean
 *     prefetchBits    -> peekBits
 *
 * Revision 1.3  2002/11/02 00:00:00  dangan
 * [bug fix]
 *     available() availableBits() で
 *     ブロックせずに読み込める量よりも大きい値を返していた。
 *
 * Revision 1.2  2002/09/05 00:00:00  dangan
 * [change]
 *     EndOfStream に達した後の read( new byte[0] ) や
 *     read( byte[] buf, int off, 0 ) の戻り値を
 *     InputStream と同じく 0 になるようにした
 *
 * Revision 1.1  2002/09/04 00:00:00  dangan
 * [bug fix]
 *     skip( len ) と skipBits( len ) で len が 0 未満のとき
 *     正しく処理できていなかった。
 *
 * Revision 1.0  2002/09/03 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     mark() で 接続された in に渡す readLimit の計算が甘かったため、
 *     要求された readLimit に達する前にマーク位置が破棄される事があった。
 *     EndOfStream に達した後の skip() および skip( 0 ) が -1 を返していた。
 * [maintenance]
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.5 $
 */
public class BitInputStream extends InputStream {

    private static final Logger logger = System.getLogger(BitInputStream.class.getName());

    /**
     * デフォルトのキャッシュサイズ
     */
    private static final int DefaultCacheSize = 1024;

    /**
     * 接続された入力ストリーム
     */
    private InputStream in;

    /**
     * 速度低下抑止用バイト配列
     */
    private byte[] cache;

    /**
     * cache 内の有効バイト数
     */
    private int cacheLimit;

    /**
     * cache 内の現在処理位置
     */
    private int cachePosition;

    /**
     * ビットバッファ。
     * ビットデータは最上位ビットから bitCount だけ格納されている。
     */
    private int bitBuffer;

    /**
     * bitBuffer の 有効ビット数
     */
    private int bitCount;

    /**
     * mark位置がキャッシュの範囲内にあるかを示す。
     * markされたとき true に設定され、
     * 次に in から キャッシュへの読み込みが
     * 行われたときに false に設定される。
     */
    private boolean markPositionIsInCache;

    /** cache の バックアップ用 */
    private byte[] markCache;

    /** cacheLimit のバックアップ用 */
    private int markCacheLimit;

    /** cachePosition のバックアップ用 */
    private int markCachePosition;

    /** bitBuffer のバックアップ用 */
    private int markBitBuffer;

    /** bitCount のバックアップ用 */
    private int markBitCount;

    /**
     * 入力ストリーム in からのデータをビット単位で
     * 読み込めるようなストリームを構築する。
     *
     * @param in 入力ストリーム
     */
    public BitInputStream(InputStream in) {
        this(in, BitInputStream.DefaultCacheSize);
    }

    /**
     * 入力ストリーム in からのデータをビット単位で
     * 読み込めるようなストリームを構築する。
     *
     * @param in        入力ストリーム
     * @param CacheSize バッファサイズ
     */
    public BitInputStream(InputStream in, int CacheSize) {
        if (in != null && 4 <= CacheSize) {
            this.in = in;
            this.cache = new byte[CacheSize];
            this.cacheLimit = 0;
            this.cachePosition = 0;
            this.bitBuffer = 0;
            this.bitCount = 0;

            this.markPositionIsInCache = false;
            this.markCache = null;
            this.markCacheLimit = 0;
            this.markCachePosition = 0;
            this.markBitBuffer = 0;
            this.markBitCount = 0;
        } else if (in == null) {
            throw new NullPointerException("in");
        } else {
            throw new IllegalArgumentException("CacheSize must be 4 or more.");
        }
    }

    /**
     * 接続されたストリームから 8ビットのデータを読み込む。
     *
     * @return 読み出された 8ビットのデータ。既に EndOfStream に達している場合は -1
     * @throws IOException            接続された入力ストリームで入出力エラーが発生した場合
     * @throws BitDataBrokenException EndOfStreamに達したため要求されたビット数のデータの読み込みに失敗した場合。
     */
    @Override
    public int read() throws IOException {
        try {
            return this.readBits(8);
        } catch (LocalEOFException exception) {
            if (exception.thrownBy(this))
                return -1;
            else
                throw exception;
        }
    }

    /**
     * 接続された入力ストリームから バイト配列 buffer を
     * 満たすようにデータを読み込む。<br>
     * データは必ずしも buffer を満たすとは限らないことに注意。
     *
     * @param buffer 読み込まれたデータを格納するためのバイト配列
     * @return buffer に読み込んだデータ量をバイト数で返す。既に EndOfStream に達していた場合は -1 を返す。
     * @throws IOException            接続された入力ストリームで入出力エラーが発生した場合
     * @throws BitDataBrokenException EndOfStreamに達したため要求されたビット数のデータの読み込みに失敗した場合。
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    /**
     * 接続された入力ストリームから バイト配列 buffer の
     * index で指定された位置から length バイトのデータを
     * 読み込む。<br>
     * このメソッドは lengthバイト読み込むか、
     * EndOfStream に到達するまでブロックする。
     * データは必ずしも length バイト読み込まれるとは限ら
     * ないことに注意。<br>
     *
     * @param buffer 読み込まれたデータを格納するためのバイト配列
     * @param index  buffer内のデータ読み込み開始位置
     * @param length bufferに読み込むデータ量
     * @return buffer に読み込んだデータ量をバイト数で返す。既に EndOfStream に達していた場合は -1 を返す。
     * @throws IOException            接続された入力ストリームで入出力エラーが発生した場合
     * @throws BitDataBrokenException EndOfStreamに達したため要求されたビット数のデータの読み込みに失敗した場合。
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        int requested = length;
        try {
            while (0 < length) {
                buffer[index++] = (byte) this.readBits(8);
                length--;
            }
            return requested;
        } catch (LocalEOFException exception) {
            if (exception.thrownBy(this)) {
                if (requested != length)
                    return requested - length;
                else
                    return -1;
            } else {
                throw exception;
            }
        } catch (BitDataBrokenException exception) {
            if (exception.getCause() instanceof LocalEOFException
                    && ((LocalEOFException) exception.getCause()).thrownBy(this)) {
                this.bitBuffer >>>= exception.getBitCount();
                this.bitCount += exception.getBitCount();
                this.bitBuffer |= exception.getBitData() << (32 - exception.getBitCount());

                return requested - length;
            } else {
                throw exception;
            }
        }
    }

    /**
     * 接続された入力ストリームのデータを length バイト
     * 読み飛ばす。<br>
     * このメソッドは lengthバイト読み飛ばすか、
     * EndOfStream に到達するまでブロックする。
     * データは必ずしも length バイト読み飛ばされるとは限ら
     * ないことに注意。
     *
     * @param length 読み飛ばすバイト数。d
     * @return 実際に読み飛ばされたバイト数。
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    @Override
    public long skip(long length) throws IOException {
        length = (0 < length ? length : 0);
        long requested = length;
        try {
            while (0 < length) {
                this.readBits(8);
                length--;
            }
            return requested;
        } catch (LocalEOFException exception) {
            return requested - length;
        } catch (BitDataBrokenException exception) {
            if (exception.getCause() instanceof LocalEOFException
                    && ((LocalEOFException) exception.getCause()).thrownBy(this)) {
                this.bitBuffer >>>= exception.getBitCount();
                this.bitCount += exception.getBitCount();
                this.bitBuffer |= exception.getBitData() << (32 - exception.getBitCount());
                return requested - length;
            } else {
                throw exception;
            }
        }
    }

    /**
     * 接続された入力ストリームの現在位置にマークを設定し、
     * reset() メソッドでマークした時点の 読み込み位置に
     * 戻れるようにする。
     *
     * @param readLimit マーク位置に戻れる限界のバイト数。
     *                  このバイト数を超えてデータを読み込んだ場合{@link #reset()}できなくなる可能性がある。
     */
    @Override
    public void mark(int readLimit) {
        readLimit -= this.cacheLimit - this.cachePosition;
        readLimit -= this.bitCount / 8;
        readLimit += 4;
        readLimit = ((readLimit / this.cache.length) * this.cache.length
                + (readLimit % this.cache.length == 0 ? 0 : this.cache.length));

        this.in.mark(readLimit);

        if (this.markCache == null) {
            this.markCache = this.cache.clone();
        } else {
            System.arraycopy(this.cache, 0, this.markCache, 0, this.cacheLimit);
        }
        this.markCacheLimit = this.cacheLimit;
        this.markCachePosition = this.cachePosition;
        this.markBitBuffer = this.bitBuffer;
        this.markBitCount = this.bitCount;
        this.markPositionIsInCache = true;
    }

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。
     *
     * @throws IOException <od>
     *                     <li> {@link BitInputStream}に{@code mark}がなされていない場合。</li>
     *                     <li> 接続された入力ストリームが markSupported()でfalse を返す場合。</li>
     *                     <li> 接続された入力ストリームで入出力エラーが発生した場合。</li>
     *                     </od>
     *                     の何れか。
     */
    @Override
    public void reset() throws IOException {
        if (this.markPositionIsInCache) {
            this.cachePosition = this.markCachePosition;
            this.bitBuffer = this.markBitBuffer;
            this.bitCount = this.markBitCount;
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
            this.bitBuffer = this.markBitBuffer;
            this.bitCount = this.markBitCount;
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
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    @Override
    public int available() throws IOException {
        return this.availableBits() / 8;
    }

    /**
     * この入力ストリームを閉じ、
     * 使用していたリソースを開放する。
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
        this.bitBuffer = 0;
        this.bitCount = 0;

        this.markCache = null;
        this.markCacheLimit = 0;
        this.markCachePosition = 0;
        this.markBitBuffer = 0;
        this.markBitCount = 0;
        this.markPositionIsInCache = false;
    }

    /**
     * 接続された入力ストリームから 1ビットのデータを
     * 読み込む。
     *
     * @return 読み込まれた1ビットのデータ。既にEndOfStreamに達している場合は -1。
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    public int readBit() throws IOException {
        if (0 < this.bitCount) {
            int bit = this.bitBuffer >>> 31;
            this.bitBuffer <<= 1;
            this.bitCount -= 1;
            return bit;
        } else {
            try {
                this.fillBitBuffer();
                int bit = this.bitBuffer >>> 31;
                this.bitBuffer <<= 1;
                this.bitCount -= 1;
                return bit;
            } catch (LocalEOFException exception) {
                if (exception.thrownBy(this)) {
                    return -1;
                } else {
                    throw exception;
                }
            }
        }
    }

    /**
     * 接続された入力ストリームから 1ビットのデータを
     * 真偽値として読み込む。
     *
     * @return 読み込まれた1ビットのデータが 1であれば true、0であれば false を返す。
     * @throws EOFException 既にEndOfStreamに達していた場合
     * @throws IOException  接続された入力ストリームで入出力エラーが発生した場合
     */
    public boolean readBoolean() throws IOException {
        if (0 < this.bitCount) {
            boolean bool = (this.bitBuffer < 0);
            this.bitBuffer <<= 1;
            this.bitCount -= 1;
            return bool;
        } else {
            this.fillBitBuffer();
            boolean bool = (this.bitBuffer < 0);
            this.bitBuffer <<= 1;
            this.bitCount -= 1;
            return bool;
        }
    }

    /**
     * 接続された入力ストリームから count ビットのデータを
     * 読み込む。 戻り値が int値である事からも判るように
     * 読み込むことのできる 最大有効ビット数は 32ビットで
     * あるが、count は32以上の値を設定してもチェックを
     * 受けないため それ以上の値を設定した場合は ビット
     * データが読み捨てられる。<br>
     * たとえば readBits( 33 ) としたときは まず1ビットの
     * データを読み捨て、その後の 32ビットのデータを返す。<br>
     * また count に 0以下の数字を設定して呼び出した場合、
     * データを読み込む動作を伴わないため 戻り値は 常に0、
     * EndOfStream に達していても EOFException を
     * 投げない点に注意すること。
     *
     * @param count 読み込むデータのビット数
     * @return 読み込まれたビットデータ。
     * @throws IOException            接続された入力ストリームで入出力エラーが発生した場合
     * @throws EOFException           既にEndOfStreamに達していた場合
     * @throws BitDataBrokenException 読み込み途中で EndOfStreamに達したため要求されたビット数のデータの読み込みに失敗した場合。
     */
    public int readBits(int count) throws IOException {
        if (0 < count) {
            if (count <= this.bitCount) {
                int bits = this.bitBuffer >>> (32 - count);
                this.bitBuffer <<= count;
                this.bitCount -= count;
                return bits;
            } else {
                int requested = count;
                int bits = 0;
                try {
                    this.fillBitBuffer();
                    while (this.bitCount < count) {
                        count -= this.bitCount;
                        if (count < 32) {
                            bits |= (this.bitBuffer >>> (32 - this.bitCount)) << count;
                        }
                        this.bitBuffer = 0;
                        this.bitCount = 0;
                        this.fillBitBuffer();
                    }
                    bits |= this.bitBuffer >>> (32 - count);
                    this.bitBuffer <<= count;
                    this.bitCount -= count;
                    return bits;
                } catch (LocalEOFException exception) {
                    if (exception.thrownBy(this) && count < requested) {
                        throw new BitDataBrokenException(exception, bits >>> count, requested - count);
                    } else {
                        throw exception;
                    }
                }
            }
        } else {
            return 0;
        }
    }

    /**
     * 接続されたストリームから count ビットのデータを
     * 読み飛ばす。
     *
     * @param count 読み飛ばしてほしいビット数
     * @return 実際に読み飛びしたビット数
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    public int skipBits(int count) throws IOException {
        count = Math.max(count, 0);

        if (count < this.bitCount) {
            this.bitBuffer <<= count;
            this.bitCount -= count;
            return count;
        } else {
            int requested = count;
            count -= this.bitCount;
            this.bitCount = 0;
            this.bitBuffer = 0;
            try {
                while ((this.cacheLimit - this.cachePosition) * 8 <= count) {
                    count -= (this.cacheLimit - this.cachePosition) * 8;
                    this.cachePosition = this.cacheLimit;
                    this.fillCache();
                    if (this.cacheLimit == this.cachePosition) {
                        throw new LocalEOFException(this);
                    }
                }
                this.cachePosition += (count >> 3);
                count = count & 0x07;
                if (0 < count) {
                    this.bitCount = 8 - count;
                    this.bitBuffer = this.cache[this.cachePosition++] << (24 + count);
                    count = 0;
                }
            } catch (LocalEOFException exception) {
                logger.log(Level.ERROR, exception.getMessage(), exception);
            }
            return requested - count;
        }
    }

    /**
     * 読み込み位置を変えずに 1ビットのデータを先読みする。
     *
     * @return 読み込まれた1ビットのデータ。<br>
     * 既にEndOfStreamに達している場合は -1。
     * @throws IOException 接続された入力ストリームで
     *                     入出力エラーが発生した場合
     */
    public int peekBit() throws IOException {
        if (0 < this.bitCount) {
            return this.bitBuffer >>> 31;
        } else {
            try {
                this.fillBitBuffer();
                return this.bitBuffer >>> 31;
            } catch (LocalEOFException exception) {
                if (exception.thrownBy(this)) {
                    return -1;
                } else {
                    throw exception;
                }
            }
        }
    }

    /**
     * 読み込み位置を変えずに 1ビットのデータを
     * 真偽値として先読みする。
     *
     * @return 読み込まれた1ビットのデータが
     * 1であれば true、0であれば false を返す。
     * @throws EOFException 既にEndOfStreamに達していた場合
     * @throws IOException  接続された入力ストリームで
     *                      入出力エラーが発生した場合
     */
    public boolean peekBoolean() throws IOException {
        if (0 < this.bitCount) {
            return this.bitBuffer < 0;
        } else {
            this.fillBitBuffer();
            return this.bitBuffer < 0;
        }
    }

    /**
     * 読み込み位置を変えずに count ビットのデータを先読みする。<br>
     * 戻り値が int型であることからもわかるように
     * 最大有効ビット数は 32ビットである。<br>
     * EndOfStream 付近を除いて、先読み出来ることが保障されるのは
     * 32ビットである。(ビットバッファの大きさが 32ビットであるため)<br>
     * もし 32ビット以上の先読み機能が必須となる場合は
     * その都度 mark()、readBits()、reset() を繰り返すか、
     * このクラスを使用することを諦めること。<br>
     *
     * @param count 読み込むビット数
     * @return 先読みした count ビットのビットデータ
     * @throws EOFException           既にEndOfStreamに達していた場合
     * @throws IOException            接続された入力ストリームで入出力エラーが発生した場合
     * @throws NotEnoughBitsException count が先読み可能な範囲外の場合
     */
    public int peekBits(int count) throws IOException {
        if (0 < count) {
            if (count <= this.bitCount) {
                return this.bitBuffer >>> (32 - count);
            } else {
                this.fillBitBuffer();
                if (count <= this.bitCount) {
                    return this.bitBuffer >>> (32 - count);
                } else if (count <= this.cachedBits()) {
                    if (count <= 32) {
                        int bits = this.bitBuffer;
                        bits |= (this.cache[this.cachePosition] & 0xFF) >> (this.bitCount - 24);
                        return bits >>> (32 - count);
                    } else if (count - 32 < this.bitCount) {
                        int bits = this.bitBuffer << (count - 32);
                        int bcnt = this.bitCount - (count - 32);
                        int pos = this.cachePosition;
                        while (bcnt < 25) {
                            bits |= (this.cache[pos++] & 0xFF) << (24 - bcnt);
                            bcnt += 8;
                        }
                        if (bcnt < 32) {
                            bits |= (this.cache[pos] & 0xFF) >> (bcnt - 24);
                        }
                        return bits;
                    } else {
                        count -= this.bitCount;
                        count -= 32;
                        int pos = this.cachePosition + (count >> 3);
                        count &= 0x07;
                        if (0 < count) {
                            return (this.cache[pos] << (24 + count)) | ((this.cache[pos + 1] & 0xFF) << (16 + count))
                                    | ((this.cache[pos + 2] & 0xFF) << (8 + count)) | ((this.cache[pos + 3] & 0xFF) << count)
                                    | ((this.cache[pos + 4] & 0xFF) >> (8 - count));
                        } else {
                            return (this.cache[pos] << 24) | ((this.cache[pos + 1] & 0xFF) << 16)
                                    | ((this.cache[pos + 2] & 0xFF) << 8) | (this.cache[pos + 3] & 0xFF);
                        }
                    }
                } else {
                    throw new NotEnoughBitsException(this.cachedBits());
                }
            }
        } else {
            return 0;
        }
    }

    /**
     * 接続された入力ストリームからブロックしないで
     * 読み込むことのできるビット数を得る。
     *
     * @return ブロックしないで読み出せるビット数。
     * @throws IOException 接続された入力ストリームで入出力エラーが発生した場合
     */
    public int availableBits() throws IOException {
        int avail = (this.cacheLimit - this.cachePosition) + this.in.available() / this.cache.length * this.cache.length;
        avail += this.bitCount - 32;

        return Math.max(avail, 0);
    }

    /**
     * この BitInputStream 内に蓄えられているビット数を得る。
     *
     * @return この BitInputStream 内に蓄えられているビット数。
     */
    private int cachedBits() {
        return this.bitCount + ((this.cacheLimit - this.cachePosition) << 3);
    }

    /**
     * bitBuffer にデータを満たす。
     * EndOfStream 付近を除いて bitBuffer には
     * 25bit のデータが確保されることを保障する。
     *
     * @throws IOException       入出力エラーが発生した場合
     * @throws LocalEOFException bitBuffer が空の状態で EndOfStream に達した場合
     */
    private void fillBitBuffer() throws IOException {
        if (32 <= this.cachedBits()) {
            if (this.bitCount <= 24) {
                if (this.bitCount <= 16) {
                    if (this.bitCount <= 8) {
                        if (this.bitCount <= 0) {
                            this.bitBuffer = this.cache[this.cachePosition++] << 24;
                            this.bitCount = 8;
                        }
                        this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                        this.bitCount += 8;
                    }
                    this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                    this.bitCount += 8;
                }
                this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                this.bitCount += 8;
            }
        } else if (this.bitCount < 25) {
            if (this.bitCount == 0) {
                this.bitBuffer = 0;
            }

            int count = Math.min((32 - this.bitCount) >> 3, this.cacheLimit - this.cachePosition);
            while (0 < count--) {
                this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                this.bitCount += 8;
            }
            this.fillCache();
            if (this.cachePosition < this.cacheLimit) {
                count = Math.min((32 - this.bitCount) >> 3, this.cacheLimit - this.cachePosition);
                while (0 < count--) {
                    this.bitBuffer |= (this.cache[this.cachePosition++] & 0xFF) << (24 - this.bitCount);
                    this.bitCount += 8;
                }
            } else if (this.bitCount <= 0) {
                throw new LocalEOFException(this);
            }
        }
    }

    /**
     * cache が空になった時に cache にデータを読み込む。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    private void fillCache() throws IOException {
        this.markPositionIsInCache = false;
        this.cacheLimit = 0;
        this.cachePosition = 0;

        // cache にデータを読み込む
        int read = 0;
        while (0 <= read && this.cacheLimit < this.cache.length) {
            read = this.in.read(this.cache, this.cacheLimit, this.cache.length - this.cacheLimit);

            if (0 < read)
                this.cacheLimit += read;
        }
    }

    /**
     * BitInputStream 内で EndOfStream の検出に
     * EOFException を使用するのは少々問題があるので
     * ローカルな EOFException を定義する。
     */
    private static class LocalEOFException extends EOFException {

        /**
         * この例外を投げたオブジェクト
         */
        private final Object owner;

        /**
         * コンストラクタ。
         *
         * @param object この例外を投げたオブジェクト
         */
        public LocalEOFException(Object object) {
            super();
            this.owner = object;
        }

        /**
         * この例外が{@code object}によって投げられたかどうかを得る。
         *
         * @param object オブジェクト
         * @return この例外が{@code object}によって投げられた例外であれば{@code true}違えば{@code false}
         */
        public boolean thrownBy(Object object) {
            return this.owner == object;
        }
    }
}
