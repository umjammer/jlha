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

import java.util.zip.Checksum;


/**
 * LHAで使用される 単純な 1バイトのチェックサム値を
 * 算出するためのクラス。
 *
 * <pre>
 * -- revision history --
 * $Log: LhaChecksum.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [maintanance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class LhaChecksum implements Checksum {

    /**
     * チェックサム値
     */
    private int checksum;

    /**
     * 新しい チェックサムクラスを作成する。
     */
    public LhaChecksum() {
        super();
        this.reset();
    }

    /**
     * byte8 で指定した 1バイトのデータで チェックサム値を更新する。
     *
     * @param byte8 チェックサムを更新する1バイトのデータ
     */
    @Override
    public void update(int byte8) {
        this.checksum += byte8;
    }

    /**
     * buffer で指定したバイト配列で チェックサム値を更新する。
     * このメソッドは
     * update( buffer, 0, buffer.length )
     * と同等。
     *
     * @param buffer チェックサムを更新するデータを持つバイト配列
     */
    @Override
    public void update(byte[] buffer) {
        this.update(buffer, 0, buffer.length);
    }

    /**
     * buffer で指定したバイト配列で チェックサム値を更新する。
     *
     * @param buffer チェックサムを更新するデータを持つバイト配列
     * @param index データの開始位置
     * @param length チェックサムの更新に使うバイト数
     */
    @Override
    public void update(byte[] buffer, int index, int length) {
        while (0 < length--)
            this.checksum += buffer[index++];
    }

    /**
     * チェックサム値を初期値に設定しなおす。
     */
    @Override
    public void reset() {
        this.checksum = 0;
    }

    /**
     * チェックサム値を得る。
     * チェックサム値は 1バイトの値であり、
     * 0x00〜0xFFにマップされる。
     *
     * @return チェックサム値
     */
    @Override
    public long getValue() {
        return this.checksum & 0xFF;
    }
}
