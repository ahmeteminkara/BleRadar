package com.ahmet.blescanner.tools;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.ahmet.blescanner.BleScanner;
import com.ahmet.blescanner.enums.BleScanErrors;
import com.ahmet.blescanner.listener.BleScannerCallback;
import com.ahmet.blescanner.listener.BleScannerErrorCallback;
import com.ahmet.blescanner.listener.BleServiceCallback;

import java.util.ArrayList;
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
    private boolean autoConnect;
    /**
     * Bağlanılan cihaz
     */
    private BluetoothDevice connectedBluetoothDevice;

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
     * @param maxRssi     Bulunacak cihazın uzaklık limiti
     * @param vibration   Cihaz bulununca titresin mi
     * @param autoConnect Otomatik bağlan
     */
    protected void start(int maxRssi, boolean vibration, boolean autoConnect) {
        Log.d(BleScanner.TAG, "----> Start Scanner");
        this.maxRssi = maxRssi;
        this.vibration = vibration;
        this.autoConnect = autoConnect;
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
        clearHandler();
        bluetoothLeScanner.stopScan(scanCallback);
        bluetoothLeScanner.flushPendingScanResults(scanCallback);
        bleScannerCallback.onScanning(false);
    }

    public void connectDevice() {

        Log.d(BleScanner.TAG, "connectGatt");
        try {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothGatt = connectedBluetoothDevice.connectGatt(activity, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    bluetoothGatt = connectedBluetoothDevice.connectGatt(activity, false, bluetoothGattCallback);
                }
            }, 100);

        } catch (Exception e) {
            bleScannerErrorCallback.onScanError(BleScanErrors.NOT_CONNECTED_DEVICE);
        }

    }

    public void disconnect() {

        bluetoothGatt.disconnect();
        bluetoothGatt = null;
        connectedBluetoothDevice = null;
        bleScannerCallback.onConnectDevice(false, null);
    }

    /**
     * Cihazı titret
     */
    private void startVibration() {

        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(50);
        }
    }

    private final Runnable restartRunnable = () -> {

        Log.d(BleScanner.TAG, "Restart Scanner");
        stop();
        new Handler().postDelayed(() -> start(maxRssi, vibration, autoConnect), 1000);
    };
    private final Handler restartHandler = new Handler();


    /**
     * Zamanlayıcıyı çalıştır
     */
    private void setHandler() {
        long restartDelaySecond = 500;
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

            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.e(BleScanner.TAG, "error SCAN_FAILED_ALREADY_STARTED");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e(BleScanner.TAG, "error SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.e(BleScanner.TAG, "error SCAN_FAILED_INTERNAL_ERROR");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(BleScanner.TAG, "error SCAN_FAILED_FEATURE_UNSUPPORTED");
                    break;
                default:
                    Log.e(BleScanner.TAG, "onScanFailed errorCode " + errorCode);
                    break;
            }


            super.onScanFailed(errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {


            if (result.getRssi() < 0 && result.getRssi() > maxRssi) {
                Log.d(BleScanner.TAG, "cihaz bulundu: " + result.getDevice().getName()
                        + ", " + result.getRssi());

                connectedBluetoothDevice = result.getDevice();
                stop();

                if (vibration) startVibration();
                boolean autoConnect = bleScannerCallback.onDetectDevice(connectedBluetoothDevice);
                if (autoConnect) connectDevice();
            }


        }
    };


    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {


        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status,
                                            int newState) {



            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.e(BleScanner.TAG, "newState: " + newState + " STATE_CONNECTED 🔗");

                    bleScannerCallback.onConnectDevice(true, connectedBluetoothDevice);
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(BleScanner.TAG, "newState: " + newState + " STATE_DISCONNECTED ✂️");
                    gatt.close();
                    new Handler().postDelayed(
                            () -> bleScannerCallback.onConnectDevice(false, null),
                            200);
                    break;
                default:
                    Log.e(BleScanner.TAG, "newState: --");
                    gatt.close();
                    bleScannerErrorCallback.onScanError(BleScanErrors.NOT_CONNECTED_DEVICE);
                    break;
            }

            if (status == 133 || status == 257) {
                Log.e(BleScanner.TAG, "-----> !!! onConnectionStateChange status: " + status);
                setHandler();
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {

            Log.d(BleScanner.TAG, "servisler keşfedildi");
            if (status == GATT_SUCCESS) {
                bleServiceCallback.onDetectServices(true, gatt);
            } else {
                bleServiceCallback.onDetectServices(false, null);
            }


        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (status == GATT_SUCCESS) {
                bleServiceCallback.onCharacteristicWrite(true, characteristic.getService().getUuid(), characteristic);
            } else {
                bleServiceCallback.onCharacteristicWrite(false, null, null);
            }
            connectedBluetoothDevice = null;

            disconnect();

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
