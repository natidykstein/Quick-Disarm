package com.quick.disarm.add;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quick.disarm.R;
import com.quick.disarm.infra.ILog;
import com.quick.disarm.register.RegisterActivity;

import java.util.ArrayList;
import java.util.List;

public class DetectCarBluetoothActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BluetoothDeviceAdapter adapter;
    private List<BluetoothDeviceItem> bluetoothDevices = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device!=null) {
                    bluetoothDevices.add(new BluetoothDeviceItem(device.getName(), device.getAddress()));
                    adapter.notifyItemInserted(bluetoothDevices.size() - 1);
                } else {
                    ILog.e("Failed to get device information - got null instead");
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_car_bluetooth);

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new BluetoothDeviceAdapter(bluetoothDevices, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final BluetoothDeviceItem device = (BluetoothDeviceItem) view.getTag();
                showDeviceDetails(device);
                final Intent startRegisterActivityIntent = new Intent(DetectCarBluetoothActivity.this, RegisterActivity.class);
                startRegisterActivityIntent.putExtra(RegisterActivity.EXTRA_CAR_BLUETOOTH, device.getAddress());
                startActivity(startRegisterActivityIntent);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter);

        // Initialize Bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.startDiscovery();
        } else {
            // Handle Bluetooth not being enabled
        }
    }

    private void showDeviceDetails(BluetoothDeviceItem device) {
        new AlertDialog.Builder(this)
                .setTitle(device.getName())
                .setMessage("Address: " + device.getAddress())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
    }
}
