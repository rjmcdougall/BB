package com.richardmcdougall.bb;

// the intent of this class is to centralize all of the config settings we have stashed
// in order to enable our debugging.
public class DebugConfigs {

    // if this is true, the video will show in the app. This isn't needed
    // in embedded mode, so conserve the resources.
    // works with BoardType classic and azul to set the screen grid for video display
    public static boolean DISPLAY_VIDEO_IN_APP = false;
    public static final BurnerBoardUtil.BoardType EMULATING_VISUAL = BurnerBoardUtil.BoardType.azul;

    // lots of logging for decoder. LOTS
    public static boolean VIDEO_DECODER_VERBOSE_LOGGING = false;

    // these all used to be a single debug flag but i broke them out by type.
    public static final boolean DEBUG_MUSIC_PLAYER = false;
    public static final boolean DEBUG_FMF = false;
    public static final boolean DEBUG_RF = false;
    public static final boolean DEBUG_GPS = false;
    public static final boolean DEBUG_RF_CLIENT_SERVER = false;
    public static final boolean DEBUG_FAVORITES = false;
    public static final boolean DEBUG_BATTERY = false;
    public static final boolean DEBUG_ALL_BOARDS = true;
    public static final boolean DEBUG_DOWNLOAD_MANAGER = true;
    public static final boolean DEBUG_BATTERY_SUPERVISOR = false;
    public static final boolean DEBUG_WIFI = false;

    // name the board as a testing overriden.
    public static final String OVERRIDE_PUBLIC_NAME = "";

    // Switch any of these to 'true' to force identification as that board type.
    public static final BurnerBoardUtil.BoardType OVERRIDE_BOARD_TYPE = null;

}
