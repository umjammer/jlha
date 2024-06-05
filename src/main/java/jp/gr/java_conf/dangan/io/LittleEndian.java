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
import java.io.OutputStream;


/**
 * リトルエンディアンで
 * バイト配列や InputStream, OutputStream
 * にアクセスするメソッドを提供するユーティリティクラス。
 *
 * <pre>
 * -- revision history --
 * $Log: LittleEndian.java,v $
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [change]
 *     writeByte(), readByte() を撤去。
 * [maintenance]
 *     ソース整備
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class LittleEndian {

    /**
     * デフォルトコンストラクタ。使用不可。
     */
    private LittleEndian() {
    }

    /**
     * ByteArray の index の位置から リトルエンディアンで
     * 2バイト値を読み出す。読み出された 2バイト値は
     * 0x0000〜0xFFFFにマップされる。
     *
     * @param ByteArray バイト配列
     * @param index     ByteArray内のデータの開始位置
     * @return 読み出された2バイト値
     * @throws ArrayIndexOutOfBoundsException indexから始まるデータがByteArrayの範囲内に無い場合。
     */
    public static int readShort(byte[] ByteArray, int index) {
        return (ByteArray[index] & 0xFF) | ((ByteArray[index + 1] & 0xFF) << 8);
    }

    /**
     * ByteArray の index の位置からリトルエンディアンで
     * 4バイト値を読み出す。
     *
     * @param ByteArray バイト配列
     * @param index     ByteArray内のデータの開始位置
     * @return 読み出された4バイト値
     * @throws ArrayIndexOutOfBoundsException indexから始まるデータがByteArrayの範囲内に無い場合。
     */
    public static int readInt(byte[] ByteArray, int index) {
        return (ByteArray[index] & 0xFF) | ((ByteArray[index + 1] & 0xFF) << 8) | ((ByteArray[index + 2] & 0xFF) << 16)
                | (ByteArray[index + 3] << 24);
    }

    /**
     * ByteArray の index の位置からリトルエンディアンで
     * 8バイト値を読み出す。
     *
     * @param ByteArray バイト配列
     * @param index     ByteArray内のデータの開始位置
     * @return 読み出された8バイト値
     * @throws ArrayIndexOutOfBoundsException indexから始まるデータがByteArrayの範囲内に無い場合。
     */
    public static long readLong(byte[] ByteArray, int index) {
        return (LittleEndian.readInt(ByteArray, index) & 0xFFFFFFFFL)
                | ((long) LittleEndian.readInt(ByteArray, index + 4) << 32L);
    }

    /**
     * 入力ストリーム in から リトルエンディアンで
     * 2byte値を読み出す。
     *
     * @param in 入力ストリーム
     * @return 読み出された2byte値
     * @throws EOFException 既に End Of Streamに達していたか、読み込みの途中で End Of Streamに達した。読み込み途中のデータは消失する。
     * @throws IOException  入出力エラーが発生した場合
     */
    public static int readShort(InputStream in) throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();

        if (0 <= byte1 && 0 <= byte2) {
            return (byte1 & 0xFF) | ((byte2 & 0xFF) << 8);
        } else {
            throw new EOFException();
        }
    }

    /**
     * 入力ストリーム in から リトルエンディアンで
     * 4byte値を読み出す。
     *
     * @param in 入力ストリーム
     * @return 読み出された4byte値
     * @throws EOFException 既に End Of Streamに達していたか、読み込みの途中で End Of Streamに達した。読み込み途中のデータは消失する。
     * @throws IOException  入出力エラーが発生した場合
     */
    public static int readInt(InputStream in) throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();
        int byte3 = in.read();
        int byte4 = in.read();

        if (0 <= byte1 && 0 <= byte2 && 0 <= byte3 && 0 <= byte4) {
            return (byte1 & 0xFF) | ((byte2 & 0xFF) << 8) | ((byte3 & 0xFF) << 16) | (byte4 << 24);
        } else {
            throw new EOFException();
        }
    }

    /**
     * 入力ストリーム in から リトルエンディアンで
     * 8byte値を読み出す。
     *
     * @param in 入力ストリーム
     * @return 読み出された8byte値
     * @throws EOFException 既に End Of Streamに達していたか、読み込みの途中で End Of Streamに達した。読み込み途中のデータは消失する。
     * @throws IOException  入出力エラーが発生した場合
     */
    public static long readLong(InputStream in) throws IOException {

        return (LittleEndian.readInt(in) & 0xFFFFFFFFL) | ((long) LittleEndian.readInt(in) << 32);

    }

    /**
     * ByteArray の index の位置にリトルエンディアンで
     * 2byte値を書き出す。
     *
     * @param ByteArray バイト配列
     * @param index     ByteArray内のデータを書きこむ位置
     * @param value     書きこむ 2byte値
     * @throws ArrayIndexOutOfBoundsException indexから始まるデータがByteArrayの範囲内に無い場合。
     */
    public static void writeShort(byte[] ByteArray, int index, int value) {

        if (0 <= index && index + 1 < ByteArray.length) {
            ByteArray[index] = (byte) value;
            ByteArray[index + 1] = (byte) (value >> 8);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * ByteArray の index の位置にリトルエンディアンで
     * 4byte値を書き出す。
     *
     * @param ByteArray バイト配列
     * @param index     ByteArray内のデータを書きこむ位置
     * @param value     書きこむ 4byte値
     * @throws ArrayIndexOutOfBoundsException indexから始まるデータがByteArrayの範囲内に無い場合。
     */
    public static void writeInt(byte[] ByteArray, int index, int value) {

        if (0 <= index && index + 3 < ByteArray.length) {
            ByteArray[index] = (byte) value;
            ByteArray[index + 1] = (byte) (value >> 8);
            ByteArray[index + 2] = (byte) (value >> 16);
            ByteArray[index + 3] = (byte) (value >> 24);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * ByteArray の index の位置にリトルエンディアンで
     * 8byte値を書き出す。
     *
     * @param ByteArray バイト配列
     * @param index     ByteArray内のデータを書きこむ位置
     * @param value     書きこむ 8byte値
     * @throws ArrayIndexOutOfBoundsException indexから始まるデータがByteArrayの範囲内に無い場合。
     */
    public static void writeLong(byte[] ByteArray, int index, long value) {
        if (0 <= index && index + 7 < ByteArray.length) {
            LittleEndian.writeInt(ByteArray, index, (int) value);
            LittleEndian.writeInt(ByteArray, index + 4, (int) (value >> 32));
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * 出力ストリーム out に リトルエンディアンで
     * 2バイト書き出す。
     *
     * @param out   出力ストリーム
     * @param value 書き出す2バイト値
     * @throws IOException 入出力エラーが発生した場合
     */
    public static void writeShort(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    /**
     * 出力ストリーム out に リトルエンディアンで
     * 4バイト値を書き出す。
     *
     * @param out   出力ストリーム
     * @param value 書き出す1バイト値
     * @throws IOException 入出力エラーが発生した場合
     */
    public static void writeInt(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write(value >>> 24);
    }

    /**
     * 出力ストリーム out に リトルエンディアンで
     * 8バイト値を書き出す。
     *
     * @param out   出力ストリーム
     * @param value 書き出す1バイト値
     * @throws IOException 入出力エラーが発生した場合
     */
    public static void writeLong(OutputStream out, long value) throws IOException {
        int low = (int) value;
        int hi = (int) (value >> 32);

        out.write(low & 0xFF);
        out.write((low >> 8) & 0xFF);
        out.write((low >> 16) & 0xFF);
        out.write(low >>> 24);
        out.write(hi & 0xFF);
        out.write((hi >> 8) & 0xFF);
        out.write((hi >> 16) & 0xFF);
        out.write(hi >>> 24);
    }
}
