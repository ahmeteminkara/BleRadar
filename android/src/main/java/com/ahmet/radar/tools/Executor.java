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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.ahmet.radar.BleScanner;
import com.ahmet.radar.enums.BleScanErrors;
import com.ahmet.radar.listener.BleScannerCallback;
import com.ahmet.radar.listener.BleScannerErrorCallback;
import com.ahmet.radar.listener.BleServiceCallback;
import com.ahmet.radar.thread.ConnectGattThread;
import com.ahmet.radar.thread.DisconnectGattThread;
import com.ahmet.radar.thread.ScanResultThread;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_CONNECTION_CONGESTED;
import static android.bluetooth.BluetoothGatt.GATT_FAILURE;
import static android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION;
import static android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION;
import static android.bluetooth.BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
import static android.bluetooth.BluetoothGatt.GATT_INVALID_OFFSET;
import static android.bluetooth.BluetoothGatt.GATT_READ_NOT_PERMITTED;
import static android.bluetooth.BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGatt.GATT_WRITE_NOT_PERMITTED;
import static android.bluetooth.BluetoothProfile.GATT_SERVER;

public class Executor {
    /**
     * Arayüz aktivitesi
     */
    private Activity activity;

    /**
     * Tarama filtresinin listesi
     */
    private List<ScanFilter> scanFilterList;

    private ScanSettings scanSettings;

    public UUID[] filterUUID = new UUID[]{};

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
    public BluetoothDevice bluetoothDevice;

    public BluetoothGatt bluetoothGatt;

    public BluetoothManager bluetoothManager;

    public BluetoothAdapter bluetoothAdapter;

    private BluetoothLeScanner bluetoothLeScanner;



    boolean flutterHardStop = false;

    final long restartDelaySecond = 4000;
    final double startStopDiff = restartDelaySecond * 0.7;


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

            bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

            for (int i = 0; i < scanFilters.size(); i++) {
                ScanFilter scanFilter = scanFilters.get(i);
                filterUUID[i] = scanFilter.getServiceUuid().getUuid();
            }


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


                    Log.e(BleScanner.TAG, "----> startScan");
                    //bluetoothLeScanner.startScan(scanFilterList, scanSettings, scanCallback);
                    bluetoothAdapter.startLeScan(filterUUID,scanCallbackEski);
                    bleScannerCallback.onScanning(true);

                } catch (Exception e) {
                    bleScannerErrorCallback.onScanError(BleScanErrors.NOT_START_SCANNER);
                    Log.e(BleScanner.TAG, e.toString());
                }

                try {
                    Thread.sleep((long) startStopDiff);


                    Log.e(BleScanner.TAG, "----> STOP Scanner");
                    //bluetoothLeScanner.stopScan(scanCallback);
                    bluetoothAdapter.stopLeScan(scanCallbackEski);
                    bluetoothAdapter.cancelDiscovery();

                    /*
                     if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                     bluetoothLeScanner.stopScan(scanCallback);
                     } else {
                     //bluetoothAdapter.cancelDiscovery();
                     bluetoothAdapter.stopLeScan(scanCallbackEski);
                     }
                     */
                    bleScannerCallback.onScanning(false);

                } catch (InterruptedException e) {
                    Log.e(BleScanner.TAG, e.toString());
                }

            }
        }, 0, restartDelaySecond);


    }


    public void stop() {
        /*
         if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
         bluetoothLeScanner.stopScan(scanCallback);
         } else {
         bluetoothAdapter.stopLeScan(scanCallbackEski);
         }
         */
        timer.cancel();
        bleScannerCallback.onScanning(false);
        bluetoothAdapter.cancelDiscovery();
        //bluetoothLeScanner.stopScan(scanCallback);
        bluetoothAdapter.stopLeScan(scanCallbackEski);
    }

    public void connectDevice() {

        new ConnectGattThread(activity, bluetoothDevice, bluetoothGattCallback, bleScannerCallback).start();

    }

    public void disconnect() {

        new DisconnectGattThread(bluetoothGatt, bleScannerCallback).start();

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

    private final BluetoothAdapter.LeScanCallback scanCallbackEski = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
            new ScanResultThread(bluetoothDevice, rssi, maxRssi, bleScannerCallback).start();
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            new ScanResultThread(result, maxRssi, bleScannerCallback).start();

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.e(BleScanner.TAG, "onBatchScanResults");
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


    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {


        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status,
                                            int newState) {


            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.e(BleScanner.TAG, "STATE_CONNECTED 🔗");
                    bleScannerCallback.onConnectDevice(true, bluetoothDevice);
                    bluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(BleScanner.TAG, "STATE_DISCONNECTED ✂️");
                    bluetoothGatt.close();
                    //new Handler(activity.getMainLooper()).postDelayed(() -> bleScannerCallback.onConnectDevice(false, null), 200);
                    break;
                default:
                    Log.e(BleScanner.TAG, "newState: " + newState);
                    bleScannerErrorCallback.onScanError(BleScanErrors.NOT_CONNECTED_DEVICE);
                    break;
            }


        }


        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {

            Log.d(BleScanner.TAG, "servisler keşfedildi");
            if (status == GATT_SUCCESS) {
                bleServiceCallback.onDetectServices(true, bluetoothGatt);
            } else {
                bleServiceCallback.onDetectServices(false, null);

            }


        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {


            switch (status) {
                case GATT_SUCCESS:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_SUCCESS");
                    break;
                case GATT_CONNECTION_CONGESTED:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_CONNECTION_CONGESTED");
                    break;
                case GATT_FAILURE:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_FAILURE");
                    break;
                case GATT_INSUFFICIENT_AUTHENTICATION:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_INSUFFICIENT_AUTHENTICATION");
                    break;
                case GATT_INSUFFICIENT_ENCRYPTION:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_INSUFFICIENT_ENCRYPTION");
                    break;
                case GATT_INVALID_ATTRIBUTE_LENGTH:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_INVALID_ATTRIBUTE_LENGTH");
                    break;
                case GATT_INVALID_OFFSET:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_INVALID_OFFSET");
                    break;
                case GATT_READ_NOT_PERMITTED:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_READ_NOT_PERMITTED");
                    break;
                case GATT_REQUEST_NOT_SUPPORTED:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_REQUEST_NOT_SUPPORTED");
                    break;
                case GATT_WRITE_NOT_PERMITTED:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_WRITE_NOT_PERMITTED");
                    break;
                case GATT_SERVER:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite GATT_SERVER");
                    break;
                default:
                    Log.d(BleScanner.TAG, "onCharacteristicWrite status: " + status);
                    break;
            }

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
