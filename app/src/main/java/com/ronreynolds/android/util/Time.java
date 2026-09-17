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

    public static long getMillisTillNextMinute() {
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

    public static String getTimeNow() {
        synchronized (TIMESTAMP_FORMAT) {
            return TIMESTAMP_FORMAT.format(new Date());
        }
    }
}
