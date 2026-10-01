package com.ronreynolds.android.clock;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import com.ronreynolds.android.util.Logs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * centralizes app-wide data and behavior
 */
public class MainApplication extends Application {
    private static final Object LOG_LOCK = new Object();
    private static final AtomicInteger nextLogOrdinal = new AtomicInteger();
    private static final int MAX_LOG_ORDINAL = 5;   // keep 5 log files (0..4)
    private static final long MAX_LOG_BYTES = 1 << 17;  // 128kB

    private final String LOG_TAG = getClass().getSimpleName();
    private final AtomicReference<File> logFile = new AtomicReference<>();
    // service refs so we only have to get them once for the whole app
    private AlarmManager alarmManager;
    private AudioManager audioManager;
    // constants to only be evaluated once
    private String appNameAndVersion;
    private File logDir;    // should be /sdcard/Android/data/com.ronreynolds.android.clock/files/
    // a way to route logs into the log-file (kept so we can unregister in case of disaster)
    private Logs.LogObserver logger;

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        logDir = getExternalFilesDir(null);
        logFile.set(findLastLog());

        // write our logs to local file for debugging beyond the lifetime of the app
        logger = event -> {
            synchronized (LOG_LOCK) {
                File currentLog = logFile.get();
                try (FileOutputStream fos = new FileOutputStream(currentLog, true)) {
                    fos.write((event.formatLine() + "\n").getBytes(StandardCharsets.UTF_8));
                } catch (IOException iox) {
                    Logs.removeObserver(logger);    // logging to file is broken so disable it
                    Logs.wtf(LOG_TAG, iox.toString(), iox);
                }
                if (currentLog.length() >= MAX_LOG_BYTES) {
                    Logs.i(LOG_TAG, "rotating log due to size");
                    File nextLogFile = nextLogFile();
                    if (nextLogFile.exists()) {
                        if (!nextLogFile.delete()) {
                            Logs.wtf(LOG_TAG, "failed to delete next log file '"
                                    + nextLogFile.getAbsolutePath() + "'; keep appending to current");
                        } else {
                            logFile.set(nextLogFile);
                        }
                    }
                }
            }
        };
        Logs.addObserver(logger);

        Logs.i(LOG_TAG, "onCreate() - " + getAppNameAndVersion() +
                "; logFile:" + logFile.get().getAbsolutePath());

        // initialize the global application settings (this can be quite slow)
        Settings.init(this);
        // a few things we'd like to record once
        startupLogs();
    }

    public String getAppNameAndVersion() {
        // not exactly AtomicRef clean but good enough while supporting API-23
        if (appNameAndVersion == null) {
            appNameAndVersion = getApplicationInfo().loadLabel(getPackageManager())
                    + " v" + BuildConfig.VERSION_NAME + "(" + BuildConfig.BUILD_TYPE + ")";
        }
        return appNameAndVersion;
    }

    public AlarmManager getAlarmManager() {
        return alarmManager;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    private void startupLogs() {
        // not much to log at this point (good sign?)
    }

    private File findLastLog() {
        Pattern logFileNamePattern = Pattern.compile("app-(\\d+).log");
        File[] logs = logDir.listFiles(
                (dir, name) -> logFileNamePattern.matcher(name).matches());
        if (logs != null && logs.length > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Arrays.sort(logs, Comparator.comparingLong(File::lastModified));
            } else {
                Arrays.sort(logs, (file1, file2) -> Long.compare(file1.lastModified(), file2.lastModified()));
            }
            File newest = logs[logs.length - 1];
            Matcher mat = logFileNamePattern.matcher(newest.getName());
            if (mat.matches()) {
                String ordinal = mat.group(1);
                if (ordinal != null) {
                    nextLogOrdinal.set(Integer.parseInt(ordinal));
                    return newest;
                } else {
                    Logs.wtf(LOG_TAG, "found null ordinal in file-name '" + newest.getName() + "'");
                }
            } else {
                Logs.wtf(LOG_TAG, "file name no longer matches regular expression - '" + newest.getName() + "'");
            }
        }
        nextLogOrdinal.set(0);
        return nextLogFile();
    }

    @SuppressLint("DefaultLocale")
    private File nextLogFile() {
        // increment the ordinal before generating the file name
        int newOrdinal;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newOrdinal = nextLogOrdinal.getAndUpdate(ord -> (ord + 1) % MAX_LOG_ORDINAL);
        } else {
            int currentOrdinal;
            do {
                currentOrdinal = nextLogOrdinal.get();
                newOrdinal = (currentOrdinal + 1) % MAX_LOG_ORDINAL;
            } while (nextLogOrdinal.compareAndSet(currentOrdinal, newOrdinal));
        }
        return new File(logDir, String.format("app-%d.log", newOrdinal));
    }

    public File getCurrentLogFile(boolean rotate) {
        File oldLogFile = logFile.get();
        if (rotate) {
            Logs.i(LOG_TAG, "rotating log file due to upload - " + logFile.get().getAbsolutePath());
            // last writer wins; KISS
            logFile.set(nextLogFile());
        }
        return oldLogFile;
    }
}