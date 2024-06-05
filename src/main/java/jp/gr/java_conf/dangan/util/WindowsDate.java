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

package jp.gr.java_conf.dangan.util;

import java.util.Date;


/**
 * WindowsのFILETIME形式の情報を扱うDateの派生クラス。<br>
 * FILETIME は 1601年 1月 1日 0時0分0秒からの経過時間を
 * 100ナノ秒単位で持つ64ビット値。<br>
 * このクラスでは FILETIME を long(64ビット値)として扱うときは
 * 基本的に符号無しとみなす。<br>
 * 1601年 1月 1日 0時0分0秒以前の時間を扱いたい場合は
 * WindowsDate( Date date ) か、WindowsDate.setTime( long time )を使用する。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: WindowsDate.java,v $
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     javadoc コメントのスペルミスを修正。
 *     ソース整備
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     set系メソッドで 範囲外の時間をセットしようとして
 *     例外を投げるケースで時間の書き戻しが正しく行われていなかった。
 *     checkRange の時間の範囲が間違っていた。
 * [maintenance]
 *     タブの廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class WindowsDate extends Date implements Cloneable {

    /**
     * FILETIME形式のデータと、java.util.Date.getTime() で
     * 得られる時間形式との時間差を 100ナノセカンド単位で示した数値。
     * なお、閏秒等は考慮に入れていない。
     */
    public static final long TIME_DIFFERENCE = 0x19DB1DED53E8000L;

    /**
     * java.util.Date では保持できない
     * ナノ秒単位の時間を保持するために用いる。
     */
    private int NanoSecounds;

    /**
     * デフォルトコンストラクタ。
     * 現在の時間情報を持つ WindowsDateを構築する。
     * ナノ秒単位の時間は取得できないため、0に設定される。
     *
     * @exception IllegalArgumentException 現在の時間が FILETIME 形式で表現できる範囲外だった場合。
     */
    public WindowsDate() {
        super();
        this.NanoSecounds = 0;

        this.checkRange();
    }

    /**
     * dateで示される時間を表す WindowsDateを構築する。<br>
     * dateが WindowsDate のインスタンスならば
     * ナノ秒単位の情報もコピーされるが、それ以外の場合は
     * ナノ秒単位の情報には 0 が設定される。
     *
     * @param date 新しく構築される WindowsDate の元となる時間情報を持つDate のオブジェクト
     * @exception IllegalArgumentException 現在の時間が FILETIME 形式で表現できる範囲外だった場合。
     */
    public WindowsDate(Date date) {
        super(date.getTime());
        if (date instanceof WindowsDate) {
            this.NanoSecounds = ((WindowsDate) date).NanoSecounds;
        } else {
            this.NanoSecounds = 0;
            this.checkRange();
        }
    }

    /**
     * 符号無し64ビットのFILETIME形式の時間情報から
     * 新しいWindowsDateを構築する。
     *
     * @param time FILETIME形式の時間情報
     */
    public WindowsDate(long time) {
        super(0 <= time ? (time - WindowsDate.TIME_DIFFERENCE) / 10000L
                        : ((time >>> 1) - (WindowsDate.TIME_DIFFERENCE >>> 1)) / 5000L);

        this.NanoSecounds = (int) ((time >>> 1) % 5000L * 2 + (time & 1)) * 100;
    }

    /**
     * このオブジェクトのコピーを返す。
     *
     * @return このWindowsDateオブジェクトの複製
     */
    @Override
    public Object clone() {
        return new WindowsDate(this);
    }

    /**
     * この WindowsDate の示す年を year で
     * 指定された値に1900を足したものに設定する。<br>
     * このメソッドは範囲チェックを行うだけのために存在する。
     *
     * @param year 1900を足すことで西暦を表すような 年の値
     * @exception IllegalArgumentException year に変更したところ FILETIME形式で扱えない範囲の時間になった場合
     * @deprecated
     */
    @Override
    public void setYear(int year) {
        long temp = this.getTime();

        try {
            super.setYear(year);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * この WindowsDate の示す月を month で指定された値に設定する。<br>
     * このメソッドは範囲チェックを行うだけのために存在する。
     *
     * @param month 0が1月、1が2月を示すような月の値
     * @exception IllegalArgumentException month に変更したところ FILETIME形式で扱えない範囲の時間になった場合
     * @deprecated
     */
    @Override
    public void setMonth(int month) {
        long temp = this.getTime();

        try {
            super.setMonth(month);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * この WindowsDate の示す 一ヶ月の
     * 中での何日目かを date で指定された値に設定する。<br>
     * このメソッドは範囲チェックを行うだけのために存在する。
     *
     * @param date 1が1日、2が2日を示すような日の値
     * @exception IllegalArgumentException date に変更したところ FILETIME形式で扱えない範囲の時間になった場合
     * @deprecated
     */
    @Override
    public void setDate(int date) {
        long temp = this.getTime();

        try {
            super.setDate(date);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * この WindowsDate の示す一日の中での時間を
     * hours で指定された値に設定する。<br>
     * このメソッドは範囲チェックを行うだけのために存在する。
     *
     * @param hours 時間の値
     * @exception IllegalArgumentException hours に変更したところ FILETIME形式で扱えない範囲の時間になった場合
     * @deprecated
     */
    @Override
    public void setHours(int hours) {
        long temp = this.getTime();

        try {
            super.setHours(hours);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * この WindowsDate の示す一時間の中での分を
     * minutes で指定された値に設定する。<br>
     * このメソッドは範囲チェックを行うだけのために存在する。
     *
     * @param minutes 分の値
     * @exception IllegalArgumentException minutes に変更したところ FILETIME形式で扱えない範囲の時間になった場合
     * @deprecated
     */
    @Override
    public void setMinutes(int minutes) {
        long temp = this.getTime();

        try {
            super.setMinutes(minutes);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * この WindowsDate の示す一分の中での秒数を
     * secounds で指定された値に設定する。<br>
     * このメソッドは範囲チェックを行うだけのために存在する。
     *
     * @param seconds 秒数
     * @exception IllegalArgumentException secounds に変更したところ FILETIME形式で扱えない範囲の時間になった場合
     * @deprecated
     */
    @Override
    public void setSeconds(int seconds) {
        long temp = this.getTime();

        try {
            super.setSeconds(seconds);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * この WindowsDate の示す時間を
     * 1970年1月1日 00:00:00 GMTから
     * time ミリ秒経過した時刻に設定する。<br>
     * このメソッドは範囲チェックを行うだけのために存在する。
     *
     * @param time 1970年1月1日 00:00:00GMT からの経過ミリ秒
     * @exception IllegalArgumentException time がFILETIME形式で扱えない範囲の時間を示していた場合
     */
    @Override
    public void setTime(long time) {
        long temp = this.getTime();

        try {
            super.setTime(time);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * この WindowsDate に FILETIME形式の時間情報を設定する。
     *
     * @param time FILETIME形式の時間情報
     */
    public void setWindowsTime(long time) {
        super.setTime(0 <= time ? (time - WindowsDate.TIME_DIFFERENCE) / 10000L
                                : ((time >>> 1) - (WindowsDate.TIME_DIFFERENCE >>> 1)) / 5000L);

        this.NanoSecounds = (int) ((time >>> 1) % 5000L * 2 + (time & 1)) * 100;
    }

    /**
     * この WindowsDateが示す時間情報を FILETIME 形式で得る。
     *
     * @return FILETIME形式の値
     */
    public long getWindowsTime() {
        return (super.getTime() * 10000L + WindowsDate.TIME_DIFFERENCE + this.NanoSecounds / 100);
    }

    /**
     * この WindowsDate が FILETIME形式で表せる時間の
     * 範囲内であるかを判定する。まだ不完全
     *
     * @exception IllegalArgumentException この WindowsDate が FILETIME形式で扱えない範囲の時間を示していた場合
     */
    private void checkRange() {
        long time = super.getTime();
        if (!(0xFFFCAE8C71E60F9BL <= time && time <= 0x000683218A10A8CBL))
            throw new IllegalArgumentException("outside of range of Windows FILETIME format. ");
    }
}
