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

/**
 * main class for the application
 */
public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "ToneService";

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
            Intent intent = new Intent(this, ToneService.class);
            // startForegroundService(intent); - API 26
            startService(intent);   // API 23
        });

        // for now we'll do this at startup (rather than as part of a "start" button push)
        scheduleRepeatingTone();
    }

    private void createNotificationChannel() {
        // only available on Oreo (Android-8.0) and later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "tone_channel",
                    "Tone Clock",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    private void scheduleRepeatingTone() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, ToneReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT // if PendingIntent already exists just update it
        );

        long interval = 60 * 1000; // 1 minute
        long startTime = System.currentTimeMillis() + 1000; // start in 1 second

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                startTime,
                interval,
                pendingIntent
        );
    }
}
