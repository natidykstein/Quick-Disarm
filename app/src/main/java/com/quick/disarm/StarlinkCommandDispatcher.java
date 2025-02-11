package com.quick.disarm;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.text.TextUtils;

import com.quick.disarm.infra.ILog;
import com.quick.disarm.infra.Utils;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class StarlinkCommandDispatcher {
    public static final byte COMMAND_UNLOCK = 1;

    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mSendCharacteristic;
    private BluetoothGattCharacteristic mRandomCharacteristic;
    private String mDeviceUuid;
    private byte[] mRandom;

    private Car mConnectedCar;

    private StarlinkCommandDispatcher() {
        // Prevent instances
    }

    private static final class InstanceHolder {
        private static final StarlinkCommandDispatcher INSTANCE = new StarlinkCommandDispatcher();
    }

    public static StarlinkCommandDispatcher get() {
        return InstanceHolder.INSTANCE;
    }

    public void init(BluetoothGatt gatt, Car connectedCar) {
        mGatt = gatt;
        mConnectedCar = connectedCar;
        mDeviceUuid = Utils.getDeviceUuid(QuickDisarmApplication.getAppContext());

        initCharacteristics();

        ILog.d("Initialized with device UUID = " + mDeviceUuid + " and car =" + mConnectedCar);
    }

    private void initCharacteristics() {
        final BluetoothGattService bluetoothGattService = mGatt.getService(StartLinkGattCallback.SERVICE_UUID);
        if (bluetoothGattService != null) {
            mSendCharacteristic = bluetoothGattService.getCharacteristic(StartLinkGattCallback.SEND_COMMAND_UUID);
            mRandomCharacteristic = bluetoothGattService.getCharacteristic(StartLinkGattCallback.RANDOM_NUMBER_UUID);
        } else {
            ILog.e("Failed to get service from Gatt server: " + StartLinkGattCallback.SERVICE_UUID);
        }
    }

    @SuppressLint("MissingPermission")
    public void dispatchReadRandomCommand() {
        if (mRandomCharacteristic == null) {
            initCharacteristics();
        }
        if (mRandomCharacteristic != null) {
            if (mGatt.readCharacteristic(mRandomCharacteristic)) {
                ILog.d("Started reading device random...");
            } else {
                ILog.e("Failed to read device random - readCharacteristic returned false");
            }
        } else {
            ILog.logException("Failed to read random - characteristics not initialized");
        }
    }

    public void setRandom(byte[] random) {
        if (!Arrays.equals(mRandom, random)) {
            ILog.d("Updated random from " + bytesToHex(mRandom) + " to " + bytesToHex(random));
            mRandom = random;
        }
    }

    public void dispatchDisarmCommand() {
        dispatchWritePacketCommand(COMMAND_UNLOCK);
    }

    @SuppressLint("MissingPermission")
    private void dispatchWritePacketCommand(byte commandByte) {
        final byte[] commandBufferBytes = new byte[12];
        commandBufferBytes[0] = 1;
        commandBufferBytes[1] = commandByte;

        int codeAsUnsignedInt;
        hexToBytes(mDeviceUuid, commandBufferBytes, 2);
        if (commandByte == COMMAND_UNLOCK) {
            codeAsUnsignedInt = Short.toUnsignedInt(Short.parseShort(mConnectedCar.getIturanCode()));
        } else {
            codeAsUnsignedInt = 0;
        }
        commandBufferBytes[10] = (byte) (codeAsUnsignedInt & 0xFF);
        commandBufferBytes[11] = (byte) ((codeAsUnsignedInt & 0xFF00) >> 8);
        ILog.d("Command buffer: " + bytesToHex(commandBufferBytes));


        final byte[] randomizedSerialBytes = keyGen(mConnectedCar.getStarlinkSerial(), mRandom);
        ILog.d("Key: " + bytesToHex(randomizedSerialBytes));


        final byte[] encryptedRandomizedSerialBytes = encryptCommand(commandBufferBytes, randomizedSerialBytes);
        ILog.d("Encrypted command: " + bytesToHex(encryptedRandomizedSerialBytes));

        if (Build.VERSION.SDK_INT >= 33) {
            mGatt.writeCharacteristic(mSendCharacteristic, encryptedRandomizedSerialBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else {
            mSendCharacteristic.setValue(encryptedRandomizedSerialBytes);
            mGatt.writeCharacteristic(mSendCharacteristic);
        }
    }

    private static String bytesToHex(byte[] paramArrayOfbyte) {
        final StringBuilder stringBuilder = new StringBuilder("[");
        if (paramArrayOfbyte != null) {
            for (byte b1 : paramArrayOfbyte) {
                stringBuilder.append(String.format(Locale.ENGLISH, " %02x", Byte.toUnsignedInt(b1)));
            }
        }
        stringBuilder.append(" ]");
        return stringBuilder.toString();
    }

    private static byte[] encryptCommand(byte[] paramArrayOfbyte1, byte[] paramArrayOfbyte2) {
        try {
            final Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec secretKeySpec = new SecretKeySpec(paramArrayOfbyte2, "AES");
            cipher.init(1, secretKeySpec);
            paramArrayOfbyte1 = cipher.doFinal(paramArrayOfbyte1);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException e) {
            ILog.logException(e);
        }
        return paramArrayOfbyte1;
    }

    private static void hexToBytes(String hex, byte[] targetByteArray, int offset) {
        if (TextUtils.isEmpty(hex) || hex.length() % 2 != 0) {
            throw new NumberFormatException("Not a hex string: " + hex);
        }

        if (targetByteArray.length - offset < hex.length() / 2) {
            throw new ArrayIndexOutOfBoundsException("Not enough room in buffer");
        }

        for (int i = 0, j; i < hex.length(); i = j) {
            int k = i / 2;
            j = i + 2;
            targetByteArray[k + offset] = (byte) Short.parseShort(hex.substring(i, j), 16);
        }
    }

    private static byte[] keyGen(int deviceSerial, byte[] random) {
        random = random.clone();
        if (random.length != 16) {
            ILog.e("random size must be equal to 16 - got " + random.length);
            return null;
        }

        final byte[] serialByteArray = ByteBuffer.allocate(4).putInt(deviceSerial).array();
        for (int i = 0; i < random.length; i++) {
            random[i] = (byte) (random[i] ^ serialByteArray[i % serialByteArray.length]);
        }
        return random;
    }
}
