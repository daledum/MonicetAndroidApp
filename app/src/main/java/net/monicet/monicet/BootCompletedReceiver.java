package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startIntent = new Intent(context, ForegroundService.class);
        startIntent.setAction(Utils.START_FOREGROUND_SERVICE_FROM_BOOT_RECEIVER);
        context.startService(startIntent);
    }
}
