package com.ahmet.ble_radar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ahmet.ble_module.enums.BleRadarDeviceError;
import com.ahmet.ble_module.enums.BleRadarScanError;
import com.ahmet.ble_module.listener.BleRadarErrorListener;
import com.ahmet.ble_module.listener.BleRadarListener;
import com.ahmet.ble_module.tools.BleUtils;
import com.ahmet.ble_module.tools.ThreadBle;
import com.ahmet.radar.BleScanner;
import com.ahmet.radar.enums.BleDeviceErrors;
import com.ahmet.radar.listener.BleScannerCallback;
import com.ahmet.radar.thread.ScanResultThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;


public class BleRadarPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware,
        PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener, BleRadarListener, BleRadarErrorListener {

    public static final String TAG = "BleRadarPlugin";

    EventSink flutterScanningStatus;
    EventSink flutterDetectDevice;
    EventSink flutterConnectedDevice;
    EventSink flutterServicesDiscovered;
    EventSink flutterReadCharacteristic;
    EventSink flutterWriteCharacteristic;

    private MethodChannel channel;

    private Activity activity;
    private Context context;

    BluetoothDevice bluetoothDevice;
    BluetoothGatt bluetoothGatt;

    UUID[] filterUUID = new UUID[]{};
    List<ScanFilter> scanFilterList = new ArrayList<>();
    ScanSettings scanSettings;


    public boolean flutterControlScanning = false;
    private boolean vibration = false;
    private boolean autoConnect = false;

    public int maxRssi = 0;

    long startDelay = 4000;
    long stopTimeout = 500;

    Timer timerStart;

    BluetoothLeScanner bluetoothLeScanner;

    /**
     * <h1>Scanner Methods</h1>
     * <hr>
     */

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!flutterControlScanning) return;
            int rssi = result.getRssi();
            BluetoothDevice device = result.getDevice();

            if (rssi < 0 && rssi > maxRssi) {
                Log.d(TAG, "cihaz bulundu -: " + device.getName() + ", " + rssi);

                try {
                    JSONObject json = new JSONObject();
                    json.put("rssi", rssi);
                    json.put("name", device.getName());
                    json.put("mac", device.getAddress());

                    bluetoothDevice = device;
                    activity.runOnUiThread(() -> {
                        if (flutterDetectDevice != null)
                            flutterDetectDevice.success(json.toString());
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.e(BleScanner.TAG, "onBatchScanResults");
        }

        @Override
        public void onScanFailed(int errorCode) {
            onRadarScanError(BleRadarScanError.NOT_START_SCANNER);
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

    final BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, bytes) -> {
        if (!flutterControlScanning) return;

        if (rssi < 0 && rssi > maxRssi) {
            Log.d(TAG, "cihaz bulundu -: " + device.getName() + ", " + rssi);

            try {
                JSONObject json = new JSONObject();
                json.put("rssi", rssi);
                json.put("name", device.getName());
                json.put("mac", device.getAddress());

                bluetoothDevice = device;
                activity.runOnUiThread(() -> {
                    if (flutterDetectDevice != null)
                        flutterDetectDevice.success(json.toString());
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public void startTimer() {
        stopTimer();
        timerStart = new Timer();
        timerStart.schedule(new TimerTask() {
            @Override
            public void run() {

                try {
                    startScan();
                    Thread.sleep(startDelay - stopTimeout);
                    stopScan();
                } catch (Exception e) {
                    Log.e(TAG, "start timer start stop: " + e.getMessage());
                }

            }
        }, 0, startDelay);

    }

    public void stopTimer() {
        if (timerStart != null) timerStart.cancel();
    }

    public void startScan() {

        if (!flutterControlScanning) return;

        final Thread th = new ThreadBle(() -> {
            try {
                activity.runOnUiThread(() -> {
                    if (flutterScanningStatus != null)
                        flutterScanningStatus.success(true);
                });

                Log.e(BleScanner.TAG, "----> startScan");
                Log.e(BleScanner.TAG, "filterUUID.length " + filterUUID.length);
                if (filterUUID.length > 0) {
                    Log.d(TAG, "uuids: " + Arrays.toString(filterUUID));
                    //BleUtils.getAdapter(activity).startLeScan(filterUUID, leScanCallback);
                } else {
                    //BleUtils.getAdapter(activity).startLeScan(leScanCallback);
                }
                bluetoothLeScanner.startScan(scanFilterList, scanSettings, scanCallback);


            } catch (Exception e) {
                onRadarScanError(BleRadarScanError.NOT_START_SCANNER);
            }
        });
        th.start();

    }

    public void stopScan() {
        new ThreadBle(() -> {
            try {

                activity.runOnUiThread(() -> {
                    if (flutterScanningStatus != null)
                        flutterScanningStatus.success(false);
                });
                //BleUtils.getAdapter(activity).stopLeScan(leScanCallback);
                bluetoothLeScanner.stopScan(scanCallback);
                //BleUtils.getAdapter(activity).cancelDiscovery();

            } catch (Exception ignore) {
            }
        }).start();
    }

    final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.e(TAG, "newState: " + newState + " STATE_CONNECTED 🔗");
                    activity.runOnUiThread(() -> flutterConnectedDevice.success(true));
                    bluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(TAG, "newState: " + newState + " STATE_DISCONNECTED ✂️");
                    bluetoothGatt.close();
                    activity.runOnUiThread(() -> flutterConnectedDevice.success(false));
                    break;
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    JSONArray json = new JSONArray();

                    List<BluetoothGattService> list = gatt.getServices();
                    for (BluetoothGattService gattService : list) {
                        json.put(gattService.getUuid().toString());
                    }

                    onRadarDiscoveryService(json.toString());
                } catch (Exception e) {
                    Log.e(TAG, "onDetectServices -> error: " + e);
                }

            }


        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    public void connectDevice() {

        new ThreadBle(() -> {
            try {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.e(TAG, "connectGatt TRANSPORT_LE");
                    bluetoothGatt = bluetoothDevice.connectGatt(activity, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    Log.e(TAG, "connectGatt");
                    bluetoothGatt = bluetoothDevice.connectGatt(activity, false, bluetoothGattCallback);
                }

                if (bluetoothGatt == null) {
                    Log.e(TAG, "bluetoothGatt is null");
                    onRadarScanError(BleRadarScanError.NOT_CONNECTED_DEVICE);

                }

            } catch (Exception e) {
                Log.e(TAG, "connectGatt error: " + e.getMessage());
                onRadarScanError(BleRadarScanError.NOT_CONNECTED_DEVICE);
            }
        }).start();

    }

    public void disconnectDevice() {
        new ThreadBle(() -> {
            try {

                bluetoothGatt.disconnect();

            } catch (Exception e) {
                onRadarScanError(BleRadarScanError.NOT_CONNECTED_DEVICE);
                Log.e(TAG, "connectGatt error: " + e.getMessage());
            }
        }).start();
    }

    public boolean writeData(BluetoothGattCharacteristic characteristic, String data) {
        if (!this.bluetoothGatt.getServices().contains(characteristic.getService())) {
            return false;
        }
        return writeData(characteristic, data.getBytes());
    }

    public boolean writeData(BluetoothGattCharacteristic characteristic, byte[] data) {
        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        return bluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * <h1>Listener Methods</h1>
     * <hr>
     */

    @Override
    public void onRadarDeviceError(BleRadarDeviceError deviceError) {

    }

    @Override
    public void onRadarScanError(BleRadarScanError scanError) {

    }


    @Override
    public void onRadarDiscoveryService(String jsonStr) {
        try {
            activity.runOnUiThread(() -> {
                if (flutterServicesDiscovered != null)
                    flutterServicesDiscovered.success(jsonStr);
            });
        } catch (Exception e) {
            Log.e(TAG, "onDetectServices -> error: " + e);
        }

    }

    @Override
    public void onRadarGetterCharacteristic(List<BluetoothGattCharacteristic> gattCharacteristicList) {

    }

    @Override
    public void onRadarReadReadCharacteristic(BluetoothGattCharacteristic characteristic, String data) {

    }

    @Override
    public void onRadarWriteReadCharacteristic(BluetoothGattCharacteristic characteristic) {

    }

    /**
     * <h1>İzin kontrolü</h1>
     * <hr>
     */
    public boolean permissionOK() {

        if (activity == null) {
            Log.e(TAG, "Activity is null permissionOK");
            return false;
        }
        return BleUtils.checkSetting(this.activity, this);
    }

    /**
     * <h1>Flutter Call Method</h1>
     * <hr>
     */
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

        switch (call.method) {
            case "startScan":
                try {
                    if (call.hasArgument("maxRssi") && call.hasArgument("autoConnect")
                            && call.hasArgument("vibration") && call.hasArgument("filterUUID")) {
                        if (!permissionOK()) {
                            return;
                        }
                        flutterControlScanning = true;

                        maxRssi = call.argument("maxRssi");
                        vibration = call.argument("vibration");
                        autoConnect = call.argument("autoConnect");


                        final ArrayList<String> paramsFilter = call.argument("filterUUID");

                        if (paramsFilter != null) {
                            filterUUID = new UUID[paramsFilter.size()];
                            for (int i = 0; i < paramsFilter.size(); i++) {
                                String s = paramsFilter.get(i);
                                filterUUID[i] = UUID.fromString(s);

                                ScanFilter filter = new ScanFilter.Builder()
                                        .setServiceUuid(ParcelUuid.fromString(s))
                                        .build();
                                scanFilterList.add(filter);
                            }
                        }
                        Log.e(TAG, "startScan -> startTimer()");

                        startTimer();

                    }
                } catch (Exception e) {
                    Log.e(TAG, "startScan: " + e.getMessage());
                }

                break;
            case "stopScan":
                Log.w(TAG, "flutter dan STOP geldi");
                flutterControlScanning = false;
                stopTimer();
                stopScan();
                break;
            case "connectDevice":
                connectDevice();
                break;
            case "disconnectDevice":
                Log.w(TAG, "Flutter -> disconnectDevice");
                disconnectDevice();
                break;
            case "getServices":
                //result.success(bleModule.getServicesList());
                break;
            case "getCharacteristics":
                if (call.hasArgument("serviceUUID")) {
                    String serviceUUID = call.argument("serviceUUID");
                    //result.success(bleModule.getCharacteristicsList(serviceUUID));
                }
                break;
            case "isOpenBluetooth":
                result.success(BleUtils.isOpenBluetooth(activity));
                break;
            case "isOpenLocation":
                result.success(BleUtils.isOpenLocation(activity));
                break;
            case "isScanning":
                result.success(flutterControlScanning);
                break;
            case "readCharacteristic":
                if (call.hasArgument("characteristicUUID")
                        && call.hasArgument("serviceUUID")) {

                    String characteristicUUID = call.argument("characteristicUUID"),
                            serviceUUID = call.argument("serviceUUID");

                    //BluetoothGattService gattService = bleModule.getService(serviceUUID);
                    //BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(UUID.fromString(characteristicUUID));

                    //bleModule.readCharacteristic(gattCharacteristic);
                }
                break;
            case "writeCharacteristic":
                try {

                    if (call.hasArgument("data")
                            && call.hasArgument("characteristicUUID")
                            && call.hasArgument("serviceUUID")) {

                        String data = call.argument("data"),
                                characteristicUUID = call.argument("characteristicUUID"),
                                serviceUUID = call.argument("serviceUUID");


                        BluetoothGattService gattService = bluetoothGatt.getService(UUID.fromString(serviceUUID));

                        if (gattService != null) {
                            BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(UUID.fromString(characteristicUUID));

                            boolean status = writeData(gattCharacteristic, data);

                            Log.e(TAG, "writeCharacteristic result: " + status);
                            flutterWriteCharacteristic.success(status);

                        } else {
                            Log.e(TAG, "gattService is null");
                            result.success(false);
                            flutterWriteCharacteristic.success(false);
                        }


                    }
                } catch (Exception e) {
                    Log.e(TAG, "writeCharacteristic error: " + e.toString());
                }
                break;
            default:
                result.notImplemented();
        }
    }


    @Override
    public void onDetachedFromActivityForConfigChanges() {
        Log.d(TAG, "onDetachedFromActivityForConfigChanges");


    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        Log.d(TAG, "onReattachedToActivityForConfigChanges");
        onAttachedToActivity(binding);

        binding.addActivityResultListener(this);
        binding.addRequestPermissionsResultListener(this);
    }

    @Override
    public void onDetachedFromActivity() {
        Log.d(TAG, "onDetachedFromActivity");
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");

        if (requestCode == BleUtils.bluetoothRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                activity.runOnUiThread(this::permissionOK);
            }
        } else if (requestCode == BleUtils.fineLocationRequestCode) {

            Log.d(TAG, "locationRequestCode: " + resultCode);
            if (BleUtils.isOpenLocation(activity)) {
                activity.runOnUiThread(this::permissionOK);
            }

        }
        return false;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == BleUtils.fineLocationRequestCode) {


            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activity.runOnUiThread(this::permissionOK);
            }

        } else if (requestCode == BleUtils.bgLocationRequestCode) {


            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activity.runOnUiThread(this::permissionOK);
            }

        }
        return false;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        Log.d(TAG, "onAttachedToActivity");
        this.activity = binding.getActivity();

        //permissionOK();

        binding.addActivityResultListener(this);
        binding.addRequestPermissionsResultListener(this);


        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();

        bluetoothLeScanner = BleUtils.getLeScanner(activity);
    }


    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(
                binding.getApplicationContext(),
                binding.getBinaryMessenger()
        );

    }


    private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        context = applicationContext;
        channel = new MethodChannel(messenger, "ble_radar");
        channel.setMethodCallHandler(this);

        new EventChannel(messenger, "bluetoothStatusStream").setStreamHandler(new StreamHandler() {

            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int extra = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothDevice.ERROR);
                        //Log.e(TAG, "ble extra: " + extra);
                        switch (extra) {
                            case 10:
                            case 12:
                                boolean isOpen = BleUtils.isOpenBluetooth(activity);

                                events.success(isOpen);
                                break;

                        }


                    }
                }, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            }

            @Override
            public void onCancel(Object arguments) {

            }
        });
        new EventChannel(messenger, "locationStatusStream").setStreamHandler(new StreamHandler() {

            @Override
            public void onListen(Object arguments, EventSink events) {
                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        boolean isOpen = BleUtils.isOpenLocation(activity);
                        events.success(isOpen);

                    }
                }, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
            }

            @Override
            public void onCancel(Object arguments) {

            }
        });
        new EventChannel(messenger, "scanningStatusStream").setStreamHandler(new StreamHandler() {

            @Override
            public void onListen(Object arguments, EventSink events) {
                flutterScanningStatus = events;
            }

            @Override
            public void onCancel(Object arguments) {
                flutterScanningStatus = null;
            }
        });
        new EventChannel(messenger, "detectDeviceStream").setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                flutterDetectDevice = events;
            }

            @Override
            public void onCancel(Object arguments) {
                flutterDetectDevice = null;
            }
        });
        new EventChannel(messenger, "connectedDeviceStream").setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                flutterConnectedDevice = events;
            }

            @Override
            public void onCancel(Object arguments) {
                flutterConnectedDevice = null;
            }
        });
        new EventChannel(messenger, "servicesDiscoveredStream").setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                flutterServicesDiscovered = events;
            }

            @Override
            public void onCancel(Object arguments) {
                flutterServicesDiscovered = null;
            }
        });
        new EventChannel(messenger, "readCharacteristicStream").setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                flutterReadCharacteristic = events;
            }

            @Override
            public void onCancel(Object arguments) {
                flutterReadCharacteristic = null;
            }
        });
        new EventChannel(messenger, "writeCharacteristicStream").setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                flutterWriteCharacteristic = events;
            }

            @Override
            public void onCancel(Object arguments) {
                flutterWriteCharacteristic = null;
            }
        });
    }


    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
    }


}