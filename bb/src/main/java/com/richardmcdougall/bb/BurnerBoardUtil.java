package com.richardmcdougall.bb;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;


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

    // String by which to identify the Satechi remotes; it's their mac address, which always
    // starts with DC:
    public static final String MEDIA_CONTROLLER_MAC_ADDRESS_PREFIX = "DC:";

    // radio packet codes
    public static final int kRemoteAudioTrack = 0x01;
    public static final int kRemoteVideoTrack = 0x02;
    public static final int kRemoteMute = 0x03;
    public static final int kRemoteMasterName = 0x04;


    /* DIRECT MAP SETTINGS */
    public static final int kVisualizationDirectMapDefaultWidth = 8;
    public static final int kVisualizationDirectMapDefaultHeight = 256;

    // JosPacks have 1x166 strands of LEDs. Currently RPI == JosPack
    public static final int kVisualizationDirectMapWidth = BoardState.kIsRPI ? 1 : kVisualizationDirectMapDefaultWidth;
    public static final int kVisualizationDirectMapHeight = BoardState.kIsRPI ? 166 : kVisualizationDirectMapDefaultHeight;

    /* JosPacks have more of a power constraint, so we don't want to set it to full brightness. Empirically tested
        with with a rapidly refreshing pattern (BlueGold):
        100 -> 1.90a draw
        50  -> 0.50a draw
        25  -> 0.35a draw
    */
    public static final int kVisualizationDirectMapPowerMultiplier = BoardState.kIsRPI ? 25 : 100; // should be ok for nano

    /*

    THIS SETS UP PRETTY / HUMAN NAMES FOR ANY DEVICES

     */
    public static final String wifiJSON = "wifi.json";
    public static final String favoritesJSON = "favorites.json";

    // Hash String as 32-bit
    public static long hashTrackName(String name) {
        byte[] encoded = {0, 0, 0, 0};
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            encoded = digest.digest(name.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return -1;
        }
        return (encoded[0] << 24) + (encoded[1] << 16) + (encoded[2] << 8) + encoded[0];
    }

}
