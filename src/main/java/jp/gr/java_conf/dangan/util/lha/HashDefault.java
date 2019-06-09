//start of HashDefault.java
//TEXT_STYLE:CODE=Shift_JIS(Japanese):RET_CODE=CRLF

/**
 * HashDefault.java
 *
 * Copyright (C) 2002  Michel Ishizuka  All rights reserved.
 *
 * 以下の条件に同意するならばソースとバイナリ形式の再配布と使用を
 * 変更の有無にかかわらず許可する。
 *
 * １．ソースコードの再配布において著作権表示と この条件のリスト
 *     および下記の声明文を保持しなくてはならない。
 *
 * ２．バイナリ形式の再配布において著作権表示と この条件のリスト
 *     および下記の声明文を使用説明書もしくは その他の配布物内に
 *     含む資料に記述しなければならない。
 *
 * このソフトウェアは石塚美珠瑠によって無保証で提供され、特定の目
 * 的を達成できるという保証、商品価値が有るという保証にとどまらず、
 * いかなる明示的および暗示的な保証もしない。
 * 石塚美珠瑠は このソフトウェアの使用による直接的、間接的、偶発
 * 的、特殊な、典型的な、あるいは必然的な損害(使用によるデータの
 * 損失、業務の中断や見込まれていた利益の遺失、代替製品もしくは
 * サービスの導入費等が考えられるが、決してそれだけに限定されない
 * 損害)に対して、いかなる事態の原因となったとしても、契約上の責
 * 任や無過失責任を含む いかなる責任があろうとも、たとえそれが不
 * 正行為のためであったとしても、またはそのような損害の可能性が報
 * 告されていたとしても一切の責任を負わないものとする。
 */

package jp.gr.java_conf.dangan.util.lha;

//import classes and interfaces
import jp.gr.java_conf.dangan.util.lha.HashMethod;

//import exceptions


/**
 * 試作プログラム ar940528 や LHa for Unix で使用されているハッシュ関数。<br>
 * gzip で使用されているを参考にしたようだ。<br>
 *
 * <pre>
 * -- revision history --
 * $Log: HashDefault.java,v $
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [change]
 *     HashMethod のインタフェイス変更にあわせてインテフェイス変更。
 *     コンストラクタで引数チェックを削除。
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
public class HashDefault implements HashMethod {

    //------------------------------------------------------------------
    //  class field
    //------------------------------------------------------------------
    //  private static final int HashMask
    //------------------------------------------------------------------
    /**
     * ハッシュ値を生成するためのマスク値
     */
    private static final int HashMask = 0x7FFF;

    //------------------------------------------------------------------
    //  instance field
    //------------------------------------------------------------------
    //  private byte[] TextBuffer
    //------------------------------------------------------------------
    /**
     * LZSS圧縮を施すためのバッファ。
     * 前半は辞書領域、
     * 後半は圧縮を施すためのデータの入ったバッファ。
     * HashMethodの実装内では Hash値の生成のための読み込みにのみ使用する。
     */
    private byte[] TextBuffer;

    /**
     * ar940528 や LHa for Unix で使用されているハッシュ関数を構築する。
     *
     * @param TextBuffer LZSS圧縮用のバッファ。
     *            Hash値生成のため読み込み用に使用する。
     */
    public HashDefault(byte[] TextBuffer) {
        this.TextBuffer = TextBuffer;
    }

    //------------------------------------------------------------------
    //  method of jp.gr.java_conf.dangan.util.lha.HashMethod
    //------------------------------------------------------------------
    //  public int hash( int position )
    //  public int hashRequires()
    //  public int tableSize()
    //------------------------------------------------------------------
    /**
     * ハッシュ関数。<br>
     * コンストラクタで渡された TextBuffer の position からの
     * データパタンの hash値を生成する。
     *
     * @param position データパタンの開始位置
     *
     * @return ハッシュ値
     */
    public int hash(int position) {
        return (((this.TextBuffer[position] << 5) ^ (this.TextBuffer[position + 1] & 0xFF)) << 5
                ^ (this.TextBuffer[position + 2] & 0xFF))
               & HashDefault.HashMask;
    }

    /**
     * ハッシュ関数がハッシュ値を生成するために使用するバイト数を得る。<br>
     * このハッシュ関数は 3バイトのデータから
     * シフトとXORを使用してハッシュ値を生成するため、
     * このメソッドは常に 3 を返す。
     *
     * @return 常に 3
     */
    public int hashRequires() {
        return 3;
    }

    /**
     * ハッシュテーブルのサイズを得る。<br>
     * このハッシュ関数は 0x0000 〜 0x7FFF のハッシュ値を生成するため、
     * このメソッドは常に 0x8000(32768) を返す。
     *
     * @return 常に 0x8000(32768)
     */
    public int tableSize() {
        return 0x8000;
    }

}
//end of HashDefault.java
