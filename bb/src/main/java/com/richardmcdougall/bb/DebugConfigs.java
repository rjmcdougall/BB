package com.richardmcdougall.bb;

// the intent of this class is to centralize all of the config settings we have stashed
// in order to enable our debugging.
public class DebugConfigs {

    // if this is true, the video will show in the app. This isn't needed
    // in embedded mode, so conserve the resources.
    // works with BoardType classic and azul to set the screen grid for video display
    public static boolean DISPLAY_VIDEO_IN_APP = false;

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
    public static final boolean DEBUG_ALL_BOARDS = false;
    public static final boolean DEBUG_DOWNLOAD_MANAGER = false;
    public static final boolean DEBUG_BATTERY_SUPERVISOR = false;
    public static final boolean DEBUG_WIFI = false;
    public static final boolean DEBUG_BOARD_STATE = false;

    // name the board as a testing overriden.
    public static final String OVERRIDE_PUBLIC_NAME = "";

    //  force identification as that board type.
    //if you want to debug in the app you need to set this to classic or azul
    public static final BurnerBoardUtil.BoardType OVERRIDE_BOARD_TYPE = null;

    //bypass the per-second music sync because of debug perf issues
    public static final boolean BYPASS_MUSIC_SYNC = true;


}
