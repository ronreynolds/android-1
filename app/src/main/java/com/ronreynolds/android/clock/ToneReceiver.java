package com.ronreynolds.android.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ronreynolds.android.util.Logs;

/**
 * relays the broadcast Intent to the ToneService (which can only receive regular Intent)
 */
public class ToneReceiver extends BroadcastReceiver {
    private final String LOG_TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Logs.d(LOG_TAG, "onReceive");
        // relay message to the Tone service
        context.startService(new Intent(context, ToneService.class));
    }
}
