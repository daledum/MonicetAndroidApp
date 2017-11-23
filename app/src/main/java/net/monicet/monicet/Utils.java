package net.monicet.monicet;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

/**
 * Created by ubuntu on 07-02-2017.
 */

public final class Utils {
    private Utils() {}

    public static final String FINISHED = "finished";
    public static final String TEMP = "temp";
    public static final String ROUTE = "Route";
    public static final String TRIP = "Trip";
    public static final String FOREGROUND_PREFIX = "fgr";
    public static final String MINUTES = "Min";
    public static final String HOURS = "Hours";
    public static final String TIME = "Time";
    public static final long ONE_SECOND_IN_MILLIS = 1000;
    public static final long ONE_MINUTE_IN_MILLIS = 60 * 1000;
    public static final long FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000;
    public static final long ONE_HOUR_IN_MILLIS = 3600000;

    // this is used for dealing with views in the SightingAdapter (when working with Animal end quantity and TimeAndPlace)
    // code smell - hack due to changes done in onclick listener not sticking around (tried using views from main act,
    // running on ui thread...
    public static final int INITIAL_VALUE = 0; // was -1
    public static final int MAX_VALUE = 199;
    public static final int INITIAL_RANK = 999;
    public static final int ASSOCIATED_INDIV_INITIAL_VALUE = 1;

    public static final String PREFS_NAME = "MonicetPrefsFile";
    public static final String GPS_SAMPLING_INTERVAL = "interval";
    public static final String TRIP_DURATION = "duration";
    public static final String TRIP_START_TIME = "time";
    public static final String FILENAME = "filename";
    public static final String USERNAME = "username";
    public static final String ADD_EXTENSION = "addExtension";
    public static final String TIME_ACTIVITY_SAMPLED_GPS = "timeActivitySampledGps";
    public static final String DELETE_TRIP = "delete";
    public static final String ACCOUNT_NAME = "accountName";
    public static final String UNKNOWN_USER = "unknown@unknown.unknown";
    public static final String LATIN_NAME_DELIMITER = "=";
    public static final String ORDER_NAME = "orderName";
    public static final String DOWNLOAD_TIME = "downloadTime";

    public static final String DESTINATION_URL = "http://infohive.org.uk/monicet/send/";
    public static final String SERVER_URL = "http://infohive.org.uk/monicet/";
    public static final String DESTINATION_FOLDER = "send/";
    public static final String CUSTOM_SPECIE_ORDER_FILENAME = "customSpecieOrder.csv";
    public static final String PERMANENT_FILES_DIR = "donotremove/";

    public static final String START_FOREGROUND_SERVICE_FROM_ACTIVITY =
            ".START_FOREGROUND_SERVICE_FROM_ACTIVITY";
//    public static final String RESTART_FOREGROUND_SERVICE_FROM_ACTIVITY =
//            ".RESTART_FOREGROUND_SERVICE_FROM_ACTIVITY";
    public static final String START_FOREGROUND_SERVICE_FROM_BOOT_RECEIVER =
            ".START_FOREGROUND_SERVICE_FROM_BOOT_RECEIVER";
    public static final String STOP_FOREGROUND_SERVICE = ".STOP_FOREGROUND_SERVICE";
    public static final String STOP_GPS_ALARM_INTENT_SERVICE = ".STOP_GPS_ALARM";
    public static final int FOREGROUND_ID = 1234;//get rid?
    public static final int NOTIFICATION_ID = 1235;
    public static final int SENDING_ALARM_REQUEST_CODE = 0;
    public static final int GPS_SAMPLING_ALARM_REQUEST_CODE = 1;
    public static final int OPEN_TRIP_REQUEST_CODE = 2;
    public static final int DELETE_TRIP_REQUEST_CODE = 3;

    public static final String INTENT_CONNECTION_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String START_ACTION = ".START";
    public static final String STOP_ACTION = ".STOP";

    //TODO: new order list feature, get rid if not using
    public static final Semaphore LOCK = new Semaphore(0);

    // back-ups, if directory is null (for usage in SendAndDeleteFiles())
    public static final String EXTERNAL_DIRECTORY =
            Environment.getExternalStorageDirectory() + "/Monicet";
//    public static final String EXTERNAL_DIRECTORY =
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)//Environment.getExternalStorageDirectory()
//            + "/Monicet"; // this should be set in the activity via the same mechanism...should drop this
//    // this is a back-up, it's not being used
//    public static final String INTERNAL_DIRECTORY =
//            Environment.getDataDirectory()
//            + "/data/"
//            + BuildConfig.APPLICATION_ID + "/files";
//    // or hard-coded internal path
//    public static final String INTERNAL_DIRECTORY_HARDCODED =
//            Environment.getDataDirectory()
//            + "/data/net.monicet.monicet/files";

    // this will get set by the MainActivity.. as a default: set it to all the registered extension
    //private static String DIRECTORY; //get rid eventually

    //private static final String DIRECTORY = EXTERNAL_DIRECTORY;//this was here before //TODO: path issue This was the default, together with getdatadir without conntext

    // allow to be set only once, only when it's null, get rid NB won't survive the reboot
//    public static void setDirectory(String dir) {
//        if (DIRECTORY == null) {
//            DIRECTORY = dir;
//        }
//    }

    //should get rid of this, too
    //public static String getDirectory() { return DIRECTORY; }

    public static String getDirectory(Context context) {

        return context.getFilesDir().toString();//TODO: internal version

        //TODO: external version
//        File dir = new File(EXTERNAL_DIRECTORY);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//
//        return dir.toString();
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

    public static boolean sendAndDeleteFiles(Context context) {

        File dir;//reinstate below for testing
//        if (DIRECTORY != null) {
//            dir = new File(DIRECTORY); //TODO: toString? if external remove + remember to use dir everywhere
//        } else {
//            // if this is called before the path was set by the MainActivity
//            // (in case the dynamic receiver is registered, thus (sticky intent) triggered before setting the dir)
//            // default path, when DIRECTORY is null
//            if (context != null) {
//                dir = new File(context.getFilesDir().toString());
//            } else {
//                dir = new File(INTERNAL_DIRECTORY);
//            }
//        }

        //test // TODO: remove this after tests
//        dir = new File(EXTERNAL_DIRECTORY);
        //test

        //test//reinstate this while testing
//        File testFile = new File(dir, "send" + System.currentTimeMillis());
//        try {
//            testFile.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //test

        dir = new File(getDirectory(context)); //comment this out while testing

        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                for(AllowedFileExtension fe: AllowedFileExtension.values()) {
                    if (pathname.getName().toLowerCase().endsWith(fe.toString().toLowerCase())) {
                        return true;
                    }
                }
                return false;
            }
        };

        // firstly, check that there is an Internet connection
        if (isConnected(context)) {

            boolean toDownload = false;
            // We do not want more threads to try to download and write the same file at the same time
            long lastDownloadTime = Utils.readTimeMillisFromSharedPrefs(context);
            // If I get 0, this means that the file was never downloaded or that the OS deleted the value from Shared Prefs...
            // If I get more than a second between now and the moment another method call (sendFiles) downloaded the file...
            // In either case I want to download the file
            if (lastDownloadTime == 0 ||
                    System.currentTimeMillis() - lastDownloadTime > Utils.ONE_SECOND_IN_MILLIS) {

                // write the current time to shared prefs, so that if another method fires,
                // it knows when another method call started to try to download the file (so that it doesn't try, too)
                Utils.writeTimeMillisToSharedPrefs(context, System.currentTimeMillis());
                toDownload = true;
            }

            // secondly, check if the directory exists
            if (dir.exists()) {
                // thirdly, if it has any of our files
                if (dir.listFiles(fileFilter).length > 0) {

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
                                url = new URL(SERVER_URL + DESTINATION_FOLDER);//was new URL(DESTINATION_URL)
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

            // positioned writing start time at beginning of this method because thus will take longer then speciesOrderLogic method
            // ?otherwise we would have a race between two runner of the same speed

//            // We do not want more threads to try to download and write the same file at the same time
//            long lastDownloadTime = Utils.readTimeMillisFromSharedPrefs(context);
//            // If I get 0, this means that the file was never downloaded or that the OS deleted the value from Shared Prefs...
//            // If I get more than a second between now and the moment another method call (sendFiles) downloaded the file...
//            // In either case I want to download the file
//            if (lastDownloadTime == 0 ||
//                    System.currentTimeMillis() - lastDownloadTime > Utils.ONE_SECOND_IN_MILLIS) {
//
//                // if successful, write the current time to shared prefs, so that if another method fires,
//                // it knows when another method call started to try to download the file (so that it doesn't try, too)
//                Utils.writeTimeMillisToSharedPrefs(context, System.currentTimeMillis());
//                // in this case I am connected to the Internet (but a connection does not exist)
//                downloadFile(context, CUSTOM_SPECIE_ORDER_FILENAME);// returns the file or null
//            }

            if (toDownload) {
                // in this case I am connected to the Internet (but a connection does not exist)
                downloadFile(context, CUSTOM_SPECIE_ORDER_FILENAME);// returns the file or null
            }
        }

        // method is successful if it has sent and therefore deleted all the files
        // returns true if directory doesn't exist or there aren't any of our files in it
        // returns false if directory exists and it has at least one of our files in it
        return !(dir.exists() && dir.listFiles(fileFilter).length > 0);
    }

    public static boolean isConnected(Context context) {
        if (context != null) {
            ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
        } else {
            // assume it's connected if we get a null context
            return true;
        }
    }

    public static File getFile(Context context, String fileName) {

        File file = null;
        File dir = new File(getDirectory(context));

        if (dir.exists()) {
            File permanentFilesDir = new File(dir, PERMANENT_FILES_DIR);
            if (permanentFilesDir.exists()) {
                file = new File(permanentFilesDir, fileName);
                if (file.exists()) {
                    return file;
                }
            }
        }

        return file;
    }

//    public static boolean deleteFile(Context context, String fileName) {
//
//        File dir = new File(getDirectory(context));
//
//        if (dir.exists()) {
//            File permanentFilesDir = new File(dir, PERMANENT_FILES_DIR);
//            if (permanentFilesDir.exists()) {
//                File file = new File(permanentFilesDir, fileName);
//                if (file.exists()) {
//                    return file.delete();
//                }
//            }
//        }
//
//        return true;
//    }

    private static File newlyCreatedFile(File dir, String fileName) {

        File file = new File(dir, fileName);

        try {
            //createNewFile atomically creates a new, empty file named by this abstract pathname
            // if and only if a file with this name does not yet exist.
            // Returns true if the named file does not exist and was successfully created;
            // false if the named file already exists
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file;
    }

    // This method, in case the file with fileName already exists, it returns it
    public static File createFile(Context context, String fileName) {

        File file = null;
        File dir = new File(getDirectory(context));

        if (dir.exists()) {
            File permanentFilesDir = new File(dir, PERMANENT_FILES_DIR);
            if (permanentFilesDir.exists()) {
                file = newlyCreatedFile(permanentFilesDir, fileName);
            } else {
                if (permanentFilesDir.mkdirs()) {
                    file = newlyCreatedFile(permanentFilesDir, fileName);
                }
            }
        } else {
            if (dir.mkdirs()) {
                File permanentFilesDir = new File(dir, PERMANENT_FILES_DIR);
                if (permanentFilesDir.mkdirs()) {
                    file = newlyCreatedFile(permanentFilesDir, fileName);
                }
            }
        }

        return file;
    }

    public static File downloadFile(Context context, String fileName) {

        HttpURLConnection connection = null;
        URL url;
        InputStream input = null;
        OutputStream output = null;
        File file = null;

        try {
            url = new URL(SERVER_URL + fileName);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            url = null;
        }

        if (url != null) {
            try {

                connection = (HttpURLConnection)url.openConnection();
                connection.setReadTimeout((int)ONE_SECOND_IN_MILLIS * 5);
                connection.setConnectTimeout((int)ONE_SECOND_IN_MILLIS * 5);
                connection.connect();//redundant?
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;//"it's not HTTP OK";
                }

                input = connection.getInputStream();

                //                File dir = new File(getDirectory(context));
//                if (!dir.exists()) {
//                    dir.mkdirs();
//                }
//
//                File permanentFilesDir = new File(dir, PERMANENT_FILES_DIR);
//                if (!permanentFilesDir.exists()) {
//                    permanentFilesDir.mkdirs();
//                }
//
//                file = new File(permanentFilesDir, fileName);
//
//                if (!file.exists()) { // if the file doesn't already exist, then create it
//                    try {
//                        file.createNewFile();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        return "could not create file when file did not exist before";//return null;//this was thrown
//                    }
//                } else { // the file already exists
//                    // delete it first (safe to delete it here, the download is going smoothly)
//                    if (!file.delete()) {
//                        // if we could not delete it from the first try - maybe it
//                        // was being updated by a different service - we try again
//                        file.delete();
//                    }
//                    // then we try to re-create it
//                    try {
//                        file.createNewFile();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        return "could not create file when file already existed";//return null;
//                    }
//                }

                // see if the file already is present on the mobile device
                file = getFile(context, fileName);
                if (file == null) {
                    //it does not exist, so we create a new one
                    file = createFile(context, fileName);
                } else {
                    // it exists, so we delete it and put the new one on top
                    // safe to delete it here when the download is going smoothly
                    if (!file.delete()) {
                        // if we could not delete it from the first try - maybe it
                        // was being updated by a different service - we try again
                        file.delete();
                    }

                    // then we try to re-create it
                    file = createFile(context, fileName);
                }

                if (file != null) {

                    output = new FileOutputStream(file);

                    byte data[] = new byte[4096];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                }
            } catch (IOException e) { // this will catch timeoutexceptions, too
                e.printStackTrace();
                return null;//"IOException main download method";
            } finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        return file; //"end of method";
    }

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

    public static void stopForegroundService(Context context, boolean addExtension) {
        Intent stopIntent = new Intent(context, ForegroundService.class);
        stopIntent.setAction(STOP_FOREGROUND_SERVICE);
        stopIntent.putExtra(ADD_EXTENSION, addExtension);
        context.startService(stopIntent);
    }

    public static void writeAccountNameToSharedPrefs(Context context, String accountName) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ACCOUNT_NAME, accountName);
        editor.apply();
    }

    public static void clearAccountNameFromSharedPrefs(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ACCOUNT_NAME, null);
        editor.apply();
    }

    public static String readAccountNameFromSharedPrefs(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, 0);
        // default null is returned if prefs don't exist (no app run yet etc)
        // or if user did not give us their account yet or if app cleared the account name
        return sharedPref.getString(ACCOUNT_NAME, null);
    }

    public static void writeOrderNameToSharedPrefs(Context context, String orderName) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ORDER_NAME, orderName);
        editor.apply();
    }

    public static String readOrderNameFromSharedPrefs(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, 0);
        // default null is returned if prefs don't exist (no app run yet etc)
        // or if app/system cleared the order name
        return sharedPref.getString(ORDER_NAME, null);
    }

    public static void writeTimeMillisToSharedPrefs(Context context, long timeMillis) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(DOWNLOAD_TIME, timeMillis);
        editor.apply();
    }

    public static long readTimeMillisFromSharedPrefs(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFS_NAME, 0);
        // default 0 is returned if prefs don't exist (no app run yet etc)
        // or if app/system cleared the account name or if no file download yet
        return sharedPref.getLong(DOWNLOAD_TIME, 0);
    }

    // this requires read contacts permission
    public static String getUserCredentials(Context context) {//TODO: get rid, eventually (gps intent service last one to use this)
        String emailAddresses = "";

//        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
//        Account[] accounts = AccountManager.get(context).getAccounts();
//
//        for (Account account : accounts) {
//            if (emailPattern.matcher(account.name).matches()) {
//                emailAddresses += account.name + ",";
//            }
//        }
//
//        return emailAddresses.substring(0, emailAddresses.length() - 1);
        return UNKNOWN_USER;//test - get rid
    }

    public static void writeTimeAndCoordinates(BufferedWriter bufferedWriter,
                                               Map<Long, double[]> routeData) throws IOException {

        for (Map.Entry<Long, double[]> entry : routeData.entrySet()) {
            double[] coordinates = entry.getValue();
            bufferedWriter.append(entry.getKey().toString());
            bufferedWriter.append(",");
            bufferedWriter.append("" + coordinates[0]);
            bufferedWriter.append(",");
            bufferedWriter.append("" + coordinates[1]);
            bufferedWriter.newLine();
        }
    }

//    public static String getSplitName(String name) {
//
//        int i = 0;
//        for (i = name.length() - 1; i >= 0; i--) {
//            if (Character.isUpperCase(name.charAt(i))) { break; }
//        }
//
//        return name.substring(0, i) + " " + name.substring(i).toLowerCase();
//    }

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
