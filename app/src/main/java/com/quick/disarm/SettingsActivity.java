package com.quick.disarm;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.utils.PreferenceCache;

public class SettingsActivity extends AppCompatActivity {

    private RadioButton mDeviceRadioButton;

    private boolean mSkipAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final RadioButton appRadioButton = findViewById(R.id.radioButtonApp);
        mDeviceRadioButton = findViewById(R.id.radioButtonDevice);
        final RadioButton noneRadioButton = findViewById(R.id.radioButtonNone);

        final AuthLevel currentAuthLevel = PreferenceCache.get(this).getAuthenticationLevel();
        switch (currentAuthLevel) {
            case APP:
                appRadioButton.setChecked(true);
                break;
            case NONE:
                noneRadioButton.setChecked(true);
                break;
            default:
                mDeviceRadioButton.setChecked(true);
        }

        final RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (mSkipAlert) {
                return;
            }
            final RadioButton selectedButton = findViewById(checkedId);
            // String values are taken from the radio buttons XML 'tag' attribute
            switch (selectedButton.getTag().toString()) {
                case "app":
                    showDescription(selectedButton.getText().toString(), "Each disarm will require an authentication. This is more secured than the original app but less convenient");
                    PreferenceCache.get(this).setAuthenticationLevel(AuthLevel.APP);
                    break;
                case "device":
                    showDescription(selectedButton.getText().toString(), "The device must be unlocked to allow disarming. This will display a notification when connected to the car that disarms Ituran when tapped. This has the same security level as the original app but more convenient.");
                    PreferenceCache.get(this).setAuthenticationLevel(AuthLevel.DEVICE);
                    break;
                case "none":
                    showConfirmationDialog(selectedButton.getText().toString());
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected radio button tag found:" + selectedButton.getTag());
            }

            ILog.d("Authentication level selected: " + selectedButton.getText());
        });
    }

    private void showDescription(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showConfirmationDialog(String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Car can be disarmed in the background while the device is locked. This is the less secured option but the most comfortable one.\nI understand that this option allows anyone who's using my device to disarm my car's Ituran automatically and I use it on my own risk since I love living the easy life!")
                .setPositiveButton("I understand", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ILog.d("User accepted disclaimer for 'None authentication'");
                        PreferenceCache.get(SettingsActivity.this).setAuthenticationLevel(AuthLevel.NONE);
                    }
                })
                .setNegativeButton("Forget it", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ILog.d("User did not agree to disclaimer - reverting 'No authentication' selection to 'Device authentication'...");
                        mSkipAlert = true;
                        mDeviceRadioButton.setChecked(true);
                        mSkipAlert = false;
                    }
                }).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}
