package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.hardware.BQ34Z100;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;


public class BMS_BQTeensy extends BMS {
    private String TAG = this.getClass().getSimpleName();

    public BMS_BQTeensy(BBService service) {
        super(service);

        BLog.e(TAG, "Teensy BMS startihng");

        //BLog.e(TAG, "Status: voltage_volts:             " + String.format("%f", mMBS.voltage_volts()));
        //BLog.e(TAG, "Status: average_current_amps:      " + String.format("%f", mMBS.average_current_amps()));
        //BLog.e(TAG, "Status: current_amps:              " + String.format("%f", mMBS.current_amps()));
    }

    public void update() {
    }

    public float get_voltage() {
        return 0;
     }

    public float get_current() {

        return 0;
    }

    public float get_current_instant() {
        return 0;
    }

    public float get_level() {

        return 0;
    }
}

