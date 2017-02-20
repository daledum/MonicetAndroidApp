package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

import static android.R.attr.path;
import static net.monicet.monicet.Utils.sendAndDeleteFiles;

/**
 * Created by ubuntu on 13-02-2017.
 */

// this is used for Android API 23 and below, pre-Nougat
public class StaticNetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean successful = Utils.sendAndDeleteFiles(context);
        if (successful) {
            // disable itself
            Utils.setComponentState(
                    context,
                    this.getClass().getSimpleName(),
                    false
            );
        }
    }
}
