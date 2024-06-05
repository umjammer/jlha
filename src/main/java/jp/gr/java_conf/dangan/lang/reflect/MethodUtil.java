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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * メソッドに関するユーティリティクラス。
 *
 * <pre>
 * -- revision history --
 * $Log: MethodUtil.java,v $
 * Revision 1.0  2002/10/01 00:00:00  dangan
 * first edition
 * add to version control
 *
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class MethodUtil {

    /**
     * デフォルトコンストラクタ。
     * 使用不可。
     */
    private MethodUtil() {
    }

    /**
     * インスタンスobjの nameという名前の
     * メソッドをargsを引数として実行する。
     *
     * @param obj  インスタンス
     * @param name メソッド名
     * @param args 引数の配列
     * @return 戻り値
     * @throws InvocationTargetException コンストラクタで例外が発生した場合
     * @throws NoSuchMethodException     args を引数に取れる name という名前のインスタンスメソッドが見つからなかった場合。
     */
    public static Object invoke(Object obj, String name, Object... args) throws InvocationTargetException,
            NoSuchMethodException {
        Class<?> type = obj.getClass();
        Method method = MethodUtil.getMatchFullInstanceMethod(type, name, args);

        if (method == null) {
            method = MethodUtil.getInstanceMethod(type, name, args);

            if (method != null)
                args = Type.parseAll(method.getParameterTypes(), args);
        }

        if (method != null) {
            try {
                return method.invoke(obj, args);
            } catch (IllegalAccessException exception) {
                throw new IllegalAccessError(exception.toString());
            }
        } else {
            throw new NoSuchMethodException();
        }
    }

    /**
     * classname で示されるクラスの nameという名前の
     * static メソッドをargsを引数として実行する。
     *
     * @param classname クラス名
     * @param name      メソッド名
     * @param args      引数の配列
     * @return 戻り値
     * @throws ClassNotFoundException    classname のクラスが見つからなかった場合
     * @throws InvocationTargetException コンストラクタで例外が発生した場合
     * @throws NoSuchMethodException     args を引数に取れる name という名前の
     *                                   インスタンスメソッドが見つからなかった場合。
     */
    public static Object invokeStatic(String classname, String name, Object... args) throws ClassNotFoundException,
            InvocationTargetException,
            NoSuchMethodException {
        return MethodUtil.invokeStatic(Class.forName(classname), // throw ClassNotFoundException
                name,
                args); // throw InvocationTargetException, NoSuchMethodException
    }

    /**
     * type で示されるクラスの nameという名前の
     * static メソッドをargsを引数として実行する。
     *
     * @param type 型情報
     * @param name メソッド名
     * @param args 引数の配列
     * @return 戻り値
     * @throws InvocationTargetException コンストラクタで例外が発生した場合
     * @throws NoSuchMethodException     args を引数に取れる name という名前のインスタンスメソッドが見つからなかった場合。
     */
    public static Object invokeStatic(Class<?> type, String name, Object... args) throws InvocationTargetException,
            NoSuchMethodException {
        Method method = MethodUtil.getMatchFullStaticMethod(type, name, args);

        if (method == null) {
            method = MethodUtil.getStaticMethod(type, name, args);

            if (method != null)
                args = Type.parseAll(method.getParameterTypes(), args);
        }

        if (method != null) {
            try {
                return method.invoke(null, args);
            } catch (IllegalAccessException exception) {
                throw new IllegalAccessError(exception.toString());
            }
        } else {
            throw new NoSuchMethodException();
        }
    }

    /**
     * classname で示されるクラスの public なインスタンスメソッドのうち、
     * name という名前で args を Type.parse した後
     * 受け入れることができるものを得る。
     *
     * @param classname クラス名。
     * @param name      検索するメソッド名。
     * @param args      引数配列。null を含めても良いが、null を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、
     *                  目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。
     * 見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Method getInstanceMethod(String classname, String name, Object... args) throws ClassNotFoundException {
        return MethodUtil.getInstanceMethod(Class.forName(classname), name, false, args);
    }

    /**
     * type の public なインスタンスメソッドのうち、
     * name という名前で args を Type.parse した後
     * 受け入れることができるものを得る。
     *
     * @param type 型情報。
     * @param name 検索するメソッド名。
     * @param args 引数配列。null を含めても良いが、null を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。
     * 見つからなければ null。
     */
    public static Method getInstanceMethod(Class<?> type, String name, Object... args) {
        return MethodUtil.getInstanceMethod(type, name, false, args);
    }

    /**
     * classname で示されるクラスの インスタンスメソッドのうち、
     * name という名前で args を Type.parse した後
     * 受け入れることができるものを得る。
     *
     * @param classname クラス名。
     * @param name      検索するメソッド名。
     * @param all       public のメソッドのみを検索するなら false。
     *                  public, protected, private, パッケージプライベートの全てのメソッドを検索するなら true。
     * @param args      引数配列。null を含めても良いが、null を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。
     * 見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Method getInstanceMethod(String classname,
                                           String name,
                                           boolean all, Object... args) throws ClassNotFoundException {
        return MethodUtil.getInstanceMethod(Class.forName(classname), name, all, args);
    }

    /**
     * type の インスタンスメソッドのうち、name という名前で args を
     * Type.parse した後 受け入れることができるものを得る。
     *
     * @param type 型情報。
     * @param name 検索するメソッド名。
     * @param all  public のメソッドのみを検索するなら false。
     *             public, protected, private, パッケージプライベートの全てのメソッドを検索するなら true。
     * @param args 引数配列。null を含めても良いが、null を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。
     * 見つからなければ null。
     */
    public static Method getInstanceMethod(Class<?> type, String name, boolean all, Object... args) {
        Method[] methods = all ? type.getDeclaredMethods() : type.getMethods();

        for (Method item : methods)
            if (item.getName().equals(name) && !Modifier.isStatic(item.getModifiers())
                    && Type.matchFullAll(item.getParameterTypes(), args))
                return item;

        for (Method value : methods)
            if (value.getName().equals(name) && !Modifier.isStatic(value.getModifiers())
                    && Type.matchRestrictAll(value.getParameterTypes(), args))
                return value;

        for (Method method : methods)
            if (method.getName().equals(name) && !Modifier.isStatic(method.getModifiers())
                    && Type.matchAll(method.getParameterTypes(), args))
                return method;

        return null;
    }

    /**
     * classname で示されるクラスの public なインスタンスメソッドのうち、
     * name という名前で args を 直接受け入れることができるものを得る。
     *
     * @param classname クラス名。
     * @param name      検索するメソッド名。
     * @param args      引数配列。null を含めても良いが、null を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Method getMatchFullInstanceMethod(String classname,
                                                    String name,
                                                    Object... args) throws ClassNotFoundException {
        return MethodUtil.getMatchFullInstanceMethod(Class.forName(classname), name, false, args);
    }

    /**
     * type の public なインスタンスメソッドのうち、
     * name という名前で args を直接受け入れることができるものを得る。
     *
     * @param type 型情報。
     * @param name 検索するメソッド名。
     * @param args 引数配列。null を含めても良いが、null を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     */
    public static Method getMatchFullInstanceMethod(Class<?> type, String name, Object... args) {
        return MethodUtil.getMatchFullInstanceMethod(type, name, false, args);
    }

    /**
     * classname で示されるクラスの インスタンスメソッドのうち、
     * name という名前で args を直接受け入れることができるものを得る。
     *
     * @param classname クラス名。
     * @param name      検索するメソッド名。
     * @param args      引数配列。null を含めても良いが、null を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @param all       public のメソッドのみを検索するなら false。
     *                  public, protected, private, パッケージプライベートの全てのメソッドを検索するなら true。
     * @return args を引数に取ることができる nameという名前の メソッド。
     * 見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Method getMatchFullInstanceMethod(String classname,
                                                    String name,
                                                    Object[] args,
                                                    boolean all) throws ClassNotFoundException {
        return MethodUtil.getMatchFullInstanceMethod(Class.forName(classname), name, all, args);
    }

    /**
     * type の インスタンスメソッドのうち、name という名前で
     * args を直接受け入れることができるものを得る。
     *
     * @param type 型情報。
     * @param name 検索するメソッド名。
     * @param all  public のメソッドのみを検索するなら false。
     *             public, protected, private, パッケージプライベートの全てのメソッドを検索するなら true。
     * @param args 引数配列。null を含めても良いが、null を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     */
    public static Method getMatchFullInstanceMethod(Class<?> type, String name, boolean all, Object... args) {
        Method[] methods = all ? type.getDeclaredMethods() : type.getMethods();

        for (Method method : methods)
            if (method.getName().equals(name) && !Modifier.isStatic(method.getModifiers())
                    && Type.matchFullAll(method.getParameterTypes(), args))
                return method;

        return null;
    }

    /**
     * classname で示されるクラスの public static メソッドのうち、
     * name という名前で args を Type.parse した後
     * 受け入れることができるものを得る。
     *
     * @param classname クラス名。
     * @param name      検索するメソッド名。
     * @param args      引数配列。null を含めても良いが、null を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Method getStaticMethod(String classname, String name, Object... args) throws ClassNotFoundException {
        return MethodUtil.getStaticMethod(Class.forName(classname), name, false, args);
    }

    /**
     * type の public staticなメソッドのうち、
     * name という名前で args を Type.parse した後
     * 受け入れることができるものを得る。
     *
     * @param type 型情報。
     * @param name 検索するメソッド名。
     * @param args 引数配列。null を含めても良いが、null を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     */
    public static Method getStaticMethod(Class<?> type, String name, Object... args) {
        return MethodUtil.getStaticMethod(type, name, false, args);
    }

    /**
     * classname で示されるクラスの static メソッドのうち、
     * name という名前で args を Type.parse した後
     * 受け入れることができるものを得る。
     *
     * @param classname クラス名。
     * @param name      検索するメソッド名。
     * @param all       public のメソッドのみを検索するなら false。public, protected, private, パッケージプライベートの
     *                  全てのメソッドを検索するなら true。
     * @param args      引数配列。null を含めても良いが、null を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Method getStaticMethod(String classname,
                                         String name,
                                         boolean all, Object... args) throws ClassNotFoundException {
        return MethodUtil.getStaticMethod(Class.forName(classname), name, all, args);
    }

    /**
     * type の static メソッドのうち、name という名前で args を
     * Type.parse した後 受け入れることができるものを得る。
     *
     * @param type 型情報。
     * @param name 検索するメソッド名。
     * @param all  public のメソッドのみを検索するなら false。
     *             public, protected, private, パッケージプライベートの全てのメソッドを検索するなら true。
     * @param args 引数配列。null を含めても良いが、null を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     */
    public static Method getStaticMethod(Class<?> type, String name, boolean all, Object... args) {
        Method[] methods = all ? type.getDeclaredMethods() : type.getMethods();

        for (Method item : methods)
            if (item.getName().equals(name) && Modifier.isStatic(item.getModifiers())
                    && Type.matchFullAll(item.getParameterTypes(), args))
                return item;

        for (Method value : methods)
            if (value.getName().equals(name) && Modifier.isStatic(value.getModifiers())
                    && Type.matchRestrictAll(value.getParameterTypes(), args))
                return value;

        for (Method method : methods)
            if (method.getName().equals(name) && Modifier.isStatic(method.getModifiers())
                    && Type.matchAll(method.getParameterTypes(), args))
                return method;

        return null;
    }

    /**
     * classname で示されるクラスの public static メソッドのうち、
     * name という名前で args を 直接受け入れることができるものを得る。
     *
     * @param classname クラス名。
     * @param name      検索するメソッド名。
     * @param args      引数配列。null を含めても良いが、null を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Method getMatchFullStaticMethod(String classname, String name, Object... args) throws ClassNotFoundException {
        return MethodUtil.getMatchFullStaticMethod(Class.forName(classname), name, false, args);
    }

    /**
     * type の public staticなメソッドのうち、
     * name という名前で args を直接受け入れることができるものを得る。
     *
     * @param type 型情報。
     * @param name 検索するメソッド名。
     * @param args 引数配列。null を含めても良いが、null を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     */
    public static Method getMatchFullStaticMethod(Class<?> type, String name, Object... args) {
        return MethodUtil.getMatchFullStaticMethod(type, name, false, args);
    }

    /**
     * classname で示されるクラスの static メソッドのうち、
     * name という名前で args を直接受け入れることができるものを得る。
     *
     * @param classname クラス名。
     * @param name      検索するメソッド名。
     * @param all       public のメソッドのみを検索するなら false。public, protected, private, パッケージプライベートの
     *                  全てのメソッドを検索するなら true。
     * @param args      引数配列。null を含めても良いが、null を使用した場合は
     *                  Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。
     * 見つからなければ null。
     * @throws ClassNotFoundException classname で示されるクラスが存在しない場合
     */
    public static Method getMatchFullStaticMethod(String classname,
                                                  String name,
                                                  boolean all, Object... args) throws ClassNotFoundException {
        return MethodUtil.getMatchFullStaticMethod(Class.forName(classname), name, all, args);
    }

    /**
     * type の static メソッドのうち、name という名前で
     * args を直接受け入れることができるものを得る。
     *
     * @param type 型情報。
     * @param name 検索するメソッド名。
     * @param all  public のメソッドのみを検索するなら false。public, protected, private, パッケージプライベートの
     *             全てのメソッドを検索するなら true。
     * @param args 引数配列。null を含めても良いが、null を使用した場合は
     *             Object のサブクラスであれば全てマッチしてしまうため、目的のメソッド以外のものが見つかる可能性がある。
     * @return args を引数に取ることができる nameという名前の メソッド。見つからなければ null。
     */
    public static Method getMatchFullStaticMethod(Class<?> type, String name, boolean all, Object... args) {
        Method[] methods = all ? type.getDeclaredMethods() : type.getMethods();

        for (Method method : methods)
            if (method.getName().equals(name) && Modifier.isStatic(method.getModifiers())
                    && Type.matchFullAll(method.getParameterTypes(), args))
                return method;

        return null;
    }
}
