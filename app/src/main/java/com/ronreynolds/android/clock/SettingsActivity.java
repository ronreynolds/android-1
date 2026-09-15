package com.ronreynolds.android.clock;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Activity to display your app's settings using the classic API-23 preference system.
 * It loads res/xml/settings.xml and automatically builds the UI.
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load your preferences from res/xml/settings.xml
        addPreferencesFromResource(R.xml.settings);
    }
}
