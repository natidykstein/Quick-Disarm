package com.quick.disarm;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

public class Car implements Serializable {
    @SerializedName(value="phoneNumber", alternate={"n"})
    private final String mPhoneNumber;
    @SerializedName(value="bluetoothTrigger", alternate={"o"})
    private final String mBluetoothTrigger;
    @SerializedName(value="licensePlate", alternate={"p"})
    private final String mLicensePlate;
    @SerializedName(value="starlinkMac", alternate={"q"})
    private final String mStarlinkMac;
    @SerializedName(value="starlinkSerial", alternate={"r"})
    private final int mStarlinkSerial;
    @SerializedName(value="ituranCode", alternate={"s"})
    private final String mIturanCode;

    public Car(String phoneNumber, String bluetoothTrigger, String licensePlate, String starlinkMac, int starLinkSerial, String ituranCode) {
        mPhoneNumber = phoneNumber;
        mBluetoothTrigger = bluetoothTrigger;
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

    public String getBluetoothTrigger() {
        return mBluetoothTrigger;
    }

    public String getLicensePlate() {
        return mLicensePlate;
    }

    public String getFormattedLicensePlate() {
        final StringBuilder prettyPlate = new StringBuilder(mLicensePlate);
        prettyPlate.insert(3, "-");
        prettyPlate.insert(6, "-");
        return prettyPlate.toString();
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
        Car car = (Car) o;
        return Objects.equals(mLicensePlate, car.mLicensePlate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mLicensePlate);
    }

    @Override
    public String toString() {
        return "Car(" + mLicensePlate + ")";
    }

    public String toStringExtended() {
        return "Car{" +
                "mPhoneNumber='" + mPhoneNumber + '\'' +
                ", mBluetoothTrigger='" + mBluetoothTrigger + '\'' +
                ", mLicensePlate='" + mLicensePlate + '\'' +
                ", mStarlinkMac='" + mStarlinkMac + '\'' +
                ", mStarlinkSerial=" + mStarlinkSerial +
                ", mIturanCode='" + mIturanCode + '\'' +
                '}';
    }
}
