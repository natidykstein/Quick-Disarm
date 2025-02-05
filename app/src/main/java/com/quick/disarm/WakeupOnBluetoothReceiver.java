package com.quick.disarm;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.quick.disarm.utils.PreferenceCache;

import java.util.Set;

@SuppressLint("MissingPermission")
public class WakeupOnBluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = WakeupOnBluetoothReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                Log.d(TAG, "Detected Bluetooth connected to device: " + device.getName() + "(" + device.getAddress() + ")");
            }

            if (device != null) {
                final Set<String> bluetoothSet =
                        PreferenceCache.get(context).getCarBluetoothSet();
                // Iterate through configured car bluetooth list
                final String connectedCarBluetoothMac =
                        getConnectedBluetoothMac(device.getAddress(), bluetoothSet);
                if (connectedCarBluetoothMac != null) {
                    // Offload disarming to intent service
                    final Intent serviceIntent = new Intent(context, DisarmService.class);
                    serviceIntent.putExtra(DisarmService.EXTRA_CAR_BLUETOOTH, connectedCarBluetoothMac);
                    DisarmService.enqueueWork(context, serviceIntent);
                }
            }
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                Log.d(TAG, "Detected Bluetooth disconnected from device: " + device.getName() + "(" + device.getAddress() + ")");
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.d(TAG, "Device booted, re-register the BluetoothReceiver if necessary");
            // Re-register the receiver if needed
        }
    }

    /**
     * Check if the connected device address is one of the configured cars bluetooth mac
     */
    private String getConnectedBluetoothMac(String connectedDeviceAddress, Set<String> configuredCarAddressList) {
        return configuredCarAddressList.stream()
                .filter(bluetoothMac -> bluetoothMac.equals(connectedDeviceAddress))
                .findFirst()
                .orElse(null);
    }
}
