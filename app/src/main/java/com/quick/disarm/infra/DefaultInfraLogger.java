package com.quick.disarm.infra;

import android.util.Log;

public class DefaultInfraLogger
        implements InfraLogger {

    @Override
    public void v(String msg) {
        Log.v(ILog.TAG, msg);
    }

    @Override
    public void d(String msg) {
        Log.d(ILog.TAG, msg);
    }

    @Override
    public void i(String msg) {
        Log.i(ILog.TAG, msg);
    }

    @Override
    public void w(String msg) {
        Log.w(ILog.TAG, msg);
    }

    @Override
    public void e(String msg) {
        Log.e(ILog.TAG, msg);
    }
}
