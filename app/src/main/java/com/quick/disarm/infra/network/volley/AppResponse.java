package com.quick.disarm.infra.network.volley;

import androidx.annotation.NonNull;

/**
 * In addition to the actual parsed data
 * this class provides more useful information regarding the response
 */
public class AppResponse<T> {

    private final T       mData;
    private final boolean mLoadedFromCache;
    private final long    mReceivedTimeMs;

    public AppResponse(@NonNull T data, boolean loadedFromCache) {
        this(data, loadedFromCache, 0);
    }

    public AppResponse(@NonNull final T data, final boolean loadedFromCache, final long receivedTimeMs) {
        mData = data;
        mLoadedFromCache = loadedFromCache;
        mReceivedTimeMs = receivedTimeMs;
    }

    public @NonNull
    T getData() {
        return mData;
    }

    /**
     * Returns whether the response was received from cache
     *
     * @return true if this response is from cache, false otherwise
     */
    public boolean isLoadedFromCache() {
        return mLoadedFromCache;
    }

    /**
     * @return The time that the response was received from the server
     */
    public long getReceivedTimeMs() {
        return mReceivedTimeMs;
    }

    @Override
    public String toString() {
        return mData.toString() + (mLoadedFromCache ? " [cache]" : "");
    }
}
