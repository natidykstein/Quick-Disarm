package com.quick.disarm.register;

import java.util.Objects;

public class BluetoothDeviceItem {
    private String name;
    private String address;

    public BluetoothDeviceItem(String name, String address) {
        this.name = name;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BluetoothDeviceItem that = (BluetoothDeviceItem) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address);
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
