package com.quick.disarm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Set;

class SharedPreferencesProxy {
    private static final String TAG = SharedPreferencesProxy.class.getSimpleName();

    private final SharedPreferences mSharedPref;

    public SharedPreferencesProxy(Context context, String prefName) {
        mSharedPref = createSecuredSharedPreferences(context, prefName);
    }

    private SharedPreferences createSecuredSharedPreferences(Context context, String prefName) {
        try {
            // Attempt to create our secured shared preferences
            final MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    prefName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to create secured shared prefs - defaulting to unsecured shared prefs");

            return null;
        }
    }

    public boolean contains(String key) {
        return mSharedPref.contains(key);
    }

    public String getString(String key) {
        return getString(key, "");
    }

    public String getString(String key, String defaultValue) {
        return mSharedPref.getString(key, defaultValue);
    }

    public void putString(String key, String value) {
        final SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void putStringSet(String key, Set<String> values) {
        final SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putStringSet(key, values);
        editor.apply();
    }

    public Set<String> getStringSet(String key) {
        return mSharedPref.getStringSet(key, null);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return mSharedPref.getBoolean(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        final SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public int getInt(String key, int defaultValue) {
        return mSharedPref.getInt(key, defaultValue);
    }

    public void putInt(String key, int value) {
        final SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public long getLong(String key, long defaultValue) {
        return mSharedPref.getLong(key, defaultValue);
    }

    public void putLong(String key, long value) {
        final SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public float getFloat(String key, float defaultValue) {
        return mSharedPref.getFloat(key, defaultValue);
    }

    public void putFloat(String key, float value) {
        final SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    public void putObject(String key, Object object) {
        final SharedPreferences.Editor editor = mSharedPref.edit();
        if (object != null) {
            editor.putString(key, Utils.toJson(object));
        } else {
            editor.remove(key);
        }
        editor.apply();
    }

    public <T> T getObject(String key, Class<T> classType, T defaultValue) {
        final String stringValue = mSharedPref.getString(key, null);
        try {
            return stringValue != null ? Utils.fromJson(stringValue, classType) : defaultValue;
        } catch (JsonSyntaxException e) {
            Log.e(TAG, e.toString());
            return defaultValue;
        }
    }

    public <T> T getObject(String key, TypeToken<T> typeOfT, T defaultValue) {
        final String stringValue = mSharedPref.getString(key, null);
        try {
            return stringValue != null ? Utils.fromJson(stringValue, typeOfT) : defaultValue;
        } catch (JsonSyntaxException e) {
            Log.e(TAG, e.toString());
            return defaultValue;
        }
    }

    public <T> T getObject(String key, Class<T> classType) {
        return getObject(key, classType, null);
    }

    public <T> T getObject(String key, TypeToken<T> classType) {
        return getObject(key, classType, null);
    }

    public void remove(String key) {
        final SharedPreferences.Editor editor = mSharedPref.edit();
        editor.remove(key);
        editor.apply();
    }

    public Map<String, ?> getAll() {
        return mSharedPref.getAll();
    }

    public void clearAll() {
        mSharedPref.edit()
                .clear()
                .apply();
    }
}
