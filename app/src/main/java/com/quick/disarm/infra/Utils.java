package com.quick.disarm.infra;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.quick.disarm.QuickDisarmApplication;
import com.scottyab.rootbeer.RootBeer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created on 22/02/2016 6:26 PM.
 */
public class Utils {

    private static final String UTILS_SHARED_PREF_FILE_NAME = "gong_utils_shared_pref.db";
    private static final String SYSTEM_PROPERTY_HTTP_AGENT = "http.agent";

    private static final Gson sGson;
    private static final Handler sUiHandler;
    private static Toast sNetworkErrorToast;

    public static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    interface ApplicationPatterns {
        // Gets the current application version M.m.p.
        Pattern APPLICATION_VERSION = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)\\.\\d+$");

        // Gets the current application build number.
        Pattern APPLICATION_BUILD_NUMBER = Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.(\\d+)$");
    }

    static {
        sGson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, type, jsonDeserializationContext) -> Instant.ofEpochSecond(json.getAsJsonPrimitive().getAsLong()))
                .create();
        sUiHandler = new Handler(Looper.getMainLooper());
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Generates a bitmap capturing a provided view, this is currently not in use, but can be used for deals -> account page animations
     *
     * @param view - The view to be captured
     * @return - Bitmap which holds the view image
     */
    public static Bitmap generateBitmapFromView(View view) {
        final int width = view.getMeasuredWidth();
        final int height = view.getMeasuredHeight();
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(bitmap);
        view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        view.draw(c);
        return bitmap;
    }

    public static boolean saveBitmapToStorage(Context context, String destinationFileName, Bitmap generateBitmapFromView) {
        try {
            final File destinationFile = new File(getFullFilePath(context, destinationFileName));
            final FileOutputStream stream = new FileOutputStream(destinationFile);
            final boolean saveBitmapResult = generateBitmapFromView.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            generateBitmapFromView.recycle();
            return saveBitmapResult;
        } catch (IOException e) {
            ILog.logException(e);
            return false;
        }
    }

    public static Bitmap loadBitmapFromFile(Context context, String fileName) {
        final String destinationFilePath = Uri.fromFile(new File(getFullFilePath(context, fileName))).getPath();
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(destinationFilePath, options);
    }

    private static String getFullFilePath(Context context, String fileName) {
        return context.getFilesDir().getAbsolutePath() + File.separatorChar + fileName;
    }

    interface UrlPatterns {

        /**
         * Regular expression pattern to match deep links urls with custom protocol
         */
        Pattern DEEP_LINK_URL = Pattern.compile("\\w+://.+");

        /**
         * Regular expression pattern to match web links urls with http/https protocol
         */
        Pattern WEB_LINK_URL = Pattern.compile("http(s)?://.+");
    }

    public static Gson getSharedGson() {
        return sGson;
    }

    public static String toJson(Gson gson, Object object) {
        return gson.toJson(object);
    }

    public static String toJson(Object object) {
        return toJson(sGson, object);
    }

    public static <T> T fromJson(Gson gson, String jsonString, Class<T> classType) {
        return gson.fromJson(jsonString, classType);
    }

    public static <T> T fromJson(Gson gson, String jsonString, TypeToken<T> typeOfT) {
        return gson.fromJson(jsonString, typeOfT.getType());
    }

    public static <T> T fromJson(String jsonString, Class<T> classType) {
        return fromJson(sGson, jsonString, classType);
    }

    public static <T> T fromJson(String jsonString, TypeToken<T> typeOfT) {
        return fromJson(sGson, jsonString, typeOfT);
    }

    public static JsonObject fromJson(String jsonString) {
        try {
            return JsonParser.parseString(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            ILog.logException(e);
            return null;
        }
    }

    public static void runOnUiThread(final Runnable runnable) {
        sUiHandler.post(runnable);
    }

    /**
     * Gets the current application version M.m.p.
     *
     * @param context the context
     * @return the current application version M.m.p
     */
    @Nullable
    public static String getApplicationVersion(Context context) {
        final String versionName = getApplicationVersionName(context);
        final Matcher matcher = ApplicationPatterns.APPLICATION_VERSION.matcher(versionName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Gets the current application build number.
     *
     * @param context the context
     * @return the current application build number
     */
    @Nullable
    public static String getApplicationBuildNumber(Context context) {
        final String versionName = getApplicationVersionName(context);
        final Matcher matcher = ApplicationPatterns.APPLICATION_BUILD_NUMBER.matcher(versionName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Gets the current application version code.
     *
     * @param context the context
     * @return the application version code
     */
    public static long getApplicationVersionCode(Context context) {
        return getApplicationVersionCode(context, context.getPackageName());
    }

    /**
     * Gets the application version name.
     *
     * @param context the context
     * @return the application version code
     */
    public static String getApplicationVersionName(Context context) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /**
     * Gets the specified application version code.
     *
     * @param context the context
     * @return the application version code
     */
    public static long getApplicationVersionCode(Context context, String packageName) {
        try {
            final PackageInfo pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return PackageInfoCompat.getLongVersionCode(pInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public static boolean isDownloadManagerEnabled(Context context) {
        final ApplicationInfo applicationInfo = getApplicationInfo(context, "com.android.providers.downloads");
        return applicationInfo != null && applicationInfo.enabled;
    }

    private static ApplicationInfo getApplicationInfo(Context context, String packageName) {
        try {
            final PackageManager packageManager = context.getPackageManager();
            return packageManager.getApplicationInfo(packageName, 0);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @return The base URL (without the query parameters)
     */
    public static String getBaseUrl(String url) {
        return url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
    }

    public static boolean isConnectedToNetwork(Context context) {
        final NetworkCapabilities ni = getActiveNetworkCapabilities(context);
        return ni != null && ni.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && ni.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public static boolean isRoamingNetwork(Context context) {
        final NetworkCapabilities nc = getActiveNetworkCapabilities(context);
        return nc != null && !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
    }

    public static boolean isConnectedToMobileNetwork(Context context) {
        final NetworkCapabilities nc = getActiveNetworkCapabilities(context);
        return isConnectedToNetwork(context) && nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    private static NetworkCapabilities getActiveNetworkCapabilities(Context context) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getNetworkCapabilities(cm.getActiveNetwork());
    }

    public enum ConnectionType {
        NONE,
        CELLULAR,
        WIFI,
        UNKNOWN
    }

    @NonNull
    public static ConnectionType getConnectionType() {
        if (!isConnectedToNetwork(QuickDisarmApplication.getAppContext())) {
            return ConnectionType.NONE;
        }

        final NetworkCapabilities nc = getActiveNetworkCapabilities(QuickDisarmApplication.getAppContext());
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return ConnectionType.CELLULAR;
        } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return ConnectionType.WIFI;
        }
        return ConnectionType.UNKNOWN;
    }

    /**
     * Validates the specified URL.
     *
     * @param url A URL for a web link
     * @return true if the specified url is valid
     */
    public static boolean isValidUrl(String url) {
        return isValidUrl(url, false);
    }

    /**
     * Validates the specified URL.
     *
     * @param url                    A URL for a web link
     * @param supportCustomProtocols if true then custom protocols are also considered valid(xxx://path)
     * @return true if the specified url is valid
     */
    public static boolean isValidUrl(String url, boolean supportCustomProtocols) {
        if (url == null) {
            return false;
        }

        final Pattern pattern = supportCustomProtocols ? UrlPatterns.DEEP_LINK_URL : UrlPatterns.WEB_LINK_URL;
        return pattern.matcher(url)
                .matches();
    }

    public static boolean waitThread(Object monitor, long timeout) {
        final long start = SystemClock.uptimeMillis();
        synchronized (monitor) {
            try {
                monitor.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        final long waitingTime = SystemClock.uptimeMillis() - start;
        ILog.d("Thread awaken after " + waitingTime + "ms");
        return waitingTime >= timeout;
    }

    public static void wakeupThread(Object monitor) {
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    public static boolean isMainLooperThread() {
        return Looper.getMainLooper()
                .getThread() == Thread.currentThread();
    }

    public static void verifyWorkerThread() {
        if (isMainLooperThread()) {
            throw new IllegalStateException("Cannot invoke this method from UI thread!");
        }
    }

    public static void verifyUIThread() {
        if (!isMainLooperThread()) {
            throw new IllegalStateException("Cannot invoke this method from worker thread!");
        }
    }

    public static boolean equals(SparseArray<?> arr, SparseArray<?> arr2) {
        if ((arr == null && arr2 != null) || (arr != null && arr2 == null)) {
            return false;
        }

        if (arr == arr2) {
            return true;
        }

        if (arr.size() != arr2.size()) {
            return false;
        }

        for (int i = 0; i < arr.size(); i++) {
            if (!Objects.equals(arr.keyAt(i), arr2.keyAt(i)) || !Objects.equals(arr.valueAt(i), arr2.valueAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static File copyFile(String sourcePath, String targetFolder) throws
            IOException {
        return copyFile(sourcePath, targetFolder, null, null);
    }

    public static File copyFile(String sourcePath, String targetFolder, Executor executor, FileUtils.ProgressListener progressListener) throws IOException {
        final File inputFile = new File(sourcePath);
        final File outputFile = new File(targetFolder, inputFile.getName());

        // Create path if necessary
        Optional.ofNullable(outputFile.getParentFile()).ifPresent(fileParent -> {
            if (fileParent.mkdirs()) {
                ILog.d("Successfully created directory structure " + outputFile.getParent());
            }
        });

        try (final FileInputStream inputStream = new FileInputStream(sourcePath); final FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            FileUtils.copy(inputStream, outputStream, null, executor, progressListener);
        }

        return outputFile;
    }

    private static void copyFile(InputStream inputStream, OutputStream outputStream) throws
            IOException {
        // 32K is the ideal buffer size.
        // See https://stackoverflow.com/questions/10143731/android-optimal-buffer-size
        final byte[] buffer = new byte[32 * 1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
        outputStream.close();

        inputStream.close();
    }

    public static int getScreenWidth(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowManager windowManager = context.getSystemService(WindowManager.class);
            return windowManager.getCurrentWindowMetrics().getBounds().width();
        } else {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            getMetricsBeforeApi30(context, displayMetrics);
            return displayMetrics.widthPixels;
        }
    }

    @SuppressWarnings("deprecation")
    private static void getMetricsBeforeApi30(Context context, DisplayMetrics displayMetrics) {
        getDisplay(context).getMetrics(displayMetrics);
    }

    public static Drawable getTintedDrawable(Context context, Drawable sourceDrawable, @ColorRes int colorResource) {
        final Drawable tintedDrawable = DrawableCompat.wrap(sourceDrawable.mutate());
        DrawableCompat.setTint(tintedDrawable, ResourcesCompat.getColor(context.getResources(), colorResource, null));
        return tintedDrawable;
    }

    public static int convertDpToPixels(int dp) {
        return convertDpToPixels(QuickDisarmApplication.getAppContext(), dp);
    }

    public static int convertSpToPixels(int sp) {
        return convertSpToPixels(QuickDisarmApplication.getAppContext(), sp);
    }

    public static int convertDpToPixels(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int convertSpToPixels(Context context, int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param pixels  A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(Context context, int pixels) {
        return pixels / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * @param timeMs
     * @return The time in the format of 'HH:MM' (For example - '12:34')
     */
    public static String formatTime(final long timeMs) {
        final long normalizedTimeSeconds = timeMs <= 0 ? 0 : timeMs / 1000;
        return DateUtils.formatElapsedTime(normalizedTimeSeconds);
    }

    /**
     * @param durationMs
     * @return The duration in format of 'HHh MMm' (For example - '1h 32m')
     */
    public static String formatDuration(long durationMs) {
        final long normalizedTimeSeconds = durationMs <= 0 ? 0 : durationMs / 1000;
        final long hours = normalizedTimeSeconds / 3600;
        // Anything less than a minute is considered 1 minutes (except 0 seconds)
        final long minutes = (long) Math.ceil((normalizedTimeSeconds % 3600) / 60d);

        return hours > 0 ? String.format(Locale.US, "%dh %dm", hours, minutes)
                : String.format(Locale.US, "%dm", minutes);
    }

    /**
     * @param durationInSeconds
     * @return The duration in format of 'HHh MMm' (For example - '1h 32m')
     */
    public static String formatDurationFromSeconds(long durationInSeconds) {
        final long normalizedTimeSeconds = Math.max(durationInSeconds, 0);
        final long hours = normalizedTimeSeconds / 3600;
        // Anything less than a minute is considered 1 minutes (except 0 seconds)
        final long minutes = (long) Math.ceil((normalizedTimeSeconds % 3600) / 60d);

        return hours > 0 ? String.format(Locale.US, "%dh %dm", hours, minutes)
                : String.format(Locale.US, "%dm", minutes);
    }

    public static float getDeviceCurrentVolumeNormalized(Context context) {
        final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        return (float) currentVolume / maxVolume;
    }

    /**
     * We need to check if app is in foreground otherwise the app will crash.
     * http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
     *
     * @param context Context
     * @return boolean
     */
    public static boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return false;

        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }

        return false;
    }

    public static void animateScaleView(View v, float startScale, float endScale, long duration) {
        final Animation anim = new ScaleAnimation(
                startScale, endScale, // Start and end values for the X axis scaling
                startScale, endScale, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setDuration(duration);
        anim.setInterpolator(new OvershootInterpolator());
        v.startAnimation(anim);
    }

    public static boolean hasPermission(Context context, String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static Animation animateBlinking(int duration) {
        final Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(duration);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        return animation;
    }

    public static void setKeyboardVisible(View view, boolean showKeyboard) {
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (showKeyboard) {
                imm.showSoftInput(view, 0);
            } else {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static String formatNumber(int number) {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(number);
    }


    /**
     * Moves file to given folder. If the folder doesn't exist, it will be created.
     */
    public static boolean moveFile(File sourceFile, File targetFolder) {
        if (!targetFolder.exists()) {
            if (!targetFolder.mkdirs()) {
                ILog.e("Failed to create target folder: " + targetFolder);
                return false;
            }
        }

        final File targetFile = new File(targetFolder, sourceFile.getName());
        return sourceFile.renameTo(targetFile);
    }

    public static String concatOptionalStrings(String s1, String s2, String separator) {
        if (!TextUtils.isEmpty(s1) && !TextUtils.isEmpty(s2)) {
            return s1 + separator + s2;
        } else {
            if (!TextUtils.isEmpty(s1)) {
                return s1;
            } else if (!TextUtils.isEmpty(s2)) {
                return s2;
            } else {
                return "";
            }
        }
    }

    /**
     * Create directory if not exists.
     */
    @Nullable
    public static File createOrGetDir(@NonNull String dirName) {
        if (TextUtils.isEmpty(dirName)) {
            return null;
        }
        final String dirPath = QuickDisarmApplication.getAppContext().getFilesDir() + File.separator + dirName;
        final File dir = new File(dirPath);
        if (dir.exists()) {
            return dir;
        }
        return dir.mkdir() ? dir : null;
    }

    public static void disableTemporarily(final View view, int delayMs) {
        view.setEnabled(false);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setEnabled(true);
            }
        }, delayMs);
    }

    public static void fitDialogWidthToScreen(Activity activity) {
        // Initialize a new window manager layout parameters
        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(activity.getWindow().getAttributes());
        layoutParams.width = (int) (getScreenWidth(activity) * 0.85f);

        // Apply the newly created layout parameters to the alert dialog window
        activity.getWindow().setAttributes(layoutParams);
    }

    public static void openAppSettingsScreen(Activity activity) {
        try {
            activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.getPackageName(), null)));
        } catch (ActivityNotFoundException e) {
            ILog.e("Failed to open app settings screen: " + e.toString());
        }
    }

    /**
     * Return the device user agent in the safest way known to mankind
     *
     * @param context
     * @return
     */
    public static String safeGetWebViewDefaultUserAgent(Context context) {
        try {
            return WebSettings.getDefaultUserAgent(context);
        } catch (Exception e) {
            // Method can throw PackageManager.NameNotFoundException in due to a firmware bug
            // see https://bugs.chromium.org/p/chromium/issues/detail?id=506369#c3
            try {
                return new WebView(context).getSettings()
                        .getUserAgentString();
            } catch (Exception e1) {
                // Fallback to system property in case webview init fails
                try {
                    return System.getProperty(SYSTEM_PROPERTY_HTTP_AGENT);
                } catch (SecurityException e3) {
                    // Shouldn't happen, but just in case
                    return null;
                }
            }
        }
    }

    /**
     * Creates and launches an ACTION_VIEW intent
     *
     * @param context The context upon which to start the activity
     * @param url     A String representing a valid web address. Must contain a scheme
     */
    public static void openUrl(Context context, String url) {
        openUrl(context, Uri.parse(url));
    }

    public static void openUrl(Context context, Uri uri) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No URL browsing activity found", Toast.LENGTH_SHORT).show();
            ILog.d(e.toString());
        }
    }

    static boolean launchWithNativeAppApi30(Context context, Uri uri) {
        ILog.d("Detected Android>=11, opening custom tab using native app");
        @SuppressLint("InlinedApi") Intent nativeAppIntent = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER);
        try {
            context.startActivity(nativeAppIntent);
            return true;
        } catch (ActivityNotFoundException ex) {
            ILog.w("No activity found to launch intent");
            return false;
        }
    }

    private static boolean launchWithNativeAppBeforeApi30(Context context, Uri uri) {
        ILog.d("Detected Android<11, opening custom tab using native app");
        final PackageManager pm = context.getPackageManager();

        // Get all Apps that resolve a generic url
        final Intent browserActivityIntent = new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.fromParts("http", "", null));
        final Set<String> genericResolvedList = extractPackageNames(
                pm.queryIntentActivities(browserActivityIntent, 0));

        // Get all apps that resolve the specific Url
        final Intent specializedActivityIntent = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE);
        final Set<String> resolvedSpecializedList = extractPackageNames(
                pm.queryIntentActivities(specializedActivityIntent, 0));

        // Keep only the Urls that resolve the specific, but not the generic urls
        resolvedSpecializedList.removeAll(genericResolvedList);

        // If the list is empty, no native app handlers were found
        if (resolvedSpecializedList.isEmpty()) {
            return false;
        }

        // We found native handlers - launch the Intent
        specializedActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(specializedActivityIntent);
        return true;
    }

    private static Set<String> extractPackageNames(List<ResolveInfo> resolveInfoList) {
        final Set<String> packageNames = new HashSet<>();
        for (ResolveInfo info : resolveInfoList) {
            packageNames.add(info.resolvePackageName);
        }

        return packageNames;
    }

    // https://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in-the-emulator
    public static boolean isEmulator() {
        return (Build.FINGERPRINT.startsWith("google/sdk_gphone_")
                && Build.FINGERPRINT.endsWith(":user/release-keys")
                && Build.MANUFACTURER.equals("Google") && Build.PRODUCT.startsWith("sdk_gphone_") && Build.BRAND.equals("google")
                && Build.MODEL.startsWith("sdk_gphone_"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || "QC_Reference_Phone".equals(Build.BOARD) && !"Xiaomi".equalsIgnoreCase(Build.MANUFACTURER) // Bluestacks
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST.startsWith("Build") // MSI App Player
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.PRODUCT.equals("google_sdk");
    }

    /**
     * @return true if pass or pin or pattern locks screen
     */
    public static boolean isDeviceSecure(Context context) {
        final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE); //api 23+
        // Returns whether the device is secured with a PIN, pattern or password.
        return keyguardManager != null && keyguardManager.isDeviceSecure();
    }

    //https://medium.com/@scottyab/detecting-root-on-android-97803474f694
    //https://github.com/scottyab/rootbeer
    public static boolean isDeviceRooted(Context context) {
        return new RootBeer(context).isRooted();
    }

    // Allowing to separately check for dangerous props and exclude them from the root check
    public static boolean isOnlyDangerousPropsDetected(Context context) {
        final RootBeer rootBeer = new RootBeer(context);

        return rootBeer.checkForDangerousProps() && !(rootBeer.detectRootManagementApps() ||
                rootBeer.detectPotentiallyDangerousApps() || rootBeer.checkForSuBinary() ||
                rootBeer.checkForRWPaths() || rootBeer.detectTestKeys() ||
                rootBeer.checkSuExists() || rootBeer.checkForRootNative() || rootBeer.checkForMagiskBinary());
    }

    public static void reportRootIssue(Context context) {
        final RootBeer rootBeer = new RootBeer(context);

        ILog.w("RootBeer detected the following security issues:");

        if (rootBeer.detectRootManagementApps()) {
            ILog.w("Root management apps");
        }

        if (rootBeer.detectPotentiallyDangerousApps()) {
            ILog.w("Potentially dangerous apps");
        }

        if (rootBeer.checkForSuBinary()) {
            ILog.w("SU binary");
        }

        if (rootBeer.checkForDangerousProps()) {
            ILog.w("Dangerous Props");
        }

        if (rootBeer.checkForRWPaths()) {
            ILog.w("RW paths");
        }

        if (rootBeer.detectTestKeys()) {
            ILog.w("Test keys");
        }

        if (rootBeer.checkSuExists()) {
            ILog.w("SU");
        }

        if (rootBeer.checkForRootNative()) {
            ILog.w("Root Native");
        }

        if (rootBeer.checkForMagiskBinary()) {
            ILog.w("Magisk binary");
        }
    }

    public static String convertUriQueryToJsonString(Uri uri) {
        // Create the sanitizer and parse the uri
        final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
        sanitizer.setAllowUnregisteredParamaters(true);
        sanitizer.setUnregisteredParameterValueSanitizer(UrlQuerySanitizer.getSpaceLegal());
        sanitizer.parseUrl(uri.toString());

        // Convert uri query params to map (handles duplicate keys by overwriting old with new)
        // If a query parameter is null (as in highlights=null for example) -
        // we will get a "null" String value in the map (as in {"highlights":"null"}) so we need to filter those out
        final Map<String, String> queryMap = !sanitizer.getParameterList().isEmpty() ?
                sanitizer.getParameterList()
                        .stream()
                        .filter(p -> !p.mValue.equals("null"))
                        .collect(Collectors.toMap(p -> p.mParameter, p -> p.mValue.trim(), (k1, k2) -> k2)) : new HashMap<>();

        return Utils.getSharedGson().toJson(queryMap);
    }

    public static Map<String, String> bundleToMap(Bundle extras) {
        if (extras == null) {
            return null;
        }

        final Map<String, String> map = new HashMap<>();
        final Set<String> ks = extras.keySet();
        final Iterator<String> iterator = ks.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            map.put(key, extras.getString(key));
        }
        return map;
    }

    public static void reveal(View v) {
        activateRipple(v, 500, 2);
    }

    public static void activateRipple(View v, int durationMs, int iterations) {
        final int DELAY_BETWEEN_ITERATIONS_MS = 350;
        if (v != null && v.getBackground() instanceof RippleDrawable) {
            final RippleDrawable rippleDrawable = ((RippleDrawable) v.getBackground());
            rippleDrawable.setState(new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled});
            v.postDelayed(() -> {
                rippleDrawable.setState(new int[]{});
                if (iterations > 1) {
                    v.postDelayed(() -> activateRipple(v, durationMs, iterations - 1), DELAY_BETWEEN_ITERATIONS_MS);
                }
            }, durationMs);
        } else {
            ILog.w("Failed to activate ripple - view is null or not RippleDrawable");
        }
    }

    public static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static int getNavigationBarHeight(Activity activity) {
        if (!hasNavBar(activity)) {
            return 0;
        }
        final int resourceId = activity.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? activity.getResources().getDimensionPixelSize(resourceId) : 0;
    }

    // Inspired by https://stackoverflow.com/questions/37699161/detect-soft-navigation-bar-availability-in-oneplusone-device-progmatically
    public static boolean hasNavBar(Activity activity) {
        final boolean landscape = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        final Display display = getDisplay(activity);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        final int realSize = landscape ? displayMetrics.widthPixels : displayMetrics.heightPixels;
        final int displaySize = getDisplaySizeForNavCheck(activity, landscape);
        return (realSize - displaySize) > 0;
    }

    public static void createChannel(Context context, int channelIdResource, int channelNameResource, String description) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationChannel channel = new NotificationChannel(context.getString(channelIdResource),
                context.getString(channelNameResource),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(description);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
    }

    private static int getDisplaySizeForNavCheck(Context context, Boolean isLandscape) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowManager windowManager = context.getSystemService(WindowManager.class);
            final Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
            return isLandscape ? rect.width() : rect.height();
        } else {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            getMetricsBeforeApi30(context, displayMetrics);
            return isLandscape ? displayMetrics.widthPixels : displayMetrics.heightPixels;
        }
    }

    private static Display getDisplay(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return context.getDisplay();
        } else {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            @SuppressWarnings("deprecation")
            Display display = windowManager.getDefaultDisplay();

            return display;
        }
    }

    // This is the way Ituran is working - so we use it too
    // Just note that Settings.Secure.getString(paramContext.getContentResolver(), "android_id")
    // generates different value per app signing key and user.
    // (see https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID)
    public static String getDeviceUuid(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
