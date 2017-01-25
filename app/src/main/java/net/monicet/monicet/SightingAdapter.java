package net.monicet.monicet;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.resource;

/**
 * Created by ubuntu on 24-01-2017.
 */

public class SightingAdapter extends ArrayAdapter<Sighting> {
    public SightingAdapter(Activity context, ArrayList<Sighting> sightings) {
        super(context, 0, sightings);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Sighting currentSighting = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        NumberPicker numberPicker = (NumberPicker) convertView.findViewById(R.id.quantity_number_picker);
        numberPicker.setValue(currentSighting.getQuantity());
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(99);

        TextView specie = (TextView) convertView.findViewById(R.id.specie_text_view);
        specie.setText(currentSighting.getAnimal().getSpecie());

        ImageButton photo = (ImageButton) convertView.findViewById(R.id.photo_imageButton);
        //photo.set(?);

        ImageButton description = (ImageButton) convertView.findViewById(R.id.description_imageButton);
        //description.set(?);

        return convertView;
    }
}
