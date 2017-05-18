package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.IOException;

/**
 * Created by ubuntu on 17-02-2017.
 */

public class SendFilesAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Context receivedContext = context;

        // test starts here
        File dir = new File(Utils.EXTERNAL_DIRECTORY);
        File testFile = new File(dir, "alarmRec" + System.currentTimeMillis());
        try {
            testFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //test

        // does network work, so it needs to be on a separate thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean successful = Utils.sendAndDeleteFiles(receivedContext);
                if(successful) {
                    // send Intent to filesAndBoot receiver, thus stopping the alarms and disabling the receiver
                    Intent stopIntent = new Intent(Utils.STOP_ACTION);
                    receivedContext.sendBroadcast(stopIntent);
                }
            }
        }).start();
    }
}
