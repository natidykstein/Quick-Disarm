package com.quick.disarm;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class ReportAnalytics {

    private static FirebaseAnalytics sFirebaseAnalytics;

    public static void init(FirebaseAnalytics firebaseAnalytics) {
        sFirebaseAnalytics = firebaseAnalytics;
    }

    public static void reportSelectButtonEvent(String id, String name) {
        final Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, AnalyticsConstants.CONTENT_BUTTON);
        sFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public static void reportEventWithMetric(String eventName, String key, long value) {
        final Bundle bundle = new Bundle();
        bundle.putLong(key, value);
        sFirebaseAnalytics.logEvent(eventName, bundle);
    }
}
