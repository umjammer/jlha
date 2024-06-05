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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;


/**
 * LZSS 圧縮されたデータを解凍しながら供給する入力ストリーム。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: LzssInputStream.java,v $
 * Revision 1.1  2002/12/08 00:00:00  dangan
 * [bug fix]
 *     mark() 内で接続された PreLzssDecoder の
 *     mark に与える readLimit の計算が甘かったのを修正。
 *
 * Revision 1.0  2002/07/25 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     available() のスペルミスを修正。
 *     skip() において decode() を呼ぶ判定条件が間違っていたのを修正。
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class LzssInputStream extends InputStream {

    /**
     * LZSS圧縮コードを返す入力ストリーム
     */
    private PreLzssDecoder decoder;

    /**
     * LZSS圧縮に使用される閾値。
     * 一致長が この値以上であれば、圧縮コードを出力する。
     */
    private final int Threshold;

    /**
     * LZSS圧縮に使用される値。
     * 最大一致長を示す。
     */
    private final int MaxMatch;

    /**
     * 解凍後のデータサイズ
     */
    private final long Length;

    /**
     * LZSS圧縮を展開するためのバッファ。
     */
    private byte[] TextBuffer;

    /**
     * 現在読み込み位置。
     * read() によって外部に読み出された位置を示す。
     */
    private long TextPosition;

    /**
     * 現在読み込み位置。
     * LZSS圧縮コードを展開して得られた位置を示す。
     */
    private long TextDecoded;

    /** TextBuffer のバックアップ用 */
    private byte[] MarkTextBuffer;

    /** TextPosition のバックアップ用 */
    private long MarkTextPosition;

    /** TextDecoded のバックアップ用 */
    private long MarkTextDecoded;

    /**
     * in から LZSS圧縮データ の入力を受けて、
     * 解凍されたデータを提供する入力ストリームを構築する。
     * このコンストラクタから生成された LzssInputStreamは
     * -lh1-等の解凍データの最後のデータを読み込んだ後、
     * 次のデータの読み取りで必ずEndOfStreamに達するとは
     * 限らないデータを正常に復元できない(終端以降にゴミ
     * データがつく可能性がある)。
     *
     * @param decoder LZSS圧縮データ供給ストリーム
     */
    public LzssInputStream(PreLzssDecoder decoder) {
        this(decoder, Long.MAX_VALUE);
    }

    /**
     * in から LZSS圧縮データ の入力を受けて、
     * 解凍されたデータを提供する入力ストリームを構築する。
     *
     * @param decoder LZSS圧縮データ供給ストリーム
     * @param length 解凍後のサイズ
     */
    public LzssInputStream(PreLzssDecoder decoder, long length) {
        this.MaxMatch = decoder.getMaxMatch();
        this.Threshold = decoder.getThreshold();
        this.Length = length;

        this.decoder = decoder;
        this.TextBuffer = new byte[decoder.getDictionarySize()];
        this.TextPosition = 0;
        this.TextDecoded = 0;

        if (this.decoder instanceof PreLz5Decoder)
            this.initLz5TextBuffer();
    }

    /**
     * コンストラクタで指定された PreLzssDecoder の
     * 圧縮されたデータを解凍し、1バイトのデータを供給する。
     *
     * @return 解凍された 1バイトのデータ
     *
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int read() throws IOException {
        if (this.TextDecoded <= this.TextPosition) {
            try {
                this.decode();
            } catch (EOFException exception) {
                if (this.TextDecoded <= this.TextPosition)
                    return -1;
            }
        }

        return this.TextBuffer[(int) this.TextPosition++ & (this.TextBuffer.length - 1)] & 0xFF;
    }

    /**
     * コンストラクタで指定された PreLzssDecoder の
     * 圧縮されたデータを解凍し、bufferを満たすように
     * 解凍されたデータを読み込む。
     *
     * @param buffer データを読み込むバッファ
     * @return 読みこんだデータ量
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    /**
     * コンストラクタで指定された PreLzssDecoder の
     * 圧縮されたデータを解凍し、buffer の index から
     * length バイトのデータを読み込む。
     *
     * @param buffer データを読み込むバッファ
     * @param index buffer 内のデータ読みこみ開始位置
     * @param length 読み込むデータ量
     * @return 読みこんだデータ量
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int read(byte[] buffer, int index, int length) throws IOException {
        int position = index;
        int end = index + length;
        try {
            while (position < end) {
                if (this.TextDecoded <= this.TextPosition)
                    this.decode();

                position = this.copyTextBufferToBuffer(buffer, position, end);
            }
        } catch (EOFException exception) {
            position = this.copyTextBufferToBuffer(buffer, position, end);

            if (position == index)
                return -1;
        }

        return position - index;
    }

    /**
     * 解凍されたデータを lengthバイト読み飛ばす。
     *
     * @param length 読み飛ばすデータ量(単位はバイト)
     * @return 実際に読み飛ばしたバイト数
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public long skip(long length) throws IOException {
        long end = this.TextPosition + length;
        try {
            while (this.TextPosition < end) {
                if (this.TextDecoded <= this.TextPosition)
                    this.decode();

                this.TextPosition = Math.min(end, this.TextDecoded);
            }
        } catch (EOFException exception) {
            this.TextPosition = Math.min(end, this.TextDecoded);
        }

        return length - (end - this.TextPosition);
    }

    /**
     * 接続された入力ストリームの現在位置にマークを設定し、
     * reset() メソッドでマークした時点の 読み込み位置に
     * 戻れるようにする。<br>
     * InputStream の mark() と違い、 readLimit で設定した
     * 限界バイト数より前にマーク位置が無効になる可能性がある。
     * ただし、readLimit を無視して無限に reset() 可能な
     * InputStream と接続している場合は readLimit に
     * どのような値を設定されても
     * reset() で必ずマーク位置に復旧できる事を保証する。
     *
     * @param readLimit マーク位置に戻れる限界のバイト数。
     *            このバイト数を超えてデータを読み込んだ場合 reset()できなくなる可能性がある。
     * @see PreLzssDecoder#mark(int)
     */
    @Override
    public void mark(int readLimit) {
        readLimit -= (int) (this.TextDecoded - this.TextPosition);
        int Size = this.TextBuffer.length - this.MaxMatch;
        readLimit = (readLimit + Size - 1) / Size * Size;
        this.decoder.mark(Math.max(readLimit, 0));

        if (this.MarkTextBuffer == null) {
            this.MarkTextBuffer = this.TextBuffer.clone();
        } else {
            System.arraycopy(this.TextBuffer, 0, this.MarkTextBuffer, 0, this.TextBuffer.length);
        }
        this.MarkTextPosition = this.TextPosition;
        this.MarkTextDecoded = this.TextDecoded;
    }

    /**
     * 接続された入力ストリームの読み込み位置を最後に
     * mark() メソッドが呼び出されたときの位置に設定する。
     *
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void reset() throws IOException {
        if (this.MarkTextBuffer == null) {
            throw new IOException("not marked.");
        } else if (this.TextDecoded - this.MarkTextPosition <= this.TextBuffer.length) {
            this.TextPosition = this.MarkTextPosition;
        } else if (this.decoder.markSupported()) {
            // reset
            this.decoder.reset();
            System.arraycopy(this.MarkTextBuffer, 0, this.TextBuffer, 0, this.TextBuffer.length);
            this.TextPosition = this.MarkTextPosition;
            this.TextDecoded = this.MarkTextDecoded;
        } else {
            throw new IOException("mark/reset not supported.");
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
        return this.decoder.markSupported();
    }

    /**
     * 接続された入力ストリームからブロックしないで
     * 読み込むことのできるバイト数を得る。<br>
     *
     * @return ブロックしないで読み出せるバイト数。<br>
     *
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public int available() throws IOException {
        return (int) (this.TextDecoded - this.TextPosition) + this.decoder.available();
    }

    /**
     * この入力ストリームを閉じ、使用していた
     * 全てのリソースを開放する。<br>
     *
     * @exception IOException 入出力エラーが発生した場合
     */
    @Override
    public void close() throws IOException {
        this.decoder.close();
        this.decoder = null;
        this.TextBuffer = null;
        this.MarkTextBuffer = null;
    }

    /**
     * private変数 this.in から圧縮データを読み込み
     * 解凍しながら TextBuffer にデータを書きこむ。
     *
     * @exception IOException 入出力エラーが発生した場合
     * @exception EOFException ストリーム終端に達した場合
     */
    private void decode() throws IOException {
        if (this.TextDecoded < this.Length) {
            int TextMask = this.TextBuffer.length - 1;
            int TextStart = (int) this.TextDecoded & TextMask;
            int TextPos = TextStart;
            int TextLimit = (int) (Math.min(this.TextPosition + this.TextBuffer.length - this.MaxMatch, this.Length)
                                   - this.TextDecoded)
                            + TextStart;
            try {
                while (TextPos < TextLimit) {
                    int Code = this.decoder.readCode();

                    if (Code < 0x100) {
                        this.TextBuffer[TextMask & TextPos++] = (byte) Code;
                    } else {
                        int MatchLength = (Code & 0xFF) + this.Threshold;
                        int MatchPosition = TextPos - this.decoder.readOffset() - 1;

                        while (0 < MatchLength--)
                            this.TextBuffer[TextMask & TextPos++] = this.TextBuffer[TextMask & MatchPosition++];
                    }
                }
            } finally {
                this.TextDecoded += TextPos - TextStart;
            }
        } else {
            throw new EOFException();
        }
    }

    /**
     * private 変数 this.TextBuffer から bufferにデータを転送する。
     *
     * @param buffer TextBufferの内容をコピーするバッファ
     * @param position buffer内の書き込み現在位置
     * @param end buffer内の書き込み終了位置
     * @return bufferの次に書き込みが行われるべき位置
     */
    private int copyTextBufferToBuffer(byte[] buffer, int position, int end) {
        if ((this.TextPosition & ~(this.TextBuffer.length - 1)) < (this.TextDecoded & ~(this.TextBuffer.length - 1))) {
            int length = Math.min(this.TextBuffer.length - ((int) this.TextPosition & this.TextBuffer.length - 1),
                                  end - position);

            System.arraycopy(this.TextBuffer, (int) this.TextPosition & this.TextBuffer.length - 1, buffer, position, length);

            this.TextPosition += length;
            position += length;
        }

        if (this.TextPosition < this.TextDecoded) {
            int length = Math.min((int) (this.TextDecoded - this.TextPosition), end - position);

            System.arraycopy(this.TextBuffer, (int) this.TextPosition & this.TextBuffer.length - 1, buffer, position, length);

            this.TextPosition += length;
            position += length;
        }

        return position;
    }

    /**
     * -lz5- 用に TextBuffer を初期化する。
     */
    private void initLz5TextBuffer() {
        int position = 18;
        for (int i = 0; i < 256; i++)
            for (int j = 0; j < 13; j++)
                this.TextBuffer[position++] = (byte) i;

        for (int i = 0; i < 256; i++)
            this.TextBuffer[position++] = (byte) i;

        for (int i = 0; i < 256; i++)
            this.TextBuffer[position++] = (byte) (255 - i);

        for (int i = 0; i < 128; i++)
            this.TextBuffer[position++] = 0;

        while (position < this.TextBuffer.length)
            this.TextBuffer[position++] = (byte) ' ';
    }
}
