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
