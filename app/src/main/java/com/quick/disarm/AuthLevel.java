package com.quick.disarm;

/**
 * Allow user to select between 3 levels of authentication:
 * App(most secured) - Each app open will require an authentication
 * Device(normal/default) - The device must be unlocked to allow disarming
 * None(less secured) - Car can be disarmed in the background while the device is locked
 */
public enum AuthLevel {APP, DEVICE, NONE}
