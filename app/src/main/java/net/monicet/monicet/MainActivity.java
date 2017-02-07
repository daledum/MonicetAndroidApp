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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static android.R.string.no;

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

                                // Alex - test
//                                Toast.makeText(getApplicationContext(),
//                                        ""+trip.getCurrentLocation().getAdditionalLatitude(), Toast.LENGTH_SHORT).show();
//                                Toast.makeText(getApplicationContext(),
//                                        ""+trip.getCurrentLocation().getAdditionalLongitude(), Toast.LENGTH_SHORT).show();


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
                //trip.setEndTimeInMilliseconds(System.currentTimeMillis());

                // TODO: 2 - Then jasonize (toJSON, GSON library) your Trip instance object
                // TODO: 3 - Then serialize (save .json text file). Give the file a XXXX name,
                // where XXXX is the time, or user or trip number etc.

                // when using SENDTO: http://stackoverflow.com/questions/3132889/action-sendto-for-sending-an-email
                // sendIntent.setData(Uri.parse("mailto:alex_samprasno1@yahoo.co.uk"));
                // how to use Gmail only:http://stackoverflow.com/questions/16645100/how-to-send-attached-file-using-intent-in-android

                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"alex_samprasno1@yahoo.co.uk"});
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "A new trip - name of JSON file"); // add something else
                sendIntent.putExtra(Intent.EXTRA_TEXT, "toString of trip here");// or tripJson File to String
                sendIntent.putExtra(Intent.EXTRA_TITLE, "Please, choose Gmail :)");

                // TODO: continuous GPS TRK, GPX, KML, KMZ,PLT
                if (trip.getGpsMode() == GpsMode.CONTINUOUS) {
                    // A - a file with the appropriate extension (TRK, GPX, KML, KMZ,PLT) will be created
                    // B - and the file's name will be assigned to to the routeFileName variable (empty, by default)
                    // trip.setRouteFileName();
                    // C - Trip's continuousGpsSamples (Set), continuousDateTime (Set) (empty by default)
                    // will be saved inside this file
                    // D - taking the json file, make a zip file
                    // and attach the file to the intent
                    // https://developer.android.com/reference/java/util/zip/ZipOutputStream.html
                    // http://stackoverflow.com/questions/25594792/zipoutputstream-produces-corrupted-zip-file-on-android
                    // http://stackoverflow.com/questions/34978608/android-java-zipping-files-and-send-with-intent
                    sendIntent.setType("application/zip");

                } else {
                    sendIntent.setType("application/json");
                }
                // Alex: or intent.setType("*/*");

                // NB make sure it uses the correct file (tripXXX.zip or tripXXX.json)
                // This opens the file
                //File tripFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/Download/monicet/trip.json");
                File jsonFile = new File(Environment.getExternalStorageDirectory(), "/Download/monicet/trip.json");

//                try {
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                String pathname = Environment.getExternalStorageDirectory().getAbsolutePath();
//                String filename = "/MyFiles/mysdfile.txt";
//                File file = new File(pathname, filename);

                Uri jsonFileUri = Uri.fromFile(jsonFile);
                sendIntent.putExtra(Intent.EXTRA_STREAM, jsonFileUri);

                // TODO: 4 - Then turn off the GPS service
                trip.setGpsMode(GpsMode.OFF);
                // also, actually turn the gps off

                // 5 - send the saved file(type text) via implicit intent (forced Gmail to ?@monicet.net)
                if (sendIntent.resolveActivity(getPackageManager()) != null) {
                    // add a try catch here
                    sendIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                    // the one below doesn't work
                    //sendIntent.setClassName("com.google.android.gm", "com.google.android.gm.ConversationListActivity");
                    startActivity(sendIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "No way", Toast.LENGTH_SHORT).show();
                }

                Toast.makeText(getApplicationContext(), "" + Environment.getExternalStorageDirectory().getAbsolutePath(),
                        Toast.LENGTH_SHORT).show(); // this comes after opening gmail
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
        gpsAlertDialogBuilder.setTitle("GPS Tracking");
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

        gpsModeUserInput.setContent(GpsMode.SLOW); // not too slow, it's still needed by the SEND button, when saving the trip

        for (Sighting sighting: currentLocation.getSightings()) {
            sighting.getQuantityUserInput().setVisible(false);
        }
        sightingAdapter.notifyDataSetChanged();

        Toast.makeText(getApplicationContext(),
                R.string.location_saved_confirmation_message, Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(),
                R.string.location_saved_instructions_message, Toast.LENGTH_LONG).show();

        findViewById(R.id.fab_save).setVisibility(View.INVISIBLE);
        findViewById(R.id.fab_add).setVisibility(View.VISIBLE);
        findViewById(R.id.fab_send).setVisibility(View.VISIBLE);
    }

    public Location buildLocationFromResources() {

        String[] species = getResources().getStringArray(R.array.speciesArray);
        // TODO: implement getting the photo ids and description data later
        String[] photos = new String[30];
        String[] descriptions = new String[30];
        Arrays.fill(photos, "photo");
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
