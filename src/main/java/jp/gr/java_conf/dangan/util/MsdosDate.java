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

package jp.gr.java_conf.dangan.util;

import java.util.Date;


/**
 * MS-DOS形式の時間情報を扱うDateの派生クラス。<br>
 * データは 4byte値であり、MS-DOSが 主にIntel の x86系CPU上で
 * 動作したことから LittleEndianで格納される。<br>
 * フォーマットは以下のとおり。
 *
 * <pre>
 * +---------------+---------------++---------------+---------------+
 * | 日付-上位byte  | 日付-下位byte   || 時刻-上位byte   | 時刻-下位byte  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-++-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |7|6|5|4|3|2|1|0|7|6|5|4|3|2|1|0||7|6|5|4|3|2|1|0|7|6|5|4|3|2|1|0|
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-++-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   年-7bit   |月-4bit | 日-5bit ||時間-5bit |  分-6bit  | 秒-5bit  |
 * +-------------+-------+---------++---------+-----------+---------+
 * </pre>
 *
 * <li>年は 1980〜2107年 を 0〜127 で表す。WindowsのシステムAPIの
 * 一部は 2099年までしかサポートしないという情報がある。</li>
 * <li>月は 1〜12月 を 1〜12で表す。0〜11でないことに注意。</li>
 * <li>日は 1〜31日 を 1〜31で表す。0〜30でないことに注意。</li>
 * <li>時間は 0〜23時 を 0〜23で表す。</li>
 * <li>分は 0〜59分 を 0〜59で表す。</li>
 * <li>秒は 0〜58秒 を 0〜29で表す。秒の情報はビット数が足りない
 * ため 最小単位は 1秒でなく 2秒である。</li>
 *
 * <pre>
 * -- revision history --
 * $Log: MsdosDate.java,v $
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     javadoc コメントのスペルミスを修正。
 *     ソース整備
 *
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     setTime() で ftimeの限界精度である2秒以上の精度で記録していた。
 * [maintenance]
 *     タブの廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
@SuppressWarnings("deprecation")
public class MsdosDate extends Date implements Cloneable {

    /**
     * date で示される時間を表す MsdosDate を構築する。 <br>
     * MS-DOS 形式の時間情報で表せない細かい精度の情報は
     * 無視され、最小時間単位は java.util.Date の 1ミリ秒でなく
     * MS-DOS 形式の時間情報 の最小単位である 2秒となる。
     *
     * @param date 新しく構築される MsdosDate の基になる時間情報を持つ Dateオブジェクト
     * @throws IllegalArgumentException date が MS-DOS時間形式で扱えない範囲の時間を示していた場合
     */
    public MsdosDate(Date date) {
        super((date.getTime() / 2000L) * 2000L);
        this.checkRange();
    }

    /**
     * MS-DOS 形式の時間情報から 新しい MsdosDate を構築する。
     *
     * @param time MS-DOS 形式の時間情報
     */
    public MsdosDate(int time) {
        super(((time >> 25) & 0x7F) + 80,
                ((time >> 21) & 0x0F) - 1,
                (time >> 16) & 0x1F,
                (time >> 11) & 0x1F,
                (time >> 5) & 0x3F,
                (time << 1) & 0x3F); // deprecated

        this.checkRange();
    }

    /**
     * このオブジェクトのコピーを返す。
     *
     * @return このMsdosDateオブジェクトの複製
     */
    @Override
    public Object clone() {
        return new MsdosDate(this);
    }

    /**
     * この MsdosDate の示す年を year で指定された値に1900を足し
     * たものに設定する。<br>
     * このメソッドは範囲チェックを行うだけのために存在する。
     *
     * @param year 1900を足すことで西暦を表すような 年の値
     * @throws IllegalArgumentException year が MS-DOS時間形式で扱えない範囲の時間を示していた場合
     * @deprecated
     */
    @Override
    public void setYear(int year) {
        if (year < 80 || 207 < year) {
            throw new IllegalArgumentException("out of MS-DOS time format range.");
        } else {
            super.setYear(year); // deprecated
        }
    }

    /**
     * この MsdosDate の示す時間を 1970年1月1日 00:00:00 GMTから
     * time ミリ秒経過した時刻に設定する。<br>
     * MS-DOS 形式の時間情報で表せない細かい精度の情報は
     * 無視され、最小時間単位は java.util.Date の 1ミリ秒でなく
     * MS-DOS 形式の時間情報 の最小単位である 2秒となる。
     *
     * @param time 1970年1月1日 00:00:00GMT からの経過ミリ秒
     * @throws IllegalArgumentException time が MS-DOS時間形式で扱えない範囲の時間を示していた場合
     */
    @Override
    public void setTime(long time) {
        int year = (new Date(time)).getYear();
        if (year < 80 || 207 < year) {
            throw new IllegalArgumentException("out of MS-DOS time format range.");
        } else {
            super.setTime((time / 2000L) * 2000L);
        }
    }

    /**
     * この MsdosDate に MS-DOS 時間形式の時間情報を設定する。
     *
     * @param time MS-DOS 時間形式の時間情報
     */
    public void setMsdosTime(int time) {
        Date date = new Date(((time >> 25) & 0x7F) + 80,
                ((time >> 21) & 0x0F) - 1,
                (time >> 16) & 0x1F,
                (time >> 11) & 0x1F,
                (time >> 5) & 0x3F,
                (time << 1) & 0x3F);

        this.setTime(date.getTime());
    }

    /**
     * この MsdosDateが示す時間情報を MS-DOS 時間形式で得る。
     *
     * @return MS-DOS時間形式の値
     */
    public int getMsdosTime() {
        return ((super.getYear() - 80) << 25)
                | ((super.getMonth() + 1) << 21)
                | (super.getDate() << 16)
                | (super.getHours() << 11)
                | (super.getMinutes() << 5)
                | (super.getSeconds() >> 1);
    }

    /**
     * この MsdosDate が MS-DOS時間形式で表せる時間の範囲内で
     * あるかを判定する。
     *
     * @throws IllegalArgumentException この MsdosDate が MS-DOS時間形式で扱えない範囲の時間を示していた場合
     */
    private void checkRange() {
        int year = this.getYear();
        if (year < 80 || 207 < year)
            throw new IllegalArgumentException("out of MS-DOS time format range.");
    }
}
