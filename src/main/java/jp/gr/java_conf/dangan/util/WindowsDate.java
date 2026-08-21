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

package jp.gr.java_conf.dangan.util;

import java.util.Date;


/**
 * A derived class of {@link Date} that handles Windows FILETIME format information.<br>
 * FILETIME is a 64-bit value holding the elapsed time from January 1, 1601, 00:00:00
 * in 100-nanosecond units.<br>
 * In this class, when treating FILETIME as a {@code long} (64-bit value),
 * it is basically treated as unsigned.<br>
 * If you want to handle time before January 1, 1601, 00:00:00,
 * use {@link #WindowsDate(Date)} or {@link #setTime(long)}.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: WindowsDate.java,v $
 * Revision 1.1  2002/12/05 00:00:00  dangan
 * [maintenance]
 *     Fixed typo in javadoc comments.
 *     Source cleanup.
 *
 * Revision 1.0  2002/08/05 00:00:00  dangan
 * add to version control
 * [bug fix]
 *     In setter methods, when attempting to set an out-of-range time and throwing an exception,
 *     the time was not properly restored.
 *     Fixed incorrect time range in checkRange.
 * [maintenance]
 *     Eliminated tabs.
 *     Updated license text.
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.1 $
 */
public class WindowsDate extends Date implements Cloneable {

    /**
     * The numerical value showing the time difference between the FILETIME format data and
     * the time format obtained by {@link Date#getTime()} in 100-nanosecond units.
     * Note that leap seconds are not taken into consideration.
     */
    public static final long TIME_DIFFERENCE = 0x19DB1DED53E8000L;

    /**
     * Used to hold time in nanosecond units that cannot be held in {@link Date}.
     */
    private int NanoSecounds;

    /**
     * Default constructor.
     * Constructs a {@link WindowsDate} with current time information.
     * Since nanosecond unit time cannot be obtained, it is set to 0.
     *
     * @exception IllegalArgumentException if the current time is outside the range that can be represented in FILETIME format.
     */
    public WindowsDate() {
        super();
        this.NanoSecounds = 0;

        this.checkRange();
    }

    /**
     * Constructs a {@link WindowsDate} representing the time indicated by {@code date}.<br>
     * If {@code date} is an instance of {@link WindowsDate},
     * the information in nanosecond units is also copied, but otherwise
     * 0 is set to the information in nanosecond units.
     *
     * @param date the {@link Date} object containing the time information on which the newly constructed {@link WindowsDate} is based
     * @exception IllegalArgumentException if the current time is outside the range that can be represented in FILETIME format.
     */
    public WindowsDate(Date date) {
        super(date.getTime());
        if (date instanceof WindowsDate) {
            this.NanoSecounds = ((WindowsDate) date).NanoSecounds;
        } else {
            this.NanoSecounds = 0;
            this.checkRange();
        }
    }

    /**
     * Constructs a new {@link WindowsDate} from unsigned 64-bit FILETIME format time information.
     *
     * @param time FILETIME format time information
     */
    public WindowsDate(long time) {
        super(0 <= time ? (time - WindowsDate.TIME_DIFFERENCE) / 10000L
                        : ((time >>> 1) - (WindowsDate.TIME_DIFFERENCE >>> 1)) / 5000L);

        this.NanoSecounds = (int) ((time >>> 1) % 5000L * 2 + (time & 1)) * 100;
    }

    /**
     * Returns a copy of this object.
     *
     * @return a clone of this {@link WindowsDate} object
     */
    @Override
    public Object clone() {
        return new WindowsDate(this);
    }

    /**
     * Sets the year represented by this {@link WindowsDate} to the value obtained by adding 1900 to the value specified by {@code year}.<br>
     * This method exists solely for range checking.
     *
     * @param year the year value such that adding 1900 represents the Western calendar year
     * @exception IllegalArgumentException if changing to {@code year} results in a time outside the range that can be handled in FILETIME format
     * @deprecated
     */
    @Override
    public void setYear(int year) {
        long temp = this.getTime();

        try {
            super.setYear(year);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * Sets the month represented by this {@link WindowsDate} to the value specified by {@code month}.<br>
     * This method exists solely for range checking.
     *
     * @param month month value where 0 represents January and 1 represents February
     * @exception IllegalArgumentException if changing to {@code month} results in a time outside the range that can be handled in FILETIME format
     * @deprecated
     */
    @Override
    public void setMonth(int month) {
        long temp = this.getTime();

        try {
            super.setMonth(month);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * Sets the day of the month represented by this {@link WindowsDate} to the value specified by {@code date}.<br>
     * This method exists solely for range checking.
     *
     * @param date day value where 1 represents the 1st and 2 represents the 2nd
     * @exception IllegalArgumentException if changing to {@code date} results in a time outside the range that can be handled in FILETIME format
     * @deprecated
     */
    @Override
    public void setDate(int date) {
        long temp = this.getTime();

        try {
            super.setDate(date);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * Sets the hour of the day represented by this {@link WindowsDate} to the value specified by {@code hours}.<br>
     * This method exists solely for range checking.
     *
     * @param hours the hour value
     * @exception IllegalArgumentException if changing to {@code hours} results in a time outside the range that can be handled in FILETIME format
     * @deprecated
     */
    @Override
    public void setHours(int hours) {
        long temp = this.getTime();

        try {
            super.setHours(hours);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * Sets the minute within an hour represented by this {@link WindowsDate} to the value specified by {@code minutes}.<br>
     * This method exists solely for range checking.
     *
     * @param minutes the minute value
     * @exception IllegalArgumentException if changing to {@code minutes} results in a time outside the range that can be handled in FILETIME format
     * @deprecated
     */
    @Override
    public void setMinutes(int minutes) {
        long temp = this.getTime();

        try {
            super.setMinutes(minutes);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * Sets the seconds within a minute represented by this {@link WindowsDate} to the value specified by {@code seconds}.<br>
     * This method exists solely for range checking.
     *
     * @param seconds number of seconds
     * @exception IllegalArgumentException if changing to {@code seconds} results in a time outside the range that can be handled in FILETIME format
     * @deprecated
     */
    @Override
    public void setSeconds(int seconds) {
        long temp = this.getTime();

        try {
            super.setSeconds(seconds);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * Sets the time represented by this {@link WindowsDate} to the time when {@code time} milliseconds have elapsed since January 1, 1970, 00:00:00 GMT.<br>
     * This method exists solely for range checking.
     *
     * @param time elapsed milliseconds from January 1, 1970, 00:00:00 GMT
     * @exception IllegalArgumentException if {@code time} indicates a time outside the range that can be handled in FILETIME format
     */
    @Override
    public void setTime(long time) {
        long temp = this.getTime();

        try {
            super.setTime(time);
            this.checkRange();
        } catch (IllegalArgumentException exception) {
            this.setTime(temp);
            throw exception;
        }
    }

    /**
     * Sets FILETIME format time information to this {@link WindowsDate}.
     *
     * @param time FILETIME format time information
     */
    public void setWindowsTime(long time) {
        super.setTime(0 <= time ? (time - WindowsDate.TIME_DIFFERENCE) / 10000L
                                : ((time >>> 1) - (WindowsDate.TIME_DIFFERENCE >>> 1)) / 5000L);

        this.NanoSecounds = (int) ((time >>> 1) % 5000L * 2 + (time & 1)) * 100;
    }

    /**
     * Gets the time information represented by this {@link WindowsDate} in FILETIME format.
     *
     * @return the value in FILETIME format
     */
    public long getWindowsTime() {
        return (super.getTime() * 10000L + WindowsDate.TIME_DIFFERENCE + this.NanoSecounds / 100);
    }

    /**
     * Determines whether this {@link WindowsDate} is within the range of time that can be represented in FILETIME format. Still incomplete.
     *
     * @exception IllegalArgumentException if this {@link WindowsDate} indicates a time outside the range that can be handled in FILETIME format
     */
    private void checkRange() {
        long time = super.getTime();
        if (!(0xFFFCAE8C71E60F9BL <= time && time <= 0x000683218A10A8CBL))
            throw new IllegalArgumentException("outside of range of Windows FILETIME format. ");
    }
}
