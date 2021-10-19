package com.ahmet.ble_module.tools;

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
import android.util.Log;

import com.ahmet.ble_module.enums.BleRadarDeviceError;
import com.ahmet.ble_module.listener.BleRadarErrorListener;
import com.ahmet.radar.BleScanner;

@SuppressLint("StaticFieldLeak")
public class BleUtils {

    public static int bluetoothRequestCode = 1;
    public static int fineLocationRequestCode = 2;
    public static int bgLocationRequestCode = 3;


    public static BluetoothAdapter getAdapter(Activity activity) {

        BluetoothManager bluetoothManager = (BluetoothManager)
                activity.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter();
    }


    /**
     * cihazda bluetooth u kontrol eder
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
            status = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
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
    public static boolean isFineLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        BleScanner.fineLocationRequestCode
                );
                return false;
            }

        }
        return true;
    }

    public static boolean isCourseLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        BleScanner.courseLocationRequestCode
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
            BleRadarErrorListener errorCallback) {


        if (!isOpenBluetooth(activity)) {
            errorCallback.onRadarDeviceError(BleRadarDeviceError.BLUETOOTH_NOT_ACTIVE);
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent, BleScanner.bluetoothRequestCode);
            return false;
        }
        if (!isSupportBle(activity)) {
            errorCallback.onRadarDeviceError(BleRadarDeviceError.UNSUPPORT_BLUETOOTH_LE);
            return false;
        }

        if (!isOpenLocation(activity)) {
            errorCallback.onRadarDeviceError(BleRadarDeviceError.LOCATION_NOT_ACTIVE);
            Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

            activity.startActivityForResult(viewIntent, BleScanner.fineLocationRequestCode);
            activity.startActivityForResult(viewIntent, BleScanner.courseLocationRequestCode);

            return false;
        }
        if (!isFineLocationPermission(activity)) {
            Log.e(BleScanner.TAG, "isFineLocationPermission false");
            errorCallback.onRadarDeviceError(BleRadarDeviceError.LOCATION_NOT_PERMISSION);
            return false;
        }

        if (!isCourseLocationPermission(activity)) {
            Log.e(BleScanner.TAG, "isCourseLocationPermission false");
            errorCallback.onRadarDeviceError(BleRadarDeviceError.LOCATION_NOT_PERMISSION);
            return false;
        }

        return true;
    }
}
















