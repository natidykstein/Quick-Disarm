package com.quick.disarm;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.utils.PreferenceCache;

import java.util.Objects;

/**
 * @noinspection deprecation
 */
public class DisarmForegroundService extends IntentService implements DisarmStateListener {
    private static final long TOLERABLE_DURATION_LIMIT = 0; // TimeUnit.SECONDS.toMillis(1);

    public static final String EXTRA_CAR_BLUETOOTH = "com.quick.disarm.extra.CAR_BLUETOOTH_MAC";
    public static final String EXTRA_START_TIME = "com.quick.disarm.extra.START_TIME";
    private static final int NOTIFICATION_ID = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private PowerManager.WakeLock mWakeLock;
    private long mDisarmStartTime;

    public DisarmForegroundService() {
        super("DisarmForegroundService");
    }

    public static void startService(Context context, String connectedCarBluetoothMac) {
        ILog.d("Starting disarm foreground service...");
        Intent serviceIntent = new Intent(context, DisarmForegroundService.class);
        serviceIntent.putExtra(DisarmForegroundService.EXTRA_CAR_BLUETOOTH, connectedCarBluetoothMac);
        serviceIntent.putExtra(DisarmForegroundService.EXTRA_START_TIME, System.currentTimeMillis());
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    @Override
    protected void onHandleIntent(@NonNull Intent intent) {
        ILog.d("Starting onHandleIntent...");

        // Create the notification channel if needed
        WakeupOnBluetoothReceiver.createNotificationChannel(this);

        // Create the notification
        final Notification notification = new NotificationCompat.Builder(this, WakeupOnBluetoothReceiver.CHANNEL_ID)
                .setContentTitle(getString(R.string.disarming_in_progress_notification_title))
                .setContentText(getString(R.string.disarming_in_progress_notification_message))
                .setSmallIcon(R.drawable.ic_small_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        // Start the foreground service with the notification
        startForeground(NOTIFICATION_ID, notification);

        // Acquire a wake lock to keep the CPU running
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuickDisarm::WakeLock");

        // We assume the work will be done much quicker but just
        // to be on the safe side we limit it to 10 seconds
        mWakeLock.acquire(10_000);

        // Get the start time as measured by the bluetooth broadcast receiver
        mDisarmStartTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis());

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter != null) {
            String carBluetoothMac = intent.getStringExtra(EXTRA_CAR_BLUETOOTH);
            final Car connectedCar = PreferenceCache.get(this).getCar(carBluetoothMac);
            if (connectedCar != null) {
                ILog.d("Connecting to " + connectedCar + "'s Ituran...");
                connectToDevice(connectedCar);
            } else {
                ILog.logException("No car found for bluetooth device with address [" + carBluetoothMac + "]");
                mWakeLock.release();
            }
        } else {
            ILog.logException("Bluetooth is not supported on this device");
            mWakeLock.release();
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(Car connectedCar) {
        final StartLinkGattCallback bluetoothGattCallback = new StartLinkGattCallback(this, connectedCar);
        final BluetoothDevice device = getStarlinkDevice(connectedCar.getStarlinkMac());
        device.connectGatt(this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private BluetoothDevice getStarlinkDevice(String starlinkMac) {
        return Build.VERSION.SDK_INT >= 33 ? mBluetoothAdapter.getRemoteLeDevice(starlinkMac, BluetoothDevice.ADDRESS_TYPE_PUBLIC) : mBluetoothAdapter.getRemoteDevice(starlinkMac);
    }

    @Override
    public void onDisarmStatusChange(DisarmStatus currentState, DisarmStatus newState) {
        if (Objects.requireNonNull(newState, "DisarmStatus new state must not be null") == DisarmStatus.RANDOM_READ_SUCCESSFULLY) {
            ILog.d("Attempting to disarm...");
            StarlinkCommandDispatcher.get().dispatchDisarmCommand();
        }
        if (currentState == DisarmStatus.CONNECTING_TO_DEVICE && newState == DisarmStatus.READY_TO_CONNECT) {
            ILog.e("Failed to connect to device");
        }
        if (newState == DisarmStatus.DISARMED) {
            final long duration = System.currentTimeMillis() - mDisarmStartTime;
            final String logMessage = "Successfully disarmed device in " + duration + "ms";
            ILog.d(logMessage);
            Analytics.reportEvent("disarm_success", "duration", String.valueOf(duration));

            // Log as an exception for increased visibility
            if (duration > TOLERABLE_DURATION_LIMIT) {
                ILog.logException("Disarm device took longer than expected: " + duration + "ms");
            }

            mWakeLock.release();
            stopForeground(true);
            stopSelf();
        }
    }
}
