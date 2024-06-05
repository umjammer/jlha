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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import jp.gr.java_conf.dangan.io.DisconnectableInputStream;
import jp.gr.java_conf.dangan.io.LimitedInputStream;


/**
 * 接続されたストリームからLHA書庫データを読みこみ、
 * エントリを解凍しつつ読み込むためのユーティリティクラス。<br>
 * java.util.zip.ZipInputStream と似たインターフェイスを持つように作った。<br>
 * 壊れた書庫の処理に関しては壊れたエントリ以降の
 * 壊れていないエントリも正常に読みこめない可能性がある。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: LhaInputStream.java,v $
 * Revision 1.1.2.1  2003/07/20 13:22:31  dangan
 * [bug fix]
 *     getNextEntry() で CompressMethod.connectDecoder に
 *     this.limit を渡すべきところで this.in を渡していた。
 *
 * Revision 1.1  2002/12/08 00:00:00  dangan
 * [maintenance]
 *     LhaConstants から CompressMethod へのクラス名の変更に合わせて修正。
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     コンストラクタから 引数に String encode を取るものを廃止、
 *     Properties を引数に取るものを追加。
 *     書庫終端に達した場合はそれ以上読み込めないように修正。
 *     available() の振る舞いを java.util.zip.ZipInputStream と同じように
 *     エントリの終端に達していない場合は 1 エントリの終端に達した場合は 0 を返すように変更。
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1.2.1 $
 */
public class LhaInputStream extends InputStream {

    /**
     * LHA書庫形式のデータを供給するInputStream。
     */
    private InputStream source;

    /**
     * 既に最初のエントリを読み込んでいるかを示す。
     */
    private boolean alreadyOpenedFirstEnrty;

    /**
     * 書庫終端に達したかを示す。
     */
    private boolean reachedEndOfArchive;

    /**
     * LHA書庫内の１エントリの解凍されたデータ
     * を供給する InputStream。
     */
    private InputStream in;

    /**
     * LHA書庫内の１エントリの圧縮されたデータ
     * を供給するLimitedInputStream。
     * closeEntry 時にスキップするため。
     */
    private LimitedInputStream limit;

    /**
     * 現在処理中のエントリの終端に達した時に true にセットされる。
     */
    private boolean reachedEndOfEntry;

    /** reachEndOfEntry のバックアップ用 */
    private boolean markReachedEndOfEntry;

    /**
     * 各圧縮形式に対応した復号器の生成式等が含まれるプロパティ
     */
    private Properties property;

    /**
     * in から LHA書庫のデータを読み取る InputStream を構築する。<br>
     * 各圧縮形式に対応した復号器の生成式等を持つプロパティには
     * LhaProperty.getProperties() で得られたプロパティが使用される。
     *
     * @param in LHA書庫形式のデータを供給する入力ストリーム
     * @see LhaProperty#getProperties()
     */
    public LhaInputStream(InputStream in) {
        Properties property = LhaProperty.getProperties();

        try {
            this.constructerHelper(in, property);
        } catch (UnsupportedEncodingException exception) {
            throw new Error("Unsupported encoding \"" + property.getProperty("lha.encoding") + "\".");
        }
    }

    /**
     * in から LHA書庫のデータを読み取る InputStreamを構築する。
     *
     * @param in       LHA書庫形式のデータを供給する入力ストリーム
     * @param property 各圧縮形式に対応した復号器の生成式等が含まれるプロパティ
     * @throws UnsupportedEncodingException property.getProperty( "lha.encoding" ) で得られた
     *                                      エンコーディング名がサポートされない場合
     */
    public LhaInputStream(InputStream in, Properties property) throws UnsupportedEncodingException {

        this.constructerHelper(in, property);
    }

    /**
     * コンストラクタの初期化処理を担当するメソッド。
     *
     * @param in       LHA書庫形式のデータを供給する入力ストリーム
     * @param property 各圧縮形式に対応した復号器の生成式等が含まれるプロパティ
     * @throws UnsupportedEncodingException encode がサポートされない場合
     */
    private void constructerHelper(InputStream in, Properties property) throws UnsupportedEncodingException {

        if (in != null && property != null) {
            String encoding = property.getProperty("lha.encoding");
            if (encoding == null) {
                encoding = LhaProperty.getProperty("lha.encoding");
            }

            // encoding名チェック
            encoding.getBytes(encoding);

            if (in.markSupported()) {
                this.source = in;
            } else {
                this.source = new BufferedInputStream(in);
            }

            this.in = null;
            this.limit = null;
            this.property = (Properties) property.clone();
            this.reachedEndOfEntry = false;
            this.reachedEndOfArchive = false;

        } else if (in == null) {
            throw new NullPointerException("in");
        } else {
            throw new NullPointerException("property");
        }
    }

    /**
     * 現在のエントリから 1バイトのデータを読み込む。
     *
     * @return 読みこまれた 1バイトのデータ。既にエントリの終端に達した場合は -1
     * @throws IOException 現在読み込み中のエントリが無いか入出力エラーが発生した場合
     */
    @Override
    public int read() throws IOException {
        if (this.in != null) {
            int ret = this.in.read();
            if (ret < 0) {
                this.reachedEndOfEntry = true;
            }
            return ret;
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * 現在のエントリから buffer を満たすようにデータを読み込む。
     *
     * @param buffer データを読み込むバッファ
     * @return 読みこまれたデータの量。既にエントリの終端に達した場合は -1。
     * @throws IOException 現在読み込み中のエントリが無いか
     *                     入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    /**
     * 現在のエントリから buffer のindexへ lengthバイトの
     * データをを読み込む。
     *
     * @param buffer データを読み込むバッファ
     * @param index  buffer内のデータ読み込み開始位置
     * @param length 読み込むデータ量
     * @return 読みこまれたデータの量。既にエントリの終端に達した場合は -1。
     * @throws IOException 現在読み込み中のエントリが無いか入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        if (this.in != null) {
            int ret = this.in.read(buffer, index, length);
            if (ret < 0) {
                this.reachedEndOfEntry = true;
            }
            return ret;
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * 現在のエントリのデータを length バイト読みとばす。
     *
     * @param length 読みとばすデータ量
     * @return 実際に読みとばしたデータ量
     * @throws IOException 現在読み込み中のエントリが無いか入出力エラーが発生した場合
     */
    @Override
    public long skip(long length) throws IOException {
        if (this.in != null) {
            if (0 < length) {
                long len = this.in.skip(length - 1);
                int ret = this.in.read();
                if (ret < 0) {
                    this.reachedEndOfEntry = true;
                    return len;
                } else {
                    return len + 1;
                }
            } else {
                return 0;
            }
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * 現在読み取り中のエントリの現在位置にマークを設定し、
     * reset() でマークした読み込み位置に戻れるようにする。
     *
     * @param readLimit マーク位置に戻れる限界読み込み量。このバイト数を超えてデータを読み込んだ場合 reset() できる保証はない。
     * @throws IllegalStateException 現在読み込み中のエントリが無い場合
     */
    @Override
    public void mark(int readLimit) {
        if (this.in != null) {
            this.in.mark(readLimit);
            this.markReachedEndOfEntry = this.reachedEndOfEntry;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * 現在読み取り中のエントリの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。
     *
     * @throws IOException 現在読み込み中のエントリが無いか入出力エラーが発生した場合
     */
    @Override
    public void reset() throws IOException {
        if (this.in != null) {
            this.in.reset();
            this.reachedEndOfEntry = this.markReachedEndOfEntry;
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * 接続された入力ストリームが mark()と
     * reset()をサポートするかを得る。<br>
     * ヘッダ読み込み時に mark/reset が必須のため
     * コンストラクタで渡された in が markSupported() で
     * false を返す場合、このクラスは in を mark/reset をサポートする
     * BufferedInputStream でラップする。
     * そのため、このメソッドは常に true を返す。
     *
     * @return 常に true
     */
    @Override
    public boolean markSupported() {
        return this.source.markSupported();
    }

    /**
     * 現在読み取り中のエントリの終端に達したかを得る。<br>
     * ブロックしないで読み込めるデータ量を返さない事に注意すること。
     *
     * @return 現在読み取り中のエントリの終端に達した場合 0 達していない場合 1
     * @throws IOException 現在読み込み中のエントリが無いか入出力エラーが発生した場合
     * @see java.util.zip.ZipInputStream#available()
     */
    @Override
    public int available() throws IOException {
        if (this.in != null) {
            return (this.reachedEndOfEntry ? 0 : 1);
        } else {
            throw new IOException("no entry");
        }
    }

    /**
     * この入力ストリームを閉じ、使用していた
     * 全てのリソースを開放する。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        if (this.in != null) {
            this.in.close();
            this.limit = null;
            this.in = null;
        }

        this.source.close();
        this.source = null;
    }

    /**
     * 次のエントリを解凍しながら読みこむようにストリームを設定する。
     *
     * @return エントリの情報を持つ LhaHeader, null when next entry doesn't exist.
     * @throws IOException 入出力エラーが発生した場合
     */
    public LhaHeader getNextEntry() throws IOException {
        if (!this.reachedEndOfArchive) {
            if (this.in != null) {
                this.closeEntry();
            }

            byte[] HeaderData;
            if (this.alreadyOpenedFirstEnrty) {
                HeaderData = LhaHeader.getNextHeaderData(this.source);
            } else {
                HeaderData = LhaHeader.getFirstHeaderData(this.source);
                this.alreadyOpenedFirstEnrty = true;
            }
            if (null != HeaderData) {
                LhaHeader header = LhaHeader.createInstance(HeaderData, this.property);
                this.in = new DisconnectableInputStream(this.source);
                this.limit = new LimitedInputStream(this.in, header.getCompressedSize());
                this.in = CompressMethod
                        .connectDecoder(this.limit, header.getCompressMethod(), this.property, header.getOriginalSize());

                this.reachedEndOfEntry = false;
                this.markReachedEndOfEntry = false;
                return header;
            } else {
                this.reachedEndOfArchive = true;
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 次のエントリを解凍しないで読みこむようにストリームを設定する。
     *
     * @return エントリの情報を持つ LhaHeader, null when next entry doesn't exist.
     * @throws IOException 入出力エラーが発生した場合
     */
    public LhaHeader getNextEntryWithoutExtract() throws IOException {

        if (!this.reachedEndOfArchive) {

            if (this.in != null) {
                this.closeEntry();
            }

            byte[] HeaderData;
            if (this.alreadyOpenedFirstEnrty) {
                HeaderData = LhaHeader.getNextHeaderData(this.source);
            } else {
                HeaderData = LhaHeader.getFirstHeaderData(this.source);
                this.alreadyOpenedFirstEnrty = true;
            }
            if (HeaderData != null) {

                LhaHeader header = LhaHeader.createInstance(HeaderData, this.property);
                this.in = new DisconnectableInputStream(this.source);
                this.limit = new LimitedInputStream(this.in, header.getCompressedSize());
                this.in = this.limit;

                this.reachedEndOfEntry = false;
                this.markReachedEndOfEntry = false;
                return header;
            } else {
                this.reachedEndOfArchive = true;
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 現在読み取り中のエントリを閉じ、
     * 次のエントリを読みこめるようにストリームを設定する。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    public void closeEntry() throws IOException {
        if (this.in != null) {
            while (0 <= this.limit.read()) {
                this.limit.skip(Long.MAX_VALUE);
            }

            this.in.close();
            this.in = null;
            this.limit = null;
        }
    }
}
