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

public class PreNougatNetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Utils.sendAndDeleteFiles(context);
    }
}
