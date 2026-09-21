package com.ronreynolds.android.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
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
    private boolean muteSettingsChanges;

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
        Logs.i(LOG_TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        // create the GUI bits (but keep it light to avoid skipped frames on startup)
        setContentView(R.layout.activity_main);
        setupSettingsGUI();
        setupButtons();
        // delay the rest of our startup work to after the first draw to avoid skipped frames
        getWindow().getDecorView().post(this::finishSetup);
    }

    private void finishSetup() {
        Logs.d(LOG_TAG, "finishSetup()");

        // create our line-limited wrapper around the log TextView/ScrollView pair
        setupLogView();
        // initialize the global application settings (this can be quite slow)
        Settings.init(this);
        // reflect settings in the UI controls
        updateGuiToSettings();

        // startup log message
        PackageInfo packageInfo;
        String appName = getPackageManager().getApplicationLabel(getApplicationInfo()).toString();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    // API 33+
                packageInfo = getPackageManager()
                        .getPackageInfo(getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } else {
                packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            }
            Logs.i(LOG_TAG, appName + " v" + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException fail) {
            Logs.e(LOG_TAG, "failed to get package info - " + fail, fail);
        }

        // send the first message to start up the SpeechService
        sendFirstIntent();
    }

    private void setupLogView() {
        // our Log view - updated as log events occur
        logView = new LimitedTextView(MAX_LOG_LINES, findViewById(R.id.logView),
                findViewById(R.id.logScrollView));
        Logs.addObserver((level, context, message, ex) -> {
            String timestamp = Time.getNowTimestamp();
            char cLevel = Logs.levelToChar(level);
            logView.appendLine(
                    String.format("%s %c %s \"%s\"", timestamp, cLevel, context, message));
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
            if (!muteSettingsChanges) {
                Settings.setPeriodMinutes(periodMinutes);
            }
        });

        RadioGroup timeFormatGroup = findViewById(R.id.timeFormatGroup);
        timeFormatGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!muteSettingsChanges) {
                Settings.setUse24HourTime(checkedId == R.id.time24);
            }
        });
    }

    private void setupButtons() {
        // simple "go" button (mostly for testing but also to init the TTS system)
        Button btn = findViewById(R.id.btnSayTime);
        btn.setOnClickListener(this::sayTime);
        // clean quit option
        Button quietBtn = findViewById(R.id.btnQuiet);
        quietBtn.setOnClickListener(this::setMinimumVolume);
        // clean quit option
        Button quitBtn = findViewById(R.id.btnQuit);
        quitBtn.setOnClickListener(this::shutdown);
    }

    private void updateGuiToSettings() {
        // disable the code that would send these GUI changes back into settings
        muteSettingsChanges = true;
        try {
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
            RadioGroup timeFormatGroup = findViewById(R.id.timeFormatGroup);
            timeFormatGroup.check(Settings.is24HrTime() ? R.id.time24 : R.id.time12);
        } finally {
            // from now on push any GUI element changes into settings
            muteSettingsChanges = false;
        }
    }

    private PendingIntent createIntent() {
        Context context = this;
        int requestCode = 0;
        var messageWithTarget = new Intent(this, IntentRelay.class);
        return PendingIntent.getBroadcast(
                context, requestCode, messageWithTarget, PendingIntent.FLAG_IMMUTABLE);
    }

    private void sendFirstIntent() {
        Logs.i(LOG_TAG, "sendFirstIntent");
        // create these before delay-till-next-minute calc to minimize edge-case near minute boundary
        var alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = createIntent();
        // try to start ON the minute (can introduce up to 60 seconds of delay on startup)
        long startTimeMillis = Time.getMillisTillNextMinute();
        // schedule the event for startTimeMillis in the future
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTimeMillis, operation);
        Logs.i(LOG_TAG, () -> "alarmManager.setAndAllowWhileIdle returned; " +
                "firing in " + startTimeMillis / 1000 + " seconds");
    }

    private void sayTime(View ignore) {
        startService(new Intent(this, SpeechService.class));
    }

    private void setMinimumVolume(View ignore) {
        var am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);    // lowest audible volume
    }

    private void shutdown(View ignore) {
        Logs.i(LOG_TAG, "shutdown called");

        // Cancel first alarm (if any)
        var alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createIntent());

        // Stop any running services (if you have one)
        stopService(new Intent(this, SpeechService.class));

        // Finish the Activity
        finish();

        // Optional: return to home screen explicitly
        var homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }
}
