package com.ahmet.radar;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.util.Log;

import com.ahmet.radar.listener.BleScannerCallback;
import com.ahmet.radar.listener.BleScannerErrorCallback;
import com.ahmet.radar.listener.BleServiceCallback;
import com.ahmet.radar.tools.DeviceControls;
import com.ahmet.radar.tools.Executor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleScanner extends Executor {
    /**
     * Log ekraınıda filtreleme için "BleScanner" ı aratın
     */
    public static String TAG = "BleScanner";

    public static int bluetoothRequestCode = 1;
    public static int fineLocationRequestCode = 2;
    public static int courseLocationRequestCode = 3;
    public static int bgLocationRequestCode = 4;

    /**
     * Tanımlanan activity
     */
    private final Activity activity;

    private final BleScannerErrorCallback errorCallback;

    /**
     * @param activity        [...context.this] şeklinde belirtilmesi lazım
     * @param scannerCallback BLE tarama durumunu izlemek için
     * @param errorCallback   Hataları yakalamak için
     */
    public BleScanner(Activity activity,
                      List<ScanFilter> scanFilters,
                      ScanSettings scanSettings,
                      BleScannerCallback scannerCallback,
                      BleServiceCallback bleServiceCallback,
                      BleScannerErrorCallback errorCallback) {

        super(activity,scanFilters, scanSettings, scannerCallback, bleServiceCallback, errorCallback);

        this.activity = activity;
        this.errorCallback = errorCallback;
        if (!DeviceControls.checkSetting(this.activity, this.errorCallback)) return;

    }

    public void startScan(int maxRssi, boolean vibration,boolean autoConnect) {
        if (!DeviceControls.checkSetting(this.activity, this.errorCallback)) return;
        super.start(maxRssi, vibration,autoConnect);
    }

    public BluetoothGattService getService(String uuid) {
        try {
            return super.bluetoothGatt.getService(UUID.fromString(uuid));
        } catch (Exception e) {
            Log.e(BleScanner.TAG, "getService() error: " + e.toString());
            return null;
        }
    }

    public List<String> getServicesList() {
        List<String> list = new ArrayList<>();
        try {
            for (BluetoothGattService service : this.bluetoothGatt.getServices()) {
                list.add(service.getUuid().toString());
            }
            return list;
        } catch (Exception e) {
            Log.e(BleScanner.TAG, "getServicesList() is empty");
            return list;
        }
    }

    public List<String> getCharacteristicsList(String serviceUUID) {
        List<String> list = new ArrayList<>();
        try {
            BluetoothGattService service = this.getService(serviceUUID);

            if (service != null) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    list.add(characteristic.getUuid().toString());
                }
                Log.d(BleScanner.TAG, "getCharacteristicsList() list length: " + list.size());
            }
            return list;
        } catch (Exception e) {
            Log.e(BleScanner.TAG, "getCharacteristicsList() error: " + e.toString());
            return list;
        }
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        return this.bluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String data) {
        if (!this.bluetoothGatt.getServices().contains(characteristic.getService())) {
            return false;
        }
        return writeCharacteristic(characteristic, data.getBytes());
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        return this.bluetoothGatt.writeCharacteristic(characteristic);
    }
}