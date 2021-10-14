package com.ahmet.ble_module.listener;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

public interface BleRadarListener {

    void onRadarChangedScannerStatus(boolean status);

    void onRadarDetectedDevice(BluetoothDevice device,int rssi);

    void onRadarDiscoveryService(String jsonStr);

    void onRadarGetterCharacteristic(List<BluetoothGattCharacteristic> gattCharacteristicList);

    void onRadarReadReadCharacteristic(BluetoothGattCharacteristic characteristic,String data);

    void onRadarWriteReadCharacteristic(BluetoothGattCharacteristic characteristic);
}
