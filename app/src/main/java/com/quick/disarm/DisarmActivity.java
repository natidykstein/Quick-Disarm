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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.quick.disarm.add.DetectCarBluetoothActivity;
import com.quick.disarm.infra.ILog;
import com.quick.disarm.utils.PreferenceCache;


/**
 * PENDING:
 *  3. Add menu item in DisarmActivity to manage car data
 *  5. Display list of currently configured cars (read-only)
 *  6. Add setup wizard that go through Ituran registration process to register device UUID and get starlink serial+mac
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

        mAddCarButton = findViewById(R.id.add_car_button);
        mAddCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent startDetectActivityIntent = new Intent(DisarmActivity.this, DetectCarBluetoothActivity.class);
                startActivity(startDetectActivityIntent);
            }
        });

        mDisarmButton = findViewById(R.id.disarm_button);
        mDisarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDisarmButton.setEnabled(false);
                mProgressBar.setVisibility(View.VISIBLE);
                mDisarmButton.setText("DISARMING...");
                StarlinkCommandDispatcher.get().dispatchDisarmCommand();
            }
        });

        mProgressBar = findViewById(R.id.progress_bar);

        if (hasRequiredPermissions()) {
            setDisarmStatus(DisarmStatus.READY_TO_CONNECT);
        } else {
            requestNeededPermissions();
        }

        // Check if we need to setup the cars data
        initCarsIfNeeded();
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

    /**
     * Keep car data hardcoded intentionally to prevent unauthorized reuse
     * of the APK by others
     * This method should be configured per APK distribution
     */
    private void initCarsIfNeeded() {
        if (PreferenceCache.get(this).getCarBluetoothList().isEmpty()) {
            ILog.d("No cars configured - adding cars...");

            final Car myXpengG9 = new Car("76579403", "D0:1F:DD:C2:37:2D", 2276181, "2233");
            PreferenceCache.get(this).putCar("A4:04:50:44:C1:0F", myXpengG9);
            ILog.d("Added " + myXpengG9);

            final Car fakeCar = new Car("12345678", "D0:1F:DD:C2:37:2D", 2276181, "1234");
            PreferenceCache.get(this).putCar("60:AB:D2:B2:95:AE", fakeCar);

            ILog.d("Added " + fakeCar);
        }
    }

    private void connectToDevice() {
        setDisarmStatus(DisarmStatus.CONNECTING_TO_DEVICE);

        // PENDING: In the activity we need to allow selecting the car to which we want to connect and disarm
        final String defaultCarBluetoothMac = PreferenceCache.get(this).getCarBluetoothList().get(0);
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
                Toast.makeText(this, "Failed to acquire needed permissions", Toast.LENGTH_SHORT).show();
                ILog.d("Failed to get required permissions - exiting...");
                finish();
            }
        }
    }

    private void setDisarmStatus(final DisarmStatus disarmStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (disarmStatus) {
                    case READY_TO_CONNECT:
                        mDisarmButton.setEnabled(true);
                        mProgressBar.setVisibility(View.GONE);
                        mDisarmButton.setText("Connect");
                        break;
                    case CONNECTING_TO_DEVICE:
                        mDisarmButton.setEnabled(false);
                        mProgressBar.setVisibility(View.VISIBLE);
                        mDisarmButton.setText("Connecting to device...");
                        break;
                    case DEVICE_CONNECTED:
                        mDisarmButton.setEnabled(false);
                        mProgressBar.setVisibility(View.VISIBLE);
                        mDisarmButton.setText("Discovering device...");
                        break;
                    case DEVICE_DISCOVERED:
                        mDisarmButton.setText("Reading random...");
                        break;
                    case RANDOM_READ_SUCCESSFULLY:
                        mDisarmButton.setEnabled(true);
                        mProgressBar.setVisibility(View.GONE);
                        mDisarmButton.setText("DISARM");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
