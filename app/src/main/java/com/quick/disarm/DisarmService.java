package com.quick.disarm;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.quick.disarm.utils.PreferenceCache;

/**
 * @noinspection deprecation
 */
public class DisarmService extends JobIntentService implements DisarmStateListener {
    private static final String TAG = "BluetoothService";

    public static final String EXTRA_CAR_BLUETOOTH = "com.quick.disarm.extra.CAR_BLUETOOTH_MAC";

    private static final int JOB_ID = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private PowerManager.WakeLock mWakeLock;
    private long mDisarmStartTime;

    private Car mConnectedCar;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, DisarmService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "Starting onHandleWork...");

        // Acquire a wake lock to keep the CPU running
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuickDisarm::WakeLock");

        // We assume the work will be done much quicker but just
        // to be on the safe side we limit it to 10 seconds
        mWakeLock.acquire(10_000);

        mDisarmStartTime = System.currentTimeMillis();

        initBluetoothAdapter();
        if (mBluetoothAdapter != null) {
            final String carBluetoothMac = intent.getStringExtra(EXTRA_CAR_BLUETOOTH);
            mConnectedCar = PreferenceCache.get(this).getCar(carBluetoothMac);
            connectToDevice(mConnectedCar);
        } else {
            Log.e(TAG, "Bluetooth is not supported on this device");
            mWakeLock.release();
        }
    }

    private void initBluetoothAdapter() {
        // Initialize Bluetooth adapter
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(Car connectedCar) {
        final StartLinkGattCallback bluetoothGattCallback =
                new StartLinkGattCallback(this, connectedCar);

        final BluetoothDevice device = getStarlinkDevice(connectedCar.getStarlinkMac());
        device.connectGatt(this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private BluetoothDevice getStarlinkDevice(String starlinkMac) {
        return Build.VERSION.SDK_INT >= 33 ?
                mBluetoothAdapter.getRemoteLeDevice(starlinkMac, BluetoothDevice.ADDRESS_TYPE_PUBLIC) :
                mBluetoothAdapter.getRemoteDevice(starlinkMac);
    }

    @Override
    public void onDisarmStatusChange(DisarmStatus currentState, DisarmStatus newState) {
        if (newState == DisarmStatus.RANDOM_READ_SUCCESSFULLY) {
            Log.d(TAG, "Attempting to disarm...");
            StarlinkCommandDispatcher.get().dispatchDisarmCommand();
        }
        if (newState == DisarmStatus.DISARMED) {
            Log.d(TAG, "Successfully disarmed device in " + (System.currentTimeMillis() - mDisarmStartTime) + "ms");
            mWakeLock.release();
        }
    }
}
