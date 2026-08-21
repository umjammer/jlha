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
 * A utility class that creates new instances via {@code createInstance()}
 * from a class name and an array of {@link Object}s as arguments.
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
     * Default constructor.
     * Cannot be used.
     */
    private Factory() {
    }

    /**
     * Creates an instance of the class indicated by {@code classname}.
     * A constructor matching the types of {@code args} is used.
     *
     * @param classname class name
     * @param args      array of arguments
     * @return the created instance, or {@code null} if no constructor matching the type information of {@code args} exists
     * @throws InvocationTargetException if an exception occurs in the constructor
     * @throws InstantiationException    if an attempt is made to get an instance of an {@code abstract} class
     * @throws ClassNotFoundException    if the class indicated by {@code classname} does not exist
     * @throws NoSuchMethodException     if no constructor matching {@code args} is found
     */
    public static Object createInstance(String classname, Object... args) throws InvocationTargetException,
            InstantiationException,
            ClassNotFoundException,
            NoSuchMethodException {
        return Factory.createInstance(Class.forName(classname), args);
    }

    /**
     * Creates an instance of the class indicated by {@code type}.
     * A constructor matching the types of {@code args} is used.
     *
     * @param type class
     * @param args array of arguments
     * @return the created instance, or {@code null} if no constructor matching the type information of {@code args} exists
     * @throws InvocationTargetException if an exception occurs in the constructor
     * @throws InstantiationException    if an attempt is made to get an instance of an {@code abstract} class
     * @throws NoSuchMethodException     if no constructor matching {@code args} is found
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
     * Obtains a {@code public} constructor of the class indicated by {@code classname}
     * that can accept {@code args} as arguments when parsed with {@link Type#parse(Class, Object)}.
     *
     * @param classname class name. Obtains a constructor to create an instance of this type.
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, constructors other than the intended one might be found.
     * @return a constructor that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Constructor<?> getConstructor(String classname, Object... args) throws ClassNotFoundException {
        return Factory.getConstructor(Class.forName(classname), args);
    }

    /**
     * Obtains a {@code public} constructor of {@code type}
     * that can accept {@code args} as arguments when parsed with {@link Type#parse(Class, Object)}.
     *
     * @param type type information. Obtains a constructor to create an instance of this type.
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, constructors other than the intended one might be found.
     * @return a constructor that can take {@code args} as arguments, or {@code null} if not found.
     */
    public static Constructor<?> getConstructor(Class<?> type, Object... args) {
        return Factory.getConstructor(type, false, args);
    }

    /**
     * Obtains a constructor of the class indicated by {@code classname}
     * that can accept {@code args} as arguments when parsed with {@link Type#parse(Class, Object)}.
     *
     * @param classname class name. Obtains a constructor to create an instance of this type.
     * @param all       {@code false} to search only {@code public} constructors;
     *                  {@code true} to search all constructors (public, protected, private, package-private).
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, constructors other than the intended one might be found.
     * @return a constructor that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Constructor<?> getConstructor(String classname, boolean all, Object... args) throws ClassNotFoundException {
        return Factory.getConstructor(Class.forName(classname), all, args);
    }

    /**
     * Obtains a constructor of {@code type}
     * that can accept {@code args} as arguments when parsed with {@link Type#parse(Class, Object)}.
     *
     * @param type type information. Obtains a constructor to create an instance of this type.
     * @param all  {@code false} to search only {@code public} constructors;
     *             {@code true} to search all constructors (public, protected, private, package-private).
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, constructors other than the intended one might be found.
     * @return a constructor that can take {@code args} as arguments, or {@code null} if not found.
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
     * Obtains a {@code public} constructor of the class indicated by {@code classname}
     * that can accept {@code args} as arguments directly as is.
     *
     * @param classname class name. Obtains a constructor to create an instance of this type.
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, constructors other than the intended one might be found.
     * @return a constructor that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Constructor<?> getMatchFullConstructor(String classname, Object... args) throws ClassNotFoundException {
        return Factory.getMatchFullConstructor(Class.forName(classname), args);
    }

    /**
     * Obtains a {@code public} constructor of {@code type}
     * that can accept {@code args} as arguments directly as is.
     *
     * @param type type information. Obtains a constructor to create an instance of this type.
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, constructors other than the intended one might be found.
     * @return a constructor that can take {@code args} as arguments, or {@code null} if not found.
     */
    public static Constructor<?> getMatchFullConstructor(Class<?> type, Object... args) {
        return Factory.getMatchFullConstructor(type, false, args);
    }

    /**
     * Obtains a constructor of the class indicated by {@code classname}
     * that can accept {@code args} as arguments directly as is.
     *
     * @param classname class name. Obtains a constructor to create an instance of this type.
     * @param all       {@code false} to search only {@code public} constructors;
     *                  {@code true} to search all constructors (public, protected, private, package-private).
     * @param args      array of arguments. May include {@code null}, but if {@code null} is used,
     *                  since any subclass of {@link Object} will match, constructors other than the intended one might be found.
     * @return a constructor that can take {@code args} as arguments, or {@code null} if not found.
     * @throws ClassNotFoundException if the class indicated by {@code classname} does not exist
     */
    public static Constructor<?> getMatchFullConstructor(String classname,
                                                         boolean all, Object... args) throws ClassNotFoundException {
        return Factory.getMatchFullConstructor(Class.forName(classname), all, args);
    }

    /**
     * Obtains a constructor of {@code type}
     * that can accept {@code args} as arguments directly as is.
     *
     * @param type type information. Obtains a constructor to create an instance of this type.
     * @param all  {@code false} to search only {@code public} constructors;
     *             {@code true} to search all constructors (public, protected, private, package-private).
     * @param args array of arguments. May include {@code null}, but if {@code null} is used,
     *             since any subclass of {@link Object} will match, constructors other than the intended one might be found.
     * @return a constructor that can take {@code args} as arguments, or {@code null} if not found.
     */
    public static Constructor<?> getMatchFullConstructor(Class<?> type, boolean all, Object... args) {
        Constructor<?>[] constructors = all ? type.getDeclaredConstructors() : type.getConstructors();

        for (Constructor<?> constructor : constructors)
            if (Type.matchFullAll(constructor.getParameterTypes(), args))
                return constructor;

        return null;
    }
}
