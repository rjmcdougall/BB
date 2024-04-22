package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.io.IOException;

public class BMS {

    private static String TAG = "BMS";

    protected batteryStates mBatteryState = batteryStates.STATE_UNKNOWN;
    BBService service;

    public BMS(BBService service) {
        this.service = service;
    }

    public static BMS Builder(BBService service) {

        BMS bms;

        if (true || (service.boardState.GetBoardType() == BoardState.BoardType.mezcal)) {
            BLog.d(TAG, "BMS: Emulated BMS from VESC ");
            bms = new BMS_EmulatedVesc(service);
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.littlewing) {
            BLog.d(TAG, "BMS: Using BQ on I2c");
            bms = new BMS_BQ(service);
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.classic || service.boardState.GetBoardType() == BoardState.BoardType.azul) {
            BLog.d(TAG, "BMS: Teensy+BQ");
            bms = new BMS_BQTeensy(service);
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.panel) {
            BLog.d(TAG, "BMS: Emulated BMS from VESC ");
            bms = new BMS_EmulatedVesc(service);
        } else {
            BLog.d(TAG, "BMS: Using none");
            bms = new BMS_None(service);
        }

        return bms;
    }

    public void update() throws IOException {
    }

    public float getLevel() throws IOException {
        throw new IOException("unknown BMS state");
    }

    public float getVoltage() throws IOException {
        throw new IOException("unknown BMS state");
    }

    public float getCurrent() throws IOException {
        throw new IOException("unknown BMS state");
    }

    public float getCurrentInstant() throws IOException {
        throw new IOException("unknown BMS state");
    }

    public batteryLevelStates getBatteryLevelState() {
        try {
            float level = getLevel();
            if (level < 15) {
                return batteryLevelStates.STATE_CRITICAL;
            } else if (level < 25) {
                return batteryLevelStates.STATE_LOW;
            } else {
                return batteryLevelStates.STATE_NORMAL;
            }
        } catch (Exception e) {
            return batteryLevelStates.STATE_UNKNOWN;
        }
    }

    public batteryStates getBatteryState() {

        try {
            float voltage = getVoltage();
            float currentInstant = getCurrentInstant();
            float current = getCurrent();

            // Slow transition from to idle
            if ((current > -.1) && (current < .010)) {
                // Any state -> IDLE
                mBatteryState = batteryStates.STATE_IDLE.STATE_IDLE;
                // Fast transition from idle to discharging
            } else if ((currentInstant < -.1)) {
                // Any state -> Displaying
                mBatteryState = batteryStates.STATE_IDLE.STATE_DISCHARGING;
            } else if (mBatteryState == batteryStates.STATE_DISCHARGING &&
                    (current > 5)) {                 // STATE_DISCHARGING -> Charging (avg current)
                mBatteryState = batteryStates.STATE_CHARGING;
            } else if (mBatteryState == batteryStates.STATE_IDLE &&
                    (currentInstant > .01)) {
                // STATE_IDLE -> Charging // instant
                mBatteryState = batteryStates.STATE_CHARGING;
            } else if ((current > .01)) {
                // Anystate other state -> Charging // avg current
                mBatteryState = batteryStates.STATE_CHARGING;
            }
            return mBatteryState;
        } catch (Exception e) {
            return batteryStates.STATE_UNKNOWN;
        }
    }

    public enum batteryStates {STATE_UNKNOWN, STATE_CHARGING, STATE_IDLE, STATE_DISCHARGING}

    public enum batteryLevelStates {STATE_UNKNOWN, STATE_NORMAL, STATE_LOW, STATE_CRITICAL}


}

