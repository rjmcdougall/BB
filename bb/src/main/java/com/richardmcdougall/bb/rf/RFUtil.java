package com.richardmcdougall.bb.rf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class RFUtil {

    // radio packet codes
    public static final int REMOTE_AUDIO_TRACK_CODE = 0x01;
    public static final int REMOTE_VIDEO_TRACK_CODE = 0x02;
    public static final int REMOTE_VOLUME_CODE = 0x03;
    public static final int REMOTE_MASTER_NAME_CODE = 0x04;
    public static final int MAX_LOCATION_STORAGE_MINUTES = 30;
    public static final int LOCATION_INTERVAL_MINUTES = 5;

    public static final int[] kClientSyncMagicNumber = new int[]{0xbb, 0x03};
    public static final int[] kServerSyncMagicNumber = new int[]{0xbb, 0x04};
    public static final int[] kServerBeaconMagicNumber = new int[]{0xbb, 0x05};
    public static final int[] kRemoteControlMagicNumber = new int[]{0xbb, 0x06};
    public static final int[] kGPSMagicNumber = new int[]{0xbb, 0x01};
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


    public static byte[] longToBytes(long l, byte[] result, int offset) {
        for (int i = 7; i >= 0; i--) {
            result[i + offset] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static long bytesToLong(byte[] b, int offset) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i + offset] & 0xFF);
        }
        return result;
    }

    public static void WriteMagicNumber(ByteArrayOutputStream bytes, int[] magicNumber){
        for (int i = 0; i < magicNumber.length; i++) {
            bytes.write(magicNumber[i]);
        }
    }

    public static void int32ToPacket(ByteArrayOutputStream bytes, long n) {
        bytes.write((byte) (n & 0xFF));
        bytes.write((byte) ((n >> 8) & 0xFF));
        bytes.write((byte) ((n >> 16) & 0xFF));
        bytes.write((byte) ((n >> 24) & 0xFF));
    }

    public static void stringToPacket(ByteArrayOutputStream bytes, String s) {
        try {
            bytes.write(s.getBytes());
        } catch (Exception e) {
        }
    }

    public static long int16FromPacket(ByteArrayInputStream bytes) {
        return ((long) ((bytes.read() & (long) 0xff) +
                ((bytes.read() & (long) 0xff) << 8)));
    }

    public static void int16ToPacket(ByteArrayOutputStream bytes, int n) {
        bytes.write((byte) (n & 0xFF));
        bytes.write((byte) ((n >> 8) & 0xFF));
    }

    public static void boolToPacket(ByteArrayOutputStream bytes, boolean b) {
        bytes.write((byte) ((b ? 1 : 0 ) & 0xFF));
    }

    public static boolean boolFromPacket(ByteArrayInputStream bytes) {
        return  (bytes.read() & (long) 0xff) !=0;
    }

    public static long int64FromPacket(ByteArrayInputStream bytes) {
        return ((long) ((bytes.read() & (long) 0xff) +
                ((bytes.read() & (long) 0xff) << 8) +
                ((bytes.read() & (long) 0xff) << 16) +
                ((bytes.read() & (long) 0xff) << 24) +
                ((bytes.read() & (long) 0xff) << 32) +
                ((bytes.read() & (long) 0xff) << 40) +
                ((bytes.read() & (long) 0xff) << 48) +
                ((bytes.read() & (long) 0xff) << 56)));
    }

    public static void int64ToPacket(ByteArrayOutputStream bytes, long n) {
        bytes.write((byte) (n & 0xFF));
        bytes.write((byte) ((n >> 8) & 0xFF));
        bytes.write((byte) ((n >> 16) & 0xFF));
        bytes.write((byte) ((n >> 24) & 0xFF));
        bytes.write((byte) ((n >> 32) & 0xFF));
        bytes.write((byte) ((n >> 40) & 0xFF));
        bytes.write((byte) ((n >> 48) & 0xFF));
        bytes.write((byte) ((n >> 56) & 0xFF));
    }

    public static long int32FromPacket(ByteArrayInputStream bytes) {
        return ((long) ((bytes.read() & (long) 0xff) +
                ((bytes.read() & (long) 0xff) << 8) +
                ((bytes.read() & (long) 0xff) << 16) +
                ((bytes.read() & (long) 0xff) << 24)));
    }

}
