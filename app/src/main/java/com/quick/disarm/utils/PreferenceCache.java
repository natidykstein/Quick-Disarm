package com.quick.disarm.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.quick.disarm.Car;

import java.util.ArrayList;
import java.util.List;

public class PreferenceCache {
    private static final String SHARED_PREFERENCES_FILE_NAME = "disarm_pref_cache";

    private static final String SPF_CAR_BLUETOOTH_LIST = "spf_car_bluetooth_list";


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

    private void setCarBluetoothList(List<String> carBluetoothList) {
        mSharedPreferencesProxy.putObject(SPF_CAR_BLUETOOTH_LIST, carBluetoothList);
    }

    public List<String> getCarBluetoothList() {
        return mSharedPreferencesProxy.getObject(SPF_CAR_BLUETOOTH_LIST, new TypeToken<List<String>>() {
        }, new ArrayList<>());
    }

    @Nullable
    public Car getCar(String bluetoothMac) {
        return mSharedPreferencesProxy.getObject(bluetoothMac, Car.class, null);
    }

    public void putCar(String bluetoothMac, Car car) {
        // Add bluetooth to existing list
        final List<String> bluetoothList = getCarBluetoothList();
        bluetoothList.add(bluetoothMac);
        setCarBluetoothList(bluetoothList);

        mSharedPreferencesProxy.putObject(bluetoothMac, car);
    }


//    public void saveRecordingData(@NonNull RecordingData recordingData) {
//        mSharedPreferencesProxy.putObject(recordingData.getId(), recordingData);
//    }
//
//    public void removeRecordingData(@NonNull String recordingId) {
//        mSharedPreferencesProxy.remove(recordingId);
//    }
//
//    @Nullable
//    public RecordingData getRecordingData(@NonNull String recordingId) {
//        return mSharedPreferencesProxy.getObject(recordingId, RecordingData.class);
//    }

}
