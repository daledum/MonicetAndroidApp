package net.monicet.monicet;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import static android.R.attr.action;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class GpsAlarmIntentService extends IntentService implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    HashMap<Long,double[]> routeData = new HashMap<Long,double[]>();
    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequest;
    private File file = null;
    private String userName = null;

    public GpsAlarmIntentService() {
        // Used to name the worker thread, important only for debugging.
        super("GpsAlarmIntentService");
    }

//    @Override
//    public void onCreate() {
//        super.onCreate(); // if you override onCreate(), make sure to call super().
//        // If a Context object is needed, call getApplicationContext() here.
//    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent == null) {
            stopSelf();
        } else {
            String fileName = intent.getStringExtra(Utils.FILENAME);

            if (fileName == null) {
                stopSelf();
            } else {
                // This means I am called by the Foreground Service via the Alarm Manager and I am getting the fileName
                // fileName is not set as an extra only in the scenario when the alarm is cancelled (the alarm is not set then, anyway)

                // test starts here .. file name should be fgrMinMHoursHTimeId - did not get action
//                File dir = new File(Utils.EXTERNAL_DIRECTORY);
//                File testFile = new File(dir, String.valueOf((int)(System.currentTimeMillis()/1000000)) + fileName);
//                try {
//                    testFile.createNewFile();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                //test, get rid

                long tripDuration = intent.getExtras().getLong(Utils.TRIP_DURATION);

                SharedPreferences sharedPref = getSharedPreferences(Utils.PREFS_NAME, 0);
                // there will definitely be something there (fixGpsSignal writes to it pre-START button press)
                long lastTimeActivitySampledGps = sharedPref.getLong(Utils.TIME_ACTIVITY_SAMPLED_GPS, 0);
                userName = intent.getExtras().getString(Utils.USERNAME);

                // If it's been more than H hours since the last time when the activity sampled gps coordinates,
                // stop the alarm..thus stopping this gps sampling service
                // (H is the number of hours the user said the trip will last, aka the trip duration)
                if (System.currentTimeMillis() > (lastTimeActivitySampledGps + tripDuration)) {

                    Intent stopIntent = new Intent(this, ForegroundService.class);
                    stopIntent.setAction(Utils.STOP_GPS_ALARM_INTENT_SERVICE);
                    startService(stopIntent);
                    stopSelf();
                } else {

                    file = new File(Utils.getDirectory(), fileName);
                    // Not checking if it exists here, because I have called the above constructor and it just created new files
                    // on the drive, without calling createNewFile() or anything

                    // see if the file exists
                    boolean fileExisted = false;
                    try {
                        // createNewFile() creates a new, empty file named by this abstract pathname
                        // if and only if a file with this name does not yet exist and
                        // returns true if the named file does not exist and was successfully created
                        // false if the named file already exists
                        if(!file.createNewFile()) {
                            // this means that the file already existed
                            fileExisted = true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (!fileExisted) {
                            file.delete();
                            file = null;
                        }
                    }

                    if (fileExisted) {
                        // This means that the file seen by (or created by the foreground service) still exists
                        // this file is needed here (we add the coordinates to it)
                        createLocationRequest();
                        buildGoogleApiClient();
                        mGoogleApiClient.connect();
                        try {
                            Thread.sleep(Utils.ONE_MINUTE_IN_MILLIS);
                        } catch (InterruptedException e) {
                            Log.d("GpsAlarmIntentService", "1 minute wait interrupted");
                        } finally {
                            appendToFile();
                            stopSelf();
                        }
                    } else {
                        // The file doesn't exist anymore
                        // 1 It was deleted by the user/app. The file existed before (foreground service makes sure of that).
                        // a App deletes it when back is pressed and user wants the trip to be deleted (or when, via notif deletes the trip).
                        // All files containing that ID (startingTime) would be then deleted.
                        // b user deletes it. Then, maybe the extensionless tempTrip file is still there. This overlaps with scenario 2
                        // 2 The foreground service gave it an extension (only after SEND was pressed). In this case, the foreground service also
                        // stopped the alarm which triggers this service, so, do nothing. SEND was pressed, so the other files have extensions, too?
                        // Maybe my alarm triggers before the activity adds the extension to any of the trip or route files.
                        // And the fgr file with extension could have been sent already (sending mechs are alive). So, we could have
                        // no fgr file and an extensionless trip or route file...? For a short time, at least.
                        stopSelf();
                    }
                }
            }
        }
    }

    protected void appendToFile() {
        // first stop the location updates writing to the routeData you'll be reading soon
        stopGps();

        if (file != null && file.exists()) {

            String fileName = file.getName();

            try {

                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
                bufferedWriter.append(userName);
                bufferedWriter.append(",");
                bufferedWriter.append(fileName);
                //NB if this extension changes in ForegroundService, where it's added, it should change here, too
                bufferedWriter.append(AllowedFileExtension.CSV.toString());
                bufferedWriter.newLine();
                Utils.writeTimeAndCoordinates(bufferedWriter, routeData);
                bufferedWriter.flush();
                bufferedWriter.close();

            } catch (IOException e) {

                Log.d("GpsAlarmIntentService", "File IOException when adding to fgr - first try");

                // The file existed, but, there were problems when writing to it. Or maybe it was deleted during the write.
                if (file.exists()) {
                    // the file still exists, so, try again

                    try {

                        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
                        Utils.writeTimeAndCoordinates(bufferedWriter, routeData);
                        bufferedWriter.flush();
                        bufferedWriter.close();

                    } catch (IOException exc) {

                        Log.d("GpsAlarmIntentService", "File IOException when adding to fgr - second try");

                        // Still not working: delete file and create a new one, with the same name
                        if (file.delete()) {

                            File dir = new File(Utils.getDirectory());
                            File newFile = new File(dir, fileName);
                            try {
                                newFile.createNewFile();
                            } catch (IOException exception) {
                                Log.d("GpsAlarmIntentService", "File IOException - couldn't create a new fgr file");
                            }
                        }

                    }
                }

            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(Utils.ONE_SECOND_IN_MILLIS)
                .setFastestInterval(Utils.ONE_SECOND_IN_MILLIS);
    }

    protected void stopGps() {
        //This can be called multiple times in a row, without error
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {// Doc:  It must be connected at the time of this call
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            }

            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // TODO: Will the background gps sampling be allowed to work without permission in the newer APIs?
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.
                    requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Log.d("GpsAlarmIntentService", "No GPS Fine Location Permission");
            stopGps();
            stopSelf();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        appendToFile();
        stopSelf();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        appendToFile();
        stopSelf();
    }

    @Override
    public void onLocationChanged(Location location) {
        //if (location.getAccuracy() < 100.0f) {
        //}
        routeData.put(System.currentTimeMillis(),
                new double[]{location.getLatitude(), location.getLongitude()});

    }

    @Override
    public void onDestroy() {
        // onDestroy might not always be called by the OS
        stopGps();
        super.onDestroy();
    }

}
