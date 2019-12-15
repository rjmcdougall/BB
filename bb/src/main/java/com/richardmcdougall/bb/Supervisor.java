package com.richardmcdougall.bb;

import android.util.Log;

public class Supervisor {

    private int loopCnt = 0;
    private BBService mBBService = null;
    private static final String TAG = "BB.Supervisor";

    Supervisor(BBService service){
        mBBService = service;
    }

    public void d(String logMsg) {
        if (DebugConfigs.DEBUG_SUPERVISOR) {
            Log.d(TAG, logMsg);
        }
    }

    public void e(String logMsg) {
        Log.e(TAG, logMsg);
    }


    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run()
            {
                Thread.currentThread().setName("Supervisor");
                runSupervisor();
            }
        });
        t.start();
    }

    private void runSupervisor() {

        /* Communicate the settings for the supervisor thread */
        d("Enable Battery Monitoring? " + mBBService.mEnableBatteryMonitoring);
        d("Enable IoT Reporting? " + mBBService.mEnableIoTReporting);
        d("Enable WiFi reconnect? " + mBBService.wifi.mEnableWifiReconnect);

        while (true) {

            // Every 60 seconds check WIFI
            if (mBBService.wifi.mEnableWifiReconnect && (loopCnt % mBBService.wifi.mWifiReconnectEveryNSeconds == 0)) {
                if (mBBService.wifi != null) {
                    d("Check Wifi");
                    mBBService.wifi.checkWifiReconnect();
                }
            }

            // Every second, check & update battery
            if (mBBService.mEnableBatteryMonitoring && (mBBService.burnerBoard != null)) {
                mBBService.checkBattery();

                // Every 10 seconds, send battery update via IoT.
                // Only do this if we're actively checking the battery.
                if (mBBService.mEnableIoTReporting && (loopCnt % mBBService.mIoTReportEveryNSeconds == 0)) {
                    d("Sending MQTT update");
                    try {
                        mBBService.iotClient.sendUpdate("bbtelemetery", mBBService.burnerBoard.getBatteryStats());
                    } catch (Exception e) {
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (Throwable e) {
            }

            loopCnt++;
        }
    }
}
