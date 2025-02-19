package com.quick.disarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Since notifications can't start JobIntentService via PendingIntent we need
 * to go through a broadcast receiver.
 */
public class JobIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DisarmForegroundService.startService(context, intent.getStringExtra(DisarmJobIntentService.EXTRA_CAR_BLUETOOTH));
    }
}
