package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

import java.util.Arrays;
import java.io.IOException;


// For VESC systems with no BMS, this estimates battery level using cell voltage from VESC

public class BMS_EmulatedVesc extends BMS {
    private String TAG = this.getClass().getSimpleName();

    private float voltageFromVesc = 0;
    private float currentFromVesc = 0;
    private int level = -2;

    // Defaults for Mezcal and 13s packs
    private static final float kMaxSampleCurrent = 5.0f;
    private static final float kCellMin = 3.2f;
    private static final float kCellMax = 4.2f;
    private static final int kNumCells = 13;

    private float[] batteryCurrentHistory = new float[10];

    private static final float kPresumedCurrentLoadLights = -5.0f;

    public BMS_EmulatedVesc(BBService service) {
        super(service);

        BLog.e(TAG, "Emulated VESC BMS starting");
    }

    public void update() {

        // TODO: Smooth voltage over n samples, deal with absent values
        try {
            if (service.vesc.vescOn()) {
                voltageFromVesc = service.vesc.getVoltage();
                currentFromVesc = service.vesc.getBatteryCurrent();
                // Only calculate estimated capacity if no load on the motor
                if ((currentFromVesc >= 0.0f) && (currentFromVesc < kMaxSampleCurrent)) {
                    level = (int) (100.0f * liion_norm_v_to_capacity(map(voltageFromVesc / kNumCells, kCellMin, kCellMax, 0.0f, 1.0f)));
                    service.boardState.batteryLevel = level;
                }
                BLog.d(TAG, "getBatteryLevel: " + service.boardState.batteryLevel + "%, " +
                        "voltage: " + getVoltage() + ", " +
                        "current: " + getCurrent() + ", " +
                        "current instant: " + getCurrentInstant());
            }
        } catch (Exception E) {
        }
    }

    private float map(float x, float in_min, float in_max, float out_min, float out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    private float truncate_number(float number, float min, float max) {
        if (number > max) {
            number = max;
        } else if (number < min) {
            number = min;
        }

        return number;
    }

    private float liion_norm_v_to_capacity(float norm_v) {
        // constants for polynomial fit of lithium ion battery
        final float li_p[] = {
                -2.979767f, 5.487810f, -3.501286f, 1.675683f, 0.317147f};
        norm_v = truncate_number(norm_v, 0.0f, 1.0f);
        float v2 = norm_v * norm_v;
        float v3 = v2 * norm_v;
        float v4 = v3 * norm_v;
        float v5 = v4 * norm_v;
        float capacity = li_p[0] * v5 + li_p[1] * v4 + li_p[2] * v3 + li_p[3] * v2 + li_p[4] * norm_v;
        return capacity;
    }

    public int getBatteryHealth() {
        return 100;
    }

    public float getVoltage() throws IOException {
        if (level < 0) {
            throw new IOException("unknown emulated BMS voltage");
        }
        return (voltageFromVesc * 1000f);
    }

    public float getCurrent() {
        float current = getCurrentInstant();
        int timeslot = (int) ((System.currentTimeMillis() / 1000 ) % batteryCurrentHistory.length);
        batteryCurrentHistory[timeslot] = current;
        float averageCurrent = 0;
        for (int i = 0; i < batteryCurrentHistory.length; i++) {
            averageCurrent = averageCurrent + batteryCurrentHistory[i];
        }
        averageCurrent = averageCurrent / batteryCurrentHistory.length;
        return (averageCurrent);
    }

    public float getCurrentInstant() {
        if (service.vesc.vescOn()) {
            return (kPresumedCurrentLoadLights - (-1.0f * currentFromVesc));
        }
        return (0);
    }

    public float getLevel() throws IOException {
        if (level < 0) {
            throw new IOException("unknown emulated BMS level");
        }
        return level;
    }
}

