package com.ahmet.radar.tools;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.ahmet.radar.Radar;
import com.ahmet.radar.enums.BleScanErrors;
import com.ahmet.radar.listener.BleScannerCallback;
import com.ahmet.radar.listener.BleScannerErrorCallback;
import com.ahmet.radar.listener.BleServiceCallback;

import java.util.UUID;


import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class Executor {
    /**
     * Arayüz aktivitesi
     */
    private Activity activity;

    /**
     * Tarama filtresinin listesi
     */
    private UUID[] scanFilterList;

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


    public Executor(
            Activity activity,
            UUID[] scanFilters,
            BleScannerCallback callback,
            BleServiceCallback bleServiceCallback,
            BleScannerErrorCallback errorCallback) {

        try {
            this.activity = activity;
            this.scanFilterList = scanFilters;
            this.bleScannerCallback = callback;
            this.bleServiceCallback = bleServiceCallback;
            this.bleScannerErrorCallback = errorCallback;

        } catch (Exception e) {
            Log.e(Radar.TAG, "Executer: " + e.getMessage());
        }


    }

    /**
     * @param maxRssi     Bulunacak cihazın uzaklık limiti
     * @param vibration   Cihaz bulununca titresin mi
     * @param autoConnect Otomatik bağlan
     */
    protected void start(int maxRssi, boolean vibration, boolean autoConnect) {
        Log.d(Radar.TAG, "----> Start Scanner");
        this.maxRssi = maxRssi;
        this.vibration = vibration;
        this.autoConnect = autoConnect;
        BluetoothManager bluetoothManager = (BluetoothManager)
                activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        try {

            bluetoothAdapter.startLeScan(scanFilterList.length > 0 ? scanFilterList : null, scanCallback);
            bleScannerCallback.onScanning(true);

            setHandler();

        } catch (Exception e) {
            this.bleScannerErrorCallback.onScanError(BleScanErrors.NOT_START_SCANNER);
        }
    }


    public void stop() {
        clearHandler();
        bluetoothAdapter.stopLeScan(scanCallback);
        bleScannerCallback.onScanning(false);
    }

    public void connectDevice() {

        Log.d(Radar.TAG, "connectGatt");
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

        Log.d(Radar.TAG, "Restart Scanner");
        stop();
        new Handler().postDelayed(() -> start(maxRssi, vibration, autoConnect), 1000);
    };
    private final Handler restartHandler = new Handler();


    /**
     * Zamanlayıcıyı çalıştır
     */
    private void setHandler() {
        long restartDelaySecond = 1000;
        restartHandler.postDelayed(restartRunnable, restartDelaySecond);
    }

    /**
     * Zamanlayıcıyı durdur
     */
    private void clearHandler() {
        restartHandler.removeCallbacks(restartRunnable);
    }

    private final BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {


        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (rssi < 0 && rssi > maxRssi) {
                Log.d(Radar.TAG, "cihaz bulundu: " + device.getName() + ", " + rssi);

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
                    Log.e(Radar.TAG, "newState: " + newState + " STATE_CONNECTED 🔗");

                    bleScannerCallback.onConnectDevice(true, connectedBluetoothDevice);
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(Radar.TAG, "newState: " + newState + " STATE_DISCONNECTED ✂️");
                    gatt.close();
                    new Handler().postDelayed(
                            () -> bleScannerCallback.onConnectDevice(false, null),
                            200);
                    break;
                default:
                    Log.e(Radar.TAG, "newState: --");
                    gatt.close();
                    bleScannerErrorCallback.onScanError(BleScanErrors.NOT_CONNECTED_DEVICE);
                    break;
            }

            if (status == 133 || status == 257) {
                Log.e(Radar.TAG, "-----> !!! onConnectionStateChange status: " + status);
                setHandler();
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {

            Log.d(Radar.TAG, "servisler keşfedildi");
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
