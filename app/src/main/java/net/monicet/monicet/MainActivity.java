package net.monicet.monicet;

import android.content.BroadcastReceiver;
import android.content.DialogInterface;
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static android.R.string.no;
import static android.R.transition.move;

public class MainActivity extends AppCompatActivity {

    // declaring trip as a class field made the app not start.. why?
    //final Trip trip = new Trip(buildNewLocationFromResources());
    final Trip trip = new Trip();

    // Declare and initialize the receiver dynamically // TODO: maybe this should be done in a singleton, application level
    final BroadcastReceiver dynamicReceiver = new DynamicNetworkStateReceiver(); // or declare the class here, occupying more space

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
//            //todo: somehow inform user or fallback to different method, this relates to the GCM too
//            //stop our activity initialization code
//            return;
//        }

        // before registering the dynamic receiver, which will trigger - inform the package where you saved the files
        // set directory here for the SendAndDeleteFiles Utils method
        Utils.setDirectory(Utils.EXTERNAL_DIRECTORY);
        //should be Utils.setDirectory(getFilesDir().toString()); // Alex: was, directory.toString(), toString should be optional
        // step III ends here

        // Register the dynamic receiver. Once registered it's enabled by default,
        // therefore it could fire on connectivity change before SEND.
        // android.net.conn.CONNECTIVITY_CHANGE is a sticky broadcast, so, the receiver fires when registered
        // in order to stop that, do work in onReceive only if if (!isInitialStickyBroadcast())
        // All the "file sending" receivers are enabled by default and after pressing SEND.
        // Disabling them here (start of onCreate), would stop them from working while the app
        // is alive (not forcefully closed by the user/system). So, let them do their thing.
        // They disable themselves if the work is done/or if there is no work to be done (except
        // the dynamic receiver, which lives and tries to send the files throughout the lifetime of the app)
//        IntentFilter filter = new IntentFilter();//TODO: maybe register it in onResume or onStart
//        filter.addAction(Utils.INTENT_CONNECTION_ACTION);
//        filter.addAction(Utils.START_ACTION);
//        getApplicationContext().registerReceiver(dynamicReceiver, filter); // application lifetime or just activity

        // TODO: check after installing the app if dynamic receiver runs even after restart
        // (bad, that means it's not unregistering), when the app is not running
        //check for receiver on monicet package
//        adb shell dumpsys activity broadcasts or
//        dumpsys activity -h
//        dumpsys activity b
//        dumpsys package net.monicet.monicet
        // Step 1 starts here:
        // using the data from resources (containing specie names, photos and description),
        // create the first Location, then instantiate a Trip (which will contain one location)
        // and feed its array of sightings to the custom ListView ArrayAdapter
        // Create the custom ArrayAdapter and populate it

        //this will create a trip with one location, Alex: trip was declared and initialized here
        //can move outside 'global', also, if I'm not using anonymous inners, can drop the final
        //final Trip trip = new Trip(buildNewLocationFromResources());//TODO: change constructor of trip() and make global

        // Initialization steps: not done in the constructor because they might not be kept in future versions
        // set label to MONICET - START TRIP
        setTitle(getText(R.string.app_name) + " - " + getText(R.string.start_trip));
        // TODO: start the GPS so that it's calibrated when you first sample (start button pressed)
        // set the mode to SLOW (before this, it was OFF, trip is constructed with OFF)
        trip.setGpsMode(GpsMode.SLOW); // TODO: setGpsMode should calibrate the google location services gps - should be connected
        // Initialization steps end here

        // TODO: should these be set in the xml initially
        // should be invisible in xml, made visible here (before implementing the play button)
        //fabAdd.setVisibility(View.INVISIBLE);
        //fabSend.setVisibility(View.INVISIBLE);
        // Save button should be invisible in xml and made visible

        // TODO: get rid of all these assignment and just use findViewById, so that you can split in methods?
        // if coming back from a config change - I should first check if they are visible
        final FloatingActionButton fabStart = (FloatingActionButton) findViewById(R.id.fab_start);
        final FloatingActionButton fabSave = (FloatingActionButton) findViewById(R.id.fab_save);
        final FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        final FloatingActionButton fabSend = (FloatingActionButton) findViewById(R.id.fab_send);
        final CheckBox checkboxTrackingGps = (CheckBox) findViewById(R.id.checkBox_tracking_gpsmode);
        final ListView listView = (ListView) findViewById(R.id.list_view);

        // TODO: give it the gpsmodeuserinput.. it's a reference, but give it null in place of sightings, also change SightingAdapter to deal with null
        final SightingAdapter sightingAdapter = new SightingAdapter(
                this,
                new ArrayList<Sighting>(Arrays.asList(new Sighting[]{null})),
                trip.getGpsModeUserInput()
        );
//        final SightingAdapter sightingAdapter = new SightingAdapter(
//                this, trip.getCurrentLocation().getSightings(), trip.getGpsModeUserInput());
        // set the adapter
        listView.setAdapter(sightingAdapter); // TODO: maybe I should do this after the first ADD (disable the listview in xml)
        // if not disabling .. then play is pressed..no listview,
        // then press ADD (setAdapter in it, if trip has no locations aka first press).. or a little later
        // Step 1 ends here

        //Step 2 starts here:
        // START floating action button
        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // a) when pressed takes the value from the tracking gpsmode checkbox
                // (if selected set gpsmode to continuous)
                if (checkboxTrackingGps.isChecked()) {
                    if (trip.getGpsModeUserInput().isVisible() == true) { // get rid of this?
                        trip.setGpsMode(GpsMode.CONTINUOUS);
                        // TODO: GPS this should trigger continuous gps

                        trip.getGpsModeUserInput().setVisible(false);
                    } else {
                        //here we arrive in the case the user was already asked about the gps mode,
                        // therefore the trip already has the gps mode set
                        // TODO: this should trigger the trip.getGpsMode mode
                    }
                }

                // b) TODO: get username and set it
                // trip.setUserName();

                // c) the gps was already started (when app was started): take gps sample, date and time
                // TODO: started (see above) with a gps mode SLOW, so that you can sample immediately, then turn it to 'really slow'?
                // TODO: remember to set the mode to continuous if the checkbox was checked
                //trip.setGpsMode(GpsMode.FAST); //? this before sampling
                //trip.setGpsMode(GpsMode.SLOW); //? this after sampling
                //trip.setStartLatitude();
                //trip.setStartLongitude();
                trip.setStartTimeInMilliseconds(System.currentTimeMillis());
                // d) set lable to Monicet - Add location
                setTitle(getText(R.string.app_name) + " - " + getText(R.string.add_location));

                // d) make itself and the checkbox gone or invisible
                fabStart.setVisibility(View.INVISIBLE);
                checkboxTrackingGps.setVisibility(View.INVISIBLE);

                // e) make the ADD and SEND buttons visible
                fabAdd.setVisibility(View.VISIBLE);
                fabSend.setVisibility(View.VISIBLE);

            }
        });
        //Step 2 ends here

        // Step 3 starts here:
        // SAVE floating action button
        // Check first if this Location has at least one non-empty Sighting (quantity different to 0)
        // If it is non-empty, open Comments dialogFragment
        // Else (all quantities are 0): put a toast on the screen (and do nothing), with the message:
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
                    Toast.makeText(MainActivity.this,
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

                // TODO: remember to set the adapter if the first time and to make the listview visible
                //trip.addLocation();
                if (trip.getNumberOfLocations() == 0) {
                    // if the trip has no locations, create a new one from resources
                    trip.addLocation(buildNewLocationFromResources());
                } else {
                    // important to create a new location based on the seed(current) one, and not just reference the current one
                    // and the current one must be used, not a different one,
                    // because it' guaranteed to hold the valid state data for user input objects etc.
                    // if for one location, comments are turned off, that should be copied to the next location
                    // we can insure that correct chaining only if that next location is created on the bases of the previous location
                    trip.addLocation(new Location(trip.getSeedLocation()));
                }

                // do I need this after every add - or just once, when it's first pressed (in the if above)
                // make listview (sighting adapter - list of sightings) visible
                listView.setVisibility(View.VISIBLE);

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

                if (trip.getNumberOfLocations() == 0) {
                    Toast.makeText(
                            MainActivity.this,
                            R.string.no_locations_toast_message,
                            Toast.LENGTH_LONG
                    ).show();

                } else {
                    // TODO: step I - Sample GPS, Date, Time and save Trip instance end_gps, end_date/time
                    //trip.setEndLatitude();
                    //trip.setEndLongitude()
                    trip.setEndTimeInMilliseconds(System.currentTimeMillis());
                    // step I ends here

                    // Alex: android:installLocation is internalOnly
                    // TODO:  Give the files good names (XXXX is the time, or user or trip number etc.)
                    // TODO: write the json and csv files internally
                    // TODO: try to send them via a http post request to a server... if successful, delete the files, if not don't delete the files
                    // TODO: maybe just create the files and leave all the rest to the service (so that the 2 don't step on each other's toes)
                    // TODO: create that server page (page will display all non-empty sightings for the trip, and also create a kml with the csv)
                    // TODO: create that service that runs continuously, goes to the Monicet folder and sends all the json and csv files to the server (if successful, deletes them)
                    // TODO: be careful so that both the service and the send button try to send the data to the same place (DRY)

                    // step II - create the files to be sent
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

                        String tripFileTitle = tripPrefix + System.currentTimeMillis();
                        String tripFileName = tripFileTitle + AllowedFileExtension.JSON;
                        trip.setTripFileName(tripFileName);

                        if (trip.getGpsMode() == GpsMode.CONTINUOUS) {

                            String routeFileTitle = routePrefix + System.currentTimeMillis();
                            String routeFileName = routeFileTitle + AllowedFileExtension.CSV;
                            trip.setRouteFileName(routeFileName); // this will be written to the JSON file
                            File routeFile = new File(directory, routeFileTitle);
                            FileWriter routeWriter = new FileWriter(routeFile);
                            routeWriter.append(trip.getUserName());
                            routeWriter.append(",");
                            routeWriter.append(tripFileName);
                            routeWriter.append(",");
                            routeWriter.append(routeFileName);
                            routeWriter.append("\r\n"); //routeWriter.append(System.getProperty("line.separator"));

                            for (Map.Entry<Long, double[]> entry : trip.getContinuousData().entrySet()) {
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
                        // get rid of the empty sightings
                        trimTrip();
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
                    // step II ends here

                    // step III - enable and start mechanisms designed to send (and delete if sent)
                    // those files via the Internet (they all use the Utils method SendAndDeleteFiles,
                    // which first checks for a live Internet connection)
                    // This is the right moment (after SEND) for enabling them, because they've disabled
                    // themselves by now (if folder doesn't contain json or csv files aka empty...
                    // although it always has one instant-run file)

                    // first - use GCM Network Manager. I updates itself and stops when folder is empty
                    // (setPersisted and updateCurrent are true). Sends the files within a minute when connected.
                    // maybe pass it the application context - it only uses it for the path anyways
                    // MainActivity.this.getApplicationContext(); //getApplication().getBaseContext();
                    // TODO: new Thread here or try retrofit inside the sendAndDeleteFiles method ? uses a different thread?
                    SendFilesTaskService.scheduleOneOff(MainActivity.this);

                    // secondly, use broadcast receivers
                    // a) Alarm Manager receiver: starts an hourly alarm after BOOT COMPLETED or START_ACTION
                    // which starts the receiver, which sends and deletes files.
                    // It disables itself if folder is empty. It doesn't listen to the network
                    // i) enable it
//                Utils.setComponentState(
//                        MainActivity.this,
//                        FilesAndBootReceiver.class,
//                        true
//                );
//                // ii) fire it:
//                Intent startIntent = new Intent(Utils.START_ACTION);
//                MainActivity.this.sendBroadcast(startIntent);

                    // b) dynamicReceiver: a dynamically created broadcast receiver to listen to the network change
                    // i) enable it: don't touch. It's an error prone mechanism. It was already registered.
                    // ii) fire it, so that it tries to send the files now (before a connection
                    // change occurs, in case the phone is already connected)
                    // it's fired above (the alarm and this dynamic receiver are both fired by START_ACTION)

                    // c) a static receiver (defined in xml, only working pre API 24 - Nougat)
                    // is deployed to listen to the network. It disables itself if the folder is empty
                    // i) enable it
//                Utils.setComponentState(
//                        MainActivity.this,
//                        StaticNetworkStateReceiver.class,
//                        true
//                );
                    // ii) fire it: no need to fire this one right know, because
                    // the dynamic receiver deals with the present moment

                    //step III ends here

                    // TODO: Then turn off the GPS service
                    trip.setGpsMode(GpsMode.OFF);
                    // also, actually turn the gps off

                    // Final point - Then stop the application. *make sure you finish it off. Test the order.
                    // http://stackoverflow.com/questions/10847526/what-exactly-activity-finish-method-is-doing
                    // returning to this app from Gmail ?
                    // http://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
                    // TODO: finish(); send: stop application from being in the foreground (exit)...kills the activity and? eventually the app
                    // but then I want a fresh trip object and initial view (just show them quickly, create object) and exit...
                    // There should be a button for starting a trip and a button for adding a location

                    //        if (files.length < 1 || files[0] == null) {//AsyncTask
                    //            return null;
                    //        }
                }// Alex: up to here
            }
        });
        //Step 5 ends here

    }

    // modify saveLocation(so that it takes 4 Views, fabadd, send, save and list_view) and the sighting adapter
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

        Toast.makeText(MainActivity.this,
                R.string.location_saved_confirmation_message, Toast.LENGTH_SHORT).show();
//        Toast.makeText(MainActivity.this,
//                R.string.location_saved_instructions_message, Toast.LENGTH_LONG).show();

        // TODO: is this smelly code? addressing the view by id.. we initialized variables with them in onCreate
        findViewById(R.id.fab_save).setVisibility(View.INVISIBLE);
        findViewById(R.id.fab_add).setVisibility(View.VISIBLE);
        findViewById(R.id.fab_send).setVisibility(View.VISIBLE);
        // TODO: maybe also set
        //findViewById(R.id.list_view).setVisibility(View.INVISIBLE);
    }

    public Location buildNewLocationFromResources() { //? should be: build array of sightings from resources

        // get the resources (species, descriptions, photos)
        String[] species = getResources().getStringArray(R.array.speciesArray);
        // TODO: implement getting the photo ids and description data later
        String[] photos = new String[30];
        String[] descriptions = new String[30]; // all descriptions can be in one single text file
        Arrays.fill(photos, "photo"); // remember to give the photos names like SpermWhale_1, CommonDolphin_X
        Arrays.fill(descriptions, "description");
        // here (if the 3 arrays have the same size, at least check) add each sighting to the list, one by one
        int sizeOfArrays = species.length;
        if ( sizeOfArrays != photos.length || sizeOfArrays != descriptions.length) {
            Log.d("MainActivity", "the sizes of the specie, photo and description arrays are not the same");
        }

        // create an array of sightings
        ArrayList<Sighting> sightings = new ArrayList<Sighting>(sizeOfArrays);

        for (int i = 0; i < sizeOfArrays; i++ ) {
            Animal animal = new Animal(species[i], photos[i], descriptions[i]);
            sightings.add(new Sighting(animal));
        }

        return new Location(sightings);
    }

    // no argument for this one, trip is a class variable
    //the activity has finish() at the end, therefore the big trip gets gc-ed, so decide what it is that you want to save
    // to the json file... for reopening...do you really want to reopen the big trip on the phone?
    public void trimTrip() {

        for (int i = 0; i < trip.getNumberOfLocations(); i++) {
            Iterator<Sighting> iter = trip.getLocationAtIndex(i).getSightings().iterator();

            while (iter.hasNext()) {
                Sighting sighting = iter.next();

                if (sighting.isEmpty()) {
                    iter.remove();
                }
            }
        }
    }

    //    public void showGpsModeDialog(final UserInput<GpsMode> gpsModeUserInput) {}
    // it comes here only if the user changeable variable is visible (it was not shown before, in this case)

}
