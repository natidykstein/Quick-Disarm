package com.quick.disarm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
 *  10. Show toast when performing disarm (allow setting this by the user to on/off)
 * <p>
 * Add 'Add Car' button [Deferred since it will allow reuse of the app without permission]
 */
@SuppressLint("MissingPermission")
public class DisarmActivity extends AppCompatActivity implements DisarmStateListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSIONS_REQUEST = 1000;

    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};


    private BluetoothAdapter bluetoothAdapter;

    private Button mAddCarButton;
    private Button mDisarmButton;
    private ProgressBar mProgressBar;
    private TextView mDataSummaryEditText;

    private DisarmStatus mDisarmStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        // Initialize Bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Check if Bluetooth is supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final TextView verionTextView = findViewById(R.id.textViewVersionName);
        verionTextView.setText(Utils.getApplicationVersionName(this));

        mAddCarButton = findViewById(R.id.add_car_button);
        mAddCarButton.setOnClickListener(v -> {
            final Intent startDetectActivityIntent = new Intent(DisarmActivity.this, DetectCarBluetoothActivity.class);
            startActivity(startDetectActivityIntent);
        });

        mDisarmButton = findViewById(R.id.disarm_button);
        mDisarmButton.setOnClickListener(v -> {
            if (!getConfiguredCars().isEmpty()) {
                // PENDING: Act upon status
                if (mDisarmStatus == DisarmStatus.READY_TO_CONNECT) {
                    connectToDevice();
                } else {
                    StarlinkCommandDispatcher.get().dispatchDisarmCommand();
                }
            } else {
                Toast.makeText(DisarmActivity.this, "Please add a car first", Toast.LENGTH_SHORT).show();
            }
        });

        mProgressBar = findViewById(R.id.progress_bar);

        if (hasRequiredPermissions()) {
            setDisarmStatus(DisarmStatus.READY_TO_CONNECT);
        } else {
            requestNeededPermissions();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDataSummaryEditText = findViewById(R.id.editTextDataSummary);
        mDataSummaryEditText.setText(getString(R.string.number_of_configured_cars, getConfiguredCars().size()));
    }

    private Set<String> getConfiguredCars() {
        return PreferenceCache.get(this).getCarBluetoothSet();
    }

    private void requestNeededPermissions() {
        ILog.d("Missing required permissions - asking now...");
        ActivityCompat.requestPermissions(DisarmActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST);
    }

    private boolean hasRequiredPermissions() {
        if (!bluetoothAdapter.isEnabled()) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        } else {
            return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && hasPermission(Manifest.permission.BLUETOOTH_SCAN) && hasPermission(Manifest.permission.BLUETOOTH_CONNECT);
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(DisarmActivity.this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void connectToDevice() {
        setDisarmStatus(DisarmStatus.CONNECTING_TO_DEVICE);

        // PENDING: In the activity we need to allow selecting the car to which we want to connect and disarm
        final String defaultCarBluetoothMac = PreferenceCache.get(this).getCarBluetoothSet().iterator().next();
        final Car connectedCar = PreferenceCache.get(this).getCar(defaultCarBluetoothMac);
        final BluetoothDevice device = getStarlinkDevice(connectedCar.getStarlinkMac());
        device.connectGatt(this, false, new StartLinkGattCallback(this, connectedCar), BluetoothDevice.TRANSPORT_LE);
    }

    private BluetoothDevice getStarlinkDevice(String starlinkMac) {
        return Build.VERSION.SDK_INT >= 33 ? bluetoothAdapter.getRemoteLeDevice(starlinkMac, BluetoothDevice.ADDRESS_TYPE_PUBLIC) : bluetoothAdapter.getRemoteDevice(starlinkMac);
    }

    @Override
    public void onDisarmStatusChange(DisarmStatus currentState, DisarmStatus newState) {
        setDisarmStatus(newState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                ILog.d("Got required permissions");
                setDisarmStatus(DisarmStatus.READY_TO_CONNECT);
            } else {
                final String errorMessage = getString(R.string.failed_to_acquire_missing_permissions);
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                ILog.d(errorMessage + " - exiting...");
                finish();
            }
        }
    }

    private void setDisarmStatus(final DisarmStatus disarmStatus) {
        mDisarmStatus = disarmStatus;
        runOnUiThread(() -> {
            switch (disarmStatus) {
                case READY_TO_CONNECT:
                    mDisarmButton.setEnabled(true);
                    mProgressBar.setVisibility(View.GONE);
                    mDisarmButton.setText(R.string.connect);
                    break;
                case CONNECTING_TO_DEVICE:
                    mDisarmButton.setEnabled(false);
                    mProgressBar.setVisibility(View.VISIBLE);
                    mDisarmButton.setText(R.string.connecting_to_device);
                    break;
                case DEVICE_CONNECTED:
                    mDisarmButton.setEnabled(false);
                    mProgressBar.setVisibility(View.VISIBLE);
                    mDisarmButton.setText(R.string.discovering_device);
                    break;
                case DEVICE_DISCOVERED:
                    mDisarmButton.setText(R.string.reading_random);
                    break;
                case RANDOM_READ_SUCCESSFULLY:
                    mDisarmButton.setEnabled(true);
                    mProgressBar.setVisibility(View.GONE);
                    mDisarmButton.setText(R.string.disarm);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
