package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bbcommon.DebugConfigs;

import java.io.IOException;
import java.util.Arrays;

public class BMS {

    private static String TAG = "BMS";

    BBService service;

    public BMS(BBService service) {
        this.service = service;
    }

    public static BMS Builder(BBService service) {

        BMS bms;

        if (service.boardState.GetBoardType() == BoardState.BoardType.v4) {
            BLog.d(TAG, "BMS: Emulated BMS from VESC ");
            bms = new BMS_EmulatedVesc(service);
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.littlewing) {
            BLog.d(TAG, "BMS: Using BQ on I2c");
            bms = new BMS_BQ(service);
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.classic || service.boardState.GetBoardType() == BoardState.BoardType.azul) {
            BLog.d(TAG, "BMS: Teensy+BQ");
            bms = new BMS_BQTeensy(service);
        } else {
            BLog.d(TAG, "BMS: Using none");
            bms = new BMS_None(service);
        }

        return bms;
    }

    public void update() throws IOException {
    }

    public float get_level() throws IOException {
        return 90;
    }

    public float get_voltage() throws IOException {
        return 40;
    }

    public float get_current() throws IOException {
        return 1;
    }

    public float get_current_instant() throws IOException {
        return 2;
    }


}

