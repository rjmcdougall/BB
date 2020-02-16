package com.richardmcdougall.bb;

import com.richardmcdougall.bb.board.BurnerBoardAzul;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.util.Arrays;
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
    private CmdMessenger mListener = null;

    private int[] mBatteryStats = new int[16];

    Runnable batteryIOT = () ->  service.iotClient.sendUpdate("bbtelemetery", service.batterySupervisor.getBatteryStats());
    Runnable batterySupervisor = () ->  checkBattery();

    BatterySupervisor(BBService service) {
        this.service = service;

        if (service.boardState.boardType == BoardState.BoardType.azul ||
                service.boardState.boardType == BoardState.BoardType.classic) {
            enableBatteryMonitoring = true;
            enableIoTReporting = true;
        }

        // test devices
        if (service.boardState.boardType == BoardState.BoardType.panel) {
            enableIoTReporting = true;
        }

        /* Communicate the settings for the supervisor thread */
        BLog.d(TAG, "Enable Battery Monitoring? " + enableBatteryMonitoring);
        BLog.d(TAG, "Enable IoT Reporting? " + enableIoTReporting);

        if (enableBatteryMonitoring)
            sch.scheduleWithFixedDelay(batterySupervisor, 10, 10, TimeUnit.SECONDS);

        if (enableIoTReporting)
            sch.scheduleWithFixedDelay(batteryIOT, 10, iotReportEveryNSeconds, TimeUnit.SECONDS);


        // attach getBatteryLevel cmdMessenger callback
        BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel();
        mListener.attach(8, getBatteryLevelCallback);
        mListener.attach(10, getBatteryLevelCallback); // this was different on classic
    }

    public void checkBattery() {

        boolean announce;
        powerStates powerState = powerStates.STATE_DISPLAYING;

        int level = service.boardState.batteryLevel;
        int current = service.batterySupervisor.getBatteryCurrent();
        int currentInstant = service.batterySupervisor.getBatteryCurrentInstant();
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
            service.boardVisualization.inhibitVisual = false;
        } else if (powerState == powerStates.STATE_DISPLAYING &&
                // DISPLAYING -> Charging (avg current)
                (voltage > 20000) && (current > 10)) {
            powerState = powerStates.STATE_CHARGING;
            service.boardVisualization.inhibitVisual = false;
        } else if (powerState == powerStates.STATE_IDLE &&
                (voltage > 20000) && (currentInstant > 10)) {
            // STATE_IDLE -> Charging // instant
            powerState = powerStates.STATE_CHARGING;
            service.boardVisualization.inhibitVisual = false;
        } else if ((voltage > 20000) && (current > 10)) {
            // Anystate -> Charging // avg current
            powerState = powerStates.STATE_CHARGING;
            service.boardVisualization.inhibitVisual = false;
        } else {
            BLog.d(TAG, "Unhandled power state " + powerState);
            service.boardVisualization.inhibitVisual = false; // this occurs on all nonstandard devices.
        }

        BLog.d(TAG, "Power state is " + powerState);

        // Show battery if charging
        service.boardVisualization.showBattery(powerState == powerStates.STATE_CHARGING);

        // Battery voltage is critically low
        // Board will come to a halt in < 60 seconds
        // current is milliamps
        if ((voltage > 20000) && (voltage < 35300)) {
            service.boardVisualization.lowBatteryVisual = true;
        } else {
            service.boardVisualization.lowBatteryVisual = false;
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


    public int getBatteryVoltage() {
        return mBatteryStats[5];
    }

    public class BoardCallbackGetBatteryLevel implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            int[] tmpBatteryStats = new int[16];

            for (int i = 0; i < mBatteryStats.length; i++) {
                tmpBatteryStats[i] = mListener.readIntArg();
            }
            if ((tmpBatteryStats[0] > 0) && //flags
                    (tmpBatteryStats[1] != -1) &&  // level
                    (tmpBatteryStats[5] > 20000)) { // voltage
                service.boardState.batteryLevel = tmpBatteryStats[1];
                System.arraycopy(tmpBatteryStats, 0, mBatteryStats, 0, 16);
                BLog.d(TAG, "getBatteryLevel: " + service.boardState.batteryLevel + "%, " +
                        "voltage: " + getBatteryVoltage() + ", " +
                        "current: " + getBatteryCurrent() + ", " +
                        "flags: " + mBatteryStats[0]);
            } else {
                BLog.d(TAG, "getBatteryLevel error: " + tmpBatteryStats[1] + "%, " +
                        "voltage: " + tmpBatteryStats[5] + ", " +
                        "flags: " + tmpBatteryStats[0]);
            }
        }
    }


    // Instant current in milliamps
    public int getBatteryCurrent() {
        int codedLevel = mBatteryStats[6];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    public int getBatteryCurrentInstant() {
        int codedLevel = mBatteryStats[9];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    public String getBatteryStats() {
        return Arrays.toString(mBatteryStats);
    }

}
