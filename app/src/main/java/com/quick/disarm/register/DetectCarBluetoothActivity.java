package com.quick.disarm.register;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quick.disarm.R;
import com.quick.disarm.ReportAnalytics;
import com.quick.disarm.infra.ILog;

import java.util.ArrayList;
import java.util.List;

public class DetectCarBluetoothActivity extends AppCompatActivity {

    private static final String XPENG_BLUETOOTH_DEFAULT_NAME = "XPENGMotor";
    private static final String[] EXCLUDED_DEVICE_NAME_PREFIXES = new String[]{
            // Known XPENG car's internal bluetooth device name prefixes
            "XPWL-",
            "XPED-",
            // Ituran's bluetooth device name prefix
            "4X-"};

    private BluetoothDeviceAdapter adapter;
    private final List<BluetoothDeviceItem> bluetoothDevices = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    String deviceName = device.getName();
                    final String deviceAddress = device.getAddress();
                    if (!shouldExcludeDevice(deviceName)) {
                        if (deviceName.equals(XPENG_BLUETOOTH_DEFAULT_NAME)) {
                            deviceName += " " + context.getString(R.string.device_recommended);
                        }
                        final BluetoothDeviceItem deviceItem = new BluetoothDeviceItem(deviceName, deviceAddress);
                        if (!bluetoothDevices.contains(deviceItem)) {
                            bluetoothDevices.add(deviceItem);
                            adapter.notifyItemInserted(bluetoothDevices.size() - 1);
                        }
                    } else {
                        ILog.d("Skipping excluded device: " + deviceName);
                    }
                } else {
                    ILog.e("Failed to get device information - got null instead");
                }
            }
        }

        // Help users select the correct bluetooth device -
        // we're excluding no-name devices, Xpeng's internal bluetooth and Ituran's bluetooth.
        private boolean shouldExcludeDevice(String deviceName) {
            if (deviceName == null) {
                return true;
            }

            for (String excludedPrefix : EXCLUDED_DEVICE_NAME_PREFIXES) {
                if (deviceName.startsWith(excludedPrefix)) {
                    return true;
                }
            }

            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_car_bluetooth);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        adapter = new BluetoothDeviceAdapter(bluetoothDevices, view -> {
            final BluetoothDeviceItem device = (BluetoothDeviceItem) view.getTag();
            ReportAnalytics.reportSelectButtonEvent("select_bluetooth", device.getName());

            final Intent startRegisterActivityIntent = new Intent(DetectCarBluetoothActivity.this, RegisterActivity.class);
            startRegisterActivityIntent.putExtra(RegisterActivity.EXTRA_TRIGGER_BLUETOOTH_NAME, device.getName());
            startRegisterActivityIntent.putExtra(RegisterActivity.EXTRA_TRIGGER_BLUETOOTH_ADDRESS, device.getAddress());
            startActivity(startRegisterActivityIntent);
            finish();
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter);

        // Initialize Bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        startDiscovery();
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            ILog.d("Starting bluetooth discovery...");
            bluetoothAdapter.startDiscovery();
        } else {
            ILog.e("Bluetooth device not enabled");
        }
    }

    @SuppressLint("MissingPermission")
    private void cancelDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            ILog.d("Cancelling bluetooth discovery...");
            bluetoothAdapter.cancelDiscovery();
        } else {
            ILog.e("Bluetooth device not enabled");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelDiscovery();
        unregisterReceiver(bluetoothReceiver);
    }
}
