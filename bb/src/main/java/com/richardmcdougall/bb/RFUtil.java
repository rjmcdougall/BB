package com.richardmcdougall.bb;


public class RFUtil {

    static final int MAX_LOCATION_STORAGE_MINUTES = 180;
    static final int LOCATION_INTERVAL_MINUTES = 1;

    static final int [] kClientSyncMagicNumber =        new int[] {0xbb, 0x03};
    static final int [] kServerSyncMagicNumber =        new int[] {0xbb, 0x04};
    static final int [] kServerBeaconMagicNumber =      new int[] {0xbb, 0x05};
    static final int [] kRemoteControlMagicNumber =     new int[] {0xbb, 0x06};
    static final int [] kTrackerMagicNumber =           new int[] {0x02, 0xcb};
    static final int [] kGPSMagicNumber =               new int[] {0xbb, 0x01};
    static final int [] kFavoritesMagicNumber =         new int[] {0xbb, 0x08};
    static final int kMagicNumberLen = 2;

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
