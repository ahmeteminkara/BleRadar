package com.ahmet.ble_module.tools;

import android.util.Log;

import com.ahmet.ble_radar.BleRadarPlugin;

public class ThreadBle extends Thread {
    final Runnable runnable;

    public ThreadBle(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        super.run();

        if (runnable != null)
            runnable.run();

        try {
            this.join();
        } catch (InterruptedException e) {
            Log.e(BleRadarPlugin.TAG, e.getMessage());
        }
    }
}
