package net.monicet.monicet;

import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class GetAccountActivity extends AppCompatActivity {

    private final int PICK_ACCOUNT_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_account);

        Toast.makeText(
                this,
                R.string.email_account,
                Toast.LENGTH_SHORT
        ).show();

        // get user name (user's email address) and set it inside onActivityResult
        Intent getAccountsIntent = AccountManager.get(this).newChooseAccountIntent(
                null, null, null, false, null, null, null, null
        );
        startActivityForResult(getAccountsIntent, PICK_ACCOUNT_REQUEST);// use const I lose the data because the activity finishes

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to

        if (requestCode == PICK_ACCOUNT_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                //this might return null
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null) {
                    if (accountName.contains("@")) {
                        Utils.writeAccountNameToSharedPrefs(this, accountName);
                    }
                }
            }
        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
