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

package jp.gr.java_conf.dangan.util;

import java.util.Date;


/**
 * A derived class of {@link Date} that handles MS-DOS format time information.<br>
 * The data is a 4-byte value and is stored in little-endian because MS-DOS mainly operated
 * on Intel x86 CPUs.<br>
 * The format is as follows.
 *
 * <pre>
 * +---------------+---------------++---------------+---------------+
 * | Date high-byte| Date low-byte || Time high-byte| Time low-byte |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-++-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |7|6|5|4|3|2|1|0|7|6|5|4|3|2|1|0||7|6|5|4|3|2|1|0|7|6|5|4|3|2|1|0|
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-++-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Year-7bit  |Mo-4bit|Day-5bit ||Hour-5bit|Minute-6bit|Sec-5bit |
 * +-------------+-------+---------++---------+-----------+---------+
 * </pre>
 *
 * <li>Year represents 1980 to 2107 with 0 to 127. There is information that some
 * Windows system APIs only support up to the year 2099.</li>
 * <li>Month represents January to December with 1 to 12. Note that it is not 0 to 11.</li>
 * <li>Day represents 1st to 31st with 1 to 31. Note that it is not 0 to 30.</li>
 * <li>Hour represents 0 to 23 hours with 0 to 23.</li>
 * <li>Minute represents 0 to 59 minutes with 0 to 59.</li>
 * <li>Second represents 0 to 58 seconds with 0 to 29. Because the number of bits is insufficient
 * for second information, the minimum unit is 2 seconds instead of 1 second.</li>
 *
 * <pre>
 * -- revision history --
 * $Log: MsdosDate.java,v $
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     Fixed typo in javadoc comments.
 *     Source cleanup.
 *
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     In setTime(), recorded with precision exceeding the 2-second limit precision of ftime.
 * [maintenance]
 *     Eliminated tabs.
 *     Updated license text.
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
@SuppressWarnings("deprecation")
public class MsdosDate extends Date implements Cloneable {

    /**
     * Constructs an {@link MsdosDate} representing the time indicated by {@code date}.<br>
     * Detailed precision information that cannot be represented in MS-DOS format time information
     * is ignored, and the minimum time unit is 2 seconds, which is the minimum unit of MS-DOS format time information,
     * instead of 1 millisecond of {@link Date}.
     *
     * @param date the {@link Date} object containing the time information on which the newly constructed {@link MsdosDate} is based
     * @throws IllegalArgumentException if {@code date} indicates a time outside the range that can be handled in MS-DOS time format
     */
    public MsdosDate(Date date) {
        super((date.getTime() / 2000L) * 2000L);
        this.checkRange();
    }

    /**
     * Constructs a new {@link MsdosDate} from MS-DOS format time information.
     *
     * @param time MS-DOS format time information
     */
    public MsdosDate(int time) {
        super(((time >> 25) & 0x7F) + 80,
                ((time >> 21) & 0x0F) - 1,
                (time >> 16) & 0x1F,
                (time >> 11) & 0x1F,
                (time >> 5) & 0x3F,
                (time << 1) & 0x3F); // deprecated

        this.checkRange();
    }

    /**
     * Returns a copy of this object.
     *
     * @return a clone of this {@link MsdosDate} object
     */
    @Override
    public Object clone() {
        return new MsdosDate(this);
    }

    /**
     * Sets the year represented by this {@link MsdosDate} to the value obtained by adding 1900 to the value specified by {@code year}.<br>
     * This method exists solely for range checking.
     *
     * @param year the year value such that adding 1900 represents the Western calendar year
     * @throws IllegalArgumentException if {@code year} indicates a time outside the range that can be handled in MS-DOS time format
     * @deprecated
     */
    @Override
    public void setYear(int year) {
        if (year < 80 || 207 < year) {
            throw new IllegalArgumentException("out of MS-DOS time format range.");
        } else {
            super.setYear(year); // deprecated
        }
    }

    /**
     * Sets the time represented by this {@link MsdosDate} to the time when {@code time} milliseconds have elapsed since January 1, 1970, 00:00:00 GMT.<br>
     * Detailed precision information that cannot be represented in MS-DOS format time information
     * is ignored, and the minimum time unit is 2 seconds, which is the minimum unit of MS-DOS format time information,
     * instead of 1 millisecond of {@link Date}.
     *
     * @param time elapsed milliseconds from January 1, 1970, 00:00:00 GMT
     * @throws IllegalArgumentException if {@code time} indicates a time outside the range that can be handled in MS-DOS time format
     */
    @Override
    public void setTime(long time) {
        int year = (new Date(time)).getYear();
        if (year < 80 || 207 < year) {
            throw new IllegalArgumentException("out of MS-DOS time format range.");
        } else {
            super.setTime((time / 2000L) * 2000L);
        }
    }

    /**
     * Sets MS-DOS format time information to this {@link MsdosDate}.
     *
     * @param time MS-DOS format time information
     */
    public void setMsdosTime(int time) {
        Date date = new Date(((time >> 25) & 0x7F) + 80,
                ((time >> 21) & 0x0F) - 1,
                (time >> 16) & 0x1F,
                (time >> 11) & 0x1F,
                (time >> 5) & 0x3F,
                (time << 1) & 0x3F);

        this.setTime(date.getTime());
    }

    /**
     * Gets the time information represented by this {@link MsdosDate} in MS-DOS time format.
     *
     * @return the value in MS-DOS time format
     */
    public int getMsdosTime() {
        return ((super.getYear() - 80) << 25)
                | ((super.getMonth() + 1) << 21)
                | (super.getDate() << 16)
                | (super.getHours() << 11)
                | (super.getMinutes() << 5)
                | (super.getSeconds() >> 1);
    }

    /**
     * Determines whether this {@link MsdosDate} is within the time range that can be represented in MS-DOS time format.
     *
     * @throws IllegalArgumentException if this {@link MsdosDate} indicates a time outside the range that can be handled in MS-DOS time format
     */
    private void checkRange() {
        int year = this.getYear();
        if (year < 80 || 207 < year)
            throw new IllegalArgumentException("out of MS-DOS time format range.");
    }
}
