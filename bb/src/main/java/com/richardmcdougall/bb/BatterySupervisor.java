package com.richardmcdougall.bb;

import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BatterySupervisor {
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;
    private long lastOkStatement = System.currentTimeMillis();
    private long lastLowStatement = System.currentTimeMillis();
    private boolean enableBatteryMonitoring = false;
    private boolean enableIoTReporting = false;
    private int iotReportEveryNSeconds = 10;


    Runnable batteryIOT = () -> {
        try {
            String stats = service.bms.getBatteryStatsIoT();
            service.iotClient.sendUpdate("bbtelemetery", stats);
        } catch (IOException e) {
            BLog.e(TAG, "Cannot read battery for IOT Update");
        }
    };

    Runnable batterySupervisor = () ->  checkBattery();

    BatterySupervisor(BBService service) {
        this.service = service;


        if (service.boardState.GetBoardType() == BoardState.BoardType.azul ||
                service.boardState.GetBoardType() == BoardState.BoardType.classic ||
                service.boardState.GetBoardType() == BoardState.BoardType.panel) {
            enableBatteryMonitoring = true;
            enableIoTReporting = true;
        }

        /* Communicate the settings for the supervisor thread */
        BLog.d(TAG, "Enable Battery Monitoring? " + enableBatteryMonitoring);
        BLog.d(TAG, "Enable IoT Reporting? " + enableIoTReporting);

        if (enableBatteryMonitoring)
            sch.scheduleWithFixedDelay(batterySupervisor, 10, 10, TimeUnit.SECONDS);

        if (enableIoTReporting)
            sch.scheduleWithFixedDelay(batteryIOT, 10, iotReportEveryNSeconds, TimeUnit.SECONDS);

    }


    float voltage = 0.0f;
    float current = 0.0f;
    float currentInstant = 0.0f;
    float level = 0.0f;


    public void checkBattery() {

        boolean announce;
        powerStates powerState = powerStates.STATE_DISPLAYING;

        // Old config w/teensy3
        /*
        float level = service.boardState.batteryLevel;
        float current = service.burnerBoard.getBatteryCurrent();
        float currentInstant = service.burnerBoard.getBatteryCurrentInstant();
        float voltage = service.burnerBoard.getBatteryVoltage();
        */


        // Temporary hack to check and report gyro for testing
        if (this.service.gyro != null) {
            this.service.gyro.update();
        }

        try {
            voltage = service.bms.get_voltage();
            current = service.bms.get_current();
            currentInstant = service.bms.get_current_instant();
            level = service.bms.get_level();
        } catch (IOException e) {
            BLog.e(TAG, "Cannot read BMS");
        }


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
        if ((voltage > 20) && (current > -.150) && (current < .010)) {
            // Any state -> IDLE
            powerState = powerStates.STATE_IDLE;
            service.visualizationController.inhibitVisual = true;
        } else if ((voltage > 20) && (currentInstant < -.150)) {
            // Any state -> Displaying
            powerState = powerStates.STATE_DISPLAYING;
            service.visualizationController.inhibitVisual = false;
        } else if (powerState == powerStates.STATE_DISPLAYING &&
                // DISPLAYING -> Charging (avg current)
                (voltage > 20) && (current > 10)) {
            powerState = powerStates.STATE_CHARGING;
            service.visualizationController.inhibitVisual = false;
        } else if (powerState == powerStates.STATE_IDLE &&
                (voltage > 20) && (currentInstant > .010)) {
            // STATE_IDLE -> Charging // instant
            powerState = powerStates.STATE_CHARGING;
            service.visualizationController.inhibitVisual = false;
        } else if ((voltage > 20) && (current > .010)) {
            // Anystate -> Charging // avg current
            powerState = powerStates.STATE_CHARGING;
            service.visualizationController.inhibitVisual = false;
        } else {
            BLog.d(TAG, "Unhandled power state " + powerState);
            service.visualizationController.inhibitVisual = false; // this occurs on all nonstandard devices.
        }

        BLog.d(TAG, "Power state is " + powerState);

        // Show battery if charging
        service.visualizationController.showBattery(powerState == powerStates.STATE_CHARGING);

        // Battery voltage is critically low
        // Board will come to a halt in < 60 seconds
        // current is milliamps
        if ((voltage > 20.000) && (voltage < 35.300)) {
            service.visualizationController.lowBatteryVisual = true;
        } else {
            service.visualizationController.lowBatteryVisual = false;
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

        }
        if (announce) {
            service.speak("Battery Level is " + level + " percent", "batteryLow");
        }

    }

    public enum powerStates {STATE_CHARGING, STATE_IDLE, STATE_DISPLAYING}
}