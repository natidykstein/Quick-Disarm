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

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.infra.Utils;

/**
 * @noinspection deprecation
 */
public class DisarmForegroundService extends IntentService implements DisarmStateListener {
    private static final long MAX_RETRIES = 3;
    private static final long TOLERABLE_DURATION_LIMIT = 0; // TimeUnit.SECONDS.toMillis(1);
    private static final int NOTIFICATION_ID = 1;


    public static final String EXTRA_CONNECTED_CAR = "com.quick.disarm.extra.CONNECTED_CAR";
    public static final String EXTRA_START_TIME = "com.quick.disarm.extra.START_TIME";
    public static final String EXTRA_NOTIFICATION_DISPLAY_TIME = "com.quick.disarm.extra.NOTIFICATION_DISPLAY_TIME";

    private BluetoothAdapter mBluetoothAdapter;
    private PowerManager.WakeLock mWakeLock;
    private long mDisarmStartTime;
    private Car mConnectedCar;
    private int mAttemptNumber;

    public DisarmForegroundService() {
        super("DisarmForegroundService");
    }

    public static void startService(Context context, Car connectedCar) {
        ILog.d("Starting disarm foreground service...");
        Intent serviceIntent = new Intent(context, DisarmForegroundService.class);
        serviceIntent.putExtra(DisarmForegroundService.EXTRA_CONNECTED_CAR, connectedCar);
        serviceIntent.putExtra(DisarmForegroundService.EXTRA_START_TIME, System.currentTimeMillis());
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ILog.d("Starting onHandleIntent...");

        if(intent==null) {
            ILog.e("Got null intent - ignoring");
            return;
        }

        // Create the notification channel if needed
        WakeupOnBluetoothReceiver.createNotificationChannel(this);

        // Create the notification
        final Notification notification = new NotificationCompat.Builder(this, WakeupOnBluetoothReceiver.CHANNEL_ID)
                .setContentTitle(getString(R.string.disarming_in_progress_notification_title))
                .setSmallIcon(R.drawable.ic_small_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // PENDING: Might need to create a separate channel here to prevent sound
                .build();

        // Start the foreground service with the notification
        startForeground(NOTIFICATION_ID, notification);

        // Acquire a wake lock to keep the CPU running
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuickDisarm::WakeLock");

        // We assume the work will be done much quicker but just
        // to be on the safe side we limit it to 10 seconds
        mWakeLock.acquire(10_000);

        if(intent.hasExtra(EXTRA_START_TIME)) {
            // Get the start time as measured by the bluetooth broadcast receiver
            mDisarmStartTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis());
        } else {
            final long notificationDisplayTime = intent.getLongExtra(EXTRA_NOTIFICATION_DISPLAY_TIME, 0);
            ILog.d("User tapped disarm notification that was displayed " + Utils.formatDuration(System.currentTimeMillis() - notificationDisplayTime) + " ago");
            mDisarmStartTime = System.currentTimeMillis();
        }

        mConnectedCar = intent != null ? Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                intent.getSerializableExtra(EXTRA_CONNECTED_CAR, Car.class) :
                (Car) intent.getSerializableExtra(EXTRA_CONNECTED_CAR) : null;

        if (mConnectedCar != null) {
            attemptToDisarm();
        } else {
            ILog.logException(new RuntimeException("Failed to retrieve car from intent"));
            stopService();
        }
    }

    private void attemptToDisarm() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter != null) {
            connectToDevice();
        } else {
            ILog.logException(new RuntimeException("Bluetooth is not supported on this device"));
            stopService();
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice() {
        if (++mAttemptNumber <= MAX_RETRIES) {
            ILog.d("Attempting to disarm device for " + mConnectedCar + "...(attempt=" + mAttemptNumber + ")");
            final StartLinkGattCallback bluetoothGattCallback = new StartLinkGattCallback(this, mConnectedCar);
            final BluetoothDevice device = getStarlinkDevice(mConnectedCar.getStarlinkMac());
            device.connectGatt(this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            ILog.logException(new RuntimeException("Exceeded number of max attempts to disarm device (" + MAX_RETRIES + ")"));
            stopService();
        }
    }

    private BluetoothDevice getStarlinkDevice(String starlinkMac) {
        return Build.VERSION.SDK_INT >= 33 ? mBluetoothAdapter.getRemoteLeDevice(starlinkMac, BluetoothDevice.ADDRESS_TYPE_PUBLIC) : mBluetoothAdapter.getRemoteDevice(starlinkMac);
    }

    @Override
    public void onDisarmStatusChange(DisarmStatus currentState, DisarmStatus newState) {
        if (newState == DisarmStatus.READY_TO_CONNECT) {
            String errorMessage = "Unknown error - retrying...";
            switch (currentState) {
                case CONNECTING_TO_DEVICE:
                    // PENDING: Do we really want to retry when failing to connect?
                    errorMessage = "Failed to connect to bluetooth gatt device - retrying...";
                    break;
                case DEVICE_CONNECTED:
                    errorMessage = "Failed to discover device services - retrying...";
                    break;
                case DEVICE_DISCOVERED:
                    errorMessage = "Failed to read random from device - retrying...";
                    break;
                case RANDOM_READ_SUCCESSFULLY:
                    errorMessage = "Failed to disarm device - retrying...";
                    break;
            }

            ILog.e(errorMessage);

            // Retry
            connectToDevice();
        }

        // Disarm successful
        if (newState == DisarmStatus.DISARMED) {
            final long duration = System.currentTimeMillis() - mDisarmStartTime;
            ILog.d("Successfully disarmed device in " + duration + "ms");
            ReportAnalytics.reportEventWithMetric(
                    AnalyticsConstants.CUSTOM_DIMENSION_EVENT_DISARM_SUCCESS,
                    AnalyticsConstants.CUSTOM_DIMENSION_METRIC, duration);

            // Log as an exception for increased visibility
            if (duration > TOLERABLE_DURATION_LIMIT) {
                ILog.logException(new RuntimeException("Disarm device took longer than expected: " + duration + "ms on attempt " + mAttemptNumber));
            }

            stopService();
        }
    }

    private void stopService() {
        ILog.d("Stopping service...");
        mWakeLock.release();
        stopForeground(true);
        stopSelf();
    }
}
