package net.monicet.monicet;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import static android.R.string.no;
import static net.monicet.monicet.R.string.location;

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

        //next time, do this after the addLocation(), if in the same activity (with access to the adapter)
        // sightingAdapter.clear(); sightingAdapter.add(trip.getCurrentLocation().getSightings());

        final SightingAdapter sightingAdapter = new SightingAdapter(this, trip);

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(sightingAdapter);
        // Step 1 ends here

        // Testing quantity setting and displaying // Alex: remove this when finished
        trip.getCurrentLocation().getSightings().get(0).setQuantity(89); //- too slow, works only when scrolling
        trip.getCurrentLocation().getSightings().get(1).setQuantity(33);
        sightingAdapter.notifyDataSetChanged();

        // Extra steps:
        // Change label to Monicet - Stop 1
        setTitle(getText(R.string.app_name) + " - " +
                getText(location) + " " + trip.getNumberOfLocations());
        // TODO: get username - do this when dealing with GPS

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
        FloatingActionButton fabSave = (FloatingActionButton) findViewById(R.id.fab_save);
        FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        FloatingActionButton fabSend = (FloatingActionButton) findViewById(R.id.fab_send);

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
                } else {// add additionalLat and long visibility ?
                    // in the case the user hasn't turned off the comments (or it's the first show)
                    if (trip.getCurrentLocation().getCommentsUserInput().isVisible() == true) {
                        // show the comments dialog fragment here

                        // TODO: Comments Cancel button: do nothing
                        // TODO: OK button pressed, so this should happen:
                        // 1 - save user gps to Location's user gps. Be careful with the format (conversion...
                        // or just don't convert at all - straight to String)
                        // 2 - save user's comments to Location's user comments
                        // 3 - if the 'don't bother me with more messages' check-box is checked before pressing this OK button,
                        // set the comments, lat and long userInput isVisible to false
                        // trip.getCurrentLocation().setCommentsUserInputActive(false);// plus user lat and long
                        // 4 - call (passing it the whole trip) ... trip is required for setting the GPS Mode
                        // and the adapter for disabling the number pickers
                        saveLocation(trip, sightingAdapter);
                        // TODO: make sure the dialog closes when it should

                    } else {
                        saveLocation(trip, sightingAdapter);
                    }
                }
            }
        });
        // Step 3 ends here

    }

    public void showGpsModeDialog(final UserInput<GpsMode> gpsModeUserInput) {
        // it comes here only if the user changeable variable is visible (it was not shown before, in this case)
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("GPS Tracking");
        alertDialogBuilder.setMessage(R.string.tracking_dialog_message);
        alertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                gpsModeUserInput.setContent(GpsMode.CONTINUOUS);
                // TODO: GPS this should trigger continuous gps
            }
        });
        alertDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
                // dialog.dismiss();
            }
        });
        alertDialogBuilder.create();
        alertDialogBuilder.show();
        // TODO: if the user presses No or presses outside, I should go into slow or fast gps mode
        // TODO: this should trigger slow or fast gps, too
        // to check, verify that it's not in continuous mode, and if not set it to slow or fast
        // Now, set the user input state to false, registering the fact that the user was asked the question
        gpsModeUserInput.setVisible(false);
    }

    public void saveLocation(Trip trip, SightingAdapter sightingAdapter) {
        Location currentLocation = trip.getCurrentLocation();
        // TODO: sample (and save Location instance GPS, date and time)
        //currentLocation.setLatitude();
        //currentLocation.setLongitude();
        //currentLocation.setTimeInMilliseconds();

        trip.setGpsMode(GpsMode.SLOW); // not too slow, it's still needed by the SEND button, when saving the trip

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
