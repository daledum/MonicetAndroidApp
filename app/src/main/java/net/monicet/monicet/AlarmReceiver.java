package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by ubuntu on 17-02-2017.
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        boolean successful = Utils.sendAndDeleteFiles(context);
        if(successful) {
            // send Intent to broadcast, thus stopping the alarm for the rest of the day
            //".FILES_NOT_PRESENT"
        }
    }
}
