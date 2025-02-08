package com.richardmcdougall.bb;

import com.richardmcdougall.bb.bms.BMS;
import com.richardmcdougall.bb.board.BurnerBoard;
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
    private int iotReportEveryNSeconds = 10;

    private int returnFromIdle = 0;

    Runnable batterySupervisor = () -> checkBattery();

    BatterySupervisor(BBService service) {
        this.service = service;

        /* Communicate the settings for the supervisor thread */
        BLog.d(TAG, "Enable Battery Monitoring? " + service.burnerBoard.enableBatteryMonitoring);
        BLog.d(TAG, "Enable IoT Reporting? " + service.burnerBoard.enableIOTReporting);

        if (service.burnerBoard.enableBatteryMonitoring)
            sch.scheduleWithFixedDelay(batterySupervisor, 10, 1, TimeUnit.SECONDS);

    }

    float voltage = 0.0f;
    float current = 0.0f;
    float currentInstant = 0.0f;
    float level = 0.0f;


    public void checkBattery() {

        boolean announce;
        powerStates powerState = powerStates.STATE_DISPLAYING;


        try {
            voltage = service.bms.getVoltage();
            current = service.bms.getCurrent();
            currentInstant = service.bms.getCurrentInstant();
            level = service.bms.getLevel();
            service.boardState.batteryLevel = (int) level;
        } catch (IOException e) {
            BLog.e(TAG, "Cannot read BMS");
        }

        try {
            this.service.bms.update();
        } catch (Exception e) {
            BLog.e(TAG, e.getLocalizedMessage());
        }


        BLog.d(TAG, "Board Current(avg) is " + current);
        BLog.d(TAG, "Board Current(Instant) is " + currentInstant);
        BLog.d(TAG, "Board Voltage is " + voltage);
        BLog.d(TAG, "Board Level is " + level);

        BMS.batteryStates batteryState = service.bms.getBatteryState();
        BMS.batteryLevelStates batteryLevelState = service.bms.getBatteryLevelState();

        BLog.d(TAG, "Battery is " + batteryLevelState);
        BLog.d(TAG, "Battery state is " + batteryState);

        if (batteryState == BMS.batteryStates.STATE_IDLE) {
            // Any state -> IDLE
            powerState = powerStates.STATE_IDLE;
            service.visualizationController.inhibitVisual = true;
        } else if (batteryState == BMS.batteryStates.STATE_DISCHARGING) {
            // Idle -> Displaying
            if (powerState == powerStates.STATE_IDLE) {
                // Show battery when board is powered up
                service.burnerBoard.showBattery(BurnerBoard.batteryType.LARGE);
            }
            if (batteryLevelState == BMS.batteryLevelStates.STATE_LOW) {
                // Show battery when board is powered up
                service.burnerBoard.showBattery(BurnerBoard.batteryType.SMALL);
            } else if (batteryLevelState == BMS.batteryLevelStates.STATE_CRITICAL) {
                service.burnerBoard.showBattery(BurnerBoard.batteryType.CRITICAL);
            }
            // Any state -> Displaying
            powerState = powerStates.STATE_DISPLAYING;
            service.visualizationController.inhibitVisual = false;
        } else if (batteryState == BMS.batteryStates.STATE_CHARGING) {
            powerState = powerStates.STATE_CHARGING;
            service.visualizationController.inhibitVisual = false;
            service.burnerBoard.showBattery(BurnerBoard.batteryType.LARGE);
        } else {
            BLog.d(TAG, "Unhandled power state " + powerState);
            service.visualizationController.inhibitVisual = false; // this occurs on all nonstandard devices.
        }

        // uncomment to test battery display
        // service.burnerBoard.showBattery(BurnerBoard.batteryType.LARGE);

        BLog.d(TAG, "Power state is " + powerState);

        announce = false;

        if (batteryLevelState == BMS.batteryLevelStates.STATE_CRITICAL) {
            if (System.currentTimeMillis() - lastOkStatement > 300000) {
                lastOkStatement = System.currentTimeMillis();
                announce = true;
            }
        } else if (batteryLevelState == BMS.batteryLevelStates.STATE_LOW) {
            if (System.currentTimeMillis() - lastLowStatement > 600000) {
                lastLowStatement = System.currentTimeMillis();
                announce = true;
            }

        }
        if (announce) {
            service.speak("Battery Level is " + (int) level + " percent", "batteryLow");
        }
    }

    public enum powerStates {STATE_CHARGING, STATE_IDLE, STATE_DISPLAYING}
}