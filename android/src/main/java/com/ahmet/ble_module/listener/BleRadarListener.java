package com.ahmet.ble_module.listener;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

public interface BleRadarListener {


    void onRadarDiscoveryService(String jsonStr);

    void onRadarGetterCharacteristic(List<BluetoothGattCharacteristic> gattCharacteristicList);

    void onRadarReadReadCharacteristic(BluetoothGattCharacteristic characteristic,String data);

    void onRadarWriteReadCharacteristic(BluetoothGattCharacteristic characteristic);
}
