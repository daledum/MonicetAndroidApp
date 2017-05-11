package net.monicet.monicet;

import android.support.annotation.NonNull;
import android.text.format.DateFormat;
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

    public SightingAdapter(MainActivityInterface vMainActivity,
                           ArrayList<Sighting> sightings) {
        super(vMainActivity.getMyActivity(), 0, sightings);// it wanted a context, an activity is a context
        mainActivity = vMainActivity;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_sighting, parent, false);
        }

        final Sighting currentSighting = getItem(position);

        if (currentSighting != null) {

            // I want to reinflate the view ? every time? There will only be 5 or 6 sightings at most
            //convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_sighting, parent, false);//TODO: Alex test this

            // show the STOP/END/FINISH image button
            ImageButton endImgBtn =
                    (ImageButton)convertView.findViewById(R.id.end_finish_imageButton);
            endImgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: new rule - end quantity can be set only on long sightings in comments only
                    // after the end button was pressed, after that, in comments you can change anything and the
                    // comments listener will update the views (via runonuithread?)
                    // the end quantity: when you click on a sighting and you change it, the end quantity
                    // will not be changed - you can change it inside comments
                    // pressing on a sighting only allows you to change the specie and the start quantity
                    currentSighting.getAnimal().
                            setEndQuantity(currentSighting.getAnimal().getStartQuantity());

                    // set time
                    currentSighting.getEndTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                    // and gps coordinates
                    mainActivity.captureCoordinates(currentSighting.getEndTimeAndPlace());

                    // refresh views
                    mainActivity.showSightings();

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

            ImageButton deleteImgBtn =
                    (ImageButton)convertView.findViewById(R.id.delete_imageButton);
            deleteImgBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainActivity.deleteSightingDialog(currentSighting);
                }
            });

            // show the start quantity text
            TextView startQuantityTxtView =
                    (TextView)convertView.findViewById(R.id.start_animal_quantity_text_view);
            startQuantityTxtView.setText(
                    String.valueOf(currentSighting.getAnimal().getStartQuantity())
            );

            // show the time the sighting started
            TextView startTimeSightingTextView =
                    (TextView)convertView.findViewById(R.id.sighting_start_time_text_view);
            startTimeSightingTextView.setText(
                    DateFormat.format(
                            //http://alvinalexander.com/java/jwarehouse/android/core/java/android/text/format/DateFormat.java.shtml
                            "kk:mm",
                            currentSighting.getStartTimeAndPlace().getTimeInMillis()
                    ).toString()
            );

            TextView endTimeSightingTextView =
                    (TextView)convertView.findViewById(R.id.sighting_end_time_text_view);
            TextView endQuantityTxtView =
                    (TextView)convertView.findViewById(R.id.end_animal_quantity_text_view);

            // doing these inside onClick of STOP/END did not keep between orientation changes
            // important to have it after the STOP button onClick listener
            // end time of sighting becomes visible when user presses STOP/END
            // when user pressed stopped end time stopped having the initial value
            if (currentSighting.getEndTimeAndPlace().getTimeInMillis() != Utils.INITIAL_VALUE) {

                // set and show inside the view the time the sighting ended (when user clicked on STOP button)
                endTimeSightingTextView.setText(
                        DateFormat.format(
                                "kk:mm",
                                currentSighting.getEndTimeAndPlace().getTimeInMillis()
                        ).toString()
                );
                endTimeSightingTextView.setVisibility(View.VISIBLE);

                // show the end quantity (after STOP/END press it becomes visible)
                endQuantityTxtView.setText(
                        String.valueOf(currentSighting.getAnimal().getEndQuantity())
                );
                endQuantityTxtView.setVisibility(View.VISIBLE);

                // disable STOP/END button
                endImgBtn.setEnabled(false);
            }

            // show the specie name
            TextView specieNameTxtView =
                    (TextView)convertView.findViewById(R.id.specie_name_text_view);
            specieNameTxtView.setText(
                    String.valueOf(currentSighting.getAnimal().getSpecie().getName())
            );

            // when clicking on the sighting
            View sightingDetailsView = convertView.findViewById(R.id.sighting_details);
            sightingDetailsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // clicking on quantity or specie name or time
                    // CLICK on sighting and ADD share the openSighting method (Main Activity):
                    mainActivity.openSighting(
                            //too expensive to getMyActivity ?
                            getContext().getText(R.string.app_name)
                                    + " - " +
                                    getContext().getText(R.string.edit_sighting),
                            currentSighting
                    );
                }
            });
        }

        return convertView;
    }

}
