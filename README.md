[![](https://jitpack.io/v/umjammer/jbinhex.svg)](https://jitpack.io/#umjammer/jbinhex)

# LHA for Java

## Overview

LHA compatible archiving library works with Java.

## Change Log

 * version 0.06 -- 2002-12-11

     * Simplified configuration file

 * version 0.05 -- 2002-05-17
     * [fix] Due to an error in the decompression routine, some files compressed with `-lh4-`,` -lh5-`, `-lh6-` and` -lh7-` could not be decompressed properly.

     * [fix] Unable to decompress some files compressed with `-lh3-` due to a mistake in the decompression routine.

 * version 0.04 -- 2002-05-10
    - [add] Introduces a mechanism to improve the compression ratio by devising the Huffman compression unit. (Not used by default)
    - [fix] In LZSS compression routines, did not compress when finding the longest match.
    - [fix] The search routine used for the LZSS compression routine did not slide the text window.
    - [fix] The configuration file was not able to compress and decompress `-lh4-`,` -lh6-` and `-lh7-`

 * version 0.03 -- 2002-04-15
    - [add] Compress and decompress `-lzs-`,` -lz5-`, `-lh1-`,` -lh2-` and `-lh3-`
    - [fix] In the LZSS compression routine, if the longest match was found near the end of the buffer, the buffer outside was accessed and an `ArrayIndexOutOfBoundsException` was thrown.

 * version 0.01 -- 2001-06
     - First edition
     - [add] `-lh4-`,` -lh5-`, `-lh6-`,` -lh7-` compression and decompression