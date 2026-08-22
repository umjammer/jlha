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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Properties;


/**
 * Defines various constants for LHA.
 *
 * <pre>
 * -- revision history --
 * $Log: CompressMethod.java,v $
 * Revision 1.1  2002/12/08 00:00:00  dangan
 * [change]
 *     Changed class name from LhaConstants to CompressMethod.
 *
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [change]
 *     Inherited connectExtractInputStream of LhaUtil as connectDecoder,
 *     and connectCompressOutputStream as connectEncoder.
 *     Inherited CompressMethodTo????????? of LhaUtil.
 * [maintenance]
 *     Source cleanup.
 *     Eliminated tabs.
 *     Updated license text.
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class CompressMethod {

    /**
     * String indicating the compression method.
     * LH0 is "-lh0-", indicating no compression.
     */
    public static final String LH0 = "-lh0-";

    /**
     * String indicating the compression method.
     * LH1 is "-lh1-", which means using LZSS with a 4-kilobyte dictionary and maximum match length of 60 bytes
     * in the pre-stage, and adaptive Huffman coding in the post-stage.
     */
    public static final String LH1 = "-lh1-";

    /**
     * String indicating the compression method.
     * LH2 is "-lh2-", which means using LZSS with an 8-kilobyte dictionary and maximum match length of 256 bytes
     * in the pre-stage, and adaptive Huffman coding in the post-stage.
     * This compression method was used experimentally during the transition from LH1 to LH5,
     * but is not used currently.
     */
    public static final String LH2 = "-lh2-";

    /**
     * String indicating the compression method.
     * LH3 is "-lh3-", which means using LZSS with an 8-kilobyte dictionary and maximum match length of 256 bytes
     * in the pre-stage, and static Huffman coding in the post-stage.
     * This compression method was used experimentally during the transition from LH1 to LH5,
     * but is not used currently.
     */
    public static final String LH3 = "-lh3-";

    /**
     * String indicating the compression method.
     * LH4 is "-lh4-", which means using LZSS with a 4-kilobyte dictionary and maximum match length of 256 bytes
     * in the pre-stage, and static Huffman coding in the post-stage.
     * This compression method was used when compressing on low-powered machines in the early 1990s
     * when sufficient system resources to perform LH5 compression were not available,
     * but is rarely used now.
     */
    public static final String LH4 = "-lh4-";

    /**
     * String indicating the compression method.
     * LH5 is "-lh5-", which means using LZSS with an 8-kilobyte dictionary and maximum match length of 256 bytes
     * in the pre-stage, and static Huffman coding in the post-stage.
     * It is currently the standard compression method used in LHA.
     */
    public static final String LH5 = "-lh5-";

    /**
     * String indicating the compression method.
     * LH6 is "-lh6-", which means using LZSS with a 32-kilobyte dictionary and maximum match length of 256 bytes
     * in the pre-stage, and static Huffman coding in the post-stage.
     * The string "-lh6-" was used in experiments with the LH7 compression method. Therefore, it is said that
     * there are archives created by experimental versions of LHA that are compressed in LH7 format while using the "-lh6-" string.
     * Also, although nearly 10 years have passed since this compression method was developed, it is still
     * considered desirable not to register archives compressed with this compression method in public places.
     */
    public static final String LH6 = "-lh6-";

    /**
     * String indicating the compression method.
     * LH7 is "-lh7-", which means using LZSS with a 64-kilobyte dictionary and maximum match length of 256 bytes
     * in the pre-stage, and static Huffman coding in the post-stage.
     * Also, although nearly 10 years have passed since this compression method was developed, it is still
     * considered desirable not to register archives compressed with this compression method in public places.
     */
    public static final String LH7 = "-lh7-";

    /**
     * String indicating the compression method.
     * LHD is "-lhd-", which indicates no compression and storing directories.
     */
    public static final String LHD = "-lhd-";

    /**
     * String indicating the compression method.
     * LZS is "-lzs-", which indicates using LZSS with a 2-kilobyte dictionary and maximum match length of 17 bytes.
     * "-lzs-" is the format of LArc, which was popular before LHA was created, and was defined
     * in consideration of compatibility at that time. It is rarely used now.
     */
    public static final String LZS = "-lzs-";

    /**
     * String indicating the compression method.
     * LZ4 is "-lz4-", indicating no compression.
     * "-lz4-" is the format of LArc, which was popular before LHA was created, and was defined
     * in consideration of compatibility at that time. It is rarely used now.
     */
    public static final String LZ4 = "-lz4-";

    /**
     * String indicating the compression method.
     * LZ5 is "-lz5-", which indicates using LZSS with a 4-kilobyte dictionary and maximum match length of 17 bytes.
     * "-lz5-" is the format of LArc, which was popular before LHA was created, and was defined
     * in consideration of compatibility at that time. It is rarely used now.
     */
    public static final String LZ5 = "-lz5-";

    /**
     * Default constructor cannot be used.
     */
    private CompressMethod() {
    }

    /**
     * Gets the dictionary size from the compression method identifier.
     *
     * @param method compression method identifier
     * @return dictionary size
     */
    public static int toDictionarySize(String method) {
        if (CompressMethod.LZS.equalsIgnoreCase(method)) {
            return 2048;
        } else if (CompressMethod.LZ5.equalsIgnoreCase(method)) {
            return 4096;
        } else if (CompressMethod.LH1.equalsIgnoreCase(method)) {
            return 4096;
        } else if (CompressMethod.LH2.equalsIgnoreCase(method)) {
            return 8192;
        } else if (CompressMethod.LH3.equalsIgnoreCase(method)) {
            return 8192;
        } else if (CompressMethod.LH4.equalsIgnoreCase(method)) {
            return 4096;
        } else if (CompressMethod.LH5.equalsIgnoreCase(method)) {
            return 8192;
        } else if (CompressMethod.LH6.equalsIgnoreCase(method)) {
            return 32768;
        } else if (CompressMethod.LH7.equalsIgnoreCase(method)) {
            return 65536;
        } else if (CompressMethod.LZ4.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(method + " means no compress.");
        } else if (CompressMethod.LH0.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(method + " means no compress.");
        } else if (CompressMethod.LHD.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(method + " means no compress.");
        } else if (method == null) {
            throw new NullPointerException("method");
        } else {
            throw new IllegalArgumentException("Unknown compress method. " + method);
        }
    }

    /**
     * Gets the compression/non-compression threshold from the compression method identifier.
     *
     * @param method compression method identifier
     * @return threshold
     */
    public static int toThreshold(String method) {
        if (CompressMethod.LZS.equalsIgnoreCase(method)) {
            return 2;
        } else if (CompressMethod.LZ5.equalsIgnoreCase(method)) {
            return 3;
        } else if (CompressMethod.LH1.equalsIgnoreCase(method)) {
            return 3;
        } else if (CompressMethod.LH2.equalsIgnoreCase(method)) {
            return 3;
        } else if (CompressMethod.LH3.equalsIgnoreCase(method)) {
            return 3;
        } else if (CompressMethod.LH4.equalsIgnoreCase(method)) {
            return 3;
        } else if (CompressMethod.LH5.equalsIgnoreCase(method)) {
            return 3;
        } else if (CompressMethod.LH6.equalsIgnoreCase(method)) {
            return 3;
        } else if (CompressMethod.LH7.equalsIgnoreCase(method)) {
            return 3;
        } else if (CompressMethod.LZ4.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(method + " means no compress.");
        } else if (CompressMethod.LH0.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(method + " means no compress.");
        } else if (CompressMethod.LHD.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(method + " means no compress.");
        } else if (method == null) {
            throw new NullPointerException("method");
        } else {
            throw new IllegalArgumentException("Unknown compress method. " + method);
        }
    }

    /**
     * Gets the maximum match length from the compression method identifier.
     *
     * @param method compression method identifier
     * @return maximum match length
     */
    public static int toMaxMatch(String method) {
        if (CompressMethod.LZS.equalsIgnoreCase(method)) {
            return 17;
        } else if (CompressMethod.LZ5.equalsIgnoreCase(method)) {
            return 18;
        } else if (CompressMethod.LH1.equalsIgnoreCase(method)) {
            return 60;
        } else if (CompressMethod.LH2.equalsIgnoreCase(method)) {
            return 256;
        } else if (CompressMethod.LH3.equalsIgnoreCase(method)) {
            return 256;
        } else if (CompressMethod.LH4.equalsIgnoreCase(method)) {
            return 256;
        } else if (CompressMethod.LH5.equalsIgnoreCase(method)) {
            return 256;
        } else if (CompressMethod.LH6.equalsIgnoreCase(method)) {
            return 256;
        } else if (CompressMethod.LH7.equalsIgnoreCase(method)) {
            return 256;
        } else if (CompressMethod.LZ4.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(method + " means no compress.");
        } else if (CompressMethod.LH0.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(method + " means no compress.");
        } else if (CompressMethod.LHD.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(method + " means no compress.");
        } else if (method == null) {
            throw new NullPointerException("method");
        } else {
            throw new IllegalArgumentException("Unknown compress method. " + method);
        }
    }

    /**
     * Constructs a stream that compresses data using the compression method {@code method}
     * using the generator expression set in {@code property}, and outputs to {@code out}.
     *
     * @param out output stream for compressed data
     * @param method compression method identifier
     * @param property properties containing generator expressions for encoders corresponding to each compression format
     * @return stream that compresses data using the compression method {@code method} and outputs to {@code out}
     */
    public static OutputStream connectEncoder(OutputStream out, String method, Properties property) {

        String key = "lha." + CompressMethod.getCore(method) + ".encoder";

        String generator = property.getProperty(key);
        if (generator == null) {
            generator = LhaProperty.getProperty(key);
        }

        String packages = property.getProperty("lha.packages");
        if (packages == null) {
            packages = LhaProperty.getProperty("lha.packages");
        }

        Hashtable<String, Object> substitute = new Hashtable<>();
        substitute.put("out", out);

        return (OutputStream) LhaProperty.parse(generator, substitute, packages);
    }

    /**
     * Constructs an input stream that decompresses data compressed with the compression method {@code method} from {@code in}
     * and supplies it, using the generator expression set in {@code property}.
     *
     * @param in stream supplying compressed data
     * @param method compression method identifier
     * @param property properties containing generator expressions for decoders corresponding to each compression format
     * @param length length of original uncompressed data
     * @return input stream that decompresses data compressed with the compression method {@code method} from {@code in} and supplies it
     */
    public static InputStream connectDecoder(InputStream in, String method, Properties property, long length) {

        String key = "lha." + CompressMethod.getCore(method) + ".decoder";

        String generator = property.getProperty(key);
        if (generator == null) {
            generator = LhaProperty.getProperty(key);
        }

        String packages = property.getProperty("lha.packages");
        if (packages == null) {
            packages = LhaProperty.getProperty("lha.packages");
        }

        Hashtable<String, Object> substitute = new Hashtable<>();
        substitute.put("in", in);
        substitute.put("length", length);

        return (InputStream) LhaProperty.parse(generator, substitute, packages);
    }

    /**
     * Strips leading and trailing {@code '-'} from the compression method identifier
     * to generate a string that fits in {@code ???} of the {@link LhaProperty} keys {@code lha.???.encoder} / {@code lha.???.decoder}.
     *
     * @param method compression method identifier
     * @return string usable in the core part of key
     */
    private static String getCore(String method) {
        if (method.startsWith("-") && method.endsWith("-")) {
            return method.substring(1, method.lastIndexOf('-')).toLowerCase();
        } else {
            throw new IllegalArgumentException("");
        }
    }
}
