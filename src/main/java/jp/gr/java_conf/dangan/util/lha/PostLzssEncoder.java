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

import java.io.IOException;


/**
 * LZSS圧縮コードを処理する インターフェイス。
 *
 * <pre>
 * -- revision history --
 * $Log: PostLzssEncoder.java,v $
 * Revision 1.0  2002/07/25 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public interface PostLzssEncoder {

    /**
     * この PostLzssEncoder にバッファリングされている
     * 出力可能なデータを出力先の OutputStream に出力し、
     * 出力先の OutputStream を flush() する。<br>
     * java.io.OutputStream の メソッド flush() と似ているが、
     * flush() しなかった場合と flush() した場合の出力については
     * 同じであることを保証しなくて良い。<br>
     * つまりOutputStream の flush() では同じデータを出力する事を
     * 期待されるような以下の二つのコードは、
     * PostLzssEncoder においては 別のデータを出力をしても良い。
     *
     * <pre>
     * (1)
     *   PostLzssEncoder out = new ImplementedPostLzssEncoder();
     *   out.writeCode( 0 );
     *   out.writeCode( 0 );
     *   out.writeCode( 0 );
     *   out.close();
     *
     * (2)
     *   PostLzssEncoder out = new ImplementedPostLzssEncoder();
     *   out.writeCode( 0 );
     *   out.flush();
     *   out.writeCode( 0 );
     *   out.flush();
     *   out.writeCode( 0 );
     *   out.close();
     * </pre>
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    void flush() throws IOException;

    /**
     * この出力ストリームと、接続された出力ストリームを閉じ、
     * 使用していたリソースを開放する。<br>
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    void close() throws IOException;

    /**
     * 1byte の LZSS未圧縮のデータもしくは、
     * LZSS で圧縮された圧縮コードのうち一致長を書きこむ。<br>
     * 未圧縮データは 0〜255、
     * LZSS圧縮コード(一致長)は 256〜510 を使用すること。
     *
     * @param code 1byte の LZSS未圧縮のデータもしくは、LZSS で圧縮された圧縮コードのうち一致長
     * @throws IOException 入出力エラーが発生した場合
     */
    void writeCode(int code) throws IOException;

    /**
     * LZSS で圧縮された圧縮コードのうち一致位置を書きこむ。<br>
     *
     * @param offset LZSS で圧縮された圧縮コードのうち一致位置
     * @throws IOException 入出力エラーが発生した場合
     */
    void writeOffset(int offset) throws IOException;

    /**
     * このPostLzssEncoderが処理するLZSS辞書のサイズを得る。
     */
    int getDictionarySize();

    /**
     * このPostLzssEncoderが処理する最大一致長を得る。
     */
    int getMaxMatch();

    /**
     * このPostLzssEncoderが処理する圧縮、非圧縮の閾値を得る。
     */
    int getThreshold();
}
