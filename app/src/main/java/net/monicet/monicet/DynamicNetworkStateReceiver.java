package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.IOException;

/**
 * Created by ubuntu on 20-02-2017.
 */

public class DynamicNetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //final Context receivedContext = context;//TODO: uncomment this

        // android.net.conn.CONNECTIVITY_CHANGE is a sticky broadcast, so, the receiver fires when registered
        if (isInitialStickyBroadcast()) {
            // Ignore this call to onReceive, as this is the sticky broadcast
        } else {
            // Connectivity state has changed
            // test starts here
            File dir = new File(Utils.EXTERNAL_DIRECTORY);
            File testFile = new File(dir, "dynamicRec" + System.currentTimeMillis());
            try {
                testFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //test
            Utils.sendAndDeleteFiles(context);
        }

        // dynamic receivers run on the UI thread, so this should run on a separate thread
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                // this receiver could fire before the directory path is set (which is done after SEND)
//                // it will use the default path - and it will disable itself
//                boolean successful = Utils.sendAndDeleteFiles(receivedContext);
//
//                if(successful) {
//                    // disable itself (or unregister itself?)
//                    Utils.setComponentState(
//                            receivedContext,
//                            DynamicNetworkStateReceiver.class,
//                            false
//                    );
//                }
//            }
//        }).start();


    }
}
