package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bbcommon.DebugConfigs;

import java.io.IOException;

public class BMS {

    private static String TAG = "BMS";

    BBService service;

    public BMS(BBService service) {
        this.service = service;
    }

    public static BMS Builder(BBService service) {

        BMS bms = null;

        if (service.boardState.GetBoardType() == BoardState.BoardType.classic ||
                (service.boardState.GetBoardType() == BoardState.BoardType.azul)) {
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
    return bms;

    }

    public void update()  throws IOException {
    }

    public float get_level()  throws IOException {
        return 90;
    }

    public float get_voltage()  throws IOException {
        return 40;
    }

    public float get_current()  throws IOException {
        return 1;
    }

    public float get_current_instant()  throws IOException {
        return 2;
    }

    public String getBatteryStatsIoT()  throws IOException {
        // TODO: return the csv array compat for IoT
        //  cmdMessenger.sendCmdStart(BBGetBatteryLevel);
//          cmdMessenger.sendCmdArg(batteryControl);
//          cmdMessenger.sendCmdArg(batteryStateOfCharge);
//          cmdMessenger.sendCmdArg(batteryMaxError);
//          cmdMessenger.sendCmdArg(batteryRemainingCapacity);
//          cmdMessenger.sendCmdArg(batteryFullChargeCapacity);
//          cmdMessenger.sendCmdArg(batteryVoltage);
//          cmdMessenger.sendCmdArg(batteryAverageCurrent);
//          cmdMessenger.sendCmdArg(batteryTemperature);
//          cmdMessenger.sendCmdArg(batteryFlags);
//          cmdMessenger.sendCmdArg(batteryCurrent);
//          cmdMessenger.sendCmdArg(batteryFlagsB);
//          cmdMessenger.sendCmdEnd();
        int [] stats = new int[16];
        stats[0] =
        stats[1] = 0; // batteryControl
        stats[2] = (int)get_level(); // batteryStateOfCharge
        stats[3] = 0; // batteryMaxError
        stats[4] = 0; // batteryRemainingCapacity
        stats[5] = 0; // batteryFullChargeCapacity
        stats[6] = 0; // batteryVoltage
        stats[7] = 0; // batteryAverageCurrent
        stats[8] = 0; // batteryTemperature
        stats[9] = 0; // batteryFlags
        stats[10] = 0;// batteryCurrent
        stats[11] = 0;// batteryFlagsB

        return stats.toString();
    }
}


