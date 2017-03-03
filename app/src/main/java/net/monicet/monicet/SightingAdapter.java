package net.monicet.monicet;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by ubuntu on 28-02-2017.
 */

public class SightingAdapter extends ArrayAdapter<Sighting> {

    private final MainActivityInterface mainActivity;
    private final AnimalAdapter animalAdapter;

    public SightingAdapter(MainActivityInterface vMainActivity,
                           ArrayList<Sighting> sightings,
                           AnimalAdapter vAnimalAdapter) {
        super(vMainActivity.getMyActivity(), 0, sightings);// it wanted a context, an activity is a context
        mainActivity = vMainActivity;
        animalAdapter = vAnimalAdapter;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_sighting, parent, false);
        }

        final Sighting currentSighting = getItem(position);

        if (currentSighting != null) {

            // I want to reinflate the view ? every time?
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_sighting, parent, false);

            // show the STOP/END/FINISH image button
            ImageButton endImgBtn =
                    (ImageButton)convertView.findViewById(R.id.end_finish_imageButton);
            endImgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //sets and saves the end time and gps for the current sighting
                    currentSighting.getEndTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                    // TODO: maybe set the gps sampling rate to something quick enough to sample on such a short notice
                    //currentSighting.getEndTimeAndPlace().setLatitude();
                    //currentSighting.getEndTimeAndPlace().setLongitude();
                    // TODO: needs a Toast here.. confirming it ended it OK
                    Toast.makeText(
                            getContext(),
                            R.string.sighting_finished_confirmation_message,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });

            // show the Comments image button
            // listener with dialog.. get end quantity, user gps, comments etc and saves to current sighting
            ImageButton commentsImgBtn =
                    (ImageButton)convertView.findViewById(R.id.comments_imageButton);
            commentsImgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainActivity.showSightingCommentsDialog(currentSighting);
                }
            });

            // show the quantity text
            TextView startQuantityTxtView =
                    (TextView)convertView.findViewById(R.id.animal_quantity_text_view);
            startQuantityTxtView.setText(
                    String.valueOf(currentSighting.getAnimal().getStartQuantity())
            );// was ""+

            // show the specie name
            TextView specieNameTxtView =
                    (TextView)convertView.findViewById(R.id.specie_name_text_view);
            specieNameTxtView.setText(
                    String.valueOf(currentSighting.getAnimal().getSpecie().getName())
            );

            //TODO: change layout and show the start time of that sighting below the quantity and specie name

            // when clicking on the sighting
            View sightingDetailsView = convertView.findViewById(R.id.sighting_details);
            sightingDetailsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // clicking on quantity or specie name or time
                    // TODO: CLICK on sighting and ADD share the openSighting method (Main Activity):
                    mainActivity.openSighting(
                            // TODO: find a way around this... Utils set it once from resources
                            //too expensive to getMyActivity
                            getContext().getText(R.string.app_name)
                                    + " - " +
                                    getContext().getText(R.string.edit_sighting),
                            currentSighting,
                            animalAdapter
                    );
                }
            });
        }

        return convertView;
    }

}
