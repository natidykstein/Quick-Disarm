package com.quick.disarm;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckedTextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.utils.PreferenceCache;

public class SettingsActivity extends AppCompatActivity {

    private CheckedTextView mAutoDisarmEnabledCheckedTextView;
    private boolean mAutoDisarmEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAutoDisarmEnabledCheckedTextView = findViewById(R.id.checkedTextViewAllowAutoDisarm);

        mAutoDisarmEnabled = PreferenceCache.get(this).isAutoDisarmEnabled();
        mAutoDisarmEnabledCheckedTextView.setChecked(mAutoDisarmEnabled);

        // Allow showing the description dialog when clicking on an already selected radio button
        mAutoDisarmEnabledCheckedTextView.setOnClickListener(v -> {
            if(mAutoDisarmEnabled) {
                showManualDisarmDescription();
                updateAutoDisarmEnabled(false);
            } else {
                showAutoDisarmConfirmationDialog();
            }
        });
    }

    private void updateAutoDisarmEnabled(boolean autoDisarmEnabled) {
        ILog.d("Disarm mode: " + (autoDisarmEnabled ? "Don't " : "") + "allow in background");
        mAutoDisarmEnabled = autoDisarmEnabled;
        mAutoDisarmEnabledCheckedTextView.setChecked(mAutoDisarmEnabled);
        PreferenceCache.get(this).setAutoDisarmEnabled(autoDisarmEnabled);
    }

    private void showManualDisarmDescription() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.manual_disarm_title)
                .setMessage(R.string.manual_disarm_message)
                .setPositiveButton(R.string.manual_disarm_ok_button, null)
                .show();
    }

    private void showAutoDisarmConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.auto_disarm_disclaimer_title)
                .setMessage(R.string.auto_disarm_disclaimer)
                .setPositiveButton(R.string.auto_disarm_disclaimer_ok_button, (dialog, which) -> {
                    ILog.d("User accepted auto disarm disclaimer");
                    updateAutoDisarmEnabled(true);
                })
                .setNegativeButton(R.string.background_disarm_disclaimer_cancel_button, (dialog, which) -> {
                    ILog.d("User did not accept auto disarm disclaimer");
                    updateAutoDisarmEnabled(false);
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
