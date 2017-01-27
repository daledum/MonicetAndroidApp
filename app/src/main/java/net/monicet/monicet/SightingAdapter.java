package net.monicet.monicet;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.resource;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;

/**
 * Created by ubuntu on 24-01-2017.
 */

public class SightingAdapter extends ArrayAdapter<Sighting> {

    private final Trip trip;

//    public SightingAdapter(Activity context, ArrayList<Sighting> sightings) {
//        super(context, 0, sightings);
//    }

    public SightingAdapter(Activity context, Trip vTrip) {
        super(context, 0, vTrip.getCurrentLocation().getSightings());
        trip = vTrip;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Sighting currentSighting = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        TextView specie = (TextView) convertView.findViewById(R.id.specie_text_view);
        specie.setText(currentSighting.getAnimal().getSpecie());

        ImageButton photo = (ImageButton) convertView.findViewById(R.id.photo_imageButton);
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: open image for that particular specie
                Toast.makeText(getContext(), currentSighting.getAnimal().getPhoto(), Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton description = (ImageButton) convertView.findViewById(R.id.description_imageButton);
        description.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: open description for that particular specie
                Toast.makeText(getContext(), currentSighting.getAnimal().getDescription(), Toast.LENGTH_SHORT).show();
            }
        });

        final NumberPicker quantity = (NumberPicker) convertView.findViewById(R.id.quantity_number_picker);
        quantity.setValue(currentSighting.getQuantity()); // Alex: is this positioned well here
        quantity.setMinValue(0);
        quantity.setMaxValue(99);
        quantity.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                // TODO: successful SAVE - if after a successful SAVE (see SAVE button): do nothing
                // this will disable(grey out?) the buttons (make sure they are not clickable)
                // so, when coming here after a save, they could be disabled, if they are disabled, do nothing
                //quantity.setEnabled(false); // no use here
                //quantity.setBackgroundColor(Color.GRAY);

                currentSighting.setQuantity(quantity.getValue());

                // TODO: implement this (GPS, date, time)
                // GPS, date and time values from the phone should be saved to their Sighting
                // instance variables.
                // If several clicks, save each time, overwriting,
                // or immediately afterwards, in case it needs a few seconds for calibrating
                //currentSighting.setTimeInMilliseconds();
                //currentSighting.setLatitude();
                //currentSighting.setLongitude();

                // TODO: implement this only in the default (no GPS tracking, infrequent mode)
                // first click/press on the NumberPicker of a
                // Sighting instance for every Location instance: start sampling GPS data more often (quickly).
                // Location's firstNumberPickerClick is true by default/in constructor,
                // (this variable is used when NumberPicker or SAVE is pressed).
                // The variable is true at the start of each Location instance (when a new object is created).
                // When I press on the NumberPicker it checks to see if the boolean is true,
                // if so, it sets it to false (so that the subsequent clicks/presses do nothing)
                // and it starts the quick sampling (look at the class definition, logic was reversed)
                if (trip.isContinuousGpsTrackingOn() == false) {
                    if (trip.getCurrentLocation().isQuantityChangedAtLeastOnce() == false) {
                        trip.getCurrentLocation().setQuantityChangedAtLeastOnce(true);
                        // TODO: implement this (start GPS quick sampling - inside the infrequent mode)
                        // TODO: also, work with GPS via trip and create 'GPS mode' variables for it ?
                    }
                    // Alex: remove this after implementing the above
                    Toast.makeText(getContext(), "" +
                            trip.getCurrentLocation().isQuantityChangedAtLeastOnce(), Toast.LENGTH_SHORT).show();
                }


            }
        });


        return convertView;
    }

}
