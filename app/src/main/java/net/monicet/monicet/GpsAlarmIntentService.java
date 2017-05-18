package net.monicet.monicet;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;

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
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class GpsAlarmIntentService extends IntentService implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    HashMap<Long,double[]> routeData = new HashMap<Long,double[]>();

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
        if (intent != null) {
            String fileName = intent.getStringExtra("fileName");
            if (fileName != null) {
                // This means I am called by the Foreground Service via the Alarm Manager and I am getting the fileName
                // fileName is not set as an extra only when the alarm is cancelled (the alarm is not set then, anyway)

                // test starts here
                File dir = new File(Utils.EXTERNAL_DIRECTORY);
                File testFile = new File(dir, System.currentTimeMillis() + fileName);
                try {
                    testFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //test

                // this doesn't need to know the interval (alarm is triggered after that interval)

                // this should have access to maybe name of file to write to (if after reboot, service is restarted, but it won't have the ID anymore)
                // service should check before starting alarms that there is a need for sampling the gps data and should pass the name of the file to the alarm
                // so that the alarm passes it to the gps sampler

                // it should:
                // sample GPS coords for 1 minute (with a 1 or 2 sec interval and copy them to a map), which is then written to a file

                // should the writing to file be in a thread, too?

                // if interrupted it should just stop the api client etc

                // after 1 minute
                // stop the google api client etc
                /////////
                // the alarm receiver should sample gps every interval + 1 Min (it samples for a minute)
                // and add to a file containing the ID (time it started)
                // if alarm rec sees that difference between the current time and the ID is larger than the duration, it should
                // send an intent to the foreground service

            }
        }
    }

    protected void appendToFile(File file) {
        if (file.exists()) {
            //logic here
        } else {
            //logic here
        }
        // work on the exception logic here, maybe see what to do in the catch, too
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            Utils.writeTimeAndCoordinates(bufferedWriter, routeData);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            Log.d("GpsAlarmIntentService", "File IOException");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        //remember to check the accuracy

    }
}
