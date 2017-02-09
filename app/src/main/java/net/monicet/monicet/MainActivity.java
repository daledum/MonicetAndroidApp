package net.monicet.monicet;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static android.R.attr.data;
import static android.R.attr.path;
import static android.R.string.no;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class MainActivity extends AppCompatActivity {

    // declaring trip as a class field made the app not start.. why?
    //final Trip trip = new Trip(buildLocationFromResources());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // Testing quantity setting and displaying // Alex: remove this when finished
        //trip.getCurrentLocation().getSightings().get(0).setQuantity(89); // setValue must come after setmax min
        //trip.getCurrentLocation().getSightings().get(1).setQuantity(33);
        //sightingAdapter.notifyDataSetChanged();

        // Initialization steps:
        // Change label to Monicet - Stop 1
        setTitle(getText(R.string.app_name) + " - " +
                getText(R.string.location) + " " + trip.getNumberOfLocations());
        // TODO: get username and set it
        // trip.setUserName();
        // TODO: start with a reasonably fast gps mode, so that you can sample immediately, then turn it to 'really slow'
        trip.setGpsMode(GpsMode.FAST);
        //trip.setStartLatitude();
        //trip.setStartLongitude();
        //trip.setStartTimeInMilliseconds(System.currentTimeMillis());
        trip.setGpsMode(GpsMode.SLOW);


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

                        //DialogFragment commentsDialogFragment = CommentsDialogFragment.newInstance();
                        //commentsDialogFragment.show(getFragmentManager(), "comments");
                        ////saveLocation(trip.getCurrentLocation(), sightingAdapter, trip.getGpsModeUserInput());  // Alex: this will be called inside the dialog fragment
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
                                double gpsDegrees = Utility.parseGpsToDouble(
                                        latitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LATITUDE
                                );
                                EditText latitudeMinutes = (EditText) rootView.findViewById(R.id.lat_minutes_edit_text);
                                double gpsMinutes = Utility.parseGpsToDouble(
                                        latitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                                );
                                EditText latitudeSeconds = (EditText) rootView.findViewById(R.id.lat_seconds_edit_text);
                                double gpsSeconds = Utility.parseGpsToDouble(
                                        latitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                                );

                                trip.getCurrentLocation().setAdditionalLatitude(
                                        Utility.convertDegMinSecToDecimal(gpsDegrees, gpsMinutes, gpsSeconds)
                                );

                                EditText longitudeDegrees = (EditText) rootView.findViewById(R.id.long_degrees_edit_text);
                                gpsDegrees = Utility.parseGpsToDouble(
                                        longitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LONGITUDE
                                );
                                EditText longitudeMinutes = (EditText) rootView.findViewById(R.id.long_minutes_edit_text);
                                gpsMinutes = Utility.parseGpsToDouble(
                                        longitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                                );
                                EditText longitudeSeconds = (EditText) rootView.findViewById(R.id.long_seconds_edit_text);
                                gpsSeconds = Utility.parseGpsToDouble(
                                        longitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                                );

                                trip.getCurrentLocation().setAdditionalLongitude(
                                        Utility.convertDegMinSecToDecimal(gpsDegrees, gpsMinutes, gpsSeconds)
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
                                // do nothing
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
                // TODO: 1 - Sample GPS, Date, Time and save Trip instance end_gps, end_date/time
                //trip.setEndLatitude();
                //trip.setEndLongitude()
                trip.setEndTimeInMilliseconds(System.currentTimeMillis());

                // TODO: serialize your continuous data (if in continuous mode) and trip object. Give the files good names
                // where XXXX is the time, or user or trip number etc.

                // getExternalFilesDir(null): /storage/sdcard0/Android/data/net.monicet.monicet/files
                // Environment.getExternalStorageDirectory(): /storage/sdcard0/
                // Environment.getExternalStorageDirectory().getAbsolutePath(): /storage/sdcard0/
                // getFilesDir(): /data/data/net.monicet.monicet/files

                // TODO: I changed android:installLocation to internalOnly
                // in order for future service to send the files when connected to the Internet
                //https://developer.android.com/guide/topics/data/install-location.html
                //http://stackoverflow.com/questions/6169059/android-event-for-internet-connectivity-state-change
                //http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
                //http://stackoverflow.com/questions/3767591/check-intent-internet-connection
                //http://stackoverflow.com/questions/16824341/keep-broadcast-receiver-running-after-application-is-closed
                //http://stackoverflow.com/questions/16429354/broadcast-receiver-still-running-after-app-close-android
                //http://stackoverflow.com/questions/12274997/why-broadcastreceiver-works-even-when-app-is-in-background
                //http://stackoverflow.com/questions/26134560/close-application-from-broadcast-receiver

                // TODO: write the json and csv files internally
                // TODO: try to send them via a http post request to a server... if successful, delete the files, if not don't delete the files
                // TODO: maybe just create the files and leave all the rest to the service (so that the 2 don't step on each other's toes)
                // TODO: create that server page (page will display all non-empty sightings for the trip, and also create a kml with the csv)
                // TODO: create that service that runs continuously, goes to the Monicet folder and sends all the json and csv files to the server (if successful, deletes them)
                // TODO: be careful so that both the service and the send button try to send the data to the same place (DRY)
                try {
                    // TODO: stop using this when a service was created to send (and delete) json and csv files
                    String routePrefix = "route";
                    String tripPrefix = "trip";

                    // Deleting files from the internal storage
//                        File directory = new File(getFilesDir().toString());
//                        File[] files = directory.listFiles();
//                        ArrayList<String> namesOfFilesToDelete = new ArrayList<String>();
//                        if (files != null ) {
//
//                            for (int i = 0; i < files.length; i++) {
//                                if (files[i].getName().toLowerCase().contains(routePrefix.toLowerCase()) ||
//                                        files[i].getName().toLowerCase().contains(tripPrefix.toLowerCase())) {
//                                    namesOfFilesToDelete.add(files[i].getName());
//                                }
//                            }
//
//                            for (int i = 0; i < files.length; i++) {
//                                // or deleteFile("filename");//myContext.deleteFile(fileName);
//                                if (namesOfFilesToDelete.contains(files[i].getName())) { files[i].delete(); }
//                            }
//                        }

                    File rootPathExternal = new File(Environment.getExternalStorageDirectory(), "Monicet");//getFilesDir(): /data/data/net.monicet.monicet/files
                    if (!rootPathExternal.exists()) { rootPathExternal.mkdirs(); } // Alex: maybe a try catch here, throws a SecurityException?

                    if (trip.getGpsMode() == GpsMode.CONTINUOUS) {

                        String routeFileName = routePrefix + System.currentTimeMillis() + ".csv";
                        trip.setRouteFileName(routeFileName);
                        File routeFile = new File(rootPathExternal, routeFileName); // for external storage
                        //File routeFile = new File(getFilesDir(), routeFileName); // for internal storage
                        FileWriter routeWriter = new FileWriter(routeFile);

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
                    }

                    String tripFileTitle = tripPrefix + System.currentTimeMillis();
                    String tripFileExtension = ".json";
                    String tripFileName = tripFileTitle + tripFileExtension;
                    trip.setTripFileName(tripFileName);
                    Gson gson = new GsonBuilder().create();
                    File tripFile = new File(rootPathExternal, tripFileName); // external storage
                    //File tripFile = new File(getFilesDir(), tripFileName);// internal storage
                    FileWriter tripWriter = new FileWriter(tripFile);

                    tripWriter.append(gson.toJson(trip));
                    tripWriter.flush(); // Alex: redundant?
                    tripWriter.close();

                } catch (Exception e) {//IOException e
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "file exception", Toast.LENGTH_SHORT).show(); // Alex: remove this
                }

                // TODO: Then turn off the GPS service
                trip.setGpsMode(GpsMode.OFF);
                // also, actually turn the gps off

                // Final point - Then stop the application. *make sure you finish it off. Test the order.
                // http://stackoverflow.com/questions/10847526/what-exactly-activity-finish-method-is-doing
                // returning to this app from Gmail ?
                // http://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
                //finish(); // TODO: uncomment this when done
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
        //currentLocation.setTimeInMilliseconds(System.currentTimeMillis());

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
