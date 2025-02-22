package com.quick.disarm;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.quick.disarm.infra.ILog;
import com.quick.disarm.infra.network.volley.IturanServerAPI;
import com.quick.disarm.utils.PreferenceCache;

import java.util.Arrays;
import java.util.HashSet;
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
        performDataResetIfNeeded(context);

        initAnalytics(context);
    }

    // PENDING: Remove method once all have been migrated to version > 1.2.3
    private void performDataResetIfNeeded(Context context) {
        final Set<String> carBluetoothSet = PreferenceCache.get(context).getCarBluetoothSet();
        final String carBluetooth = !carBluetoothSet.isEmpty() ? carBluetoothSet.iterator().next() : null;
        final Car anyCar = carBluetooth != null ? PreferenceCache.get(context).getCar(carBluetooth) : null;
        if (anyCar != null) {
            if (anyCar.getBluetoothTrigger() == null) {
                ILog.d("Detected old cars data - performing cleanup...");
                // Reset old cars data
                PreferenceCache.get(this).removeOldCarsData();
            }
        }
    }

    public static void initAnalytics(Context context) {
        Analytics.init(FirebaseAnalytics.getInstance(context));

        final Car anyCar = getAnyCar(context);
        if (anyCar != null) {
            final String phoneNumber = anyCar.getPhoneNumber();
            if (phoneNumber != null) {
                FirebaseAnalytics.getInstance(context).setUserId(phoneNumber);
                FirebaseCrashlytics.getInstance().setUserId(phoneNumber);
                ILog.d("Set analytics user id = " + phoneNumber);
            } else {
                ILog.w("Got a car without a phone number - due to an older app version");
            }

            final Set<String> carBluetoothSet = PreferenceCache.get(context).getCarBluetoothSet();
            final Set<String> licensePlates = new HashSet<>();
            for (String bt : carBluetoothSet) {
                final Car car = PreferenceCache.get(context).getCar(bt);
                if (car != null) {
                    licensePlates.add(car.getLicensePlate());
                } else {
                    ILog.e("Failed to get car for configured bt address: " + bt);
                }
            }

            // Add license plates as custom user property
            final String licensePlateSetAsString = Arrays.toString(licensePlates.toArray());
            FirebaseAnalytics.getInstance(context).setUserProperty(QuickDisarmAnalytics.USER_PROPERTY_LICENSE_PLATES, licensePlateSetAsString);
            FirebaseCrashlytics.getInstance().setCustomKey(QuickDisarmAnalytics.USER_PROPERTY_LICENSE_PLATES, licensePlateSetAsString);
        } else {
            ILog.d("No configured cars found - not setting analytics users id");
        }
    }

    private static Car getAnyCar(Context context) {
        final Set<Car> carSet = PreferenceCache.get(context).getCarSet();
        return !carSet.isEmpty() ? carSet.iterator().next() : null;
    }
}
