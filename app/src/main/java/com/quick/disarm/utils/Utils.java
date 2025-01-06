package com.quick.disarm.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    private static final Gson sGson;

    static {
        sGson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
    }

    public static String toJson(Gson gson, Object object) {
        return gson.toJson(object);
    }

    public static String toJson(Object object) {
        return toJson(sGson, object);
    }

    public static <T> T fromJson(Gson gson, String jsonString, Class<T> classType) {
        return gson.fromJson(jsonString, classType);
    }

    public static <T> T fromJson(Gson gson, String jsonString, TypeToken<T> typeOfT) {
        return gson.fromJson(jsonString, typeOfT.getType());
    }

    public static <T> T fromJson(String jsonString, Class<T> classType) {
        return fromJson(sGson, jsonString, classType);
    }

    public static <T> T fromJson(String jsonString, TypeToken<T> typeOfT) {
        return fromJson(sGson, jsonString, typeOfT);
    }

    public static JsonObject fromJson(String jsonString) {
        try {
            return JsonParser.parseString(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }
}
