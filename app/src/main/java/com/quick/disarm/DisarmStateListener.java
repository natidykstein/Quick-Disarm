package com.quick.disarm;

public interface DisarmStateListener {
    enum DisarmStatus {
        READY_TO_CONNECT, CONNECTING_TO_DEVICE, DEVICE_CONNECTED, DEVICE_DISCOVERED, RANDOM_READ_SUCCESSFULLY, DISARMED
    }

    void onDisarmStatusChange(DisarmStatus currentState, DisarmStatus newState);
}
