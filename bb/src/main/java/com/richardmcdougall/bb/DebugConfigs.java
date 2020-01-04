package com.richardmcdougall.bb;

import java.util.ArrayList;

// the intent of this class is to centralize all of the config settings we have stashed
// in order to enable our debugging.
public class DebugConfigs {

    // if this is true, the video will show in the app. This isn't needed
    // in embedded mode, so conserve the resources.
    // works with BoardType classic and azul to set the screen grid for video display
    public static boolean DISPLAY_VIDEO_IN_APP = false;

    // lots of logging for decoder. LOTS
    public static boolean VIDEO_DECODER_VERBOSE_LOGGING = false;

    static final ArrayList<String> ExcludeFromLogs = new ArrayList<String>() {{
        add("MusicPlayer");
//        add("BatterySupervisor");
//        add("FindMyFriends");
//        add("RF");
//        add("RFClientServer");
//        add("Favorites");
//        add("AllBoards");
//        add("MediaManager");
//        add("BBWifi");
//        add("BoardState");
//        add("BluetoothCommands");
//        add("BluetoothLEServer");
//        add("ContentProvider");
//        add("BoardVisualization");
//        add("BurnerBoardUtil");
//        add("BurnerBoardAzul");
//        add("BurnerBoardPanel");
//        add("BurnerBoardClassic");
//        add("BurnerBoardDirectMap");
//        add("BurnerBoardMast");
        //add("BBService");
    }};

    public static final boolean DEBUG_RF_CLIENT_SERVER = false;

    // name the board as a testing overriden.
    public static final String OVERRIDE_PUBLIC_NAME = "";

    //  force identification as that board type.
    //if you want to debug in the app you need to set this to classic or azul
    public static final BurnerBoardUtil.BoardType OVERRIDE_BOARD_TYPE = null;

    //bypass the per-second music sync because of debug perf issues
    public static final boolean BYPASS_MUSIC_SYNC = false;


}
