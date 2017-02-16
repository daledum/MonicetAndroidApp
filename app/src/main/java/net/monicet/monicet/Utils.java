package net.monicet.monicet;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

import static android.R.attr.path;

/**
 * Created by ubuntu on 07-02-2017.
 */

public final class Utils {
    private Utils() {}

    public static final String JSON_FILE_EXTENSION = ".json";
    public static final String CSV_FILE_EXTENSION = ".csv";
    public static final String INTERNAL_DIR_PATH = "data/" + BuildConfig.APPLICATION_ID + "/files";
    public static final String INTENT_CONNECTION_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

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

    public static boolean sendAndDeleteFiles(Context context, String path) {

        //test
        //OK - works with setAction NEW_FILE, but not with connectivity change (only if app is running)
        File directory = new File(Environment.getExternalStorageDirectory(), "Monicet"); // external storage
        //test

        // Environment.getDataDirectory() : /data
        // getFilesDir() : /data/data/package/files, where package is net.monicet.monicet
        //BuildConfig.APPLICATION_ID: net.monicet.monicet

        // deal with the received path
        if (path.isEmpty()) {
            // internal, using BuildConfig.APPLICATION_ID
            //File directory = new File(Environment.getDataDirectory(), Utils.INTERNAL_DIR_PATH);
            // or use hardcoded path
            //File directory = new File(Environment.getDataDirectory(), "data/net.monicet.monicet/files");

        } else { // path will be context.getFilesDir().toString()
            //File directory = new File(path); // internal storage
        }

        // this uses Utils.JSON_FILE_EXTENSION and Utils.CSV_FILE_EXTENSION and is instantiated inside the activity, too
        // a constructor could be implemented for MyFileFilter(String extensions1, String extension 2)
        // http://stackoverflow.com/questions/5751335/using-file-listfiles-with-filenameextensionfilter
        FileFilter fileFilter = new MyFileFilter();

        // check if the directory exists and if so, if it has any of our files
        if (directory.exists() && directory.listFiles(fileFilter).length > 0) {

            //test
            File testFile = new File(directory, "test.test");
            try {
                testFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //test

            // secondly, check that there is an Internet connection
            boolean isConnected;
            if (context != null) {
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

                while (wasSentOk && directory.listFiles(fileFilter).length > 0) { // if new files are created while we send and delete the old ones

                    File[] files = directory.listFiles(fileFilter);

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
        return !(directory.exists() && directory.listFiles(fileFilter).length > 0);
    }

    public static String getInternalDirPathFromContext(Context context) {
        if (context != null) {
            return context.getFilesDir().toString();
        }
        return "";
    }
}
