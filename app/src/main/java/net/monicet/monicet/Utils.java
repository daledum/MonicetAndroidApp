package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by ubuntu on 07-02-2017.
 */

public final class Utils {
    private Utils() {}

    // Environment.getDataDirectory() : /data
    // getFilesDir() : /data/data/package/files, where package is net.monicet.monicet
    ////getFilesDir(): /data/data/net.monicet.monicet/files
    //BuildConfig.APPLICATION_ID: net.monicet.monicet
    //File directory = new File(Environment.getExternalStorageDirectory(), "Monicet"); // external storage
    // deal with the received path
    //if (path.isEmpty()) {
    // internal, using BuildConfig.APPLICATION_ID
    //File directory = new File(Environment.getDataDirectory(), Utils.INTERNAL_DIR_PATH);
    // or use hardcoded path
    //File directory = new File(Environment.getDataDirectory(), "data/net.monicet.monicet/files");

    // http://www.grokkingandroid.com/android-tutorial-broadcastreceiver/

    //} else { // path will be context.getFilesDir().toString()
    //File directory = new File(path); // internal storage
    //}

    public static final String INTENT_CONNECTION_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String START_ACTION = ".START";
    public static final String STOP_ACTION = ".STOP";

    public static final String JSON_FILE_EXTENSION = ".json";
    public static final String CSV_FILE_EXTENSION = ".csv";

    // this will get set by the MainActivity.. as a default: null, of course
    // this and dirPath are in the utils class, because it needs to accessed by several mechanisms
    // (after the activity has started them), even at times when the application is stopped
    private static String[] fileExtensionsArray;

    // this will get set by the MainActivity.. as a default: set it to all the registered extension
    private static String DIRECTORY;
    // back-ups, if directory is null (for usage in SendAndDeleteFiles()) // TODO: test need for a / ?
    public static final String EXTERNAL_DIRECTORY =
            Environment.getExternalStorageDirectory()
            + "/Monicet"; // this should be set in the activity via the same mechanism...should drop this
    // this is a back-up, it's not being used // TODO: test need for a / ?
    public static final String INTERNAL_DIRECTORY =
            Environment.getDataDirectory()
            + "/data/"
            + BuildConfig.APPLICATION_ID + "/files";
    // or hard-coded internal path
    public static final String INTERNAL_DIRECTORY_HARDCODED =
            Environment.getDataDirectory()
                    + "/data/net.monicet.monicet/files";

    // allow only a setter (no getter) and only when it's null,
    // so that the path and extensions only ever get assigned once
    public static void setDirectory(String dir) {
        if (DIRECTORY == null) {
            DIRECTORY = dir;
        }
    }

    public static void setFileExtensionsArray(String[] extensions) {
        if (fileExtensionsArray == null) {
            fileExtensionsArray = extensions;
        }
    }

    public static double parseGpsToDouble(String sValue, GpsEdgeValue edgeValue) {
        double result = 0;

        if (!sValue.isEmpty()) {
            try {
                result = Double.parseDouble(sValue);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // in case the result is not 0 (if it's 0, there's no need to check if it's too large or too small
        // latitude degree values must be between -90 and 90
        // longitude degree values must be between -180 and 180
        // and minutes and seconds can take values from 0 to 60
        if (result != 0) {
            if (edgeValue == GpsEdgeValue.DEGREES_LATITUDE) {
                if (result < -(GpsEdgeValue.DEGREES_LATITUDE.getGpsEdgeValue())) {
                    result = -(GpsEdgeValue.DEGREES_LATITUDE.getGpsEdgeValue());
                } else if (result > GpsEdgeValue.DEGREES_LATITUDE.getGpsEdgeValue()) {
                    result = GpsEdgeValue.DEGREES_LATITUDE.getGpsEdgeValue();
                }
            } else if (edgeValue == GpsEdgeValue.DEGREES_LONGITUDE) {
                if (result < -(GpsEdgeValue.DEGREES_LONGITUDE.getGpsEdgeValue())) {
                    result = -(GpsEdgeValue.DEGREES_LONGITUDE.getGpsEdgeValue());
                } else if (result > GpsEdgeValue.DEGREES_LONGITUDE.getGpsEdgeValue()) {
                    result = GpsEdgeValue.DEGREES_LONGITUDE.getGpsEdgeValue();
                }
            } else if (edgeValue == GpsEdgeValue.MINUTES_OR_SECONDS) {
                if (result < 0) { result = 0; }
                else if (result > GpsEdgeValue.MINUTES_OR_SECONDS.getGpsEdgeValue()) {
                    result = GpsEdgeValue.MINUTES_OR_SECONDS.getGpsEdgeValue();
                }
            }
        }

        return result;
    }

    public static double convertDegMinSecToDecimal(double vDeg, double vMin, double vSec) {
        return vDeg + vMin/60 + vSec/3600;
    }

    public static boolean endsWithOneOfTheseExtensions(File pathname, String[] extensions) {
        for(String extension: extensions) {
            if (pathname.getName().toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static boolean sendAndDeleteFiles(Context context) {

        File dir;
        if (DIRECTORY != null) {
            dir = new File(DIRECTORY); //TODO: toString? if external remove + remember to use dir everywhere
        } else {
            // if this is called before the path was set by the MainActivity
            // (for the dynamic receiver, registered at the beginning of onCreate)
            // default path, when DIRECTORY is null
            if (context != null) {
                dir = new File(context.getFilesDir().toString());
            } else {
                dir = new File(INTERNAL_DIRECTORY);
            }
        }

        //test // TODO: remove this after tests
        dir = new File(EXTERNAL_DIRECTORY);
        //test

        final String[] extensions;
        if (fileExtensionsArray != null) {
            extensions = fileExtensionsArray;
        } else {
            // fileExtensionsArray is null if it hasn't been set by the Main Activity)
            extensions = new String[]{JSON_FILE_EXTENSION, CSV_FILE_EXTENSION};
        }
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return endsWithOneOfTheseExtensions(pathname, extensions);
            }
        };
        // check if the directory exists and if so, if it has any of our files
        if (dir.exists() && dir.listFiles(fileFilter).length > 0) {

            //test
            File testFile = new File(dir, "test.test");
            try {
                testFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //test

            // secondly, check that there is an Internet connection
            boolean isConnected;
            if (context != null) { // TODO: remove this or keep it as a backup
                ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            } else {
                // assume it's connected
                isConnected = true;
            }

            if (isConnected) {

                ArrayList<String> namesOfFilesToDelete = new ArrayList<String>();
                boolean wasSentOk = true;

                while (wasSentOk && dir.listFiles(fileFilter).length > 0) { // if new files are created while we send and delete the old ones

                    File[] files = dir.listFiles(fileFilter);

                    sending:
                    for (int i = 0; i < files.length; i++) {
                        wasSentOk = false;
                        // Establish a connection
                        // an error can appear here
                        // TODO: check if (files[i].exists()) { }, in case it was already deleted by the receiver
                        //then send the files via http post...one by one
                        // an error can appear here
                        if (System.currentTimeMillis() != 1111111111) {// if successfully sent (connection established etc):
                            namesOfFilesToDelete.add(files[i].getName());
                            wasSentOk = true;
                        } else {
                            break sending; // exit the loop if there are problems, wasSentOk is false, so the outer while will stop
                        }
                    }

                    for (int i = 0; i < files.length; i++) {
                        // or deleteFile("filename");//myContext.deleteFile(fileName);
                        if (namesOfFilesToDelete.contains(files[i].getName())) {

                            namesOfFilesToDelete.remove(files[i]);
                            if (files[i].exists()) {
                                files[i].delete();
                            }
                        }
                    }
                }
            }
        }

        // method is successful if it has sent and therefore deleted all the files
        // returns true if directory doesn't exist or there aren't any of our files in it
        // returns false if directory exists and it has at least one of our files in it
        return !(dir.exists() && dir.listFiles(fileFilter).length > 0);
    }

    public static void setComponentState(Context context, String componentClassName, boolean enabled) {

        // Alex: maybe just use context
        // dynamically (programmatically) enabled/disabled state is kept across reboots
        PackageManager pm = context.getApplicationContext().getPackageManager();

        ComponentName componentName = new ComponentName(
                context.getApplicationContext(),
                componentClassName
        );

        int state = enabled ?
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
    }

    // class name
    // this.getClass().getSimpleName(); //this.getLocalClassName();// getBaseContext().getLocalClassName();
    // getApplicationContext().getLocalClassName();//MainActivity.class.getSimpleName();

//    public static void registerMyReceiver(Context context, BroadcastReceiver vReceiver, String action) {
//        // there is no way to check if the receiver was registered or not
//        try {
//            IntentFilter filter = new IntentFilter();
//            filter.addAction(action);
//            context.registerReceiver(vReceiver, filter);
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void unregisterMyReceiver(Context context, BroadcastReceiver vReceiver) {
//        // there is no way to check if the receiver was registered or not
//        try {
//            context.unregisterReceiver(vReceiver);
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//        }
//    }

}
