package net.monicet.monicet;

import android.app.Activity;
import android.widget.ArrayAdapter;

import static android.R.attr.label;

/**
 * Created by ubuntu on 01-03-2017.
 */

public interface MainActivityInterface {

    void openSighting(String label,
                      String family,
                      Sighting sighting);

    void showSightings();

    void showSightingCommentsDialog(Sighting sighting);

    void deleteSightingDialog(Sighting sighting);

    void captureCoordinates(TimeAndPlace timeAndPlace);

    Activity getMyActivity();
}
