package com.ronreynolds.android.clock;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * main class for the application
 */
public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = getClass().getSimpleName();

    /**
     * invoked when the app is first created
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        Button btn = findViewById(R.id.btnStartService);
        btn.setOnClickListener(v -> {
            // startForegroundService(intent); - API 26
            startService(new Intent(this, ToneService.class));   // API 23
        });

        // for now we'll do this at startup (rather than as part of a "start" button push)
        scheduleRepeatingTone();
    }

    private void createNotificationChannel() {
        // only available on Oreo (Android-8.0) and later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel(
                    "tone_channel",
                    "Tone Clock",
                    NotificationManager.IMPORTANCE_LOW
            ));
        }
    }

    private void scheduleRepeatingTone() {
        // create these before delay-till-next-minute calc to minimize edge-case near minute boundary
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long everyMinute = TimeUnit.MINUTES.toMillis(1);
        PendingIntent operation = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(this, ToneReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT // if PendingIntent already exists update it
        );

        // calculate millis until the next minute (first alarm)
        final long startTimeMillis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime nextMinute = LocalDateTime.now().withSecond(0).withNano(0).plusMinutes(1);
            startTimeMillis = nextMinute.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else {
            // can't use Java DateTime till API 26 (ZTE supports API 23)
            Calendar nextMinute = Calendar.getInstance();
            // Truncate seconds and millis and add 1 minute
            nextMinute.set(Calendar.SECOND, 0);
            nextMinute.set(Calendar.MILLISECOND, 0);
            nextMinute.add(Calendar.MINUTE, 1);
            startTimeMillis = nextMinute.getTimeInMillis();
        }
        // start the alarms flowing
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTimeMillis, everyMinute, operation);
        Log.d(LOG_TAG, "alarmManager.setRepeating returned");
    }
}
