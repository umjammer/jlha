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

package jp.gr.java_conf.dangan.util.lha;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import jp.gr.java_conf.dangan.io.LittleEndian;
import jp.gr.java_conf.dangan.util.MsdosDate;


/**
 * LHAヘッダを扱う。<br>
 * このクラスは java.util.zip パッケージでは ZipEntry と近いが、
 * ヘッダの入出力のためのユーティリティ関数を持つ点が違う。<br>
 * このクラスは set系メソッドで為された方が良いチェックを
 * getBytes() 時に行うように書かれている。その点は注意すること。
 *
 * <pre>
 * -- revision history --
 * $Log: LhaHeader.java,v $
 * Revision 1.2.2.3  2005/05/03 07:50:30  dangan
 * [bug fix]
 *     exportLevel1Header() で skip size のチェックがされていなかった。
 *
 * Revision 1.2.2.2  2005/02/02 00:57:46  dangan
 * [bug fix]
 *     importLevelXHeader(byte[], String) でファイルサイズを int で読み込んでいたため
 *     31ビット値以上のサイズのファイルを正しく扱えていなかったのを修正。
 *
 * Revision 1.2.2.1  2003/07/20 13:19:21  dangan
 * [bug fix]
 *     exportDirNameExtHeader(String) で System.arraycopy の src と dest の配置が間違っていた。
 *
 * Revision 1.2  2002/12/08 00:00:00  dangan
 * [maintenance]
 *     LhaConstants から CompressMethod へのクラス名の変更に合わせて修正。
 *
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [improvement]
 *     64ビットファイルサイズヘッダに対応。
 * [change]
 *     LhaUtil.DefaultEncoding から LhaProperty.encoding を使用するように変更。
 *     getNextHeaderData() を getFirstHeaderData() に名前変更。
 *     新しい getNextHeaderData() は呼び出された位置で
 *     ヘッダを発見できない場合 null を返す。
 *     LhaHeader を拡張したサブクラスを使用する人のための createInstance() を追加。
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     setDate( null ) を許していた。
 *     setCompressMethod( null ) を許していた。
 *     exportLevel2,3Header で
 *     Date が 32bit の time_t の範囲外の値(負の値を含む)の場合を許していた。
 * [change]
 *     exportHeader で ヘッダレベルが 0,1,2,3 のいずれでもない場合
 *     IllegalStateException を投げるように変更。
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.2.2.3 $
 */
public class LhaHeader implements Cloneable {

    /**
     * 不明を意味する値。
     * LhaHeader.getCRC(), LhaHeader.getCompressedSize(),
     * LhaHeader.getOriginalSzie() がこの値を返した場合は
     * 処理前のために、その値が不明である事を示す。
     */
    public static final int UNKNOWN = -1;

    /**
     * CRC値が無い事を意味する値。
     * レベル0ヘッダでCRC値が存在しない事を意味する。
     */
    public static final int NO_CRC = -2;

    /**
     * 圧縮前サイズ。
     * -1 は処理前のためサイズが不明であることを意味する。
     */
    private long OriginalSize;

    /**
     * 最終更新日時。
     * 圧縮したファイルの最終更新日時。
     */
    private Date LastModified;

    /**
     * パス名。
     * パスデリミタには java.io.File.separator を使用する。
     */
    private String Path;

    /**
     * CRC16 の値。
     * -1 は 処理前のためにCRC16値が不明である事を意味する。
     * -2 は レベル0ヘッダでCRC16値が無い事を意味する。
     */
    private int CRC;

    /**
     * 圧縮法文字列。
     */
    private String Method;

    /**
     * 圧縮後サイズ。
     * -1 は処理前のためサイズが不明であることを意味する。
     */
    private long CompressedSize;

    /**
     * ヘッダレベル。
     * 0,1,2,3の何れか
     */
    private int HeaderLevel;

    /**
     * ヘッダを作成した OS。
     */
    private byte OSID;

    /**
     * レベル0ヘッダもしくは レベル1ヘッダの基本ヘッダ内の
     * 拡張情報があった場合、これを保存する。
     */
    private byte[] ExtraData;

    /**
     * レベル0ヘッダにおける DOSのファイル属性を保存する。
     */
    private byte Level0DosAttribute;

    /**
     * LhaHeaderでは読み込まない情報を持つ拡張ヘッダを保存する。
     */
    private Vector<byte[]> ExtraExtHeaders;

    /**
     * LhaHeaderの各値を初期化する。
     */
    private LhaHeader() {
        this.Method = CompressMethod.LH5;
        this.OriginalSize = LhaHeader.UNKNOWN;
        this.CompressedSize = LhaHeader.UNKNOWN;
        this.LastModified = null;
        this.HeaderLevel = 2;
        this.Path = "";
        this.CRC = LhaHeader.UNKNOWN;
        this.OSID = (byte) 'J';
        this.ExtraData = null;
        this.Level0DosAttribute = 0x20;
        this.ExtraExtHeaders = null;
    }

    /**
     * path という名前を持つ LhaHeader のインスタンスを生成する。<br>
     * パスデリミタには File.separator を使用すること。<br>
     * path が パスデリミタでターミネートされている場合は
     * ディレクトリであると解釈される。
     *
     * @param path パス名
     * @throws IllegalArgumentException path が null か 空文字列のいずれかである場合
     */
    public LhaHeader(String path) {
        this(path, new Date(System.currentTimeMillis()));
    }

    /**
     * path という名前を持ち、最終更新日時が date の
     * LhaHeader のインスタンスを生成する。<br>
     * パスデリミタには File.separator を使用すること。<br>
     * path が パスデリミタでターミネートされている場合は
     * ディレクトリであると解釈される。
     *
     * @param path パス名
     * @param date 最終更新日時
     * @throws IllegalArgumentException path が null か 空文字列のいずれかであるか、date が nullである場合。
     */
    public LhaHeader(String path, Date date) {
        this();
        if (path != null && !path.isEmpty() && date != null) {
            if (path.endsWith(File.separator)) {
                this.Method = CompressMethod.LHD;
            }

            this.Path = path;
            this.LastModified = date;
        } else if (path == null) {
            throw new NullPointerException("path");
        } else if (path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty.");
        } else {
            throw new NullPointerException("date");
        }
    }

    /**
     * ヘッダデータから 新しい LhaHeader の
     * インスタンスを生成する。<br>
     * エンコードは LhaUtil.DefaultEncode が使用される。
     *
     * @param HeaderData ヘッダデータ
     * @throws IndexOutOfBoundsException ヘッダデータが壊れているためデータがあると仮定した位置がHeaderData の範囲外になった
     * @throws IllegalArgumentException  ヘッダレベルが 0,1,2,3 の何れでもないか、HeaderData が null の場合
     */
    public LhaHeader(byte[] HeaderData) {
        this();
        if (HeaderData != null) {
            try {
                this.importHeader(HeaderData, LhaProperty.encoding);
            } catch (UnsupportedEncodingException exception) {
                throw new Error("Java Runtime Environment not support " + LhaProperty.encoding + " encoding");
            }
        } else {
            throw new NullPointerException("HeaderData");
        }
    }

    /**
     * ヘッダデータから 新しい LhaHeader の
     * インスタンスを生成する。
     *
     * @param HeaderData ヘッダデータ
     * @param encode     文字列情報を解釈する際に使用するエンコード
     * @throws IndexOutOfBoundsException    ヘッダデータが壊れているためデータがあると仮定した位置がHeaderData の範囲外になった
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     * @throws IllegalArgumentException     ヘッダレベルが 0,1,2,3 の何れでもないか、HeaderData が null の場合
     */
    public LhaHeader(byte[] HeaderData, String encode) throws UnsupportedEncodingException {
        this();
        if (HeaderData != null && encode != null) {
            this.importHeader(HeaderData, encode); // throw UnsupportedEncodingException
        } else if (HeaderData == null) {
            throw new NullPointerException("HeaderData");
        } else {
            throw new NullPointerException("encode");
        }
    }

    /**
     * このオブジェクトのコピーを作成して返す。
     *
     * @return このオブジェクトのコピー
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException exception) { // Ignore
            throw new Error("java.lang.Object is not support clone().");
        }
    }

    /**
     * データを圧縮した方法を識別する文字列を得る。
     *
     * @return 圧縮法文字列
     */
    public String getCompressMethod() {
        return this.Method;
    }

    /**
     * データの圧縮前のサイズを得る。
     *
     * @return 圧縮前のサイズ LhaHeader( String path ) または
     * LhaHeader( String path, Date date )で生成された インスタンスは初期状態ではサイズが不明のため
     * LhaHeader.UNKNOWN( -1 ) を返す。
     * @see LhaHeader#UNKNOWN
     */
    public long getOriginalSize() {
        return this.OriginalSize;
    }

    /**
     * データの圧縮後のサイズを得る。
     *
     * @return 圧縮後のサイズ LhaHeader( String path ) または
     * LhaHeader( String path, Date date )で生成された インスタンスは初期状態ではサイズが不明のため
     * LhaHeader.UNKNOWN( -1 ) を返す。
     * @see LhaHeader#UNKNOWN
     */
    public long getCompressedSize() {
        return this.CompressedSize;
    }

    /**
     * データの最終更新日時を得る。
     *
     * @return データの最終更新日時
     */
    public Date getLastModified() {
        return new Date(this.LastModified.getTime());
    }

    /**
     * このヘッダのヘッダレベルを得る。
     *
     * @return ヘッダレベル
     */
    public int getHeaderLevel() {
        return this.HeaderLevel;
    }

    /**
     * データの名前、
     * もしくはデータがファイルであった場合のパス名を得る。<br>
     * パス名とはいっても、Windows 系の A: のような
     * ドライブ名を含んではならない。<br>
     * パスデリミタには File.separator を使用する。
     *
     * @return データの名前、もしくは パス名。
     * @see File#separator
     */
    public String getPath() {
        return this.Path;
    }

    /**
     * データのCRC16値を得る。
     *
     * @return データのCRC16値<br>
     * LhaHeader( String path ) または
     * LhaHeader( String path, Date date )で生成された
     * インスタンスは初期状態ではCRCが不明のため
     * LhaHeader.UNKNOWN( -1 ) を返す。<br>
     * レベル0ヘッダでCRC16値の
     * フィールドが無い場合は
     * LhaHeader.NO_CRC( -2 )を返す
     * @see LhaHeader#UNKNOWN
     * @see LhaHeader#NO_CRC
     */
    public int getCRC() {
        return this.CRC;
    }

    /**
     * このヘッダを作成した OS の識別子を得る。
     *
     * @return OSの識別子
     */
    public byte getOSID() {
        return this.OSID;
    }

    /**
     * レベル 0 ヘッダ、 レベル 1 ヘッダの時に
     * 付加される可能性がある基本ヘッダ内の拡張データを得る。
     *
     * @return 拡張データ
     */
    protected byte[] getExtraData() {
        return this.ExtraData.clone();
    }

    /**
     * レベル 0 ヘッダに記される
     * DOS のファイル属性を得る。
     *
     * @return DOS の ファイル属性
     */
    protected byte getLevel0DosAttribute() {
        return this.Level0DosAttribute;
    }

    /**
     * パス名から切り分けられたファイル名を得る。
     *
     * @return ファイル名
     */
    private String getFileName() {
        return this.Path.substring(this.Path.lastIndexOf(File.separatorChar) + 1);
    }

    /**
     * パス名から切り分けられたディレクトリ名を得る。
     *
     * @return ディレクトリ名
     */
    private String getDirName() {
        return this.Path.substring(0, this.Path.lastIndexOf(File.separatorChar) + 1);
    }

    /**
     * このLhaHeaderのデータを使用して ヘッダデータを生成し、
     * それをバイト配列の形で得る。<br>
     * エンコードはデフォルトのものが使用される。
     *
     * @return バイト配列に格納したヘッダデータ
     * @throws IllegalStateException <ol>
     *                               <li>圧縮法文字列をencodeでバイト配列にしたものが 5byteで無い場合
     *                               <li>レベル0,1,2で ファイル名が長すぎるためヘッダに収まりきらない。
     *                               <li>レベル1,2で共通拡張ヘッダが大きすぎて出力できない。そのためヘッダのCRC格納場所が無い。
     *                               <li>レベル0以外で CRC に レベル0ヘッダでCRC情報が無い事を示す特別な値である
     *                               LhaHeader.NO_CRC( -2 ) が設定されていた。
     *                               <li>レベル0,1の時にLastModifiedがMS-DOS形式で表現できない範囲の時間であった場合
     *                               <li>レベル2,3の時にLastModifiedが4バイトのtime_tで表現できない範囲の時間であった場合
     *                               <li>OriginalSize にサイズが不明である事を示す特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                               <li>OriginalSize が負値である場合
     *                               <li>レベル0,1,3 の時に OriginalSize が4byte値で表現できない値である場合
     *                               <li>CompressedSize にサイズが不明である事を示す特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                               <li>CompressedSize が負値である場合
     *                               <li>レベル0,1,3 の時に CompressedSize が4byte値で表現できない値である場合
     *                               <li>レベル2の時にOriginalSize または CompressedSizeが4バイト値を超えるためファイルサイズヘッダが必要な際に
     *                               他の拡張ヘッダが大きすぎてファイルサイズヘッダが出力出来ない場合。
     *                               <li>CRC にCRC16値が不明である事を示す特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                               <li>ヘッダレベルが 0,1,2,3 以外である場合
     *                               </ol>
     *                               の何れか。
     */
    public byte[] getBytes() {
        try {
            return this.exportHeader(LhaProperty.encoding);
        } catch (UnsupportedEncodingException exception) {
            throw new Error("Java Runtime Environment not support " + LhaProperty.encoding + " encoding");
        }
    }

    /**
     * このLhaHeaderのデータを使用して ヘッダデータを生成し、
     * それをバイト配列の形で得る。
     *
     * @param encode 文字列情報を出力する際に使用するエンコード
     * @return バイト配列に格納したヘッダデータ
     * @throws IllegalStateException        <ol>
     *                                      <li>圧縮法文字列をencodeでバイト配列にしたものが 5byteで無い場合
     *                                      <li>レベル0,1,2で ファイル名が長すぎるためヘッダに収まりきらない。
     *                                      <li>レベル1,2で共通拡張ヘッダが大きすぎて出力できない。
     *                                      そのためヘッダのCRC格納場所が無い。
     *                                      <li>レベル0以外で CRC に レベル0ヘッダでCRC情報が無い事を示す特別な値である
     *                                      LhaHeader.NO_CRC( -2 ) が設定されていた。
     *                                      <li>レベル0,1の時にLastModifiedがMS-DOS形式
     *                                      で表現できない範囲の時間であった場合
     *                                      <li>レベル2,3の時にLastModifiedが4バイトの
     *                                      time_tで表現できない範囲の時間であった場合
     *                                      <li>OriginalSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>OriginalSize が負値である場合
     *                                      <li>レベル0,1,3 の時に OriginalSize が
     *                                      4byte値で表現できない値である場合
     *                                      <li>CompressedSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>CompressedSize が負値である場合
     *                                      <li>レベル0,1,3 の時に CompressedSize が
     *                                      4byte値で表現できない値である場合
     *                                      <li>レベル2の時にOriginalSize または CompressedSizeが
     *                                      4バイト値を超えるためファイルサイズヘッダが必要な際に
     *                                      他の拡張ヘッダが大きすぎてファイルサイズヘッダが出力出来ない場合。
     *                                      <li>CRC にCRC16値が不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>ヘッダレベルが 0,1,2,3 以外である場合
     *                                      </ol>
     *                                      の何れか。
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     */
    public byte[] getBytes(String encode) throws UnsupportedEncodingException {
        return this.exportHeader(encode);
    }

    /**
     * 圧縮法文字列を設定する。
     *
     * @param method 圧縮法文字列
     * @throws IllegalArgumentException 圧縮法文字列が '-' で始まっていないか、'-' で終わっていない場合。
     */
    public void setCompressMethod(String method) {
        if (method == null) {
            throw new NullPointerException("method");
        } else if (!method.startsWith("-") || !method.endsWith("-")) {
            throw new IllegalArgumentException("method must starts with '-' and ends with '-'");
        } else {
            this.Method = method;
        }
    }

    /**
     * 圧縮前データサイズを設定する。<br>
     * LhaHeader.UNKNOWN( -1 ) は サイズ不明を示す
     * 特別な数字であるため設定できない。<br>
     * また レベル0,1,3 では処理できるのは 4バイト値のみであるため
     * 4バイトで表現できない値を設定した場合 getByte() 時に例外を投げる。
     *
     * @param size 圧縮前データサイズ
     * @throws IllegalArgumentException size に LhaHeader.UNKNOWN( -1 )を設定しようとした場合
     * @see LhaHeader#UNKNOWN
     */
    public void setOriginalSize(long size) {
        if (size != LhaHeader.UNKNOWN) {
            this.OriginalSize = size;
        } else {
            throw new IllegalArgumentException("size must not LhaHeader.UNKNOWN( " + LhaHeader.UNKNOWN + " )");
        }
    }

    /**
     * 圧縮後データサイズを設定する。<br>
     * LhaHeader.UNKNOWN( -1 ) は サイズ不明を示す
     * 特別な数字であるため設定できない。<br>
     * また レベル0,1,3 では処理できるのは 4バイト値のみであるため
     * 4バイトで表現できない値を設定した場合 getByte() 時に例外を投げる。
     *
     * @param size 圧縮後データサイズ
     * @throws IllegalArgumentException size に LhaHeader.UNKNOWN を設定しようとした
     * @see LhaHeader#UNKNOWN
     */
    public void setCompressedSize(long size) {
        if (size != LhaHeader.UNKNOWN) {
            this.CompressedSize = size;
        } else {
            throw new IllegalArgumentException("size must not LhaHeader.UNKNOWN( " + LhaHeader.UNKNOWN + " )");
        }
    }

    /**
     * 圧縮データの最終更新日時を設定する。<br>
     * ヘッダレベルが 0,1 の場合は MsdosDateで表せる範囲内、
     * ヘッダレベルが 2,3 の場合は 4byte の time_tで表せる範囲内
     * の日付で無ければならない。<br>
     * 範囲内でなくても このメソッドは例外を投げないことに注意す
     * ること。範囲内に無い場合は このメソッドは例外を投げないが、
     * getBytes() 時に例外を投げる。
     *
     * @param date 最終更新日時
     * @throws IllegalArgumentException date に null を設定しようとした場合
     */
    public void setLastModified(Date date) {
        if (date != null) {
            this.LastModified = date;
        } else {
            throw new NullPointerException("date");
        }
    }

    /**
     * ヘッダレベルを設定する。<br>
     * 現在設定できるのは 0,1,2,3 のみとなっている。<br>
     * ヘッダレベルの変更はパスの最大長や、LastModified の制限範囲
     * などを変化させるため注意が必要である。
     *
     * @param level ヘッダレベル
     */
    public void setHeaderLevel(int level) {
        this.HeaderLevel = level;
    }

    /**
     * データの名前、もしくはデータがファイルである場合、
     * データのパスを設定する。<br>
     * パスデリミタには File.separator を使用する。<br>
     * ヘッダレベルによって path にはバイト数の制限が存在するが、
     * このメソッドは制限を越えた場合でも 例外を投げないことに
     * 注意。制限を越えた場合は このメソッドは例外を投げないが、
     * getBytes()時に例外を投げる
     *
     * @param path データの名前、もしくはファイル名
     * @throws IllegalArgumentException path が空文字列である場合
     * @see File#separator
     */
    public void setPath(String path) {
        if (path == null) {
            throw new NullPointerException("path");
        } else if (path.isEmpty()) {
            throw new IllegalArgumentException("path must not empty.");
        } else {
            this.Path = path;
        }
    }

    /**
     * 圧縮前のデータの CRC16値を設定する。<br>
     * LhaHeader.UNKNOWN( -1 ) は サイズ不明を示す
     * 特別な数字であるため設定できない。<br>
     * LhaHeader.NO_CRC( -2 ) は レベル0ヘッダの場
     * 合に CRC値を出力しないことを意味する特別な値
     * である。<br>
     * 他のヘッダレベルの時に LhaHeader.NO_CRC( -2 )
     * を設定しても例外を投げないが getBytes() 時に
     * 例外を投げるので注意すること。<br>
     * 有効なのは下位2バイトで、上位2バイトは無視される。
     *
     * @param crc データの圧縮前のCRC16値
     * @throws IllegalArgumentException crc に LhaHeader.UNKNOWN を設定しようとした
     * @see LhaHeader#UNKNOWN
     * @see LhaHeader#NO_CRC
     */
    public void setCRC(int crc) {
        if (crc != LhaHeader.UNKNOWN) {
            this.CRC = crc;
        } else {
            throw new IllegalArgumentException("crc must not LhaHeader.UNKNOWN( " + LhaHeader.UNKNOWN + " )");
        }
    }

    /**
     * このヘッダにOS固有の情報が含まれる場合、
     * そのデータを解釈する手がかりとして OSの識別子を設定する。
     *
     * @param id OS識別子
     */
    public void setOSID(byte id) {
        this.OSID = id;
    }

    /**
     * レベル 0,1ヘッダ時に使用される 基本ヘッダ内
     * 拡張情報を設定する。<br>
     * 拡張情報のバイト数には制限が存在するが、このメソッドは
     * 制限を越えても例外を投げないことに注意。制限を越えた場合
     * getBytes()時に例外を投げる。
     *
     * @param data 拡張情報
     *             拡張情報を出力しない場合は nullを設定する。
     */
    protected void setExtraData(byte[] data) {
        this.ExtraData = data;
    }

    /**
     * レベル 0ヘッダの場合に出力される、
     * MS-DOS のファイル属性を設定する。
     *
     * @param attribute MS-DOSのファイル属性
     */
    protected void setLevel0DosAttribute(byte attribute) {
        this.Level0DosAttribute = attribute;
    }

    /**
     * filename で指定されるファイル名をパス名に設定する。
     *
     * @param filename ファイル名
     */
    private void setFileName(String filename) {
        this.Path = this.getDirName() + filename;
    }

    /**
     * dirname で指定される ディレクトリ名をパス名に設定する。
     *
     * @param dirname ディレクトリ名
     */
    private void setDirName(String dirname) {
        this.Path = dirname + this.getFileName();
    }

    /**
     * HeaderDataをレベル0ヘッダのデータとして解釈し、
     * このLhaHeaderに値を設定する。
     *
     * @param HeaderData ヘッダデータ
     * @param encode     文字列情報を解釈する際に使用する
     *                   エンコード
     * @throws UnsupportedEncodingException encode で指定されたエンコードが
     *                                      サポートされない場合
     */
    private void importLevel0Header(byte[] HeaderData, String encode) throws UnsupportedEncodingException {

        // ヘッダデータ位置の定義
        final int HeaderSizeIndex = 0;
        int HeaderSize = (HeaderData[HeaderSizeIndex] & 0xFF) + 2;
        final int CompressMethodIndex = 2;
        final int CompressedSizeIndex = 7;
        final int OriginalSizeIndex = 11;
        final int LastModifiedIndex = 15;
        final int DosAttributeIndex = 19;
        final int HeaderLevelIndex = 20;
        final int PathLengthIndex = 21;
        int PathLength = HeaderData[PathLengthIndex] & 0xFF;
        final int PathIndex = 22;
        int CRCIndex = 22 + PathLength;
        int ExtraDataIndex = 24 + PathLength;
        int ExtraDataLength = HeaderSize - ExtraDataIndex;

        // ヘッダデータ読み込み
        this.Method = new String(HeaderData, CompressMethodIndex, 5, encode);
        this.CompressedSize = (LittleEndian.readInt(HeaderData, CompressedSizeIndex)) & 0xFFFFFFFFL;
        this.OriginalSize = (LittleEndian.readInt(HeaderData, OriginalSizeIndex)) & 0xFFFFFFFFL;
        this.LastModified = new MsdosDate(LittleEndian.readInt(HeaderData, LastModifiedIndex));
        this.Level0DosAttribute = HeaderData[DosAttributeIndex];
        this.HeaderLevel = HeaderData[HeaderLevelIndex] & 0xFF;
        this.Path = new String(HeaderData, PathIndex, PathLength, encode);
        this.Path = this.Path.replace('\\', File.separatorChar);

        if (CRCIndex + 2 <= HeaderSize) {
            this.CRC = LittleEndian.readShort(HeaderData, CRCIndex);
            if (0 < ExtraDataLength) {
                this.ExtraData = new byte[ExtraDataLength];
                System.arraycopy(HeaderData, ExtraDataIndex, this.ExtraData, 0, ExtraDataLength);
            }
        } else {
            this.CRC = LhaHeader.NO_CRC;
        }
    }

    /**
     * HeaderDataをレベル1ヘッダのデータとして解釈し、
     * このLhaHeaderに値を設定する。
     *
     * @param HeaderData ヘッダデータ
     * @param encode     文字列情報を解釈する際に使用するエンコード
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     */
    private void importLevel1Header(byte[] HeaderData, String encode) throws UnsupportedEncodingException {

        // 基本ヘッダ内データ位置の定義
        final int BaseHeaderSizeIndex = 0;
        int BaseHeaderSize = (HeaderData[BaseHeaderSizeIndex] & 0xFF) + 2;
        final int CompressMethodIndex = 2;
        final int SkipSizeIndex = 7;
        final int OriginalSizeIndex = 11;
        final int LastModifiedIndex = 15;
        final int HeaderLevelIndex = 20;
        final int FileNameLengthIndex = 21;
        int FileNameLength = HeaderData[FileNameLengthIndex] & 0xFF;
        final int FileNameIndex = 22;
        int CRCIndex = 22 + FileNameLength;
        int OSIDIndex = 24 + FileNameLength;
        int ExtraDataIndex = 25 + FileNameLength;
        int ExtraDataLength = BaseHeaderSize - ExtraDataIndex - 2;

        // 基本ヘッダデータ読み込み
        this.Method = new String(HeaderData, CompressMethodIndex, 5, encode);
        this.CompressedSize = (LittleEndian.readInt(HeaderData, SkipSizeIndex)) & 0xFFFFFFFFL;
        this.OriginalSize = (LittleEndian.readInt(HeaderData, OriginalSizeIndex)) & 0xFFFFFFFFL;
        this.LastModified = new MsdosDate(LittleEndian.readInt(HeaderData, LastModifiedIndex));
        this.HeaderLevel = HeaderData[HeaderLevelIndex] & 0xFF;
        this.Path = new String(HeaderData, FileNameIndex, FileNameLength, encode);
        this.CRC = LittleEndian.readShort(HeaderData, CRCIndex);
        this.OSID = HeaderData[OSIDIndex];
        if (0 < ExtraDataLength) {
            this.ExtraData = new byte[ExtraDataLength];
            System.arraycopy(HeaderData, ExtraDataIndex, this.ExtraData, 0, ExtraDataLength);
        }

        // 拡張ヘッダデータの読み込み
        boolean hasFileSize = false;
        int index = BaseHeaderSize;
        int length = LittleEndian.readShort(HeaderData, index - 2);
        while (length != 0) {
            if (!hasFileSize) {
                this.CompressedSize -= length;
            }

            this.importExtHeader(HeaderData, index, length - 2, encode);
            if (HeaderData[index] == (byte) 0x42) {
                hasFileSize = true;
            }

            index += length;
            length = LittleEndian.readShort(HeaderData, index - 2);
        }
    }

    /**
     * HeaderDataをレベル2ヘッダのデータとして解釈し、
     * このLhaHeaderに値を設定する。
     *
     * @param HeaderData ヘッダデータ
     * @param encode     文字列情報を解釈する際に使用するエンコード
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     */
    private void importLevel2Header(byte[] HeaderData, String encode) throws UnsupportedEncodingException {

        // 基本ヘッダ内データ位置の定義
        final int HeaderSizeIndex = 0;
        @SuppressWarnings("unused") int HeaderSize = LittleEndian.readShort(HeaderData, HeaderSizeIndex);
        final int CompressMethodIndex = 2;
        final int CompressedSizeIndex = 7;
        final int OriginalSizeIndex = 11;
        final int LastModifiedIndex = 15;
        final int HeaderLevelIndex = 20;
        final int CRCIndex = 21;
        final int OSIDIndex = 23;

        // 基本ヘッダデータ読み込み
        this.Method = new String(HeaderData, CompressMethodIndex, 5, encode);
        this.CompressedSize = (LittleEndian.readInt(HeaderData, CompressedSizeIndex)) & 0xFFFFFFFFL;
        this.OriginalSize = (LittleEndian.readInt(HeaderData, OriginalSizeIndex)) & 0xFFFFFFFFL;
        this.LastModified = new Date(LittleEndian.readInt(HeaderData, LastModifiedIndex) * 1000L);
        this.HeaderLevel = HeaderData[HeaderLevelIndex] & 0xFF;
        this.CRC = LittleEndian.readShort(HeaderData, CRCIndex);
        this.OSID = HeaderData[OSIDIndex];

        // 拡張ヘッダデータの読み込み
        final int BaseHeaderSize = 26;
        int index = BaseHeaderSize;
        int length = LittleEndian.readShort(HeaderData, index - 2);
        while (length != 0) {
            this.importExtHeader(HeaderData, index, length - 2, encode);
            index += length;
            length = LittleEndian.readShort(HeaderData, index - 2);
        }
    }

    /**
     * HeaderDataをレベル3ヘッダのデータとして解釈し、
     * このLhaHeaderに値を設定する。
     *
     * @param HeaderData ヘッダデータ
     * @param encode     文字列情報を解釈する際に使用するエンコード
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     */
    private void importLevel3Header(byte[] HeaderData, String encode) throws UnsupportedEncodingException {

        // 基本ヘッダ内データ位置の定義
        final int WordSizeIndex = 0;
        @SuppressWarnings("unused") int WordSize = LittleEndian.readShort(HeaderData, WordSizeIndex);
        final int CompressMethodIndex = 2;
        final int CompressedSizeIndex = 7;
        final int OriginalSizeIndex = 11;
        final int LastModifiedIndex = 15;
        final int HeaderLevelIndex = 20;
        final int CRCIndex = 21;
        final int OSIDIndex = 23;

        // 基本ヘッダデータ読み込み
        this.Method = new String(HeaderData, CompressMethodIndex, 5, encode);
        this.CompressedSize = (LittleEndian.readInt(HeaderData, CompressedSizeIndex)) & 0xFFFFFFFFL;
        this.OriginalSize = (LittleEndian.readInt(HeaderData, OriginalSizeIndex)) & 0xFFFFFFFFL;
        this.LastModified = new Date(LittleEndian.readInt(HeaderData, LastModifiedIndex) * 1000L);
        this.HeaderLevel = HeaderData[HeaderLevelIndex] & 0xFF;
        this.CRC = LittleEndian.readShort(HeaderData, CRCIndex);
        this.OSID = HeaderData[OSIDIndex];

        // 拡張ヘッダデータの読み込み
        final int BaseHeaderSize = 32;
        int index = BaseHeaderSize;
        int length = LittleEndian.readInt(HeaderData, index - 4);
        while (length != 0) {
            this.importExtHeader(HeaderData, index, length - 4, encode);
            index += length;
            length = LittleEndian.readInt(HeaderData, index - 4);
        }
    }

    /**
     * HeaderData を LHAヘッダデータとして解釈し
     * LhaHeader に値を設定する。
     *
     * @param HeaderData ヘッダデータ
     * @param encode     文字列情報を解釈する際に使用するエンコード
     * @throws IndexOutOfBoundsException    ヘッダデータが壊れているためデータがあると仮定した位置がHeaderData の範囲外になった
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     * @throws IllegalArgumentException     ヘッダレベルが 0,1,2,3 の何れでもない。
     */
    private void importHeader(byte[] HeaderData, String encode) throws UnsupportedEncodingException {
        final int HeaderLevelIndex = 20;

        switch (HeaderData[HeaderLevelIndex]) {
            case 0:
                this.importLevel0Header(HeaderData, encode);
                break;
            case 1:
                this.importLevel1Header(HeaderData, encode);
                break;
            case 2:
                this.importLevel2Header(HeaderData, encode);
                break;
            case 3:
                this.importLevel3Header(HeaderData, encode);
                break;

            default:
                throw new IllegalArgumentException("unknown header level \"" + HeaderData[HeaderLevelIndex] + "\".");
        }
    }

    /**
     * HeaderData から 共通拡張ヘッダを読み込む。
     * このメソッドは共通拡張ヘッダに ヘッダ検査用のCRC16値以外
     * のデータが存在した場合 共通拡張ヘッダを ExtraExtHeaders に
     * 登録するだけである。
     *
     * @param HeaderData ヘッダデータ
     * @param index      HeaderData内の拡張ヘッダの開始位置
     * @param length     拡張ヘッダの長さ
     */
    private void importCommonExtHeader(byte[] HeaderData, int index, int length) {
        // ( 3 < length )の比較は 拡張ヘッダID(1byte)と
        // ヘッダのCRC16値(2byte)以外にデータを含むかの判定。
        // CRC16値以外の情報を持つなら その情報を保存するため
        // ExtraExtHeadersに登録する。
        if (3 < length) {
            if (this.ExtraExtHeaders == null) {
                this.ExtraExtHeaders = new Vector<>();
            }
            byte[] ExtHeaderData = new byte[length];
            System.arraycopy(HeaderData, index, ExtHeaderData, 0, length);
            this.ExtraExtHeaders.addElement(ExtHeaderData);
        }
    }

    /**
     * HeaderData から ファイル名拡張ヘッダを読み込む。
     *
     * @param HeaderData ヘッダデータ
     * @param index      HeaderData内の拡張ヘッダの開始位置
     * @param length     拡張ヘッダの長さ
     * @param encode     文字列情報を解釈する際に使用する
     *                   エンコード
     * @throws UnsupportedEncodingException この例外が投げられることは無い。
     */
    private void importFileNameExtHeader(byte[] HeaderData,
                                         int index,
                                         int length,
                                         String encode) throws UnsupportedEncodingException {

        this.setFileName(new String(HeaderData, index + 1, length - 1, encode));
    }

    /**
     * HeaderData から ディレクトリ名拡張ヘッダを読み込む。
     *
     * @param HeaderData ヘッダデータ
     * @param index      HeaderData内の拡張ヘッダの開始位置
     * @param length     拡張ヘッダの長さ
     * @param encode     文字列情報を解釈する際に使用する
     *                   エンコード
     * @throws UnsupportedEncodingException この例外が投げられることは無い。
     */
    private void importDirNameExtHeader(byte[] HeaderData,
                                        int index,
                                        int length,
                                        String encode) throws UnsupportedEncodingException {

        final byte LhaFileSeparator = (byte) 0xFF;

        int off = 1;
        StringBuilder dir = new StringBuilder();
        while (off < length) {
            int len = 0;
            while (off + len < length) {
                if (HeaderData[index + off + len] != LhaFileSeparator) {
                    len++;
                } else {
                    break;
                }
            }

            if (off + len < length) {
                dir.append(new String(HeaderData, index + off, len, encode)).append(File.separator);
            } else {
                dir.append(new String(HeaderData, index + off, len, encode));
            }
            off += len + 1;
        }

        this.setDirName(dir.toString());
    }

    /**
     * HeaderData から ファイルサイズ拡張ヘッダを読み込む。
     *
     * @param HeaderData ヘッダデータ
     * @param index      HeaderData内の拡張ヘッダの開始位置
     * @param length     拡張ヘッダの長さ
     */
    private void importFileSizeHeader(byte[] HeaderData, int index, int length) {
        if (length == 17) {
            this.CompressedSize = LittleEndian.readLong(HeaderData, index + 1);
            this.OriginalSize = LittleEndian.readLong(HeaderData, index + 9);
        } else {
        }
    }

    /**
     * 拡張ヘッダを読み込む。
     * このメソッドをオーバーライドする事によって
     * 様々な拡張ヘッダに対応することが可能となる。
     * LhaHeader では 拡張ヘッダを private メンバである
     * ExtraExtHeaders に登録するだけである。
     *
     * @param HeaderData ヘッダデータ
     * @param index      HeaderData内の拡張ヘッダの開始位置
     * @param length     拡張ヘッダの長さ
     * @param encode     文字列情報を解釈する際に使用する
     *                   エンコード
     * @throws UnsupportedEncodingException encode で指定されたエンコードが
     *                                      サポートされない場合
     */
    protected void importExtendHeader(byte[] HeaderData,
                                      int index,
                                      int length,
                                      String encode) throws UnsupportedEncodingException {
        if (this.ExtraExtHeaders == null) {
            this.ExtraExtHeaders = new Vector<>();
        }
        byte[] ExtHeaderData = new byte[length];
        System.arraycopy(HeaderData, index, ExtHeaderData, 0, length);
        this.ExtraExtHeaders.addElement(ExtHeaderData);
    }

    /**
     * HeaderData の index からはじまる length バイトの
     * 拡張ヘッダを読み込む。
     *
     * @param HeaderData ヘッダデータ
     * @param index      HeaderData内の拡張ヘッダの開始位置
     * @param length     拡張ヘッダの長さ
     * @param encode     文字列情報を解釈する際に使用する
     *                   エンコード
     * @throws UnsupportedEncodingException encode で指定されたエンコードが
     *                                      サポートされない場合
     */
    private void importExtHeader(byte[] HeaderData, int index, int length, String encode) throws UnsupportedEncodingException {
        final int ExtendHeaderIDIndex = 0;

        switch (HeaderData[index + ExtendHeaderIDIndex]) {
            case 0x00:
                this.importCommonExtHeader(HeaderData, index, length);
                break;
            case 0x01:
                this.importFileNameExtHeader(HeaderData, index, length, encode);
                break;
            case 0x02:
                this.importDirNameExtHeader(HeaderData, index, length, encode);
                break;
            case 0x42:
                this.importFileSizeHeader(HeaderData, index, length);
                break;
            default:
                this.importExtendHeader(HeaderData, index, length, encode);
        }
    }

    /**
     * この LhaHeader の情報を使って
     * レベル0ヘッダのデータを生成する。<br>
     * その際、ExtraData を含めるとヘッダサイズが
     * 規定値に収まらない場合は ExtraData は含まれないことがある。
     *
     * @param encode 文字列情報を出力する際に使用する
     *               エンコード
     * @throws IllegalStateException        <ol>
     *                                      <li>圧縮法文字列をencodeでバイト配列に
     *                                      したものが 5byteで無い場合
     *                                      <li>Path が大きすぎるため レベル0ヘッダの
     *                                      最大サイズに収まりきらない。
     *                                      <li>LastModifiedがMS-DOS形式で
     *                                      表現できない範囲の時間であった場合
     *                                      <li>OriginalSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定
     *                                      されていた。
     *                                      <li>OriginalSize が負値であるか、
     *                                      4byte値で表現できない値である場合
     *                                      <li>CompressedSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が
     *                                      設定されていた。
     *                                      <li>CompressedSize が負値であるか、
     *                                      4byte値で表現できない値である場合
     *                                      </ol>
     *                                      の何れか
     * @throws UnsupportedEncodingException encode で指定されたエンコードが
     *                                      サポートされない場合
     */
    private byte[] exportLevel0Header(String encode) throws UnsupportedEncodingException {

        // ヘッダ出力準備
        final int LHarcHeaderSize = 100;
        int CRCLength = (this.CRC == LhaHeader.NO_CRC || this.CRC == LhaHeader.UNKNOWN ? 0 : 2);
        byte[] CompressMethod = this.Method.getBytes(encode);
        MsdosDate dosDate;
        try {
            dosDate = this.LastModified instanceof MsdosDate ? (MsdosDate) this.LastModified : new MsdosDate(this.LastModified); // throw IllegalArgumentException
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(exception.toString());
        }
        byte[] PathData = this.Path.replace(File.separatorChar, '\\').getBytes(encode);
        int HeaderLength = 22 + CRCLength + PathData.length;
        byte[] ExtraData;
        if (CRCLength != 0 && this.ExtraData != null && (HeaderLength + this.ExtraData.length <= LHarcHeaderSize)) {
            ExtraData = this.ExtraData;
        } else {
            ExtraData = new byte[0];
        }

        HeaderLength += ExtraData.length;

        // ヘッダ正当性チェック
        if (CompressMethod.length != 5) {
            throw new IllegalStateException("CompressMethod doesn't follow Format.");
        }

        if (LHarcHeaderSize < HeaderLength) {
            throw new IllegalStateException("Header size too large.");
        }

        if (this.CompressedSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CompressedSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.CompressedSize) {
            throw new IllegalStateException("CompressedSize must be 0xFFFFFFFF or less.");
        }

        if (this.CompressedSize < 0) {
            throw new IllegalStateException("CompressedSize must be 0 or more.");
        }

        if (this.OriginalSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("OriginalSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.OriginalSize) {
            throw new IllegalStateException("OriginalSize must be 0xFFFFFFFF or less.");
        }

        if (this.OriginalSize < 0) {
            throw new IllegalStateException("OriginalSize must be 0 or more.");
        }

        // ヘッダ出力
        byte[] HeaderData;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // 出力するヘッダ長にはヘッダ先頭の ヘッダ長(1byte)、
            // チェックサム(1byte)の2byteを含まないため -2 している。
            out.write(HeaderLength - 2);
            out.write(0);
            out.write(CompressMethod);
            LittleEndian.writeInt(out, (int) this.CompressedSize);
            LittleEndian.writeInt(out, (int) this.OriginalSize);
            LittleEndian.writeInt(out, dosDate.getMsdosTime());
            out.write(this.Level0DosAttribute);
            out.write(this.HeaderLevel);
            out.write(PathData.length);
            out.write(PathData);
            if (this.CRC != -1) {
                LittleEndian.writeShort(out, this.CRC);
                out.write(ExtraData);
            }
            out.close();
            HeaderData = out.toByteArray();
        } catch (IOException exception) {
            throw new Error("caught the IOException ( " + exception.getMessage()
                    + " ) which should be never thrown by ByteArrayOutputStream.");
        }

        final int ChecksumIndex = 1;
        HeaderData[ChecksumIndex] = (byte) LhaHeader.calcHeaderChecksum(HeaderData);

        return HeaderData;
    }

    /**
     * この LhaHeader の情報を使って
     * レベル1ヘッダのデータを生成する。<br>
     * その際、ExtraData を含めるとヘッダサイズが
     * 規定値に収まらない場合は ExtraData は含まれないことがある。
     * また、拡張ヘッダで 65534バイト以上のサイズを
     * 持つものは無視される。
     *
     * @param encode 文字列情報を出力する際に使用するエンコード
     * @throws IllegalStateException        <ol>
     *                                      <li>圧縮法文字列をencodeでバイト配列に
     *                                      したものが 5byteで無い場合
     *                                      <li>ファイル名が大きすぎるため
     *                                      基本ヘッダにも拡張ヘッダにも収まりきらない。
     *                                      <li>共通拡張ヘッダが大きすぎて出力できない。
     *                                      そのためヘッダのCRC格納場所が無い。
     *                                      <li>CRC に レベル0ヘッダで CRC情報が無い事を示す
     *                                      特別な値である LhaHeader.NO_CRC( -2 ) が設定されていた。
     *                                      <li>LastModifiedがMS-DOS形式で
     *                                      表現できない範囲の時間であった場合
     *                                      <li>OriginalSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>OriginalSize が負値であるか、
     *                                      4byte値で表現できない値である場合
     *                                      <li>CompressedSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>CompressedSize が負値であるか、
     *                                      4byte値で表現できない値である場合
     *                                      <li>CRC にCRC16値が不明である事を示す 特別な値
     *                                      である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      </ol>
     *                                      の何れか
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     */
    private byte[] exportLevel1Header(String encode) throws UnsupportedEncodingException {

        // ヘッダ出力準備
        final int LHarcHeaderSize = 100;
        boolean hasFileName = false; // ファイル名情報を持つかを示す
        boolean hasCRC = false; // ヘッダのCRC情報を持つかを示す
        byte[] CompressMethod = this.Method.getBytes(encode);
        MsdosDate dosDate;
        try {
            if (this.LastModified instanceof MsdosDate) {
                dosDate = (MsdosDate) this.LastModified;
            } else {
                dosDate = new MsdosDate(this.LastModified);
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(exception.toString());
        }

        int HeaderLength = 27;
        byte[] ExtraData;
        if (this.ExtraData != null && (HeaderLength + this.ExtraData.length <= LHarcHeaderSize)) {
            ExtraData = this.ExtraData;
        } else {
            ExtraData = new byte[0];
        }
        HeaderLength += ExtraData.length;

        byte[] FileName = this.getFileName().getBytes(encode);
        if (LHarcHeaderSize < HeaderLength + FileName.length) {
            FileName = new byte[0];
        } else {
            hasFileName = true;
        }
        HeaderLength += FileName.length;

        byte[][] ExtendHeaders = this.exportExtHeaders(encode);
        long SkipSize = this.CompressedSize;
        for (int i = 0; i < ExtendHeaders.length; i++) {
            if (ExtendHeaders[i].length == 0 || 65534 <= ExtendHeaders[i].length || (ExtendHeaders[i][0] == 1 && hasFileName)) {
                ExtendHeaders[i] = null;
            } else {
                if (ExtendHeaders[i][0] == 0x00) {
                    hasCRC = true;
                }
                if (ExtendHeaders[i][0] == 0x01) {
                    hasFileName = true;
                }

                SkipSize += ExtendHeaders[i].length + 2;
            }
        }

        // ヘッダ正当性チェック
        if (CompressMethod.length != 5) {
            throw new IllegalStateException("CompressMethod doesn't follow Format.");
        }

        if (SkipSize != this.CompressedSize && !hasCRC) {
            throw new IllegalStateException("no Header CRC field.");
        }

        if (!hasFileName) {
            throw new IllegalStateException("no Filename infomation.");
        }

        if (this.CRC == LhaHeader.NO_CRC) {
            throw new IllegalStateException("no CRC value.");
        }

        if (this.CRC == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CRC is UNKNOWN.");
        }

        if (this.CompressedSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CompressedSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.CompressedSize) {
            throw new IllegalStateException("CompressedSize must be 0xFFFFFFFF or less.");
        }

        if (this.CompressedSize < 0) {
            throw new IllegalStateException("CompressedSize must be 0 or more.");
        }

        if (this.OriginalSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("OriginalSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.OriginalSize) {
            throw new IllegalStateException("OriginalSize must be 0xFFFFFFFF or less.");
        }

        if (this.OriginalSize < 0) {
            throw new IllegalStateException("OriginalSize must be 0 or more.");
        }

        if (0x0000000100000000L <= SkipSize) {
            throw new IllegalStateException("SkipSize must be 0xFFFFFFFF or less.");
        }

        // ヘッダ出力
        byte[] HeaderData;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // 出力するヘッダ長にはヘッダ先頭の ヘッダ長(1byte)、
            // チェックサム(1byte)の2byteを含まないため -2 している。
            out.write(HeaderLength - 2);
            out.write(0);
            out.write(CompressMethod);
            LittleEndian.writeInt(out, (int) SkipSize);
            LittleEndian.writeInt(out, (int) this.OriginalSize);
            LittleEndian.writeInt(out, dosDate.getMsdosTime());
            out.write(0x20);
            out.write(this.HeaderLevel);
            out.write(FileName.length);
            out.write(FileName);
            LittleEndian.writeShort(out, this.CRC);
            out.write(this.OSID);
            out.write(ExtraData);

            for (byte[] extendHeader : ExtendHeaders) {
                if (extendHeader != null) {
                    LittleEndian.writeShort(out, extendHeader.length + 2);
                    out.write(extendHeader);
                }
            }
            LittleEndian.writeShort(out, 0);
            out.close();
            HeaderData = out.toByteArray();
        } catch (IOException exception) {
            throw new Error("caught the IOException ( " + exception.getMessage()
                    + " ) which should be never thrown by ByteArrayOutputStream.");
        }

        final int ChecksumIndex = 1;
        int CRCIndex = LhaHeader.getCRC16Position(HeaderData);
        HeaderData[ChecksumIndex] = (byte) LhaHeader.calcHeaderChecksum(HeaderData);
        if (hasCRC) {
            LittleEndian.writeShort(HeaderData, CRCIndex, LhaHeader.calcHeaderCRC16(HeaderData));
        }

        return HeaderData;
    }

    /**
     * この LhaHeader の情報を使って
     * レベル2ヘッダのデータを生成する。<br>
     * また、全拡張ヘッダが65536バイト以上のサイズになる場合は
     * 共通拡張ヘッダ、ファイル名拡張ヘッダが最優先で格納される。
     * 上記の 2つの拡張ヘッダのみで 65536バイト以上になる場合は
     * 例外を投げる。その後は ディレクトリ名拡張ヘッダが優先され、
     * その後は exportExtendHeaders(String) が出力した順に
     * 優先して登録され、全ヘッダが 65536バイト以上に
     * ならないように格納される。
     *
     * @param encode 文字列情報を出力する際に使用するエンコード
     * @throws IllegalStateException        <ol>
     *                                      <li>圧縮法文字列をencodeでバイト配列に
     *                                      したものが 5byteで無い場合
     *                                      <li>ファイル名が大きすぎるため
     *                                      拡張ヘッダに収まりきらない。
     *                                      <li>共通拡張ヘッダが大きすぎて出力できない。
     *                                      そのためヘッダのCRC格納場所が無い。
     *                                      <li>CRC に レベル0ヘッダで CRC情報が無い事を示す
     *                                      特別な値である LhaHeader.NO_CRC( -2 ) が設定されていた。
     *                                      <li>LastModifiedが4バイトのtime_tで
     *                                      表現できない範囲の時間であった場合
     *                                      <li>OriginalSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>OriginalSize が負値である場合
     *                                      <li>CompressedSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>CompressedSize が負値である場合
     *                                      <li>CRC にCRC16値が不明である事を示す 特別な値である
     *                                      LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>OriginalSize または CompressedSizeが4バイト値を
     *                                      超えるためファイルサイズヘッダが必要な際に
     *                                      他の拡張ヘッダが大きすぎて
     *                                      ファイルサイズヘッダが出力出来ない場合。
     *                                      </ol>
     *                                      の何れか。
     * @throws UnsupportedEncodingException encode で指定されたエンコードが
     *                                      サポートされない場合
     */
    private byte[] exportLevel2Header(String encode) throws UnsupportedEncodingException {

        // ヘッダ出力準備
        final int MaxHeaderLength = 65535;
        boolean hasFileName = false; // ファイル名情報を持つかを示す
        boolean hasCRC = false; // ヘッダのCRC情報を持つかを示す
        boolean needExtraByte = false; // ヘッダの先頭を0x00にしないために余分な1バイトを付加するかを示す。
        boolean hasFileSize = false; // ファイルサイズヘッダを持つか示す。
        byte[] CompressMethod = this.Method.getBytes(encode); // After Java 1.1 throw UnsupportedEncodingException
        int HeaderLength = 26;

        boolean needFileSize = (0x0000000100000000L <= this.CompressedSize || 0x0000000100000000L <= this.OriginalSize);

        byte[][] ExtendHeaders = this.exportExtHeaders(encode);
        for (int i = 0; i < ExtendHeaders.length; i++) {
            if (ExtendHeaders[i].length == 0 || MaxHeaderLength <= HeaderLength + ExtendHeaders[i].length + 2) {
                ExtendHeaders[i] = null;
            } else {
                if (ExtendHeaders[i][0] == 0x00) {
                    hasCRC = true;
                }
                if (ExtendHeaders[i][0] == 0x01) {
                    hasFileName = true;
                }
                if (ExtendHeaders[i][0] == 0x42) {
                    hasFileSize = true;
                }

                HeaderLength += ExtendHeaders[i].length + 2;
            }
        }

        if ((HeaderLength & 0xFF) == 0) {
            HeaderLength++;
            needExtraByte = true;
        }

        // ヘッダ正当性チェック
        if (CompressMethod.length != 5) {
            throw new IllegalStateException("CompressMethod doesn't follow Format.");
        }

        if (this.LastModified.getTime() < 0 || ((this.LastModified.getTime() / 1000L) & 0xFFFFFFFF00000000L) != 0) {
            throw new IllegalStateException("LastModified can not change to 4byte time_t format.");
        }

        if (!hasCRC) {
            throw new IllegalStateException("HeaderSize too large. can not contain CRC of the Header.");
        }

        if (!hasFileName) {
            throw new IllegalStateException("HeaderSize too large. can not contain Filename.");
        }

        if (needFileSize && !hasFileSize) {
            throw new IllegalStateException("HeaderSize too large. can not contain Filesize.");
        }

        if (this.CRC == LhaHeader.NO_CRC) {
            throw new IllegalStateException("no CRC.");
        }

        if (this.CRC == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CRC must not be UNKNOWN.");
        }

        if (this.CompressedSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CompressedSize must not be UNKNOWN.");
        }

        if (this.CompressedSize < 0) {
            throw new IllegalStateException("CompressedSize must be 0 or more.");
        }

        if (this.OriginalSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("OriginalSize must not be UNKNOWN.");
        }

        if (this.OriginalSize < 0) {
            throw new IllegalStateException("OriginalSize must be 0 or more.");
        }

        // ヘッダ出力
        byte[] HeaderData;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            LittleEndian.writeShort(out, HeaderLength);
            out.write(CompressMethod);
            LittleEndian.writeInt(out, (int) this.CompressedSize);
            LittleEndian.writeInt(out, (int) this.OriginalSize);
            LittleEndian.writeInt(out, (int) (this.LastModified.getTime() / 1000L));
            out.write(0x20);
            out.write(this.HeaderLevel);
            LittleEndian.writeShort(out, this.CRC);
            out.write(this.OSID);

            for (byte[] extendHeader : ExtendHeaders) {
                if (extendHeader != null) {
                    LittleEndian.writeShort(out, extendHeader.length + 2);
                    out.write(extendHeader);
                }
            }
            LittleEndian.writeShort(out, 0);

            if (needExtraByte)
                out.write(0x00);

            out.close();
            HeaderData = out.toByteArray();
        } catch (IOException exception) {
            throw new Error("caught the IOException ( " + exception.getMessage()
                    + " ) which should be never thrown by ByteArrayOutputStream.");
        }

        int CRCIndex = LhaHeader.getCRC16Position(HeaderData);
        LittleEndian.writeShort(HeaderData, CRCIndex, LhaHeader.calcHeaderCRC16(HeaderData));

        return HeaderData;
    }

    /**
     * この LhaHeader の情報を使って
     * レベル3ヘッダのデータを生成する。
     *
     * @param encode 文字列情報を出力する際に使用するエンコード
     * @return バイト配列に格納したヘッダデータ
     * @throws IllegalStateException        <ol>
     *                                      <li>圧縮法文字列をencodeでバイト配列に
     *                                      したものが 5byteで無い場合
     *                                      <li>共通拡張ヘッダが大きすぎて出力できない。
     *                                      そのためヘッダのCRC格納場所が無い。
     *                                      <li>CRC に レベル0ヘッダで CRC情報が無い事を示す
     *                                      特別な値である LhaHeader.NO_CRC( -2 ) が設定されていた。
     *                                      <li>LastModifiedが4バイトのtime_tで
     *                                      表現できない範囲の時間であった場合<br>
     *                                      <li>OriginalSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。<br>
     *                                      <li>OriginalSize が負値であるか、
     *                                      4byte値で表現できない値である場合<br>
     *                                      <li>CompressedSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。<br>
     *                                      <li>CompressedSize が負値であるか、
     *                                      4byte値で表現できない値である場合<br>
     *                                      <li>CRC にCRC16値が不明である事を示す 特別な値である
     *                                      LhaHeader.UNKNOWN( -1 )が設定されていた。<br>
     *                                      </ol>
     *                                      の何れか。
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     */
    private byte[] exportLevel3Header(String encode) throws UnsupportedEncodingException {

        // ヘッダ出力準備
        final int WordSize = 4;
        byte[] CompressMethod = this.Method.getBytes(encode);
        int HeaderLength = 32;

        byte[][] ExtendHeaders = this.exportExtHeaders(encode);
        for (int i = 0; i < ExtendHeaders.length; i++) {
            if (ExtendHeaders[i].length == 0) {
                ExtendHeaders[i] = null;
            } else {
                HeaderLength += ExtendHeaders[i].length + 4;
            }
        }

        // ヘッダ正当性チェック
        if (CompressMethod.length != 5) {
            throw new IllegalStateException("CompressMethod doesn't follow Format.");
        }

        if (this.LastModified.getTime() < 0 || ((this.LastModified.getTime() / 1000L) & 0xFFFFFFFF00000000L) != 0) {
            throw new IllegalStateException("LastModified can not change to 4byte time_t format.");
        }

        if (this.CRC == LhaHeader.NO_CRC) {
            throw new IllegalStateException("no CRC value.");
        }

        if (this.CRC == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CRC is UNKNOWN.");
        }

        if (this.CompressedSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("CompressedSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.CompressedSize) {
            throw new IllegalStateException("CompressedSize must be 0xFFFFFFFF or less.");
        }

        if (this.CompressedSize < 0) {
            throw new IllegalStateException("CompressedSize must be 0 or more.");
        }

        if (this.OriginalSize == LhaHeader.UNKNOWN) {
            throw new IllegalStateException("OriginalSize must not be UNKNOWN.");
        }

        if (0x0000000100000000L <= this.OriginalSize) {
            throw new IllegalStateException("OriginalSize must be 0xFFFFFFFF or less.");
        }

        if (this.OriginalSize < 0) {
            throw new IllegalStateException("OriginalSize must be 0 or more.");
        }

        // ヘッダ出力
        byte[] HeaderData;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            LittleEndian.writeShort(out, WordSize);
            out.write(CompressMethod);
            LittleEndian.writeInt(out, (int) this.CompressedSize);
            LittleEndian.writeInt(out, (int) this.OriginalSize);
            LittleEndian.writeInt(out, (int) (this.LastModified.getTime() / 1000L));
            out.write(0x20);
            out.write(this.HeaderLevel);
            LittleEndian.writeShort(out, this.CRC);
            out.write(this.OSID);
            LittleEndian.writeInt(out, HeaderLength);

            for (byte[] extendHeader : ExtendHeaders) {
                if (extendHeader != null) {
                    LittleEndian.writeInt(out, extendHeader.length + 4);
                    out.write(extendHeader);
                }
            }
            LittleEndian.writeInt(out, 0);

            out.close();
            HeaderData = out.toByteArray();
        } catch (IOException exception) {
            throw new Error("caught the IOException ( " + exception.getMessage()
                    + " ) which should be never thrown by ByteArrayOutputStream.");
        }

        int CRCIndex = LhaHeader.getCRC16Position(HeaderData);
        LittleEndian.writeShort(HeaderData, CRCIndex, LhaHeader.calcHeaderCRC16(HeaderData));

        return HeaderData;
    }

    /**
     * このLhaHeaderのデータを使用して ヘッダデータを生成し、
     * それをバイト配列の形で得る。
     *
     * @param encode 文字列情報を出力する際に使用するエンコード
     * @throws IllegalStateException        <ol>
     *                                      <li>圧縮法文字列をencodeでバイト配列に
     *                                      したものが 5byteで無い場合
     *                                      <li>レベル0,1,2で ファイル名が長すぎるため
     *                                      ヘッダに収まりきらない。
     *                                      <li>レベル1,2で共通拡張ヘッダが大きすぎて出力できない。
     *                                      そのためヘッダのCRC格納場所が無い。
     *                                      <li>レベル0以外で CRC に レベル0ヘッダで
     *                                      CRC情報が無い事を示す特別な値である
     *                                      LhaHeader.NO_CRC( -2 ) が設定されていた。
     *                                      <li>レベル0,1の時にLastModifiedがMS-DOS形式
     *                                      で表現できない範囲の時間であった場合
     *                                      <li>レベル2,3の時にLastModifiedが4バイトの
     *                                      time_tで表現できない範囲の時間であった場合
     *                                      <li>OriginalSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>OriginalSize が負値である場合
     *                                      <li>レベル0,1,3 の時に OriginalSize が
     *                                      4byte値で表現できない値である場合
     *                                      <li>CompressedSize にサイズが不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>CompressedSize が負値である場合
     *                                      <li>レベル0,1,3 の時に CompressedSize が
     *                                      4byte値で表現できない値である場合
     *                                      <li>レベル2の時にOriginalSize または CompressedSizeが
     *                                      4バイト値を超えるためファイルサイズヘッダが必要な際に
     *                                      他の拡張ヘッダが大きすぎてファイルサイズヘッダが出力出来ない場合。
     *                                      <li>CRC にCRC16値が不明である事を示す
     *                                      特別な値である LhaHeader.UNKNOWN( -1 )が設定されていた。
     *                                      <li>ヘッダレベルが 0,1,2,3 以外である場合
     *                                      </ol>
     *                                      の何れか。
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     */
    private byte[] exportHeader(String encode) throws UnsupportedEncodingException {
        return switch (this.HeaderLevel) {
            case 0 -> this.exportLevel0Header(encode);
            case 1 -> this.exportLevel1Header(encode);
            case 2 -> this.exportLevel2Header(encode);
            case 3 -> this.exportLevel3Header(encode);
            default -> throw new IllegalStateException("unknown header level \"" + this.HeaderLevel + "\".");
        };
    }

    /**
     * 共通拡張ヘッダをバイト配列の形にして出力する。
     * このメソッドは ExtraExtHeaders に 共通拡張ヘッダの情報が
     * 登録されていればその情報を、でなれば 0で初期化された
     * 3 バイトのバイト配列を返すだけである。
     *
     * @return 共通拡張ヘッダをバイト配列に格納したもの
     */
    private byte[] exportCommonExtHeader() {
        if (this.ExtraExtHeaders != null) {
            for (int i = 0; i < this.ExtraExtHeaders.size(); i++) {
                byte[] ExtendHeaderData = this.ExtraExtHeaders.elementAt(i);

                if (ExtendHeaderData[0] == 0x00) {
                    return ExtendHeaderData;
                }
            }
        }

        return new byte[3];
    }

    /**
     * ファイル名拡張ヘッダをバイト配列の形にして出力する。
     * ファイル名拡張ヘッダは空でも出力される。
     *
     * @param encode 文字列情報を出力する際に使用するエンコード
     * @return ファイル名拡張ヘッダをバイト配列に格納したもの
     */
    private byte[] exportFileNameExtHeader(String encode) throws UnsupportedEncodingException {
        byte[] FileName = this.getFileName().getBytes(encode); // After Java 1.1

        byte[] ExtendHeaderData = new byte[FileName.length + 1];
        ExtendHeaderData[0] = 0x01; // 拡張ヘッダIDを設定
        System.arraycopy(FileName, 0, ExtendHeaderData, 1, FileName.length);
        return ExtendHeaderData;
    }

    /**
     * ディレクトリ名拡張ヘッダをバイト配列の形にして出力する。
     * このメソッドでは ディレクトリ名拡張ヘッダは
     * 空でも出力されるが、ディレクトリ名拡張ヘッダが空である
     * 場合は exportExtHeaders() の段階で取り除かれる。
     *
     * @param encode 文字列情報を出力する際に使用するエンコード
     * @return ディレクトリ名拡張ヘッダをバイト配列に格納したもの
     */
    private byte[] exportDirNameExtHeader(String encode) throws UnsupportedEncodingException {

        final byte LhaFileSeparator = (byte) 0xFF;
        String dir = this.getDirName();

        Vector<byte[]> vec = new Vector<>();
        int index = 0;
        int len = 0;
        int length = 0;
        while (index + len < dir.length()) {
            if (dir.charAt(index + len) == File.separatorChar) {
                byte[] src = dir.substring(index, index + len).getBytes(encode);
                byte[] array = new byte[src.length + 1];
                System.arraycopy(src, 0, array, 0, src.length);
                array[src.length] = LhaFileSeparator;
                length += array.length;
                vec.addElement(array);

                index += len + 1;
                len = 0;
            } else if (index + len + 1 < dir.length()) {
                byte[] array = dir.substring(index, index + len + 1).getBytes(encode);
                length += array.length;
                vec.addElement(array);

                index += len + 1;
                len = 0;
            } else {
                len++;
            }
        }

        byte[] ExtendHeaderData = new byte[length + 1];
        ExtendHeaderData[0] = 0x02; // 拡張ヘッダIDを設定
        index = 1;
        for (int i = 0; i < vec.size(); i++) {
            byte[] array = vec.elementAt(i);

            System.arraycopy(array, 0, ExtendHeaderData, index, array.length);
            index += array.length;
        }

        return ExtendHeaderData;
    }

    /**
     * 64bitファイルサイズヘッダをバイト配列にして出力する。
     * このメソッドはオリジナルサイズ、または圧縮後サイズが
     * 32bit値で表現できる場合でもバイト配列を出力する。
     * 必要の無い場合には exportExtHeaders() が出力を抑止する。
     *
     * @return 64bitファイルサイズヘッダ
     */
    private byte[] exportFileSizeHeader() {
        byte[] ExtendHeaderData = new byte[17];

        ExtendHeaderData[0] = (byte) 0x42;
        LittleEndian.writeLong(ExtendHeaderData, 1, this.CompressedSize);
        LittleEndian.writeLong(ExtendHeaderData, 9, this.OriginalSize);

        return ExtendHeaderData;
    }

    /**
     * 拡張ヘッダをバイト配列の形にして出力する。
     * このメソッドをオーバーライドする事によって
     * 様々な拡張ヘッダに対応することが可能となる。
     * LhaHeader では private メンバである
     * ExtraExtHeaders に登録された拡張ヘッダの情報を
     * 返すだけである。
     * 出力の形式は 第一バイト目に拡張ヘッダ識別子
     * 続いて、拡張ヘッダデータが格納され、
     * 次の拡張ヘッダの大きさは添付されない。
     *
     * @param encode 文字列情報を出力する際に使用するエンコード
     * @return 1つの拡張ヘッダを1つのバイト配列に格納し、 それを配列の形にしたもの
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     */
    protected byte[][] exportExtendHeaders(String encode) throws UnsupportedEncodingException {
        if (this.ExtraExtHeaders != null) {
            byte[][] ExtendHeaders = new byte[this.ExtraExtHeaders.size()][];

            for (int i = 0; i < this.ExtraExtHeaders.size(); i++) {
                ExtendHeaders[i] = this.ExtraExtHeaders.elementAt(i);
            }

            return ExtendHeaders;
        } else {
            return new byte[0][];
        }
    }

    /**
     * 拡張ヘッダをバイト配列の形にして出力する。
     *
     * @param encode 文字列情報を出力する際に使用するエンコード
     * @return 1つの拡張ヘッダを1つのバイト配列に格納し、それを全ての拡張ヘッダの配列の形にしたもの
     * @throws UnsupportedEncodingException encode で指定されたエンコードがサポートされない場合
     */
    private byte[][] exportExtHeaders(String encode) throws UnsupportedEncodingException {
        byte[] CommonExtHeader = this.exportCommonExtHeader();
        byte[] FileNameExtHeader = this.exportFileNameExtHeader(encode);
        byte[] DirNameExtHeader = this.exportDirNameExtHeader(encode);

        byte[][] ExtraExtHeaders = this.exportExtendHeaders(encode);
        Vector<byte[]> Headers = new Vector<>();

        Headers.addElement(CommonExtHeader);
        Headers.addElement(FileNameExtHeader);
        if (1 < DirNameExtHeader.length) {
            Headers.addElement(DirNameExtHeader);
        }

        if (this.HeaderLevel == 2 && (0x0000000100000000L <= this.CompressedSize || 0x0000000100000000L <= this.OriginalSize)) {
            Headers.addElement(this.exportFileSizeHeader());
        }

        for (byte[] ExtendHeaderData : ExtraExtHeaders) {
            if (0 < ExtendHeaderData.length && ExtendHeaderData[0] != 0x00 && ExtendHeaderData[0] != 0x01
                    && ExtendHeaderData[0] != 0x02) {
                Headers.addElement(ExtendHeaderData);
            }
        }

        byte[][] ExtendHeaders = new byte[Headers.size()][];
        for (int i = 0; i < ExtendHeaders.length; i++) {
            ExtendHeaders[i] = Headers.elementAt(i);
        }

        return ExtendHeaders;
    }

    /**
     * ヘッダデータが正当であるかをチェックする。
     *
     * @param HeaderData ヘッダデータをバイト配列に格納したもの
     * @return ヘッダデータが正当であれば true 違えば false
     */
    public static boolean checkHeaderData(byte[] HeaderData) {
        final int HeaderLevelIndex = 20;
        try {
            switch (HeaderData[HeaderLevelIndex] & 0xFF) {
                case 0:
                    return LhaHeader.verifyHeaderChecksum(HeaderData);

                case 1:
                    return LhaHeader.verifyHeaderChecksum(HeaderData)
                            && (LhaHeader.getCRC16Position(HeaderData) == -1 || LhaHeader.verifyHeaderCRC16(HeaderData));

                case 2:
                    return LhaHeader.verifyHeaderCRC16(HeaderData);

                case 3:
                    return LhaHeader.verifyHeaderCRC16(HeaderData);

            }
        } catch (ArrayIndexOutOfBoundsException exception) { // Ignore
        }
        return false;
    }

    /**
     * ヘッダのCRC値を格納している位置を得る。
     *
     * @param HeaderData ヘッダデータをバイト配列に格納したもの
     * @return ヘッダのCRC値の位置 ヘッダがCRC値を持たない場合は -1
     */
    private static int getCRC16Position(byte[] HeaderData) {
        final int HeaderLevelIndex = 20;
        int WordSize;
        int position;
        int length;

        switch (HeaderData[HeaderLevelIndex] & 0xFF) {
            case 1:
                WordSize = 2;
                position = length = (HeaderData[0] & 0xFF) + 2;
                break;
            case 2:
                WordSize = 2;
                position = length = 26;
                break;
            case 3:
                WordSize = 4;
                position = length = 32;
                break;
            default:
                return -1;
        }

        while (true) {
            if (0 < length && position < HeaderData.length) {
                length = 0;

                for (int i = 0; i < WordSize; i++) {
                    length = (length << 8 | (HeaderData[position - (1 + i)] & 0xFF));
                }

                if (HeaderData[position] == 0) {
                    return position + 1;
                }

                position += length;
            } else {
                return -1;
            }
        }
    }

    /**
     * レベル0ヘッダ、レベル1ヘッダの
     * ヘッダデータからチェックサム値を計算する。
     *
     * @param HeaderData ヘッダデータをバイト配列に格納したもの
     * @return 計算されたヘッダのチェックサム値
     */
    private static int calcHeaderChecksum(byte[] HeaderData) {
        int length = HeaderData[0] & 0xFF;

        LhaChecksum checksum = new LhaChecksum();
        checksum.update(HeaderData, 2, length);

        return (int) checksum.getValue();
    }

    /**
     * レベル1ヘッダ、レベル2ヘッダ、レベル3ヘッダの
     * ヘッダデータからCRC16値を計算する。
     *
     * @param HeaderData ヘッダデータをバイト配列に格納したもの
     * @return 計算されたヘッダのCRC16値
     */
    private static int calcHeaderCRC16(byte[] HeaderData) {
        int position = LhaHeader.getCRC16Position(HeaderData);
        int crcValue = 0;
        if (position != -1) {
            crcValue = LittleEndian.readShort(HeaderData, position);
            LittleEndian.writeShort(HeaderData, position, 0);
        }

        CRC16 crc16 = new CRC16();
        crc16.update(HeaderData);

        if (position != -1) {
            LittleEndian.writeShort(HeaderData, position, crcValue);
        }

        return (int) crc16.getValue();
    }

    /**
     * レベル0ヘッダ、レベル1ヘッダの
     * ヘッダデータからチェックサム値を読み込む。
     *
     * @param HeaderData ヘッダデータをバイト配列に格納したもの
     * @return ヘッダに記録されたチェックサム値
     */
    private static int readHeaderChecksum(byte[] HeaderData) {
        return HeaderData[1] & 0xFF;
    }

    /**
     * レベル1ヘッダ、レベル2ヘッダ、レベル3ヘッダの
     * ヘッダデータからCRC16値を読み込む。
     *
     * @param HeaderData ヘッダデータをバイト配列に格納したもの
     * @return ヘッダに記録されたCRC16値
     */
    private static int readHeaderCRC16(byte[] HeaderData) {
        int position = LhaHeader.getCRC16Position(HeaderData);
        if (position != -1) {
            return LittleEndian.readShort(HeaderData, position);
        } else {
            return -1;
        }
    }

    /**
     * チェックサム値によってヘッダデータの正当性をチェックする。
     *
     * @param HeaderData ヘッダデータをバイト配列に格納したもの
     * @return チェックサム値によってヘッダデータの正当性が証明されれば true、証明されなければ false
     */
    private static boolean verifyHeaderChecksum(byte[] HeaderData) {
        final int HeaderLevelIndex = 20;

        return switch (HeaderData[HeaderLevelIndex] & 0xFF) {
            case 0, 1 -> LhaHeader.readHeaderChecksum(HeaderData) == LhaHeader.calcHeaderChecksum(HeaderData);
            default -> false;
        };
    }

    /**
     * CRC16値によってヘッダデータの正当性をチェックする。
     *
     * @param HeaderData ヘッダデータをバイト配列に格納したもの
     * @return CRC16値によってヘッダデータの正当性が証明されれば true、証明されなければ false
     */
    private static boolean verifyHeaderCRC16(byte[] HeaderData) {
        final int HeaderLevelIndex = 20;

        return switch (HeaderData[HeaderLevelIndex] & 0xFF) {
            case 1, 2, 3 -> LhaHeader.readHeaderCRC16(HeaderData) == LhaHeader.calcHeaderCRC16(HeaderData);
            default -> false;
        };
    }

    /**
     * 入力ストリームから 最初のヘッダを読み込む。<br>
     * このメソッドはレベル1ヘッダ、もしくは レベル3ヘッダに
     * 似たデータが存在すると、ヘッダ全てを読み込もうとして
     * in.mark( 65536 ) の 限界を超えて 読み込む可能性があり、
     * その結果 reset() できずに その間のデータを読み落とす
     * 可能性がある。<br>
     * また、InputStream のmark/reset の実装次第では
     * ストリーム終端付近で ヘッダに似たデータが存在すると
     * ヘッダを全て読み込もうとして EndOfStreamに達してしまい、
     * reset()できずに その間のデータを読み落とす可能性がある。
     *
     * @param in ヘッダデータを読み込む入力ストリームストリームは mark/resetのサポートを必要とする。
     * @return 読み取られたヘッダデータヘッダが見つからずに EndOfStream に達した場合は null
     * @throws IOException              入出力エラーが発生した場合
     * @throws IllegalArgumentException in が mark/resetをサポートしない場合
     */
    public static byte[] getFirstHeaderData(InputStream in) throws IOException {
        if (in.markSupported()) {
            try {
                int stock1 = -1;
                int stock2 = -1;
                int read;

                while (0 <= (read = in.read())) {
                    if (read == '-' && 0 < stock1) {
                        in.mark(65536); // 65536 で保証できるのは level 0,2 のみ
                        LhaHeader.ensureSkip(in, 3);
                        if (in.read() == '-') {
                            LhaHeader.ensureSkip(in, 13);
                            int HeaderLevel = in.read();
                            in.reset();
                            byte[] HeaderData = switch (HeaderLevel) {
                                case 0 -> LhaHeader.readLevel0HeaderData(stock1, stock2, read, in);
                                case 1 -> LhaHeader.readLevel1HeaderData(stock1, stock2, read, in);
                                case 2 -> LhaHeader.readLevel2HeaderData(stock1, stock2, read, in);
                                case 3 -> LhaHeader.readLevel3HeaderData(stock1, stock2, read, in);
                                default -> null;
                            };

                            if (HeaderData != null && LhaHeader.checkHeaderData(HeaderData))
                                return HeaderData;
                        }
                        in.reset();
                    }
                    stock1 = stock2;
                    stock2 = read;
                }
            } catch (EOFException exception) { // Ignore
            }
            return null;
        } else {
            throw new IllegalArgumentException("InputStream needed mark()/reset() support.");
        }
    }

    /**
     * 入力ストリームから 次のヘッダを読み込む。<br>
     * このメソッドはレベル1ヘッダ、もしくは レベル3ヘッダに
     * 似たデータが存在すると、ヘッダ全てを読み込もうとして
     * in.mark( 65536 ) の 限界を超えて 読み込む可能性があり、
     * その結果 reset() できずに その間のデータを読み落とす
     * 可能性がある。<br>
     * また、ストリーム終端付近で ヘッダに似たデータが存在する
     * と ヘッダを全て読み込もうとして EndOfStreamに達してしまい、
     * reset()できずに その間のデータを読み落とす可能性がある。
     *
     * @param in ヘッダデータを読み込む入力ストリーム ストリームは mark/resetのサポートを必要とする。
     * @return 読み取られたヘッダデータ ヘッダが見つからずに EndOfStream に達した場合は null
     * @throws IOException              入出力エラーが発生した場合
     * @throws IllegalArgumentException in が mark/resetをサポートしない場合
     */
    public static byte[] getNextHeaderData(InputStream in) throws IOException {
        if (in.markSupported()) {
            try {
                int first = in.read();
                if (0 < first) { // 負の値は EndOfStream に到達、 0 の場合は書庫終端に到達
                    int second = in.read();
                    int third = in.read();
                    in.mark(65536); // 65536 で保証できるのは level 0, 2 のみ
                    LhaHeader.ensureSkip(in, 3);
                    int seventh = in.read();
                    if (third == '-' && seventh == '-') {
                        LhaHeader.ensureSkip(in, 13);
                        int HeaderLevel = in.read();
                        in.reset();
                        byte[] HeaderData = switch (HeaderLevel) {
                            case 0 -> LhaHeader.readLevel0HeaderData(first, second, third, in);
                            case 1 -> LhaHeader.readLevel1HeaderData(first, second, third, in);
                            case 2 -> LhaHeader.readLevel2HeaderData(first, second, third, in);
                            case 3 -> LhaHeader.readLevel3HeaderData(first, second, third, in);
                            default -> null;
                        };

                        if (HeaderData != null && LhaHeader.checkHeaderData(HeaderData)) {
                            return HeaderData;
                        }
                    }
                }
            } catch (EOFException exception) { // Ignore
            }
            return null;
        } else {
            throw new IllegalArgumentException("InputStream needed mark()/reset() support.");
        }
    }

    /**
     * 入力ストリームからレベル0ヘッダを読み込む
     *
     * @param HeaderLength    ヘッダの長さ
     * @param HeaderChecksum  ヘッダのチェックサム
     * @param CompressMethod1 圧縮法文字列
     * @param in              ヘッダデータを読み込む入力ストリーム
     * @return 読み取られたヘッダデータ
     * @throws IOException  入出力エラーが発生した場合
     * @throws EOFException ヘッダの読み込み途中で EndOfStream に達した場合
     */
    private static byte[] readLevel0HeaderData(int HeaderLength,
                                               int HeaderChecksum,
                                               int CompressMethod1,
                                               InputStream in) throws IOException {
        byte[] HeaderData = new byte[HeaderLength + 2];
        HeaderData[0] = (byte) HeaderLength;
        HeaderData[1] = (byte) HeaderChecksum;
        HeaderData[2] = (byte) CompressMethod1;
        int readed = 3;
        int length = 0;
        HeaderLength += 2;

        while (readed < HeaderLength && 0 <= length) {
            length = in.read(HeaderData, readed, HeaderLength - readed);
            readed += length;
        }

        if (readed == HeaderLength) {
            return HeaderData;
        } else {
            throw new EOFException();
        }
    }

    /**
     * 入力ストリームからレベル1ヘッダを読み込む
     *
     * @param BaseHeaderLength   基本ヘッダの長さ
     * @param BaseHeaderChecksum 基本ヘッダのチェックサム
     * @param CompressMethod1    圧縮法文字列
     * @param in                 ヘッダデータを読み込む入力ストリーム
     * @return 読み取られたヘッダデータ。
     * レベル1ヘッダでないことが判明した場合は nullを返す。
     * @throws IOException  入出力エラーが発生した場合
     * @throws EOFException ヘッダの読み込み途中で EndOfStream に達した場合
     */
    private static byte[] readLevel1HeaderData(int BaseHeaderLength,
                                               int BaseHeaderChecksum,
                                               int CompressMethod1,
                                               InputStream in) throws IOException {
        int HeaderLength = BaseHeaderLength + 2;
        Vector<byte[]> headers = new Vector<>();
        byte[] HeaderData = new byte[HeaderLength];
        HeaderData[0] = (byte) BaseHeaderLength;
        HeaderData[1] = (byte) BaseHeaderChecksum;
        HeaderData[2] = (byte) CompressMethod1;

        // ヘッダデータ取得
        int readed;
        int length = 0;
        do {
            if (headers.isEmpty()) {
                readed = 3;
            } else {
                readed = 0;
            }

            while (readed < HeaderLength && 0 <= length) {
                length = in.read(HeaderData, readed, HeaderLength - readed);
                readed += length;
            }

            if (readed == HeaderLength) {
                if (headers.isEmpty() && !LhaHeader.verifyHeaderChecksum(HeaderData)) {
                    return null;
                } else {
                    headers.addElement(HeaderData);
                }
            } else {
                throw new EOFException();
            }

            length = HeaderLength;
            HeaderLength = LittleEndian.readShort(HeaderData, HeaderLength - 2);
            HeaderData = new byte[HeaderLength];
        } while (0 < HeaderLength && readed == length);

        // 取得したヘッダデータを一つのバイト配列に
        HeaderLength = 0;
        for (int i = 0; i < headers.size(); i++) {
            HeaderLength += headers.elementAt(i).length;
        }

        HeaderData = new byte[HeaderLength];
        int position = 0;
        for (int i = 0; i < headers.size(); i++) {
            byte[] Data = headers.elementAt(i);
            System.arraycopy(Data, 0, HeaderData, position, Data.length);

            position += Data.length;
        }
        return HeaderData;
    }

    /**
     * 入力ストリームからレベル2ヘッダを読み込む
     *
     * @param HeaderLengthLow ヘッダの長さ下位バイト
     * @param HeaderLengthHi  ヘッダの長さ上位バイト
     * @param CompressMethod1 圧縮法文字列
     * @param in              ヘッダデータを読み込む入力ストリーム
     * @return 読み取られたヘッダデータ
     * @throws IOException  入出力エラーが発生した場合
     * @throws EOFException ヘッダの読み込み途中で EndOfStreamに達した場合
     */
    private static byte[] readLevel2HeaderData(int HeaderLengthLow,
                                               int HeaderLengthHi,
                                               int CompressMethod1,
                                               InputStream in) throws IOException {
        int HeaderLength = (HeaderLengthHi << 8) | HeaderLengthLow;
        byte[] HeaderData = new byte[HeaderLength];
        HeaderData[0] = (byte) HeaderLengthLow;
        HeaderData[1] = (byte) HeaderLengthHi;
        HeaderData[2] = (byte) CompressMethod1;

        int readed = 3;
        int length = 0;
        while (readed < HeaderLength && 0 <= length) {
            length = in.read(HeaderData, readed, HeaderLength - readed);
            readed += length;
        }

        if (readed == HeaderLength) {
            return HeaderData;
        } else {
            throw new EOFException();
        }
    }

    /**
     * 入力ストリームからレベル3ヘッダを読み込む。<br>
     * このメソッドは 他の読み込みメソッドと違い、
     * getNextHeaderData() において mark() されて
     * いる事を前提としている。
     *
     * @param WordSizeLow     ヘッダに使用されるワードサイズ 下位バイト
     * @param WordSizeHi      ヘッダに使用されるワードサイズ 上位バイト
     * @param CompressMethod1 圧縮法文字列
     * @param in              ヘッダデータを読み込む入力ストリーム
     * @return 読み取られたヘッダデータ。<br>
     * レベル3ヘッダでないことが判明した場合は nullを返す。
     * @throws IOException  入出力エラーが発生した場合
     * @throws EOFException ヘッダの読み込み途中で EndOfStream に達した場合
     */
    private static byte[] readLevel3HeaderData(int WordSizeLow,
                                               int WordSizeHi,
                                               int CompressMethod1,
                                               InputStream in) throws IOException {
        if (WordSizeLow == 0x04 && WordSizeHi == 0x00) {
            in.skip(21);
            int HeaderLength = LittleEndian.readInt(in);
            in.reset();

            byte[] HeaderData = new byte[HeaderLength];
            HeaderData[0] = (byte) WordSizeLow;
            HeaderData[1] = (byte) WordSizeHi;
            HeaderData[2] = (byte) CompressMethod1;

            int readed = 3;
            int length = 0;
            while (readed < HeaderLength && 0 <= length) {
                length = in.read(HeaderData, readed, HeaderLength - readed);
                readed += length;
            }

            if (readed == HeaderLength) {
                return HeaderData;
            } else {
                throw new EOFException();
            }
        } else {
            return null;
        }
    }

    /**
     * property の キー"lha.header" に結び付けられた生成式を使用して
     * HeaderData から LhaHeader のインスタンスを生成する。
     *
     * @param HeaderData ヘッダのデータを持つバイト配列
     * @param property   LhaProperty.parse() で LhaHeader のインスタンスが生成できるような
     *                   生成式を キー"lha.header" の値として持つプロパティ
     * @return LhaHeader のインスタンス
     */
    public static LhaHeader createInstance(byte[] HeaderData, Properties property) {

        String encoding = property.getProperty("lha.encoding");
        if (encoding == null) {
            encoding = LhaProperty.getProperty("lha.encoding");
        }

        String packages = property.getProperty("lha.packages");
        if (packages == null) {
            packages = LhaProperty.getProperty("lha.packages");
        }

        String generator = property.getProperty("lha.header");
        if (generator == null) {
            generator = LhaProperty.getProperty("lha.header");
        }

        Hashtable<String, Object> substitute = new Hashtable<>();
        substitute.put("data", HeaderData);
        substitute.put("encoding", encoding);

        return (LhaHeader) LhaProperty.parse(generator, substitute, packages);
    }

    /**
     * InputStream を len バイトスキップする。
     *
     * @param in  入力ストリーム
     * @param len スキップする長さ
     * @throws IOException  入出力エラーが発生した場合
     * @throws EOFException EndOfStream に達した場合。
     */
    private static void ensureSkip(InputStream in, long len) throws IOException {
        while (0 < len) {
            long skiplen = in.skip(len);
            if (skiplen <= 0) {
                if (0 <= in.read()) {
                    len--;
                } else {
                    throw new EOFException();
                }
            } else {
                len -= skiplen;
            }
        }
    }
}
