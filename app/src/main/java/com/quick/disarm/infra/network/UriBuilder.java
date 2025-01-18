package com.quick.disarm.infra.network;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created on 23/02/2016 1:55 PM.
 */
public class UriBuilder {

    public static Uri build(String url, Map<String, String> params) {
        return build(url, null, params, true);
    }

    public static Uri build(String url, Map<String, String> params, boolean encodePath) {
        return build(url, null, params, encodePath);
    }

    public static Uri build(String url, String[] paths) {
        return build(url, paths, null, true);
    }

    public static Uri build(String url, String[] paths, boolean encodePath) {
        return build(url, paths, null, encodePath);
    }

    public static Uri build(String url, String[] paths, Map<String, String> params) {
        return build(url, paths, params, true);
    }

    public static Uri build(Uri uri, Map<String, String> params) {
        params.putAll(getQueryParamsFromUri(uri));
        return build(uri.getScheme() + "://" + uri.getAuthority(), new String[]{uri.getPath().substring(1)}, params, false);
    }

    /**
     * Constructs a valid URL using the specified parameters.
     *
     * @param paths
     * @param params
     * @param encodePath false to prevent encoding the path (if it is already encoded)
     * @return
     */
    public static Uri build(String url, String[] paths, Map<String, String> params, boolean encodePath) {
        if (url == null) {
            return Uri.EMPTY;
        }

        final Uri.Builder builder = new Uri.Builder();
        url = normalizeUrl(url);
        final String[] uriParts = url.split("://");
        boolean hasAuthority = uriParts.length > 1;
        if (hasAuthority) {
            builder.scheme(uriParts[0]);
        }
        builder.encodedAuthority(uriParts[hasAuthority ? 1 : 0]);

        if (paths != null) {
            for (String path : paths) {
                if (encodePath) {
                    builder.appendPath(path);
                } else {
                    builder.appendEncodedPath(path);
                }
            }
        }

        if (params != null) {
            Set<Map.Entry<String, String>> paramsSet = params.entrySet();
            for (Map.Entry<String, String> param : paramsSet) {
                builder.appendQueryParameter(param.getKey(), param.getValue());
            }
        }

        return builder.build();
    }

    private static Map<String, String> getQueryParamsFromUri(Uri uri) {
        Map<String, String> queryParams = new HashMap<>();
        for (String paramName : uri.getQueryParameterNames()) {
            queryParams.put(paramName, uri.getQueryParameter(paramName));
        }

        return queryParams;
    }

    /**
     * Normalizes authority by omitting "/" in the end
     *
     * @param authority
     * @return
     */
    private static String normalizeUrl(String authority) {
        return (authority != null && authority.endsWith("/")) ? authority.substring(0, authority.length() - 1) : authority;
    }
}
