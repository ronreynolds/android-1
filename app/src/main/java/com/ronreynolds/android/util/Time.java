package com.ronreynolds.android.util;

import android.os.Build;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * simple util for dealing with time/durations
 */
public class Time {
    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.US);

    /**
     * @return the epoch-millis of the next minute's start (rounds up if you're at the 0.0 second)
     */
    public static long getMillisOfNextMinute() {
        // before Oreo java-time wasn't available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime nextMinute = LocalDateTime.now().withSecond(0).withNano(0).plusMinutes(1);
            return nextMinute.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else {
            Calendar nextMinute = Calendar.getInstance();
            // Truncate seconds and millis and add 1 minute
            nextMinute.set(Calendar.SECOND, 0);
            nextMinute.set(Calendar.MILLISECOND, 0);
            nextMinute.add(Calendar.MINUTE, 1);
            return nextMinute.getTimeInMillis();
        }
    }

    /**
     * given an epochMillis value round it down to the HH:mm:00 of the current minute
     * @param epochMillis the value to be rounded down to the 0.0 second boundary of its minute
     * @return the epoch-millis of the most recent HH:mm:00.0 instant
     */
    public static long roundDownToMinuteMillis(long epochMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epochMillis);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * get "now" in HH:mm:ss format (in whatever the default timezone is)
     * @return an HH:mm:ss format string of the current time in the system default timezone
     */
    public static String getNowTimestamp() {
        synchronized (TIMESTAMP_FORMAT) {
            return TIMESTAMP_FORMAT.format(new Date());
        }
    }
}
