package com.ahmet.radar.tools;


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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.ahmet.radar.BleScanner;
import com.ahmet.radar.enums.BleScanErrors;
import com.ahmet.radar.listener.BleScannerCallback;
import com.ahmet.radar.listener.BleScannerErrorCallback;
import com.ahmet.radar.listener.BleServiceCallback;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class Executor {
    /**
     * Arayüz aktivitesi
     */
    private Activity activity;

    /**
     * Tarama filtresinin listesi
     */
    //private UUID[] scanFilterList;
    private List<ScanFilter> scanFilterList;

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

    protected BluetoothGatt bluetoothGatt;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothLeScanner bluetoothLeScanner;

    boolean flutterHardStop = false;

    long restartDelaySecond = 5000;

    Timer timer = new Timer();

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

            BluetoothManager bluetoothManager = (BluetoothManager)
                    activity.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

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


        this.maxRssi = maxRssi;
        this.vibration = vibration;
        this.autoConnect = autoConnect;

        flutterHardStop = false;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {


                try {
                    Log.e(BleScanner.TAG, "----> Start Scanner");
                    //bluetoothAdapter.startLeScan(scanFilterList.length > 0 ? scanFilterList : null, scanCallback);
                    bluetoothLeScanner.startScan(scanFilterList, scanSettings, scanCallback);
                    bleScannerCallback.onScanning(true);

                } catch (Exception e) {
                    bleScannerErrorCallback.onScanError(BleScanErrors.NOT_START_SCANNER);
                    Log.e(BleScanner.TAG, e.toString());
                }

                try {
                    Thread.sleep((long) (restartDelaySecond * 0.6));


                    Log.e(BleScanner.TAG, "----> STOP Scanner");
                    //bluetoothAdapter.cancelDiscovery();
                    //bluetoothAdapter.stopLeScan(scanCallback);
                    bluetoothLeScanner.stopScan(scanCallback);
                    bleScannerCallback.onScanning(false);

                } catch (InterruptedException e) {
                    Log.e(BleScanner.TAG, e.toString());
                }

                //new Handler().postDelayed(() -> {}, (long) (restartDelaySecond * 0.6));


            }
        }, 0, restartDelaySecond);


    }


    public void stop() {
        timer.cancel();
    }

    public void connectDevice() {

        Log.e(BleScanner.TAG, "connectGatt");
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


    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            if (result.getRssi() < 0 && result.getRssi() > maxRssi) {
                Log.d(BleScanner.TAG, "cihaz bulundu -: " + result.getDevice().getName() + ", " + result.getRssi());

                connectedBluetoothDevice = result.getDevice();


                if (vibration) startVibration();
                boolean autoConnect = bleScannerCallback.onDetectDevice(connectedBluetoothDevice, result.getRssi());
                if (autoConnect) {
                    stop();
                    connectDevice();
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                if (result.getRssi() < 0 && result.getRssi() > maxRssi) {
                    Log.d(BleScanner.TAG, "cihaz bulundu---: " + result.getDevice().getName() + ", " + result.getRssi());

                    connectedBluetoothDevice = result.getDevice();


                    if (vibration) startVibration();
                    boolean autoConnect = bleScannerCallback.onDetectDevice(connectedBluetoothDevice, result.getRssi());
                    if (autoConnect) {
                        stop();
                        connectDevice();
                    }
                }
            }
        }

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
        }
    };

    private final BluetoothAdapter.LeScanCallback scanCallbackEski = new BluetoothAdapter.LeScanCallback() {


        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (rssi < 0 && rssi > maxRssi) {
                Log.d(BleScanner.TAG, "cihaz bulundu: " + device.getName() + ", " + rssi);

                connectedBluetoothDevice = device;


                if (vibration) startVibration();
                boolean autoConnect = bleScannerCallback.onDetectDevice(connectedBluetoothDevice, rssi);
                if (autoConnect) {
                    stop();
                    connectDevice();
                }
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
                //setTimer();
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
