package com.ronreynolds.android.clock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.ronreynolds.android.util.Logs;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * a app-specific wrapper around Settings; static so it can be accessed by all once it's initialized
 * within MainActivity.onCreate
 */
public class Settings {
    public interface SettingObserver {
        void onPeriodChange();

        void onClockTypeChange();
    }

    private static final String LOG_TAG = "Settings";
    private static final String KEY_24HR_TIME = "is_24hr_time";
    private static final String KEY_MINUTE_PERIOD = "minute_period";
    // don't be the only reason the observers don't get GCed.
    private static SettingObserver observer;
    private static SharedPreferences preferences;

    /**
     * version used for single observer; dropping multi-observer pattern for now
     */
    public static void setObserver(SettingObserver obs) {
        observer = obs;
    }

    /**
     * initialize the Settings; otherwise you get only default values back
     */
    @SuppressWarnings("deprecation")    // required for API-23 (deprecated for API-29+)
    public static void init(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> prefMap = preferences.getAll();
        Logs.d(LOG_TAG, "init-preferences:" + prefMap);
    }

    public static boolean getBoolean(String key, boolean defaultVal) {
        boolean value = preferences != null ? preferences.getBoolean(key, defaultVal) : defaultVal;
        Logs.d(LOG_TAG, key + " = " + value);
        return value;
    }

    public static int getInteger(String key, int defaultValue) {
        int value = preferences != null ? preferences.getInt(key, defaultValue) : defaultValue;
        Logs.d(LOG_TAG, key + " = " + value);
        return value;
    }

    public static String getString(String key, String defaultValue) {
        String value = preferences != null ? preferences.getString(key, defaultValue) : defaultValue;
        Logs.d(LOG_TAG, key + " = " + value);
        return value;
    }

    // special helpers to keep keys in one place
    public static boolean is24HrTime() {
        return getBoolean(KEY_24HR_TIME, true);
    }

    public static long getPeriodMillis() {
        return TimeUnit.MINUTES.toMillis(getInteger(KEY_MINUTE_PERIOD, 1));
    }

    public static int getPeriodMinutes() {
        return getInteger(KEY_MINUTE_PERIOD, 1);
    }

    public static void setPeriodMinutes(int minutes) {
        preferences.edit().putInt(KEY_MINUTE_PERIOD, minutes).apply();
        if (observer != null) {
            Logs.d(LOG_TAG, "setPeriodMinutes; notifying " + observer);
            observer.onPeriodChange();
        }
    }

    public static void setUse24HourTime(boolean h24) {
        preferences.edit().putBoolean(KEY_24HR_TIME, h24).apply();
        if (observer != null) {
            Logs.d(LOG_TAG, "setUse24HourTime; notifying " + observer);
            observer.onClockTypeChange();
        }
    }
}
