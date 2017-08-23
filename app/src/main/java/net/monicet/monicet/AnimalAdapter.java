package net.monicet.monicet;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by ubuntu on 24-01-2017.
 */

// this receives the seed animals...same 30 animals, with one of them maybe with a non zero quantity
    //it will always receive non null values

public class AnimalAdapter extends ArrayAdapter<Animal> {//implements Filterable

    public AnimalAdapter(Activity context, ArrayList<Animal> animals) {
        super(context, 0, animals);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_animal, parent, false);
        }

        final Animal currentAnimal = getItem(position);

        // set the specie name
        TextView specie = (TextView)convertView.findViewById(R.id.specie_text_view);
        specie.setText(String.valueOf(currentAnimal.getSpecie().getName()));

        // set the photo button for that specie and implement logic when clicked
        ImageButton photo = (ImageButton)convertView.findViewById(R.id.photo_imageButton);
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: open image for that particular specie
                Toast.makeText(getContext(), currentAnimal.getSpecie().getPhoto(), Toast.LENGTH_SHORT).show();
            }
        });

        // set the description button for that specie and implement logic when clicked
        ImageButton description = (ImageButton)convertView.findViewById(R.id.description_imageButton);
        description.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: open description for that particular specie
                Toast.makeText(getContext(), currentAnimal.getSpecie().getDescription(), Toast.LENGTH_SHORT).show();
            }
        });

        final NumberPicker quantity = (NumberPicker)convertView.findViewById(R.id.animal_quantity_number_picker);
        quantity.setMinValue(0);
        quantity.setMaxValue(Utils.MAX_VALUE);
        quantity.setValue(currentAnimal.getStartQuantity());

        quantity.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                // when changing the values, change the quantities for the seed/generic animals
                currentAnimal.setStartQuantity(quantity.getValue());
            }
        });

        return convertView;
    }

//    @NonNull
//    @Override
//    public Filter getFilter() {
//        return super.getFilter();
//    }
//
//    private class AnimalFilter extends Filter {
//
//        @Override
//        protected FilterResults performFiltering(CharSequence constraint) {
//
//            FilterResults filterResults = new FilterResults();
//
//            return null;
//        }
//
//        @Override
//        protected void publishResults(CharSequence constraint, FilterResults results) {
//
//        }
//    }

}
