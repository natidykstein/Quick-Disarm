package com.quick.disarm.infra.network.volley;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.quick.disarm.infra.ILog;

import java.net.HttpURLConnection;

public abstract class VolleyResponseListener<T> implements Response.Listener<T>, Response.ErrorListener {

    private String mRequestUrl;
    private boolean mReceivedResponse;
    protected int mHttpStatusCode;
    protected String mServerMessage;
    protected String mResponseString;

    private void initErrorMessage(VolleyError volleyError) {
        if (volleyError != null && volleyError.networkResponse != null) {
            mHttpStatusCode = volleyError.networkResponse.statusCode;
            mResponseString = volleyError.networkResponse.data != null && volleyError.networkResponse.data.length > 0 ? new String(volleyError.networkResponse.data) : null;
            mServerMessage = "HTTP " + mHttpStatusCode + (mResponseString != null ? " - " + mResponseString : "");
        } else {
            mServerMessage = volleyError != null ? volleyError.toString() : "volleyError==null";
        }
    }

    @Override
    public final void onResponse(final T response) {
        onResponse(response, mReceivedResponse);
        mReceivedResponse = true;
    }

    protected abstract void onResponse(final T response, final boolean secondCallback);

    @Override
    public final void onErrorResponse(VolleyError volleyError) {
        initErrorMessage(volleyError);
        if (mHttpStatusCode != HttpURLConnection.HTTP_UNAVAILABLE) {
            ILog.logException(createException(volleyError));
        }

        onErrorResponse(volleyError, mReceivedResponse, mHttpStatusCode == HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    private RuntimeException createException(VolleyError volleyError) {
        final String errorMessage = "onErrorResponse(): " + mServerMessage + (mRequestUrl != null ? ". Request url: " + mRequestUrl : "");
        return new RuntimeException(errorMessage, volleyError);
    }

    protected abstract void onErrorResponse(final VolleyError volleyError, final boolean secondCallback, final boolean unauthorized);

    public static String responseParser(VolleyError volleyError) {
        if (volleyError != null && volleyError.networkResponse != null) {
            final NetworkResponse networkResponse = volleyError.networkResponse;
            final int httpStatusCode = networkResponse.statusCode;
            final String statusCodeStr = String.valueOf(httpStatusCode);
            final String moreInfo = new String(networkResponse.data);
            return volleyError.toString() + " [" + moreInfo + " (" + statusCodeStr + ")]";
        } else {
            return volleyError != null ? volleyError.toString() : "volleyError==null";
        }
    }

    public void setRequestUrl(String requestUrl) {
        mRequestUrl = requestUrl;
    }
}
