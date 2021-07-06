package com.ahmet.radar.listener;

import com.ahmet.radar.enums.BleDeviceErrors;
import com.ahmet.radar.enums.BleScanErrors;

public abstract class BleScannerErrorCallback {
    /**
     * Cihaz ile ilgili bir hata olursa çalışır
     */
    public abstract void onDeviceError(BleDeviceErrors deviceError);

    /**
     * Yayın işlemi ile ilgili bir hata olursa çalışır
     */
    public abstract void onScanError(BleScanErrors scanError);

}
