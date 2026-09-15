package com.ronreynolds.android.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

/**
 * main class for the application
 */
public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = getClass().getSimpleName();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu resource
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // Launch your settings screen
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_quit) {
            shutdown();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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
        Settings.init(this);    // initialize the global application settings
        scheduleRepeatEvent();
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.btnSayTime);
        btn.setOnClickListener(v -> {
            startService(new Intent(this, ToneService.class));   // API 23
        });
    }

    private void scheduleRepeatEvent() {
        // create these before delay-till-next-minute calc to minimize edge-case near minute boundary
        final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        final long periodMillis = Settings.getPeriodMillis();
        final PendingIntent operation = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(this, ToneReceiver.class),   // target for messages
                PendingIntent.FLAG_UPDATE_CURRENT // if PendingIntent already exists update it
        );
        final long startTimeMillis = getMillisTillNextMinute();
        // start the alarms flowing
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTimeMillis, periodMillis, operation);
        Log.d(LOG_TAG, "alarmManager.setRepeating returned; " +
                "starting in " + startTimeMillis / 1000 + " seconds with period "
                + periodMillis / 1000);
    }

    private static long getMillisTillNextMinute() {
        // before Oreo java-time wasn't available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime nextMinute = LocalDateTime.now().withSecond(0).withNano(0).plusMinutes(1);
            return nextMinute.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else {
            Calendar nextMinute = Calendar.getInstance();
            // Truncate seconds and millis and add 1 minute
            nextMinute.set(Calendar.SECOND, 0);
            nextMinute.set(Calendar.MILLISECOND, 0);
            nextMinute.add(Calendar.MINUTE, 1);
            return nextMinute.getTimeInMillis();
        }
    }

    private void shutdown() {
        Log.d(LOG_TAG, "shutdown called");

        // Cancel repeating alarm
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent operation = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(this, ToneReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        alarmManager.cancel(operation);

        // Stop any running services (if you have one)
        stopService(new Intent(this, ToneService.class));

        // Finish the Activity
        finish();

        // Optional: return to home screen explicitly
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }
}
