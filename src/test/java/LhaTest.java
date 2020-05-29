/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */



import java.io.File;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jp.gr.java_conf.dangan.util.lha.LhaFile;
import jp.gr.java_conf.dangan.util.lha.LhaHeader;
import jp.gr.java_conf.dangan.util.lha.LhaInputStream;


/**
 * LhaArchiveTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/10 umjammer initial version <br>
 */
class LhaTest {

    @Test
    void test0() throws Exception {
        LhaInputStream lis = new LhaInputStream(LhaTest.class.getResourceAsStream("/test.lzh"));
        LhaHeader header;
        int c = 0;
        while ((header = lis.getNextEntry()) != null) {
            System.err.println(header.getPath());
            c++;
        }
        assertEquals(7, c);
    }

    @Test
    void test1() throws Exception {
        LhaFile file = new LhaFile(new File("src/test/resources/test.lzh"));
        int c = 0;
        for (LhaHeader header : file.getEntries()) {
            System.err.println(header.getPath());
            c++;
        }
        assertEquals(7, c);
    }
}

/* */
