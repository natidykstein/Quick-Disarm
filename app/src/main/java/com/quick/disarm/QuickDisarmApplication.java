package com.quick.disarm;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

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
    }
}
