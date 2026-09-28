package com.ronreynolds.android.clock;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import com.ronreynolds.android.util.Logs;
import com.ronreynolds.android.util.Time;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * centralizes app-wide data and behavior
 */
public class MainApplication extends Application {
    private static final AtomicInteger logCounter = new AtomicInteger();
    private static final String logPrefix = "app-" + Time.getFSSafeDatetime(System.currentTimeMillis()) + "-";

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
        logFile.set(newUniqueLogFile());

        Context appContext = this.getApplicationContext();
        // write our logs to local file for debugging beyond the lifetime of the app
        logger = event -> {
            File currentLog = logFile.get();
            synchronized (currentLog) {
                try (FileOutputStream fos = new FileOutputStream(currentLog, true)) {
                    fos.write((event.formatLine() + "\n").getBytes(StandardCharsets.UTF_8));
                } catch (IOException iox) {
                    Logs.removeObserver(logger);    // logging to file is broken so disable it
                    Logs.wtf(LOG_TAG, iox.toString(), iox);
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
                    + " v" + BuildConfig.VERSION_NAME;
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
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Logs.i(LOG_TAG, "max volume:" + maxVolume);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Logs.i(LOG_TAG, "volume 1 dB:" + audioManager.getStreamVolumeDb(
                    AudioManager.STREAM_MUSIC, 1, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER));
            Logs.i(LOG_TAG, "max-volume dB:" + audioManager.getStreamVolumeDb(
                    AudioManager.STREAM_MUSIC, maxVolume, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER));
        }
    }

    private File newUniqueLogFile() {
        return new File(logDir, logPrefix + logCounter.getAndIncrement() + ".log");
    }

    public File getCurrentLogFile(boolean rotate) {
        File oldLogFile = logFile.get();
        if (rotate) {
            // last writer wins; KISS
            logFile.set(newUniqueLogFile());
        }
        return oldLogFile;
    }
}