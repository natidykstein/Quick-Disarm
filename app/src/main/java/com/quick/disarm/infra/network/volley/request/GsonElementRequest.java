package com.quick.disarm.infra.network.volley.request;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.quick.disarm.infra.ILog;
import com.quick.disarm.infra.Utils;
import com.quick.disarm.infra.network.volley.AppResponse;
import com.quick.disarm.infra.network.volley.VolleyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

/**
 * A request that parses the JSON response using Gson
 */
public class GsonElementRequest<T>
        extends AbstractJsonRequest<AppResponse<T>> {

    private static final boolean DEBUG_SERVER_JSON = false;

    private static final String HEADER_DATE = "date";
    private static final String HEADER_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";

    private final Class<T> mClazz;
    private final TypeToken<T> mTypeToken;

    public GsonElementRequest(Class<T> clazz,
                              int method,
                              String url,
                              Response.Listener<AppResponse<T>> listener,
                              Response.ErrorListener errorListener) {
        this(clazz, method, url, 0, 0, listener, errorListener);
    }

    public GsonElementRequest(Class<T> clazz,
                              int method,
                              String url,
                              long ttl,
                              long softTtl,
                              Response.Listener<AppResponse<T>> listener,
                              Response.ErrorListener errorListener) {
        this(clazz, null, method, url, null, ttl, softTtl, listener, errorListener);
    }

    public GsonElementRequest(TypeToken<T> typeToken,
                              int method,
                              String url,
                              long ttl,
                              long softTtl,
                              Response.Listener<AppResponse<T>> listener,
                              Response.ErrorListener errorListener) {
        this(null, typeToken, method, url, null, ttl, softTtl, listener, errorListener);
    }

    public GsonElementRequest(Class<T> clazz,
                              int method,
                              String url,
                              String body,
                              Response.Listener<AppResponse<T>> listener,
                              Response.ErrorListener errorListener) {
        this(clazz, null, method, url, body, 0, 0, listener, errorListener);
    }

    public GsonElementRequest(TypeToken<T> typeToken,
                              int method,
                              String url,
                              String body,
                              Response.Listener<AppResponse<T>> listener,
                              Response.ErrorListener errorListener) {
        this(null, typeToken, method, url, body, 0, 0, listener, errorListener);
    }

    public GsonElementRequest(Class<T> clazz,
                              TypeToken<T> typeToken,
                              int method,
                              String url,
                              String body,
                              long ttl,
                              long softTtl,
                              Response.Listener<AppResponse<T>> listener,
                              Response.ErrorListener errorListener) {
        super(method, url, body, ttl, softTtl, listener, errorListener);
        mClazz = clazz;
        mTypeToken = typeToken;
    }

    @Override
    protected Response parseNetworkResponse(NetworkResponse response, boolean fromCache) {
        try {
            final long start = System.nanoTime();
            final String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers, StandardCharsets.UTF_8.name()));
            final String targetType = (mClazz != null ? mClazz.getSimpleName() : mTypeToken.getType().toString());

            if (DEBUG_SERVER_JSON) {
                ILog.d("Attempting to create " + targetType + " from JSON: " + json);
            }

            if (!TextUtils.isEmpty(json)) {
                T data;
                if (String.class == mClazz) {
                    // Allow handling server response of type String
                    //noinspection unchecked
                    data = (T) json;
                } else {
                    data = Utils.getSharedGson()
                            .fromJson(json, mClazz != null ? mClazz : mTypeToken.getType());

                    if (data == null) {
                        return Response.error(new ParseError(new RuntimeException(
                                "Failed to parse JSON into " + targetType + ": " + json)));
                    }

                    if (DEBUG_SERVER_JSON) {
                        ILog.logPerformance("Parsed JSON to " + targetType, start);
                    }
                }
                try {
                    processResponse(new AppResponse<>(data, fromCache, getReceiveTimeMs(response)));
                } catch (VolleyError volleyError) {
                    return Response.error(volleyError);
                }
                return Response.success(new AppResponse<>(data, fromCache, getReceiveTimeMs(response)),
                        VolleyUtils.parseIgnoreCacheHeaders(response, getTtl(), getSoftTtl()));
            } else {
                ILog.d("Got empty json from server");
                return Response.success(new AppResponse<>("Empty response from server", fromCache, getReceiveTimeMs(response)),
                        VolleyUtils.parseIgnoreCacheHeaders(response, getTtl(), getSoftTtl()));
            }
        } catch (JsonSyntaxException | IOException e) {
            return Response.error(new ParseError(e));
        } catch (JsonIOException e) {
            return Response.error(new NetworkError(e));
        }
    }

    /**
     * Descendants may override to allow additional processing to the data received from the server on a background thread
     *
     * @param gongResponse
     * @throws VolleyError
     */
    protected void processResponse(AppResponse<T> gongResponse) throws
            VolleyError {
    }

    @SuppressLint("SimpleDateFormat")
    protected long getReceiveTimeMs(NetworkResponse response) {
        final String responseDate = response.headers.get(HEADER_DATE);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(HEADER_DATE_FORMAT);

        long receivedTimeMs;
        try {
            receivedTimeMs = dateFormat.parse(responseDate)
                    .getTime();
        } catch (Exception e) {
            ILog.e(e.getMessage());
            receivedTimeMs = System.currentTimeMillis();
        }
        return receivedTimeMs;
    }
}
