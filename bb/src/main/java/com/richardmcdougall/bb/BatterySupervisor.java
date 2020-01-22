package com.richardmcdougall.bb;

import android.speech.tts.TextToSpeech;

public class BatterySupervisor {
    private String TAG = this.getClass().getSimpleName();

    private int loopCnt = 0;
    private BBService service = null;
    private long lastOkStatement = System.currentTimeMillis();
    private long lastLowStatement = System.currentTimeMillis();
    private boolean enableBatteryMonitoring = true;
    private boolean enableIoTReporting = true;
    private int iotReportEveryNSeconds = 10;

    BatterySupervisor(BBService service) {
        this.service = service;

        if (!(service.boardState.platformType == BoardState.PlatformType.rpi)) {
            enableBatteryMonitoring = false;
            enableIoTReporting = false;
        }
    }

    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("Supervisor");
                runSupervisor();
            }
        });
        t.start();
    }

    private void runSupervisor() {

        /* Communicate the settings for the supervisor thread */
        BLog.d(TAG, "Enable Battery Monitoring? " + enableBatteryMonitoring);
        BLog.d(TAG, "Enable IoT Reporting? " + enableIoTReporting);
        BLog.d(TAG, "Enable WiFi reconnect? " + service.wifi.enableWifiReconnect);

        while (true) {

            // Every second, check & update battery
            if (enableBatteryMonitoring && (service.burnerBoard != null)) {
                checkBattery();

                // Every 10 seconds, send battery update via IoT.
                // Only do this if we're actively checking the battery.
                if (enableIoTReporting && (loopCnt % iotReportEveryNSeconds == 0)) {
                    BLog.d(TAG, "Sending MQTT update");
                    try {
                        service.iotClient.sendUpdate("bbtelemetery", service.burnerBoard.getBatteryStats());
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

    public enum powerStates {STATE_CHARGING, STATE_IDLE, STATE_DISPLAYING}

    public void checkBattery() {
        if ((service.burnerBoard != null) && (service.boardVisualization != null)) {

            boolean announce = false;
            powerStates powerState = powerStates.STATE_DISPLAYING;

            int level = service.boardState.batteryLevel;
            int current = service.burnerBoard.getBatteryCurrent();
            int currentInstant = service.burnerBoard.getBatteryCurrentInstant();
            int voltage = service.burnerBoard.getBatteryVoltage();

            BLog.d(TAG, "Board Current(avg) is " + current);
            BLog.d(TAG, "Board Current(Instant) is " + currentInstant);
            BLog.d(TAG, "Board Voltage is " + voltage);

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
                service.boardVisualization.inhibitVisual = true;
            } else if ((voltage > 20000) && (currentInstant < -150)) {
                // Any state -> Displaying
                powerState = powerStates.STATE_DISPLAYING;
                service.boardVisualization.inhibitVisual = true;
            } else if (powerState == powerStates.STATE_DISPLAYING &&
                    // DISPLAYING -> Charging (avg current)
                    (voltage > 20000) && (current > 10)) {
                powerState = powerStates.STATE_CHARGING;
                service.boardVisualization.inhibitVisual = true;
            } else if (powerState == powerStates.STATE_IDLE &&
                    (voltage > 20000) && (currentInstant > 10)) {
                // STATE_IDLE -> Charging // instant
                powerState = powerStates.STATE_CHARGING;
                service.boardVisualization.inhibitVisual = true;
            } else if ((voltage > 20000) && (current > 10)) {
                // Anystate -> Charging // avg current
                powerState = powerStates.STATE_CHARGING;
                service.boardVisualization.inhibitVisual = true;
            } else {
                BLog.d(TAG, "Unhandled power state " + powerState);
                service.boardVisualization.inhibitVisual = true;
            }

            BLog.d(TAG, "Power state is " + powerState);

            // Show battery if charging
            service.boardVisualization.showBattery(powerState == powerStates.STATE_CHARGING);

            // Battery voltage is critically low
            // Board will come to a halt in < 60 seconds
            // current is milliamps
            if ((voltage > 20000) && (voltage < 35300)) {
                service.boardVisualization.emergencyVisual = true;
            } else {
                service.boardVisualization.emergencyVisual = true;
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
                service.voice.speak("Battery Level is " +
                        level + " percent", TextToSpeech.QUEUE_FLUSH, null, "batteryLow");
            }
        }
    }
}
