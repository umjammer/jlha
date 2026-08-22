/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jp.gr.java_conf.dangan.util.lha.CompressMethod;
import jp.gr.java_conf.dangan.util.lha.LhaFile;
import jp.gr.java_conf.dangan.util.lha.LhaHeader;
import jp.gr.java_conf.dangan.util.lha.LhaImmediateOutputStream;
import jp.gr.java_conf.dangan.util.lha.LhaInputStream;
import jp.gr.java_conf.dangan.util.lha.LhaOutputStream;
import jp.gr.java_conf.dangan.util.lha.LhaProperty;
import jp.gr.java_conf.dangan.util.lha.LhaRetainedOutputStream;


/**
 * Unit tests for each LH# compression method.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2026/08/22 umjammer initial version <br>
 */
class CompressMethodTest {

    /**
     * Generates repetitive, highly compressible test data to ensure compression succeeds
     * (compressed size < original size) across all dictionary sizes (up to 64KB).
     *
     * @param size target byte size
     * @return test data
     */
    private static byte[] generateCompressibleData(int size) {
        StringBuilder sb = new StringBuilder();
        String samplePattern = """
                The quick brown fox jumps over the lazy dog. 0123456789!\s
                jLHA is a Java library for LHA archives supporting -lh0-, -lh1-, -lh2-, -lh3-, -lh4-, -lh5-, -lh6-, -lh7-.
                Repeating patterns allow LZSS sliding window and Huffman encoders to achieve high compression ratios.
                """;
        while (sb.length() < size) {
            sb.append(samplePattern);
        }
        byte[] raw = sb.toString().getBytes(StandardCharsets.UTF_8);
        return Arrays.copyOf(raw, size);
    }

    /**
     * Tests round-trip compression and decompression using {@link LhaOutputStream}, {@link LhaInputStream},
     * {@link LhaFile}, direct {@link CompressMethod#connectEncoder} / {@link CompressMethod#connectDecoder},
     * {@link LhaRetainedOutputStream}, and {@link LhaImmediateOutputStream}.
     */
    private void assertLhaRoundTrip(String method, byte[] originalData) throws Exception {
        // 1. LhaOutputStream -> LhaInputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LhaOutputStream out = new LhaOutputStream(baos)) {
            LhaHeader outHeader = new LhaHeader("file_" + method.replace("-", "") + ".txt");
            outHeader.setCompressMethod(method);
            out.putNextEntry(outHeader);
            out.write(originalData);
            out.closeEntry();
        }

        byte[] archiveBytes = baos.toByteArray();
        assertTrue(archiveBytes.length > 0, "Archive bytes should not be empty");

        // Decompress via LhaInputStream
        try (LhaInputStream lis = new LhaInputStream(new ByteArrayInputStream(archiveBytes))) {
            LhaHeader inHeader = lis.getNextEntry();
            assertNotNull(inHeader, "Header should not be null");
            if (!CompressMethod.LH0.equalsIgnoreCase(method) && !CompressMethod.LZ4.equalsIgnoreCase(method) && !CompressMethod.LHD.equalsIgnoreCase(method)) {
                assertEquals(method, inHeader.getCompressMethod(), "Compress method mismatch in header");
            }
            assertEquals(originalData.length, inHeader.getOriginalSize(), "Original size mismatch in header");

            byte[] extracted = lis.readAllBytes();
            assertArrayEquals(originalData, extracted, "Decompressed data via LhaInputStream did not match original");
        }

        // 2. Decompress via LhaFile
        File tempArchive = File.createTempFile("lha_test_", ".lzh");
        try {
            Files.write(tempArchive.toPath(), archiveBytes);
            try (LhaFile lhaFile = new LhaFile(tempArchive)) {
                LhaHeader[] entries = lhaFile.getEntries();
                assertEquals(1, entries.length, "Expected 1 entry in archive");
                if (!CompressMethod.LH0.equalsIgnoreCase(method) && !CompressMethod.LZ4.equalsIgnoreCase(method) && !CompressMethod.LHD.equalsIgnoreCase(method)) {
                    assertEquals(method, entries[0].getCompressMethod(), "Compress method mismatch in LhaFile entry");
                }
                assertEquals(originalData.length, entries[0].getOriginalSize(), "Original size mismatch in LhaFile entry");

                try (InputStream in = lhaFile.getInputStream(entries[0])) {
                    byte[] extracted = in.readAllBytes();
                    assertArrayEquals(originalData, extracted, "Decompressed data via LhaFile did not match original");
                }
            }
        } finally {
            tempArchive.delete();
        }

        // 3. Direct encoder -> decoder streams
        Properties props = LhaProperty.getProperties();
        ByteArrayOutputStream directCompressed = new ByteArrayOutputStream();
        try (OutputStream enc = CompressMethod.connectEncoder(directCompressed, method, props)) {
            enc.write(originalData);
            enc.flush();
        }

        byte[] rawCompressed = directCompressed.toByteArray();
        try (InputStream dec = CompressMethod.connectDecoder(new ByteArrayInputStream(rawCompressed), method, props, originalData.length)) {
            byte[] extracted = dec.readAllBytes();
            assertArrayEquals(originalData, extracted, "Direct stream decompressed data did not match original");
        }

        // 4. LhaRetainedOutputStream -> LhaFile
        File retainedArchive = File.createTempFile("lha_retained_", ".lzh");
        try {
            try (RandomAccessFile raf = new RandomAccessFile(retainedArchive, "rw");
                 LhaRetainedOutputStream lro = new LhaRetainedOutputStream(raf)) {
                LhaHeader retHeader = new LhaHeader("retained_" + method.replace("-", "") + ".txt");
                retHeader.setCompressMethod(method);
                retHeader.setOriginalSize(originalData.length);
                lro.putNextEntry(retHeader);
                lro.write(originalData);
                lro.closeEntry();
            }

            try (LhaFile lhaFile = new LhaFile(retainedArchive)) {
                LhaHeader[] entries = lhaFile.getEntries();
                assertEquals(1, entries.length);
                try (InputStream in = lhaFile.getInputStream(entries[0])) {
                    byte[] extracted = in.readAllBytes();
                    assertArrayEquals(originalData, extracted, "Decompressed data via LhaRetainedOutputStream did not match original");
                }
            }
        } finally {
            retainedArchive.delete();
        }

        // 5. LhaImmediateOutputStream -> LhaFile
        File immediateArchive = File.createTempFile("lha_immediate_", ".lzh");
        try {
            try (LhaImmediateOutputStream lio = new LhaImmediateOutputStream(immediateArchive.getPath())) {
                LhaHeader immHeader = new LhaHeader("immediate_" + method.replace("-", "") + ".txt");
                immHeader.setCompressMethod(method);
                immHeader.setOriginalSize(originalData.length);
                lio.putNextEntry(immHeader);
                lio.write(originalData);
                lio.closeEntry();
            }

            try (LhaFile lhaFile = new LhaFile(immediateArchive)) {
                LhaHeader[] entries = lhaFile.getEntries();
                assertEquals(1, entries.length);
                try (InputStream in = lhaFile.getInputStream(entries[0])) {
                    byte[] extracted = in.readAllBytes();
                    assertArrayEquals(originalData, extracted, "Decompressed data via LhaImmediateOutputStream did not match original");
                }
            }
        } finally {
            immediateArchive.delete();
        }
    }

    @Nested
    @DisplayName("Individual LH# compression method tests")
    class LhMethodTests {

        private final byte[] testData = generateCompressibleData(128 * 1024); // 128KB to exceed 64KB LH7 dictionary

        @Test
        @DisplayName("Test -lh0- (stored, no compression)")
        void testLh0() throws Exception {
            assertLhaRoundTrip(CompressMethod.LH0, testData);
        }

        @Test
        @DisplayName("Test -lh1- (4KB dictionary, dynamic Huffman)")
        void testLh1() throws Exception {
            assertLhaRoundTrip(CompressMethod.LH1, testData);
        }

        @Test
        @DisplayName("Test -lh2- (8KB dictionary, dynamic Huffman)")
        void testLh2() throws Exception {
            assertLhaRoundTrip(CompressMethod.LH2, testData);
        }

        @Test
        @DisplayName("Test -lh3- (8KB dictionary, static Huffman)")
        void testLh3() throws Exception {
            assertLhaRoundTrip(CompressMethod.LH3, testData);
        }

        @Test
        @DisplayName("Test -lh4- (4KB dictionary, static Huffman)")
        void testLh4() throws Exception {
            assertLhaRoundTrip(CompressMethod.LH4, testData);
        }

        @Test
        @DisplayName("Test -lh5- (8KB dictionary, static Huffman standard)")
        void testLh5() throws Exception {
            assertLhaRoundTrip(CompressMethod.LH5, testData);
        }

        @Test
        @DisplayName("Test -lh6- (32KB dictionary, static Huffman)")
        void testLh6() throws Exception {
            assertLhaRoundTrip(CompressMethod.LH6, testData);
        }

        @Test
        @DisplayName("Test -lh7- (64KB dictionary, static Huffman)")
        void testLh7() throws Exception {
            assertLhaRoundTrip(CompressMethod.LH7, testData);
        }

        @Test
        @DisplayName("Test -lzs- (2KB dictionary, 17-byte match)")
        void testLzs() throws Exception {
            assertLhaRoundTrip(CompressMethod.LZS, testData);
        }

        @Test
        @DisplayName("Test -lz5- (4KB dictionary, 18-byte match)")
        void testLz5() throws Exception {
            assertLhaRoundTrip(CompressMethod.LZ5, testData);
        }

        @Test
        @DisplayName("Test -lz4- (no compression)")
        void testLz4() throws Exception {
            assertLhaRoundTrip(CompressMethod.LZ4, testData);
        }
    }

    @Nested
    @DisplayName("Parameterized LH# compression method tests with various data patterns and sizes")
    class ParameterizedLhTests {

        @ParameterizedTest
        @ValueSource(strings = {
                CompressMethod.LH0,
                CompressMethod.LH1,
                CompressMethod.LH2,
                CompressMethod.LH3,
                CompressMethod.LH4,
                CompressMethod.LH5,
                CompressMethod.LH6,
                CompressMethod.LH7
        })
        @DisplayName("Test with small compressible data")
        void testSmallData(String method) throws Exception {
            byte[] smallData = generateCompressibleData(512);
            assertLhaRoundTrip(method, smallData);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                CompressMethod.LH0,
                CompressMethod.LH1,
                CompressMethod.LH2,
                CompressMethod.LH3,
                CompressMethod.LH4,
                CompressMethod.LH5,
                CompressMethod.LH6,
                CompressMethod.LH7
        })
        @DisplayName("Test with medium compressible data (16KB)")
        void testMediumData(String method) throws Exception {
            byte[] mediumData = generateCompressibleData(16 * 1024);
            assertLhaRoundTrip(method, mediumData);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                CompressMethod.LH0,
                CompressMethod.LH1,
                CompressMethod.LH2,
                CompressMethod.LH3,
                CompressMethod.LH4,
                CompressMethod.LH5,
                CompressMethod.LH6,
                CompressMethod.LH7
        })
        @DisplayName("Test with single byte data")
        void testSingleByteData(String method) throws Exception {
            byte[] singleByte = new byte[] { 0x42 };
            Properties props = LhaProperty.getProperties();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (OutputStream enc = CompressMethod.connectEncoder(baos, method, props)) {
                enc.write(singleByte);
                enc.flush();
            }
            try (InputStream dec = CompressMethod.connectDecoder(new ByteArrayInputStream(baos.toByteArray()), method, props, singleByte.length)) {
                byte[] extracted = dec.readAllBytes();
                assertArrayEquals(singleByte, extracted);
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {
                CompressMethod.LH0,
                CompressMethod.LH1,
                CompressMethod.LH2,
                CompressMethod.LH3,
                CompressMethod.LH4,
                CompressMethod.LH5,
                CompressMethod.LH6,
                CompressMethod.LH7
        })
        @DisplayName("Test header levels 0, 1, and 2 with each LH# method")
        void testHeaderLevels(String method) throws Exception {
            byte[] data = generateCompressibleData(8192);
            for (int level = 0; level <= 2; level++) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (LhaOutputStream out = new LhaOutputStream(baos)) {
                    LhaHeader header = new LhaHeader("level" + level + "_" + method.replace("-", "") + ".txt");
                    header.setHeaderLevel(level);
                    header.setCompressMethod(method);
                    out.putNextEntry(header);
                    out.write(data);
                    out.closeEntry();
                }

                try (LhaInputStream in = new LhaInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
                    LhaHeader entry = in.getNextEntry();
                    assertNotNull(entry);
                    assertEquals(level, entry.getHeaderLevel());
                    byte[] extracted = in.readAllBytes();
                    assertArrayEquals(data, extracted);
                }
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {
                CompressMethod.LH0,
                CompressMethod.LH1,
                CompressMethod.LH2,
                CompressMethod.LH3,
                CompressMethod.LH4,
                CompressMethod.LH5,
                CompressMethod.LH6,
                CompressMethod.LH7
        })
        @DisplayName("Test single-byte read, skip, and chunk read operations with each LH# method")
        void testStreamOperations(String method) throws Exception {
            byte[] data = generateCompressibleData(4096);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (LhaOutputStream out = new LhaOutputStream(baos)) {
                LhaHeader header = new LhaHeader("stream_ops_" + method.replace("-", "") + ".txt");
                header.setCompressMethod(method);
                out.putNextEntry(header);
                out.write(data);
                out.closeEntry();
            }

            try (LhaInputStream in = new LhaInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
                assertNotNull(in.getNextEntry());
                // Read first 100 bytes single-byte
                for (int i = 0; i < 100; i++) {
                    int b = in.read();
                    assertEquals(data[i] & 0xFF, b);
                }
                // Skip 200 bytes
                long skipped = in.skip(200);
                assertEquals(200, skipped);
                // Read next 50 bytes into array
                byte[] chunk = new byte[50];
                int readCount = in.read(chunk);
                assertEquals(50, readCount);
                byte[] expectedChunk = Arrays.copyOfRange(data, 300, 350);
                assertArrayEquals(expectedChunk, chunk);
            }
        }
    }

    @Test
    @DisplayName("Multi-entry archive containing all LH# methods")
    void testArchiveWithAllLhMethods() throws Exception {
        String[] methods = {
                CompressMethod.LH0,
                CompressMethod.LH1,
                CompressMethod.LH2,
                CompressMethod.LH3,
                CompressMethod.LH4,
                CompressMethod.LH5,
                CompressMethod.LH6,
                CompressMethod.LH7
        };

        byte[][] entryData = new byte[methods.length][];
        for (int i = 0; i < methods.length; i++) {
            entryData[i] = generateCompressibleData((i + 1) * 8 * 1024);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LhaOutputStream out = new LhaOutputStream(baos)) {
            for (int i = 0; i < methods.length; i++) {
                LhaHeader header = new LhaHeader("file_" + i + "_" + methods[i].replace("-", "") + ".txt");
                header.setCompressMethod(methods[i]);
                out.putNextEntry(header);
                out.write(entryData[i]);
                out.closeEntry();
            }
        }

        byte[] archiveBytes = baos.toByteArray();

        // Verify with LhaInputStream
        try (LhaInputStream in = new LhaInputStream(new ByteArrayInputStream(archiveBytes))) {
            for (int i = 0; i < methods.length; i++) {
                LhaHeader header = in.getNextEntry();
                assertNotNull(header, "Entry " + i + " should not be null");
                assertEquals("file_" + i + "_" + methods[i].replace("-", "") + ".txt", header.getPath());
                byte[] extracted = in.readAllBytes();
                assertArrayEquals(entryData[i], extracted, "Entry " + i + " (" + methods[i] + ") content mismatch");
            }
            assertNull(in.getNextEntry());
        }

        // Verify with LhaFile
        File tempArchive = File.createTempFile("lha_multi_all_", ".lzh");
        try {
            Files.write(tempArchive.toPath(), archiveBytes);
            try (LhaFile lhaFile = new LhaFile(tempArchive)) {
                LhaHeader[] entries = lhaFile.getEntries();
                assertEquals(methods.length, entries.length);
                for (int i = 0; i < methods.length; i++) {
                    assertEquals("file_" + i + "_" + methods[i].replace("-", "") + ".txt", entries[i].getPath());
                    try (InputStream in = lhaFile.getInputStream(entries[i])) {
                        byte[] extracted = in.readAllBytes();
                        assertArrayEquals(entryData[i], extracted, "LhaFile entry " + i + " (" + methods[i] + ") mismatch");
                    }
                }
            }
        } finally {
            tempArchive.delete();
        }
    }

    @Test
    @DisplayName("Verify CompressMethod dictionary size metadata")
    void testDictionarySize() {
        assertEquals(2048, CompressMethod.toDictionarySize(CompressMethod.LZS));
        assertEquals(4096, CompressMethod.toDictionarySize(CompressMethod.LZ5));
        assertEquals(4096, CompressMethod.toDictionarySize(CompressMethod.LH1));
        assertEquals(8192, CompressMethod.toDictionarySize(CompressMethod.LH2));
        assertEquals(8192, CompressMethod.toDictionarySize(CompressMethod.LH3));
        assertEquals(4096, CompressMethod.toDictionarySize(CompressMethod.LH4));
        assertEquals(8192, CompressMethod.toDictionarySize(CompressMethod.LH5));
        assertEquals(32768, CompressMethod.toDictionarySize(CompressMethod.LH6));
        assertEquals(65536, CompressMethod.toDictionarySize(CompressMethod.LH7));

        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toDictionarySize(CompressMethod.LH0));
        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toDictionarySize(CompressMethod.LZ4));
        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toDictionarySize(CompressMethod.LHD));
        assertThrows(NullPointerException.class, () -> CompressMethod.toDictionarySize(null));
        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toDictionarySize("-unknown-"));
    }

    @Test
    @DisplayName("Verify CompressMethod threshold metadata")
    void testThreshold() {
        assertEquals(2, CompressMethod.toThreshold(CompressMethod.LZS));
        assertEquals(3, CompressMethod.toThreshold(CompressMethod.LZ5));
        assertEquals(3, CompressMethod.toThreshold(CompressMethod.LH1));
        assertEquals(3, CompressMethod.toThreshold(CompressMethod.LH2));
        assertEquals(3, CompressMethod.toThreshold(CompressMethod.LH3));
        assertEquals(3, CompressMethod.toThreshold(CompressMethod.LH4));
        assertEquals(3, CompressMethod.toThreshold(CompressMethod.LH5));
        assertEquals(3, CompressMethod.toThreshold(CompressMethod.LH6));
        assertEquals(3, CompressMethod.toThreshold(CompressMethod.LH7));

        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toThreshold(CompressMethod.LH0));
        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toThreshold(CompressMethod.LZ4));
        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toThreshold(CompressMethod.LHD));
        assertThrows(NullPointerException.class, () -> CompressMethod.toThreshold(null));
        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toThreshold("-unknown-"));
    }

    @Test
    @DisplayName("Verify CompressMethod max match metadata")
    void testMaxMatch() {
        assertEquals(17, CompressMethod.toMaxMatch(CompressMethod.LZS));
        assertEquals(18, CompressMethod.toMaxMatch(CompressMethod.LZ5));
        assertEquals(60, CompressMethod.toMaxMatch(CompressMethod.LH1));
        assertEquals(256, CompressMethod.toMaxMatch(CompressMethod.LH2));
        assertEquals(256, CompressMethod.toMaxMatch(CompressMethod.LH3));
        assertEquals(256, CompressMethod.toMaxMatch(CompressMethod.LH4));
        assertEquals(256, CompressMethod.toMaxMatch(CompressMethod.LH5));
        assertEquals(256, CompressMethod.toMaxMatch(CompressMethod.LH6));
        assertEquals(256, CompressMethod.toMaxMatch(CompressMethod.LH7));

        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toMaxMatch(CompressMethod.LH0));
        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toMaxMatch(CompressMethod.LZ4));
        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toMaxMatch(CompressMethod.LHD));
        assertThrows(NullPointerException.class, () -> CompressMethod.toMaxMatch(null));
        assertThrows(IllegalArgumentException.class, () -> CompressMethod.toMaxMatch("-unknown-"));
    }
}
