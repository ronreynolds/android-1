package com.ronreynolds.android.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.ronreynolds.android.util.LimitedTextView;
import com.ronreynolds.android.util.Logs;
import com.ronreynolds.android.util.Time;

/**
 * main class for the application
 */
public class MainActivity extends AppCompatActivity {
    private static final int MAX_LOG_LINES = 100;    // any point in making this a setting?
    private final String LOG_TAG = getClass().getSimpleName();
    private LimitedTextView logView;

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

        // create the GUI bits
        setContentView(R.layout.activity_main);
        setupLogView();
        setupSettingsGUI();
        setupButtons();

        // schedule (with delay) the first message to start up the SpeechService
        sendFirstIntent();
    }

    private void setupLogView() {
        // our Log view - updated as log events occur
        logView = new LimitedTextView(MAX_LOG_LINES, findViewById(R.id.logView), findViewById(R.id.logScrollView));
        Logs.addObserver((level, context, message, ex) -> {
            String timestamp = Time.getNowTimestamp();
            char cLevel = Logs.levelToChar(level);
            logView.appendLine(String.format("%s %c %s \"%s\"", timestamp, cLevel, context, message));
        });
    }

    private void setupSettingsGUI() {
        // setup Settings controls
        RadioGroup periodGroup = findViewById(R.id.periodGroup);
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
                periodGroup.check(R.id.period1);
        }
        // register change-listener AFTER setting radios to initial state
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

        RadioGroup timeFormatGroup = findViewById(R.id.timeFormatGroup);
        if (Settings.is24HrTime()) {
            timeFormatGroup.check(R.id.time24);
        } else {
            timeFormatGroup.check(R.id.time12);
        }
        // register change-listener AFTER setting radios to initial state
        timeFormatGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Settings.setUse24HourTime(checkedId == R.id.time24);
        });
    }

    private void setupButtons() {
        // simple "go" button (mostly for testing but also to init the TTS system)
        Button btn = findViewById(R.id.btnSayTime);
        btn.setOnClickListener(this::sayTime);
        ;
        // clean quit option
        Button quietBtn = findViewById(R.id.btnQuiet);
        quietBtn.setOnClickListener(this::setMinimumVolume);
        // clean quit option
        Button quitBtn = findViewById(R.id.btnQuit);
        quitBtn.setOnClickListener(this::shutdown);
    }

    private PendingIntent createIntent() {
        Context context = this;
        int requestCode = 0;
        Intent messageWithTarget = new Intent(this, IntentRelay.class);
        return PendingIntent.getBroadcast(context, requestCode, messageWithTarget,
                PendingIntent.FLAG_UPDATE_CURRENT   // update if one already exists with this context + id
        );
    }

    private void sendFirstIntent() {
        // create these before delay-till-next-minute calc to minimize edge-case near minute boundary
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = createIntent();
        // try to start ON the minute (can introduce up to 60 seconds of delay on startup)
        long startTimeMillis = Time.getMillisTillNextMinute();
        // schedule the event for startTimeMillis in the future
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTimeMillis, operation);
        Logs.d(LOG_TAG, () -> "alarmManager.setAndAllowWhileIdle returned; " +
                "firing in " + startTimeMillis / 1000 + " seconds");
    }

    private void sayTime(View ignore) {
        startService(new Intent(this, SpeechService.class));
    }

    private void setMinimumVolume(View ignore) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);    // lowest audible volume
    }

    private void shutdown(View ignore) {
        Logs.d(LOG_TAG, "shutdown called");

        // Cancel first alarm (if any)
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createIntent());

        // Stop any running services (if you have one)
        stopService(new Intent(this, SpeechService.class));

        // Finish the Activity
        finish();

        // Optional: return to home screen explicitly
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }
}
