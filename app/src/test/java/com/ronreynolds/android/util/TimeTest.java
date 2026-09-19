package com.ronreynolds.android.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Calendar;

/**
 * @author Copilot (with some minor tweaks by me)
 */
public class TimeTest {
    @Test
    public void roundDownToMinuteMillis_roundsDownCorrectly() {
        // Arrange: 12:34:56.789
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 34);
        cal.set(Calendar.SECOND, 56);
        cal.set(Calendar.MILLISECOND, 789);

        long input = cal.getTimeInMillis();

        // Expected: 12:34:00.000
        Calendar expected = Calendar.getInstance();
        expected.set(Calendar.HOUR_OF_DAY, 12);
        expected.set(Calendar.MINUTE, 34);
        expected.set(Calendar.SECOND, 0);
        expected.set(Calendar.MILLISECOND, 0);

        // Act
        long result = Time.roundDownToMinuteMillis(input);

        // Assert
        assertThat(result).isEqualTo(expected.getTimeInMillis());
    }

    @Test
    public void roundDownToMinuteMillis_whenAlreadyOnBoundary_returnsSameValue() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long input = cal.getTimeInMillis();
        long result = Time.roundDownToMinuteMillis(input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    public void getMillisTillNextMinute_returnsFutureTimeWithinReasonableBounds() {
        long now = System.currentTimeMillis();
        long next = Time.getMillisTillNextMinute();

        // next minute must be >= now and < now + 60,000 ms
        assertThat(next)
                .isGreaterThanOrEqualTo(now)
                .isLessThan(now + 60_000);
    }

    @Test
    public void getNowTimestamp_returnsFormattedString() {
        String ts = Time.getNowTimestamp();
        assertThat(ts).matches("\\d{2}:\\d{2}:\\d{2}"); // HH:mm:ss
    }
}
