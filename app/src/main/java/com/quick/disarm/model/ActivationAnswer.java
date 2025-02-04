package com.quick.disarm.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;


/**
 * <MBKActivationAnswer xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.ituran.com/IturanMobileService">
 * <ReturnError>OK</ReturnError>
 * <DidRecognizeOwner>true</DidRecognizeOwner>
 * <PlatId>3413856</PlatId> // Named 'platformId' and acts as username in some queries
 * </MBKActivationAnswer>z
 */
public class ActivationAnswer {
    @SerializedName("ReturnError")
    private String mReturnError;

    @SerializedName("DidRecognizeOwner")
    private boolean mDidRecognizeOwner;

    @SerializedName("PlatId")
    private int mPlatformId;

    public ActivationAnswer() {
        // Used by Gson
    }

    public String getReturnError() {
        return mReturnError;
    }

    public boolean getDidRecognizerOwner() {
        return mDidRecognizeOwner;
    }

    public int getPlatformId() {
        return mPlatformId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivationAnswer that = (ActivationAnswer) o;
        return mDidRecognizeOwner == that.mDidRecognizeOwner && mPlatformId == that.mPlatformId && Objects.equals(mReturnError, that.mReturnError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mReturnError, mDidRecognizeOwner, mPlatformId);
    }

    @NonNull
    @Override
    public String toString() {
        return "ActivationAnswer{" +
                "mReturnError='" + mReturnError + '\'' +
                ", mDidRecognizeOwner=" + mDidRecognizeOwner +
                ", mPlatformId=" + mPlatformId +
                '}';
    }
}
