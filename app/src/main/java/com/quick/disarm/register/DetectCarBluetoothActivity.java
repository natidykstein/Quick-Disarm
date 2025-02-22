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

import com.quick.disarm.Analytics;
import com.quick.disarm.R;
import com.quick.disarm.infra.ILog;

import java.util.ArrayList;
import java.util.List;

public class DetectCarBluetoothActivity extends AppCompatActivity {

    private static final String XPENG_APP_BLUETOOTH_PREFIX = "XPWL";
    private static final String ITURAN_BLUETOOTH_PREFIX = "4X-";

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
                    final String deviceName = device.getName();
                    final String deviceAddress = device.getAddress();
                    if (!shouldExcludeDevice(deviceName)) {
                        bluetoothDevices.add(new BluetoothDeviceItem(deviceName, deviceAddress));
                        adapter.notifyItemInserted(bluetoothDevices.size() - 1);
                    } else {
                        ILog.d("Skipping excluded device: " + deviceName);
                    }
                } else {
                    ILog.e("Failed to get device information - got null instead");
                }
            }
        }

        // To prevent users mistakes -
        // we're excluding no-name devices, Xpeng's app bluetooth component and Ituran's device.
        private boolean shouldExcludeDevice(String deviceName) {
            return deviceName == null ||
                    deviceName.startsWith(XPENG_APP_BLUETOOTH_PREFIX) ||
                    deviceName.startsWith(ITURAN_BLUETOOTH_PREFIX);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_car_bluetooth);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        adapter = new BluetoothDeviceAdapter(bluetoothDevices, view -> {
            final BluetoothDeviceItem device = (BluetoothDeviceItem) view.getTag();
            Analytics.reportSelectButtonEvent("select_bluetooth", device.getName());

            final Intent startRegisterActivityIntent = new Intent(DetectCarBluetoothActivity.this, RegisterActivity.class);
            startRegisterActivityIntent.putExtra(RegisterActivity.EXTRA_BLUETOOTH_TRIGGER_NAME, device.getName());
            startRegisterActivityIntent.putExtra(RegisterActivity.EXTRA_BLUETOOTH_TRIGGER, device.getAddress());
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
