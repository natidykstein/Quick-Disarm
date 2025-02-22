package com.quick.disarm.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.quick.disarm.Car;
import com.quick.disarm.infra.ILog;

import java.util.HashSet;
import java.util.Set;

public class PreferenceCache {
    private static final String SHARED_PREFERENCES_FILE_NAME = "disarm_pref_cache";

    @Deprecated
    private static final String SPF_CAR_BLUETOOTH_LIST = "spf_car_bluetooth_list";
    private static final String SPF_CAR_SET = "spf_car_set";
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

    public void removeOldCarsData() {
        final Set<String> bluetoothList = getCarBluetoothSet();
        for (String bluetoothTrigger : bluetoothList) {
            mSharedPreferencesProxy.remove(bluetoothTrigger);
        }
        mSharedPreferencesProxy.remove(SPF_CAR_BLUETOOTH_LIST);

        ILog.d("Removed " + bluetoothList.size() + " old car(s) data");
    }

    @Deprecated
    public void addCar(String bluetoothTrigger, Car car) {
        final Set<String> bluetoothList = getCarBluetoothSet();
        bluetoothList.add(bluetoothTrigger);
        setCarBluetoothSet(bluetoothList);
        mSharedPreferencesProxy.putObject(bluetoothTrigger, car);
    }

    @Nullable
    @Deprecated
    public Car getCar(String bluetoothTrigger) {
        return mSharedPreferencesProxy.getObject(bluetoothTrigger, Car.class, null);
    }

    @Deprecated
    private void setCarBluetoothSet(Set<String> carBluetoothList) {
        mSharedPreferencesProxy.putObject(SPF_CAR_BLUETOOTH_LIST, carBluetoothList);
    }

    @Deprecated
    public Set<String> getCarBluetoothSet() {
        return mSharedPreferencesProxy.getObject(SPF_CAR_BLUETOOTH_LIST, new TypeToken<Set<String>>() {
        }, new HashSet<>());
    }

    public Set<Car> getCarSet() {
        return mSharedPreferencesProxy.getObject(SPF_CAR_SET, new TypeToken<Set<Car>>() {
        }, new HashSet<>());
    }

    public void addCar(Car car) {
        final Set<Car> carSet = getCarSet();
        carSet.add(car);
        mSharedPreferencesProxy.putObject(SPF_CAR_SET, carSet);
    }

    public void setAutoDisarmEnabled(boolean autoDisarmEnabled) {
        mSharedPreferencesProxy.putBoolean(SPF_AUTO_DISARM_ENABLED, autoDisarmEnabled);
    }

    public boolean isAutoDisarmEnabled() {
        return mSharedPreferencesProxy.getBoolean(SPF_AUTO_DISARM_ENABLED, false);
    }
}
