package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static android.R.string.no;
import static android.os.Build.VERSION_CODES.M;
import static net.monicet.monicet.Utils.START_ACTION;

public class MainActivity extends AppCompatActivity {

    // declaring trip as a class field made the app not start.. why?
    //final Trip trip = new Trip(buildLocationFromResources());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
//            //todo: somehow inform user or fallback to different method
//            //stop our activity initialization code
//            return;
//        }

        // Declare and initialize the receiver dynamically
        BroadcastReceiver dynamicReceiver = new DynamicNetworkStateReceiver();
        // Register the receiver. Once registered it's enabled by default, therefore it could fire (see actions)
        // It won't find anything in the folder before SEND, so, it will disable itself
        // The same applies to the static receiver (pre-nougat version of this one) and to the
        // receivers. They are all enabled after pressing SEND. Disabling them here (start of onCreate),
        // would stop them from working while the app is alive (not forcefully closed by the user/system).
        // So, let them do their thing. They disable themselves if the work is done/or if there is no
        // work to be done.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utils.INTENT_CONNECTION_ACTION);
        filter.addAction(Utils.START_ACTION);
        getApplicationContext().registerReceiver(dynamicReceiver, filter);

        // Step 1 starts here:
        // using the data from resources (containing specie names, photos and description),
        // create the first Location, then instantiate a Trip (which will contain one location)
        // and feed its array of sightings to the custom ListView ArrayAdapter
        // Create the custom ArrayAdapter and populate it

        //this will create a trip with one location, Alex: trip was declared and initialized here
        //can move outside 'global', also, if I'm not using anonymous inners, can drop the final
        final Trip trip = new Trip(buildLocationFromResources());

        final SightingAdapter sightingAdapter = new SightingAdapter(
                this, trip.getCurrentLocation().getSightings(), trip.getGpsModeUserInput());
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(sightingAdapter);
        // Step 1 ends here

        // Initialization steps: *not done in the constructor because they might not be kept in future versions
        // Change label to Monicet - Stop 1
        setTitle(getText(R.string.app_name) + " - " +
                getText(R.string.location) + " " + trip.getNumberOfLocations());
        // TODO: get username and set it
        // trip.setUserName();
        // TODO: start with a reasonably fast gps mode, so that you can sample immediately, then turn it to 'really slow'
        trip.setGpsMode(GpsMode.FAST);
        //trip.setStartLatitude();
        //trip.setStartLongitude();
        trip.setStartTimeInMilliseconds(System.currentTimeMillis());
        trip.setGpsMode(GpsMode.SLOW);
        // Initialization steps end here


        // Step 2 starts here:
        // open a dialog (if the dialog wasn't shown before)
        // and ask the user if they want the Continuous GPS Tracking Mode
        // if 'yes', set the trip variable accordingly
        if (trip.getGpsModeUserInput().isVisible() == true) {
            showGpsModeDialog(trip.getGpsModeUserInput());
        } else {
            //here we arrive in the case the user was already asked about the gps mode,
            // therefore the trip already has the gps mode set
            // TODO: this should trigger the trip.getGpsMode mode
        }
        //
        // b) after the dialog was exited, then we do nothing
        // Step 2 ends here

        // if coming back from a config change - I should first check if they are visible
        final FloatingActionButton fabSave = (FloatingActionButton) findViewById(R.id.fab_save);
        final FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        final FloatingActionButton fabSend = (FloatingActionButton) findViewById(R.id.fab_send);

        // should these be set in the xml initially
        fabAdd.setVisibility(View.INVISIBLE);
        fabSend.setVisibility(View.INVISIBLE);

        // Step 3 starts here:
        // SAVE floating action button
        // Check first if this Location has at least one non-empty Sighting (quantity different to 0)
        // If it is non-empty, open Comments dialogFragment
        // Else (all quantities are 0): put a LONG Toast on the screen (and do nothing), with the message:
        //"your trip has no sightings. There is nothing to send. Please add the quantity of individuals that you've seen.
        // Or no species were seen/No animals were seen at this location. There is nothing to save."
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean noAnimalsWereSeen = true;
                ArrayList<Sighting> sightings = trip.getCurrentLocation().getSightings();

                for (Sighting sighting: sightings) {
                    if (!sighting.isEmpty()) {
                        noAnimalsWereSeen = false;
                        break;
                    }
                }

                if (noAnimalsWereSeen) {
                    Toast.makeText(getApplicationContext(),
                            R.string.no_animals_toast_message, Toast.LENGTH_LONG).show();
                } else {
                    // in the case the user hasn't turned off the comments (or it's the first show)
                    if (trip.getCurrentLocation().getCommentsUserInput().isVisible() == true) {

                        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
                        final View rootView = layoutInflater.inflate(R.layout.comments_dialog, null);
                        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

                        comAlertDialogBuilder.setTitle(R.string.comments_message_title);

                        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO: take other smartphone gps reading (from trip, sighting, location)
                                // and compare the sign (if near the 0 degree point, don't do this check)

                                EditText latitudeDegrees = (EditText) rootView.findViewById(R.id.lat_degrees_edit_text);
                                double gpsDegrees = Utils.parseGpsToDouble(
                                        latitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LATITUDE
                                );
                                EditText latitudeMinutes = (EditText) rootView.findViewById(R.id.lat_minutes_edit_text);
                                double gpsMinutes = Utils.parseGpsToDouble(
                                        latitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                                );
                                EditText latitudeSeconds = (EditText) rootView.findViewById(R.id.lat_seconds_edit_text);
                                double gpsSeconds = Utils.parseGpsToDouble(
                                        latitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                                );

                                trip.getCurrentLocation().setAdditionalLatitude(
                                        Utils.convertDegMinSecToDecimal(gpsDegrees, gpsMinutes, gpsSeconds)
                                );

                                EditText longitudeDegrees = (EditText) rootView.findViewById(R.id.long_degrees_edit_text);
                                gpsDegrees = Utils.parseGpsToDouble(
                                        longitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LONGITUDE
                                );
                                EditText longitudeMinutes = (EditText) rootView.findViewById(R.id.long_minutes_edit_text);
                                gpsMinutes = Utils.parseGpsToDouble(
                                        longitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                                );
                                EditText longitudeSeconds = (EditText) rootView.findViewById(R.id.long_seconds_edit_text);
                                gpsSeconds = Utils.parseGpsToDouble(
                                        longitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                                );

                                trip.getCurrentLocation().setAdditionalLongitude(
                                        Utils.convertDegMinSecToDecimal(gpsDegrees, gpsMinutes, gpsSeconds)
                                );

                                EditText comments = (EditText) rootView.findViewById(R.id.comments_edit_text);
                                trip.getCurrentLocation().setComments(comments.getText().toString());

                                CheckBox turnOffCommentsCheckBox = (CheckBox) rootView.findViewById(R.id.turn_off_comment_checkbox);
                                if (turnOffCommentsCheckBox.isChecked()) {
                                    trip.getCurrentLocation().getCommentsUserInput().setVisible(false);
                                    trip.getCurrentLocation().getLatitudeUserInput().setVisible(false);
                                    trip.getCurrentLocation().getLongitudeUserInput().setVisible(false);
                                }

                                saveLocation(trip.getCurrentLocation(), sightingAdapter, trip.getGpsModeUserInput());
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
                    } else {
                        saveLocation(trip.getCurrentLocation(), sightingAdapter, trip.getGpsModeUserInput());
                    }
                }
            }
        });
        // Step 3 ends here

        //Step 4 starts here:
        // ADD + button logic
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trip.addLocation();

                sightingAdapter.clear();
                sightingAdapter.addAll(trip.getCurrentLocation().getSightings());
                sightingAdapter.notifyDataSetChanged();

                fabAdd.setVisibility(View.INVISIBLE);
                fabSend.setVisibility(View.INVISIBLE);
                fabSave.setVisibility(View.VISIBLE);

                setTitle(getText(R.string.app_name) + " - " +
                        getText(R.string.location) + " " + trip.getNumberOfLocations());
            }
        });
        //Step 4 ends here

        //Step 5 starts here:
        // SEND button logic
        fabSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: step 1 - Sample GPS, Date, Time and save Trip instance end_gps, end_date/time
                //trip.setEndLatitude();
                //trip.setEndLongitude()
                trip.setEndTimeInMilliseconds(System.currentTimeMillis());

                // Alex: android:installLocation is internalOnly
                // TODO:  Give the files good names (XXXX is the time, or user or trip number etc.)
                // TODO: write the json and csv files internally
                // TODO: try to send them via a http post request to a server... if successful, delete the files, if not don't delete the files
                // TODO: maybe just create the files and leave all the rest to the service (so that the 2 don't step on each other's toes)
                // TODO: create that server page (page will display all non-empty sightings for the trip, and also create a kml with the csv)
                // TODO: create that service that runs continuously, goes to the Monicet folder and sends all the json and csv files to the server (if successful, deletes them)
                // TODO: be careful so that both the service and the send button try to send the data to the same place (DRY)

                // step 2 - create the files to be sent
                // test external storage version
                // TODO: test here the / issue
                File directory = new File(Environment.getExternalStorageDirectory(), "Monicet");
                //File directory = new File(Utils.EXTERNAL_DIRECTORY);
                if (!directory.exists()) { directory.mkdirs(); } // only for external
                //deployment - use internal storage
                //File directory = new File(getFilesDir().toString());

                //test
                //File directory = new File(Utils.INTERNAL_DIRECTORY);
//                File[] files = directory.listFiles();
//                for (File file: files) {
//                    Toast.makeText(getApplicationContext(), "After:" + file.getName(), Toast.LENGTH_SHORT).show();
//                }
                // end test

                try {

                    String routePrefix = "route";
                    String tripPrefix = "trip";

                    String tripFileTitle = tripPrefix + System.currentTimeMillis();
                    String tripFileName = tripFileTitle + Utils.JSON_FILE_EXTENSION;
                    trip.setTripFileName(tripFileName);

                    if (trip.getGpsMode() == GpsMode.CONTINUOUS) {

                        String routeFileTitle = routePrefix + System.currentTimeMillis();
                        String routeFileName = routeFileTitle + Utils.CSV_FILE_EXTENSION;
                        trip.setRouteFileName(routeFileName); // this will be written to the JSON file
                        File routeFile = new File(directory, routeFileTitle);
                        FileWriter routeWriter = new FileWriter(routeFile);
                        routeWriter.append(trip.getUserName());
                        routeWriter.append(",");
                        routeWriter.append(tripFileName);
                        routeWriter.append(",");
                        routeWriter.append(routeFileName);
                        routeWriter.append("\r\n"); //routeWriter.append(System.getProperty("line.separator"));

                        for (Map.Entry<Long, double[]> entry: trip.getContinuousData().entrySet()) {
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
                    }

                    Gson gson = new GsonBuilder().create();
                    File tripFile = new File(directory, tripFileTitle);
                    FileWriter tripWriter = new FileWriter(tripFile);
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

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "file exception", Toast.LENGTH_SHORT).show(); // Alex: remove this
                }

                // step 3 - inform the package where you saved the files and what extensions you gave them
                // set directory here for the SendAndDeleteFiles Utils method
                Utils.setDirectory(directory.toString()); // Alex: toString should be optional
                // do the same for the FileFilter extensions, used by the same Utils method
                // write here what extensions you gave your files when saving them
                Utils.setFileExtensionsArray(
                        new String[]{Utils.JSON_FILE_EXTENSION, Utils.CSV_FILE_EXTENSION}
                );

                // step 4 - enable and start mechanism designed to send (and delete if sent)
                // those files via the Internet (they all use the Utils method SendAndDeleteFiles,
                // which first checks for a live Internet connection
                // This is the right moment (after SEND) for enabling them, because they've disabled
                // themselves by now

                // first - use GCM Network Manager (setPersisted and updateCurrent are true)
                // maybe pass it the application context - it only uses it for the path anyways
                // MainActivity.this.getApplicationContext(); //getApplication().getBaseContext();
                SendFilesTaskService.scheduleOneOff(MainActivity.this);

                // then, use receivers
                // a) a static receiver (defined in xml, only working pre API 24 - Nougat)
                // is deployed to listen to the network
                // i) enable it
                Utils.setComponentState(
                        MainActivity.this,
                        StaticNetworkStateReceiver.class.getSimpleName(),
                        true
                );
                // ii) fire it: no need to fire this one right know, because
                // the dynamic receiver deals with the present moment

                // b) a dynamically created broadcast receiver to listen to the network change
                // i) enable it (it was registered in onCreate already)
                Utils.setComponentState(
                        MainActivity.this,
                        DynamicNetworkStateReceiver.class.getSimpleName(),
                        true
                );
                // ii) fire it, so that it tries to send the files now (before a connection
                // change occurs, in case the phone is already connected)
                Intent startIntent = new Intent(Utils.START_ACTION);
                MainActivity.this.sendBroadcast(startIntent);

                // c) Alarm Manager receiver: starts the hourly alarm which starts the receiver,
                // which sends and deletes files
                // i) enable it
                Utils.setComponentState(
                        MainActivity.this,
                        FilesAndBootReceiver.class.getSimpleName(),
                        true
                );
                // ii) fire it: already done above (see dynamic receiver)

                // TODO: Then turn off the GPS service
                trip.setGpsMode(GpsMode.OFF);
                // also, actually turn the gps off

                // Final point - Then stop the application. *make sure you finish it off. Test the order.
                // http://stackoverflow.com/questions/10847526/what-exactly-activity-finish-method-is-doing
                // returning to this app from Gmail ?
                // http://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
                // TODO: finish(); send: stop application from being in the foreground (exit)...
                // but then I want a fresh trip object and initial view (just show them quickly, create object) and exit...
                // There should be a button for starting a trip and a button for adding a location

                //        if (files.length < 1 || files[0] == null) {
                //            return null;
                //        }
            }
        });
        //Step 5 ends here

    }

    public void showGpsModeDialog(final UserInput<GpsMode> gpsModeUserInput) {
        // it comes here only if the user changeable variable is visible (it was not shown before, in this case)
        AlertDialog.Builder gpsAlertDialogBuilder = new AlertDialog.Builder(this);
        gpsAlertDialogBuilder.setTitle(R.string.tracking_dialog_title);
        gpsAlertDialogBuilder.setMessage(R.string.tracking_dialog_message);
        gpsAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                gpsModeUserInput.setContent(GpsMode.CONTINUOUS);
                // TODO: GPS this should trigger continuous gps
            }
        });
        gpsAlertDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
                // dialog.dismiss();
            }
        });
        gpsAlertDialogBuilder.create();
        gpsAlertDialogBuilder.show();
        // TODO: if the user presses No or presses outside, I should go into slow or fast gps mode
        // TODO: this should trigger slow or fast gps, too
        // to check, verify that it's not in continuous mode, and if not set it to slow or fast
        // Now, set the user input state to false, registering the fact that the user was asked the question
        gpsModeUserInput.setVisible(false);
    }

    public void saveLocation(Location currentLocation, SightingAdapter sightingAdapter,
                             UserInput<GpsMode> gpsModeUserInput) {
        //Location currentLocation = trip.getCurrentLocation();
        // TODO: sample (and save Location instance GPS, date and time)... later, slow down the gps sampling
        //currentLocation.setLatitude();
        //currentLocation.setLongitude();
        currentLocation.setTimeInMilliseconds(System.currentTimeMillis());

        if (gpsModeUserInput.getContent() != GpsMode.CONTINUOUS) {
            gpsModeUserInput.setContent(GpsMode.SLOW); // not too slow, it's still needed by the SEND button, when saving the trip
        }

        for (Sighting sighting: currentLocation.getSightings()) {
            sighting.getQuantityUserInput().setVisible(false);
        }
        sightingAdapter.notifyDataSetChanged();

        Toast.makeText(getApplicationContext(),
                R.string.location_saved_confirmation_message, Toast.LENGTH_SHORT).show();
//        Toast.makeText(getApplicationContext(),
//                R.string.location_saved_instructions_message, Toast.LENGTH_LONG).show();

        findViewById(R.id.fab_save).setVisibility(View.INVISIBLE);
        findViewById(R.id.fab_add).setVisibility(View.VISIBLE);
        findViewById(R.id.fab_send).setVisibility(View.VISIBLE);
    }

    public Location buildLocationFromResources() {

        String[] species = getResources().getStringArray(R.array.speciesArray);
        // TODO: implement getting the photo ids and description data later
        String[] photos = new String[30];
        String[] descriptions = new String[30];
        Arrays.fill(photos, "photo"); // remember to give the photos names like SpermWhale_1, CommonDolphin_X
        Arrays.fill(descriptions, "description");
        // here (if the 3 arrays have the same size, at least check) add each sighting to the list, one by one
        int sizeOfArrays = species.length;
        if ( sizeOfArrays != photos.length || sizeOfArrays != descriptions.length) {
            Log.d("MainActivity", "the sizes of the specie, photo and description arrays are not the same");
        }

        ArrayList<Sighting> sightings = new ArrayList<Sighting>(sizeOfArrays);

        for (int i = 0; i < sizeOfArrays; i++ ) {
            Animal animal = new Animal(species[i], photos[i], descriptions[i]);
            sightings.add(new Sighting(animal));
        }

        return new Location(sightings);//Location location =
    }

}
