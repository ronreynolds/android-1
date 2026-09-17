package com.ronreynolds.android.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ronreynolds.android.util.Logs;
import com.ronreynolds.android.util.Time;

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
        Logs.d(LOG_TAG, "onCreate called");
        super.onCreate(savedInstanceState);

        Settings.init(this);    // initialize the global application settings

        scheduleRepeatEvent();

        // register out listener for Setting changes
        Settings.addObserver(new Settings.SettingObserver() {
            @Override
            public void onPeriodChange() {
                scheduleRepeatEvent();  // overwrite scheduled event when the period is changed
            }

            @Override
            public void onClockTypeChange() {
                // we don't care about this here
            }
        });

        // create the GUI bits
        setContentView(R.layout.activity_main);
        setupLogView();
        setupSettingsGUI();
        setupButtons();
    }

    private void setupLogView() {
        // our Log view - updated as log events occur
        TextView logView = findViewById(R.id.logView);
        Logs.addObserver((int level, String context, String message, Throwable ex) -> {
            String timestamp = Time.getTimeNow();
            char cLevel = Logs.levelToChar(level);
            logView.append(String.format("%s %s %c \"%s\"%n", timestamp, context, cLevel, message));
        });
    }

    private void setupSettingsGUI() {
        // setup Settings controls
        RadioGroup periodGroup = findViewById(R.id.periodGroup);
        periodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int periodMinutes;
            if (checkedId == R.id.period1) {
                periodMinutes = 1;
            } else if (checkedId == R.id.period5) {
                periodMinutes = 5;
            } else if (checkedId == R.id.period10) {
                periodMinutes = 10;
            } else if (checkedId == R.id.period15) {
                periodMinutes = 15;
            } else {
                Logs.wtf(LOG_TAG, "invalid period minutes; checkedId:" + checkedId);
                periodMinutes = 1;  // default to every minute (even tho this should never ever happen)
            }
            Settings.setPeriodMinutes(periodMinutes);
        });
        switch (Settings.getPeriodMinutes()) {
            case 1:
                periodGroup.check(R.id.period1);
                break;
            case 5:
                periodGroup.check(R.id.period5);
                break;
            case 10:
                periodGroup.check(R.id.period10);
                break;
            case 15:
                periodGroup.check(R.id.period15);
                break;
            default:
                Logs.w(LOG_TAG, "period not set or invalid - " + Settings.getPeriodMillis());
                Settings.setPeriodMinutes(1);
        }

        RadioGroup timeFormatGroup = findViewById(R.id.timeFormatGroup);
        timeFormatGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Settings.setUse24HourTime(checkedId == R.id.time24);
        });
        if (Settings.is24HrTime()) {
            timeFormatGroup.check(R.id.time24);
        } else {
            timeFormatGroup.check(R.id.time12);
        }
    }

    private void setupButtons() {
        // simple "go" button (mostly for testing but also to init the TTS system)
        Button btn = findViewById(R.id.btnSayTime);
        btn.setOnClickListener(v -> {
            startService(new Intent(this, ToneService.class));   // API 23
        });
        // clean quit option
        Button quitBtn = findViewById(R.id.btnQuit);
        quitBtn.setOnClickListener(v -> shutdown());
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
        final long startTimeMillis = Time.getMillisTillNextMinute();
        // start the alarms flowing
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTimeMillis, periodMillis, operation);
        Logs.d(LOG_TAG, () -> "alarmManager.setRepeating returned; " +
                "starting in " + startTimeMillis / 1000 + " seconds with period "
                + periodMillis / 1000);
    }

    private void shutdown() {
        Logs.d(LOG_TAG, "shutdown called");

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
