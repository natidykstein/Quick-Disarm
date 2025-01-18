package com.quick.disarm.model;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;


/**
 * <MBKisRegisteredAnswer xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.ituran.com/IturanMobileService">
 * <ReturnError>OK</ReturnError>
 * <RegistryStatus>1</RegistryStatus>
 * <IsActivated>true</IsActivated>
 * </MBKisRegisteredAnswer>
 */
public class IsRegisteredAnswer {
    @SerializedName("ReturnError")
    private String mReturnError;

    @SerializedName("RegistryStatus")
    private int mRegistryStatus;

    @SerializedName("IsActivated")
    private boolean mIsActivated;

    public IsRegisteredAnswer() {
        // Used by Gson
    }

    public String getReturnError() {
        return mReturnError;
    }

    public int getRegistryStatus() {
        return mRegistryStatus;
    }

    public boolean getActivated() {
        return mIsActivated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsRegisteredAnswer that = (IsRegisteredAnswer) o;
        return mRegistryStatus == that.mRegistryStatus && mIsActivated == that.mIsActivated && Objects.equals(mReturnError, that.mReturnError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mReturnError, mRegistryStatus, mIsActivated);
    }

    @Override
    public String toString() {
        return "IsRegisteredAnswer{" +
                "mReturnError='" + mReturnError + '\'' +
                ", mRegistryStatus=" + mRegistryStatus +
                ", mIsActivated=" + mIsActivated +
                '}';
    }
}
