package com.richardmcdougall.bb;

public class RFUtil {

    // radio packet codes
    public static final int REMOTE_AUDIO_TRACK_CODE = 0x01;
    public static final int REMOTE_VIDEO_TRACK_CODE = 0x02;
    public static final int REMOTE_MUTE_CODE = 0x03;
    public static final int REMOTE_MASTER_NAME_CODE = 0x04;
    public static final int MAX_LOCATION_STORAGE_MINUTES = 180;
    public static final int LOCATION_INTERVAL_MINUTES = 1;

    public static final int[] kClientSyncMagicNumber = new int[]{0xbb, 0x03};
    public static final int[] kServerSyncMagicNumber = new int[]{0xbb, 0x04};
    public static final int[] kServerBeaconMagicNumber = new int[]{0xbb, 0x05};
    public static final int[] kRemoteControlMagicNumber = new int[]{0xbb, 0x06};
    public static final int[] kTrackerMagicNumber = new int[]{0x02, 0xcb};
    public static final int[] kGPSMagicNumber = new int[]{0xbb, 0x01};
    public static final int[] kFavoritesMagicNumber = new int[]{0xbb, 0x08};
    public static final int kMagicNumberLen = 2;

    public static final int magicNumberToInt(int[] magic) {
        int magicNumber = 0;
        for (int i = 0; i < kMagicNumberLen; i++) {
            magicNumber = magicNumber + (magic[i] << ((kMagicNumberLen - 1 - i) * 8));
        }
        return (magicNumber);
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
