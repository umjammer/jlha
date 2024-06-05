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
 * ハッシュと二分木を使った LzssSearchMethod の実装。<br>
 *
 * <pre>
 * データ圧縮ハンドブック[改定第二版]
 *        M.ネルソン/J.-L.ゲィリー 著
 *                萩原剛志・山口英 訳
 *                  ISBN4-8101-8605-9
 *                             5728円(税抜き,当方の購入当時の価格)
 * </pre>
 *
 * を参考にした。<br>
 * 二分木では、最長一致を見つけることはできるが、
 * 最も近い一致を見つけられるとは限らないため、
 * LZSSで 一致位置が近い場所に偏る事を
 * 利用するような -lh5- のような圧縮法では、
 * 圧縮率はいくらか低下する。
 *
 * <pre>
 * -- revision history --
 * $Log: HashAndBinaryTreeSearch.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     LzssSearchMethod のインタフェイス変更にあわせてインタフェイス変更
 * [maintenance]
 *     ソース整備
 *     タブ廃止
 *     ライセンス文の修正
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class HashAndBinaryTreeSearch implements LzssSearchMethod {

    /**
     * 使用されていない事を示す値。
     * parent[node] に UNUSED がある場合は node は未使用のnodeである。
     * small[node], large[node] に UNUSED がある場合は
     * node がそちら側の子ノードを持たない無い事を示す。
     */
    private static final int UNUSED = -1;

    /**
     * 二分木の根を示す値。
     * parent[node] に ROOT_NODE がある場合は node は二分木の根である。
     */
    private static final int ROOT_NODE = -2;

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
    private final byte[] textBuffer;

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
     * 添字はハッシュ値、内容は個々のハッシュ値を持つ
     * 二分木の根のデータパタンの開始位置。
     */
    private final int[] hashTable;

    /**
     * 親のデータパタンの開始位置を示す。
     * 添え字はノード番号、内容は親ノードのデータパタンの開始位置
     */
    private final int[] parent;

    /**
     * 小さい子のデータパタンの開始位置を示す。
     * 添え字はノード番号、内容は小さい子ノードデータパタンの開始位置
     */
    private final int[] small;

    /**
     * 大きい子のデータパタンの開始位置を示す。
     * 添え字はノード番号、内容は大きい子ノードデータパタンの開始位置
     */
    private final int[] large;

//    /**
//     * slide 用のバッファ
//     */
//     private int[] dummy;

    /**
     * ハッシュと二分木を使用した検索機構を構築する。<br>
     * ハッシュ関数はデフォルトのものを使用する。
     *
     * @param DictionarySize 辞書サイズ
     * @param MaxMatch 最長一致長
     * @param Threshold 圧縮、非圧縮の閾値
     * @param TextBuffer LZSS圧縮を施すためのバッファ
     */
    public HashAndBinaryTreeSearch(int DictionarySize, int MaxMatch, int Threshold, byte[] TextBuffer) {

        this(DictionarySize, MaxMatch, Threshold, TextBuffer, HashShort.class.getName());
    }

    /**
     * ハッシュと二分木を使用した LzssSearchMethod を構築する。
     *
     * @param dictionarySize 辞書サイズ
     * @param maxMatch 最長一致長
     * @param threshold 圧縮、非圧縮の閾値
     * @param textBuffer LZSS圧縮を施すためのバッファ
     * @param hashMethodClassName Hash関数を提供するクラス名
     *
     * @exception NoClassDefFoundError hashMethodClassName で与えられたクラスが見つからない場合。
     * @exception InstantiationError hashMethodClassName で与えられたクラスが
     *                abstract class であるためインスタンスを生成できない場合。
     * @exception NoSuchMethodError hashMethodClassName で与えられたクラスがコンストラクタ HashMethod( byte[] )
     *                を持たない場合
     */
    public HashAndBinaryTreeSearch(int dictionarySize,
            int maxMatch,
            int threshold,
            byte[] textBuffer,
            String hashMethodClassName) {

        this.dictionarySize = dictionarySize;
        this.maxMatch = maxMatch;
        this.threshold = threshold;
        this.textBuffer = textBuffer;
        this.dictionaryLimit = this.dictionarySize;

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

        // ハッシュテーブルの初期化
        this.hashTable = new int[this.hashMethod.tableSize()];
        Arrays.fill(this.hashTable, HashAndBinaryTreeSearch.UNUSED);

        // 二分木の初期化
        this.parent = new int[dictionarySize];
        this.large = new int[dictionarySize];
        this.small = new int[dictionarySize];
        Arrays.fill(this.parent, HashAndBinaryTreeSearch.UNUSED);
    }

    /**
     * position から始まるデータパタンを
     * ハッシュと二分木を使用した検索機構に登録する。
     *
     * @param position TextBuffer内のデータパタンの開始位置
     */
    @Override
    public void put(int position) {

        // 二分木から最も古いデータパタンを削除
        this.deleteNode(position - this.dictionarySize);

        // 二分木から position を挿入する位置を検索
        int hash = this.hashMethod.hash(position);
        int parentpos = this.hashTable[hash];
        int scanpos = this.hashTable[hash];

        byte[] buf = this.textBuffer;
        int max = position + this.maxMatch;
        int p = 0;
        int s;
        while (scanpos != HashAndBinaryTreeSearch.UNUSED) {

            s = scanpos;
            p = position;
            while (buf[s] == buf[p]) {
                s++;
                p++;
                if (max <= p) {
                    // 完全一致を発見
                    this.replaceNode(scanpos, position);
                    return;
                }
            }

            parentpos = scanpos;
            scanpos = (buf[s] < buf[p] ? this.large[scanpos & (this.dictionarySize - 1)]
                                       : this.small[scanpos & (this.dictionarySize - 1)]);
        }

        // position から始まるデータパタンを 二分木に登録
        if (this.hashTable[hash] != HashAndBinaryTreeSearch.UNUSED) {
            this.addNode(parentpos, position, p - position);
        } else {
            this.hashTable[hash] = position;
            int node = position & (this.dictionarySize - 1);
            this.parent[node] = HashAndBinaryTreeSearch.ROOT_NODE;
            this.small[node] = HashAndBinaryTreeSearch.UNUSED;
            this.large[node] = HashAndBinaryTreeSearch.UNUSED;
        }
    }

    /**
     * ハッシュと二分木を使用した検索機構に登録された
     * データパタンから{@code position}から始まるデータパタンと最長の一致を持つものを検索し、
     * 同時に{@code position}から始まるデータパタンをハッシュと二分木を使用した検索機構に登録する。
     *
     * @param position TextBuffer内のデータパタンの開始位置。
     * @return 一致が見つかった場合は{@link LzssOutputStream#createSearchReturn}
     *         によって生成された一致位置と一致長の情報を持つ値、一致が見つからなかった場合は
     *         {@link LzssOutputStream#NOMATCH}。
     * @see LzssOutputStream#createSearchReturn(int,int)
     * @see LzssOutputStream#NOMATCH
     */
    @Override
    public int searchAndPut(int position) {

        // 二分木から最も古いデータパタンを削除
        this.deleteNode(position - this.dictionarySize);

        // 二分木から最長一致を検索
        int hash = this.hashMethod.hash(position);
        int matchlen = -1;
        int matchpos = this.hashTable[hash];
        int parentpos = this.hashTable[hash];
        int scanpos = this.hashTable[hash];

        byte[] buf = this.textBuffer;
        int max = position + this.maxMatch;
        int p;
        int s;
        int len = 0;
        while (scanpos != HashAndBinaryTreeSearch.UNUSED) {
            s = scanpos;
            p = position;
            while (buf[s] == buf[p]) {
                s++;
                p++;
                if (max <= p) {
                    // 完全一致を発見
                    this.replaceNode(matchpos, position);
                    return LzssOutputStream.createSearchReturn(matchlen, matchpos);
                }
            }

            len = p - position;
            if (matchlen < len) {
                matchpos = scanpos;
                matchlen = len;
            } else if (matchlen == len && matchpos < scanpos) {
                matchpos = scanpos;
            }

            parentpos = scanpos;
            scanpos = (buf[s] < buf[p] ? this.large[scanpos & (this.dictionarySize - 1)]
                                       : this.small[scanpos & (this.dictionarySize - 1)]);
        }

        // position から始まるデータパタンを 二分木に登録
        if (this.hashTable[hash] != HashAndBinaryTreeSearch.UNUSED) {
            this.addNode(parentpos, position, len);
        } else {
            this.hashTable[hash] = position;
            int node = position & (this.dictionarySize - 1);
            this.parent[node] = HashAndBinaryTreeSearch.ROOT_NODE;
            this.small[node] = HashAndBinaryTreeSearch.UNUSED;
            this.large[node] = HashAndBinaryTreeSearch.UNUSED;
        }

        // メソッドの先頭で削除された
        // 最も遠いデータパタンと比較
        scanpos = position - this.dictionarySize;
        if (this.dictionaryLimit <= scanpos) {
            len = 0;
            while (this.textBuffer[scanpos + len] == this.textBuffer[position + len]) {
                if (this.maxMatch <= ++len)
                    break;
            }

            if (matchlen < len) {
                matchpos = scanpos;
                matchlen = len;
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
     * ハッシュと二分木を使用した検索機構に登録されたデータパタンを検索し
     * position から始まるデータパタンと最長の一致を持つものを得る。<br>
     * textBuffer.length &lt position + maxMatch となるような position では、
     * 二分木を完全に走査しないため最長一致を得られるとは限らない。
     *
     * @param position TextBuffer内のデータパタンの開始位置。
     * @param lastPutPos 最後に登録したデータパタンの開始位置。
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

        // 二分木に登録されていないデータパタンを
        // 単純な逐次検索で検索する。
        int matchlen = this.threshold - 1;
        int matchpos = position;
        int scanpos = position - 1;
        int scanlimit = Math.max(this.dictionaryLimit, lastPutPos);

        byte[] buf = this.textBuffer;
        int max = Math.min(this.textBuffer.length, position + this.maxMatch);
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

        // 二分木を探索
        if (this.hashMethod.hashRequires() <= this.textBuffer.length - position) {
            int hash = this.hashMethod.hash(position);
            scanpos = this.hashTable[hash];
            scanlimit = Math.max(this.dictionaryLimit, position - this.dictionarySize);
            while (scanpos != HashAndBinaryTreeSearch.UNUSED) {
                s = scanpos;
                p = position;
                while (buf[s] == buf[p]) {
                    s++;
                    p++;
                    if (max <= p)
                        break;
                }

                if (p < max) {
                    len = p - position;
                    if (scanlimit <= scanpos) {
                        if (matchlen < len) {
                            matchpos = scanpos;
                            matchlen = len;
                        } else if (matchlen == len && matchpos < scanpos) {
                            matchpos = scanpos;
                        }
                    }
                    scanpos = (buf[s] < buf[p] ? this.large[scanpos & (this.dictionarySize - 1)]
                                               : this.small[scanpos & (this.dictionarySize - 1)]);
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
     * TextBuffer内の position までのデータを前方へ移動する際、
     * それに応じて ハッシュと二分木を使用した検索機構を構成するデータも
     * TextBuffer内のデータと矛盾しないように前方へ移動する処理を行う。
     */
    @Override
    public void slide() {
        this.dictionaryLimit = Math.max(0, this.dictionaryLimit - this.dictionarySize);

        this.slideTree(this.hashTable);
        this.slideTree(this.parent);
        this.slideTree(this.small);
        this.slideTree(this.large);
    }

    /**
     * put() または searchAndPut() を使用して
     * データパタンを二分木に登録する際に
     * 必要とするデータ量を得る。<br>
     * 二分木は登録の際にデータパタンを構成する
     * 全て(MaxMatchバイト)のデータを必要とする。
     *
     * @return コンストラクタで与えた maxMatch
     */
    @Override
    public int putRequires() {
        return this.maxMatch;
    }

    /**
     * parentpos のデータパタンの子として
     * position から始まるデータパタンを二分木に登録する。<br>
     * parentpos と position のデータパタンは len バイト一致する。
     * position の位置のノードはあらかじめ deleteNode 等で
     * UNUSED の状態にしておくこと。
     *
     * @param parentpos 親のデータパタンのTextBuffer内の開始位置
     * @param position 新規追加するデータパタンのTextBuffer内の開始位置
     * @param len 親のデータパタンと新規追加するデータパタンの一致長
     */
    private void addNode(int parentpos, int position, int len) {
        int parentnode = parentpos & (this.dictionarySize - 1);
        int node = position & (this.dictionarySize - 1);

        if (this.textBuffer[parentpos + len] < this.textBuffer[position + len]) {
            this.large[parentnode] = position;
        } else {
            this.small[parentnode] = position;
        }
        this.parent[node] = parentpos;
        this.small[node] = HashAndBinaryTreeSearch.UNUSED;
        this.large[node] = HashAndBinaryTreeSearch.UNUSED;
    }

    /**
     * position から始まるデータパタンを二分木から削除する。
     *
     * @param position 削除するデータパタンの開始位置
     */
    private void deleteNode(int position) {
        int node = position & (this.dictionarySize - 1);

        if (this.parent[node] != HashAndBinaryTreeSearch.UNUSED) {
            if (this.small[node] == HashAndBinaryTreeSearch.UNUSED && this.large[node] == HashAndBinaryTreeSearch.UNUSED) {
                this.contractNode(position, HashAndBinaryTreeSearch.UNUSED);
            } else if (this.small[node] == HashAndBinaryTreeSearch.UNUSED) {
                this.contractNode(position, this.large[node]);
            } else if (this.large[node] == HashAndBinaryTreeSearch.UNUSED) {
                this.contractNode(position, this.small[node]);
            } else {
                int replace = this.findNext(position);
                this.deleteNode(replace);
                this.replaceNode(position, replace);
            }
        }
    }

    /**
     * 子に newpos しか持たない oldpos を, newpos で置き換える。
     * oldpos は二分木から削除される。
     *
     * @param oldpos 削除するデータパタンの開始位置
     * @param newpos oldposに置き換わるデータパタンの開始位置
     */
    private void contractNode(int oldpos, int newpos) {
        int oldnode = oldpos & (this.dictionarySize - 1);
        int newnode = newpos & (this.dictionarySize - 1);
        int parentpos = this.parent[oldnode];
        int parentnode = parentpos & (this.dictionarySize - 1);

        if (parentpos != HashAndBinaryTreeSearch.ROOT_NODE) {
            if (oldpos == this.small[parentnode]) {
                this.small[parentnode] = newpos;
            } else {
                this.large[parentnode] = newpos;
            }
        } else {
            this.hashTable[this.hashMethod.hash(oldpos)] = newpos;
        }

        if (newpos != HashAndBinaryTreeSearch.UNUSED) {
            this.parent[newnode] = parentpos;
        }

        this.parent[oldnode] = HashAndBinaryTreeSearch.UNUSED;
    }

    /**
     * oldpos を二分木に含まれない新しいデータパタン newpos で置き換える。
     * newpos が二分木に含まれているような場合には、
     * いったんdeleteNode(newpos) するなどして、
     * 二分木から外す必要がある。
     * oldpos は二分木から削除される。
     *
     * @param oldpos 削除するデータパタンの開始位置
     * @param newpos oldposに置き換わるデータパタンの開始位置
     */
    private void replaceNode(int oldpos, int newpos) {
        int oldnode = oldpos & (this.dictionarySize - 1);
        int newnode = newpos & (this.dictionarySize - 1);
        int parentpos = this.parent[oldnode];
        int parentnode = parentpos & (this.dictionarySize - 1);

        if (parentpos != HashAndBinaryTreeSearch.ROOT_NODE) {
            if (oldpos == this.small[parentnode]) {
                this.small[parentnode] = newpos;
            } else {
                this.large[parentnode] = newpos;
            }
        } else {
            this.hashTable[this.hashMethod.hash(oldpos)] = newpos;
        }

        this.parent[newnode] = parentpos;
        this.small[newnode] = this.small[oldnode];
        this.large[newnode] = this.large[oldnode];
        if (this.small[newnode] != HashAndBinaryTreeSearch.UNUSED) {
            this.parent[this.small[newnode] & (this.dictionarySize - 1)] = newpos;
        }
        if (this.large[newnode] != HashAndBinaryTreeSearch.UNUSED) {
            this.parent[this.large[newnode] & (this.dictionarySize - 1)] = newpos;
        }

        this.parent[oldnode] = HashAndBinaryTreeSearch.UNUSED;
        this.large[oldnode] = HashAndBinaryTreeSearch.UNUSED;
        this.small[oldnode] = HashAndBinaryTreeSearch.UNUSED;
    }

    /**
     * deleteNode( position ) したときに、
     * small と large の両方の葉が見つかった場合、
     * position のから始まるデータパタンと
     * 置き換えるべき データパタンの開始位置を探し出す。
     *
     * @param position 置き換えられるデータパタンの開始位置
     *
     * @return position のから始まるデータパタンと
     *         置き換えるべき データパタンの開始位置
     */
    private int findNext(int position) {
        int node = position & (this.dictionarySize - 1);

        position = this.small[node];
        node = position & (this.dictionarySize - 1);
        while (HashAndBinaryTreeSearch.UNUSED != this.large[node]) {
            position = this.large[node];
            node = position & (this.dictionarySize - 1);
        }

        return position;
    }

    /**
     * slide() 時に、二分木の各要素を移動させるために使用する。
     *
     * @param array 走査する配列
     */
    private void slideTree(int[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = (0 <= array[i] ? array[i] - this.dictionarySize : array[i]);
        }
    }
}
