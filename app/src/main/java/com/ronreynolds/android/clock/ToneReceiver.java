package com.ronreynolds.android.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * relays the broadcast Intent to the ToneService (which can only receive regular Intent)
 */
public class ToneReceiver extends BroadcastReceiver {
    private final String LOG_TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "onReceive");
        // start the Tone service when we receive an Intent
        context.startService(new Intent(context, ToneService.class));
    }
}
