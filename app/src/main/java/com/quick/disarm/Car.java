package com.quick.disarm;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

public class Car implements Serializable {
    @SerializedName(value = "phoneNumber", alternate = {"n"})
    private final String mPhoneNumber;
    @SerializedName("triggerBluetoothName")
    private final String mTriggerBluetoothName;
    @SerializedName(value = "triggerBluetoothAddress", alternate = {"o", "bluetoothTrigger"})
    private final String mTriggerBluetoothAddress;
    @SerializedName(value = "licensePlate", alternate = {"p"})
    private final String mLicensePlate;
    private transient String mFormattedLicensePlate;
    @SerializedName(value = "starlinkMac", alternate = {"q"})
    private final String mStarlinkMac;
    @SerializedName(value = "starlinkSerial", alternate = {"r"})
    private final int mStarlinkSerial;
    @SerializedName(value = "ituranCode", alternate = {"s"})
    private final String mIturanCode;

    public Car(String phoneNumber, String triggerBluetoothName, String triggerBluetoothAddress, String licensePlate, String starlinkMac, int starLinkSerial, String ituranCode) {
        mPhoneNumber = phoneNumber;
        mTriggerBluetoothName = triggerBluetoothName;
        mTriggerBluetoothAddress = triggerBluetoothAddress;
        mLicensePlate = licensePlate;
        mStarlinkMac = convertToValidMac(starlinkMac);
        mStarlinkSerial = starLinkSerial;
        mIturanCode = ituranCode;
    }

    private String convertToValidMac(String macString) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int j = i * 2;
            stringBuilder.append(macString.substring(j, j + 2));
            if (i != 5) stringBuilder.append(':');
        }

        return stringBuilder.toString();
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public String getTriggerBluetoothName() {
        return mTriggerBluetoothName;
    }

    public String getTriggerBluetoothAddress() {
        return mTriggerBluetoothAddress;
    }

    public String getLicensePlate() {
        return mLicensePlate;
    }

    public String getFormattedLicensePlate() {
        if (mFormattedLicensePlate == null || mFormattedLicensePlate.isEmpty()) {
            final StringBuilder prettyPlate = new StringBuilder(mLicensePlate);
            prettyPlate.insert(3, "-");
            prettyPlate.insert(6, "-");
            mFormattedLicensePlate = prettyPlate.toString();
        }

        return mFormattedLicensePlate;
    }

    public String getStarlinkMac() {
        return mStarlinkMac;
    }

    public int getStarlinkSerial() {
        return mStarlinkSerial;
    }

    public String getIturanCode() {
        return mIturanCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Car car = (Car) o;
        return Objects.equals(mLicensePlate, car.mLicensePlate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mLicensePlate);
    }

    @Override
    public String toString() {
        return "Car [" + getFormattedLicensePlate() + "]";
    }

    public String toStringExtended() {
        return "Car{" +
                "phoneNumber='" + mPhoneNumber + '\'' +
                ", triggerBluetoothName='" + mTriggerBluetoothName + '\'' +
                ", triggerBluetoothAddress='" + mTriggerBluetoothAddress + '\'' +
                ", licensePlate='" + mLicensePlate + '\'' +
                ", starlinkMac='" + mStarlinkMac + '\'' +
                ", starlinkSerial=" + mStarlinkSerial +
                ", ituranCode='" + mIturanCode + '\'' +
                '}';
    }
}
