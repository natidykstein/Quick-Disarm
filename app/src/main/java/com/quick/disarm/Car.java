package com.quick.disarm;

public class Car {
    private final String mLicensePlate;
    private final String mStarlinkMac;
    private final int mStarlinkSerial;
    private final String mIturanCode;

    public Car(String licensePlate, String starlinkMac, int starLinkSerial, String ituranCode) {
        mLicensePlate = licensePlate;
        mStarlinkMac = convertToValidMac(starlinkMac);
        mStarlinkSerial = starLinkSerial;
        mIturanCode = ituranCode;
    }

    private String convertToValidMac(String macString) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int j = i * 2;
            stringBuilder.append(macString.substring(j, j + 2));
            if (i != 5) stringBuilder.append(':');
        }
        return stringBuilder.toString();
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
