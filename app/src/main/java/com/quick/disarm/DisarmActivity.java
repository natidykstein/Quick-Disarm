package com.quick.disarm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ComponentCaller;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.infra.Utils;
import com.quick.disarm.register.DetectCarBluetoothActivity;
import com.quick.disarm.utils.PreferenceCache;

import java.util.Set;


/**
 * PENDING:
 *  5. Display list of currently configured cars (read-only) + selecting current car for manual disarm
 *  8. Allow deleting a car
 *  9. Add new google maps activity for tracking a car
 *  11. Allow user to select between 3 levels of authentication - App, device or none.
 *      App(most secured) - Each app open will require an authentication
 *      Device(normal/default) - The device must be unlocked to allow disarming
 *      None(less secured) - Car can be disarmed in the background while the device is locked
 *  12. Make manual disarming in one click (instead of 2) (reuse foreground service?)
 * <p>
 */
@SuppressLint("MissingPermission")
public class DisarmActivity extends AppCompatActivity implements DisarmStateListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSIONS_REQUEST = 1000;

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS};

    private BluetoothAdapter bluetoothAdapter;

    private Button mDisarmButton;
    private ProgressBar mProgressBar;

    private DisarmStatus mDisarmStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disarm);

        final Toolbar toolbar = findViewById(R.id.toolbar_disarm);
        setSupportActionBar(toolbar);

        if (!Utils.isDeviceSecure(this)) {
            Toast.makeText(this, R.string.device_must_be_protected, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Check if Bluetooth is supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported_on_device, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final TextView verionTextView = findViewById(R.id.textViewVersionName);
        verionTextView.setText(Utils.getApplicationVersionName(this));

        final Button addCarButton = findViewById(R.id.add_car_button);
        addCarButton.setOnClickListener(v -> {
            ReportAnalytics.reportSelectButtonEvent("add_car", "Add car");

            final Intent startDetectActivityIntent = new Intent(DisarmActivity.this, DetectCarBluetoothActivity.class);
            startActivity(startDetectActivityIntent);
        });

        mDisarmButton = findViewById(R.id.disarm_button);
        mDisarmButton.setOnClickListener(v -> {
            final Set<Car> carSet = PreferenceCache.get(DisarmActivity.this).getCarSet();
            if (!carSet.isEmpty()) {
                ReportAnalytics.reportSelectButtonEvent("disarm_button", "Disarm");
                // PENDING: Allow selecting the car to which we want to connect and disarm
                //  Currently we're taking the first car
                final Car car = carSet.iterator().next();
                ILog.d("Attempting to manually disarm car: " + car.toStringExtended());
                connectToDevice(car);
            } else {
                Toast.makeText(DisarmActivity.this, R.string.please_add_a_car_first, Toast.LENGTH_SHORT).show();
            }
        });

        mProgressBar = findViewById(R.id.progress_bar);

        handlePermissions();
    }

    private void handlePermissions() {
        if (!hasRequiredPermissionsAndBluetoothEnabled()) {
            askForRequiredPermissions();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Refresh when getting back to the activity
        final Set<Car> carSet = PreferenceCache.get(this).getCarSet();
        final String carsSummaryText;
        if (!carSet.isEmpty()) {
            final StringBuilder carPlates = new StringBuilder();
            // Add all cars license plates
            for (Car car : carSet) {
                carPlates.append(car.getFormattedLicensePlate()).append(", ");
            }
            // Remove trailing comma
            carsSummaryText = getString(R.string.added_cars, carPlates.substring(0, carPlates.length() - 2));
        } else {
            carsSummaryText = getString(R.string.no_cars_added_yet);
        }

        this.<TextView>findViewById(R.id.editTextDataSummary).setText(carsSummaryText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void askForRequiredPermissions() {
        ILog.d("Missing required permissions - asking now...");
        ActivityCompat.requestPermissions(DisarmActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST);
    }

    /**
     * Method verifies that bluetooth is enabled and app has all required permissions.
     * If bluetooth not enabled an 'enable bluetooth' dialog will displayed to the user
     */
    private boolean hasRequiredPermissionsAndBluetoothEnabled() {
        // We check permissions first and bluetooth enabled later since we can't display
        // the 'enable bluetooth' popup without BLUETOOTH_CONNECT permission
        if (hasRequiredPermissions()) {
            if (bluetoothAdapter.isEnabled()) {
                return true;
            } else {
                ILog.w("Bluetooth not enabled - showing enable bluetooth popup to user");
                Toast.makeText(DisarmActivity.this, R.string.bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
                final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean hasRequiredPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(DisarmActivity.this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void connectToDevice(Car car) {
        setDisarmStatus(DisarmStatus.READY_TO_CONNECT, DisarmStatus.CONNECTING_TO_DEVICE);
        final BluetoothDevice device = getStarlinkDevice(car.getStarlinkMac());
        device.connectGatt(this, false, new StartLinkGattCallback(this, car), BluetoothDevice.TRANSPORT_LE);
    }

    private BluetoothDevice getStarlinkDevice(String starlinkMac) {
        return Build.VERSION.SDK_INT >= 33 ? bluetoothAdapter.getRemoteLeDevice(starlinkMac, BluetoothDevice.ADDRESS_TYPE_PUBLIC) : bluetoothAdapter.getRemoteDevice(starlinkMac);
    }

    @Override
    public void onDisarmStatusChange(DisarmStatus currentState, DisarmStatus newState) {
        setDisarmStatus(currentState, newState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length == REQUIRED_PERMISSIONS.length &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                ILog.d("Got required permissions");
                if (hasRequiredPermissionsAndBluetoothEnabled()) {
                    setDisarmStatus(DisarmStatus.READY_TO_CONNECT, DisarmStatus.READY_TO_CONNECT);
                }
            } else {
                final String errorMessage = getString(R.string.failed_to_acquire_missing_permissions);
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                ILog.d(errorMessage + " - exiting...");

                // Open the App's permissions settings to allow the user to manually add the permissions
                final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);

                finish();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data, @NonNull ComponentCaller caller) {
        super.onActivityResult(requestCode, resultCode, data, caller);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                ILog.d("Bluetooth is enabled - re-verifying permissions...");
                handlePermissions();
            } else {
                ILog.w("Bluetooth not enabled - exiting app");
                Toast.makeText(this, R.string.bluetooth_must_be_enabled, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void setDisarmStatus(final DisarmStatus currentStatus, final DisarmStatus newStatus) {
        mDisarmStatus = newStatus;

        runOnUiThread(() -> {
            switch (newStatus) {
                case READY_TO_CONNECT:
                    if (currentStatus != DisarmStatus.RANDOM_READ_SUCCESSFULLY) {
                        ILog.e("Failed to disarm device");
                        Toast.makeText(this, R.string.failed_to_disarm_device, Toast.LENGTH_SHORT).show();
                    }
                    mDisarmButton.setEnabled(true);
                    mProgressBar.setVisibility(View.GONE);
                    mDisarmButton.setText(R.string.disarm);
                    break;
                case CONNECTING_TO_DEVICE:
                    mProgressBar.setVisibility(View.VISIBLE);
                    mDisarmButton.setText(R.string.connecting_to_device);
                    break;
                case DEVICE_CONNECTED:
                    mDisarmButton.setText(R.string.discovering_device);
                    break;
                case DEVICE_DISCOVERED:
                    mDisarmButton.setText(R.string.reading_random);
                    break;
                case RANDOM_READ_SUCCESSFULLY:
                    mDisarmButton.setText(R.string.disarming);
                    break;
            }
        });
    }
}
