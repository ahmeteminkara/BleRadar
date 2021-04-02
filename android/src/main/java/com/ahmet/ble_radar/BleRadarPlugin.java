package com.ahmet.ble_radar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanFilter;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.ahmet.blescanner.BleScanner;
import com.ahmet.blescanner.enums.BleDeviceErrors;
import com.ahmet.blescanner.enums.BleScanErrors;
import com.ahmet.blescanner.listener.BleScannerCallback;
import com.ahmet.blescanner.listener.BleScannerErrorCallback;
import com.ahmet.blescanner.listener.BleServiceCallback;
import com.ahmet.blescanner.tools.DeviceControls;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
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
import io.flutter.plugin.common.PluginRegistry.Registrar;


public class BleRadarPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware,
        PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {

    private MethodChannel channel;

    private BleScanner bleScanner;

    private Activity activity;
    private Context context;

    private boolean isScanning = false;

    private boolean autoConnect = false;

    private final List<String> listFilterUUID = new ArrayList<>();

    public boolean permissionOK() {

        if (activity == null) {
            Log.e(BleScanner.TAG, "Activity is null permissionOK");
            return false;
        }
        boolean status = DeviceControls.checkSetting(this.activity, this.errorCallback);
        if (status)
            prepareBleScanner();

        return status;
    }

    public void prepareBleScanner() {
        if (activity == null) {
            Log.e(BleScanner.TAG, "Activity is null");
        }


        List<ScanFilter> filterList = new ArrayList<>();

        for (String uuid : listFilterUUID) {
            filterList.add(new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(uuid)).build());
        }

        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            scanSettingsBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);


        bleScanner = new BleScanner(
                activity,
                filterList,
                scanSettingsBuilder.build(),
                scannerCallback,
                serviceCallback,
                errorCallback);


    }


    public static void registerWith(Registrar registrar) {
        //Flutter-1.12 öncesi için silme
        final BleRadarPlugin instance = new BleRadarPlugin();
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        Log.e(BleScanner.TAG, "onAttachedToActivity");
        this.activity = binding.getActivity();

        permissionOK();

        binding.addActivityResultListener(this);
        binding.addRequestPermissionsResultListener(this);

    }


    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(
                binding.getApplicationContext(),
                binding.getBinaryMessenger()
        );

    }


    BleScannerErrorCallback errorCallback = new BleScannerErrorCallback() {
        @Override
        public void onDeviceError(BleDeviceErrors deviceError) {
            Log.e(BleScanner.TAG, "onDeviceError: " + deviceError);

        }

        @Override
        public void onScanError(BleScanErrors scanError) {
            Log.e(BleScanner.TAG, "scanError: " + scanError);
        }
    };

    EventSink eventSinkScanningStatus;
    EventSink eventSinkDetectDevice;
    EventSink eventSinkConnectedDevice;
    EventSink eventSinkServicesDiscovered;
    EventSink eventSinkReadCharacteristic;
    EventSink eventSinkWriteCharacteristic;

    private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        context = applicationContext;
        channel = new MethodChannel(messenger, "ble_radar");
        channel.setMethodCallHandler(this);


        EventChannel bluetoothStatus = new EventChannel(messenger, "bluetoothStatusStream");
        bluetoothStatus.setStreamHandler(new StreamHandler() {

            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                Log.e(BleScanner.TAG, "ble onListen");
                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int extra = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothDevice.ERROR);
                        //Log.e(BleScanner.TAG, "ble extra: " + extra);
                        switch (extra) {
                            case 10:
                            case 12:
                                boolean isOpen = DeviceControls.isOpenBluetooth(activity);
                                /*if (isOpen) {
                                    bleScanner.startScan(maxRssi, vibration, autoConnect);
                                } else {
                                    bleScanner.stop();
                                }

                                 */
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

        EventChannel locationStatus = new EventChannel(messenger, "locationStatusStream");
        locationStatus.setStreamHandler(new StreamHandler() {

            @Override
            public void onListen(Object arguments, EventSink events) {
                Log.e(BleScanner.TAG, "location onListen");

                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        boolean isOpen = DeviceControls.isOpenLocation(activity);
                        if (!isOpen) {
                            bleScanner.stop();
                        }
                        events.success(isOpen);

                    }
                }, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
            }

            @Override
            public void onCancel(Object arguments) {

            }
        });

        EventChannel scanningStatus = new EventChannel(messenger, "scanningStatusStream");
        scanningStatus.setStreamHandler(new StreamHandler() {

            @Override
            public void onListen(Object arguments, EventSink events) {
                Log.e(BleScanner.TAG, "scanning status onListen");
                eventSinkScanningStatus = events;
            }

            @Override
            public void onCancel(Object arguments) {
                eventSinkScanningStatus = null;
            }
        });

        EventChannel onDetectDevice = new EventChannel(messenger, "detectDeviceStream");
        onDetectDevice.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                eventSinkDetectDevice = events;
            }

            @Override
            public void onCancel(Object arguments) {
                eventSinkDetectDevice = null;
            }
        });

        EventChannel isConnectedDevice = new EventChannel(messenger, "connectedDeviceStream");
        isConnectedDevice.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                eventSinkConnectedDevice = events;
            }

            @Override
            public void onCancel(Object arguments) {
                eventSinkConnectedDevice = null;
            }
        });

        EventChannel servicesDiscovered = new EventChannel(messenger, "servicesDiscoveredStream");
        servicesDiscovered.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                eventSinkServicesDiscovered = events;
            }

            @Override
            public void onCancel(Object arguments) {
                eventSinkServicesDiscovered = null;
            }
        });

        EventChannel readCharacteristic = new EventChannel(messenger, "readCharacteristicStream");
        readCharacteristic.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                eventSinkReadCharacteristic = events;
            }

            @Override
            public void onCancel(Object arguments) {
                eventSinkReadCharacteristic = null;
            }
        });

        EventChannel writeCharacteristic = new EventChannel(messenger, "writeCharacteristicStream");
        writeCharacteristic.setStreamHandler(new StreamHandler() {
            @Override
            public void onListen(Object arguments, EventSink events) {
                eventSinkWriteCharacteristic = events;
            }

            @Override
            public void onCancel(Object arguments) {
                eventSinkWriteCharacteristic = null;
            }
        });
    }


    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

        switch (call.method) {
            case "startScan":
                if (
                        call.hasArgument("maxRssi")
                                && call.hasArgument("autoConnect")
                                && call.hasArgument("vibration")
                                && call.hasArgument("filterUUID")
                ) {

                    if (!permissionOK()) {
                        return;
                    }

                    try {

                        int maxRssi = call.argument("maxRssi");
                        boolean vibration = call.argument("vibration");
                        autoConnect = call.argument("autoConnect");

                        final List<String> paramsFilter = call.argument("filterUUID");

                        listFilterUUID.clear();
                        listFilterUUID.addAll(paramsFilter);


                        bleScanner.startScan(maxRssi, vibration, autoConnect);
                    } catch (Exception e) {
                        Log.e("startScanAA", e.getMessage());
                    }

                }
                break;
            case "stopScan":
                Log.e(BleScanner.TAG, "flutter dan STOP geldi");
                bleScanner.stop();
                break;
            case "connectDevice":
                bleScanner.connectDevice();
                break;
            case "disconnectDevice":
                Log.e(BleScanner.TAG, "Flutter -> disconnectDevice");
                bleScanner.disconnect();
                break;
            case "getServices":
                result.success(bleScanner.getServicesList());
                break;
            case "getCharacteristics":
                if (call.hasArgument("serviceUUID")) {
                    String serviceUUID = call.argument("serviceUUID");
                    result.success(bleScanner.getCharacteristicsList(serviceUUID));
                }
                break;
            case "isOpenBluetooth":
                result.success(DeviceControls.isOpenBluetooth(activity));
                break;
            case "isOpenLocation":
                result.success(DeviceControls.isOpenLocation(activity));
                break;
            case "isScanning":
                result.success(isScanning);
                break;
            case "readCharacteristic":
                if (call.hasArgument("characteristicUUID")
                        && call.hasArgument("serviceUUID")) {

                    String characteristicUUID = call.argument("characteristicUUID"),
                            serviceUUID = call.argument("serviceUUID");

                    BluetoothGattService gattService = bleScanner.getService(serviceUUID);
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(UUID.fromString(characteristicUUID));

                    bleScanner.readCharacteristic(gattCharacteristic);
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

                        BluetoothGattService gattService = bleScanner.getService(serviceUUID);
                        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(UUID.fromString(characteristicUUID));

                        boolean status = bleScanner.writeCharacteristic(gattCharacteristic, data);
                        result.success(status);



                    } else {
                    }
                } catch (Exception e) {
                    Log.e(BleScanner.TAG, "writeCharacteristic error: " + e.toString());
                }
                break;
            default:
                result.notImplemented();
        }
    }


    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {

        channel.setMethodCallHandler(null);
        channel = null;
    }


    @SuppressLint("SimpleDateFormat")
    BleScannerCallback scannerCallback = new BleScannerCallback() {
        @Override
        public void onScanning(boolean status) {
            String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

            //Log.d(BleScanner.TAG, "onScanning: " + status + " - " + timeStamp);
            isScanning = status;
            if (eventSinkScanningStatus != null)
                activity.runOnUiThread(() -> eventSinkScanningStatus.success(status));
        }

        @Override
        public boolean onDetectDevice(BluetoothDevice device) {

            if (bleScanner.scanResult != null) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("rssi", bleScanner.scanResult.getRssi());
                    json.put("name", device.getName());
                    json.put("mac", device.getAddress());

                    Log.d(BleScanner.TAG, "onDetect: " + json.toString());

                    if (eventSinkDetectDevice != null)
                        activity.runOnUiThread(() -> eventSinkDetectDevice.success(json.toString()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return autoConnect; // bulunan cihaza bağlan
        }

        @Override
        public void onConnectDevice(boolean isConnected, BluetoothDevice device) {
            if (eventSinkConnectedDevice != null)
                activity.runOnUiThread(() -> eventSinkConnectedDevice.success(isConnected));
        }

    };

    BleServiceCallback serviceCallback = new BleServiceCallback() {
        @Override
        public void onDetectServices(boolean status, BluetoothGatt gatt) {
            if (status) {
                try {
                    JSONArray json = new JSONArray();

                    List<BluetoothGattService> list = gatt.getServices();
                    for (BluetoothGattService gattService : list) {
                        json.put(gattService.getUuid().toString());
                    }
                    if (eventSinkServicesDiscovered != null)
                        activity.runOnUiThread(() -> eventSinkServicesDiscovered.success(json.toString()));
                } catch (Exception e) {

                }

                //startReadValue();
                //startWriteValue();
            } else {
                Log.e(BleScanner.TAG, "onDetectServices -> status: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(boolean status, UUID serviceUUID,
                                         BluetoothGattCharacteristic characteristic,
                                         String data) {

            Log.d(BleScanner.TAG, "onCharacteristicRead -> data: " + data);
            if (eventSinkReadCharacteristic != null)
                activity.runOnUiThread(() -> eventSinkReadCharacteristic.success(data));
        }

        @Override
        public void onCharacteristicWrite(boolean status, UUID serviceUUID, BluetoothGattCharacteristic characteristic) {
            if (status) {
                Log.d(BleScanner.TAG, "onCharacteristicWrite success");
            } else {
                Log.e(BleScanner.TAG, "onCharacteristicWrite failed");
            }
            if (eventSinkWriteCharacteristic != null)
                activity.runOnUiThread(() -> eventSinkWriteCharacteristic.success(status));
        }
    };


    
    @Override
    public void onDetachedFromActivityForConfigChanges() {
        Log.e(BleScanner.TAG, "onDetachedFromActivityForConfigChanges");


    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        Log.e(BleScanner.TAG, "onReattachedToActivityForConfigChanges");
        onAttachedToActivity(binding);

        binding.addActivityResultListener(this);
        binding.addRequestPermissionsResultListener(this);
    }

    @Override
    public void onDetachedFromActivity() {
        Log.e(BleScanner.TAG, "onDetachedFromActivity");
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(BleScanner.TAG, "onActivityResult");

        if (requestCode == BleScanner.bluetoothRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                activity.runOnUiThread(this::permissionOK);
                Toast.makeText(activity, "Bluetooth açıldı", Toast.LENGTH_SHORT).show();
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Uygulamanın Bluetooth erişimine ihtiyacı var");
                builder.setMessage("Bu uygulamanın çevre birimlerini" +
                        " algılayabilmesi için lütfen Bluetooth'u açınız");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(dialog -> activity.runOnUiThread(this::permissionOK));
                builder.show();

            }
        } else if (requestCode == BleScanner.locationRequestCode) {

            Log.e(BleScanner.TAG, "locationRequestCode: " + resultCode);
            if (DeviceControls.isOpenLocation(activity)) {
                activity.runOnUiThread(this::permissionOK);
                Toast.makeText(activity, "Konum açıldı", Toast.LENGTH_SHORT).show();
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Uygulamanın konum erişimine ihtiyacı var");
                builder.setMessage("Bu uygulamanın çevre birimlerini" +
                        " algılayabilmesi için lütfen konumu açınız");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(dialog -> activity.runOnUiThread(this::permissionOK));
                builder.show();
            }

        }
        return false;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.e(BleScanner.TAG, "onRequestPermissionsResult");
        if (requestCode == BleScanner.locationRequestCode) {


            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Konum erişimi reddedildi");
                builder.setMessage("Konum erişimi izni vermeniz gerekmektedir");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(dialog -> activity.runOnUiThread(this::permissionOK));
                builder.show();
            } else {
                activity.runOnUiThread(this::permissionOK);
            }

        }
        return false;
    }

}