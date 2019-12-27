package com.richardmcdougall.bb;

import android.os.Build;
import java.io.*;


public class BurnerBoardUtil {
    /*
        Feature flag section here
    */

    public enum BoardType {
        azul("azul"),
        panel("panel"),
        mast("mast"),
        classic("classic"),
        boombox("boombox"),
        backpack("backpack"),
        unknown("unknown");

        private String stringValue;

        BoardType(final String toString) {
            stringValue = toString;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    // This enabled GPS Time being polled
    public static final boolean fEnableGpsTime = false;

    /* Step one to make WiFi configurable is to pull out the strings */
    public static final String WIFI_SSID = "burnerboard";
    public static final String WIFI_PASS = "firetruck";

    // Known board types we have
    public static final String BOARD_TYPE = Build.MANUFACTURER;

    // String by which to identify the Satechi remotes; it's their mac address, which always
    // starts with DC:
    public static final String MEDIA_CONTROLLER_MAC_ADDRESS_PREFIX = "DC:";

    // radio packet codes
    public static final int kRemoteAudioTrack = 0x01;
    public static final int kRemoteVideoTrack = 0x02;
    public static final int kRemoteMute = 0x03;
    public static final int kRemoteMasterName = 0x04;

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
    public static final boolean kIsNano = Build.MODEL.contains("NanoPC-T4");



    /* DIRECT MAP SETTINGS */
    public static final int kVisualizationDirectMapDefaultWidth = 8;
    public static final int kVisualizationDirectMapDefaultHeight = 256;

    // JosPacks have 1x166 strands of LEDs. Currently RPI == JosPack
    public static final int kVisualizationDirectMapWidth = BurnerBoardUtil.kIsRPI ? 1 : kVisualizationDirectMapDefaultWidth;
    public static final int kVisualizationDirectMapHeight = BurnerBoardUtil.kIsRPI ? 166 : kVisualizationDirectMapDefaultHeight;

    /* JosPacks have more of a power constraint, so we don't want to set it to full brightness. Empirically tested
        with with a rapidly refreshing pattern (BlueGold):
        100 -> 1.90a draw
        50  -> 0.50a draw
        25  -> 0.35a draw
    */
    public static final int kVisualizationDirectMapPowerMultiplier = BurnerBoardUtil.kIsRPI ? 25 : 100; // should be ok for nano

    /*

    THIS SETS UP PRETTY / HUMAN NAMES FOR ANY DEVICES

     */
    public static final String wifiJSON = "wifi.json";
    public static final String favoritesJSON = "favorites.json";



}
