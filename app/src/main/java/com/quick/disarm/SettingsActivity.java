package com.quick.disarm;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.utils.PreferenceCache;

public class SettingsActivity extends AppCompatActivity {

    private RadioButton mDeviceRadioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDeviceRadioButton = findViewById(R.id.radioButtonDevice);
        final RadioButton noneRadioButton = findViewById(R.id.radioButtonNone);

        final boolean allowBackgroundDisarm = PreferenceCache.get(this).isAllowBackgroundDisarm();
        if (allowBackgroundDisarm) {
            noneRadioButton.setChecked(true);
        } else {
            mDeviceRadioButton.setChecked(true);
        }

        // Allow showing the description dialog when clicking on an already selected radio button
        mDeviceRadioButton.setOnClickListener(v -> {
            showDescription(mDeviceRadioButton.getText().toString());
            PreferenceCache.get(this).setAllowBackgroundDisarm(false);
            ILog.d("Authentication selected: " + mDeviceRadioButton.getText());
        });

        noneRadioButton.setOnClickListener(v -> {
            showConfirmationDialog(noneRadioButton.getText().toString());
        });
    }

    private void showDescription(String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Ituran will be disarmed by tapping a notification.\nWhen connected to the car simply tap the displayed notification to quickly disarm Ituran")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showConfirmationDialog(String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Ituran will be disarmed automatically in the background while device is locked.\nI understand that this option allows anyone who's using my device to disarm my car's Ituran automatically and I use it on my own risk since I love living the easy life!")
                .setPositiveButton("I understand", (dialog, which) -> {
                    ILog.d("User accepted disclaimer, selecting 'None' authentication");
                    PreferenceCache.get(SettingsActivity.this).setAllowBackgroundDisarm(true);
                })
                .setNegativeButton("Forget it", (dialog, which) -> {
                    ILog.d("User did not accept disclaimer - reverting to 'Device' authentication");
                    mDeviceRadioButton.setChecked(true);
                    PreferenceCache.get(SettingsActivity.this).setAllowBackgroundDisarm(false);
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
