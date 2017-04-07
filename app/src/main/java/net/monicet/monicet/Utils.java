package net.monicet.monicet;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by ubuntu on 07-02-2017.
 */

public final class Utils {
    private Utils() {}

    public static final int ONE_SECOND_IN_MILLIS = 1000;
    public static final int ONE_MINUTE_IN_MILLIS = 60 * 1000;
    public static final int FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000;

    // this is used for dealing with views in the SightingAdapter (when working with Animal end quantity and TimeAndPlace)
    // code smell - hack due to changes done in onclick listener not sticking around (tried using views from main act,
    // running on ui thread...
    public static final int INITIAL_VALUE = 0; // was -1

    public static final String INTENT_CONNECTION_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String START_ACTION = ".START";
    public static final String STOP_ACTION = ".STOP";

    // this will get set by the MainActivity.. as a default: set it to all the registered extension
    private static String DIRECTORY;
    // back-ups, if directory is null (for usage in SendAndDeleteFiles())
    public static final String EXTERNAL_DIRECTORY =
            Environment.getExternalStorageDirectory()
            + "/Monicet"; // this should be set in the activity via the same mechanism...should drop this
    // this is a back-up, it's not being used
    public static final String INTERNAL_DIRECTORY =
            Environment.getDataDirectory()
            + "/data/"
            + BuildConfig.APPLICATION_ID + "/files";
    // or hard-coded internal path
    public static final String INTERNAL_DIRECTORY_HARDCODED =
            Environment.getDataDirectory()
            + "/data/net.monicet.monicet/files";

    // allow to be set only once, only when it's null
    public static void setDirectory(String dir) {
        if (DIRECTORY == null) {
            DIRECTORY = dir;
        }
    }
    public static String getDirectory() { return DIRECTORY; }

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

    public static boolean sendAndDeleteFiles(Context context) {

        File dir;
        if (DIRECTORY != null) {
            dir = new File(DIRECTORY); //TODO: toString? if external remove + remember to use dir everywhere
        } else {
            // if this is called before the path was set by the MainActivity
            // (in case the dynamic receiver is registered, thus (sticky intent) triggered before setting the dir)
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

        //test
        File testFile = new File(dir, "send" + System.currentTimeMillis());
        try {
            testFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //test

        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                for(AllowedFileExtension fe: AllowedFileExtension.values()) {
                    if (pathname.getName().toLowerCase().endsWith(fe.toString())) {
                        return true;
                    }
                }
                return false;
            }
        };

        // check if the directory exists and if so, if it has any of our files
        if (dir.exists() && dir.listFiles(fileFilter).length > 0) {

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

                    // this is done file by file
                    sending:
                    for (int i = 0; i < files.length; i++) {
                        wasSentOk = false;

                        // Establish a connection
                        // an error can appear here
                        HttpURLConnection connection = null;
                        URL url = null;
                        int result = 0;//was Integer

                        //DataOutputStream outputStream = null;//? get rid
                        //String lineEnd = "\r\n";
                        //String twoHyphens = "--";
                        //String boundary =  "*****";

                        int bytesRead, bytesAvailable, bufferSize;
                        byte[] buffer;
                        int maxBufferSize = 1*1024*1024;

                        try {
                            url = new URL("http://infohive.org.uk/monicet/send/");
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }

                        if (url != null) {
                            try {
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setDoInput (true); // setdoinput is true by default
                                connection.setDoOutput (true);// set it for output
                                connection.setUseCaches (false);
                                connection.setInstanceFollowRedirects(false);
                                connection.setRequestMethod("POST");
                                connection.setRequestProperty("Connection", "Keep-Alive");

                                //connection.setRequestProperty("Content-Type", "multipart/formdata;boundary=" + boundary);

                                // check that the file still exists - check this as late as possible
                                if (files[i].exists()) {
                                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

                                    //outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                                    //outputStream.writeBytes("Content-Disposition: form-data;name=\"uploadedfile\";filename=\""
                                            ////+ dir//dir + files[i].getName()//maybe put a toast what getpath shows (with extension or not?)
                                            //+ files[i].getPath()
                                            //+"\""
                                            //+ lineEnd);
                                    //outputStream.writeBytes(lineEnd);

                                    FileInputStream fileInputStream = new FileInputStream(files[i]);
                                    bytesAvailable = fileInputStream.available();
                                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                                    buffer = new byte[bufferSize];

                                    // Read file
                                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                                    while (bytesRead > 0)
                                    {
                                        outputStream.write(buffer, 0, bufferSize);
                                        bytesAvailable = fileInputStream.available();
                                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                                    }

                                    //outputStream.writeBytes(lineEnd);
                                    //outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                                    // TODO: does it really need a line end

                                    // get the result
                                    result = connection.getResponseCode();//int responseCode = connection.getResponseCode();

                                    fileInputStream.close();
                                    outputStream.flush();
                                    outputStream.close();
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (connection != null) {
                                    connection.disconnect();
                                }
                            }
                        }

                        if (result == HttpURLConnection.HTTP_OK) {
                            if (files[i].exists()) {
                                namesOfFilesToDelete.add(files[i].getName());
                            }
                            wasSentOk = true;
                        } else {
                            break sending; // exit the loop if there are problems, wasSentOk is false, so the outer while will stop
                        }
                    }

                    for (int i = 0; i < files.length; i++) {
                        // or deleteFile("filename");//myContext.deleteFile(fileName);
                        if (namesOfFilesToDelete.contains(files[i].getName())) {

                            namesOfFilesToDelete.remove(files[i].getName());
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

//    private static void sendFile(URL url, File file, Context context) {
//        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
//        try {
//            urlConnection.setDoOutput(true);// set it for output
//            //urlConnection.setChunkedStreamingMode(0);
//            //urlConnection.setFixedLengthStreamingMode(); // get content length The number of bytes
//
//            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
//            writeStream(out);
//
//            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
//            readStream(in);
//        } finally {
//            urlConnection.disconnect();
//        }
//
//
//    }

    public static void setComponentState(Context context, Class componentClass, boolean enabled) {

        // Alex: maybe just use context
        // dynamically (programmatically) enabled/disabled state is kept across reboots
        PackageManager pm = context.getApplicationContext().getPackageManager();

        ComponentName componentName = new ComponentName(
                context.getApplicationContext(),
                componentClass
        );

        int state = enabled ?
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
    }

    // class name
    // this.getClass().getSimpleName(); //this.getLocalClassName();// getBaseContext().getLocalClassName();
    // getApplicationContext().getLocalClassName();//MainActivity.class.getSimpleName();

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
