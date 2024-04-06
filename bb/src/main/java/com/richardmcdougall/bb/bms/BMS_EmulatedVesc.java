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

    private float voltageFromVescPrior = 0;
    private float voltageFromVescInstant = 0;

    private float currentFromVescInstant = 0;

    private float estimatedChargerCurrent = 0;
    private float voltageDelta = 0;
    private int level = -2;

    // Defaults for Mezcal and 13s packs
    private static final float kMaxSampleCurrent = 5.0f;
    private static final float kCellMin = 3.2f;
    private static final float kCellMax = 4.2f;
    private static final int kNumCells = 13;

    private float[] batteryCurrentHistory = new float[10];
    private float[] batteryVoltageHistory = new float[10];


    public BMS_EmulatedVesc(BBService service) {
        super(service);

        BLog.e(TAG, "Emulated VESC BMS starting");
    }

    public void update() {

        // TODO: Smooth voltage over n samples, deal with absent values
        try {
            int timeslot = (int) ((System.currentTimeMillis() / 1000 ) % batteryCurrentHistory.length);
            if (service.vesc.vescOn()) {

                voltageFromVescInstant = service.vesc.getVoltage();;
                voltageFromVescInstant += service.vesc.getVoltage();;
                voltageFromVescInstant += service.vesc.getVoltage();;
                voltageFromVescInstant = voltageFromVescInstant / 3;;

                batteryVoltageHistory[timeslot] = voltageFromVescInstant;
                float averageVoltageTmp = 0;
                for (int i = 0; i < batteryVoltageHistory.length; i++) {
                    averageVoltageTmp = averageVoltageTmp + batteryVoltageHistory[i];
                }
                voltageFromVesc = averageVoltageTmp / batteryVoltageHistory.length;

                if (voltageFromVescPrior > 0) {
                    voltageDelta = voltageFromVesc - voltageFromVescPrior;
                }
                voltageFromVescPrior = voltageFromVesc;

                if (voltageDelta > 0) {
                    estimatedChargerCurrent = 0.05f / voltageDelta;
                } else {
                    estimatedChargerCurrent = 0;
                }

                currentFromVescInstant = service.vesc.getBatteryCurrent();// + estimatedChargerCurrent;

                batteryCurrentHistory[timeslot] = currentFromVescInstant;
                float averageCurrentTmp = 0;
                for (int i = 0; i < batteryCurrentHistory.length; i++) {
                    averageCurrentTmp = averageCurrentTmp + batteryCurrentHistory[i];
                }
                currentFromVesc = averageCurrentTmp / batteryCurrentHistory.length;

                // Only calculate estimated capacity if no load on the motor
                if ((currentFromVescInstant >= 0.0f) && (currentFromVescInstant < kMaxSampleCurrent)) {
                    level = (int) (100.0f * liion_norm_v_to_capacity(map(voltageFromVescInstant / kNumCells, kCellMin, kCellMax, 0.0f, 1.0f)));
                    service.boardState.batteryLevel = level;
                }
                BLog.d(TAG, "getBatteryLevel: " + service.boardState.batteryLevel + "%, " +
                        "voltage: " + getVoltage() + ", " +
                        "current: " + getCurrent() + ", " +
                        "current instant: " + getCurrentInstant() + ", " +
                        "voltage delta: " + voltageDelta);
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
        if (service.vesc.vescOn()) {
            return (-1.0f * currentFromVesc);
        }
        return (0);
    }

    public float getCurrentInstant() {
        if (service.vesc.vescOn()) {
            return (-1.0f * currentFromVescInstant);
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

