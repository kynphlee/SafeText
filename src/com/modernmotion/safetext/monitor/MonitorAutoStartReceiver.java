package com.modernmotion.safetext.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MonitorAutoStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent autostartIntent = new Intent(context, STMonitorService.class);
            context.startService(autostartIntent);
        }
    }
}
