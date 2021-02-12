package com.ahmet.blescanner;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;

import com.ahmet.blescanner.listener.BleScannerCallback;
import com.ahmet.blescanner.listener.BleScannerErrorCallback;
import com.ahmet.blescanner.listener.BleServiceCallback;
import com.ahmet.blescanner.tools.DeviceControls;
import com.ahmet.blescanner.tools.Executor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleScanner extends Executor {
    /**
     * Log ekraınıda filtreleme için "BleScanner" ı aratın
     */
    public static String TAG = "BleScanner";

    public static int bluetoothRequestCode = 57;
    public static int locationRequestCode = 5757;

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


        super(activity, scanFilters, scanSettings, scannerCallback, bleServiceCallback, errorCallback);

        this.activity = activity;
        this.errorCallback = errorCallback;
        if (!DeviceControls.checkSetting(this.activity, this.errorCallback)) return;

    }

    public void startScan(int maxRssi, boolean vibration, boolean autoConnect) {
        if (!DeviceControls.checkSetting(this.activity, this.errorCallback)) return;
        super.start(maxRssi, vibration, autoConnect);
    }

    public BluetoothGattService getService(String uuid) {
        try {
            return super.bluetoothGatt.getService(UUID.fromString(uuid));
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getServicesList() {
        try {
            List<String> list = new ArrayList<>();
            for (BluetoothGattService service : super.bluetoothGatt.getServices()) {
                list.add(service.getUuid().toString());
            }

            return list;
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getCharacteristicsList(String serviceUUID) {
        try {
            BluetoothGattService service = this.getService(serviceUUID);

            List<String> list = new ArrayList<>();
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                list.add(characteristic.getUuid().toString());
            }

            return list;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        return super.bluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String data) {
        if (!super.bluetoothGatt.getServices().contains(characteristic.getService())) {
            return false;
        }
        return writeCharacteristic(characteristic, data.getBytes());
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        return super.bluetoothGatt.writeCharacteristic(characteristic);
    }
}
