package com.quick.disarm;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.utils.PreferenceCache;

import java.util.Set;

@SuppressLint("MissingPermission")
public class WakeupOnBluetoothReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "QuickDisarmChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                ILog.d("Connected to " + getLoggedString(device));

                // TESTING
                // showAuthenticationRequiredNotification(context, device.getAddress());

                final Set<String> bluetoothSet =
                        PreferenceCache.get(context).getCarBluetoothSet();
                // Iterate through configured car bluetooth list
                final String connectedCarBluetoothMac =
                        getConnectedBluetoothMac(device.getAddress(), bluetoothSet);
                if (connectedCarBluetoothMac != null) {
                    ILog.d("Connected to car's configured bluetooth");

                    boolean autoDisarmEnabled = PreferenceCache.get(context).isAutoDisarmEnabled();
                    if (autoDisarmEnabled) {
                        ILog.d("Auto disarm enabled - starting disarm service in the background");
                        // Offload disarming to intent service
                        DisarmJobIntentService.enqueueWork(context, connectedCarBluetoothMac);
                    } else {
                        ILog.d("Auto disarm disabled - showing disarm notification");
                        // Show notification before starting to disarm
                        showAuthenticationRequiredNotification(context, connectedCarBluetoothMac);
                    }
                } else {
                    ILog.d("Device is not a known car bluetooth: " + getLoggedString(device));
                }
            } else {
                ILog.e("Got null device from intent extra");
            }
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                ILog.d("Detected bluetooth disconnected from " + getLoggedString(device));
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            ILog.d("Device booted");
        }
    }

    private String getLoggedString(BluetoothDevice device) {
        final String name = device.getName();
        final String address = device.getAddress();
        return (name != null ? name : "NO_NAME") + " [" + address + "]";
    }

    private void showAuthenticationRequiredNotification(Context context, String connectedCarBluetoothMac) {
        ILog.d("Showing authentication notification...");
        createNotificationChannel(context);

        final PendingIntent pendingIntent = getPendingIntent(context, connectedCarBluetoothMac);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_small_notification)
                .setContentTitle("Welcome to your Xpeng")
                .setContentText("Tap to disarm Ituran")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private PendingIntent getPendingIntent(Context context, String connectedCarBluetoothMac) {
        final Intent startJobIntentReceiver = new Intent(context, JobIntentReceiver.class);
        startJobIntentReceiver.putExtra(DisarmJobIntentService.EXTRA_CAR_BLUETOOTH, connectedCarBluetoothMac);
        startJobIntentReceiver.putExtra(DisarmJobIntentService.EXTRA_START_TIME, System.currentTimeMillis());
        return PendingIntent.getBroadcast(context, 0, startJobIntentReceiver, PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannel(Context context) {
        final String name = "QuickDisarm Channel";
        final String description = "Channel for QuickDisarm notifications";
        final int importance = NotificationManager.IMPORTANCE_HIGH;
        final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Check if the connected device address is one of the configured cars bluetooth mac
     */
    private String getConnectedBluetoothMac(String connectedDeviceAddress, Set<String> configuredCarAddressList) {
        ILog.d("Checking if connected to car's bluetooth...");
        return configuredCarAddressList.stream()
                .filter(bluetoothMac -> bluetoothMac.equals(connectedDeviceAddress))
                .findFirst()
                .orElse(null);
    }
}
