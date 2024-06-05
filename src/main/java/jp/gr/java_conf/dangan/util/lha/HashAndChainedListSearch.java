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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import jp.gr.java_conf.dangan.lang.reflect.Factory;


/**
 * ハッシュと単方向連結リストを使って高速化された LzssSearchMethod。<br>
 * 検索を打ち切ることによる高速化も行っているため、
 * 必ず最長一致を見つけることが出来るとは限らない。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: HashAndChainedListSearch.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     LzssSearchMethod のインタフェイス変更にあわせてインタフェイス変更
 * [improvement]
 *     ar940528 の TEST5相当 の実装に変更。
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class HashAndChainedListSearch implements LzssSearchMethod {

    /**
     * LZSS辞書サイズ。
     */
    private final int dictionarySize;

    /**
     * LZSS圧縮に使用される値。
     * 最大一致長を示す。
     */
    private final int maxMatch;

    /**
     * LZSS圧縮に使用される閾値。
     * 一致長が この値以上であれば、圧縮コードを出力する。
     */
    private final int threshold;

    /**
     * LZSS圧縮を施すためのバッファ。
     * 前半は辞書領域、
     * 後半は圧縮を施すためのデータの入ったバッファ。
     * LzssSearchMethodの実装内では読み込みのみ許される。
     */
    private final byte[] TextBuffer;

    /**
     * 辞書の限界位置。
     * TextBuffer前半の辞書領域にデータが無い場合に
     * 辞書領域にある不定のデータ(Javaでは0)を使用
     * して圧縮が行われるのを抑止する。
     */
    private int dictionaryLimit;

    /**
     * ハッシュ関数
     */
    private final HashMethod hashMethod;

    /**
     * ハッシュテーブル
     * 添字はハッシュ値、内容はTextBuffer内の位置
     */
    private final int[] hashTable;

    /**
     * 同じハッシュ値を持つデータパタンの連結リストの長さが
     * 一定以上になった場合にセットするフラグ。
     * <p>
     * boolean[] にすると何故か遅くなるので
     * char[] として 16個纏めて扱う。
     * 扱う場合は ユーティリティメソッド
     * isTooBig(), setTooBigFlag(), clearTooBigFlag() を介して扱う。
     */
    private final char[] tooBigFlag;

    /**
     * 同じハッシュ値を持つデータパタン開始位置を持つ
     * 単方向連結リスト。
     */
    private final int[] prev;

    /**
     * 探索試行回数の上限値を持つ。
     * この回数以上の探索は行わない。
     */
    private final int searchLimitCount;

    /**
     * ハッシュと連結リストを使用した LzssSearchMethod を構築する。<br>
     * ハッシュ関数と探索試行回数の上限値にはデフォルトのものが使用される。
     *
     * @param DictionarySize 辞書サイズ
     * @param MaxMatch 最長一致長
     * @param Threshold 圧縮、非圧縮の閾値
     * @param TextBuffer LZSS圧縮を施すためのバッファ
     */
    public HashAndChainedListSearch(int DictionarySize, int MaxMatch, int Threshold, byte[] TextBuffer) {
        this(DictionarySize, MaxMatch, Threshold, TextBuffer, HashDefault.class.getName(), 256);
    }

    /**
     * ハッシュと連結リストを使用した LzssSearchMethod を構築する。<br>
     * ハッシュ関数にはデフォルトのものが使用される。
     *
     * @param DictionarySize 辞書サイズ
     * @param MaxMatch 最長一致長
     * @param Threshold 圧縮、非圧縮の閾値
     * @param TextBuffer LZSS圧縮を施すためのバッファ
     * @param SearchLimitCount 探索試行回数の上限
     *
     * @exception IllegalArgumentException
     *                searchLimitCount が0以下の場合
     */
    public HashAndChainedListSearch(int DictionarySize, int MaxMatch, int Threshold, byte[] TextBuffer, int SearchLimitCount) {
        this(DictionarySize, MaxMatch, Threshold, TextBuffer, HashDefault.class.getName(), SearchLimitCount);
    }

    /**
     * ハッシュと連結リストを使用した LzssSearchMethod を構築する。<br>
     * 探索試行回数の上限値にはデフォルトのものが使用される。
     *
     * @param DictionarySize 辞書サイズ
     * @param MaxMatch 最長一致長
     * @param Threshold 圧縮、非圧縮の閾値
     * @param TextBuffer LZSS圧縮を施すためのバッファ
     * @param HashMethodClassName Hash関数を提供するクラス名
     * @exception NoClassDefFoundError HashMethodClassName で与えられたクラスが見つからない場合。
     * @exception InstantiationError HashMethodClassName で与えられたクラスが
     *                abstract class であるためインスタンスを生成できない場合。
     * @exception NoSuchMethodError HashMethodClassName で与えられたクラスが
     *                コンストラクタ HashMethod( byte[] )を持たない場合
     */
    public HashAndChainedListSearch(int DictionarySize,
            int MaxMatch,
            int Threshold,
            byte[] TextBuffer,
            String HashMethodClassName) {
        this(DictionarySize, MaxMatch, Threshold, TextBuffer, HashMethodClassName, 256);
    }

    /**
     * ハッシュと連結リストを使用した LzssSearchMethod を構築する。
     *
     * @param dictionarySize 辞書サイズ
     * @param maxMatch 最長一致長
     * @param threshold 圧縮、非圧縮の閾値
     * @param textBuffer LZSS圧縮を施すためのバッファ
     * @param hashMethodClassName Hash関数を提供するクラス名
     * @param searchLimitCount 探索試行回数の上限
     * @exception IllegalArgumentException searchLimitCount が0以下の場合
     * @exception NoClassDefFoundError hashMethodClassName で与えられたクラスが見つからない場合。
     * @exception InstantiationError hashMethodClassName で与えられたクラスが
     *                abstract class であるためインスタンスを生成できない場合。
     * @exception NoSuchMethodError hashMethodClassName で与えられたクラスが
     *                コンストラクタ HashMethod( byte[] )を持たない場合
     */
    public HashAndChainedListSearch(int dictionarySize,
            int maxMatch,
            int threshold,
            byte[] textBuffer,
            String hashMethodClassName,
            int searchLimitCount) {

        if (0 < searchLimitCount) {

            this.dictionarySize = dictionarySize;
            this.maxMatch = maxMatch;
            this.threshold = threshold;
            this.TextBuffer = textBuffer;
            this.dictionaryLimit = this.dictionarySize;
            this.searchLimitCount = searchLimitCount;

            try {
                this.hashMethod = (HashMethod) Factory.createInstance(hashMethodClassName, (Object) textBuffer);
            } catch (ClassNotFoundException exception) {
                throw new NoClassDefFoundError(exception.getMessage());
            } catch (InvocationTargetException exception) {
                throw new Error(exception.getTargetException().getMessage());
            } catch (NoSuchMethodException exception) {
                throw new NoSuchMethodError(exception.getMessage());
            } catch (InstantiationException exception) {
                throw new InstantiationError(exception.getMessage());
            }

            // ハッシュテーブル初期化
            this.hashTable = new int[this.hashMethod.tableSize()];
            Arrays.fill(this.hashTable, -1);

            // 連結リスト初期化
            this.prev = new int[this.dictionarySize];
            Arrays.fill(this.prev, -1);

            this.tooBigFlag = new char[this.hashMethod.tableSize() >> 4];

        } else {
            throw new IllegalArgumentException("searchLimitCount must be 1 or more.");
        }
    }

    /**
     * position から始まるデータパタンを
     * ハッシュと連結リストから成る検索機構に登録する。
     *
     * @param position TextBuffer内のデータパタンの開始位置
     */
    @Override
    public void put(int position) {
        int hash = this.hashMethod.hash(position);
        this.prev[position & (this.dictionarySize - 1)] = this.hashTable[hash];
        this.hashTable[hash] = position;
    }

    /**
     * ハッシュと連結リストから成る検索機構に登録された
     * データパタンから position から始まるデータパタンと
     * 最長の一致を持つものを検索し、
     * 同時に position から始まるデータパタンを
     * ハッシュと連結リストから成る検索機構に登録する。
     *
     * @param position TextBuffer内のデータパタンの開始位置。
     *
     * @return 一致が見つかった場合は
     *         LzssOutputStream.createSearchReturn
     *         によって生成された一致位置と一致長の情報を持つ値、
     *         一致が見つからなかった場合は
     *         LzssOutputStream.NOMATCH。
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int searchAndPut(int position) {

        int matchlen = this.threshold - 1;
        int matchpos = position;
        int maxmatch;
        int scanlimit = Math.max(this.dictionaryLimit, position - this.dictionarySize);

        // 連結リストの長さが長すぎる場合 offset を使用して
        // 連結リストの短いハッシュ値を使う。
        int poshash = this.hashMethod.hash(position);
        int offhash = poshash;
        int offset = 0;
        while (this.isTooBig(offhash) && offset < this.maxMatch - this.hashMethod.hashRequires()) {
            offset++;
            offhash = this.hashMethod.hash(position + offset);
        }

        // メインループ
        // 最大 offhash と poshash から始まる 2つの連結リストを走査する。
        byte[] buf = this.TextBuffer;
        int max = position + this.maxMatch;
        int s;
        int p;
        int len;
        while (true) {
            int scanpos = this.hashTable[offhash];
            int searchCount = this.searchLimitCount;

            while (scanlimit <= scanpos - offset && 0 < --searchCount) {
                if (buf[scanpos + matchlen - offset] == buf[position + matchlen]) {
                    s = scanpos - offset;
                    p = position;
                    while (buf[s] == buf[p]) {
                        s++;
                        p++;
                        if (max <= p)
                            break;
                    }

                    len = p - position;
                    if (matchlen < len) {
                        matchpos = scanpos - offset;
                        matchlen = len;
                        if (max <= p)
                            break;
                    }
                }
                scanpos = this.prev[scanpos & (this.dictionarySize - 1)];
            }

            if (searchCount <= 0) {
                this.setTooBigFlag(offhash);
            } else if (scanpos < scanlimit) {
                this.clearTooBigFlag(offhash);
            }

            if (0 < offset && matchlen < this.hashMethod.hashRequires() + offset) {
                offset = 0;
                maxmatch = this.hashMethod.hashRequires() + offset - 1;
                max = position + maxmatch;
                offhash = poshash;
            } else {
                break;
            }
        }

        // ハッシュと連結リストを使用した検索機構に
        // position から始まるデータパタンを登録する。
        this.prev[position & (this.dictionarySize - 1)] = this.hashTable[poshash];
        this.hashTable[poshash] = position;

        // 最長一致を呼び出し元に返す。
        if (this.threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * ハッシュと連結リストを使用した検索機構に登録された
     * データパタンを検索し position から始まるデータパタンと
     * 最長の一致を持つものを得る。
     *
     * @param position TextBuffer内のデータパタンの開始位置。
     * @param lastPutPos 最後に登録したデータパタンの開始位置。
     *
     * @return 一致が見つかった場合は
     *         LzssOutputStream.createSearchReturn
     *         によって生成された一致位置と一致長の情報を持つ値、
     *         一致が見つからなかった場合は
     *         LzssOutputStream.NOMATCH。
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int search(int position, int lastPutPos) {

        // ハッシュと連結リストによる検索機構に登録されていない
        // データパタンを単純な逐次検索で検索する。
        int matchlen = this.threshold - 1;
        int matchpos = position;
        int scanpos = position - 1;
        int scanlimit = Math.max(this.dictionaryLimit, lastPutPos);

        byte[] buf = this.TextBuffer;
        int max = Math.min(this.TextBuffer.length, position + this.maxMatch);
        int s;
        int p;
        int len = 0;
        while (scanlimit < scanpos) {
            s = scanpos;
            p = position;
            while (buf[s] == buf[p]) {
                s++;
                p++;
                if (max <= p)
                    break;
            }

            if (matchlen < len) {
                matchpos = scanpos;
                matchlen = len;
            }
            scanpos--;
        }

        // ハッシュと連結リストを使用した検索機構 から検索する。
        if (this.hashMethod.hashRequires() < this.TextBuffer.length - position) {
            int maxmatch;
            scanlimit = Math.max(this.dictionaryLimit, position - this.dictionarySize);

            // 連結リストの長さが長すぎる場合 offset を使用して
            // 連結リストの短いハッシュ値を使う。
            int poshash = this.hashMethod.hash(position);
            int offhash = poshash;
            int offset = 0;
            while (this.isTooBig(offhash) && offset < this.maxMatch - this.hashMethod.hashRequires()) {
                offset++;
                offhash = this.hashMethod.hash(position + offset);
            }

            // メインループ
            // 最大 offhash と poshash から始まる 2つの連結リストを走査する。
            while (true) {
                int searchCount = this.searchLimitCount;
                scanpos = this.hashTable[offhash];

                while (scanlimit <= scanpos - offset && 0 < --searchCount) {
                    if (buf[scanpos + matchlen - offset] == buf[position + matchlen]) {
                        s = scanpos - offset;
                        p = position;
                        while (buf[s] == buf[p]) {
                            s++;
                            p++;
                            if (max <= p)
                                break;
                        }

                        len = p - position;
                        if (matchlen < len) {
                            matchpos = scanpos - offset;
                            matchlen = len;
                            if (max <= p)
                                break;
                        }
                    }
                    scanpos = this.prev[scanpos & (this.dictionarySize - 1)];
                }

                if (searchCount <= 0) {
                    this.setTooBigFlag(offhash);
                } else if (scanpos < scanlimit) {
                    this.clearTooBigFlag(offhash);
                }

                if (0 < offset && matchlen < this.hashMethod.hashRequires() + offset) {
                    offset = 0;
                    maxmatch = this.hashMethod.hashRequires() + offset - 1;
                    max = Math.min(this.TextBuffer.length, position + maxmatch);
                    offhash = poshash;
                } else {
                    break;
                }
            }
        }

        // 最長一致を呼び出し元に返す。
        if (this.threshold <= matchlen) {
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        } else {
            return LzssOutputStream.NOMATCH;
        }
    }

    /**
     * TextBuffer内のpositionまでのデータを
     * 前方へ移動する際、それに応じて SearchMethod内の
     * データも TextBuffer内のデータと矛盾しないように
     * 前方へ移動する処理を行う。
     */
    @Override
    public void slide() {
        this.dictionaryLimit = Math.max(0, this.dictionaryLimit - this.dictionarySize);

        for (int i = 0; i < this.hashTable.length; i++) {
            int pos = this.hashTable[i] - this.dictionarySize;
            this.hashTable[i] = (0 <= pos ? pos : -1);
        }

        for (int i = 0; i < this.prev.length; i++) {
            int pos = this.prev[i] - this.dictionarySize;
            this.prev[i] = (0 <= pos ? pos : -1);
        }
    }

    /**
     * put() で LzssSearchMethodにデータを
     * 登録するときに使用されるデータ量を得る。
     * HashAndChainedListSearch では、
     * 内部で使用している HashMethod の実装が
     * hash() のために必要とするデータ量
     * ( HashMethod.hashRequires() の戻り値 ) を返す。
     *
     * @return 内部で使用している HashMethod の実装が
     *         hash() のために必要とするデータ量
     */
    @Override
    public int putRequires() {
        return this.hashMethod.hashRequires();
    }

    /**
     * より良い LZSS 圧縮のための選択肢を提供する searchAndPut()。
     * 例えば一致長 3, 一致位置 4 と 一致長 4, 一致位置 1024 では
     * 一致長 3, 一致位置 4 + 非圧縮1文字 の方が出力ビット数が
     * 少なくなる事がある。そのような場合に対処するため一致長毎に
     * positionに一番近い一致位置を列挙する。
     *
     * @param position 検索対象のデータパタンの開始位置
     * @param matchposs 一致位置の列挙を格納して返すための配列<br>
     *            matchpos[0] には 一致長が threshold の一致位置が、<br>
     *            matchpos[1] には 一致長が threshold + 1 の一致位置が格納される。<br>
     *            一致が見つからなかった場合には LzssOutputStream.NOMATCH が格納される。
     * @return 一致が見つかった場合は
     *         LzssOutputStream.createSearchReturn で生成された SearchReturn
     *         が返される。<br>
     *         一致が見つからない場合は LzssOutputStream.NOMATCH が返される。
     */
    public int searchAndPut(int position, int[] matchposs) {
        int matchlen = this.threshold - 1;
        int matchpos = position;
        int maxmatch = this.maxMatch;
        int scanlimit = Math.max(this.dictionaryLimit, position - this.dictionarySize);
        int searchCount = this.searchLimitCount;

        Arrays.fill(matchposs, LzssOutputStream.NOMATCH);

        int scanpos = this.hashTable[this.hashMethod.hash(position)];

        while (scanlimit < scanpos && 0 < searchCount--) {
            if (this.TextBuffer[scanpos + matchlen] == this.TextBuffer[position + matchlen]) {
                int len = 0;
                while (this.TextBuffer[scanpos + len] == this.TextBuffer[position + len]) {
                    if (maxmatch <= ++len)
                        break;
                }

                if (matchlen < len) {
                    int i = matchlen + 1 - this.threshold;
                    int end = Math.min(len + 1 - this.threshold, matchposs.length);
                    while (i < end)
                        matchposs[i++] = scanpos;

                    matchpos = scanpos;
                    matchlen = len;
                    if (maxmatch <= len)
                        break;
                }
            }
            scanpos = this.prev[scanpos & (this.dictionarySize - 1)];
        }

        this.put(position);

        if (matchpos < position)
            return LzssOutputStream.createSearchReturn(matchlen, matchpos);
        else
            return LzssOutputStream.NOMATCH;
    }

    /**
     * hash の連結リストが閾値を超えているかを得る。
     *
     * @param hash ハッシュ値
     * @return 連結リストの長さが閾値を超えているなら true
     *         超えていなければ false
     */
    private boolean isTooBig(int hash) {
        return 0 != (this.tooBigFlag[hash >> 4] & (1 << (hash & 0x0F)));
    }

    /**
     * hash の連結リストが閾値を超えた事を示す
     * フラグをセットする。
     *
     * @param hash too big フラグをセットするハッシュ値
     */
    private void setTooBigFlag(int hash) {
        this.tooBigFlag[hash >> 4] |= 1 << (hash & 0x0F);
    }

    /**
     * hash の連結リストが閾値を超えている事を示す
     * フラグをクリアする。
     *
     * @param hash too big フラグをクリアするハッシュ値
     */
    private void clearTooBigFlag(int hash) {
        this.tooBigFlag[hash >> 4] &= ~(1 << (hash & 0x0F));
    }
}
