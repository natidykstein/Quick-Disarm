package com.quick.disarm;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class StartLinkGattCallback extends BluetoothGattCallback {
    private static final String TAG = StartLinkGattCallback.class.getSimpleName();

    public static final UUID CONFIGURATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID GET_RESULT_UUID = UUID.fromString("2445524D-37EA-11E4-90C4-D421C4C8CC12");
    public static final UUID RANDOM_NUMBER_UUID = UUID.fromString("2445524D-37EA-11E4-90C4-D421C4C8CC13");
    public static final UUID SEND_COMMAND_UUID = UUID.fromString("2445524D-37EA-11E4-90C4-D421C4C8CC11");
    public static final UUID SERVICE_UUID = UUID.fromString("2445524D-37EA-11E4-90C4-D421C4C8CC10");

    private final DisarmStateListener mListener;
    private final Car mConnectedCar;
    private DisarmStateListener.DisarmStatus mDisarmStatus;

    public StartLinkGattCallback(DisarmStateListener listener, Car connectedCar) {
        mListener = listener;
        mConnectedCar = connectedCar;
        mDisarmStatus = DisarmStateListener.DisarmStatus.CONNECTING_TO_DEVICE;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            setDisarmStatus(DisarmStateListener.DisarmStatus.DEVICE_CONNECTED);
            Log.d(TAG, "Connected to device's GATT server");
            gatt.discoverServices();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            Log.d(TAG, "Disconnected from GATT server");
            setDisarmStatus(DisarmStateListener.DisarmStatus.READY_TO_CONNECT);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "Discovering device completed - attempting to read random...");
            setDisarmStatus(DisarmStateListener.DisarmStatus.DEVICE_DISCOVERED);
            StarlinkCommandDispatcher.get().init(gatt, mConnectedCar);
            StarlinkCommandDispatcher.get().dispatchReadRandomCommand();
        } else {
            Log.w(TAG, "onServicesDiscovered received status: " + status);
            setDisarmStatus(DisarmStateListener.DisarmStatus.READY_TO_CONNECT);
        }
    }

    @Override
    public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (characteristic.getUuid().equals(RANDOM_NUMBER_UUID)) {
                Log.d(TAG, "Random read successfully");
                StarlinkCommandDispatcher.get().setRandom(value);
                setDisarmStatus(DisarmStateListener.DisarmStatus.RANDOM_READ_SUCCESSFULLY);
            } else {
                Log.w(TAG, "Got characteristic read from unknown uuid: " + characteristic.getUuid());
                setDisarmStatus(DisarmStateListener.DisarmStatus.READY_TO_CONNECT);
            }
        } else {
            Log.w(TAG, "onCharacteristicRead received status: " + status);
            setDisarmStatus(DisarmStateListener.DisarmStatus.READY_TO_CONNECT);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (characteristic.getUuid().equals(SEND_COMMAND_UUID)) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Write successful for send command");
                setDisarmStatus(DisarmStateListener.DisarmStatus.DISARMED);
            } else {
                Log.w(TAG, "Write failed on characteristic: " + characteristic.getUuid() + " with status " + status);
                setDisarmStatus(DisarmStateListener.DisarmStatus.READY_TO_CONNECT);
            }
        } else {
            Log.w(TAG, "Got Write response on unsupported characteristic: " + characteristic.getUuid() + " with status " + status);
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
        Log.d(TAG, "internalOnCharacteristicChanged()");

        if (characteristic.getUuid().equals(RANDOM_NUMBER_UUID)) {
            StarlinkCommandDispatcher.get().setRandom(value);
        }

        if (characteristic.getUuid().equals(GET_RESULT_UUID)) {
            final byte resultByte = value.length == 1 ? value[0] : characteristic.getValue()[0];

            // PENDING: Update with result after writing value
            Log.d(TAG, "Got result with value " + resultByte);
        }
    }

    private void setDisarmStatus(DisarmStateListener.DisarmStatus disarmStatus) {
        mListener.onDisarmStatusChange(mDisarmStatus, disarmStatus);
        mDisarmStatus = disarmStatus;
    }
}
