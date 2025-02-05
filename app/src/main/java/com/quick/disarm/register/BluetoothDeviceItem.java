package com.quick.disarm.register;

public class BluetoothDeviceItem {
    private String name;
    private String address;

    public BluetoothDeviceItem(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
