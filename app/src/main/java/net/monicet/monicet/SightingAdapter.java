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
        quantity.setValue(currentSighting.getQuantity());
        quantity.setMinValue(0);
        quantity.setMaxValue(99);
        quantity.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

                // set the variable etc only if quantity state is active
                if (currentSighting.isQuantityUserInputActive() == true) {
                    currentSighting.setQuantity(quantity.getValue());

                    // TODO: sample and save GPS
                    // GPS, date and time values from the phone should be saved to their Sighting
                    // instance variables.
                    // If several clicks, save each time, overwriting,
                    // or immediately afterwards, in case it needs a few seconds for calibrating
                    //currentSighting.setLatitude();
                    //currentSighting.setLongitude();
                    currentSighting.setTimeInMilliseconds(System.currentTimeMillis());


                    // while in the default (no GPS tracking, infrequent mode)
                    // first click/press on the NumberPicker of a sighting for every location:
                    // start sampling GPS data more often (fast/quickly)
                    if (trip.getGpsMode() != GpsMode.CONTINUOUS) {
                        if (trip.getGpsMode() != GpsMode.FAST) {
                            trip.setGpsMode(GpsMode.FAST);
                            Toast.makeText(getContext(), "" + trip.isGpsModeUserInputActive(),
                                    Toast.LENGTH_SHORT).show();// get rid of this
                            // TODO: GPS this should actually change the sampling rate (via a View listener?)
                        }
                    }
                }

            }
        });


        return convertView;
    }

}
