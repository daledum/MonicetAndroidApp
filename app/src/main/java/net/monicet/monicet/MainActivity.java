package net.monicet.monicet;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static android.R.string.no;
import static java.lang.Math.abs;
import static java.lang.Math.log;

public class MainActivity extends AppCompatActivity implements
        MainActivityInterface,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    final Trip trip = new Trip();
    final Sighting[] openedSightings = new Sighting[1]; // artifact/hack so I can use it inside anonymous classes
    final ArrayList<Animal> seedAnimals = new ArrayList<Animal>();
    final ArrayAdapter[] arrayAdapters = new ArrayAdapter[2];
    // Declare and initialize the receiver dynamically // TODO: maybe this should be done in a singleton, application level
    final BroadcastReceiver dynamicReceiver = new DynamicNetworkStateReceiver(); // or declare the class here, occupying more space

    private final GpsModeState gpsModeState = new GpsModeState();
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;//TODO: no point for volatile here - get rid later
//    Volatile only has relevance to modifications of the variable itself, not the object it refers to.
//    A volatile field gives you guarantees as what happens when you change it. (Not an object which it might be a reference to)

    private volatile Location mostRecentLocation;// TODO: no need for volatile here, either, I'm only changing it's lat and long values

    {
        mostRecentLocation = new Location("dummyprovider");
        mostRecentLocation.setLatitude(Utils.INITIAL_VALUE);
        mostRecentLocation.setLongitude(Utils.INITIAL_VALUE);
    }

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    // single thread executor for capturing gps coordinates
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: if coming back from a config change - I should first check if the views are visible?
        if (areGooglePlayServicesInstalled()) {
            // if google play services are OK
            getUserCredentials();

            // create loc request so that when google api client connects - shouldn't matter, it's ready (does it onStart)
            createLocationRequest();
            buildGoogleApiClient();

            // set data directory, where files exist - used by the SEND button logic, by receivers, alarm and GCM
            setDataDirectory();

            //registerDynamicReceiver();

            // create seed animals from resources,containing specie names, photos and description
            // (to feed the custom ListView ArrayAdapter)
            buildSeedAnimalsFromResources();

            // create animal adapter (which uses seed animals) +
            // create the custom Sightings ArrayAdapter and populate it will null
            makeAndSetArrayAdapters();

            // TODO: should I use this or MainActivity.this
            // initialize and show the views (with their logic)... list views, buttons, labels
            initViews();
        } else {
            //dialog
            // trip is by default on SLOW, no Google Play Services, so switch to OFF
            gpsModeState.setUserParentGpsMode(null);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // If executor service has been made null (I'm coming here after onStop())
        // make a new one
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor(); // this is for future coordinate capturing tasks

            // Finish off any tasks that might have been interrupted in onStop()
            // Call this before re-connecting the google api client (the client will set mostRecentLocation again)
            finishTimeAndPlaces(getUnfinishedTimeAndPlaces());
        }

        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {

        // this might come after .shutdown() or shutdownNow() on the same object
        if (!executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        executorService = null;

        // if threads were interrupted - they might have left the interval in FAST mode
        // interval and distance get turned back on to their original state in onStart (when google api client connects..in onConnected)
        stopLocationUpdates();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void openSighting(String label, Sighting sighting) {
        // set openedSighting - to be later used by SAVE
        openedSightings[0] = sighting; // TODO: issues here?

        // set label
        setTitle(label);

        // TODO: issue - animal array adapter doesn't focus on the clicked specie (array adapter remembers last position)
        // how to focus array adapter on a specific item
        // I can start with the first specie if I clear and setdataall seedanimals (start with null)

        // first, clean the seed animals - maybe use INITIAL_VALUE
        for (Animal seedAnimal: seedAnimals) {
            seedAnimal.setStartQuantity(0);
            seedAnimal.setEndQuantity(0);
        }

        Animal animal = sighting.getAnimal();
        if (animal != null) {
            String specieName = animal.getSpecie().getName();

            for (Animal seedAnimal: seedAnimals) {
                // set the end quantity for all animals to be the end quantity of the sighting's animal
                // so that we keep this value when we save
                seedAnimal.setEndQuantity(animal.getEndQuantity());

                if (specieName.equals(seedAnimal.getSpecie().getName())) {
                    // this is the same animal (with the same specie as this sighting's animal)
                    // Insert the sighting's start animal quantity within this animal for displaying in the animal adapter
                    seedAnimal.setStartQuantity(animal.getStartQuantity());
                }
            }
        }

        // let the animal adapter know that the seedAnimals changed
        arrayAdapters[0].notifyDataSetChanged();

        // make sighting list view invisible
        findViewById(R.id.list_view_sightings).setVisibility(View.INVISIBLE);
        // make "no sightings message" invisible
        findViewById(R.id.no_sightings_text_view).setVisibility(View.INVISIBLE);

        // make animal list view visible
        findViewById(R.id.list_view_animals).setVisibility(View.VISIBLE);

        findViewById(R.id.fab_add).setVisibility(View.INVISIBLE);
        findViewById(R.id.fab_send).setVisibility(View.INVISIBLE);
        findViewById(R.id.fab_save).setVisibility(View.VISIBLE);
        findViewById(R.id.fab_back).setVisibility(View.VISIBLE);
    }

    // method called by START, BACK, SAVE, DELETE, STOP/END and Comments buttons
    @Override
    public void showSightings() {
        // set label
        setTitle(getText(R.string.app_name) +  " - " + getText(R.string.my_sightings));

        // hide START button
        findViewById(R.id.fab_start).setVisibility(View.INVISIBLE);
        // hide user GPS interval box
        findViewById(R.id.gps_user_interval_box).setVisibility(View.INVISIBLE);
        // hide BACK button
        findViewById(R.id.fab_back).setVisibility(View.INVISIBLE);
        // hide SAVE button
        findViewById(R.id.fab_save).setVisibility(View.INVISIBLE);
        // hide animals list view
        findViewById(R.id.list_view_animals).setVisibility(View.INVISIBLE); // it was already INVISIBLE, when coming from START

        // show ADD button
        findViewById(R.id.fab_add).setVisibility(View.VISIBLE);
        // show SEND button
        findViewById(R.id.fab_send).setVisibility(View.VISIBLE);

        if (trip.getNumberOfSightings() == 0) {
            // show the no sightings message (its text is set in XML), if the trip is empty
            findViewById(R.id.no_sightings_text_view).setVisibility(View.VISIBLE);
            findViewById(R.id.list_view_sightings).setVisibility(View.INVISIBLE);
        } else {
            // show sightings list view, if the trip has any sightings
            findViewById(R.id.list_view_sightings).setVisibility(View.VISIBLE);
            findViewById(R.id.no_sightings_text_view).setVisibility(View.INVISIBLE);
        }

        // show the "wait for gps signal to fix" message, if gps signal hasn't fixed (we're still in fixing mode)
        // gps signal has fixed if onLocationChanged has fired at least once (changing
        // the mostRecentLocation's lat and long to the location's lat and long..they're no longer on INITIAL_VALUE)
        if (gpsModeState.getCurrentParentGpsMode() == GpsMode.FIXING) {
            findViewById(R.id.wait_for_gps_fix_textview).setVisibility(View.VISIBLE);
        }

        // update sighting adapter
        arrayAdapters[1].clear();
        arrayAdapters[1].addAll(trip.getSightings());
        arrayAdapters[1].notifyDataSetChanged();
    }

    @Override
    public void showSightingCommentsDialog(final Sighting sighting) {
        // TODO: take other smartphone gps reading (from trip, sighting, animal)
        // and compare the sign (if near the 0 degree point, don't do this check)
        LayoutInflater layoutInflater = LayoutInflater.from(this);

        View rootView = layoutInflater.inflate(R.layout.comments_dialog, null);
        final EditText latitudeDegrees = (EditText)rootView.findViewById(R.id.lat_degrees_edit_text);
        latitudeDegrees.setText(String.valueOf(sighting.getUserEndTimeAndPlace().getLatitude()));
        final EditText latitudeMinutes = (EditText)rootView.findViewById(R.id.lat_minutes_edit_text);
        final EditText latitudeSeconds = (EditText)rootView.findViewById(R.id.lat_seconds_edit_text);

        final EditText longitudeDegrees = (EditText)rootView.findViewById(R.id.long_degrees_edit_text);
        longitudeDegrees.setText(String.valueOf(sighting.getUserEndTimeAndPlace().getLongitude()));
        final EditText longitudeMinutes = (EditText)rootView.findViewById(R.id.long_minutes_edit_text);
        final EditText longitudeSeconds = (EditText)rootView.findViewById(R.id.long_seconds_edit_text);

        final EditText comments = (EditText)rootView.findViewById(R.id.comments_edit_text);
        comments.setText(sighting.getUserComments());

        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(this);

        comAlertDialogBuilder.setTitle(R.string.comments_message_title);

        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // take and set the user's latitude
                double gpsDegrees = Utils.parseGpsToDouble(
                        latitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LATITUDE
                );
                double gpsMinutes = Utils.parseGpsToDouble(
                        latitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                double gpsSeconds = Utils.parseGpsToDouble(
                        latitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                sighting.getUserEndTimeAndPlace().setLatitude(
                        Utils.convertDegMinSecToDecimal(gpsDegrees, gpsMinutes, gpsSeconds)
                );

                // take and set the user's longitude
                gpsDegrees = Utils.parseGpsToDouble(
                        longitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LONGITUDE
                );
                gpsMinutes = Utils.parseGpsToDouble(
                        longitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                gpsSeconds = Utils.parseGpsToDouble(
                        longitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                sighting.getUserEndTimeAndPlace().setLongitude(
                        Utils.convertDegMinSecToDecimal(gpsDegrees, gpsMinutes, gpsSeconds)
                );

                // take the system's time
                // this is giving me the time when they edited the comments the last time
                sighting.getUserEndTimeAndPlace().setTimeInMillis(System.currentTimeMillis());

                // take and set the user's comments
                sighting.setUserComments(comments.getText().toString());

                // refresh the views (maybe the final quantity was changed)
                showSightings();
            }
        });
        comAlertDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dialog.dismiss();
            }
        });

        comAlertDialogBuilder.setView(rootView);
        comAlertDialogBuilder.create();
        comAlertDialogBuilder.show();
    }

    @Override
    public void deleteSightingCommentsDialog(final Sighting sighting) {
        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(this);

        comAlertDialogBuilder.setTitle(R.string.delete_sighting_title);
        comAlertDialogBuilder.setMessage(R.string.delete_sighting_message);

        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                trip.getSightings().remove(sighting);
                // refresh the views
                showSightings();
            }
        });
        comAlertDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dialog.dismiss();
            }
        });

        comAlertDialogBuilder.create();
        comAlertDialogBuilder.show();
    }

    // capture time and place is called by a different thread every time
    // one thread starts when START trip is pressed - this is and must be the first time the method is called
    // all other threads must work after this first one
    // another thread starts when ADD sighting is pressed - this must take place before the STOP thread for the same sighting
    // another thread starts when STOP is pressed - this must take place after the ADD sighting thread for the same sighting
    // all threads must take place before the SEND trip one
    // one thread at a time - share the gps mode of the trip - they change it, so no overlapping, please
    // last thread must be when SENDing the trip (time and place for the end of the trip)

    @Override
    public void captureCoordinates(final TimeAndPlace timeAndPlace) {

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                // here, changing the interval and smallest distance, for fast sampling.
                // It must be called on thread which registered the location request
                RunnableFuture<Void> task = new FutureTask<Void>(new Runnable() {
                    @Override
                    public void run() {

                        // go into sampling mode and restart location updates with the sampling interval
                        updateSamplingGpsMode(GpsMode.SAMPLING);
                    }
                }, null);

                runOnUiThread(task);
                // wait for the task to finish (wait between 0 and 10 seconds)
                try {
                    task.get(10, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    task.cancel(false);
                }

                // wait for the location to capture something (2 seconds)
                // what if user was stationary throughout
                try {
                    Thread.sleep(2 * GpsMode.SAMPLING.getIntervalInMillis());
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // sample GPS coordinates
                // no need to make it volatile, timeAndPlace is written only by this thread and only one of these threads can run at the same time
                // onStart, if unfinished TimeAndPlaces...they are filled in before starting new captureCoordinates threads
                // if the timeAndPlace object still exists (maybe I captured after ADD, but then I pressed BACK)
                if (timeAndPlace != null) {
                    timeAndPlace.setLatitude(mostRecentLocation.getLatitude());
                    timeAndPlace.setLongitude(mostRecentLocation.getLongitude());
//                    try {
//                        timeAndPlace.setLatitude(LocationServices.FusedLocationApi.getLastLocation(
//                                mGoogleApiClient).getLatitude());
//                        timeAndPlace.setLongitude(LocationServices.FusedLocationApi.getLastLocation(
//                              mGoogleApiClient).getLongitude());
//                    } catch (SecurityException e) {
//
//                    }

                }

                // when done sampling, go back to the original sampling interval and smallest displacement
                // no need to wait for the interval to be set again, only this method, onStart and changes of GPS mode -  all do it on the same thread?
                // and if another capture coordinate runnable is started, the new runOnUiThread will pass the runnable to the MainActivity looper,
                // and the looper will run it after the old runOnUiThread runnable has finished
                // onStart sets the gps mode interval and distance to the trip's values, when calling googleapiclient.connect()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // this mode might be changed while this runnable is running by the UI thread, runnable is run on the UI thread, but
                        // just to be on the safe side GpsMode is volatile
                        // TODO: now - get back to ....1 either the user mode or 2 the fixing mode...how do I know which one is active now?
                        // if main thread changed the mode while I was here, I should just get back to it?
                        // let's say I interrupted the fixing mode, I went into sampling mode...no other changes were made to the mode

                        // Here check if the current mode is the sampling mode... (new object)
                        // a - If not, that means the gps mode was changed while I was doing my thing. It could have only gone into user mode from
                        // the fixing mode, or the user interval value could have changed
                        // b - if it's still the sampling mode, go back into the original mode

                        // go back into the current parent mode and restart location updates with the new interval
                        updateSamplingGpsMode(gpsModeState.getCurrentParentGpsMode());
                    }
                });
            }
        });
    }

    @Override
    public Activity getMyActivity() {
        return this;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        // I check every time the Google API client connects - because user can revoke permission on newer Android APIs
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            // permission had already been granted, start requestLocationUpdates with interval and distance from the trip's gps mode (its normal mode)
            // was trip get gps mode
            startLocationUpdates();

        } else { // permission had not been granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                // app-defined int constant. The callback method gets the result of the request.
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);// this was here by default

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the location-related task you need to do.
                    startLocationUpdates();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    gpsModeState.setUserParentGpsMode(null);
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //TODO: get rid of this
        Log.i("MainActivity", "GoogleApiClient connection has been suspend");
        // attempt to re-establish the connection.
        //stopLocationUpdates();
        //mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("MainActivity", "GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {

        // this means that I'm in fixing...which ends here, the first time coordinates are captured
        // so, get rid of the "wait for gps to fix" text
        findViewById(R.id.wait_for_gps_fix_textview).setVisibility(View.INVISIBLE);

        // gps has been fixed if this method fired, so get off the fixing mode (and if not sampling, update the interval right now)
        if (gpsModeState.getCurrentParentGpsMode() == GpsMode.FIXING) { updateGpsModeState(); }
        // else - I was not in fixing, that means I was in user mode, so captureCoords will deal with this, if it's running now
        // and if not I changed the mode and restarted loc updates when user changed the interval
        // captureCoord works with currentParentGpsMode - when it does its job, it puts the sampling mode back into
        // the currentParentGpsMode

        // TODO: do I need to check Location is null ?
        mostRecentLocation.setLatitude(location.getLatitude());
        mostRecentLocation.setLongitude(location.getLongitude());

        trip.addRouteData(System.currentTimeMillis(), location.getLatitude(), location.getLongitude());
        //test - get rid of this
        trip.addRouteData(System.currentTimeMillis()+(long)(2550-300+400/300*89*log(3.14)),
                1, mLocationRequest.getInterval());
        //test ends here

    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED && // redundant check - android studio complains otherwise
                mGoogleApiClient.isConnected()) {// Doc:  It must be connected at the time of this call

            // set the values
            mLocationRequest.setInterval(gpsModeState.getSamplingGpsMode().getIntervalInMillis());

            // re/start location updates
            // this interval set here could be changed while inside the captureCoord method, if a late connection, if a change of GpsMode, in onConnected
            // Doc: This method is suited for the foreground use cases, more specifically for requesting locations while being connected to GoogleApiClient.
            // Doc: Any previous LocationRequests registered on this LocationListener will be replaced

            LocationServices.FusedLocationApi.
                    requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) { // Doc:  It must be connected at the time of this call
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    public boolean areGooglePlayServicesInstalled() {

        // check Google Play Services method starts here - gps needs it, gcm has backups
        // make a method out of this? and if it returns false...
        //https://github.com/filipproch/gcmnetworkmanager-android-example/blob/master/app/src/main/java/cz/jacktech/gcmnetworkmanager/MainActivity.java
        //http://stackoverflow.com/questions/22493465/check-if-correct-google-play-service-available-unfortunately-application-has-s
//        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
//        int errorCheck = api.isGooglePlayServicesAvailable(this);
//        if(errorCheck == ConnectionResult.SUCCESS) {
//            //google play services available, hooray
//        } else if(api.isUserResolvableError(errorCheck)) {
//            //GPS_REQUEST_CODE = 1000, and is used in onActivityResult
//            api.showErrorDialogFragment(this, errorCheck, GPS_REQUEST_CODE);
//            //stop our activity initialization code
//            return;
//        } else {
//            //GPS not available, user cannot resolve this error
//            //todo: somehow inform user or fallback to different method, this relates to the GCM too
//            //stop our activity initialization code
//            return;
//        }
        return true;
    }

    protected void getUserCredentials() {
        String emailAddresses = "";
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                emailAddresses += account.name + ",";
            }
        }
        trip.setUserName(emailAddresses.substring(0, emailAddresses.length() - 1));
    }

    // google sample - why synchronized - only the main UI thread calls it
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        // default location request - SLOW
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                //.setInterval(trip.getGpsMode().getIntervalInMillis())// get rid was trip get gps mode get interval
                .setInterval(gpsModeState.getSamplingGpsMode().getIntervalInMillis())
                .setFastestInterval(1000); // 1 second
    }

    protected void finishTimeAndPlaces(List<TimeAndPlace> timeAndPlaceList) {

        // onStop happened, thread was interrupted (did not capture lat or long or neither), then onStart came and
        // this method was called. To finish up the coordinates, use mostRecentLocation (it will be the one from just before the interruption).
        for (TimeAndPlace timeAndPlace: timeAndPlaceList) {
            if (timeAndPlace.getLatitude() == Utils.INITIAL_VALUE) {
                timeAndPlace.setLatitude(mostRecentLocation.getLatitude());
            }
            if (timeAndPlace.getLongitude() == Utils.INITIAL_VALUE) {
                timeAndPlace.setLongitude(mostRecentLocation.getLongitude());
            }
        }
    }

    protected List<TimeAndPlace> getUnfinishedTimeAndPlaces() {
        List<TimeAndPlace> timeAndPlaceList = new ArrayList<TimeAndPlace>();
        // meaning that the time was set
        if (isTimeAndPlaceUnfinished(trip.getStartTimeAndPlace())) {
            timeAndPlaceList.add(trip.getStartTimeAndPlace());
        }
        for (Sighting sighting: trip.getSightings()) {
            if (isTimeAndPlaceUnfinished(sighting.getStartTimeAndPlace())) {
                timeAndPlaceList.add(sighting.getStartTimeAndPlace());
            }
            if (isTimeAndPlaceUnfinished(sighting.getEndTimeAndPlace())) {
                timeAndPlaceList.add(sighting.getEndTimeAndPlace());
            }
        }

        return timeAndPlaceList;
    }

//    protected void finishAllCaptureCoordsTasks() {
//        // shutdown executor service dealing with capturing gps coordinates
//        executorService.shutdown();
//        int numberOfSeconds = 0;
//        // loops for 20 seconds or until it terminates all tasks, whichever comes first
//        boolean isTerminated = executorService.isTerminated();
//        while (!isTerminated && numberOfSeconds < 20) {
//
//            try {
//                // true if this executor terminated and false if the timeout elapsed before termination
//                isTerminated = executorService.awaitTermination(1, TimeUnit.SECONDS);
//            } catch(InterruptedException e) {
//                e.printStackTrace();
//                isTerminated = true; // If tasks were running or waiting, I'll deal with them in onResume
//            }
//
//            numberOfSeconds++;
//        }
//    }

    void shutdownAndAwaitTerminationOfExecutorService() {
        executorService.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(20, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    protected boolean isTimeAndPlaceUnfinished(TimeAndPlace timeAndPlace) {
        // a task hasn't finished writing the gps coordinates to a timeAndPlace (NB the time is written immediately,
        // on the spot, in the main UI thread and not inside the task) if the time has been set, but at least one (lat or long)
        // hasn't been set (it's still on INITIAL_VALUE)
        if (timeAndPlace.getTimeInMillis() != Utils.INITIAL_VALUE &&
                (timeAndPlace.getLatitude() == Utils.INITIAL_VALUE ||
                        timeAndPlace.getLongitude() == Utils.INITIAL_VALUE)) {
            return true;
        }
        return false;
    }

//    protected boolean gpsSignalIsFixed() {
//        return (mostRecentLocation.getLatitude() != Utils.INITIAL_VALUE &&
//                mostRecentLocation.getLongitude() != Utils.INITIAL_VALUE);
//    }

    protected synchronized void updateGpsModeState() {
        // the current mode should be the new user mode
        gpsModeState.setCurrentParentGpsMode(gpsModeState.getUserParentGpsMode());

        if (gpsModeState.getSamplingGpsMode() != GpsMode.SAMPLING) { // if not in the sampling mode
            // make sampling mode the current parent mode
            // and restart location updates (which run in with the sampling interval)
            updateSamplingGpsMode(gpsModeState.getCurrentParentGpsMode());
        } // else, if in the sampling mode, captureCoordinates
        // method (at the end) will make the sampling mode be the current mode
    }

    protected synchronized void updateSamplingGpsMode(GpsMode gpsMode) {
        // update the sampling mode
        gpsModeState.setSamplingGpsMode(gpsMode);
        // and restart location updates (which run on the sampling mode interval)
        startLocationUpdates();
    }

    protected void gpsUserIntervalLogic() {

        List<Long> intervalValues = new ArrayList<Long>(GpsMode.values().length);

        for (GpsMode gpsMode: GpsMode.values()) {
            // take only the intervals equal or larger than one minute
            if (gpsMode.getIntervalInMillis() >= Utils.ONE_MINUTE_IN_MILLIS) {
                intervalValues.add(gpsMode.getIntervalInMillis() / Utils.ONE_MINUTE_IN_MILLIS);
            }
        }
        Collections.sort(intervalValues);

        int i = 0;
        int size = intervalValues.size();
        String[] displayedValues = new String[size];
        for (i = 0; i < size; i++) {
            displayedValues[i] = String.valueOf(intervalValues.get(i));
        }

        for (i = 0; i < displayedValues.length; i++) {
            if (displayedValues[i].equals(String.valueOf(gpsModeState.getUserParentGpsMode().
                    getIntervalInMillis() / Utils.ONE_MINUTE_IN_MILLIS))) {
                break;
            }
        }

        NumberPicker interval = (NumberPicker) findViewById(R.id.gps_user_interval_number_picker);
        interval.setMinValue(0);
        interval.setMaxValue(displayedValues.length - 1);
        interval.setDisplayedValues(displayedValues);
        // set the default value (see constructor of gpsmodestate)
        interval.setValue(i);
    }

    public void setDataDirectory() {
        // before registering the dynamic receiver, which will trigger - inform the package where you saved the files
        // set directory here for the SendAndDeleteFiles Utils method
        Utils.setDirectory(Utils.EXTERNAL_DIRECTORY); // have this in the initData() method - must be called before registerDynamicReceiver()
        //should be Utils.setDirectory(getFilesDir().toString()); // Alex: was, directory.toString(), toString should be optional

    }

    public void registerDynamicReceiver() {
        // Register the dynamic receiver. Once registered it's enabled by default,
        // therefore it could fire on connectivity change before SEND.
        // android.net.conn.CONNECTIVITY_CHANGE is a sticky broadcast, so, the receiver fires when registered
        // in order to stop that, do work in onReceive only if if (!isInitialStickyBroadcast())
        // All the "file sending" receivers are enabled by default and after pressing SEND.
        // Disabling them here (start of onCreate), would stop them from working while the app
        // is alive (not forcefully closed by the user/system). So, let them do their thing.
        // They disable themselves if the work is done/or if there is no work to be done (except
        // the dynamic receiver, which lives and tries to send the files throughout the lifetime of the app)
        IntentFilter filter = new IntentFilter();//TODO: maybe register it in onResume or onStart
        filter.addAction(Utils.INTENT_CONNECTION_ACTION);
        filter.addAction(Utils.START_ACTION);
        getApplicationContext().registerReceiver(dynamicReceiver, filter); // application lifetime or just activity

        // TODO: check after installing the app if dynamic receiver runs even after restart
        // (bad, that means it's not unregistering), when the app is not running
        //check for receiver on monicet package
//        adb shell dumpsys activity broadcasts or
//        dumpsys activity -h
//        dumpsys activity b
//        dumpsys package net.monicet.monicet

    }

    public void buildSeedAnimalsFromResources() {

        // get the resources (specie_names, descriptions, photos)
        String[] specie_names = getResources().getStringArray(R.array.speciesArray);
        // TODO: implement getting the photo ids and description data later
        String[] photos = new String[30];
        String[] descriptions = new String[30]; // all descriptions can be in one single text file
        Arrays.fill(photos, "photo"); // remember to give the photos names like SpermWhale_1, CommonDolphin_X
        Arrays.fill(descriptions, "description");

        // here (if the 3 arrays have the same size, at least check) add each animal to the list, one by one
        int sizeOfArrays = specie_names.length;

        if ( sizeOfArrays != photos.length || sizeOfArrays != descriptions.length) {
            Log.d("MainActivity", "the sizes of the specie_names, photos and descriptions arrays are not the same");
        }

        for (int i = 0; i < sizeOfArrays; i++ ) {
            Specie specie = new Specie(specie_names[i], photos[i], descriptions[i]);
            seedAnimals.add(new Animal(specie));
        }

    }

    public void makeAndSetArrayAdapters() {
        arrayAdapters[0] = new AnimalAdapter(this, seedAnimals);

        // giving it null here, because the trip doesn't have any sightings, yet
        arrayAdapters[1] = new SightingAdapter(
                this,
                new ArrayList<Sighting>(Arrays.asList(new Sighting[]{null}))
        );

    }


    // method called by BACK and SAVE
    public void disconnectOpenedSighting() {
        // Java is Pass-by-value/Call by sharing - therefore the referred object will not be nullified
        openedSightings[0] = null;
    }

    public void initViews() {
        // set label to MONICET - START TRIP
        setTitle(getText(R.string.app_name) + " - " + getText(R.string.start_trip));

        // TODO: gps user interval view here
        gpsUserIntervalLogic();

        // set animal adapter to custom list view
        ((ListView) findViewById(R.id.list_view_animals)).setAdapter(arrayAdapters[0]);

        // set sighting adapter to custom list view
        ((ListView) findViewById(R.id.list_view_sightings)).setAdapter(arrayAdapters[1]);

        // buttons - this must come after the adapter initialization procedure
        // START - starts the trip
        startButtonLogic();

        // ADD - adds a sighting to the trip
        // (this view is displayed at the same time with the Sighting adapter)
        addButtonLogic();

        // SEND - sends the trip (all its sightings) to a server
        // (this view is displayed at the same time with the Sighting adapter)
        sendButtonLogic();

        // BACK - goes back to the view showing the ADD and SEND buttons (and the Sighting adapter)
        //(this view is displayed at the same time with the Animal adapter)
        backButtonLogic();

        // SAVE - saves the sighting (its (new) animal and (new) start quantity
        //(this view is displayed at the same time with the Animal adapter)
        saveButtonLogic();

    }

    public void startButtonLogic() {
        findViewById(R.id.fab_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // a) when pressed takes the value from the gps mode interval number picker
                // (if selected set gpsmode to continuous, trip is on SLOW by default)
                // start of onCreate - Google Play Services might not be installed or no permission for GPS usage
                // normally we should not get to this point if no Google Play Services

                //here get the user interval
                NumberPicker np = (NumberPicker) findViewById(R.id.gps_user_interval_number_picker);
                int indexOfInterval = np.getValue();
                long intervalToCompareWith = (indexOfInterval == 0) ?
                        Utils.ONE_MINUTE_IN_MILLIS : indexOfInterval * Utils.FIVE_MINUTES_IN_MILLIS;

                for (GpsMode gpsMode: GpsMode.values()) {
                    if (gpsMode.getIntervalInMillis() == intervalToCompareWith) {
                        // set the user mode with the newly selected user mode
                        gpsModeState.setUserParentGpsMode(gpsMode);
                        break;
                    }
                }

                // if I'm not in fixing parent mode now, meaning that I am in one of the user
                // modes (the one selected most recently by the user)
                // update the parent mode (to the new user mode) and update interval (if not sampling)
                if (gpsModeState.getCurrentParentGpsMode() != GpsMode.FIXING) { updateGpsModeState(); }
                //else, if I am in fixing mode - onLocationChanged (end of fixing mode) will
                // get the mode into the new user mode

                // b) - time
                trip.getStartTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                // and gps coords
                captureCoordinates(trip.getStartTimeAndPlace());

                // deal with the views
                showSightings(); // shared between the START, SAVE, BACK and DELETE buttons
            }
        });
    }

    public void addButtonLogic() {
        // uses the animal adapter
        findViewById(R.id.fab_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if GPS has connected and fixed on a location - capture coordinate
                if (mGoogleApiClient.isConnected() &&
                        gpsModeState.getCurrentParentGpsMode() != GpsMode.FIXING) {
                    // create a sighting here and now
                    // This is needed because we want to save time and gps to it from when ADD was pressed
                    trip.getSightings().add(new Sighting());

                    // a sighting was created above, so the trip will have at least one
                    //set time
                    trip.getLastCreatedSighting().
                            getStartTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                    //set coordinates (place)
                    captureCoordinates(trip.getLastCreatedSighting().getStartTimeAndPlace());

                    // and link the openedSighting to it (most recently added sighting),
                    // so that the save button knows where to save
                    openSighting(
                            getText(R.string.app_name) + " - " + getText(R.string.add_sighting),
                            trip.getLastCreatedSighting()
                    );
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            R.string.gps_fix_message,
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    public void sendButtonLogic() {

        findViewById(R.id.fab_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (trip.getNumberOfSightings() == 0) {
                    Toast.makeText(
                            MainActivity.this,
                            R.string.no_sighting_to_send_message,
                            Toast.LENGTH_LONG
                    ).show();

                } else {

                    final Thread sendSightingsAndShutdownTask = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    findViewById(R.id.fab_add).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.fab_send).setVisibility(View.INVISIBLE);

                                    Toast.makeText(
                                            MainActivity.this,
                                            R.string.send_wait_message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });

                            shutdownAndAwaitTerminationOfExecutorService();

                            // make sure you stop the location updates - which write
                            // to the hashmap which is being read inside sendSightings()
                            // stopLocationUpdates() is called in onStop(), but that's fine.
                            // You can call it multiple times in a row.
                            stopLocationUpdates();
                            sendSightings();
                            finish();
                        }
                    });

                    AlertDialog.Builder comAlertDialogBuilder =
                            new AlertDialog.Builder(MainActivity.this);

                    comAlertDialogBuilder.setTitle(R.string.send_sightings_title_message);

                    String sightingsInString = "";
                    for (Sighting sighting: trip.getSightings()) {
                        // the start quantity, specie, start time of sighting then new line
                        sightingsInString +=
                                String.valueOf(sighting.getAnimal().getStartQuantity()) + " " +
                                        sighting.getAnimal().getSpecie().getName() + " " +
                                        DateFormat.format(
                                                "kk:mm",
                                                sighting.getStartTimeAndPlace().getTimeInMillis()
                                        ).toString() + "\n";
                    }

                    comAlertDialogBuilder.setMessage(sightingsInString);

                    comAlertDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // dialog.dismiss();
                        }
                    });

                    comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // set the time
                            trip.getEndTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                            // set the coordinates
                            captureCoordinates(trip.getEndTimeAndPlace());

                            sendSightingsAndShutdownTask.start();
                        }
                    });

                    comAlertDialogBuilder.create();
                    comAlertDialogBuilder.show();
                }
            }
        });

    }

    public void sendSightings() {
        // Alex: android:installLocation is internalOnly
        // TODO: create that server page (page will display all sightings for the trip, and also create a kml with the csv)

        saveSightingsToFile();
        //restartSendingMechanisms();//Alex - uncomment this

        // TODO: Then turn off the GPS service
        // also, actually turn the gps off - make sure that onPause, when it tries to turn it off, too, works without error

        // and finish
        // Final point - Then stop the application. *make sure you finish it off. Test the order.
        // http://stackoverflow.com/questions/10847526/what-exactly-activity-finish-method-is-doing
        // returning to this app from Gmail ?
        // http://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
        // TODO: finish(); send: stop application from being in the foreground (exit)...kills the activity and? eventually the app
        // but then I want a fresh trip object and initial view (just show them quickly, create object) and exit...
        // There should be a button for starting a trip and a button for adding a sighting
    }

    public void saveSightingsToFile() {
        // TODO:  Give the files good names (XXXX is the time, or user or trip number etc.)
        // TODO: write the json and csv files internally

        try {

            // test external storage version
            File directory = new File(Utils.getDirectory());
            //File directory = new File(Environment.getExternalStorageDirectory(), "Monicet");
            //File directory = new File(Utils.EXTERNAL_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            } // only for external, TODO: remove this?
            //deployment - use internal storage
            //File directory = new File(getFilesDir().toString());

            //test - show files in directory
//                final File d = new File(Utils.INTERNAL_DIRECTORY);
//                File[] files = d.listFiles();
//                for (File file: files) {
//                    Toast.makeText(getApplicationContext(), "After:" + file.getName(), Toast.LENGTH_SHORT).show();
//                }
            // end test

            String routePrefix = "route";
            String tripPrefix = "trip";
            long timeNowInMillis = System.currentTimeMillis();

            String tripFileTitle = tripPrefix + timeNowInMillis;
            String tripFileName = tripFileTitle + AllowedFileExtension.JSON;
            trip.setTripFileName(tripFileName);

            String routeFileTitle = routePrefix + timeNowInMillis;
            String routeFileName = routeFileTitle + AllowedFileExtension.CSV;
            trip.setRouteFileName(routeFileName); // this will be written to the JSON file

            File routeFile = new File(directory, routeFileTitle);
            FileWriter routeWriter = new FileWriter(routeFile);
            routeWriter.append(trip.getUserName());
            routeWriter.append(",");
            routeWriter.append(tripFileName);
            routeWriter.append(",");
            routeWriter.append(routeFileName);
            if (gpsModeState.getUserParentGpsMode() == null) {
                routeWriter.append(",");
                routeWriter.append("GPS OFF");
            } else {
                routeWriter.append(",");
                routeWriter.append(String.valueOf(gpsModeState.getUserParentGpsMode().
                        getIntervalInMillis() / Utils.ONE_MINUTE_IN_MILLIS));
                routeWriter.append(" MIN");
            }
            routeWriter.append("\r\n"); //routeWriter.append(System.getProperty("line.separator"));

            for (Map.Entry<Long, double[]> entry : trip.getRouteData().entrySet()) {
                double[] coords = entry.getValue();
                routeWriter.append(entry.getKey().toString());
                routeWriter.append(",");
                routeWriter.append("" + coords[0]);
                routeWriter.append(",");
                routeWriter.append("" + coords[1]);
                routeWriter.append("\r\n"); //routeWriter.append(System.getProperty("line.separator"));
            }
            routeWriter.flush(); // Alex: redundant?
            routeWriter.close();
            // add the extension at the end, so that the broadcast receiver doesn't
            // try to sent it before we're finished with the file
            routeFile.renameTo(new File(directory, routeFileName));

            Gson gson = new GsonBuilder().create();
            File tripFile = new File(directory, tripFileTitle);
            FileWriter tripWriter = new FileWriter(tripFile);
            // jasonize the trip
            tripWriter.append(gson.toJson(trip));
            tripWriter.flush(); // Alex: redundant?
            tripWriter.close();
            // add the extension at the end, so that the broadcast receiver doesn't try to
            // sent it before we've finished with the file
            tripFile.renameTo(new File(directory, tripFileName));

            // test
//                    File[] files2 = directory.listFiles();
//                    for (File file: files2) {
//                        Toast.makeText(getApplicationContext(), "Before:" + file.getName(), Toast.LENGTH_SHORT).show();
//                    }
            //test
            //test - delete internal dir
//                    FileFilter fileFilter = new FileFilter() {
//                        @Override
//                        public boolean accept(File pathname) {
//                            return false;
//                        }
//                    };
//                    File[] fs = d.listFiles(fileFilter);
//                    for (File f: fs) {
//                        f.delete();
//                    }
            //test

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "file exception", Toast.LENGTH_SHORT).show(); // Alex: remove this
        }
    }

    public void restartSendingMechanisms() {
        // enable and (re)start mechanisms designed to send (and delete if sent)
        // those files via the Internet (they all use the Utils method SendAndDeleteFiles,
        // which first checks for a live Internet connection)
        // This is the right moment (after SEND) for enabling them, because they've disabled
        // themselves by now (if folder doesn't contain json or csv files aka empty...
        // although it always has one instant-run file)

        // first, use GCM
        useGcmNetworkManager();

        // send message to receivers to try to send the files now
        // Alarm receiver and the dynamic receiver will get this message
        Intent startIntent = new Intent(Utils.START_ACTION);
        this.sendBroadcast(startIntent);//Alex MainActivity.this

        // secondly, use AlarmManager, hooked up to a receiver
        useAlarmManager();

        //thirdly, use static receiver
        useStaticReceiver();

        // Fourthly, a dynamically created broadcast receiver to listen to the network change.
        // It also listens to START_ACTION
        // It was already registered at the beginning of onCreate.
        // Fire it, so that it tries to send the files now (before a connection
        // change occurs, in case the phone is already connected):
        // it's fired above (the alarm and this dynamic receiver are both fired by START_ACTION)
    }

    public void useGcmNetworkManager() {
        // first - use GCM Network Manager. I updates itself and stops when folder is empty
        // (setPersisted and updateCurrent are true). Sends the files within a minute when connected.
        // maybe pass it the application context - it only uses it for the path anyways
        // MainActivity.this.getApplicationContext(); //getApplication().getBaseContext();
        // TODO: new Thread here or try retrofit inside the sendAndDeleteFiles method ? uses a different thread?
        new Thread(new Runnable() {
            @Override
            public void run() {
                //maybe the thread needs to be inside scheduleOneOff
                SendFilesTaskService.scheduleOneOff(MainActivity.this);
            }
        }).start();
    }

    public void useAlarmManager() {
        // Alarm Manager starts an hourly alarm after BOOT COMPLETED or START_ACTION
        // which starts the AlarmReceiver, which sends and deletes files.
        // It disables itself if folder is empty. It doesn't listen to the network
        // enable it
        Utils.setComponentState(
                this,//Alex MainActivity.this
                FilesAndBootReceiver.class,
                true
        );
    }

    public void useStaticReceiver() {
        // A static receiver (defined in xml, only working pre API 24 - Nougat)
        // is deployed to listen to the network. It disables itself if the folder is empty
        // enable it
        Utils.setComponentState(
                this,//Alex MainActivity.this
                StaticNetworkStateReceiver.class,
                true
        );
    }

    public void backButtonLogic() {

        findViewById(R.id.fab_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // maybe move ADD logic to SAVE logic, so here, we do nothing,
                // like we should, see SAVE button logic for more info

                // TODO:
                // if we came here from ADD, it switched into fast gps mode,
                // and tried to write data to the linked sighting

                // openedSighting should no longer point to our unsaved (if coming from ADD)
                // or opened sighting (if coming from CLICK on sight)
                // shared by the SAVE and BACK button
                // call this before removing the unsaved sighting, so, openedSighting doesn't point to it
                // hopefully, the unsaved sighting will be GC-ed soon
                disconnectOpenedSighting();

                // check that the most recent sighting has an Animal
                // we are here after ADD or CLICK on a sighting logic, therefore at least a sighting exists
                if (trip.getLastCreatedSighting().getAnimal() == null) {
                    // in this case, we arrived here after ADD (newly created, animal-less sighting
                    // so, remove this sighting from this trip
                    trip.getSightings().remove(trip.getLastCreatedSighting());
                }
                // else - we're coming here from CLICK on Sight, we are not touching that already existing sighting

                // prepare views - hide and show what's needed
                showSightings(); // shared between the START, SAVE, BACK and DELETE buttons
            }
        });
    }

    public void saveButtonLogic() {
        // saving is done inside the animal adapter and inside the sighting adapter
        findViewById(R.id.fab_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // read the seedAnimals that have just been changed by the AnimalAdapter
                int numberOfSeenSpecies = 0;
                // stop when you've found quantities different from zero at 2 different species
                // or stop at the end, when you've found zero quantities different from zero from all the species
                Animal animalToInsertInSighting = null;

                for (Animal seedAnimal : seedAnimals) {
                    if (seedAnimal.getStartQuantity() != 0) {
                        // if it found an animal with the quantity different from 0, assign it to our animal
                        animalToInsertInSighting = seedAnimal;
                        numberOfSeenSpecies++;
                        if (numberOfSeenSpecies > 1) { break; }
                    }
                }

                if (numberOfSeenSpecies == 0) {
                    // no animal had the quantity different from 0 (in this case our animal to be inserted is still null)
                    Toast.makeText(
                            MainActivity.this,
                            R.string.no_animals_to_save_message,
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    // more than one animal had the quantity different from 0 (our animal to be inserted is the second animal found)
                    if (numberOfSeenSpecies == 2) {
                        Toast.makeText(
                                MainActivity.this,
                                R.string.only_one_animal_message,
                                Toast.LENGTH_LONG
                        ).show();
                    } else { // everything OK, only one animal was selected

                        // TODO: maybe move SAVE logic here? See below why not:
                        // ADD adds time and gps data after having been pressed.
                        // I would have to use a temp sighting, that would complicate things even further:
                        //I just added time and gps (in ADD) to the temp sighting. so it has them.
                        //If temp has time (gps might take a while), that means I just came from ADD.
                        //Now, when saving, I want to add a new sighting to the trip - a clone of
                        //seed, if coming from ADD, of course...temp sighting needs to be global
                        // and final (array hack again).
                        //add sighting (new sighting(seedSighting))
                        //Clear all data from temp sighting on SAVE and BACK. Null animal and all zero.
                        //I was in ADD, I added time and gps to the temp sighting
                        //otherwise (opened sighting is not the temp sighting), here (in save) just
                        //save the opened sighting - coming from click on a sighting

                        // insert animal into opened Sighting, setAnimal() calls new Animal
                        openedSightings[0].setAnimal(animalToInsertInSighting);

                        // SAVE no longer works on the sighting, so openedSighting does not need to connect to it anymore
                        // SAVE and BACK share this
                        disconnectOpenedSighting();

                        // saved successfully message
                        Toast.makeText(MainActivity.this,
                                R.string.sighting_saved_confirmation_message, Toast.LENGTH_SHORT).show();
//                        Toast.makeText(activity,
//                                R.string.sighting_saved_instructions_message, Toast.LENGTH_LONG).show();

                        // show and hide the appropriate views
                        showSightings(); // shared among the START, SAVE, BACK and DELETE buttons
                    }
                }
            }
        });
    }

    // no argument for this one, trip is a class variable
    //the activity has finish() at the end, therefore the big trip gets gc-ed, so decide what it is that you want to save
    // to the json file... for reopening...do you really want to reopen the big trip on the phone?
//    public void trimTrip() {
//
    //        if (files.length < 1 || files[0] == null) {//AsyncTask
    //            return null;
    //        }

//        for (int i = 0; i < trip.getNumberOfSightings(); i++) {
//            Iterator<Animal> iter = trip.getSightingAtIndex(i).getAnimal().iterator();
//
//            while (iter.hasNext()) {
//                Animal animal = iter.next();
//
//                if (animal.getStartQuantity() == 0) {
//                    iter.remove();
//                }
//            }
//        }
//    }

}
