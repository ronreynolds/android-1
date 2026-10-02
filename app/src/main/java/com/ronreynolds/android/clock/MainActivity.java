package com.ronreynolds.android.clock;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ronreynolds.android.util.LimitedTextView;
import com.ronreynolds.android.util.Logs;

/**
 * main class for the application
 */
public class MainActivity extends AppCompatActivity {
    private static final int MAX_LOG_LINES = 100;    // any point in making this a setting?
    private static final float QUIET_RATIO = 0.1f;
    private final String LOG_TAG = getClass().getSimpleName();
    private LimitedTextView logView;
    private boolean muteSettingsChanges;
    private MainApplication mainApplication;

    /**
     * invoked when the app is first created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mainApplication = (MainApplication) getApplication();
        Logs.i(LOG_TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        // create the GUI bits (but keep it light to avoid skipped frames on startup)
        setContentView(R.layout.activity_main);
        setupSettingsGUI();
        setupButtons();

        TextView nameAndVersion = findViewById(R.id.txtAppVersion);
        nameAndVersion.setText(mainApplication.getAppNameAndVersion());

        // delay the rest of our startup work to after the first draw to avoid skipped frames
        getWindow().getDecorView().post(this::finishSetup);
    }

    private void finishSetup() {
        Logs.d(LOG_TAG, "finishSetup()");
        setupLogView();
        updateGuiToSettings();
        // bootstrap SpeechService
        sayTime(null);
    }

    private void setupLogView() {
        // our Log view - updated as log events occur
        logView = new LimitedTextView(MAX_LOG_LINES,
                findViewById(R.id.logView), findViewById(R.id.logScrollView));
        Logs.addObserver((event) -> logView.appendLine(event.formatLine()));
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
        findViewById(R.id.btnSayTime).setOnClickListener(this::sayTime);
        findViewById(R.id.btnSetQuiet).setOnClickListener(this::setQuietToVolume);
        findViewById(R.id.btnBeQuiet).setOnClickListener(this::setVolumeToQuiet);
        findViewById(R.id.btnQuit).setOnClickListener(this::shutdown);
        findViewById(R.id.upload_log).setOnClickListener(this::uploadLog);
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

    private void sayTime(View ignore) {
        startService(new Intent(this, SpeechService.class));
    }

    /**
     * set the current volume to the quiet value
     */
    private void setVolumeToQuiet(View ignore) {
        AudioManager am = mainApplication.getAudioManager();
        int quietVolume = Settings.getQuietVolume();
        Logs.i(LOG_TAG, "setting current volume to " + quietVolume);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, quietVolume, 0);
    }

    /**
     * set the current volume as the quiet volume
     */
    private void setQuietToVolume(View ignore) {
        AudioManager am = mainApplication.getAudioManager();
        int currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        Logs.i(LOG_TAG, "setting quiet volume to " + currentVolume);
        Settings.setQuietVolume(currentVolume);
    }

    private void shutdown(View ignore) {
        Logs.i(LOG_TAG, "shutdown called");

        // Stop any running services (if you have one)
        boolean stopped = stopService(new Intent(this, SpeechService.class));
        Logs.i(LOG_TAG, "stopService(SpeechService) returned " + stopped);

        // Finish the Activity and all associated processes
        finishAffinity();
        // remove the task from the recents list
        finishAndRemoveTask();
    }

    private void uploadLog(View ignore) {
        Logs.i(LOG_TAG, "uploadLog");
        Uri uri = FileProvider.getUriForFile(
                this,
                "com.ronreynolds.android.clock.fileprovider",
                mainApplication.getCurrentLogFile(true)
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Upload log file"));
    }
}
