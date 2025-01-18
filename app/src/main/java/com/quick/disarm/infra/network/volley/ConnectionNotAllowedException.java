package com.quick.disarm.infra.network.volley;

import com.android.volley.AuthFailureError;

/**
 * Created on 07/03/2016 6:31 PM.
 */
public class ConnectionNotAllowedException
        extends AuthFailureError {

    public ConnectionNotAllowedException(String exceptionMessage) {
        super(exceptionMessage);
    }
}
