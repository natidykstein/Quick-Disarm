package com.quick.disarm.infra.network.volley;

import android.content.Context;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;

import java.io.File;

/**
 * A singleton serving an HTTP connection to the server. Internally uses the volley android library + okhttp.
 * <p/>
 *
 * @author Nati
 */
public enum VolleyRequestManager {
    INSTANCE;

    private static final boolean DEBUG_VOLLEY = false;

    /**
     * Default on-disk cache directory.
     */
    private static final String DEFAULT_CACHE_DIR = "volley";

    private static final int REQUEST_QUEUE_THREAD_POOL_SIZE = 4;
    private static final int CACHED_REQUEST_QUEUE_THREAD_POOL_SIZE = 4;

    // Set our requests cache size to 50M
    private static final int MAX_CACHE_SIZE_BYTES = 50 * 1024 * 1024;

    private RequestQueue mRequestQueue;

    private boolean mAllowedOverRoaming = true;
    private boolean mAllowedOverMobileData = true;

    private synchronized RequestQueue getRequestQueue(final Context context) {
        if (mRequestQueue == null) {
            mRequestQueue = createRequestQueue(context, false);
            mRequestQueue.start();
        }
        return mRequestQueue;
    }

    private synchronized RequestQueue createRequestQueue(final Context context, boolean cached) {
        if (cached) {
            final File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
            final Cache diskCache = new DiskBasedCache(cacheDir, MAX_CACHE_SIZE_BYTES);
            return new RequestQueue(diskCache, createVolleyNetwork(context), CACHED_REQUEST_QUEUE_THREAD_POOL_SIZE);
        } else {
            return new RequestQueue(new NoCache(), createVolleyNetwork(context), REQUEST_QUEUE_THREAD_POOL_SIZE);
        }
    }

    /**
     * Creates a volley Network object
     */
    private Network createVolleyNetwork(Context context) {
        // BasicNetwork logs slow responses(>3 seconds) and cannot be removed. See https://issuetracker.google.com/issues/37074321
        return new BasicNetwork(new HurlStack());
    }

    /**
     * Cancels all requests with the matching filter
     *
     * @param context A context (surprising right?)
     * @param filter  A <code>RequestFilter</code> filtering the requests to cancel
     */
    public void cancelRequests(Context context, RequestQueue.RequestFilter filter) {
        getRequestQueue(context).cancelAll(filter);
    }

    public boolean isAllowedOverRoaming() {
        return mAllowedOverRoaming;
    }

    public void setAllowedOverRoaming(boolean allowedOverRoaming) {
        mAllowedOverRoaming = allowedOverRoaming;
    }

    public boolean isAllowedOverMobileData() {
        return mAllowedOverMobileData;
    }

    public void setAllowedOverMobileData(boolean allowedOverMobileData) {
        mAllowedOverMobileData = allowedOverMobileData;
    }
}
