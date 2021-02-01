package com.ahmet.blescanner.listener;

import com.ahmet.blescanner.enums.BleDeviceErrors;
import com.ahmet.blescanner.enums.BleScanErrors;

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
