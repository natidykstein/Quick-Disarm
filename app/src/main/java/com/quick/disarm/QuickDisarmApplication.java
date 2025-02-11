package com.quick.disarm;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.quick.disarm.infra.ILog;
import com.quick.disarm.infra.network.volley.IturanServerAPI;
import com.quick.disarm.utils.PreferenceCache;

import java.util.Set;

public final class QuickDisarmApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static Context getAppContext() {
        return QuickDisarmApplication.context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

        IturanServerAPI.init(context);

        initAnalytics();
    }

    private void initAnalytics() {
        Analytics.init(FirebaseAnalytics.getInstance(this));

        final Car car = getAnyCar();
        if (car != null) {
            final String phoneNumber = car.getPhoneNumber();
            if (phoneNumber != null) {
                FirebaseAnalytics.getInstance(this).setUserId(phoneNumber);
                ILog.d("Set analytics users id = " + phoneNumber);
            } else {
                ILog.w("Got a car without a phone number - due to an older app version");
            }
        } else {
            ILog.d("No configured cars found - not setting analytics users id");
        }
    }

    private Car getAnyCar() {
        final Set<String> carBluetoothSet = PreferenceCache.get(this).getCarBluetoothSet();
        final String carBluetooth = !carBluetoothSet.isEmpty() ? carBluetoothSet.iterator().next() : null;
        return carBluetooth != null ? PreferenceCache.get(this).getCar(carBluetooth) : null;
    }
}
