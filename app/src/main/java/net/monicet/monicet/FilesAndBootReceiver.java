package net.monicet.monicet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Created by ubuntu on 17-02-2017.
 */

public class FilesAndBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        // TODO: only works with local storage ? - see manifest
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, myIntent, 0);

        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR,
                AlarmManager.INTERVAL_HOUR, alarmIntent);

        // here, if the action is boot completed check to see if directory is empty and if so, don't start the alarm, shouldn't check the directory, the utils method does it and replies
        // and if it's not empty, start the alarm
        // if the action is files_present, check that alarm exists and if not: start the alarm (else, do nothing)
        // if the action is not_present, check that alarm exists and if yes: cancel alarm (else do nothing)
    }
}
