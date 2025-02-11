package com.quick.disarm;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class Analytics {

    private static FirebaseAnalytics sFirebaseAnalytics;

    public static void init(FirebaseAnalytics firebaseAnalytics) {
        sFirebaseAnalytics = firebaseAnalytics;
    }

    public static void reportSelectButtonEvent(String id, String name) {
        final Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, QuickDisarmAnalytics.CONTENT_BUTTON);
        sFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public static void reportEvent(String eventName, String key, String value) {
        final Bundle bundle = new Bundle();
        bundle.putString(key, value);
        sFirebaseAnalytics.logEvent(eventName, bundle);
    }
}
