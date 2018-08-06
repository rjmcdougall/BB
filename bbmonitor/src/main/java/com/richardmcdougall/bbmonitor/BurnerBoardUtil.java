package com.richardmcdougall.bbmonitor;

import android.os.Build;

public class BurnerBoardUtil {
    /*
        Feature flag section here
    */

    // This enabled GPS Time being polled
    public static final boolean fEnableGpsTime = false;


    /* Step one to make WiFi configurable is to pull out the strings */
    public static final String WIFI_SSID = "burnerboard";
    public static final String WIFI_PASS = "firetruck";


    // Known board types we have
    public static final String BOARD_TYPE = Build.MANUFACTURER;

    /* XXX TODO refactor out the string use cases and transform to constants -jib
    public static final String BB_TYPE_AZUL = "Burner Board Azul";
    public static final String BB_TYPE_CLASSIC = "Burner Board Classi";
    public static final String BB_TYPE_DIRECT_MAP = "Burner Board DirectMap";
    public static final String BB_TYPE_MAST = "Burner Board Mast";
    public static final String BB_TYPE_PANEL = "Burner Board Panel";
    /*

     */
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

    // Switch any of these to 'true' to force identification as that board type.
    // Only useful for debugging! -jib
    private static boolean kForceBBTypeAzul = false;
    private static boolean kForceBBTypeClassic = false;
    private static boolean kForceBBTypeDirectMap = false;
    private static boolean kForceBBTypeMast = false;
    private static boolean kForceBBTypePanel = false;

    public static final boolean isBBAzul() {
        return (kForceBBTypeAzul || BOARD_TYPE.contains("Azul")) ? true : false;
    }

    public static final boolean isBBClassic() {
        return (kForceBBTypeClassic || BOARD_TYPE.contains("Classic")) ? true : false;
    }

    public static final boolean isBBDirectMap() {
        return (kForceBBTypeDirectMap || BOARD_ID.contains("mickey")) ? true : false;
    }

    public static final boolean isBBMast() {
        return (kForceBBTypeMast || BOARD_TYPE.contains("Mast") || BOARD_ID.contains("test")) ? true : false;
    }

    /*  rjmcdougall says:
        Obviously we need a better persistent way of setting board types on android things.

        The embedded android 6 we deploy allows us to set the board type at provision time via USB.

        With android things we either need to allow config through the cloud or through the app.
    */
    public static final boolean isBBPanel() {
        return (kForceBBTypePanel
            || BOARD_ID.contains("Panel")
            || BOARD_ID.contains("cranky")
            || BOARD_ID.contains("grumpy")
            || BOARD_ID.contains("imx7d_pico")
            || BurnerBoardUtil.kIsRPI
        ) ? true : false;
    }
}