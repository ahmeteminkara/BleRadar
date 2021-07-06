package com.ahmet.radar.listener;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

abstract public class BleServiceCallback {
    /**
     * @param status
     * @param gatt
     */
    public abstract void onDetectServices(
            boolean status,
            BluetoothGatt gatt);

    /**
     * @param status
     * @param serviceUUID servis id
     * @param characteristic hizmet verisi
     * @param data hizmetten gelen veri
     */
    public abstract void onCharacteristicRead(
            boolean status,
            UUID serviceUUID,
            BluetoothGattCharacteristic characteristic,
            String data);


    public abstract void onCharacteristicWrite(
            boolean status,
            UUID serviceUUID,
            BluetoothGattCharacteristic characteristic);
}
