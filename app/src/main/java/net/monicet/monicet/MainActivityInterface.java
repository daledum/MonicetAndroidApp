package net.monicet.monicet;

import android.app.Activity;
import android.widget.ArrayAdapter;

import static android.R.attr.label;

/**
 * Created by ubuntu on 01-03-2017.
 */

public interface MainActivityInterface {

    void openSighting(String label,
                      Sighting sighting,
                      AnimalAdapter animalAdapter);

    void showSightings(SightingAdapter sightingAdapter);

    void showSightingCommentsDialog(Sighting sighting, SightingAdapter sightingAdapter);

    void deleteSightingCommentsDialog(Sighting sighting, SightingAdapter sightingAdapter);

    Activity getMyActivity();
}
