package com.quick.disarm.infra;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.quick.disarm.BuildConfig;
import com.quick.disarm.infra.network.volley.VolleyUtils;

public class ILog {

    static final String TAG = "QuickDisarm";

    private static InfraLogger sLogger = new DefaultInfraLogger();

    //==========================================================================
    // 							public methods
    //==========================================================================

    public static InfraLogger getLogger() {
        return sLogger;
    }

    public static void setLogger(InfraLogger logger) {
        sLogger = logger;
    }

    public static void v(String text) {
        doLog(text, Log.VERBOSE);
    }

    public static void d(String text) {
        doLog(text, Log.DEBUG);
    }

    public static void i(String text) {
        doLog(text, Log.INFO);
    }

    public static void w(String text) {
        doLog(text, Log.WARN);
    }

    public static void e(String text) {
        doLog(text, Log.ERROR);
    }

    public static void logException(Throwable t, boolean report) {
        e(t.toString());

        if (report && shouldReport(t)) {
            FirebaseCrashlytics.getInstance().recordException(t);
        }
    }

    public static void logException(Throwable t) {
        logException(t, true);
    }

    public static void logException(String message) {
        logException(new Throwable(message));
    }

    private static boolean shouldReport(Throwable t) {
        // Don't report network-related or 401/403 errors
        return !VolleyUtils.isNetworkRelatedError(t) && !VolleyUtils.isUnauthorized(t) && !VolleyUtils.isForbidden(t);
    }

    public static void logPerformance(String action, long startNanos) {
        final long THRESHOLD_MS = 10;
        final long millis = (System.nanoTime() - startNanos) / 1000000;
        final String logMessage = action + " in " + millis + "ms";
        doLog(logMessage, millis > THRESHOLD_MS ? Log.INFO : Log.VERBOSE);
    }

    //==========================================================================
    // 							private static methods
    //==========================================================================

    private static void doLog(String logText, int logLevel) {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        //take stackTrace element at 4 because:
        //0: dalvik.system.VMStack.getThreadStackTrace(Native Method) / ART
        //1: java.lang.Thread.getStackTrace(Thread.java:579)
        //2: Logger.doLog()
        //3: either one of the log calls (v,d...)
        //4: this is the calling method!
        if (stackTrace.length > 4) {
            StackTraceElement element = stackTrace[4];

            String fullClassName = element.getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1); // no package
            String threadName = Thread.currentThread().getName();

            //add class and method data to logText
            logText = "T:" + threadName + " " + className + "->" + element.getMethodName() + "(): " + logText;
        }

        FirebaseCrashlytics.getInstance().log(logText);

        if (shouldLogToConsole()) {
            switch (logLevel) {
                case Log.VERBOSE:
                    sLogger.v(logText);
                    break;
                case Log.DEBUG:
                    sLogger.d(logText);
                    break;
                case Log.INFO:
                    sLogger.i(logText);
                    break;
                case Log.WARN:
                    sLogger.w(logText);
                    break;
                case Log.ERROR:
                    sLogger.e(logText);
                    break;
            }
        }
    }

    /**
     * Allow printing logs to console in the following conditions:
     * 1. Version is not a release version (LOGS_ENABLED = false for release version)
     * 2. Version is release and user is not logged in
     * 3. Version is release and user is logged in as a Gong user
     * 4. We're currently impersonating
     * <p>
     * Note: Logs are always sent to Sentry as breadcrumbs
     *
     * @return true if logs should be printed to the console (standard output)
     */
    private static boolean shouldLogToConsole() {
        if (sLogger == null) {
            return false;
        }

        return BuildConfig.LOGS_ENABLED;
    }
}
