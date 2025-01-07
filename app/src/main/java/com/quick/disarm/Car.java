package com.quick.disarm;

public class Car {
    private final String mLicensePlate;
    private final String mStarlinkMac;
    private final int mStarlinkSerial;
    private final String mIturanCode;

    public Car(String licensePlate, String starlinkMac, int starLinkSerial, String ituranCode) {
        mLicensePlate = licensePlate;
        mStarlinkMac = starlinkMac;
        mStarlinkSerial = starLinkSerial;
        mIturanCode = ituranCode;
    }

    public String getLicensePlate() {
        return mLicensePlate;
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
    public String toString() {
        return "Car{" +
                "licensePlate='" + mLicensePlate + '\'' +
                ", starlinkMac='" + mStarlinkMac + '\'' +
                ", starLinkSerial=" + mStarlinkSerial +
                ", ituranCode='" + mIturanCode + '\'' +
                '}';
    }
}
