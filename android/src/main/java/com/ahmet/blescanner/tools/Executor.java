package com.ahmet.blescanner.tools;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.ahmet.blescanner.BleScanner;
import com.ahmet.blescanner.enums.BleScanErrors;
import com.ahmet.blescanner.listener.BleScannerCallback;
import com.ahmet.blescanner.listener.BleScannerErrorCallback;
import com.ahmet.blescanner.listener.BleServiceCallback;

import java.util.Calendar;
import java.util.List;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class Executor {
    /**
     * Arayüz aktivitesi
     */
    private Activity activity;
    /**
     * Tarama filtresinin listesi
     */
    private List<ScanFilter> scanFilterList;
    /**
     * Tarama ayarları
     */
    private ScanSettings scanSettings;
    /**
     * Taramanın dinleyicisi
     */
    private BleScannerCallback bleScannerCallback;

    private BleServiceCallback bleServiceCallback;
    /**
     * Taramanın hata dinleyicisi
     */
    private BleScannerErrorCallback bleScannerErrorCallback;
    /**
     * Bulunacak cihazın uzaklık limiti
     */
    private int maxRssi;
    /**
     * Cihaz bulununca titresin mi
     */
    private boolean vibration;
    /**
     * Bağlanılan cihaz
     */
    private BluetoothDevice bluetoothDevice;

    public ScanResult scanResult;

    private BluetoothLeScanner bluetoothLeScanner;

    protected BluetoothGatt bluetoothGatt;

    private BluetoothAdapter bluetoothAdapter;

    public Executor(
            Activity activity,
            List<ScanFilter> scanFilters,
            ScanSettings scanSettings,
            BleScannerCallback callback,
            BleServiceCallback bleServiceCallback,
            BleScannerErrorCallback errorCallback) {

        try {
            this.activity = activity;
            this.scanFilterList = scanFilters;
            this.scanSettings = scanSettings;
            this.bleScannerCallback = callback;
            this.bleServiceCallback = bleServiceCallback;
            this.bleScannerErrorCallback = errorCallback;

        } catch (Exception e) {
            Log.e(BleScanner.TAG, "Executer: " + e.getMessage());
        }


    }


    /**
     * @param maxRssi   Bulunacak cihazın uzaklık limiti
     * @param vibration Cihaz bulununca titresin mi
     */
    protected void start(int maxRssi, boolean vibration) {
        this.maxRssi = maxRssi;
        this.vibration = vibration;

        BluetoothManager bluetoothManager = (BluetoothManager)
                activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        try {

            bluetoothLeScanner.startScan(scanFilterList, scanSettings, scanCallback);
            bleScannerCallback.onScanning(true);

            setHandler();

        } catch (Exception e) {
            this.bleScannerErrorCallback.onScanError(BleScanErrors.NOT_START_SCANNER);
        }

    }

    public void stop() {
        try {

            clearHandler();
            bluetoothLeScanner.stopScan(scanCallback);
            bluetoothLeScanner.flushPendingScanResults(scanCallback);
            bleScannerCallback.onScanning(false);

        } catch (Exception ignored) {
        }
    }

    public void connectDevice() {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = bluetoothDevice.connectGatt(activity, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = bluetoothDevice.connectGatt(activity, false, bluetoothGattCallback);
            }

            Log.d(BleScanner.TAG, "connectDevice");

        } catch (Exception e) {
            Log.e(BleScanner.TAG, "connectDevice Error");
            bleScannerErrorCallback.onScanError(BleScanErrors.NOT_CONNECTED_DEVICE);
        }
    }

    public void disconnect() {

        bluetoothGatt.close();
        bluetoothGatt.disconnect();
        bluetoothGatt = null;
        bluetoothDevice = null;
        bleScannerCallback.onConnectDevice(false, null);
    }

    /**
     * Cihazı titret
     */
    private void startVibration() {

        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(100);
        }
    }


    private final Runnable restartRunnable = () -> {
        Log.d(BleScanner.TAG, "restartRunnable, " + Calendar.getInstance().getTime().toString());
        scanResult = null;
        bluetoothDevice = null;

        this.stop();
        this.start(maxRssi, vibration);
    };
    private final Handler restartHandler = new Handler();

    /**
     * Zamanlayıcıyı çalıştır
     */
    private void setHandler() {
        clearHandler();
        long restartDelaySecond = 2000;
        restartHandler.postDelayed(restartRunnable, restartDelaySecond);
    }

    /**
     * Zamanlayıcıyı durdur
     */
    private void clearHandler() {
        restartHandler.removeCallbacks(restartRunnable);
    }


    private final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(BleScanner.TAG, "onScanFailed");
            super.onScanFailed(errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            if (result.getRssi() < 0 && result.getRssi() > maxRssi) {
                Log.d(BleScanner.TAG, "onScanResult: " + result.getDevice().getName() + ", " + result.getRssi());

                //stop();
                scanResult = result;
                bluetoothDevice = result.getDevice();
                bleScannerCallback.onDetectDevice(bluetoothDevice);
            }

        }

    };


    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status,
                                            int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    if (vibration) startVibration();
                    bleScannerCallback.onConnectDevice(true, bluetoothDevice);
                    bluetoothGatt.discoverServices();
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    if (bluetoothDevice == null) {
                        setHandler();
                    }
                    bleScannerCallback.onConnectDevice(false, null);
                    break;
                default:
                    bleScannerErrorCallback.onScanError(BleScanErrors.NOT_CONNECTED_DEVICE);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {

            Log.d(BleScanner.TAG, "onServicesDiscovered");
            if (status == GATT_SUCCESS) {
                bleServiceCallback.onDetectServices(true, gatt);
            } else {
                bleServiceCallback.onDetectServices(false, null);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == GATT_SUCCESS) {
                bleServiceCallback.onCharacteristicWrite(true, characteristic.getService().getUuid(), characteristic);
            } else {
                bleServiceCallback.onCharacteristicWrite(false, null, null);
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == GATT_SUCCESS) {
                bleServiceCallback.onCharacteristicRead(
                        true,
                        characteristic.getService().getUuid(),
                        characteristic,
                        characteristic.getStringValue(0));
            } else {
                bleServiceCallback.onCharacteristicRead(
                        false,
                        characteristic.getService().getUuid(),
                        characteristic,
                        null);
            }
        }

    };


}
