package com.quick.disarm.infra.network.volley.request;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonRequest;
import com.quick.disarm.infra.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// PENDING: Consider overriding deliverResponse() and consult isCancelled() before passing
// PENDING: the response to the listener
public abstract class AbstractJsonRequest<T>
        extends JsonRequest<T> {

    private static final String TAG_CACHE_HIT = "cache-hit";
    protected static final int INITIAL_TIME_OUT_MS = (int) TimeUnit.MINUTES.toMillis(2);
    protected static final int MAX_NUMBER_OF_RETIRES = 0;
    protected static final float BACKOFF_MULTIPLIER = 0f;

    private final List<String> mTags = new ArrayList<>();

    private final long mTtl;
    private final long mSoftTtl;
    private boolean mCacheHit;

    /**
     * Creates a new request.
     *
     * @param method        the HTTP method to use
     * @param url           URL to fetch the JSON from
     * @param listener      Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public AbstractJsonRequest(int method, String url, Listener<T> listener, ErrorListener errorListener) {
        this(method, url, 0, 0, listener, errorListener);
    }

    /**
     * Creates a new request.
     *
     * @param method        the HTTP method to use
     * @param url           URL to fetch the JSON from
     * @param listener      Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public AbstractJsonRequest(int method, String url, String body, Listener<T> listener, ErrorListener errorListener) {
        this(method, url, body, 0, 0, listener, errorListener);
    }

    /**
     * Creates a new request.
     *
     * @param method        the HTTP method to use
     * @param url           URL to fetch the JSON from
     * @param ttl
     * @param softTtl
     * @param listener      Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public AbstractJsonRequest(int method, String url, long ttl, long softTtl, Listener<T> listener, ErrorListener errorListener) {
        this(method, url, null, ttl, softTtl, listener, errorListener);
    }

    /**
     * Creates a new request.
     *
     * @param method        the HTTP method to use
     * @param url           URL to fetch the JSON from
     * @param body          A string representation of the object to post
     * @param ttl
     * @param softTtl
     * @param listener      Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public AbstractJsonRequest(int method, String url, String body, long ttl, long softTtl, Listener<T> listener, ErrorListener errorListener) {
        super(method, url, body, listener, errorListener);
        mTtl = ttl;
        mSoftTtl = softTtl;

        setRetryPolicy(new DefaultRetryPolicy(INITIAL_TIME_OUT_MS,
                MAX_NUMBER_OF_RETIRES,
                BACKOFF_MULTIPLIER));
        // Useful for cancelling requests
        setTag(getBaseUrl());
    }

    /**
     * @return The base URL (without the query parameters)
     */
    public String getBaseUrl() {
        return Utils.getBaseUrl(getUrl());
    }

    protected List<String> getTags() {
        return mTags;
    }

    /**
     * We prevent descendants from trying to provide a new retry policy by overriding this method
     * since it will create the retry policy object each time it's invoked causing an infinite loop of retries.
     *
     * @return The value of mRetryPolicy
     */
    @Override
    public final RetryPolicy getRetryPolicy() {
        return super.getRetryPolicy();
    }

    @Override
    public void addMarker(String tag) {
        super.addMarker(tag);
        // Only turn flag on once to prevent subsequent calls from overriding value
        if (!mCacheHit && TAG_CACHE_HIT.equals(tag)) {
            mCacheHit = true;
        }
        mTags.add(tag);
    }

    @Override
    protected final Response<T> parseNetworkResponse(final NetworkResponse response) {
        final Response<T> res = parseNetworkResponse(response, mCacheHit);

        // Reset flag in case soft TTL expires and parseResponse is called again
        mCacheHit = false;

        return res;
    }

    protected abstract Response<T> parseNetworkResponse(final NetworkResponse response, final boolean fromCache);


    public long getSoftTtl() {
        return mSoftTtl;
    }

    public long getTtl() {
        return mTtl;
    }
}
