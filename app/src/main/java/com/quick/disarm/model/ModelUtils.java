package com.quick.disarm.model;

public class ModelUtils {
    public static final boolean isValid(String errorCode) {
        return "OK".equalsIgnoreCase(errorCode);
    }
}
