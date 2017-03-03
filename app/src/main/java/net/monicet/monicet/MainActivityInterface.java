package net.monicet.monicet;

import android.app.Activity;

import static android.R.attr.label;

/**
 * Created by ubuntu on 01-03-2017.
 */

public interface MainActivityInterface {

    void openSighting(String label,
                      Sighting sighting,
                      AnimalAdapter animalAdapter);

    void showSightingCommentsDialog(Sighting sighting);

    Activity getMyActivity();
}
