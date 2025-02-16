package com.quick.disarm.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.quick.disarm.AuthLevel;
import com.quick.disarm.Car;

import java.util.HashSet;
import java.util.Set;

public class PreferenceCache {
    private static final String SHARED_PREFERENCES_FILE_NAME = "disarm_pref_cache";

    private static final String SPF_CAR_BLUETOOTH_LIST = "spf_car_bluetooth_list";
    private static final String SPF_AUTH_LEVEL = "spf_auth_level";


    private static volatile PreferenceCache sInstance;

    private final SharedPreferencesProxy mSharedPreferencesProxy;

    public static PreferenceCache get(Context context) {
        if (sInstance == null) {
            synchronized (PreferenceCache.class) {
                if (sInstance == null) {
                    sInstance = new PreferenceCache(context);
                }
            }
        }

        return sInstance;
    }

    private PreferenceCache(Context context) {
        if (context == null) {
            throw new IllegalStateException("Must invoke PreferencesCache.init(context) first!");
        }

        mSharedPreferencesProxy = new SharedPreferencesProxy(context, SHARED_PREFERENCES_FILE_NAME);
    }

    private void setCarBluetoothSet(Set<String> carBluetoothList) {
        mSharedPreferencesProxy.putObject(SPF_CAR_BLUETOOTH_LIST, carBluetoothList);
    }

    public Set<String> getCarBluetoothSet() {
        return mSharedPreferencesProxy.getObject(SPF_CAR_BLUETOOTH_LIST, new TypeToken<Set<String>>() {
        }, new HashSet<>());
    }

    @Nullable
    public Car getCar(String bluetoothMac) {
        return mSharedPreferencesProxy.getObject(bluetoothMac, Car.class, null);
    }

    public void addCar(String bluetoothMac, Car car) {
        final Set<String> bluetoothList = getCarBluetoothSet();
        bluetoothList.add(bluetoothMac);
        setCarBluetoothSet(bluetoothList);
        mSharedPreferencesProxy.putObject(bluetoothMac, car);
    }

    public void setAuthenticationLevel(AuthLevel authLevel) {
        mSharedPreferencesProxy.putString(SPF_AUTH_LEVEL, authLevel.toString());
    }

    public AuthLevel getAuthenticationLevel() {
        return AuthLevel.valueOf(mSharedPreferencesProxy.getString(SPF_AUTH_LEVEL, AuthLevel.DEVICE.toString()));
    }
}
