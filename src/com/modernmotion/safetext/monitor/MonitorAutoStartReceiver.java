package com.modernmotion.safetext.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MonitorAutoStartReceiver extends BroadcastReceiver {
    private final static String TAG = MonitorAutoStartReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent autostartIntent = new Intent(context, STMonitorService.class);
            context.startService(autostartIntent);
            Log.d(TAG, "MonitorAutoStartReceiver called");
        }
    }
}
