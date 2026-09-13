package com.ronreynolds.android.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * used to fire off the service every time an Intent is received
 */
public class ToneReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // start the Tone service when we receive an Intent
        Intent svc = new Intent(context, ToneService.class);
        context.startService(svc);
    }
}
