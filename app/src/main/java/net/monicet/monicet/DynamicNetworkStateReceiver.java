package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by ubuntu on 20-02-2017.
 */

public class DynamicNetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Context receivedContext = context;

        // dynamic receivers run on the UI thread, so this should run on a separate thread
        new Thread(new Runnable() {
            @Override
            public void run() {

                // this receiver could fire before the directory path is set (which is done after SEND)
                // it will use the default path - which is fine, no files will be there before SEND (it will disable itself)
                boolean successful = Utils.sendAndDeleteFiles(receivedContext);

                if(successful) {
                    // disable itself (or unregister itself?)
                    Utils.setComponentState(
                            receivedContext,
                            this.getClass().getSimpleName(),
                            false
                    );
                }
            }
        }).start();
    }
}
