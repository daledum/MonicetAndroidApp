package net.monicet.monicet;

import android.app.Activity;
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

/**
 * Created by ubuntu on 24-01-2017.
 */

public class SightingAdapter extends ArrayAdapter<Sighting> {

    //private final Trip trip;
    private final UserInput<GpsMode> gpsModeUserInput;

    public SightingAdapter(Activity context, ArrayList<Sighting> sightings, UserInput<GpsMode> vGpsModeUserInput) {
        super(context, 0, sightings);
        gpsModeUserInput = vGpsModeUserInput;
    }

//    public SightingAdapter(Activity context, Trip vTrip) {
//        super(context, 0, vTrip.getCurrentLocation().getSightings());
//        trip = vTrip;
//    }

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
        quantity.setMinValue(0);
        quantity.setMaxValue(99);
        quantity.setValue(currentSighting.getQuantity()); // autoboxing happening here

        if (currentSighting.getQuantityUserInput().isVisible() == true) {
            // if coming back to a saved Location to make changes to it (in a future version), uncomment this:
            quantity.setBackgroundColor(Color.TRANSPARENT);
            quantity.setEnabled(true);
            quantity.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    currentSighting.setQuantity(quantity.getValue()); // autoboxing happening here

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
                    if (gpsModeUserInput.getContent() != GpsMode.CONTINUOUS) {
                        if (gpsModeUserInput.getContent() != GpsMode.FAST) {
                            gpsModeUserInput.setContent(GpsMode.FAST);
                            // TODO: GPS this should actually change the sampling rate (via a View listener?)
                        }
                    }
                }
            });
        } else {
            quantity.setBackgroundColor(Color.GRAY);
            quantity.setEnabled(false);
        }

        return convertView;
    }

}
