package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BatterySupervisor {

    private int loopCnt = 0;
    private BBService mBBService = null;
    private static final String TAG = "BB.Supervisor";
    private long lastOkStatement = System.currentTimeMillis();
    private long lastLowStatement = System.currentTimeMillis();

    BatterySupervisor(BBService service){
        mBBService = service;
    }

    public void d(String logMsg) {
        if (DebugConfigs.DEBUG_BATTERY_SUPERVISOR) {
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

            // Every second, check & update battery
            if (mBBService.mEnableBatteryMonitoring && (mBBService.burnerBoard != null)) {
                checkBattery();

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

    private void sendLogMsg(String msg) {
        Intent in = new Intent(ACTION.STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mBBService).sendBroadcast(in);
    }

    public void d_battery(String s) {
        if (DebugConfigs.DEBUG_BATTERY) {
            Log.v(TAG, s);
            sendLogMsg(s);
        }
    }

    public enum powerStates {STATE_CHARGING, STATE_IDLE, STATE_DISPLAYING}

    public void checkBattery() {
        if ((mBBService.burnerBoard != null) && (mBBService.boardVisualization != null)) {

            boolean announce = false;
            powerStates powerState = powerStates.STATE_DISPLAYING;

            int level = mBBService.burnerBoard.getBattery();
            int current = mBBService.burnerBoard.getBatteryCurrent();
            int currentInstant = mBBService.burnerBoard.getBatteryCurrentInstant();
            int voltage = mBBService.burnerBoard.getBatteryVoltage();

            d_battery("Board Current(avg) is " + current);
            d_battery("Board Current(Instant) is " + currentInstant);
            d_battery("Board Voltage is " + voltage);

            // Save CPU cycles for lower power mode
            // current is milliamps
            // Current with brain running is about 100ma
            // Check voltage to make sure we're really reading the battery gauge
            // Make sure we're not seeing +ve current, which is charging
            // Average current use to enter STATE_IDLE
            // Instant current used to exit STATE_IDLE
            if ((voltage > 20000) && (current > -150) && (current < 10)) {
                // Any state -> IDLE
                powerState = powerStates.STATE_IDLE;
                mBBService.boardVisualization.inhibit(true);
            } else if ((voltage > 20000) && (currentInstant < -150)) {
                // Any state -> Displaying
                powerState = powerStates.STATE_DISPLAYING;
                mBBService.boardVisualization.inhibit(false);
            } else if (powerState == powerStates.STATE_DISPLAYING &&
                    // DISPLAYING -> Charging (avg current)
                    (voltage > 20000) && (current > 10)) {
                powerState = powerStates.STATE_CHARGING;
                mBBService.boardVisualization.inhibit(false);
            } else if (powerState == powerStates.STATE_IDLE &&
                    (voltage > 20000) && (currentInstant > 10)) {
                // STATE_IDLE -> Charging // instant
                powerState = powerStates.STATE_CHARGING;
                mBBService.boardVisualization.inhibit(false);
            } else if ((voltage > 20000) && (current > 10)) {
                // Anystate -> Charging // avg current
                powerState = powerStates.STATE_CHARGING;
                mBBService.boardVisualization.inhibit(false);
            } else {
                d("Unhandled power state " + powerState);
                mBBService.boardVisualization.inhibit(false);
            }

            d_battery("Power state is " + powerState);

            // Show battery if charging
            mBBService.boardVisualization.showBattery(powerState == powerStates.STATE_CHARGING);

            // Battery voltage is critically low
            // Board will come to a halt in < 60 seconds
            // current is milliamps
            if ((voltage > 20000) && (voltage < 35300)) {
                mBBService.boardVisualization.emergency(true);
            } else {
                mBBService.boardVisualization.emergency(false);
            }

            announce = false;

            if ((level >= 0) && (level < 15)) {
                if (System.currentTimeMillis() - lastOkStatement > 60000) {
                    lastOkStatement = System.currentTimeMillis();
                    announce = true;
                }
            } else if ((level >= 0) && (level <= 25)) {
                if (System.currentTimeMillis() - lastLowStatement > 300000) {
                    lastLowStatement = System.currentTimeMillis();
                    announce = true;
                }

            } else if (false) {
                if (System.currentTimeMillis() - lastOkStatement > 1800000) {
                    lastOkStatement = System.currentTimeMillis();
                    announce = true;
                }
            }
            if (announce) {
                mBBService.voice.speak("Battery Level is " +
                        level + " percent", TextToSpeech.QUEUE_FLUSH, null, "batteryLow");
            }
        }
    }
}
