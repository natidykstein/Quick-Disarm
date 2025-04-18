package com.quick.disarm.infra.network.volley;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.quick.disarm.infra.network.UriBuilder;
import com.quick.disarm.infra.network.volley.request.GsonElementRequest;
import com.quick.disarm.model.ActivationAnswer;
import com.quick.disarm.model.IsRegisteredAnswer;
import com.quick.disarm.model.SerializationAnswer;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is a proxy to the server API.<BR>
 * Every request that is not a GET request will be managed using Android's WorkManager.
 */
public class IturanServerAPI {

    public static String ANDROID_PLATFORM = "ANDROID";
    public static String ANDROID_APP_KEY = "8y0D91I1S@gGiuP358W3!J1y";

    public static final String SERVER_BASE_URL = "https://www.ituran.com/ituranmobileservice/mobileservice.asmx";

    private static final String CHECK_IF_REGISTERED = "MBKcheckIfRegistered";
    private static final String ACTIVATION = "MBKactivation";
    private static final String SERIALIZATION = "MBKSerializationRequest";


    private static volatile IturanServerAPI sInstance;
    private static Context sContext;

    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    private IturanServerAPI() {
        if (sContext == null) {
            throw new IllegalStateException("Must invoke IturanServerAPI.init(context) first!");
        }
    }

    public static IturanServerAPI get() {
        if (sInstance == null) {
            synchronized (IturanServerAPI.class) {
                if (sInstance == null) {
                    sInstance = new IturanServerAPI();
                }
            }
        }

        return sInstance;
    }

    public String getServerUrl() {
        return SERVER_BASE_URL;
    }

    public Request<AppResponse<IsRegisteredAnswer>> checkIsRegisteredDriver(String plate, String phoneNumber, Response.Listener<AppResponse<IsRegisteredAnswer>> responseListener, Response.ErrorListener errorListener) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("Plate", plate);
        queryParams.put("PhoneNumber", phoneNumber);
        final String url = UriBuilder.build(getServerUrl(), new String[]{CHECK_IF_REGISTERED}, queryParams).toString();
        return new GsonElementRequest<>(IsRegisteredAnswer.class, Request.Method.GET, url, null, responseListener, errorListener);
    }

    public Request<AppResponse<ActivationAnswer>> verifyDriver(String plate, String phoneNumber, String deviceUuid, Response.Listener<AppResponse<ActivationAnswer>> responseListener, Response.ErrorListener errorListener) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("Plate", plate);
        queryParams.put("PhoneNumber", phoneNumber);
        queryParams.put("Key", deviceUuid);

        final String url = UriBuilder.build(getServerUrl(), new String[]{ACTIVATION}, queryParams).toString();
        final GsonElementRequest<ActivationAnswer> request = new GsonElementRequest<>(ActivationAnswer.class, Request.Method.GET, url, null, responseListener, errorListener);

        return VolleyRequestManager.INSTANCE.enqueueRequest(sContext, request);
    }

    public Request<AppResponse<SerializationAnswer>> validateSmsVerificationCode(String plate, String phoneNumber, String otpCode, Response.Listener<AppResponse<SerializationAnswer>> responseListener, Response.ErrorListener errorListener) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("Plate", plate);
        queryParams.put("PhoneNumber", phoneNumber);
        queryParams.put("OTPcode", otpCode);

        final String url = UriBuilder.build(getServerUrl(), new String[]{SERIALIZATION}, queryParams).toString();
        final GsonElementRequest<SerializationAnswer> request = new GsonElementRequest<>(SerializationAnswer.class, Request.Method.GET, url, null, responseListener, errorListener);

        return VolleyRequestManager.INSTANCE.enqueueRequest(sContext, request);
    }
}
