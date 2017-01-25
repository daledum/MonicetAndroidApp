package net.monicet.monicet;

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
        Trip trip = new Trip(location);

        //next time, do this after the addLocation()
        SightingAdapter sightingAdapter = new SightingAdapter(this, trip.getLastLocation().getSightings());

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(sightingAdapter);
    }
}
