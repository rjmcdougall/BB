package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.util.Arrays;


public class BMS_BQTeensy extends BMS {
    private String TAG = this.getClass().getSimpleName();
    //  public int[] mBatteryStats = new int[16];
    private static final int kAzulBatteryMah = 38000;

    public BMS_BQTeensy(BBService service) {
        super(service);

        BLog.e(TAG, "Teensy BMS starting");

        //BLog.e(TAG, "Status: voltage_volts:             " + String.format("%f", mMBS.voltage_volts()));
        //BLog.e(TAG, "Status: average_current_amps:      " + String.format("%f", mMBS.average_current_amps()));
        //BLog.e(TAG, "Status: current_amps:              " + String.format("%f", mMBS.current_amps()));
    }

    public void update() {
        int[] tmpBatteryStats = new int[16];

        if (service == null) {
            return;
        }
        if (service.burnerBoard == null) {
            return;
        }

        tmpBatteryStats = service.burnerBoard.getmBatteryStats();
        if ((tmpBatteryStats[0] > 0) && //flags
                (tmpBatteryStats[1] != -1) &&  // level
                (tmpBatteryStats[5] > 20000)) { // voltage
            service.boardState.batteryLevel = tmpBatteryStats[1];
            System.arraycopy(tmpBatteryStats, 0, tmpBatteryStats, 0, 16);
            BLog.d(TAG, "getBatteryLevel: " + service.boardState.batteryLevel + "%, " +
                    "voltage: " + getBatteryVoltage() + ", " +
                    "current: " + getBatteryCurrent() + ", " +
                    "current instant: " + getBatteryCurrentInstant() + ", " +
                    "flags: " + tmpBatteryStats[0] + ", " +
                    "battery stats: " + Arrays.toString(tmpBatteryStats));
        } else {
            BLog.d(TAG, "getBatteryLevel error: " + tmpBatteryStats[1] + "%, " +
                    "voltage: " + tmpBatteryStats[5] + ", " +
                    "flags: " + tmpBatteryStats[0]);
        }
    }


    public int getBatteryHealth() {
        return 100 * this.service.burnerBoard.getmBatteryStats()[5] / kAzulBatteryMah;
    }

    // Instant current in milliamps
    private int getBatteryCurrent() {
        int codedLevel = this.service.burnerBoard.getmBatteryStats()[6];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    private int getBatteryCurrentInstant() {
        int codedLevel = this.service.burnerBoard.getmBatteryStats()[9];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    // Voltage in millivolts
    private int getBatteryVoltage() {
        return this.service.burnerBoard.getmBatteryStats()[5];
    }


    // Volts
    public float getVoltage() throws  IOException {

        if (getBatteryVoltage() == 0) {
            throw new IOException("unkown BQ reading");
        }
        return (float) getBatteryVoltage() / 1000.0f;
    }

    public float getCurrent() throws  IOException {
        if (getBatteryVoltage() == 0) {
            throw new IOException("unkown BQ reading");
        }
        return (float) getBatteryCurrent();
    }

    public float getCurrentInstant() throws  IOException {
        if (getBatteryVoltage() == 0) {
            throw new IOException("unkown BQ reading");
        }
        return (float) getBatteryCurrentInstant();
    }

    public float getLevel() {
        int[] tmpBatteryStats = new int[16];

        if (service == null) {
            return 100.0f;
        }
        if (service.burnerBoard == null) {
            return 100.0f;
        }

        tmpBatteryStats = service.burnerBoard.getmBatteryStats();
        if ((tmpBatteryStats[0] > 0) && //flags
                (tmpBatteryStats[1] != -1) &&  // level
                (tmpBatteryStats[5] > 20000)) {
            return (tmpBatteryStats[1]);

        } else {
            return 100.0f;
        }
    }

    public batteryStates getBatteryState() {

        // Azul BQ boards have flakey BMS reading.
        // Non-standard

        try {
            float voltage = getVoltage();
            float currentInstant = getCurrentInstant();
            float current = getCurrent();

            // Slow transition from to idle
            if ((voltage > 20) && (current > -.150) && (current < .010)) {
                // Any state -> IDLE
                mBatteryState = batteryStates.STATE_IDLE.STATE_IDLE;
                // Fast transition from idle to discharging
            } else if ((voltage > 20) && (currentInstant < -.150)) {
                // Any state -> Displaying
                mBatteryState = batteryStates.STATE_IDLE.STATE_DISCHARGING;
            } else if (mBatteryState == batteryStates.STATE_DISCHARGING &&
                    (voltage > 20) && (current > 5)) {                 // STATE_DISCHARGING -> Charging (avg current)
                mBatteryState = batteryStates.STATE_CHARGING;
            } else if (mBatteryState == batteryStates.STATE_IDLE &&
                    (voltage > 20) && (currentInstant > .01)) {
                // STATE_IDLE -> Charging // instant
                mBatteryState = batteryStates.STATE_CHARGING;
            } else if ((voltage > 20) && (current > .01)) {
                // Anystate other state -> Charging // avg current
                mBatteryState = batteryStates.STATE_CHARGING;
            }
            return mBatteryState;
        } catch (Exception e) {
            return batteryStates.STATE_UNKNOWN;
        }
    }
}

