package com.ahmet.radar.thread;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import com.ahmet.radar.BleScanner;
import com.ahmet.radar.listener.BleScannerCallback;

public class ConnectGattThread extends Thread {

    Activity activity;
    BluetoothDevice device;
    BluetoothGattCallback gattCallback;
    BleScannerCallback callback;


    public ConnectGattThread(
            Activity activity,
            BluetoothDevice bluetoothDevice,
            BluetoothGattCallback bluetoothGattCallback,
            BleScannerCallback bleScannerCallback
    ) {
        this.activity = activity;
        device = bluetoothDevice;
        gattCallback = bluetoothGattCallback;
        callback = bleScannerCallback;
    }


    @Override
    public void run() {
        super.run();
        if (gattCallback == null) return;

        try {
            BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothManager.getAdapter().cancelDiscovery();


            BluetoothGatt bluetoothGatt;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.e(BleScanner.TAG, "connectGatt TRANSPORT_LE");
                bluetoothGatt = device.connectGatt(activity, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                Log.e(BleScanner.TAG, "connectGatt");
                bluetoothGatt = device.connectGatt(activity, false, gattCallback);
            }
            if (bluetoothGatt != null) {
                callback.onConnectGatt(true, bluetoothGatt);
                Thread.sleep(15000);
                new DisconnectGattThread(bluetoothGatt,callback).start();
                return;
            }
        } catch (Exception e) {
            Log.e(BleScanner.TAG, "ThreadConnectDevice run error : " + e.getMessage());

        }
        callback.onConnectDevice(false, null);
    }
}
