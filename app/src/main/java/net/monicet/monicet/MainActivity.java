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

public class MainActivity extends AppCompatActivity {

    // TODO: make trip 'global'
    //private Trip trip; // or public
    // I will eventually have to make trip a class variable in order for onPause etc methods to access it
    // I will then have to create separate listeners for my buttons (because trip will not be able to be final)

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
        //final Trip trip = new Trip(location);
        final Trip trip = new Trip(buildLocationFromResources());//was Trip(location)//can move outside 'global'

        //next time, do this after the addLocation(), if in the same activity (with access to the adapter)
        // sightingAdapter.clear(); sightingAdapter.add(trip.getCurrentLocation().getSightings());

//        SightingAdapter sightingAdapter =
//                new SightingAdapter(this, trip.getCurrentLocation().getSightings());
        SightingAdapter sightingAdapter = new SightingAdapter(this, trip);

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(sightingAdapter);
        // Step 1 ends here

        // Step 2 starts here:
        // open a dialog (if the dialog wasn't shown before)
        // and ask the user if they want the Continuous GPS Tracking Mode
        // if 'yes', set the trip variable accordingly
        if (trip.isGpsModeUserInputActive() == true) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("GPS Tracking");
            alertDialogBuilder.setMessage(R.string.tracking_dialog_message);
            alertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    trip.setGpsMode(GpsMode.CONTINUOUS);
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
            trip.setGpsModeUserInputActive(false);
        } else {
            //here we arrive in the case the user was already asked about the gps mode,
            // therefore the trip already has the gps mode set
            // TODO: this should trigger the trip.getGpsMode mode
        }

        //
        // b) after the dialog was exited, then we do nothing

        // Step 2 ends here

        // Step 3 starts here:
        // SAVE floating action button
        // Check first if this Location has at least one non-empty Sighting (quantity different to 0)
        // If it is non-empty, open Comments dialogFragment
        // Else (all quantities are 0): put a LONG Toast on the screen (and do nothing), with the message:
        //"your trip has no sightings. There is nothing to send. Please add the quantity of individuals that you've seen.
        // Or no species were seen/No animals were seen at this location. There is nothing to save."
        FloatingActionButton fabSave = (FloatingActionButton) findViewById(R.id.fab_save);
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean noAnimalsWereSeen = true;
                ArrayList<Sighting> sightings = trip.getCurrentLocation().getSightings();

                for (Sighting sighting: sightings) {
                    if (sighting.getQuantity() != 0) {
                        noAnimalsWereSeen = false;
                        break;
                    }
                }

                if (noAnimalsWereSeen) {
                    Toast.makeText(getApplicationContext(),
                            R.string.no_animals_toast_message, Toast.LENGTH_LONG).show();
                } else {
                    if (trip.getCurrentLocation().isCommentsUserInputActive() == true) {// add user lat and long state
                        showUserCommentsDialog();
                    }

                }
            }
        });
        // Step 3 ends here

        // Step 4 starts here: // Alex: remove this when finished
        trip.getCurrentLocation().getSightings().get(0).setQuantity(89); //- too slow, works only when scrolling
        trip.getCurrentLocation().getSightings().get(1).setQuantity(33);
        //sightingAdapter.notifyDataSetChanged();//works without this

        // Step 4 ends here


        // Extra steps:
        // Change label to Monicet - Stop 1
        setTitle(getText(R.string.app_name) + " - " +
                getText(R.string.location) + " " + trip.getNumberOfLocations());
        // TODO: get username - do this when dealing with GPS

    }

    public void showUserCommentsDialog() {
        // show the comments dialog fragment here
        // in case the user checks to stop the future comments
        // trip.getCurrentLocation().setCommentsUserInputActive(false);// plus user lat and long

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
