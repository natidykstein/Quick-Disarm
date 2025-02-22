package com.quick.disarm.model;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;


/**
 * <MBKSerializationRequestAnswer xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.ituran.com/IturanMobileService">
 * <ReturnError>OK</ReturnError>
 * <MacAddress>D01FDDC2372D</MacAddress>
 * <Serial>2276181</Serial>
 * </MBKSerializationRequestAnswer>
 */
public class SerializationAnswer {
    @SerializedName("ReturnError")
    private String mReturnError;

    @SerializedName("MacAddress")
    private String mStarlinkMacAddress;

    @SerializedName("Serial")
    private int mStarlinkSerial;

    public SerializationAnswer() {
        // Used by Gson
    }

    public String getReturnError() {
        return mReturnError;
    }

    public String getStarlinkMacAddress() {
        return mStarlinkMacAddress;
    }

    public int getStarlinkSerial() {
        return mStarlinkSerial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializationAnswer that = (SerializationAnswer) o;
        return mStarlinkSerial == that.mStarlinkSerial && Objects.equals(mReturnError, that.mReturnError) && Objects.equals(mStarlinkMacAddress, that.mStarlinkMacAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mReturnError, mStarlinkMacAddress, mStarlinkSerial);
    }

    @Override
    public String toString() {
        return "SerializationAnswer{" + "mReturnError='" + mReturnError + '\'' + ", mStarlinkMacAddress='" + mStarlinkMacAddress + '\'' + ", mStarlinkSerial=" + mStarlinkSerial + '}';
    }
}
