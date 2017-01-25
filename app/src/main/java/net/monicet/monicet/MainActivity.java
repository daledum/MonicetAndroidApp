package net.monicet.monicet;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

import static android.R.id.list;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Step 1 starts here:
        // using the data from resources (containing specie names, photos and description),
        // create the first Location, then instantiate a Trip (which will contain one location)
        // and feed its array of sightings to the custom ListView ArrayAdapter
        String[] species = getResources().getStringArray(R.array.speciesArray);
        // TODO: implement later
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

        Location location = new Location(sightings);
        //this will create a trip with one location
        final Trip trip = new Trip(location);

        //next time, do this after the addLocation():
        // sightingAdapter.clear(); sightingAdapter.add(trip.getLastLocation().getSightings());

        SightingAdapter sightingAdapter = new SightingAdapter(this, trip.getLastLocation().getSightings());

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(sightingAdapter);
        // Step 1 ends here

        // Step 2 starts here:
        // open a dialog and ask the user if they want the Continuous GPS Tracking Mode
        // if 'yes', set the trip variable accordingly
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("GPS Tracking");
        alertDialogBuilder.setMessage("You are currently in the normal GPS sampling mode." +
                "\nActivate the continuous GPS sampling mode, which uses a lot of battery?");
        alertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                trip.setContinuousGpsTrackingOn(true);
            }
        });
        alertDialogBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
                // dialog.dismiss();
            }
        });
        alertDialogBuilder.create();
        alertDialogBuilder.show();
        // Step 2 ends here

    }
}
