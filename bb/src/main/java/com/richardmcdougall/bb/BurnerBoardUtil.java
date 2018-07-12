package com.richardmcdougall.bb;

import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public class BurnerBoardUtil {
    public static final String BOARD_TYPE = Build.MANUFACTURER;

    // Known board types we have
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

    /*
        XXX TODO: Azul is currently the 'fall through' case in BBService. There's probably a positive way to
        identify an Azul board. -jib
     */
    public static final boolean isBBAzul() {
        boolean isOtherType = isBBClassic() || isBBDirectMap() || isBBMast() || isBBPanel();
        return (kForceBBTypeAzul || !isOtherType) ? true : false;
    }

    public static final boolean isBBClassic() {
        return (kForceBBTypeClassic || BOARD_TYPE.contains("Classic")) ? true : false;
    }

    public static final boolean isBBDirectMap() {
        return (kForceBBTypeDirectMap || BOARD_ID.contains("mickey")) ? true : false;
    }

    public static final boolean isBBMast() {
        return (kForceBBTypeMast || BOARD_ID.contains("Mast") || BOARD_ID.contains("test")) ? true : false;
    }

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