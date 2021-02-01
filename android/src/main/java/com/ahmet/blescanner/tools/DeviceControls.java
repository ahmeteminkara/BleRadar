package com.ahmet.blescanner.tools;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

import com.ahmet.blescanner.BleScanner;
import com.ahmet.blescanner.enums.BleDeviceErrors;
import com.ahmet.blescanner.listener.BleScannerErrorCallback;

public class DeviceControls {
    @SuppressLint("StaticFieldLeak")

    /**
     * cihazda bluetooth u kontrol eder
     * @param activity
     */
    public static boolean isOpenBluetooth(Activity activity) {
        
        BluetoothManager bluetoothManager = (BluetoothManager)
                activity.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        return bluetoothAdapter.isEnabled();
    }

    /**
     * cihazda ble desteğini kontrol eder
     */
    public static boolean isSupportBle(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * cihazın konumunu kontrol eder
     */
    public static boolean isOpenLocation(Activity activity) {
        boolean status;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            status = lm.isLocationEnabled();
        } else {
            int mode = Settings.Secure.getInt(activity.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            status = (mode != Settings.Secure.LOCATION_MODE_OFF);
        }

        return status;

    }

    /**
     * konum izninin verilip verilmediğini kontrol eder
     */
    public static boolean isLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        BleScanner.locationRequestCode
                );
                return false;
            }
        }
        return true;
    }

    /**
     * uygulama için cihazda gerekli ayarlamaları kontrol eder
     */
    public static boolean checkSetting(
            Activity activity,
            BleScannerErrorCallback errorCallback) {



        if (!isOpenBluetooth(activity)) {
            errorCallback.onDeviceError(BleDeviceErrors.BLUETOOTH_NOT_ACTIVE);
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent, BleScanner.bluetoothRequestCode);
            return false;
        }
        if (!isSupportBle(activity)) {
            errorCallback.onDeviceError(BleDeviceErrors.UNSUPPORTED_BLUETOOTH_LE);
            return false;
        }

        if (!isOpenLocation(activity)) {
            errorCallback.onDeviceError(BleDeviceErrors.LOCATION_NOT_ACTIVE);
            Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivityForResult(viewIntent, BleScanner.locationRequestCode);
            return false;
        }
        if (!isLocationPermission(activity)) {
            errorCallback.onDeviceError(BleDeviceErrors.LOCATION_NOT_PERMISSION);
            return false;
        }

        return true;
    }
}
















