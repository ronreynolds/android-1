package com.ronreynolds.android.clock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import androidx.core.app.NotificationCompat;

import com.ronreynolds.android.util.Logs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * the service that actually provides the sound output
 */
public class ToneService extends Service {
    private static SimpleDateFormat timeFormat;
    private static final String NOTIFICATION_CHANNEL_ID = "talk_clock_channel";
    private static final String NOTIFICATION_CHANNEL_NAME = "Talking Clock";

    private final String LOG_TAG = getClass().getSimpleName();
    private TextToSpeech textToSpeech;
    private volatile boolean ttsReady = false;
    private Handler handler;    // used to delay TTS messages until it's ready to speak
    private Notification startNotification;

    /**
     * @return the communication channel to this service; null since we have none
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * much like the ctor but better (because we actually have a context to pass)
     */
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());  // used for async callback until TTS is ready

        Settings.addObserver(new Settings.SettingObserver() {
            @Override
            public void onPeriodChange() {
                // we don't care about this here
            }
            @Override
            public void onClockTypeChange() {
                updateTimeFormat();
            }
        });

        // set our time-format from Settings before we use it (else Bad Things happen)
        updateTimeFormat();
        Logs.d(LOG_TAG, "onCreate; creating TextToSpeech");
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Logs.d(LOG_TAG, "TTS initialized");
                ttsReady = true;    // this tells us it's safe to use the TTS
            } else {
                Logs.e(LOG_TAG, "TTS init failed");
            }
        });

        // only needed on Oreo (Android-8.0) and later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager.class)
                    .createNotificationChannel(new NotificationChannel(
                            NOTIFICATION_CHANNEL_ID,
                            NOTIFICATION_CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_LOW
                    ));
        }

        startNotification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Talking Clock Running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    /**
     * called every time an intent/event is sent to the service.
     *
     * @param intent  The Intent supplied to {@link android.content.Context#startService},
     *                as given.  This may be null if the service is being restarted after
     *                its process has gone away, and it had previously returned anything
     *                except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     *                start.  Use with {@link #stopSelfResult(int)}.
     * @return START_STICKY tells caller to keep this service alive between calls
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logs.d(LOG_TAG, "onStartCommand");
        startForeground(1, startNotification);
        sayTime();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Logs.d(LOG_TAG, "onDestroy");
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();   // <-- THIS unbinds the ServiceConnection
            textToSpeech = null;
        }
        super.onDestroy();
    }

    private void sayTime() {
        if (!ttsReady) {
            Logs.d(LOG_TAG, "TTS not ready; adding recursive delayed callback");
            handler.postDelayed(this::sayTime, 100);    // call us back in 100ms
        } else {
            String text;
            final SimpleDateFormat currentTimeFormat = timeFormat;
            synchronized (currentTimeFormat) { // because SimpleDateFormat isn't thread-safe
                text = currentTimeFormat.format(new Date());
            }
            Logs.d(LOG_TAG, "sayTime - " + text);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ToneService.sayTime");
        }
    }

    private void updateTimeFormat() {
        timeFormat = new SimpleDateFormat(Settings.is24HrTime() ? "H m" : "h m a", Locale.US);
    }
}
