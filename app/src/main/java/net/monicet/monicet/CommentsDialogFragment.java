package net.monicet.monicet;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import static android.os.Build.VERSION_CODES.M;

/**
 * Created by ubuntu on 03-02-2017.
 */

public class CommentsDialogFragment extends DialogFragment {
    static CommentsDialogFragment newInstance() {
        return new CommentsDialogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.comments_dialog_fragment, container, false);
        getDialog().setTitle(R.string.comments_message_title);

        CheckBox turnOffCommentsCheckBox = (CheckBox) rootView.findViewById(R.id.turn_off_comment_checkbox);
        EditText latitudeDegrees = (EditText) rootView.findViewById(R.id.lat_degrees_edit_text);
        EditText latitudeMinutes = (EditText) rootView.findViewById(R.id.lat_minutes_edit_text);
        EditText latitudeSeconds = (EditText) rootView.findViewById(R.id.lat_seconds_edit_text);
        EditText longitudeDegrees = (EditText) rootView.findViewById(R.id.long_degrees_edit_text);
        EditText longitudeMinutes = (EditText) rootView.findViewById(R.id.long_minutes_edit_text);
        EditText longitudeSeconds = (EditText) rootView.findViewById(R.id.long_seconds_edit_text);
        EditText comments = (EditText) rootView.findViewById(R.id.comments_edit_text);
        Button cancelButton = (Button) rootView.findViewById(R.id.cancel_comments);
        Button okButton = (Button) rootView.findViewById(R.id.ok_comments);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send back comments user input and lat and long user inputs
                // if comments are not disabled: then via interface take the values when clicked on OK inside comments,
                // populate the location and saveLocation
                // could call saveLocation() if trip were global, but I would need to pass the location etc to dialog fragment

            }
        });

        return rootView;
    }


}
