package com.richardmcdougall.bb;

import android.os.Build;

public class BurnerBoardUtil {

    /*
        Feature flag section here
    */

    // This enabled GPS Time being polled
    public static final boolean fEnableGpsTime = false;





    // Raspberry PIs have some subtle different behaviour. Use this Boolean to toggle
    public static final boolean kIsRPI = Build.MODEL.contains("rpi3");




}
