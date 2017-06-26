package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.IOException;

/**
 * Created by ubuntu on 23-02-2017.
 */

public class DynamicNetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Context receivedContext = context;

        // test starts here//TODO:reinstate for testing
//        File dir = new File(Utils.EXTERNAL_DIRECTORY);
//        File testFile = new File(dir, "dynamicRec" + System.currentTimeMillis());
//        try {
//            testFile.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //test

        // dynamic receivers run on the UI thread, this receiver calls a method which connects to the Internet,
        // there this must run on a separate thread (no network calls on the UI thread)
        new Thread(new Runnable() {
            @Override
            public void run() {
                Utils.sendAndDeleteFiles(receivedContext);// or use getApplicationContext() ?
            }
        }).start();
    }
}
