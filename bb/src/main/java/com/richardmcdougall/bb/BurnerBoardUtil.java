package com.richardmcdougall.bb;

import android.os.Build;

public class BurnerBoardUtil {

    // Raspberry PIs have some subtle different behaviour. Use this Boolean to toggle
    public static final boolean kIsRPI = Build.MODEL.contains("rpi3");

}
