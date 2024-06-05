/*
 * BitDataBrokenException.java
 *
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


/**
 * EndOfStream に達してしまったため要求されたビット数の
 * データを得られなかった場合に投げられる例外。<br>
 * jp.gr.java_conf.dangan.io.BitInputStream 用であるため、
 * 保持しておける データは 32ビットまでとなっている点に
 * 注意すること。<br>
 * NotEnoughBitsException と違い、こちらの例外を投げる
 * 場合には 実際に読み込み動作を行ってしまっているため
 * 読み込み位置は例外を投げる前の時点から変化してしまっ
 * ている点に注意すること。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: BitDataBrokenException.java,v $
 * Revision 1.1  2002/12/07 00:00:00  dangan
 * [maintenance]
 *     ソース整備
 *
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class BitDataBrokenException extends IOException {

    /**
     * ビットデータが途中までしか
     * 取得できない原因となった例外
     */
    private final Throwable cause;

    /**
     * 途中までのビットデータ
     */
    private final int bitData;

    /**
     * bitData の有効ビット数
     */
    private final int bitCount;

    /**
     * 新しい BitDataBrokenException を構築する。
     *
     * @param cause ビットデータが途中までしか取得できない原因となった例外
     * @param bitData 要求されたビット数に満たないビットデータ
     * @param bitCount bitData のビット数
     *
     */
    public BitDataBrokenException(Throwable cause, int bitData, int bitCount) {
        this.cause = cause;
        this.bitData = bitData;
        this.bitCount = bitCount;
    }

    /**
     * ビットデータが途中までしか
     * 取得できない原因となった例外を得る。
     *
     * @return 原因となった例外
     */
    @Override
    public Throwable getCause() {
        return this.cause;
    }

    /**
     * 要求されたビット数に満たない
     * "壊れた" ビットデータを得る。
     *
     * @return ビットデータ
     */
    public int getBitData() {
        return this.bitData;
    }

    /**
     * getBitData() で得られる
     * ビットデータの有効ビット数を得る。
     *
     * @return ビットデータの有効ビット数
     */
    public int getBitCount() {
        return this.bitCount;
    }
}
