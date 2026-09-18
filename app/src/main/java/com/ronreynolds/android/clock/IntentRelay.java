package com.ronreynolds.android.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ronreynolds.android.util.Logs;

/**
 * relays the broadcast PendingIntent to the SpeechService (which can only receive regular Intent)
 */
public class IntentRelay extends BroadcastReceiver {
    private final String LOG_TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Logs.d(LOG_TAG, "onReceive");
        context.startService(new Intent(context, SpeechService.class));
    }
}
