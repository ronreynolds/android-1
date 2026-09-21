package com.ronreynolds.android.clock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import androidx.core.app.NotificationCompat;

import com.ronreynolds.android.util.Logs;
import com.ronreynolds.android.util.Time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * the service that actually provides the sound output
 */
public class SpeechService extends Service {
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
        Logs.d(LOG_TAG, "onCreate");
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());  // used for async callback until TTS is ready

        Settings.setObserver(new Settings.SettingObserver() {
            @Override
            public void onPeriodChange() {
                // if the period changes we discard the currently-scheduled intent and create a new one
                cancelPendingIntent();
                scheduleNextIntent();
            }

            @Override
            public void onClockTypeChange() {
                // no longer matters since we read it every time (haha)
            }
        });

        // set our time-format from Settings before we use it (else Bad Things happen)
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
        scheduleNextIntent();   // schedule the next intent before we do anything else (minimal lag)
        startForeground(1, startNotification);
        sayTime();
        return START_STICKY;    // keep us around after this Intent has been processed
    }

    @Override
    public void onDestroy() {
        Logs.d(LOG_TAG, "onDestroy");
        cancelPendingIntent();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();   // <-- THIS unbinds the ServiceConnection
            textToSpeech = null;
        }
        super.onDestroy();
    }

    private PendingIntent createIntent() {
        Context context = this;
        int requestCode = 42;
        Intent messageWithTarget = new Intent(this, IntentRelay.class);
        return PendingIntent.getBroadcast(
                context, requestCode, messageWithTarget, PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * create and schedule the next Intent to invoke this service
     */
    private void scheduleNextIntent() {
        // create these before delay-till-next-minute calc to minimize edge-case near minute boundary
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        // round down to the edge of the minute
        long now = System.currentTimeMillis();
        long triggerTime = Time.roundDownToMinuteMillis(now + Settings.getPeriodMillis());
        // schedule the intent to fire in periodMillis
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, createIntent());
        Logs.i(LOG_TAG, () -> "scheduleNextIntent; alarmManager.setAndAllowWhileIdle returned; " +
                "firing in " + (triggerTime - now) / 1000 + " seconds");
    }

    private void cancelPendingIntent() {
        Logs.d(LOG_TAG, "cancelling pending intent");
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(createIntent());
    }

    private void sayTime() {
        if (!ttsReady) {
            Logs.d(LOG_TAG, "TTS not ready; adding recursive delayed callback");
            handler.postDelayed(this::sayTime, 100);    // call us back in 100ms
        } else {
            // not sure there's much point for DateTimeFormatter for something so simple and rare
            String text = new SimpleDateFormat(Settings.is24HrTime() ? "H m" : "h m a", Locale.US)
                    .format(new Date());
            Logs.d(LOG_TAG, "sayTime - " + text);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SpeechService.sayTime");
        }
    }
}
