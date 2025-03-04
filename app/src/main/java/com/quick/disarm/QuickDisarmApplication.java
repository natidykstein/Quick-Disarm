package com.quick.disarm;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
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
        performDataResetIfNeeded(context);

        initAnalytics(context);
    }

    // PENDING: Remove method once all have been migrated to version > 1.2.3
    private void performDataResetIfNeeded(Context context) {
        final Set<String> carBluetoothSet = PreferenceCache.get(context).getCarBluetoothSet();
        final String carBluetooth = !carBluetoothSet.isEmpty() ? carBluetoothSet.iterator().next() : null;
        final Car anyCar = carBluetooth != null ? PreferenceCache.get(context).getCar(carBluetooth) : null;
        if (anyCar != null) {
            if (anyCar.getTriggerBluetoothAddress() == null) {
                ILog.d("Detected old cars data - performing cleanup...");
                // Reset old cars data
                PreferenceCache.get(this).removeOldCarsData();
            }
        }
    }

    public static void initAnalytics(Context context) {
        ReportAnalytics.init(FirebaseAnalytics.getInstance(context));

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

            final String licensePlatesAsString = getLicensePlatesAsString();
            FirebaseAnalytics.getInstance(context).setUserProperty(AnalyticsConstants.USER_PROPERTY_LICENSE_PLATES, licensePlatesAsString);
            FirebaseCrashlytics.getInstance().setCustomKey(AnalyticsConstants.USER_PROPERTY_LICENSE_PLATES, licensePlatesAsString);
        } else {
            ILog.d("No configured cars found - not setting analytics users id");
        }
    }

    // Add license plates and their corresponding bluetooth trigger as custom user property
    @NonNull
    private static String getLicensePlatesAsString() {
        final Set<Car> carSet = PreferenceCache.get(context).getCarSet();
        final StringBuilder licensePlates = new StringBuilder();
        for(Car car: carSet) {
            final String licensePlate = car.getFormattedLicensePlate();
            final String triggerBluetoothName = car.getTriggerBluetoothName();
            licensePlates.append("[").append(licensePlate).append(":").append(triggerBluetoothName).append(", ");
        }
        licensePlates.append("]");

        final String licensePlatesAsString = licensePlates.substring(0, licensePlates.length()-2);
        return licensePlatesAsString;
    }

    private static Car getAnyCar(Context context) {
        final Set<Car> carSet = PreferenceCache.get(context).getCarSet();
        return !carSet.isEmpty() ? carSet.iterator().next() : null;
    }
}
