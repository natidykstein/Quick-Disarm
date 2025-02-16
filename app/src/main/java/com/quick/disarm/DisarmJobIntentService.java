package com.quick.disarm;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.utils.PreferenceCache;

import java.util.Objects;

/**
 * @noinspection deprecation
 */
public class DisarmJobIntentService extends JobIntentService implements DisarmStateListener {
    // PENDING: Temporarily set to '0' for increased visibility -
    private static final long TOLERABLE_DURATION_LIMIT = 0;//TimeUnit.SECONDS.toMillis(1);

    public static final String EXTRA_CAR_BLUETOOTH = "com.quick.disarm.extra.CAR_BLUETOOTH_MAC";
    public static final String EXTRA_START_TIME = "com.quick.disarm.extra.START_TIME";

    private static final int JOB_ID = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private PowerManager.WakeLock mWakeLock;
    private long mDisarmStartTime;

    public static void enqueueWork(Context context, String connectedCarBluetoothMac) {
        ILog.d("Starting disarm service...");
        final Intent serviceIntent = new Intent(context, DisarmJobIntentService.class);
        serviceIntent.putExtra(DisarmJobIntentService.EXTRA_CAR_BLUETOOTH, connectedCarBluetoothMac);
        serviceIntent.putExtra(DisarmJobIntentService.EXTRA_START_TIME, System.currentTimeMillis());
        enqueueWork(context, DisarmJobIntentService.class, JOB_ID, serviceIntent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        ILog.d("Starting onHandleWork...");

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
            final String carBluetoothMac = intent.getStringExtra(EXTRA_CAR_BLUETOOTH);
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
            mWakeLock.release();

            // Log as an exception for increased visibility
            if (duration > TOLERABLE_DURATION_LIMIT) {
                ILog.logException("Disarm device took longer than expected: " + duration + "ms");
            }
        }
    }
}
