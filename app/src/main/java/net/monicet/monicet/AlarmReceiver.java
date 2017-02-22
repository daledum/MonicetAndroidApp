package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.IOException;

/**
 * Created by ubuntu on 17-02-2017.
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        // test starts here
        File dir = new File(Utils.EXTERNAL_DIRECTORY);
        File testFile = new File(dir, "alarmRec" + System.currentTimeMillis());
        try {
            testFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //test

        boolean successful = Utils.sendAndDeleteFiles(context);
        if(successful) {
            // send Intent to filesAndBoot receiver, thus stopping the alarms and disabling the receiver
            Intent stopIntent = new Intent(Utils.STOP_ACTION);
            context.sendBroadcast(stopIntent);
        }
    }
}
