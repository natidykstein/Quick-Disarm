package com.quick.disarm.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.quick.disarm.Car;

import java.util.HashSet;
import java.util.Set;

public class PreferenceCache {
    private static final String SHARED_PREFERENCES_FILE_NAME = "disarm_pref_cache";

    private static final String SPF_CAR_BLUETOOTH_LIST = "spf_car_bluetooth_list";
    private static final String SPF_AUTO_DISARM_ENABLED = "spf_auto_disarm_enabled";

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

    @Nullable
    public Car getCar(String bluetoothTrigger) {
        return mSharedPreferencesProxy.getObject(bluetoothTrigger, Car.class, null);
    }

    // PENDING: Make sure the pair(bluetoothTrigger, car) is unique per car's license plate
    public void addCar(String bluetoothTrigger, Car car) {
        final Set<String> bluetoothList = getCarBluetoothSet();
        bluetoothList.add(bluetoothTrigger);
        setCarBluetoothSet(bluetoothList);
        mSharedPreferencesProxy.putObject(bluetoothTrigger, car);
    }

    private void setCarBluetoothSet(Set<String> carBluetoothList) {
        mSharedPreferencesProxy.putObject(SPF_CAR_BLUETOOTH_LIST, carBluetoothList);
    }

    public Set<String> getCarBluetoothSet() {
        return mSharedPreferencesProxy.getObject(SPF_CAR_BLUETOOTH_LIST, new TypeToken<Set<String>>() {
        }, new HashSet<>());
    }

    public void setAutoDisarmEnabled(boolean autoDisarmEnabled) {
        mSharedPreferencesProxy.putBoolean(SPF_AUTO_DISARM_ENABLED, autoDisarmEnabled);
    }

    public boolean isAutoDisarmEnabled() {
        return mSharedPreferencesProxy.getBoolean(SPF_AUTO_DISARM_ENABLED, false);
    }
}
