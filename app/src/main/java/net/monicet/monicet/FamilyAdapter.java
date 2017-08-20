package net.monicet.monicet;

import android.content.Context;
import android.support.annotation.LayoutRes;
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
 * Created by ubuntu on 19-08-2017.
 */

public class FamilyAdapter extends ArrayAdapter<String> {

    private final MainActivityInterface mainActivity;

    public FamilyAdapter(MainActivityInterface vMainActivity, ArrayList<String> families) {
        super(vMainActivity.getMyActivity(), 0, families);
        mainActivity = vMainActivity;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //TODO: change list_item_sighting to list_item_family
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_sighting, parent, false);
        }

        final String currentFamily = getItem(position);

        if (currentFamily != null) {// in case no families are received back from the user's custom list (if no animal was chosen)

            TextView familyTxtView =
                    (TextView)convertView.findViewById(R.id.specie_family_text_view);
            familyTxtView.setText(currentFamily);
            familyTxtView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // and link the openedSighting to it (most recently added sighting),
                    // so that the save button knows where to save
                    mainActivity.openSighting(
                            getContext().getString(R.string.add_sighting),
                            currentFamily,
                            null
                    );
                }
            });

        }

        return convertView;
    }
}
