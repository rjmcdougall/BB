package com.richardmcdougall.bb;

import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public class BurnerBoardUtil {

    // Raspberry PIs have some subtle different behaviour. Use this Boolean to toggle
    public static final boolean kIsRPI = Build.MODEL.contains("rpi3");


    // Compute the boardId
    public static final String BOARD_ID;
    static {
        String serial = Build.SERIAL;

        if (BurnerBoardUtil.kIsRPI) {
            BOARD_ID = "pi" + serial.substring(Math.max(serial.length() - 6, 0),
                    serial.length());
        } else {
            BOARD_ID = Build.MODEL;
        }
    }
}

