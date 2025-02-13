package com.quick.disarm;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.utils.PreferenceCache;

import java.util.Set;

@SuppressLint("MissingPermission")
public class WakeupOnBluetoothReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                ILog.d("Detected Bluetooth connected to device: " + device.getName() + "(" + device.getAddress() + ")");

                // PENDING: TESTING!
                startAuthenticationActivity(context, "TESTING");

                final Set<String> bluetoothSet =
                        PreferenceCache.get(context).getCarBluetoothSet();
                // Iterate through configured car bluetooth list
                final String connectedCarBluetoothMac =
                        getConnectedBluetoothMac(device.getAddress(), bluetoothSet);
                if (connectedCarBluetoothMac != null) {
                    ILog.d("Connected to car's configured bluetooth, starting disarm service...");

                    // Offload disarming to intent service
                    // PENDING: Disabling temporarility for testing!
                    // PENDING: At this point we can check for the 'advnaced' setting and
                    //  skip the authentication before disarming
                    startDisarmService(context, connectedCarBluetoothMac);
                }
            } else {
                ILog.e("Got null device");
            }
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                ILog.d("Detected Bluetooth disconnected from device: " + device.getName() + "(" + device.getAddress() + ")");
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            ILog.d("Device booted");
        }
    }

    private void startDisarmService(Context context, String connectedCarBluetoothMac) {
        final Intent serviceIntent = new Intent(context, DisarmJobIntentService.class);
        serviceIntent.putExtra(DisarmJobIntentService.EXTRA_CAR_BLUETOOTH, connectedCarBluetoothMac);
        serviceIntent.putExtra(DisarmJobIntentService.EXTRA_START_TIME, System.currentTimeMillis());
        DisarmJobIntentService.enqueueWork(context, serviceIntent);
    }

    private void startAuthenticationActivity(Context context, String connectedCarBluetoothMac) {
        final Intent startAuthActivityIntent = new Intent(context, DisarmJobIntentService.class);
        startAuthActivityIntent.putExtra(DisarmJobIntentService.EXTRA_CAR_BLUETOOTH, connectedCarBluetoothMac);
        startAuthActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startAuthActivityIntent);
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
