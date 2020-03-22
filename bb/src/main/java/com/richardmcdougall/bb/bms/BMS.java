package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bbcommon.DebugConfigs;

public class BMS {

    private static String TAG = "BMS";

    BBService service;

    public BMS(BBService service) {
        this.service = service;
    }

    public static BMS Builder(BBService service) {

        BMS bms = null;

        if (DebugConfigs.OVERRIDE_BOARD_TYPE != null) {
            switch (DebugConfigs.OVERRIDE_BOARD_TYPE) {

                case azul:
                case classic:
                    if (BoardState.GetPlatformType() == BoardState.PlatformType.npi) {
                        BLog.d(TAG, "BMS: Using BQ on I2c");
                        bms = new BMS_BQ(service);
                    } else {

                        BLog.d(TAG, "BMS: Teensy+BQ");
                        bms = new BMS_BQTeensy(service);
                    }
                    break;

                case mast:
                case panel:
                case backpack:
                    BLog.d(TAG, "BMS: Using none");
                    bms = new BMS_None(service);
                    break;
            }
        } else {
            if (service.boardState.boardType == BoardState.BoardType.classic ||
                    (service.boardState.boardType == BoardState.BoardType.azul)) {
                if (BoardState.GetPlatformType() == BoardState.PlatformType.npi) {
                    BLog.d(TAG, "BMS: Using BQ on I2c");
                    bms = new BMS_BQ(service);
                } else {
                    BLog.d(TAG, "BMS: Teensy+BQ");
                    bms = new BMS_BQTeensy(service);
                }
            } else {
                BLog.d(TAG, "BMS: Using none");
                bms = new BMS_None(service);
            }
        }
        return bms;
    }

    public void update() {
    }

    public float get_level() {
        return 90;
    }

    public float get_voltage() {
        return 40;
    }

    public float get_current() {
        return 1;
    }

    public float get_current_instant() {
        return 2;
    }
}


