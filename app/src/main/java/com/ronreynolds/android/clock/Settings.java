package com.ronreynolds.android.clock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * a app-specific wrapper around Settings; static so it can be accessed by all once it's initialized
 * within MainActivity.onCreate
 */
public class Settings {
    private static final String LOG_TAG = "Settings";
    private static SharedPreferences preferences;

    /**
     * initialize the Settings; otherwise you get only default values back
     */
    @SuppressWarnings("deprecation")    // required for API-23 (deprecated for API-29+)
    public static void init(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public static boolean getBoolean(String key, boolean defaultVal) {
        boolean value = preferences != null ? preferences.getBoolean(key, defaultVal) : defaultVal;
        Log.d(LOG_TAG, key + " = " + value);
        return value;
    }

    public static int getInteger(String key, int defaultValue) {
        String valueAsString = getString(key, null);
        if (valueAsString == null) {
            Log.w(LOG_TAG, "unable to find value for key " + key);
            return defaultValue;
        }
        try {
            return Integer.parseInt(valueAsString);
        } catch (Exception fail) {
            Log.e(LOG_TAG, "failure to convert " + valueAsString + " to int", fail);
            return defaultValue;
        }
    }

    public static String getString(String key, String defaultValue) {
        String value = preferences != null ? preferences.getString(key, defaultValue) : defaultValue;
        Log.d(LOG_TAG, key + " = " + value);
        return value;
    }

    // special helpers to keep keys in one place
    public static boolean is24HrTime() {
        return getBoolean("is_24hr_time", true);
    }

    public static long getPeriodMillis() {
        return TimeUnit.MINUTES.toMillis(getInteger("minute_period", 1));
    }

    public static float getPositiveFloat(String key, float defaultVal) {
        if (preferences == null) {
            return defaultVal;
        }
        float value;
        try {
            value = Float.parseFloat(getString(key, String.valueOf(defaultVal)));
        } catch (Exception badData) {
            value = defaultVal;
        }
        return Math.max(0.1f, value);
    }

}
