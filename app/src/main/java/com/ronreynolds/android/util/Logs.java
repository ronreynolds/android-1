package com.ronreynolds.android.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
//import java.util.function.Supplier; not supported at API-23

/**
 * wrapper around Android Logs that lets us add an observer;
 * also supports the message-supplier callback paradigm (like slf4j)
 */
public final class Logs {
    private static final List<LogObserver> observers = new ArrayList<>();

    public interface LogObserver {
        void onLog(int level, String context, String message, Throwable ex);
    }

    public static void addObserver(LogObserver observer) {
        observers.add(observer);
    }

    public static void removeObserver(LogObserver observer) {
        observers.remove(observer);
    }

    public static char levelToChar(int level) {
        switch (level) {
            case Log.VERBOSE: return 'V';
            case Log.DEBUG: return 'D';
            case Log.INFO: return 'I';
            case Log.WARN: return 'W';
            case Log.ERROR: return 'E';
            case Log.ASSERT: return 'A';
        }
        throw new IllegalArgumentException("invalid level " + level);
    }

    public static void v(String context, String message) {
        v(context, message, null);
    }

    public static void v(String context, StringSupplier msg) {
        if (hasVerbose(context)) {
            v(context, msg.get());
        }
    }

    public static void v(String context, String message, Throwable ex) {
        Log.v(context, message, ex);
        onVerbose(context, message, ex);
    }

    public static void d(String context, String message) {
        d(context, message, null);
    }

    public static void d(String context, StringSupplier msg) {
        if (hasDebug(context)) {
            d(context, msg.get());
        }
    }

    public static void d(String context, String message, Throwable ex) {
        Log.d(context, message, ex);
        onDebug(context, message, ex);
    }

    public static void i(String context, String message) {
        i(context, message, null);
    }

    public static void i(String context, StringSupplier msg) {
        if (hasInfo(context)) {
            i(context, msg.get());
        }
    }

    public static void i(String context, String message, Throwable ex) {
        Log.i(context, message, ex);
        onInfo(context, message, ex);
    }

    public static void w(String context, String message) {
        w(context, message, null);
    }

    public static void w(String context, StringSupplier msg) {
        if (hasWarn(context)) {
            e(context, msg.get());
        }
    }

    public static void w(String context, String message, Throwable ex) {
        Log.w(context, message, ex);
        onWarn(context, message, ex);
    }

    public static void e(String context, String message) {
        e(context, message, null);
    }

    public static void e(String context, StringSupplier msg) {
        if (hasError(context)) {
            e(context, msg.get());
        }
    }

    public static void e(String context, String message, Throwable ex) {
        Log.e(context, message, ex);
        onError(context, message, ex);
    }

    public static void wtf(String context, String message) {
        wtf(context, message, null);
    }

    public static void wtf(String context, StringSupplier msg) {
        if (hasAssert(context)) {   // this is probably always true
            wtf(context, msg.get());
        }
    }

    public static void wtf(String context, String message, Throwable ex) {
        Log.wtf(context, message, ex);
        onAssert(context, message, ex);
    }

    public static boolean hasVerbose(String context) {
        return Log.isLoggable(context, Log.VERBOSE);
    }

    public static boolean hasDebug(String context) {
        return Log.isLoggable(context, Log.DEBUG);
    }

    public static boolean hasInfo(String context) {
        return Log.isLoggable(context, Log.INFO);
    }

    public static boolean hasWarn(String context) {
        return Log.isLoggable(context, Log.WARN);
    }

    public static boolean hasError(String context) {
        return Log.isLoggable(context, Log.ERROR);
    }

    public static boolean hasAssert(String context) {
        return Log.isLoggable(context, Log.ASSERT);
    }

    private static void onVerbose(String context, String msg, Throwable ex) {
        onLog(Log.VERBOSE, context, msg, ex);
    }

    private static void onDebug(String context, String msg, Throwable ex) {
        onLog(Log.DEBUG, context, msg, ex);
    }

    private static void onInfo(String context, String msg, Throwable ex) {
        onLog(Log.INFO, context, msg, ex);
    }

    private static void onWarn(String context, String msg, Throwable ex) {
        onLog(Log.WARN, context, msg, ex);
    }

    private static void onError(String context, String msg, Throwable ex) {
        onLog(Log.ERROR, context, msg, ex);
    }

    private static void onAssert(String context, String msg, Throwable ex) {
        onLog(Log.ASSERT, context, msg, ex);
    }

    private static void onLog(int level, String context, String msg, Throwable ex) {
        for (LogObserver observer : observers) {
            observer.onLog(level, context, msg, ex);
        }
    }
}
