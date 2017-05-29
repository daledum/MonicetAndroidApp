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
        // Set the Intent (even when creating a new one, comparison will be done using filterEquals)
        Intent myIntent = new Intent(context, SendFilesAlarmReceiver.class);
        // Returns an existing or new PendingIntent (if it wasn't previously created) matching the given parameters
        PendingIntent alarmIntent =
                PendingIntent.getBroadcast(context, Utils.SENDING_ALARM_REQUEST_CODE, myIntent, 0); //PendingIntent.FLAG_UPDATE_CURRENT

        if (intent.getAction().equals(Utils.STOP_ACTION)) {
            // Cancel the alarm
            alarmMgr.cancel(alarmIntent);
            // Docs: Only the original application owning a PendingIntent can cancel it.
            // alarmIntent.cancel(); // This receiver runs even when the Monicet application doesn't
            // Disable this receiver
            Utils.setComponentState(
                    context,
                    FilesAndBootReceiver.class,
                    false
            );
        } else {
            // it's either BOOT COMPLETED or START, so start the alarm
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR,
                    AlarmManager.INTERVAL_HOUR, alarmIntent);
        }
    }
}
