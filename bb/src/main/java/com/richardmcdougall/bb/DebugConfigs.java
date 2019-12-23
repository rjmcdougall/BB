package com.richardmcdougall.bb;

// the intent of this class is to centralize all of the config settings we have stashed
// in order to enable our debugging.
public class DebugConfigs {

    // if this is true, the video will show in the app. This isn't needed
    // in embedded mode, so conserve the resources.
    public static boolean DISPLAY_VIDEO_IN_APP = true;

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

    // name the profile as a testing overridee.
    public static final String OVERRIDE_BOARD_DOWNLOAD_PROFILE = "supersexy";

    // Set to force classic mode when using Emulator
    public static final boolean EMULATING_CLASSIC = false;

    // Switch any of these to 'true' to force identification as that board type.
    public static final boolean FORCE_BB_TYPE_AZUL = false;
    public static final boolean FORCE_BB_TYPE_CLASSIC = false;
    public static final boolean FORCE_BB_TYPE_DIRECT_MAP = false;
    public static final boolean FORCE_BB_TYPE_MAST = false;
    public static final boolean FORCE_BB_TYPE_PANEL = false;

}
