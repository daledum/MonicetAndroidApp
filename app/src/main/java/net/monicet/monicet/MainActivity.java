package net.monicet.monicet;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.gson.stream.JsonReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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
import static net.monicet.monicet.Utils.EXTERNAL_DIRECTORY;

public class MainActivity extends AppCompatActivity implements
        MainActivityInterface,
        LocationListener,//TODO: now try LocationCallback and use setmaxwaittime for batching
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    final Trip[] trips = new Trip[1]; // hack so that I can use inside anonymous classes and deserialize from json file if necessary
    final HashMap<Long,double[]> routeData = new HashMap<Long,double[]>();// To create TRK, GPX, KML, KMZ, PLT files on the server
    final Sighting[] openedSightings = new Sighting[2]; // 'temporary' sightings (one used when opening a sighting and the other when opening its comments dialog)
    final ArrayList<Animal> seedAnimals = new ArrayList<Animal>();
    final ArrayAdapter[] arrayAdapters = new ArrayAdapter[2];
    // Declare and initialize the receiver dynamically // TODO: maybe this should be done in a singleton, application level
    final BroadcastReceiver dynamicReceiver = new DynamicNetworkStateReceiver(); // or declare the class here, occupying more space

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
//    Volatile only has relevance to modifications of the variable itself, not the object it refers to.
//    A volatile field gives you guarantees as what happens when you change it. (Not an object which it might be a reference to)

    private volatile double mostRecentLocationLatitude = Utils.INITIAL_VALUE;
    private volatile double mostRecentLocationLongitude = Utils.INITIAL_VALUE;

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    // single thread executor for capturing gps coordinates
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private volatile int onLocationChangedNumberOfCalls = 0;
    private volatile long timeWhenApplicationStartedInMillis = System.currentTimeMillis();
    private volatile long timeWhenLastRunningThreadEndedInMillis = 0;
    private volatile boolean wasMinimumAmountOfGpsFixingDone = false;// this depends on the trip's constructor
    private volatile boolean wasSendButtonPressed = false; // Get rid of volatile, if not using a separate thread
    private volatile boolean wereSendingMechanismsStarted = false; // Get rid of volatile, if not using a separate thread
    private CopyOnWriteArrayList<TimeAndPlace> timeAndPlacesWhichNeedCoordinates =
            new CopyOnWriteArrayList<TimeAndPlace>();

    @Override
    public void onBackPressed() {
        if (wasMinimumAmountOfGpsFixingDone) {
            backButtonPressedDialog();
        } else {
            finishAndSave(false);
        }
    }

    protected void backButtonPressedDialog() {
        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(this);

        comAlertDialogBuilder.setTitle(R.string.save_trip_before_exiting_dialog_title);
        comAlertDialogBuilder.setMessage(R.string.save_trip_before_exiting_dialog_message);

        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAndSave(true);
                //MainActivity.super.onBackPressed();//this probably calls finish
            }
        });
        comAlertDialogBuilder.setNegativeButton(R.string.my_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAndSave(false);
                //MainActivity.super.onBackPressed();//this probably calls finish

            }
        });

        comAlertDialogBuilder.create();
        comAlertDialogBuilder.show();
    }


    protected void cloneTimeAndPlace(TimeAndPlace destination, TimeAndPlace source) {
        destination.setTimeInMillis(source.getTimeInMillis());
        destination.setLatitude(source.getLatitude());
        destination.setLongitude(source.getLongitude());

    }
    protected void cloneSighting(Sighting destination, Sighting source) {

        // here only the fields we modify in the sighting comments dialog

        cloneTimeAndPlace(destination.getStartTimeAndPlace(), source.getStartTimeAndPlace());
        cloneTimeAndPlace(destination.getEndTimeAndPlace(), source.getEndTimeAndPlace());
        cloneTimeAndPlace(destination.getUserStartTimeAndPlace(), source.getUserStartTimeAndPlace());
        cloneTimeAndPlace(destination.getUserEndTimeAndPlace(), source.getUserEndTimeAndPlace());

        // This copies the reference of the Specie (they share the specie), but the start and end
        // quantities are not shared (normal assignment between primitives)
        destination.setAnimal(source.getAnimal());

        destination.setUserComments(source.getUserComments());
    }

    protected File getTempTripFile() {

        File dir = new File(Utils.getDirectory());// set directory method uses the external directory
        if (!dir.exists()) {
            dir.mkdirs();
        } // only for external?

        // Array of pathnames (max 1 element) for files and directories in this directory
        // which contain the words TEMP and TRIP
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String filename = pathname.getName().toLowerCase();

                if (filename.contains(Utils.TEMP.toLowerCase()) &&
                        filename.contains(Utils.TRIP.toLowerCase())) {
                    return true;
                }

                return false;
            }
        });

        if (files.length > 0) {
            if (files.length > 1) {
                // test purposes only
                Log.d("MyActivity", "You should not have more than one tempTripId files in your directory");
            }
            return files[0];
        }
        return null;
    }

    protected boolean parseTripFromFile(File tempTripFile) {

        try {

            JsonReader reader = new JsonReader(new FileReader(tempTripFile));
            trips[0] = new Gson().fromJson(reader, Trip.class);
            return true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected void initTripAndViews() {

        // This method must be called before mGoogleApiClient.connect();
        // Otherwise it tries to do minimum amount of GPS fixing again

        File tempTripFile = getTempTripFile();
        boolean wasTripDeserializedSuccessfully = false;

        if (tempTripFile != null) {
            // meaning there is at least one temp trip file inside the folder

            // This properly initializes trips[0]
            // True means trips[0] was successfully assigned to the deserialized trip (from the saved json file)
            wasTripDeserializedSuccessfully = parseTripFromFile(tempTripFile);

            // in both cases (if parsing was successful or not):
            // delete the temp trip file
            if (tempTripFile.exists()) {
                tempTripFile.delete();
            }
        }

        if (wasTripDeserializedSuccessfully) {

            // TODO: should I use this or MainActivity.this
            // initialize and show the views (with their logic)... list views, buttons, labels
            initViews();

            if (trips[0].getStartTimeAndPlace().getTimeInMillis() != Utils.INITIAL_VALUE) {
                //if START button was pressed, initViews should be followed by showSightings()
                showSightings();
                // TODO: what to do if comments were interrupted?
                // TODO: also deal with back button pressed (saveAndFinish like onPause)
            }

            // If a trip was reinstated from a temp file, that means the minimum GPS fixing had already been done
            // Temp trip files are saved only after the minimum GPS fixing
            wasMinimumAmountOfGpsFixingDone = true;

            // also deal with wait for gps to fix textview (normally visible) is made
            // invisible in onConnected..therefore:
            // TODO: .connect after this logic (because it uses wasMinGpsFix done)...leave it in onResume?
            findViewById(R.id.wait_for_gps_fix_textview).setVisibility(View.INVISIBLE);

        } else {// wasTripDeserializedSuccessfully is false
            // meaning trip either wasn't deserialized succesfully, or it was never deserialized, because tempTripFile was null
            trips[0] = new Trip();

            // these two only if it wasn't deserialized (otherwise, the deserialized trip already contains this data)
            setTripIdAndFileNamesAndExtensions();
            getUserCredentials();

            initViews();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: if coming back from a config change - I should first check if the views are visible?
        if (areGooglePlayServicesInstalled()) {
            // if google play services are OK

            // set data directory, where files exist - used by:
            // initTripAndViews(); dynamic receiver, the SEND button logic, by receivers, alarm, GCM etc
            setDataDirectory();

            registerDynamicReceiver();

            // create seed animals from resources,containing specie names, photos and description
            // (to feed the custom ListView ArrayAdapter)
            buildSeedAnimalsFromResources();

            // create animal adapter (which uses seed animals) +
            // create the custom Sightings ArrayAdapter and populate it will null
            makeAndSetArrayAdapters();

            // Stuff up to here should be called before initTripAndViews();
            initTripAndViews();

            // Stuff from here and onwards writes or reads from the trips[0] object, so call after initTripAndViews();
            // create loc request so that when google api client connects - shouldn't matter, it's ready (does it onStart)
            createLocationRequest();
            buildGoogleApiClient();

            // TODO: now gps, maybe use SettingsAPI
            if (!isGpsProviderEnabled()) { showTurnOnGpsProviderDialog(); }//testing

            //TODO: now remove after testing, gps, add start moment of trip
            //trip.addRouteData(timeWhenApplicationStartedInMillis, 9.9, 9.9);// get rid
            routeData.put(timeWhenApplicationStartedInMillis, new double[]{9.9, 9.9});
            //up to here

        } else {
            //dialog
            // trip is by default on Fixing, no Google Play Services, so switch to OFF
            trips[0].setGpsMode(null);
            finish();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        // no real need for anything here...it starts fresh every time...just use onCreate
        //if temp file exists, open trip..then delete temp file...this should take place in onCreate
        mGoogleApiClient.connect();//TODO: move this to onCreate?
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 1 - You arrived here because you're app was interrupted (no one called finish), then run
        // the saveAndFinish method which calls finish at the end
        // or 2 - You are here (it is likely) because you called finish(), therefore isFinishing()
        // returns true. SaveAndFinish() will not run (thus avoiding ANR... it calls finish() at the end)

        // Check to see whether this activity is in the process of finishing, either because you
        // called finish() on it or someone else has requested that it finished.

        if (!isFinishing()) {
            finishAndSave(true);
        }
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//
//        // If executor service has been made null (I'm coming here after onStop())
//        // make a new one
//        if (executorService == null) {
//            executorService = Executors.newSingleThreadExecutor(); // this is for future coordinate capturing tasks
//        }
//
//        mGoogleApiClient.connect();
//    }
//
//    @Override
//    protected void onStop() {
//
//        // this might come after .shutdown() or shutdownNow() on the same object
//        if (!executorService.isShutdown()) {
//            executorService.shutdownNow();
//        }
//        executorService = null;
//
//        // if threads were interrupted - they might have left the interval in Sampling mode
//        // interval (and distance) get turned back on to their original state in onStart (when
//        // google api client connects..in onConnected)
//        stopLocationUpdates();
//
//        if (mGoogleApiClient.isConnected()) {
//            mGoogleApiClient.disconnect();
//        }
//
//        // Finish off any timeAndPlaces left without coordinates when threads were interrupted (exec shutdown)
//        // this will finish up objects in timeAndPlacesWhichNeedCoordinates, too
//        finishTimeAndPlaces(getAllTimeAndPlaces());
//
//        super.onStop();
//    }

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

        if (trips[0].getNumberOfSightings() == 0) {
            // show the no sightings message (its text is set in XML), if the trip is empty
            findViewById(R.id.no_sightings_text_view).setVisibility(View.VISIBLE);
            findViewById(R.id.list_view_sightings).setVisibility(View.INVISIBLE);
        } else {
            // show sightings list view, if the trip has any sightings
            findViewById(R.id.list_view_sightings).setVisibility(View.VISIBLE);
            findViewById(R.id.no_sightings_text_view).setVisibility(View.INVISIBLE);
        }

        // update sighting adapter
        arrayAdapters[1].clear();
        arrayAdapters[1].addAll(trips[0].getSightings());//no choice, this is 'global' - I cannot instantiate my bespoke adapters as globals, before oninit
        arrayAdapters[1].notifyDataSetChanged();
    }

    @Override
    public void showSightingCommentsDialog(final Sighting sighting) {

        // Save the state of the sighting into the temporary sighting
        // Cloning the relevant data into the temp sighting. It is a snapshot of the way
        // the sighting was at the beginning of this method (user for reinstating if CANCEL is pressed).
        openedSightings[1] = new Sighting();
        cloneSighting(openedSightings[1], sighting);

        // TODO: take other smartphone gps reading (from trip, sighting, animal)
        // and compare the sign (if near the 0 degree point, don't do this check)
        LayoutInflater layoutInflater = LayoutInflater.from(this);

        View rootView = layoutInflater.inflate(R.layout.comments_dialog, null);

        // take the system's time
        // this is giving me the time when they edited the comments the last time
        // This won't interfere with the finishTimeAndPlaces method. getAllTimeAndPlaces doesn't return user TimeAndPlaces
        sighting.getUserEndTimeAndPlace().setTimeInMillis(System.currentTimeMillis());

        // take and set the user's latitude
        // first element of the array: degrees; second: minutes; third: seconds
        final double[] latitudeDegreesMinutesAndSeconds = new double[]{0, 0, 0};

        final EditText latitudeDegrees = (EditText)rootView.findViewById(R.id.lat_degrees_edit_text);
        latitudeDegrees.setText(String.valueOf(openedSightings[1].getUserEndTimeAndPlace().getLatitude()));
        latitudeDegrees.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                latitudeDegreesMinutesAndSeconds[0] = Utils.parseGpsToDouble(
                        latitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LATITUDE
                );
                sighting.getUserEndTimeAndPlace().setLatitude(Utils.convertDegMinSecToDecimal(
                        latitudeDegreesMinutesAndSeconds[0],
                        latitudeDegreesMinutesAndSeconds[1],
                        latitudeDegreesMinutesAndSeconds[2]
                ));
            }
        });


        final EditText latitudeMinutes = (EditText)rootView.findViewById(R.id.lat_minutes_edit_text);
        latitudeMinutes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                latitudeDegreesMinutesAndSeconds[1] = Utils.parseGpsToDouble(
                        latitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                sighting.getUserEndTimeAndPlace().setLatitude(Utils.convertDegMinSecToDecimal(
                        latitudeDegreesMinutesAndSeconds[0],
                        latitudeDegreesMinutesAndSeconds[1],
                        latitudeDegreesMinutesAndSeconds[2]
                ));
            }
        });

        final EditText latitudeSeconds = (EditText)rootView.findViewById(R.id.lat_seconds_edit_text);
        latitudeSeconds.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                 latitudeDegreesMinutesAndSeconds[2] = Utils.parseGpsToDouble(
                        latitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                sighting.getUserEndTimeAndPlace().setLatitude(Utils.convertDegMinSecToDecimal(
                        latitudeDegreesMinutesAndSeconds[0],
                        latitudeDegreesMinutesAndSeconds[1],
                        latitudeDegreesMinutesAndSeconds[2]
                ));
            }
        });

        // take and set the user's longitude
        final double[] longitudeDegreesMinutesAndSeconds = new double[]{0, 0, 0};

        final EditText longitudeDegrees = (EditText)rootView.findViewById(R.id.long_degrees_edit_text);
        longitudeDegrees.setText(String.valueOf(openedSightings[1].getUserEndTimeAndPlace().getLongitude()));
        longitudeDegrees.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                longitudeDegreesMinutesAndSeconds[0] = Utils.parseGpsToDouble(
                        longitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LONGITUDE
                );
                sighting.getUserEndTimeAndPlace().setLongitude(Utils.convertDegMinSecToDecimal(
                        longitudeDegreesMinutesAndSeconds[0],
                        longitudeDegreesMinutesAndSeconds[1],
                        longitudeDegreesMinutesAndSeconds[2]
                ));
            }
        });

        final EditText longitudeMinutes = (EditText)rootView.findViewById(R.id.long_minutes_edit_text);
        longitudeMinutes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                longitudeDegreesMinutesAndSeconds[1] = Utils.parseGpsToDouble(
                        longitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                sighting.getUserEndTimeAndPlace().setLongitude(Utils.convertDegMinSecToDecimal(
                        longitudeDegreesMinutesAndSeconds[0],
                        longitudeDegreesMinutesAndSeconds[1],
                        longitudeDegreesMinutesAndSeconds[2]
                ));
            }
        });

        final EditText longitudeSeconds = (EditText)rootView.findViewById(R.id.long_seconds_edit_text);
        longitudeSeconds.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                longitudeDegreesMinutesAndSeconds[2] = Utils.parseGpsToDouble(
                        longitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                sighting.getUserEndTimeAndPlace().setLongitude(Utils.convertDegMinSecToDecimal(
                        longitudeDegreesMinutesAndSeconds[0],
                        longitudeDegreesMinutesAndSeconds[1],
                        longitudeDegreesMinutesAndSeconds[2]
                ));
            }
        });

        final NumberPicker startQuantity = (NumberPicker)rootView.
                findViewById(R.id.start_animal_quantity_number_picker);
        startQuantity.setMinValue(0);
        startQuantity.setMaxValue(Utils.MAX_VALUE);
        startQuantity.setValue(openedSightings[1].getAnimal().getStartQuantity());

        startQuantity.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                sighting.getAnimal().setStartQuantity(startQuantity.getValue());
            }
        });

        final NumberPicker endQuantity = (NumberPicker)rootView.
                findViewById(R.id.end_animal_quantity_number_picker);
        endQuantity.setMinValue(0);
        endQuantity.setMaxValue(Utils.MAX_VALUE);
        endQuantity.setValue(openedSightings[1].getAnimal().getEndQuantity());
        if (sighting.getEndTimeAndPlace().getTimeInMillis() != Utils.INITIAL_VALUE) {
            endQuantity.setVisibility(View.VISIBLE);
        }

        endQuantity.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                sighting.getAnimal().setEndQuantity(endQuantity.getValue());
            }
        });

        // take and set the user's comments
        final EditText comments = (EditText)rootView.findViewById(R.id.comments_edit_text);
        comments.setText(openedSightings[1].getUserComments());
        comments.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                sighting.setUserComments(comments.getText().toString());
            }
        });

        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(this);

        // So that we cannot click outside of the dialog. The only way out is OK, CANCEL or an interruption.
        // Writing into this dialog saves on the spot (live) to the sighting belonging to the trip.
        // However, if CANCEL is pressed, all changes are discarded. All other scenarions (OK, interruption):
        // changes are kept
        comAlertDialogBuilder.setCancelable(false);

        comAlertDialogBuilder.setTitle(R.string.comments_message_title);

        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Set the temp sighting to null (If app interrupted, it's not getting nulified.
                // This should not cause an issue, app gets killed, anyway)
                openedSightings[1] = null;
                // refresh the views (maybe the final quantity was changed)
                showSightings();
            }
        });
        comAlertDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Make all values like they were when dialog was opened.
                // Copy back values from temp sighting into our sighting.
                cloneSighting(sighting, openedSightings[1]);

                openedSightings[1] = null;
                showSightings();
            }
        });

        comAlertDialogBuilder.setView(rootView);
        comAlertDialogBuilder.create();
        comAlertDialogBuilder.show();
    }

    @Override
    public void deleteSightingDialog(final Sighting sighting) {
        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(this);

        comAlertDialogBuilder.setTitle(R.string.delete_sighting_title);
        comAlertDialogBuilder.setMessage(R.string.delete_sighting_message);

        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                trips[0].getSightings().remove(sighting);
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

        timeAndPlacesWhichNeedCoordinates.add(timeAndPlace);

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                // When you iterate over a CopyOnWriteArrayList,
                // the iterator uses a snapshot of the underlying list (or set)
                // and does not reflect any changes to the list or set after the snapshot was created.

                // If less than 10 seconds elapsed since the most recent thread finished,
                // therefore mostRecentLocation has fresh coordinates
                if (System.currentTimeMillis() - timeWhenLastRunningThreadEndedInMillis <
                        10 * Utils.ONE_SECOND_IN_MILLIS) {
                    finishTimeAndPlaces(timeAndPlacesWhichNeedCoordinates);
                    // Don't empty the list, you might delete stuff which was just added
                    // Also, in this case, the thread did not change the interval (getting into the fast, SAMPLING mode), so,
                    // don't set the endTime here (it did not freshen up mostRecentLocation)
                } else {

                    // here, changing the interval (and, if appropriate smallest distance), for fast sampling.
                    // It must be called on thread which registered the location request
                    RunnableFuture<Void> task = new FutureTask<Void>(new Runnable() {
                        @Override
                        public void run() {

                            // go into sampling mode and restart location updates with the sampling interval
                            startLocationUpdates(GpsMode.SAMPLING);
                        }
                    }, null);

                    runOnUiThread(task);
                    // wait for the task to finish (wait between 0 and 10 seconds)
                    try {
                        task.get(10, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        task.cancel(false);
                    }

                    // Wait for 2 minutes, so that the GPS coordinates stabilize
                    // TODO: onLocationChanged was firing 2 or 4 seconds after time of sighting, as if waiting for that period to finish
                    try {
                        Thread.sleep(2 * Utils.ONE_MINUTE_IN_MILLIS);
                    } catch(InterruptedException e) {
                        // if this thread is for the END of the trip and is interrupted (onStop, followed by onStart, onCreate makes it 0),
                        // the view will be the same, the end of the trip will be finished
                        Thread.currentThread().interrupt();
                    }

                    // Or, as an alternative, wait for a certain number of onLocationChanged calls
//                    onLocationChangedNumberOfCalls = 0;
//                    while (onLocationChangedNumberOfCalls < 5) {
//                        //do nothing
//                    }

                    // sample GPS coordinates
                    // no need to make it volatile, timeAndPlace is written only by this thread and only one of these threads can run at the same time
                    // TODO: onStart: if unfinished TimeAndPlaces...they are filled in before starting new captureCoordinates threads
                    finishTimeAndPlaces(timeAndPlacesWhichNeedCoordinates);
                    // Set the approximate (instruction below this one will add some time to this, too)
                    // time when this thread finished
                    timeWhenLastRunningThreadEndedInMillis = System.currentTimeMillis();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // go back to the user gps mode and restart location updates with the new interval
                            startLocationUpdates(trips[0].getGpsMode());
                        }
                    });
                }
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

            // permission had already been granted
            // onStop kills threads. If it killed the doInitialGpsFix thread before the 5
            // onLocationChanged calls, then the app was called, so it did not return to onStart (where onConnected is called)
            // Therefore I am here (in onStart) and this means that the fixing was not done, so, do it
            // But if onStop killed other threads after the Gps fixing was done (boolean true), I don't want to refix in onStart
            // If app was killed, I want it to re-fix (boolean will be false by default)
            // If app was not killed and it's just coming back from a break, don't re-fix (boolean is true)//TODO: new change this...save it to file
            startLocationUpdates(trips[0].getGpsMode());
            //TODO: change this logic now. If temp file exists minimum = true before googleapi.connect
            // here start a thread which gets into fast, fixing mode (short interval),
            // waits for X number of onLocationChanged calls and after that, Y number of minutes
            fixGpsSignal(5, 2);//TODO: NB now urgent Reinstate this test only commented
            //wasMinimumAmountOfGpsFixingDone = true; // TODO: get rid - only when not testing gps
            //findViewById(R.id.wait_for_gps_fix_textview).setVisibility(View.INVISIBLE);// get rid

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
                    startLocationUpdates(trips[0].getGpsMode());//was empty
                    // here start a thread which gets into fast, fixing mode (short interval),
                    // waits for X number of onLocationChanged calls and after that, Y number of minutes
                    fixGpsSignal(5, 2);//TODO: NB now urgent Reinstate this test only commented
                    //wasMinimumAmountOfGpsFixingDone = true;//TODO: get rid
                    //findViewById(R.id.wait_for_gps_fix_textview).setVisibility(View.INVISIBLE);//get rid

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    trips[0].setGpsMode(null);
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
        Log.i("MainActivity", "GoogleApiClient connection has been suspend");
        // attempt to re-establish the connection?
        Toast.makeText(
                MainActivity.this,
                R.string.google_api_client_connection_suspended,
                Toast.LENGTH_LONG
        ).show();
        finishAndSave(true);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("MainActivity", "GoogleApiClient connection has failed");
        Toast.makeText(
                MainActivity.this,
                R.string.google_api_client_connection_failed,
                Toast.LENGTH_LONG
        ).show();
        finishAndSave(true);
    }

    @Override
    public void onLocationChanged(Location location) {

        onLocationChangedNumberOfCalls++;

        mostRecentLocationLatitude = location.getLatitude();
        mostRecentLocationLongitude = location.getLongitude();

        //trip.addRouteData(System.currentTimeMillis(), location.getLatitude(), location.getLongitude());// get rid
        routeData.put(System.currentTimeMillis(),
                new double[]{location.getLatitude(), location.getLongitude()});
        //TEST TODO: now remove
        //trip.addRouteData(System.currentTimeMillis() - 1000, location.getLatitude(), (double) mLocationRequest.getInterval());//get rid
        routeData.put(System.currentTimeMillis() - 1000,
                new double[]{location.getLatitude(), (double) mLocationRequest.getInterval()});
        //remove up to here
    }

    protected void startLocationUpdates(GpsMode gpsMode) {// was empty
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED && // redundant check - android studio complains otherwise
                mGoogleApiClient.isConnected()) {// Doc:  It must be connected at the time of this call

            mLocationRequest.setInterval(gpsMode.getIntervalInMillis());

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
        trips[0].setUserName(emailAddresses.substring(0, emailAddresses.length() - 1));
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
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(trips[0].getGpsMode().getIntervalInMillis())
                .setFastestInterval(1000); // 1 second
    }

    // TODO: this requires knowledge about the trip object and about the sighting object
    // should be changed so that I get an array with all timeandplaces from trip and one from each sighting
    protected List<TimeAndPlace> getAllTimeAndPlaces() {

        List<TimeAndPlace> timeAndPlaceList = new ArrayList<TimeAndPlace>();

        timeAndPlaceList.add(trips[0].getStartTimeAndPlace());

        for (Sighting sighting: trips[0].getSightings()) {
            timeAndPlaceList.add(sighting.getStartTimeAndPlace());
            timeAndPlaceList.add(sighting.getEndTimeAndPlace());
        }

        timeAndPlaceList.add(trips[0].getEndTimeAndPlace());

        return timeAndPlaceList;
    }

    protected void finishTimeAndPlaces(List<TimeAndPlace> timeAndPlaceList) {

        // onStop happened, thread was interrupted (did not capture lat or long or neither), then onStart came and
        // this method was called. To finish up the coordinates, use mostRecentLocation (it will be the one from just before the interruption).

        // if the timeAndPlace object still exists (maybe I captured after ADD, but then I pressed BACK)
        // and if it is unfinished (time check is redundant for the capture threads, it definitely has a time)
        for (TimeAndPlace timeAndPlace: timeAndPlaceList) {

            // If it has a time, it means it was set (captured, activated)
            if (timeAndPlace != null && timeAndPlace.getTimeInMillis() != Utils.INITIAL_VALUE) {
                if (timeAndPlace.getLatitude() == Utils.INITIAL_VALUE) {
                    timeAndPlace.setLatitude(mostRecentLocationLatitude);
                }
                if (timeAndPlace.getLongitude() == Utils.INITIAL_VALUE) {
                    timeAndPlace.setLongitude(mostRecentLocationLongitude);
                }
            }
        }
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
            if (displayedValues[i].equals(String.valueOf(trips[0].getGpsMode().
                    getIntervalInMillis() / Utils.ONE_MINUTE_IN_MILLIS))) {
                break;
            }
        }

        NumberPicker interval = (NumberPicker) findViewById(R.id.gps_user_interval_number_picker);
        interval.setMinValue(0);
        interval.setMaxValue(displayedValues.length - 1);
        interval.setDisplayedValues(displayedValues);
        // set the default value
        interval.setValue(i);
    }

    private boolean isGpsProviderEnabled() {
        LocationManager lm = (LocationManager) MainActivity.this.
                getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void showTurnOnGpsProviderDialog() {
        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(this);

        comAlertDialogBuilder.setTitle(R.string.turn_on_gps_dialog_title);
        comAlertDialogBuilder.setMessage(R.string.turn_on_gps_dialog_message);

        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                MainActivity.this.startActivity(myIntent);
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

    protected void fixGpsSignal(final int numberOfCalls, final int numberOfMinutes) {

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                RunnableFuture<Void> task = new FutureTask<Void>(new Runnable() {
                    @Override
                    public void run() {

                        // go into fixing mode and restart location updates with the sampling interval
                        startLocationUpdates(GpsMode.FIXING);
                    }
                }, null);

                runOnUiThread(task);
                // wait for the task to finish (wait between 0 and 10 seconds)
                try {
                    task.get(10, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    task.cancel(false);
                }

                // in case this is a fresh start (and not a temp file reopened, temp files get saved only after initial GPS fix
                if (!wasMinimumAmountOfGpsFixingDone) {
                    while (onLocationChangedNumberOfCalls < numberOfCalls) {
                        // Wait for onLocationChanged to be called 5 times
                        if (Thread.currentThread().isInterrupted()) {
                            //The interruption of threads in Java is a collaborative process (i.e. the
                            // interrupted code must do something when asked to stop, not the interrupting code).
                            //TODO: ditch? this will not do onPause logic (which stops googlapi and locations)
                            // Replace finish with what you do in onPause. onConnectionSuspended/ common method
                            finish();
                        }
                    }

                    // Allow START button to be pressed and, if app interrupted later, allow the saving of temp files
                    wasMinimumAmountOfGpsFixingDone = true;
                }

                // Make 'wait for GPS signal to fix' text invisible
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.wait_for_gps_fix_textview).setVisibility(View.INVISIBLE);
                    }
                });

                long timeFromAppLaunchUntilNowInMillis = System.currentTimeMillis() -
                        timeWhenApplicationStartedInMillis;
                // if there's less than numberOfMinutes minutes since app was launched
                if (timeFromAppLaunchUntilNowInMillis < numberOfMinutes * Utils.ONE_MINUTE_IN_MILLIS) {
                    try {
                        // wait for the numberOfMinutes minutes to elapse
                        Thread.sleep(numberOfMinutes * Utils.ONE_MINUTE_IN_MILLIS
                                - timeFromAppLaunchUntilNowInMillis);
                    } catch (InterruptedException e) {
                        // TODO: stuff to do if interrupted...maybe let onPause deal with this
                        Thread.currentThread().interrupt();
                    }
                }

                // Set the approximate (instruction below this one will add some time to this, too)
                // time when this thread finished
                timeWhenLastRunningThreadEndedInMillis = System.currentTimeMillis();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // go back to the user gps mode and restart location updates with the new interval
                        startLocationUpdates(trips[0].getGpsMode());
                    }
                });
            }
        });
    }

    protected void finishAndSave(boolean save) {

        // stop executor service
        if (!executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        // Make sure you stop the location updates - which write to the hashmap which is being read
        // while writing to file (otherwise, concurrency error)
        stopLocationUpdates();// This can be called multiple times in a row, without error

        // TODO: get rid of this, but find a way to avoid concurrent modif exception NB TEST THIS
        // wait for 1 second for the above method to finish its work - will OS really wait 1 sec?
//        try {
//            Thread.sleep(Utils.ONE_SECOND_IN_MILLIS);
//        } catch(InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        // Finish off any timeAndPlaces left without coordinates when threads were interrupted (exec shutdown)
        // this will finish up objects in timeAndPlacesWhichNeedCoordinates, too
        finishTimeAndPlaces(getAllTimeAndPlaces());

        if(save) {
            // NB tempTrip files are not kept (deleted in onCreate)
            // In the case the minimum GPS signal fixing was done (if it wasn't done, don't bother to save temp files)
            if (wasMinimumAmountOfGpsFixingDone) {
                // NB Important to keep this order. Save data first and then start sending mechanisms.
                saveDataToFile();

                // If sending mechanisms are started before the latest files are saved, they look for csv and json files
                // and if they don't find any, they turn themselves off.
                if (wasSendButtonPressed && !wereSendingMechanismsStarted) {
                    startSendingMechanisms();
                }
            }
        } else {
            File dir = new File(Utils.getDirectory());
            // all files containing the trip's ID
            File[] filesToDelete = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String filename = pathname.getName().toLowerCase();

                    if (filename.contains(String.valueOf(trips[0].getId()))) {
                        return true;
                    }

                    return false;
                }
            });

            if (filesToDelete.length > 0) {
                for (File file: filesToDelete) {
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
        }

        // finish() might call onPause. If it does, isFinishing will be true, so, this method won't be called again.
        finish();
        // TODO: finish() stop application from being in the foreground (exit)...kills the activity and? eventually the app
    }

    protected void saveTripToFile(String status, File finishedTripFileWithoutExtension) {
        File directory = new File(Utils.getDirectory());

        // There are no temp trip files at this moment (they get deleted in onCreate, after de-jsoninizing)
        // It makes sense to check if it got to add an extension only in the case of FINISHED files
        if (status.equals(Utils.FINISHED) && finishedTripFileWithoutExtension != null) {
            // Delete this file (no need to keep it, we are creating a new one)
            finishedTripFileWithoutExtension.delete();
        }

        // file title will be either tempTrip1029282822 or finishedTrip10292929292
        String fileTitle = status + trips[0].getTripFile().getFileTitle();
        File tripFile = new File(directory, fileTitle);

        try {
            FileWriter tripWriter = new FileWriter(tripFile);

            // We are creating a JSON file here (adding the JSON ext at the end of the method), so
            // make sure that the file's extension is still JSON (maybe the logic was changed)
            if (trips[0].getTripFile().getFileExtension() == AllowedFileExtension.JSON) {
                // jasonize the trip
                Gson gson = new GsonBuilder().create();
                tripWriter.append(gson.toJson(trips[0]));
            } else {
                Log.d("MainActivity", "the extension of the trip file is not JSON, so, a JSON file was not saved");
            }

            tripWriter.flush(); // Alex: redundant?
            tripWriter.close();

            // Add the extension at the end, so that the broadcast receiver doesn't try to sent it before
            //  we're finished with the file and also for the saveDataToFile method to figure out when interruptions took place
            if (!status.equals(Utils.TEMP)) {
                tripFile.renameTo(new File(directory, fileTitle + trips[0].getTripFile().getFileExtension()));
            }// else, when saving temp data, no need for extensions

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("MainActivity", "File exception when saving trip file");
            tripFile.delete();//TODO: change this logic?
        }

    }

    protected void saveRouteToFile(String status, File finishedRouteFileWithoutExtension) {
        File directory = new File(Utils.getDirectory());
        File routeFile = null;

        // A finishedRoute file and a tempRouteFile cannot be both present in the same directory
        //TODO: test that it's actually using the finished file without extension. Check
        // that there aren't two finishedRouteId files
        if (status.equals(Utils.FINISHED) && finishedRouteFileWithoutExtension != null) {
            // this is the finished route file without an extension (to which we will try to add data
            // and the extension later)
            if (isFileWritable(finishedRouteFileWithoutExtension)) {
                routeFile = finishedRouteFileWithoutExtension;
            } else {
                finishedRouteFileWithoutExtension.delete();
            }

        } else {// if the status is TEMP, finishedRouteFileWithoutExtension can only be null, therefore here:
            // status is either TEMP or FINISHED and finishedRouteFileWithoutExtension is null
            // Let's see if the directory contains a tempRoute file with the trip's ID (there should be 0 or 1, at most)
            File[] tempRouteFiles = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String filename = pathname.getName().toLowerCase();
                    if (filename.contains(Utils.TEMP.toLowerCase()) &&
                            filename.contains(trips[0].getRouteFile().getFileTitle().toLowerCase())) {
                        return true;
                    }
                    return false;
                }
            });

            if (tempRouteFiles.length > 0) {
                if (isFileWritable(tempRouteFiles[0])) {
                    routeFile = tempRouteFiles[0];// this runs well when there's a temp route file
                } else {
                    tempRouteFiles[0].delete();
                }
            }
            //testing only
            if (tempRouteFiles.length > 1) {
                Log.d("MyActivity", "You should not have more than one tempRouteId files in your directory");
            }
            //testing up to here
        }

        String fileTitle = status + trips[0].getRouteFile().getFileTitle();

        if (routeFile == null) {
            // this means that there are no openable/writable tempRouteId or finishedRouteId files
            routeFile = new File(directory, fileTitle);
        }

        try {
            //FileWriter routeWriter = new FileWriter(routeFile, true);//get rid
            //routeWriter.append("\r\n"); //routeWriter.append(System.getProperty("line.separator"));//get rid

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(routeFile, true));

            // We are creating a CSV file here (adding the CSV ext at the end of the method), so
            // make sure that the file's extension is still CSV (maybe the logic was changed)
            if (trips[0].getRouteFile().getFileExtension() == AllowedFileExtension.CSV) {
                bufferedWriter.append(trips[0].getUserName());
                bufferedWriter.append(",");
                bufferedWriter.append(trips[0].getRouteFile().getFileTitle() +
                        trips[0].getRouteFile().getFileExtension());
                if (trips[0].getGpsMode() == null) {
                    bufferedWriter.append(",");
                    bufferedWriter.append("GPS OFF");
                } else {
                    bufferedWriter.append(",");
                    bufferedWriter.append(String.valueOf(trips[0].getGpsMode().
                            getIntervalInMillis() / Utils.ONE_MINUTE_IN_MILLIS));
                    bufferedWriter.append(" MIN");
                }
                //TODO: now get rid testing only
                bufferedWriter.append(", 5 onLocCh + 2 min for fix & 2 min for capturing, ");
                bufferedWriter.append(status);
                bufferedWriter.newLine();

                for (Map.Entry<Long, double[]> entry : routeData.entrySet()) {
                    double[] coords = entry.getValue();
                    bufferedWriter.append(entry.getKey().toString());
                    bufferedWriter.append(",");
                    bufferedWriter.append("" + coords[0]);
                    bufferedWriter.append(",");
                    bufferedWriter.append("" + coords[1]);
                    bufferedWriter.newLine();
                }
            } else {
                Log.d("MainActivity", "the extension of the route file is not CSV, so, a CSV file was not saved");
            }

            bufferedWriter.flush();
            bufferedWriter.close();

            // Add the extension at the end, so that the broadcast receiver doesn't try to sent it before we're
            // finished with the file and also for the saveDataToFile method to figure out when interruptions took place
            if (!status.equals(Utils.TEMP)) {
                routeFile.renameTo(new File(directory, fileTitle + trips[0].getRouteFile().getFileExtension()));
            }// else. When status is temp (we save temporary files in order to reopen them later),
            // we don't add extensions, so that sending mechanisms doesn't send and delete them

            //TODO: rename according to status here even for temp (but no extension...it should be temp already)
            // TODO: if it's called finished, then it has the extension already...so, change logic

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("MainActivity", "File exception when saving route file");
            routeFile.delete(); //TODO: change this logic, when working with temp route file?
        }
    }

    protected boolean isFileWritable(File file) {
        // ?maybe just use if (file.canWrite())
        try {
            //try to open the file
            FileWriter testWriter = new FileWriter(file, true);
            testWriter.close();
            // if successful
            return true;
        } catch(IOException e) {
            // if unsuccessful
            e.printStackTrace();
            //return false;// what if fileWriter will throw other exceptions (in addition or instead
            // of IOException)? Then, if a different exception is thrown (which I'm obviously not catching),
            // my method won't return anything. Is that possible? Catching any exception... would that be OK?
        }
        return false;
    }

    protected void saveDataToFile() {
        // TODO:  Give the files better names (XXXX is the time, or user or trip number etc.)
        // TODO: write the json and csv files internally
        // android:installLocation is internalOnly

        File dir = new File(Utils.getDirectory());// set directory method in onCreate uses the external directory
        if (!dir.exists()) {
            dir.mkdirs();
        } // only for external, TODO: remove this? (if yes, move dir - new file inside the logic below)

        if (wasSendButtonPressed) {
            if (!wereSendingMechanismsStarted) {
                // Meaning it's either the first time this method is started or the SEND button
                // logic was interrupted before starting the sending mechanisms

                // Array of pathnames (max 2 elements) for files and directories in this directory
                // which contain the word FINISHED and one of the words TRIP or ROUTE + trip's ID
                File[] files = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        String filename = pathname.getName().toLowerCase();

                        if (filename.contains(Utils.FINISHED.toLowerCase()) &&
                                (filename.contains(trips[0].getRouteFile().getFileTitle().toLowerCase()) ||
                                        filename.contains(trips[0].getTripFile().getFileTitle().toLowerCase()))) {
                            return true;
                        }

                        return false;
                    }
                });

                if (files.length == 0) { // No finishedTripId or finishedRouteId files in the dir
                    saveTripToFile(Utils.FINISHED, null);
                    saveRouteToFile(Utils.FINISHED, null);//save (add and rename for the temp route file, if it exists)
                } else {
                    if (files.length == 1) { // Either a finishedTripId or a finishedRouteId file, not both
                        String filename = files[0].getName().toLowerCase();

                        if (filename.contains(trips[0].
                                getTripFile().getFileTitle().toLowerCase())) {
                            // It's a trip file (they are named finishedTripFile at the start of the saveTripToFile method)
                            // Extensions are added to files at the end of the saveTrip/RouteToFile methods
                            if (!filename.contains(trips[0].
                                    getTripFile().getFileExtension().toString().toLowerCase())) {
                                // Meaning the finished trip file has no extension (method interrupted before adding the ext)

                                //should delete finished trip file without ext, create new one and add ext to it
                                saveTripToFile(Utils.FINISHED, files[0]);
                            }

                            // (if tempRoute exists), add route data, rename to FINISHED and add .csv to file
                            // OR, if it doesn't exist, create finishedRouteFile
                            saveRouteToFile(Utils.FINISHED, null);
                        } else { // It's a route file. After SEND is pressed, if a temp route file already exists
                            // and is writable, it gets renamed to finished and receives the CSV extension at the end of the method
                            // If temp route file doesn't exist (first time app is opened), after pressing SEND, the file gets the name
                            // finished route file and no extension at the beginning of the method and the CSV extension at the end of the method
                            if (!filename.contains(trips[0].
                                    getRouteFile().getFileExtension().toString().toLowerCase())) {
                                // Add route data (if possible) and add .csv
                                saveRouteToFile(Utils.FINISHED, files[0]);
                            }

                            //create a new finished trip file, save it, add json to it (no temp trip files possible)
                            saveTripToFile(Utils.FINISHED, null);
                        }
                    } else { // Both files (route and trip) present in the directory
                        String filename1 = files[0].getName().toLowerCase();
                        String filename2 = files[1].getName().toLowerCase();
                        if (filename1.contains(trips[0].getTripFile().getFileTitle().toLowerCase())) {
                            // this means the first file is the trip file and the second is the route file

                            if (!filename1.contains(trips[0].
                                    getTripFile().getFileExtension().toString().toLowerCase())) {
                                // trip file wasn't finished (left extension-less)
                                //should delete finished trip file without ext, create new one and add ext to it
                                saveTripToFile(Utils.FINISHED, files[0]);
                            }

                            if (!filename2.contains(trips[0].
                                    getRouteFile().getFileExtension().toString().toLowerCase())) {
                                // route file wasn't finished, so add route data (if possible) and add .csv
                                saveRouteToFile(Utils.FINISHED, files[1]);
                            }
                        } else {
                            // this means the first file is the route file and the second is the trip file

                            if (!filename1.contains(trips[0].
                                    getRouteFile().getFileExtension().toString().toLowerCase())) {
                                //route file wasn't finished, so add route data (if possible) and add .csv
                                saveRouteToFile(Utils.FINISHED, files[0]);
                            }

                            if (!filename2.contains(trips[0].
                                    getTripFile().getFileExtension().toString().toLowerCase())) {
                                // trip file wasn't finished (left extension-less)
                                //should delete finished trip file without ext, create new one and add ext to it
                                saveTripToFile(Utils.FINISHED, files[1]);
                            }
                        }
                    }
                }
            }//else here means sending mechs were started (they are started after this method). Therefore this method finished already.
        } else {// SEND button was not pressed, we are here due to an interruption or from onConnectionSuspended/Failed

            // Save temporary files to be used when app is restarted
            saveTripToFile(Utils.TEMP, null);
            saveRouteToFile(Utils.TEMP, null);// this should add data to tempRouteId if it exists
        }
    }

    public void setDataDirectory() {
        // before registering the dynamic receiver, which will trigger - inform the package where you saved the files
        // set directory here for the SendAndDeleteFiles Utils method
        Utils.setDirectory(EXTERNAL_DIRECTORY); // have this in the initData() method - must be called before registerDynamicReceiver()
        //should be Utils.setDirectory(getFilesDir().toString()); // Alex: was, directory.toString(), toString should be optional

    }

    public void setTripIdAndFileNamesAndExtensions() {

        // set the trip's ID - used when saving files
        trips[0].setId(timeWhenApplicationStartedInMillis);

        trips[0].getTripFile().setFileTitle(Utils.TRIP + trips[0].getId());
        trips[0].getTripFile().setFileExtension(AllowedFileExtension.JSON);

        trips[0].getRouteFile().setFileTitle(Utils.ROUTE + trips[0].getId());
        trips[0].getRouteFile().setFileExtension(AllowedFileExtension.CSV);// this will be written to the JSON file
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

    public void initViews() {
        // set label to MONICET - START TRIP
        setTitle(getText(R.string.app_name) + " - " + getText(R.string.start_trip));

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
                if (isGpsProviderEnabled()) {
                    // if GPS has connected and fixed on a location - capture coordinate
                    if (mGoogleApiClient.isConnected() && wasMinimumAmountOfGpsFixingDone) {
                        //here get the user interval
                        NumberPicker np = (NumberPicker) findViewById(R.id.gps_user_interval_number_picker);
                        int indexOfInterval = np.getValue();
                        long intervalToCompareWith = (indexOfInterval == 0) ?
                                Utils.ONE_MINUTE_IN_MILLIS : indexOfInterval * Utils.FIVE_MINUTES_IN_MILLIS;

                        for (GpsMode gpsMode: GpsMode.values()) {
                            if (gpsMode.getIntervalInMillis() == intervalToCompareWith) {
                                // set the user mode with the newly selected user mode
                                trips[0].setGpsMode(gpsMode);
                                break;
                            }
                        }

                        // if I'm not in fixing or sampling modes, meaning that I am in one of the user
                        // modes (the one selected most recently by the user)
                        // update the location request interval
                        //TODO: if I pressed START while in fixing: give START here mostRecentLoc's coords (5 calls did take place),
                        // if not add to list and start sampling mode
                        // I could not have pressed start while in SAMPLING (START and sightings only use SAMPLING)
                        if (mLocationRequest.getInterval() != GpsMode.FIXING.getIntervalInMillis() &&
                                mLocationRequest.getInterval() != GpsMode.SAMPLING.getIntervalInMillis()) {//TODO: move this in the for above
                            startLocationUpdates(trips[0].getGpsMode());
                        }
                        //else, if I am in fixing or sampling mode (doInitialFix and captureCoordinates
                        // will get the mode into the new user mode at the end

                        // b) - time
                        trips[0].getStartTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                        // and gps coords // TODO: if it hasn't finished fixing the gps signal, this will be 0 and 0
                        captureCoordinates(trips[0].getStartTimeAndPlace());

                        // deal with the views
                        showSightings(); // shared between the START, SAVE, BACK and DELETE buttons
                    } else {
                        Toast.makeText(
                                MainActivity.this,
                                R.string.gps_fix_message,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                } else { showTurnOnGpsProviderDialog(); }
            }
        });
    }

    public void addButtonLogic() {
        // uses the animal adapter
        findViewById(R.id.fab_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create a sighting here and now
                // This is needed because we want to save time and gps to it from when ADD was pressed
                trips[0].getSightings().add(new Sighting());

                // a sighting was created above, so the trip will have at least one
                //set time
                trips[0].getLastCreatedSighting().
                        getStartTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                //set coordinates (place)
                captureCoordinates(trips[0].getLastCreatedSighting().getStartTimeAndPlace());

                // and link the openedSighting to it (most recently added sighting),
                // so that the save button knows where to save
                openSighting(
                        getText(R.string.app_name) + " - " + getText(R.string.add_sighting),
                        trips[0].getLastCreatedSighting()
                );
            }
        });
    }

    public void sendButtonLogic() {

        findViewById(R.id.fab_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (trips[0].getNumberOfSightings() == 0) {
                    Toast.makeText(
                            MainActivity.this,
                            R.string.no_sighting_to_send_message,
                            Toast.LENGTH_LONG
                    ).show();

                } else {

                    // maybe get rid of this from here
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
                            finishAndSave(true);
                        }
                    });//get rid up to here, maybe

                    AlertDialog.Builder comAlertDialogBuilder =
                            new AlertDialog.Builder(MainActivity.this);

                    comAlertDialogBuilder.setTitle(R.string.send_sightings_title_message);

                    String sightingsInString = "";
                    for (Sighting sighting: trips[0].getSightings()) {
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
                            wasSendButtonPressed = true;

                            // set the time
                            trips[0].getEndTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                            // set the coordinates
                            captureCoordinates(trips[0].getEndTimeAndPlace());

                            //sendSightingsAndShutdownTask.start();//test, uncomment if necessary
                            //TODO: maybe replace the above with:
                            finishAndSave(true);
                        }
                    });

                    comAlertDialogBuilder.create();
                    comAlertDialogBuilder.show();
                }
            }
        });

    }
    
    public void startSendingMechanisms() {
        // enable and (re)start mechanisms designed to send (and delete if sent)
        // those files via the Internet (they all use the Utils method SendAndDeleteFiles,
        // which first checks for a live Internet connection)
        // This is the right moment (after SEND) for enabling them, because they've disabled
        // themselves by now (if folder doesn't contain json or csv files aka empty...
        // although it always has one instant-run file)

        // If I move it to the end of this method, maybe some are started and other no.. multiple booleans
        wereSendingMechanismsStarted = true;

        // first, use GCM // TODO: maybe start a new thread inside the method, TEST to see if scheduleOneOff actually works...by stopping all the other mechs
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
        SendFilesTaskService.scheduleOneOff(MainActivity.this);
        // if this method is not called inside a new thread...then maybe create a new runnable here
        // with scheduleoneoff
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
                // Java is Pass-by-value/Call by sharing - therefore the referred object will not be nullified
                openedSightings[0] = null;

                // check that the most recent sighting has an Animal
                // we are here after ADD or CLICK on a sighting logic, therefore at least a sighting exists
                if (trips[0].getLastCreatedSighting().getAnimal() == null) {
                    // in this case, we arrived here after ADD (newly created, animal-less sighting
                    // so, remove this sighting from this trip
                    trips[0].getSightings().remove(trips[0].getLastCreatedSighting());
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
                        // Java is Pass-by-value/Call by sharing - therefore the referred object will not be nullified
                        openedSightings[0] = null;

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
