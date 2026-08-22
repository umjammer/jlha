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
 * A utility class for methods.
 *
 * <pre>
 * -- revision history --
 * $Log: MethodUtil.java,v $
 * Revision 1.0  2002/10/01 00:00:00  dangan
 * first edition
 * add to version control
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class MethodUtil {

    /**
     * Default constructor.
     * Cannot be used.
     */
    private MethodUtil() {
    }

    /**
     * Invokes the method named {@code name} on the instance {@code obj} with {@code args} as arguments.
     *
     * @param obj  instance
     * @param name method name
     * @param args array of arguments
     * @return return value
     * @throws InvocationTargetException if an exception occurs in the method
     * @throws NoSuchMethodException     if an instance method named {@code name} that can accept {@code args} as arguments is not found.
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
     * Invokes the static method named {@code name} of the class indicated by {@code classname} with {@code args} as arguments.
     *
     * @param classname class name
     * @param name      method name
     * @param args      array of arguments
     * @return return value
     * @throws ClassNotFoundException    if the class {@code classname} is not found
     * @throws InvocationTargetException if an exception occurs in the method
     * @throws NoSuchMethodException     if an instance method named {@code name} that can accept {@code args} as arguments is not found.
     */
    public static Object invokeStatic(String classname, String name, Object... args) throws ClassNotFoundException,
            InvocationTargetException,
            NoSuchMethodException {
        return MethodUtil.invokeStatic(Class.forName(classname), // throw ClassNotFoundException
                name,
                args); // throw InvocationTargetException, NoSuchMethodException
    }

    /**
     * Invokes the static method named {@code name} of the class indicated by {@code type} with {@code args} as arguments.
     *
     * @param type type information
     * @param name method name
     * @param args array of arguments
     * @return return value
     * @throws InvocationTargetException if an exception occurs in the method
     * @throws NoSuchMethodException     if an instance method named {@code name} that can accept {@code args} as arguments is not found.
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
     * Obtains a {@code public} instance method of the class indicated by {@code classname}
     * named {@code name} that can accept {@code args} after being parsed with {@link Type#parse(Class, Object)}.
     *
     * @param classname class name.
     * @param name      method name to search for.
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Method getInstanceMethod(String classname, String name, Object... args) throws ClassNotFoundException {
        return MethodUtil.getInstanceMethod(Class.forName(classname), name, false, args);
    }

    /**
     * Obtains a {@code public} instance method of {@code type}
     * named {@code name} that can accept {@code args} after being parsed with {@link Type#parse(Class, Object)}.
     *
     * @param type type information.
     * @param name method name to search for.
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     */
    public static Method getInstanceMethod(Class<?> type, String name, Object... args) {
        return MethodUtil.getInstanceMethod(type, name, false, args);
    }

    /**
     * Obtains an instance method of the class indicated by {@code classname}
     * named {@code name} that can accept {@code args} after being parsed with {@link Type#parse(Class, Object)}.
     *
     * @param classname class name.
     * @param name      method name to search for.
     * @param all       {@code false} to search only {@code public} methods;
     *                  {@code true} to search all methods (public, protected, private, package-private).
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Method getInstanceMethod(String classname,
                                           String name,
                                           boolean all, Object... args) throws ClassNotFoundException {
        return MethodUtil.getInstanceMethod(Class.forName(classname), name, all, args);
    }

    /**
     * Obtains an instance method of {@code type}
     * named {@code name} that can accept {@code args} after being parsed with {@link Type#parse(Class, Object)}.
     *
     * @param type type information.
     * @param name method name to search for.
     * @param all  {@code false} to search only {@code public} methods;
     *             {@code true} to search all methods (public, protected, private, package-private).
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
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
     * Obtains a {@code public} instance method of the class indicated by {@code classname}
     * named {@code name} that can accept {@code args} directly as is.
     *
     * @param classname class name.
     * @param name      method name to search for.
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Method getMatchFullInstanceMethod(String classname,
                                                    String name,
                                                    Object... args) throws ClassNotFoundException {
        return MethodUtil.getMatchFullInstanceMethod(Class.forName(classname), name, false, args);
    }

    /**
     * Obtains a {@code public} instance method of {@code type}
     * named {@code name} that can accept {@code args} directly as is.
     *
     * @param type type information.
     * @param name method name to search for.
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     */
    public static Method getMatchFullInstanceMethod(Class<?> type, String name, Object... args) {
        return MethodUtil.getMatchFullInstanceMethod(type, name, false, args);
    }

    /**
     * Obtains an instance method of the class indicated by {@code classname}
     * named {@code name} that can accept {@code args} directly as is.
     *
     * @param classname class name.
     * @param name      method name to search for.
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @param all       {@code false} to search only {@code public} methods;
     *                  {@code true} to search all methods (public, protected, private, package-private).
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Method getMatchFullInstanceMethod(String classname,
                                                    String name,
                                                    Object[] args,
                                                    boolean all) throws ClassNotFoundException {
        return MethodUtil.getMatchFullInstanceMethod(Class.forName(classname), name, all, args);
    }

    /**
     * Obtains an instance method of {@code type}
     * named {@code name} that can accept {@code args} directly as is.
     *
     * @param type type information.
     * @param name method name to search for.
     * @param all  {@code false} to search only {@code public} methods;
     *             {@code true} to search all methods (public, protected, private, package-private).
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
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
     * Obtains a {@code public static} method of the class indicated by {@code classname}
     * named {@code name} that can accept {@code args} after being parsed with {@link Type#parse(Class, Object)}.
     *
     * @param classname class name.
     * @param name      method name to search for.
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Method getStaticMethod(String classname, String name, Object... args) throws ClassNotFoundException {
        return MethodUtil.getStaticMethod(Class.forName(classname), name, false, args);
    }

    /**
     * Obtains a {@code public static} method of {@code type}
     * named {@code name} that can accept {@code args} after being parsed with {@link Type#parse(Class, Object)}.
     *
     * @param type type information.
     * @param name method name to search for.
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     */
    public static Method getStaticMethod(Class<?> type, String name, Object... args) {
        return MethodUtil.getStaticMethod(type, name, false, args);
    }

    /**
     * Obtains a static method of the class indicated by {@code classname}
     * named {@code name} that can accept {@code args} after being parsed with {@link Type#parse(Class, Object)}.
     *
     * @param classname class name.
     * @param name      method name to search for.
     * @param all       {@code false} to search only {@code public} methods;
     *                  {@code true} to search all methods (public, protected, private, package-private).
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Method getStaticMethod(String classname,
                                         String name,
                                         boolean all, Object... args) throws ClassNotFoundException {
        return MethodUtil.getStaticMethod(Class.forName(classname), name, all, args);
    }

    /**
     * Obtains a static method of {@code type}
     * named {@code name} that can accept {@code args} after being parsed with {@link Type#parse(Class, Object)}.
     *
     * @param type type information.
     * @param name method name to search for.
     * @param all  {@code false} to search only {@code public} methods;
     *             {@code true} to search all methods (public, protected, private, package-private).
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
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
     * Obtains a {@code public static} method of the class indicated by {@code classname}
     * named {@code name} that can accept {@code args} directly as is.
     *
     * @param classname class name.
     * @param name      method name to search for.
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Method getMatchFullStaticMethod(String classname, String name, Object... args) throws ClassNotFoundException {
        return MethodUtil.getMatchFullStaticMethod(Class.forName(classname), name, false, args);
    }

    /**
     * Obtains a {@code public static} method of {@code type}
     * named {@code name} that can accept {@code args} directly as is.
     *
     * @param type type information.
     * @param name method name to search for.
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     */
    public static Method getMatchFullStaticMethod(Class<?> type, String name, Object... args) {
        return MethodUtil.getMatchFullStaticMethod(type, name, false, args);
    }

    /**
     * Obtains a static method of the class indicated by {@code classname}
     * named {@code name} that can accept {@code args} directly as is.
     *
     * @param classname class name.
     * @param name      method name to search for.
     * @param all       {@code false} to search only {@code public} methods;
     *                  {@code true} to search all methods (public, protected, private, package-private).
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Method getMatchFullStaticMethod(String classname,
                                                  String name,
                                                  boolean all, Object... args) throws ClassNotFoundException {
        return MethodUtil.getMatchFullStaticMethod(Class.forName(classname), name, all, args);
    }

    /**
     * Obtains a static method of {@code type}
     * named {@code name} that can accept {@code args} directly as is.
     *
     * @param type type information.
     * @param name method name to search for.
     * @param all  {@code false} to search only {@code public} methods;
     *             {@code true} to search all methods (public, protected, private, package-private).
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, methods other than the intended one might be found.
     * @return a method named {@code name} that can take {@code args} as arguments, or {@code null} if not found.
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
