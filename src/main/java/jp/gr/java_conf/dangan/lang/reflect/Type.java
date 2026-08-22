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

import java.math.BigInteger;


/**
 * A utility class that handles type information to make reflection features easier to use.
 *
 * <pre>
 * -- revision history --
 * $Log: Type.java,v $
 * Revision 1.0  2002/10/01 00:00:00  dangan
 * first edition
 * add to version control
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class Type {

    /**
     * Default constructor.
     * Cannot be used.
     */
    private Type() {
    }

    /**
     * Checks whether {@code args} matches {@code types} without conversion.
     *
     * @param types array of type information
     * @param args  array of objects to check
     * @return {@code true} if {@code args} matches {@code types};<br>
     * {@code false} otherwise.
     */
    public static boolean matchFullAll(Class<?>[] types, Object[] args) {
        boolean match = (types.length == args.length);

        for (int i = 0; i < types.length; i++)
            match = match && Type.matchFull(types[i], args[i]);

        return match;
    }

    /**
     * Checks whether {@code args} matches {@code types} when conversion via {@link Type#parse(Class, Object)} is applied.
     * Checks more strictly than {@link #matchAll(Class[], Object[])}.
     *
     * @param types array of type information
     * @param args  array of objects to check
     * @return {@code true} if {@code args} matches {@code types};<br>
     * {@code false} otherwise.
     */
    public static boolean matchRestrictAll(Class<?>[] types, Object[] args) {
        boolean match = (types.length == args.length);

        for (int i = 0; i < types.length; i++)
            match = match && Type.matchRestrict(types[i], args[i]);

        return match;
    }

    /**
     * Checks whether {@code args} matches {@code types} when conversion via {@link Type#parse(Class, Object)} is applied.
     *
     * @param types array of type information
     * @param args  array of objects to check
     * @return {@code true} if {@code args} matches {@code types};<br>
     * {@code false} otherwise.
     */
    public static boolean matchAll(Class<?>[] types, Object[] args) {
        boolean match = (types.length == args.length);

        for (int i = 0; i < types.length; i++)
            match = match && Type.match(types[i], args[i]);

        return match;
    }

    /**
     * Checks whether {@code obj} matches {@code type} without conversion.
     *
     * @param type type information
     * @param obj  object to check
     * @return {@code true} if {@code obj} is an instance of {@code type}; {@code false} otherwise.
     */
    public static boolean matchFull(Class<?> type, Object obj) {
        if (type.isInstance(obj)) {
            return true;
        } else if (!type.isPrimitive() && obj == null) {
            return true;
        } else if (type.equals(Boolean.TYPE) && obj instanceof Boolean) {
            return true;
        } else if (type.equals(Byte.TYPE) && obj instanceof Byte) {
            return true;
        } else if (type.equals(Short.TYPE) && obj instanceof Short) {
            return true;
        } else if (type.equals(Character.TYPE) && obj instanceof Character) {
            return true;
        } else if (type.equals(Integer.TYPE) && obj instanceof Integer) {
            return true;
        } else if (type.equals(Long.TYPE) && obj instanceof Long) {
            return true;
        } else if (type.equals(Float.TYPE) && obj instanceof Float) {
            return true;
        } else if (type.equals(Double.TYPE) && obj instanceof Double) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether {@code obj} is an instance of {@code type}.
     * If {@code type} is a numeric primitive type
     * (one of {@code byte}, {@code short}, {@code int}, {@code long}, {@code float}, {@code double})
     * and {@code obj} is an instance of its primitive wrapper type
     * (one of {@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double}),
     * it is determined to be convertible and {@code true} is returned.
     *
     * @param type type information
     * @param obj  object to check
     * @return {@code true} if {@code obj} is an instance of {@code type}; {@code false} otherwise.
     */
    public static boolean matchRestrict(Class<?> type, Object obj) {

        if (Type.matchFull(type, obj)) {
            return true;
        } else if ((type.equals(Byte.TYPE) || type.equals(Short.TYPE) || type.equals(Integer.TYPE) || type.equals(Long.TYPE)
                || type.equals(Float.TYPE) || type.equals(Double.TYPE))
                && (obj instanceof Byte || obj instanceof Short || obj instanceof Integer || obj instanceof Long
                || obj instanceof Float || obj instanceof Double)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether {@code obj} is an instance of {@code type}.
     * Returns {@code true} if {@code obj} is convertible via {@link Type#parse(Class, Object)}.
     *
     * @param type type information
     * @param obj  object to check
     * @return {@code true} if {@code obj} is an instance of {@code type}; {@code false} otherwise.
     */
    public static boolean match(Class<?> type, Object obj) {
        String str = (obj == null ? null : obj.toString());

        if (Type.matchRestrict(type, obj)) {
            return true;
        } else if (type.equals(String.class)) {
            return true;
        } else if (!type.isPrimitive() && "NULL".equalsIgnoreCase(str)) {
            return true;
        } else if ((type.equals(Byte.class) || type.equals(Byte.TYPE) || type.equals(Short.class) || type.equals(Short.TYPE)
                || type.equals(Integer.class) || type.equals(Integer.TYPE) || type.equals(Long.class)
                || type.equals(Long.TYPE) || type.equals(Float.class) || type.equals(Float.TYPE)
                || type.equals(Double.class) || type.equals(Double.TYPE))
                && (obj instanceof Number || (obj != null && Type.isLongString(obj.toString()))
                || (obj != null && Type.isDoubleString(obj.toString())))) {
            return true;
        } else if ((type.equals(Boolean.TYPE) || type.equals(Boolean.class))
                && ("TRUE".equalsIgnoreCase(str) || "FALSE".equalsIgnoreCase(str))) {
            return true;
        } else if ((type.equals(Character.class) || type.equals(Character.TYPE)) && obj instanceof String
                && (str.length() == 1 || Type.isUnicodeEscape(str))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Converts all {@code args} matched by {@link #matchAll(Class[], Object[])}
     * into the types indicated by {@code types}.
     *
     * @param types array of type information to convert to
     * @param args  array of objects to convert
     * @return array of converted objects
     * @throws IllegalArgumentException if {@code args} cannot be converted to {@code types}.
     */
    public static Object[] parseAll(Class<?>[] types, Object[] args) {
        if (types.length == args.length) {
            Object[] objs = new Object[args.length];

            for (int i = 0; i < args.length; i++)
                objs[i] = Type.parse(types[i], args[i]);

            return objs;
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Converts {@code obj} matched by {@link #match(Class, Object)}
     * into the type indicated by {@code type}.
     *
     * @param type type information to convert to
     * @param obj  object to convert
     * @return converted object
     * @throws IllegalArgumentException if attempting to convert an {@code obj} that did not match {@link #match(Class, Object)}.
     */
    public static Object parse(Class<?> type, Object obj) {
        String str = (obj == null ? null : obj.toString());

        if (type.isInstance(obj)) {
            return obj;
        } else if (!type.isPrimitive() && !type.equals(String.class) && (obj == null || "NULL".equalsIgnoreCase(str))) {
            return null;
        } else if (type.equals(String.class)) {
            return str;
        } else if ((type.equals(Byte.class) || type.equals(Byte.TYPE) || type.equals(Short.class) || type.equals(Short.TYPE)
                || type.equals(Integer.class) || type.equals(Integer.TYPE) || type.equals(Long.class)
                || type.equals(Long.TYPE) || type.equals(Float.class) || type.equals(Float.TYPE)
                || type.equals(Double.class) || type.equals(Double.TYPE))
                && (obj instanceof Number || (obj != null && Type.isLongString(str))
                || (obj != null && Type.isDoubleString(str)))) {
            Number num;
            if (obj instanceof Number) {
                num = (Number) obj;
            } else {
                try {
                    if (Type.isLongString(str))
                        num = Long.parseLong(str);
                    else
                        num = Double.parseDouble(str);
                } catch (NumberFormatException exception) {
                    num = Type.parseHexadecimal(str.substring(2));
                }
            }

            if (type.equals(Byte.class) || type.equals(Byte.TYPE)) {
                return num.byteValue();
            } else if (type.equals(Short.class) || type.equals(Short.TYPE)) {
                return num.shortValue();
            } else if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
                return num.intValue();
            } else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
                return num.longValue();
            } else if (type.equals(Float.class) || type.equals(Float.TYPE)) {
                return num.floatValue();
            } else {
                return num.doubleValue();
            }
        } else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
            if ("TRUE".equalsIgnoreCase(str)) {
                return Boolean.TRUE;
            } else if ("FALSE".equalsIgnoreCase(str)) {
                return Boolean.FALSE;
            }
        } else if ((type.equals(Character.class) || type.equals(Character.TYPE)) && obj != null) {
            if (str.length() == 1) {
                return str.charAt(0);
            } else if (Type.isUnicodeEscape(str)) {
                return (char) Type.parseHexadecimal(str.substring(2)).intValue();
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Checks whether {@code str} is a single Unicode-escaped character.
     *
     * @param str string
     * @return {@code true} if {@code str} is a single Unicode-escaped character
     */
    private static boolean isUnicodeEscape(String str) {
        if (str.length() == 6 && str.startsWith("\\u") && Type.isHexadecimal(str.substring(2))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether {@code str} is definitely a string representing an integer.
     *
     * @param str string
     * @return {@code true} if {@code str} is definitely a string representing an integer; {@code false} otherwise.
     */
    private static boolean isLongString(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException ignore) {
        }

        if (str.startsWith("0x") && Type.isHexadecimal(str.substring(2))) {
            BigInteger val = Type.parseHexadecimal(str.substring(2));
            BigInteger zero = new BigInteger("0");
            BigInteger limit = new BigInteger("FFFF_FFFF_FFFF_FFFF", 16);

            if (zero.compareTo(val) <= 0 && val.compareTo(limit) <= 0)
                return true;
            else
                return false;
        } else {
            return false;
        }
    }

    /**
     * Checks whether {@code str} is definitely a string representing a double.
     *
     * @param str string
     * @return {@code true} if {@code str} is definitely a string representing a double; {@code false} otherwise.
     */
    private static boolean isDoubleString(String str) {
        try {
            double num = Double.parseDouble(str);

            if (!Double.isInfinite(num) || str.equals("Infinity") || str.equals("-Infinity"))
                return true;
            else
                return false;
        } catch (NumberFormatException ignore) {
        }

        return false;
    }

    /**
     * Determines whether a string is a hexadecimal string.
     *
     * @param str string to check
     * @return {@code true} if {@code str} is a hexadecimal string; {@code false} otherwise.
     */
    private static boolean isHexadecimal(String str) {
        final String hexadecimal = "0123456789ABCDEF";
        str = str.toUpperCase();

        if (!str.isEmpty()) {
            for (int i = 0; i < str.length(); i++)
                if (hexadecimal.indexOf(str.charAt(i)) < 0)
                    return false;

            return true;
        } else {
            return false;
        }
    }

    /**
     * Parses a string as a hexadecimal string and gets the value.
     *
     * @param str string
     * @return the value interpreted from {@code str} as hexadecimal. The result is undefined if {@code str} is not hexadecimal.
     */
    private static BigInteger parseHexadecimal(String str) {
        return new BigInteger(str, 16);
    }
}
