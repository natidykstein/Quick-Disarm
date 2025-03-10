package com.quick.disarm;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import com.quick.disarm.infra.ILog;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class StartLinkGattCallback extends BluetoothGattCallback {
    public static final UUID CONFIGURATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID GET_RESULT_UUID = UUID.fromString("2445524D-37EA-11E4-90C4-D421C4C8CC12");
    public static final UUID RANDOM_NUMBER_UUID = UUID.fromString("2445524D-37EA-11E4-90C4-D421C4C8CC13");
    public static final UUID SEND_COMMAND_UUID = UUID.fromString("2445524D-37EA-11E4-90C4-D421C4C8CC11");
    public static final UUID SERVICE_UUID = UUID.fromString("2445524D-37EA-11E4-90C4-D421C4C8CC10");

    private final DisarmStateListener mListener;
    private final Car mConnectedCar;
    private DisarmStateListener.DisarmStatus mDisarmStatus;
    private boolean mCleaningUp;

    public StartLinkGattCallback(DisarmStateListener listener, Car connectedCar) {
        mListener = listener;
        mConnectedCar = connectedCar;
        mDisarmStatus = DisarmStateListener.DisarmStatus.CONNECTING_TO_DEVICE;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            setDisarmStatus(DisarmStateListener.DisarmStatus.DEVICE_CONNECTED);
            ILog.d("Connected to device's GATT server - starting device discovery...");
            gatt.discoverServices();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            ILog.d("Disconnected from GATT server");
            setDisarmStatus(DisarmStateListener.DisarmStatus.READY_TO_CONNECT);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            ILog.d("Device discovery successful - attempting to read random...");
            setDisarmStatus(DisarmStateListener.DisarmStatus.DEVICE_DISCOVERED);
            StarlinkCommandDispatcher.get().init(gatt, mConnectedCar);
            StarlinkCommandDispatcher.get().dispatchReadRandomCommand();
        } else {
            ILog.w("onServicesDiscovered received status: " + status);
            setDisarmStatus(DisarmStateListener.DisarmStatus.READY_TO_CONNECT);
            cleanup(gatt);
        }
    }

    @Override
    public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
        if (characteristic.getUuid().equals(RANDOM_NUMBER_UUID)) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                ILog.d("Random read successfully - attempting to disarm...");
                StarlinkCommandDispatcher.get().setRandom(value);
                setDisarmStatus(DisarmStateListener.DisarmStatus.RANDOM_READ_SUCCESSFULLY);
                StarlinkCommandDispatcher.get().dispatchDisarmCommand();
            } else {
                ILog.e("Random read failed with status = " + status);
                setDisarmStatus(DisarmStateListener.DisarmStatus.READY_TO_CONNECT);
                cleanup(gatt);
            }
        } else {
            ILog.w("Got characteristic read from unknown characteristic: " + characteristic.getUuid() + " with status = " + status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (characteristic.getUuid().equals(SEND_COMMAND_UUID)) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                ILog.d("Disarm successful");
                setDisarmStatus(DisarmStateListener.DisarmStatus.DISARMED);
            } else {
                ILog.e("Disarm failed with status = " + status);
                setDisarmStatus(DisarmStateListener.DisarmStatus.READY_TO_CONNECT);
            }

            cleanup(gatt);
        } else {
            ILog.w("Got Write response on unsupported characteristic: " + characteristic.getUuid() + " with status = " + status);
        }
    }

    @Override
    public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
        internalOnCharacteristicChanged(gatt, characteristic, value);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        internalOnCharacteristicChanged(gatt, characteristic, characteristic.getValue());
    }

    // PENDING: To we really need to do anything here?
    private void internalOnCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        ILog.d("internalOnCharacteristicChanged()");

        if (characteristic.getUuid().equals(RANDOM_NUMBER_UUID)) {
            StarlinkCommandDispatcher.get().setRandom(value);
        }

        if (characteristic.getUuid().equals(GET_RESULT_UUID)) {
            final byte resultByte = value.length == 1 ? value[0] : characteristic.getValue()[0];

            // PENDING: Update with result after writing value
            ILog.d("Got result with value " + resultByte);
        }
    }

    private void cleanup(BluetoothGatt gatt) {
        mCleaningUp = true;
        if (gatt != null) {
            ILog.d("Closing bluetooth gatt connection...");
            gatt.disconnect();
            gatt.close();
        } else {
            ILog.e("Bluetooth gatt is null - can't cleanup");
        }
    }

    private void setDisarmStatus(DisarmStateListener.DisarmStatus disarmStatus) {
        if (mCleaningUp) {
            ILog.d("Ignoring disarm status change after cleanup: " + disarmStatus);
            return;
        }

        mListener.onDisarmStatusChange(mDisarmStatus, disarmStatus);
        mDisarmStatus = disarmStatus;
    }
}
