package com.ahmet.radar.thread;

import android.bluetooth.BluetoothGatt;

import com.ahmet.radar.listener.BleScannerCallback;

public class DisconnectGattThread extends Thread {

    BluetoothGatt gatt;
    BleScannerCallback callback;

    public DisconnectGattThread(BluetoothGatt bluetoothGatt, BleScannerCallback bleScannerCallback) {
        gatt = bluetoothGatt;
        callback = bleScannerCallback;
    }

    @Override
    public void run() {
        super.run();

        if (gatt != null) {
            try {

                gatt.disconnect();
                callback.onConnectDevice(false, null);

            } catch (Exception ignore) {
            }

        } else {
        }


    }
}
