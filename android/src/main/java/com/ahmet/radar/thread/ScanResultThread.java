package com.ahmet.radar.thread;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import com.ahmet.radar.BleScanner;
import com.ahmet.radar.listener.BleScannerCallback;


public class ScanResultThread extends Thread {


    BluetoothDevice device;
    BleScannerCallback callback;
    int deviceRssi;
    int maxRssi;

    public ScanResultThread(ScanResult result, int maxRssi, BleScannerCallback bleScannerCallback) {
        device = result.getDevice();
        deviceRssi = result.getRssi();
        callback = bleScannerCallback;
        this.maxRssi = maxRssi;
    }

    public ScanResultThread(BluetoothDevice device,int deviceRssi, int maxRssi, BleScannerCallback bleScannerCallback) {
        this.device = device;
        this.deviceRssi = deviceRssi;
        callback = bleScannerCallback;
        this.maxRssi = maxRssi;
    }

    @Override
    public void run() {
        super.run();


        if (deviceRssi < 0 && deviceRssi > maxRssi) {
            Log.d(BleScanner.TAG, "cihaz bulundu -: " + device.getName() + ", " + deviceRssi);
            callback.onDetectDevice(device, deviceRssi);
            /*
            boolean autoConnect =
            if (autoConnect) {
                stop();
                new ConnectGattThread(activity,scanResult.getDevice(),)
            }
             */


        }
    }
}
