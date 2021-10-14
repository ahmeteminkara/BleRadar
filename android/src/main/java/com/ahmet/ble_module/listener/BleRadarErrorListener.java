package com.ahmet.ble_module.listener;

import com.ahmet.ble_module.enums.BleRadarDeviceError;
import com.ahmet.ble_module.enums.BleRadarScanError;

public interface BleRadarErrorListener {
     void onRadarDeviceError(BleRadarDeviceError deviceError);
     void onRadarScanError(BleRadarScanError scanError);
}
