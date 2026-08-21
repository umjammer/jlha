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

package jp.gr.java_conf.dangan.util.lha;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.StringTokenizer;

import jp.gr.java_conf.dangan.lang.reflect.Factory;


/**
 * Handles various configurations for LHA Library for Java.<br>
 * The values obtained by {@link #getProperty(String)} and {@link #getProperties()} are used from
 * either system properties, configuration file, or default values,
 * and the priority order is as follows:
 * <ol>
 * <li>Value set in system properties.
 * <li>Value set in jp/gr/java_conf/dangan/util/lha/resources/lha.properties.
 * <li>Default value.
 * </ol>
 * <br>
 * <br>
 * The list of keys is as follows:
 * <br>
 * <table border="0" cellspacing="4">
 * <tr>
 * <td nowrap>Key</td>
 * <td nowrap>Description of corresponding value</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.encoding</td>
 * <td nowrap>Encoding used for mutual conversion between {@link String} and strings in header</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.packages</td>
 * <td nowrap>Enumeration of package names of classes used in generator expressions (comma-separated)</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lzs.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lzs- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lz4.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lz4- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lz5.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lz5- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh0.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lh0- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh1.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lh1- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh2.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lh2- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh3.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lh3- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh4.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lh4- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh5.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lh5- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh6.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lh6- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh7.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lh7- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lhd.encoder</td>
 * <td nowrap>Generator expression for object that performs encoding into -lhd- format</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lzs.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lzs- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lz4.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lz4- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lz5.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lz5- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh0.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lh0- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh1.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lh1- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh2.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lh2- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh3.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lh3- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh4.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lh4- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh5.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lh5- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh6.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lh6- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lh7.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lh7- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.lhd.decoder</td>
 * <td nowrap>Generator expression for object that decodes -lhd- format data</td>
 * </tr>
 * <tr>
 * <td nowrap>lha.header</td>
 * <td nowrap>Generator expression for instance of {@link LhaHeader}</td>
 * </tr>
 * </table>
 * <br>
 * Generator expressions are defined as follows:<br>
 * <table border="0" cellspacing="4">
 * <tr>
 * <td nowrap>&lt;generator-expression&gt;</td>
 * <td nowrap>::= &lt;constructor&gt; | &lt;array&gt; | &lt;substitution-string&gt; | &lt;class-name&gt; |
 * &lt;string&gt;</td>
 * </tr>
 * <tr>
 * <td nowrap>&lt;constructor&gt;</td>
 * <td nowrap>::= &lt;class-name&gt; '(' argument ')'</td>
 * </tr>
 * <tr>
 * <td nowrap>&lt;argument&gt;</td>
 * <td nowrap>::= [ &lt;generator-expression&gt; [ ',' &lt;argument&gt; ] ]</td>
 * </tr>
 * <tr>
 * <td nowrap>&lt;array&gt;</td>
 * <td nowrap>::= '[' &lt;element&gt; ']'</td>
 * </tr>
 * <tr>
 * <td nowrap>&lt;element&gt;</td>
 * <td nowrap>::= [ &lt;generator-expression&gt; [ ',' &lt;element&gt; ] ]</td>
 * </tr>
 * </table>
 * <br>
 * Class names are converted to fully qualified names using values corresponding to "lha.packages".<br>
 * Substitution strings are strings that are replaced with objects inside the library, and
 * the following 4 types are currently defined:<br>
 * <table border="0" cellspacing="4">
 * <tr>
 * <td nowrap>lha.???.encoder</td>
 * <td nowrap>out</td>
 * <td nowrap>java.io.OutputStream that receives compressed data</td>
 * </tr>
 * <tr>
 * <td nowrap rowspan="2">lha.???.decoder</td>
 * <td nowrap>in</td>
 * <td nowrap>java.io.InputStream that supplies compressed data</td>
 * </tr>
 * <tr>
 * <td nowrap>length</td>
 * <td nowrap>Byte length of decoded data</td>
 * </tr>
 * <tr>
 * <td nowrap rowspan="2">lha.header</td>
 * <td nowrap>data</td>
 * <td nowrap>byte array storing header data</td>
 * </tr>
 * <tr>
 * <td nowrap>encoding</td>
 * <td nowrap>Encoding used when converting character data in header to {@link String}</td>
 * </tr>
 * </table>
 * <br>
 *
 * <pre>
 * -- revision history --
 * $Log: LhaProperty.java,v $
 * Revision 1.0.2.2  2005/04/29 02:15:53  dangan
 * [bug fix]
 *     In createDefaultProperty(), encoder and decoder for compression method identifier -lhd- were not configured.
 *
 * Revision 1.0.2.1  2004/06/27 12:09:49  dangan
 * [bugfix]
 *     Fixed period used instead of comma in generator expression.
 *
 * Revision 1.0  2002/12/05 00:00:00  dangan
 * first edition
 * add to version control
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0.2.2 $
 */
public class LhaProperty {

    /**
     * Properties holding settings for LHA Library for Java
     */
    private static final Properties property = LhaProperty.createLhaProperty();

    /**
     * Default encoding used within LHA Library for Java
     */
    public static final String encoding = LhaProperty.property.getProperty("lha.encoding");

    /**
     * Default constructor cannot be used.
     */
    private LhaProperty() {
    }

    /**
     * Gets property corresponding to {@code key} from the properties of LHA Library for Java.
     *
     * @param key property key
     * @return property string
     */
    public static String getProperty(String key) {
        String def = LhaProperty.property.getProperty(key);
        try {
            if (key.equals("lha.encoding") && System.getProperty(key, def).equals("ShiftJISAuto")) {
                try {
                    String encoding = Charset.defaultCharset().displayName();
                    if (LhaProperty.isCategoryOfShiftJIS(encoding)) {
                        return encoding;
                    } else {
                        return "SJIS";
                    }
                } catch (SecurityException exception) {
                    return "SJIS";
                }
            } else {
                return System.getProperty(key, def);
            }
        } catch (SecurityException ignore) {
        }

        return def;
    }

    /**
     * Gets a copy of the properties of LHA Library for Java.
     *
     * @return copy of properties
     */
    public static Properties getProperties() {
        Properties property = (Properties) LhaProperty.property.clone();
        Enumeration<?> enumKey = property.propertyNames();

        while (enumKey.hasMoreElements()) {
            String key = (String) enumKey.nextElement();
            try {
                String val = System.getProperty(key);
                if (null != val) {
                    property.put(key, val);
                }
            } catch (SecurityException ignore) {
            }
        }

        if (property.getProperty("lha.encoding").equals("ShiftJISAuto")) {
            try {
                String encoding = Charset.defaultCharset().displayName();
                if (LhaProperty.isCategoryOfShiftJIS(encoding)) {
                    property.put("lha.encoding", encoding);
                } else {
                    property.put("lha.encoding", "SJIS");
                }
            } catch (SecurityException exception) {
                property.put("lha.encoding", "SJIS");
            }
        }

        return property;
    }

    /**
     * Parses the generator expression {@code source} for properties of LHA Library for Java to generate a new {@link Object}.
     *
     * @param source generator expression to parse
     * @param substitute {@link Hashtable} having substitution target strings as keys and substitution {@link Object}s as values
     * @param packages comma-separated enumeration of package names
     * @return generated {@link Object}
     */
    public static Object parse(String source, Hashtable<?, ?> substitute, String packages) {

        StringTokenizer tokenizer = new StringTokenizer(packages, ",");
        String[] packageArray = new String[tokenizer.countTokens()];
        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            packageArray[i++] = tokenizer.nextToken().trim();
        }

        return LhaProperty.parse(source, substitute, packageArray);
    }

    /**
     * Parses the generator expression {@code source} for properties of LHA Library for Java to generate a new {@link Object}.
     *
     * @param source string to parse
     * @param substitute {@link Hashtable} having substitution target strings as keys and substitution {@link Object}s as values
     * @param packages array of package names
     * @return generated {@link Object}
     */
    public static Object parse(String source, Hashtable<?, ?> substitute, String[] packages) {

        source = source.trim();
        int caseArcPos = source.indexOf("(");
        int bracePos = source.indexOf("[");

        if (0 <= caseArcPos && (bracePos < 0 || caseArcPos < bracePos)) {
            return LhaProperty.parseConstructor(source, substitute, packages);
        } else if (0 <= bracePos && (caseArcPos < 0 || bracePos < caseArcPos)) {
            return LhaProperty.parseArray(source, substitute, packages);
        } else if (substitute.containsKey(source)) {
            return substitute.get(source);
        } else {
            return LhaProperty.applyPackages(source, packages);
        }
    }

    /**
     * Parses string {@code source} indicating a constructor for properties of LHA Library for Java
     * to generate a new instance.
     *
     * @param source string indicating a constructor to parse
     * @param substitute {@link Hashtable} having substitution target strings as keys and substitution {@link Object}s as values
     * @param packages array of package names
     * @return generated instance
     */
    private static Object parseConstructor(String source, Hashtable<?, ?> substitute, String[] packages) {

        String className = source.substring(0, source.indexOf('(')).trim();
        String arguments = source.substring(source.indexOf('(') + 1, source.lastIndexOf(')')).trim();

        className = LhaProperty.applyPackages(className, packages);
        Object[] args;
        if (!arguments.isEmpty()) {
            StringTokenizer tokenizer = new StringTokenizer(arguments, ",()[]", true);
            Stack<Comparable<?>> stack = new Stack<>();
            int pos = 0;
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                switch (token) {
                    case "(" -> stack.push("(");
                    case ")" -> {
                        if (!stack.empty() && stack.peek().equals("(")) {
                            stack.pop();
                        }
                    }
                    case "[" -> stack.push("[");
                    case "]" -> {
                        if (!stack.empty() && stack.peek().equals("[")) {
                            stack.pop();
                        }
                    }
                    case "," -> {
                        if (stack.empty() || (!stack.peek().equals("(") && !stack.peek().equals("["))) {
                            stack.push(pos);
                        }
                    }
                }
                pos += token.length();
            }

            pos = 0;
            args = new Object[stack.size() + 1];
            for (int i = 0; i < stack.size() + 1; i++) {
                String arg;
                if (i < stack.size()) {
                    arg = arguments.substring(pos, (Integer) stack.elementAt(i));
                } else {
                    arg = arguments.substring(pos);
                }
                pos += arg.length() + 1;
                args[i] = LhaProperty.parse(arg, substitute, packages);
            }

        } else {
            args = new Object[0];
        }

        try {
            return Factory.createInstance(className, args);
        } catch (InstantiationException exception) {
            throw new InstantiationError(exception.getMessage());
        } catch (InvocationTargetException exception) {
            if (exception.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) exception.getTargetException();
            } else if (exception.getTargetException() instanceof Error) {
                throw (Error) exception.getTargetException();
            } else {
                throw new Error(exception.getTargetException().getMessage());
            }
        } catch (ClassNotFoundException exception) {
            throw new NoClassDefFoundError(exception.getMessage());
        } catch (NoSuchMethodException exception) {
            throw new NoSuchMethodError(exception.getMessage());
        }
    }

    /**
     * Parses string {@code source} indicating an array for properties of LHA Library for Java
     * to generate a new array of {@link Object}.
     *
     * @param source string indicating a constructor to parse
     * @param substitute {@link Hashtable} having substitution target strings as keys and substitution {@link Object}s as values
     * @param packages array of package names
     * @return generated array of {@link Object}
     */
    private static Object[] parseArray(String source, Hashtable<?, ?> substitute, String[] packages) {

        String arguments = source.substring(source.indexOf('[') + 1, source.lastIndexOf(']')).trim();

        if (!arguments.isEmpty()) {
            StringTokenizer tokenizer = new StringTokenizer(arguments, ",()[]", true);
            Stack<Comparable<?>> stack = new Stack<>();
            int pos = 0;
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                switch (token) {
                    case "(" -> stack.push("(");
                    case ")" -> {
                        if (!stack.empty() && stack.peek().equals("("))
                            stack.pop();
                    }
                    case "[" -> stack.push("[");
                    case "]" -> {
                        if (!stack.empty() && stack.peek().equals("["))
                            stack.pop();
                    }
                    case "," -> {
                        if (stack.empty() || (!stack.peek().equals("(") && !stack.peek().equals("[")))
                            stack.push(pos);
                    }
                }
                pos += token.length();
            }

            pos = 0;
            Object[] array = new Object[stack.size() + 1];
            for (int i = 0; i < stack.size() + 1; i++) {
                String arg;
                if (i < stack.size()) {
                    arg = arguments.substring(pos, (Integer) stack.elementAt(i));
                } else {
                    arg = arguments.substring(pos);
                }
                pos += arg.length() + 1;
                array[i] = LhaProperty.parse(arg, substitute, packages);
            }
            return array;
        } else {
            return new Object[0];
        }
    }

    /**
     * Assumes {@code str} is a class name and attempts to concatenate it with package names
     * included in {@code packages} to create a fully qualified name.
     *
     * @param str string that might be a class name
     * @param packages enumeration of package names
     * @return fully qualified name, or {@code str}
     */
    private static String applyPackages(String str, String[] packages) {
        for (String aPackage : packages) {
            String className;
            if (aPackage.isEmpty()) {
                className = str;
            } else {
                className = aPackage + "." + str;
            }
            try {
                Class.forName(className);
                return className;
            } catch (ClassNotFoundException | LinkageError ignore) {
            }
        }
        return str;
    }

    /**
     * Generates properties for LHA Library for Java.
     *
     * @return generated properties
     */
    private static Properties createLhaProperty() {
        String path = "jp.gr.java_conf.dangan.util.lha.resources.lha";
        Properties property = LhaProperty.createDefaultProperty();

        try {
            ResourceBundle bundle = ResourceBundle.getBundle(path);
            Enumeration<?> enumkey = bundle.getKeys();
            while (enumkey.hasMoreElements()) {
                String key = (String) enumkey.nextElement();
                property.put(key, bundle.getString(key));
            }
        } catch (MissingResourceException ignore) {
        }

        if (property.getProperty("lha.encoding").equals("ShiftJISAuto")) {
            try {
                String encoding = Charset.defaultCharset().displayName();
                if (LhaProperty.isCategoryOfShiftJIS(encoding)) {
                    property.put("lha.encoding", encoding);
                } else {
                    property.put("lha.encoding", "SJIS");
                }
            } catch (SecurityException exception) {
                property.put("lha.encoding", "SJIS");
            }
        }

        return property;
    }

    /**
     * Generates default properties for LHA Library for Java.
     * For use when there is no configuration file under jp/gr/java_conf/dangan/util/lha/resources/.
     *
     * @return default properties
     */
    private static Properties createDefaultProperty() {
        Properties property = new Properties();

        // encoding of String
        property.put("lha.encoding", LhaProperty.getSystemEncoding());

        // package names
        property.put("lha.packages", "jp.gr.java_conf.dangan.util.lha");

        // encoders
        property.put("lha.lzs.encoder", "LzssOutputStream( PostLzsEncoder( out ), HashAndChainedListSearch, [ HashShort ] )");
        property.put("lha.lz4.encoder", "out");
        property.put("lha.lz5.encoder", "LzssOutputStream( PostLz5Encoder( out ), HashAndChainedListSearch )");
        property.put("lha.lhd.encoder", "out");
        property.put("lha.lh0.encoder", "out");
        property.put("lha.lh1.encoder", "LzssOutputStream( PostLh1Encoder( out ), HashAndChainedListSearch )");
        property.put("lha.lh2.encoder", "LzssOutputStream( PostLh2Encoder( out ), HashAndChainedListSearch )");
        property.put("lha.lh3.encoder", "LzssOutputStream( PostLh3Encoder( out ), HashAndChainedListSearch )");
        property.put("lha.lh4.encoder", "LzssOutputStream( PostLh5Encoder( out, -lh4- ), HashAndChainedListSearch )");
        property.put("lha.lh5.encoder", "LzssOutputStream( PostLh5Encoder( out, -lh5- ), HashAndChainedListSearch )");
        property.put("lha.lh6.encoder", "LzssOutputStream( PostLh5Encoder( out, -lh6- ), HashAndChainedListSearch )");
        property.put("lha.lh7.encoder", "LzssOutputStream( PostLh5Encoder( out, -lh7- ), HashAndChainedListSearch )");

        // decoders
        property.put("lha.lzs.decoder", "LzssInputStream( PreLzsDecoder( in ), length )");
        property.put("lha.lz4.decoder", "in");
        property.put("lha.lz5.decoder", "LzssInputStream( PreLz5Decoder( in ), length )");
        property.put("lha.lhd.decoder", "in");
        property.put("lha.lh0.decoder", "in");
        property.put("lha.lh1.decoder", "LzssInputStream( PreLh1Decoder( in ), length )");
        property.put("lha.lh2.decoder", "LzssInputStream( PreLh2Decoder( in ), length )");
        property.put("lha.lh3.decoder", "LzssInputStream( PreLh3Decoder( in ), length )");
        property.put("lha.lh4.decoder", "LzssInputStream( PreLh5Decoder( in, -lh4- ), length )");
        property.put("lha.lh5.decoder", "LzssInputStream( PreLh5Decoder( in, -lh5- ), length )");
        property.put("lha.lh6.decoder", "LzssInputStream( PreLh5Decoder( in, -lh6- ), length )");
        property.put("lha.lh7.decoder", "LzssInputStream( PreLh5Decoder( in, -lh7- ), length )");

        // header
        property.put("lha.header", "LhaHeader( data, encoding )");

        return property;
    }

    /**
     * Returns the encoding obtained by {@code System.getProperty("file.encoding")}.
     * If the obtained encoding is Japanese encoding and is not of the ShiftJIS family,
     * "SJIS" is forcibly used.
     * If the security manager does not allow access to system properties, "ISO8859_1" is used.
     *
     * @return encoding obtained by {@code System.getProperty("file.encoding")}
     */
    private static String getSystemEncoding() {
        String encoding;
        try {
            encoding = Charset.defaultCharset().displayName();
            if (LhaProperty.isJapanese(encoding) && !LhaProperty.isCategoryOfShiftJIS(encoding)) {
                return "SJIS";
            } else {
                return encoding;
            }
        } catch (SecurityException exception) {
            encoding = "ISO8859_1";
        }
        return encoding;
    }

    /**
     * Returns whether {@code encoding} is a Japanese encoding.
     *
     * @param encoding encoding
     * @return {@code true} if {@code encoding} is a Japanese encoding, {@code false} otherwise
     */
    private static boolean isJapanese(String encoding) {

        String[] Converters = {
            "Cp930", // Japanese EBCDIC
            "Cp939", // Japanese EBCDIC
            "Cp942", // SJIS OS/2 Japanese, superset of Cp932, 0x5C -> '\' (halfwidth backslash)
            "Cp942C", // SJIS OS/2 Japanese, superset of Cp932, 0x5C -> '¥' (halfwidth yen sign)
            "Cp943", // SJIS OS/2 Japanese, superset of Cp942, supporting new JIS, 0x5C -> '\' (halfwidth backslash)
            "Cp943C", // SJIS OS/2 Japanese, superset of Cp942, supporting new JIS, 0x5C -> '¥' (halfwidth yen sign)
            "Cp33722", // EUC IBM Japanese
            "MS932", // Windows Japanese
            "SJIS", // Shift-JIS, Japanese
            "EUC_JP", // EUC, Japanese JIS X 0201, 0208, 0212
            "ISO2022JP", // JIS X 0201, ISO 2022 format 0208, Japanese
            "JIS0201", // JIS X 0201, Japanese
            "JIS0208", // JIS X 0208, Japanese
            "JIS0212", // JIS X 0212, Japanese
            "JISAutoDetect"
        }; // Shift-JIS EUC-JP ISO 2022 JP detection and conversion. Read-only.
        for (String converter : Converters) {
            if (encoding.equals(converter)) {
                return true;
            }
        }

        String[] Aliases = {
            "eucjis", "euc-jp", "eucjp", "x-euc-jp", "x-eucjp", // Aliases of "EUC_JP"
            "csEUCPkdFmtJapanese", // Alias of "EUCJIS"(?)
            "extended_unix_code_packed_format_for_japanese ", // Alias of "EUCJIS"(?)
            "shift_jis", "ms_kanji", "csShiftJIS", // JDK1.1.1 - JDK1.1.7B Alias of "SJIS", JDK1.2 - JDK1.3 Alias of "MS932", JDK1.4 Alias of "SJIS"
            "csWindows31J", "windows-31j", // Alias of "MS932"
            "x-sjis", // JDK1.2 Alias of "MS932", JDK1.3 Alias of "SJIS", JDK1.4 Alias of "MS932"
            "jis", // Alias of "ISO2022JP"
            "iso-2022-jp", // JDK1.1.1-JDK1.1.5 Alias of "JIS", JDK1.1.6- Alias of "ISO2022JP"
            "csISO2022JP", // JDK1.1.1-JDK1.1.5 Alias of "JIS", JDK1.1.6- Alias of "ISO2022JP"
            "jis_encoding", // JDK1.1.1-JDK1.1.5 Alias of "JIS", JDK1.1.6- Alias of "ISO2022JP"
            "csJISEncoding", // JDK1.1.1-JDK1.1.5 Alias of "JIS", JDK1.1.6- Alias of "ISO2022JP"
            "jis auto detect", // Alias of "JISAutoDetect"
            "cp930", "ibm-930", "ibm930", "930", // Aliases of "Cp930"
            "cp939", "ibm-939", "ibm939", "939", // Aliases of "Cp939"
            "cp942", "ibm-942", "ibm942", "942", // Aliases of "Cp942"
            "cp942c", // Alias of "Cp942C"
            "cp943", "ibm-943", "ibm943", "943", // Aliases of "Cp943"
            "cp943c", // Alias of "Cp943C"
            "cp33722", "ibm-33722", "ibm33722", "33722"
        }; // Aliases of "Cp33722"
        for (String alias : Aliases) {
            if (encoding.equalsIgnoreCase(alias)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether {@code encoding} is a ShiftJIS-family encoding.
     *
     * @param encoding encoding
     * @return {@code true} if {@code encoding} is a Japanese encoding, {@code false} otherwise
     */
    private static boolean isCategoryOfShiftJIS(String encoding) {

        String[] Converters = {
            "Cp942", // SJIS OS/2 Japanese, superset of Cp932, 0x5C -> '\' (halfwidth backslash)
            "Cp942C", // SJIS OS/2 Japanese, superset of Cp932, 0x5C -> '¥' (halfwidth yen sign)
            "Cp943", // SJIS OS/2 Japanese, superset of Cp942, supporting new JIS, 0x5C -> '\' (halfwidth backslash)
            "Cp943C", // SJIS OS/2 Japanese, superset of Cp942, supporting new JIS, 0x5C -> '¥' (halfwidth yen sign)
            "MS932", // Windows Japanese
            "SJIS"
        }; // Shift-JIS, Japanese
        for (String converter : Converters) {
            if (encoding.equals(converter)) {
                return true;
            }
        }

        String[] Aliases = {
            "shift_jis", "ms_kanji", "csShiftJIS", // JDK1.1.1 - JDK1.1.7B Alias of "SJIS", JDK1.2 - JDK1.3 Alias of "MS932", JDK1.4 Alias of "SJIS"
            "csWindows31J", "windows-31j", // Alias of "MS932"
            "x-sjis", // JDK1.2 Alias of "MS932", JDK1.3 Alias of "SJIS", JDK1.4 Alias of "MS932"
            "cp942", "ibm-942", "ibm942", "942", // Aliases of "Cp942"
            "cp942c", // Alias of "Cp942C"
            "cp943", "ibm-943", "ibm943", "943", // Aliases of "Cp943"
            "cp943c"
        }; // Alias of "Cp943C"
        for (String alias : Aliases) {
            if (encoding.equalsIgnoreCase(alias)) {
                return true;
            }
        }

        return false;
    }
}
