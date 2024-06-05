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

package jp.gr.java_conf.dangan.lang.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * クラス名と 引数になるObject の配列から、
 * createInstance() によって新しいインスタンスを作り出す
 * ユーティリティクラス。
 *
 * <pre>
 * -- revision history --
 * $Log: Factory.java,v $
 * Revision 1.0  2002/10/01 00:00:00  dangan
 * first edition
 * add to version control
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class Factory {

    /**
     * デフォルトコンストラクタ。
     * 使用不可。
     */
    private Factory() {
    }

    /**
     * classname で示されるクラスのインスタンスを生成する。
     * コンストラクタには args の型と一致するものを使用する。
     *
     * @param classname クラス名
     * @param args      引数の配列
     * @return 生成されたインスタンス{@code args}と型情報がマッチするコンストラクタが存在しなかった場合は{@code null}
     * @throws InvocationTargetException コンストラクタで例外が発生した場合
     * @throws InstantiationException    {@code abstract}クラスのインスタンスを得ようとした場合
     * @throws ClassNotFoundException    classname で示されるクラスが存在しない場合
     */
    public static Object createInstance(String classname, Object... args) throws InvocationTargetException,
            InstantiationException,
            ClassNotFoundException,
            NoSuchMethodException {
        return Factory.createInstance(Class.forName(classname), args);
    }

    /**
     * type で示されるクラスのインスタンスを生成する。
     * コンストラクタには args の型と一致するものを使用する。
     *
     * @param type クラス名
     * @param args 引数の配列
     * @return 生成されたインスタンス{@code args}と型情報がマッチするコンストラクタが存在しなかった場合は{@code null}
     * @throws InvocationTargetException コンストラクタで例外が発生した場合
     * @throws InstantiationException    {@code abstract}クラスのインスタンスを得ようとした場合
     */
    public static Object createInstance(Class<?> type, Object... args) throws InvocationTargetException,
            InstantiationException,
            NoSuchMethodException {
        Constructor<?> constructor = Factory.getMatchFullConstructor(type, args);

        if (constructor == null) {
            constructor = Factory.getConstructor(type, args);

            if (constructor != null)
                args = Type.parseAll(constructor.getParameterTypes(), args);
        }

        if (constructor != null) {
            try {
                return constructor.newInstance(args);
            } catch (IllegalAccessException exception) {
                throw new IllegalAccessError(exception.toString());
            }
        } else {
            throw new NoSuchMethodException();
        }
    }

    /**
     * classname で示されるクラスの public なコンストラクタのうち、
     * args を Type.parse した場合、引数として受け入れることが
     * できるものを得る。
     *
     * @param classname クラス名。この型のインスタンスを生成するためのコンストラクタを得る。
     * @param args      引数配列。{@code null}を含めても良いが、{@code null}を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、
     *                  目的のコンストラクタ以外のものが見つかる可能性がある。
     * @return args を引数に取ることができるコンストラクタ。
     * 見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Constructor<?> getConstructor(String classname, Object... args) throws ClassNotFoundException {
        return Factory.getConstructor(Class.forName(classname), args);
    }

    /**
     * type のpublic なコンストラクタのうち、args を
     * Type.parse した場合 引数として受け入れることができるものを得る。
     *
     * @param type 型情報。この型のインスタンスを生成するためのコンストラクタを得る。
     * @param args 引数配列。{@code null}を含めても良いが、{@code null}を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のコンストラクタ以外のものが見つかる可能性がある。
     * @return args を引数に取ることができるコンストラクタ。
     * 見つからなければ null。
     */
    public static Constructor<?> getConstructor(Class<?> type, Object... args) {
        return Factory.getConstructor(type, false, args);
    }

    /**
     * classnameで示されるクラスの コンストラクタのうち、args を
     * Type.parse して 引数として受け入れることができるものを得る。
     *
     * @param classname クラス名。この型のインスタンスを生成するためのコンストラクタを得る。
     * @param all       public のコンストラクタのみを検索するなら false。
     *                  public, protected, private, パッケージプライベートの
     *                  全てのコンストラクタをけんさくするなら true。
     * @param args      引数配列。null を含めても良いが、null を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、
     *                  目的のコンストラクタ以外のものが見つかる可能性がある。
     * @return args を引数に取ることができるコンストラクタ。見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Constructor<?> getConstructor(String classname, boolean all, Object... args) throws ClassNotFoundException {
        return Factory.getConstructor(Class.forName(classname), all, args);
    }

    /**
     * {@code type}のコンストラクタのうち、{@code args}を Type.parse した場合
     * 引数として受け入れることができるものを得る。
     *
     * @param type 型情報。この型のインスタンスを生成するためのコンストラクタを得る。
     * @param all  public のコンストラクタのみを検索するなら false。public, protected, private, パッケージプライベートの
     *             全てのコンストラクタをけんさくするなら true。
     * @param args 引数配列。{@code null}を含めても良いが、{@code null}を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のコンストラクタ以外のものが見つかる可能性がある。
     * @return args を引数に取ることができるコンストラクタ。見つからなければ{@code null}。
     */
    public static Constructor<?> getConstructor(Class<?> type, boolean all, Object... args) {
        Constructor<?>[] constructors = all ? type.getDeclaredConstructors() : type.getConstructors();

        for (Constructor<?> item : constructors)
            if (Type.matchFullAll(item.getParameterTypes(), args))
                return item;

        for (Constructor<?> value : constructors)
            if (Type.matchRestrictAll(value.getParameterTypes(), args))
                return value;

        for (Constructor<?> constructor : constructors)
            if (Type.matchAll(constructor.getParameterTypes(), args))
                return constructor;

        return null;
    }

    /**
     * classname で示されるクラスの public なコンストラクタのうち、
     * args を そのまま引数として受け入れることができるものを得る。
     *
     * @param classname クラス名。この型のインスタンスを生成するためのコンストラクタを得る。
     * @param args      引数配列。{@code null}を含めても良いが、{@code null}を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、目的のコンストラクタ以外のものが見つかる可能性がある。
     * @return args を引数に取ることができるコンストラクタ。見つからなければ{@code null}。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Constructor<?> getMatchFullConstructor(String classname, Object... args) throws ClassNotFoundException {
        return Factory.getMatchFullConstructor(Class.forName(classname), args);
    }

    /**
     * type の public なコンストラクタのうち、args を
     * そのまま引数として受け入れることができるものを得る。
     *
     * @param type 型情報。この型のインスタンスを生成するためのコンストラクタを得る。
     * @param args 引数配列。{@code null}を含めても良いが、{@code null}を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のコンストラクタ以外のものが見つかる可能性がある。
     * @return args を引数に取ることができるコンストラクタ。見つからなければ{@code null}。
     */
    public static Constructor<?> getMatchFullConstructor(Class<?> type, Object... args) {
        return Factory.getMatchFullConstructor(type, false, args);
    }

    /**
     * classname で示されるクラスの コンストラクタのうち、
     * args を そのまま引数として受け入れることができるものを得る。
     *
     * @param classname クラス名。この型のインスタンスを生成するためのコンストラクタを得る。
     * @param args      引数配列。{@code null}を含めても良いが、{@code null}を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、目的のコンストラクタ以外のものが見つかる可能性がある。
     * @return args を引数に取ることができるコンストラクタ。見つからなければ{@code null}。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Constructor<?> getMatchFullConstructor(String classname,
                                                         boolean all, Object... args) throws ClassNotFoundException {
        return Factory.getMatchFullConstructor(Class.forName(classname), all, args);
    }

    /**
     * type のコンストラクタのうち、args を
     * そのまま引数として受け入れることができるものを得る。
     *
     * @param type 型情報。この型のインスタンスを生成するためのコンストラクタを得る。
     * @param all  public のコンストラクタのみを検索するなら false。public, protected, private, パッケージプライベートの
     *             全てのコンストラクタをけんさくするなら true。
     * @param args 引数配列。{@code null}を含めても良いが、{@code null}を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のコンストラクタ以外のものが見つかる可能性がある。
     * @return args を引数に取ることができるコンストラクタ。見つからなければ{@code null}。
     */
    public static Constructor<?> getMatchFullConstructor(Class<?> type, boolean all, Object... args) {
        Constructor<?>[] constructors = all ? type.getDeclaredConstructors() : type.getConstructors();

        for (Constructor<?> constructor : constructors)
            if (Type.matchFullAll(constructor.getParameterTypes(), args))
                return constructor;

        return null;
    }
}
