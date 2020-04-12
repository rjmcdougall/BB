package com.richardmcdougall.bbcommon;

import com.richardmcdougall.bbcommon.BoardState;

// the intent of this class is to centralize all of the config settings we have stashed
// in order to enable our debugging.
public class DebugConfigs {

    // if this is true, the video will show in the app. This isn't needed
    // in embedded mode, so conserve the resources.
    // works with BoardType classic and azul to set the screen grid for video display
    public static boolean DISPLAY_VIDEO_IN_APP = false;

    // name the board as a testing overriden.
    public static final String OVERRIDE_PUBLIC_NAME = "";
    public static final String OVERRIDE_DEVICE_ID = "";

    //  force identification as that board type.
    //if you want to debug in the app you need to set this to classic or azul
    public static final BoardState.BoardType OVERRIDE_BOARD_TYPE = null;


    //if you want to debug in the app you need to set this to cpu type
    public static final BoardState.PlatformType OVERRIDE_PLATFORM_TYPE = BoardState.PlatformType.none;;


    //bypass the per-second music sync because of debug perf issues
    public static final boolean BYPASS_MUSIC_SYNC = false;

}
