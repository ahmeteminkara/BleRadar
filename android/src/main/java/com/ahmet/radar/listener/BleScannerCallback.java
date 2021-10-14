package com.ahmet.radar.listener;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/**
 * Hizmet, servisin içindeki "characteristic"
 * (BluetoothGattCharacteristic)
 */

public abstract class BleScannerCallback {
    /**
     * @param status tarama işleminin durumunu belirtir
     */
    public abstract void onScanning(boolean status);

    /**
     * @param device bulunan cihaz
     * @return cihaza otomatik bağlanma
     */
    public abstract boolean onDetectDevice(BluetoothDevice device,int rssi);

    /**
     * @param isConnected cihaz ile bağlı olma durumu
     * @param device      bağlı cihaz bilgileri
     */
    public abstract void onConnectDevice(boolean isConnected, BluetoothDevice device);

    /**
     * @param isConnected cihaz ile bağlı olma durumu
     * @param gatt      bağlı olan gatt ı verir
     */
    public abstract void onConnectGatt(boolean isConnected, BluetoothGatt gatt);

}
