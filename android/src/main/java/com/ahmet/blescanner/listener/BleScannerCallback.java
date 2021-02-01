package com.ahmet.blescanner.listener;

import android.bluetooth.BluetoothDevice;

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
    public abstract boolean onDetectDevice(BluetoothDevice device);

    /**
     * @param isConnected cihaz ile bağlı olma durumu
     * @param device      bağlı cihaz bilgileri
     */
    public abstract void onConnectDevice(boolean isConnected, BluetoothDevice device);

}
