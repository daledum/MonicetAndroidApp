package net.monicet.monicet;

import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import static android.R.string.no;

public class TurnGpsOnActivity extends AppCompatActivity {

    private final int TURN_GPS_ON_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_turn_gps_on);

        showTurnOnGpsProviderDialog();
    }

    private void showTurnOnGpsProviderDialog() {
        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(this);

        comAlertDialogBuilder.setTitle(R.string.turn_on_gps_dialog_title);
        comAlertDialogBuilder.setMessage(R.string.turn_on_gps_dialog_message);

        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                TurnGpsOnActivity.this.startActivityForResult(intent, TURN_GPS_ON_REQUEST_CODE);
            }
        });
        comAlertDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        //comAlertDialogBuilder.create();//TODO: redundant get rid
        comAlertDialogBuilder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);

        // result is irrelevant - using this to go back to MainActivity (if GPS is still off, this will rerun)
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
