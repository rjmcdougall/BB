package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.hardware.BQ34Z100;
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
                    "battery stats: " + Arrays.toString(tmpBatteryStats)  );
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
    public int getBatteryCurrent() {
        int codedLevel = this.service.burnerBoard.getmBatteryStats()[6];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    public int getBatteryCurrentInstant() {
        int codedLevel = this.service.burnerBoard.getmBatteryStats()[9];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    // Voltage in millivolts
    public int getBatteryVoltage() {
        return this.service.burnerBoard.getmBatteryStats()[5];
    }


    public float get_voltage() {
        return (float)getBatteryVoltage();
     }

    public float get_current() {
        return (float)getBatteryCurrent();
    }

    public float get_current_instant() {
        return (float)getBatteryCurrentInstant();
        }

    public float get_level() {
        return 100.0f;
    }
}

