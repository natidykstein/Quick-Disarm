package com.quick.disarm.infra.network.volley;

import android.content.Context;

import com.android.volley.Cache;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.RequestFuture;
import com.quick.disarm.infra.ILog;
import com.quick.disarm.infra.Utils;

import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created on 23/02/2016 3:10 PM.
 */
public class VolleyUtils {

    /**
     * Convenience method for perform sync request
     *
     * @param requestFuture
     * @param request
     * @param <T>
     * @return
     * @throws VolleyError
     */
    public static <T> T requestAndWait(RequestFuture<T> requestFuture, Request<T> request) throws
            VolleyError {
        if (Utils.isMainLooperThread()) {
            throw new IllegalStateException("Don't invoke requestAndWait() on main-looper thread!...");
        }
        requestFuture.setRequest(request);

        try {
            final long start = System.nanoTime();
            final T t = requestFuture.get();
            ILog.logPerformance("RequestAndWait (" + request.getUrl() + ")", start);
            return t;
        } catch (InterruptedException e) {
            ILog.e(e.toString());
            ILog.logException(e);
        } catch (ExecutionException e) {
            throw (VolleyError) e.getCause();
        }

        return null;
    }

    /**
     * Extracts a {@link Cache.Entry} from a {@link NetworkResponse}.
     * Cache-control headers are ignored.
     *
     * @param response The network response to parse headers from
     * @param ttl
     * @param softTtl
     * @return a cache entry for the given response, or null if the response is not cacheable.
     */
    public static Cache.Entry parseIgnoreCacheHeaders(NetworkResponse response, long ttl, long softTtl) {
        final String serverEtag = response.headers.get("ETag");
        final long now = System.currentTimeMillis();
        long serverDate = 0;
        final String headerValue = response.headers.get("Date");
        if (headerValue != null) {
            serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
        }

        final Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = serverEtag;
        entry.serverDate = serverDate;
        entry.responseHeaders = response.headers;

        // If user only set soft ttl we set ttl to max value.
        entry.ttl = ttl == 0 && softTtl > 0 ? Long.MAX_VALUE : now + ttl;

        // Fix overflow for large ttl values
        if (entry.ttl < 0) {
            entry.ttl = Long.MAX_VALUE;
        }

        entry.softTtl = now + (softTtl > 0 ? softTtl : ttl);

        // Fix overflow for large soft ttl values
        if (entry.softTtl < 0) {
            entry.softTtl = Long.MAX_VALUE;
        }
        return entry;
    }

    public static void verifyAllowedNetwork(Context context) throws
            ConnectionNotAllowedException {
        // Prevent any communication if roaming is not allowed and we're currently roaming
        if (!VolleyRequestManager.INSTANCE.isAllowedOverRoaming() && Utils.isRoamingNetwork(context)) {
            throw new ConnectionNotAllowedException("Roaming not allowed");
        }

        // Prevent any communication if mobile data is not allowed and we're currently using mobile data
        if (!VolleyRequestManager.INSTANCE.isAllowedOverMobileData() && Utils.isConnectedToMobileNetwork(context)) {
            throw new ConnectionNotAllowedException("Not allowed over mobile data");
        }
    }

    public static boolean isNetworkRelatedError(Throwable throwable) {
        final Throwable cause = throwable.getCause();
        return throwable instanceof NetworkError || throwable instanceof TimeoutError
                || cause instanceof NetworkError || cause instanceof TimeoutError;
    }

    public static boolean isUnauthorized(Throwable throwable) {
        return getStatusCode(throwable) == HttpsURLConnection.HTTP_UNAUTHORIZED;
    }

    public static boolean isForbidden(Throwable throwable) {
        return getStatusCode(throwable) == HttpsURLConnection.HTTP_FORBIDDEN;
    }

    public static int getStatusCode(Throwable t) {
        // Check if volley error is the cause
        if (!(t instanceof VolleyError)) {
            t = t.getCause();
        }

        if (t instanceof VolleyError) {
            final VolleyError volleyError = (VolleyError) t;
            return volleyError.networkResponse != null ? volleyError.networkResponse.statusCode : -1;
        }

        return -1;
    }
}
