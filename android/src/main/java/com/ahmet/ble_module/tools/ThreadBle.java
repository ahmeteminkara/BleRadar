package com.ahmet.ble_module.tools;

import android.util.Log;

import com.ahmet.ble_radar.BleRadarPlugin;

public class ThreadBle extends Thread {
    final Runnable runnable;
    final long timeoutStop;

    public ThreadBle(Runnable runnable) {
        this.runnable = runnable;
        this.timeoutStop = 5000;
    }

    public ThreadBle(Runnable runnable, long timeoutStop) {
        this.runnable = runnable;
        this.timeoutStop = timeoutStop;
    }

    @Override
    public void run() {
        super.run();

        if (runnable != null)
            runnable.run();

        try {
            Thread.sleep(timeoutStop);
            this.join();
        } catch (InterruptedException e) {
            Log.e(BleRadarPlugin.TAG, e.getMessage());
        }


    }

}
