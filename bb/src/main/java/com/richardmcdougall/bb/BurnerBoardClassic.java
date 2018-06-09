package com.richardmcdougall.bb;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.nio.IntBuffer;
import java.util.Arrays;


/**
 * Created by rmc on 7/24/17.
 */

public class BurnerBoardClassic extends BurnerBoard {


    private int mBoardSideLights = 79;
    //private int[] mBoardScreen;
    private int[] mBoardOtherlights;
    private int mDimmerLevel = 255;
    public int mBatteryLevel = -1;
    public int[] mBatteryStats = new int[16];
    private static final String TAG = "BB.BurnerBoardClassic";


    public BurnerBoardClassic(BBService service, Context context) {
        super(service, context);
        mBoardWidth = 10;
        mBoardHeight = 70;
        mTextSizeHorizontal = 6;
        boardId = Build.MODEL;
        boardType = "Burner Board Classic";
        // Std board e.g. is 10 x 70 + 2 rows of sidelights of 79
        mBoardScreen = new int[mBoardWidth * mBoardHeight * 3];
        mBoardOtherlights = new int[mBoardSideLights * 3 * 2];
        mBBService = service;
        mContext = context;
        initPixelOffset();
        initUsb();
        mTextBuffer = IntBuffer.allocate(mBoardWidth * mBoardHeight * 4);
    }

    public void resetParams() {
        mDimmerLevel = 255;
    }

    public void start() {

        getBoardId();
        getMode();

        // attach default cmdMessenger callback
        BurnerBoardClassic.BoardCallbackDefault defaultCallback =
                new BurnerBoardClassic.BoardCallbackDefault();
        mListener.attach(defaultCallback);

        // attach Test cmdMessenger callback
        BurnerBoardClassic.BoardCallbackTest testCallback =
                new BurnerBoardClassic.BoardCallbackTest();
        mListener.attach(5, testCallback);

        // attach Mode cmdMessenger callback
        BurnerBoardClassic.BoardCallbackMode modeCallback =
                new BurnerBoardClassic.BoardCallbackMode();
        mListener.attach(4, modeCallback);

        // attach Mode cmdMessenger callback
        BurnerBoardClassic.BoardCallbackBoardID boardIDCallback =
                new BurnerBoardClassic.BoardCallbackBoardID();
        mListener.attach(11, boardIDCallback);

        // attach echoRow cmdMessenger callback
        BurnerBoardClassic.BoardCallbackEchoRow echoCallback =
                new BurnerBoardClassic.BoardCallbackEchoRow();
        mListener.attach(17, echoCallback);

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardClassic.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardClassic.BoardCallbackGetBatteryLevel();
        mListener.attach(10, getBatteryLevelCallback);

    }

    public class BoardCallbackDefault implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            Log.d(TAG, "ardunio default callback:" + str);
        }
    }

    public class BoardCallbackTest implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            l("ardunio test callback:" + str);
        }
    }

    public class BoardCallbackMode implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            int boardMode = mListener.readIntArg();
            boardCallback.BoardMode(boardMode);
        }
    }

    public class BoardCallbackBoardID implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            //String boardId = mListener.readStringArg();
            //boardCallback.BoardId(boardId);
        }
    }

    public class BoardCallbackEchoRow implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            mEchoString = mListener.readStringArg();
            l("echoRow: " + mEchoString);
        }
    }

    public class BoardCallbackGetBatteryLevel implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            for (int i = 0; i < mBatteryStats.length; i++) {
                mBatteryStats[i] = mListener.readIntArg();
            }
            if (mBatteryStats[1] != -1) {
                mBatteryLevel = mBatteryStats[1];
            } else {
                mBatteryLevel = 100;
            }
            l("getBatteryLevel: " + mBatteryLevel);
        }
    }

    public int getBattery() {
        return mBatteryLevel;
    }

    public String getBatteryStats() {
        return Arrays.toString(mBatteryStats);
    }

    public int getBatteryCurrent() {
        int codedLevel = mBatteryStats[6];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    public int getBatteryCurrentInstant() {
        int codedLevel = mBatteryStats[9];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    public int getBatteryVoltage() {
        return mBatteryStats[5];
    }

    //    cmdMessenger.attach(BBGetBoardID, OnGetBoardID);      // 11
    public String getBoardId() {

        String id;
        if (mListener != null) {
            mListener.sendCmdStart(11);
            mListener.sendCmdEnd(true, 0, 1000);
            flush2Board();
            id = mListener.readStringArg();
            return (id);
        }
        return "";
    }


    //    cmdMessenger.attach(BBsetmode, Onsetmode);            // 4
    public boolean setModeRetired(int mode) {

        l("sendCommand: 4," + mode);
        if (mListener == null) {
            initUsb();
        }
        // Disable now that the board doesn't have a selector switch
        return true;

        /*
        if (mListener == null)

            return false;

        synchronized (mSerialConn) {

            if (mListener != null) {
                mListener.sendCmdStart(4);
                mListener.sendCmdArg(mode);
                mListener.sendCmdEnd();
                flush2Board();
                return true;
            }
        }
        return true;
        */
    }


    //    cmdMessenger.attach(BBFade, OnFade);                  // 7
    public boolean fadeBoard(int amount) {

        //l("sendCommand: 7");
        sendVisual(7, amount);
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmd(7);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }

    public void fadePixels(int amount) {

        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {
                int r = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                //System.out.println("br = " + br);
                if (r >= amount) {
                    r -= amount;
                } else {
                    r = 0;
                }
                mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
                int g = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                if (g >= amount) {
                    g -= amount;
                } else {
                    g = 0;
                }
                mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
                int b = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                if (b >= amount) {
                    b -= amount;
                } else {
                    b = 0;
                }
                mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
            }
        }
    }


    public void fuzzPixels(int amount) {

        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {
                mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                        (mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] - amount);
                mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                        (mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] - amount);
                mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                        (mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] - amount);
            }
        }
    }


    //    cmdMessenger.attach(BBUpdate, OnUpdate);              // 8
    public boolean update() {

        sendVisual(8);
        //l("sendCommand: 8");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmd(8);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }


    //    cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 9
    public void showBattery() {

        sendVisual(9);
        l("sendCommand: 9");
        if (mListener != null) {
            mListener.sendCmd(9);
            mListener.sendCmdEnd();
            flush2Board();
            return;
        }
        return;
    }

    //    cmdMessenger.attach(BBsetheadlight, Onsetheadlight);  // 3
    public boolean setHeadlight(boolean state) {

        sendVisual(3);
        l("sendCommand: 3,1");
        if (mListener != null) {
            mListener.sendCmdStart(3);
            mListener.sendCmdArg(state == true ? 1 : 0);
            mListener.sendCmdEnd();
            flush2Board();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBClearScreen, OnClearScreen);    // 5
    public boolean clearScreen() {

        sendVisual(5);
        l("sendCommand: 5");
        if (mListener != null) {
            mListener.sendCmd(5);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBScroll, OnScroll);              // 6
    public boolean scroll(boolean down) {

        sendVisual(6, down == true ? 1 : 0);
        //l("sendCommand: 6,1");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(6);
                mListener.sendCmdArg(down == true ? 1 : 0);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }

    //    cmdMessenger.attach(BBGetMode, OnGetMode);            // 12
    public int getMode() {

        l("sendCommand: 12");
        int mode;

        if (mListener != null) {
            mListener.sendCmd(12);
            mListener.sendCmdEnd();
            flush2Board();
            mode = mListener.readIntArg();
            return (mode);
        }
        return -1;
    }


    public void fillScreen(int r, int g, int b) {

        //System.out.println("Fillscreen " + r + "," + g + "," + b);
        int x;
        int y;
        for (x = 0; x < mBoardWidth; x++) {
            for (y = 0; y < mBoardHeight; y++) {
                setPixel(x, y, r, g, b);
            }
        }
    }

    public void setMsg(String msg) {
    }


    //    cmdMessenger.attach(BBSetRow, OnSetRow);      // 16
    // row is 12 pixels : board has 10
    private boolean setRow(int row, int[] pixels) {

        int[] dimPixels = new int[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            dimPixels[pixel] =
                    (mDimmerLevel * pixels[pixel]) / 255;
        }

        // Send pixel row to in-app visual display
        sendVisual(14, row, dimPixels);

        // Do color correction on burner board display pixels
        byte[] newPixels = new byte[mBoardWidth * 3];
        for (int pixel = 0; pixel < mBoardWidth * 3; pixel = pixel + 3) {
            newPixels[pixel] = (byte) pixelColorCorrectionRed(dimPixels[pixel]);
            newPixels[pixel + 1] = (byte) pixelColorCorrectionGreen(dimPixels[pixel + 1]);
            newPixels[pixel + 2] = (byte) pixelColorCorrectionBlue(dimPixels[pixel + 2]);
        }

        //System.out.println("flush row:" + y + "," + bytesToHex(newPixels));

        //l("sendCommand: 14,n,...");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(14);
                mListener.sendCmdArg(row);
                mListener.sendCmdEscArg(newPixels);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }


    //    cmdMessenger.attach(BBSetRow, OnSetRow);      // 16
    public boolean setOtherlight(int other, int[] pixels) {


        // Send pixel row to in-app visual display
        sendVisual(15, other, pixels);

        // Do color correction on burner board display pixels
        //byte [] newPixels = new byte[pixels.length];
        byte[] newPixels = new byte[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel = pixel + 3) {
            newPixels[pixel] = (byte) pixelColorCorrectionRed(pixels[pixel]);
            newPixels[pixel + 1] = (byte) pixelColorCorrectionGreen(pixels[pixel + 1]);
            newPixels[pixel + 2] = (byte) pixelColorCorrectionBlue(pixels[pixel + 2]);
        }

        //System.out.println("flush row:" + y + "," + bytesToHex(newPixels));

        //byte[] testRow1 = "01234567890123456789012345678900123456789012345678901234567890123450123456789012345678901234567890012345678901234567890123456789012345".getBytes();

        //l("sendCommand: 15,n,...");

        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(15);
                mListener.sendCmdArg(other);
                mListener.sendCmdEscArg(newPixels);
                mListener.sendCmdEnd();
                return true;
            }
        }


        return false;
    }


    public void clearPixels() {
        Arrays.fill(mBoardScreen, 0);
    }


    public void dimPixels(int level) {
        mDimmerLevel = level;
    }


    // Other lights
    // Side lights: other = 1 left, other = 2 right
    public void setPixelOtherlight(int pixel, int other, int color) {

        int r = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int b = ((color & 0xff0000) >> 16);
        setPixelOtherlight(pixel, other, r, g, b);
    }

    public void setPixelOtherlight(int pixel, int other, int r, int g, int b) {

        //System.out.println("setpixelotherlight pixel:" + pixel + " light:" + other);

        if (other < 0 || other >= 2 || pixel < 0 || pixel >= mBoardSideLights) {
            System.out.println("setPixel out of range: " + other + "," + pixel);
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                System.out.println(ste);
            }
            return;
        }
        mBoardOtherlights[pixelOtherlight2Offset(pixel, other, PIXEL_RED)] = r;
        mBoardOtherlights[pixelOtherlight2Offset(pixel, other, PIXEL_GREEN)] = g;
        mBoardOtherlights[pixelOtherlight2Offset(pixel, other, PIXEL_BLUE)] = b;
    }

    public void fillOtherlight(int other, int color) {

        int r = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int b = ((color & 0xff0000) >> 16);
        fillOtherlight(other, r, g, b);
    }

    public void fillOtherlight(int other, int r, int g, int b) {
        int nPixels = 0;
        final int pixelOffset = pixelOtherlight2Offset(0, other, 0);
        if (other == kRightSidelight || other == kLeftSightlight) {
            nPixels = mBoardSideLights;
        }
        for (int pixel = 0; pixel < nPixels * 3; pixel += 3) {
            mBoardOtherlights[pixel + pixelOffset] = r;
            mBoardOtherlights[pixel + pixelOffset + 1] = b;
            mBoardOtherlights[pixel + pixelOffset + 2] = g;
        }
    }

    static int PIXEL_RED = 0;
    static int PIXEL_GREEN = 1;
    static int PIXEL_BLUE = 2;

    static int[][][] pixel2OffsetTable = new int[255][255][3];

    private void initPixelOffset() {
        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {
                for (int rgb = 0; rgb < 3; rgb++) {
                    pixel2OffsetTable[x][y][rgb] = pixel2OffsetCalc(x, y, rgb);
                }
            }
        }
    }

    // TODO: Had to hardcode boardwidth to 10 for speed
    static int pixel2OffsetCalc(int x, int y, int rgb) {
        return (y * 10 + x) * 3 + rgb;
    }

    static int pixel2Offset(int x, int y, int rgb) {
        return pixel2OffsetTable[x][y][rgb];
    }

    // Convert other lights to pixel buffer address
    public static final int kOtherLights = 2;
    public static final int kLeftSightlight = 0;
    public static final int kRightSidelight = 1;

    int pixelOtherlight2Offset(int pixel, int other, int rgb) {

        return (other * mBoardSideLights + pixel) * 3 + rgb;
    }

    public void setPixel(int x, int y, int r, int g, int b) {
        //System.out.println("Setting pixel " + x + "," + y + " : " + pixel2Offset(x, y, PIXEL_RED) + " to " + r +  "," + g + "," + b);
        //l("setPixel(" + x + "," + y + "," + r + "," + g + "," + b + ")");
        //Sstem.out.println("setpixel r = " + r);
        if (x < 0 || x >= mBoardWidth || y < 0 || y >= mBoardHeight) {
            System.out.println("setPixel out of range: " + x + "," + y);
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                System.out.println(ste);
            }
            return;
        }
        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
        mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
        mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
    }

    public void setPixel(int pixel, int r, int g, int b) {

        if (pixel < 0 || pixel >= (mBoardWidth * mBoardHeight)) {
            System.out.println("setPixel out of range: " + pixel + "," + (mBoardWidth * mBoardHeight));
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                System.out.println(ste);
            }
            return;
        }
        mBoardScreen[pixel * 3] = r;
        mBoardScreen[pixel * 3 + 1] = g;
        mBoardScreen[pixel * 3 + 2] = b;
    }

    public void setSideLight(int leftRight, int pos, int color) {
        /// TODO: implement these
    }

    public void setOtherlightsAutomatically() {
        for (int pixel = 0; pixel < mBoardSideLights; pixel++) {
            // Calculate sidelight proportional to lengths
            int fromY = (int) ((float) pixel * (float) mBoardHeight / (float) mBoardSideLights);
            setPixelOtherlight(pixel, kLeftSightlight,
                    mBoardScreen[pixel2Offset(0, fromY, PIXEL_RED)],
                    mBoardScreen[pixel2Offset(0, fromY, PIXEL_GREEN)],
                    mBoardScreen[pixel2Offset(0, fromY, PIXEL_BLUE)]);
            setPixelOtherlight(pixel, kRightSidelight,
                    mBoardScreen[pixel2Offset(9, fromY, PIXEL_RED)],
                    mBoardScreen[pixel2Offset(9, fromY, PIXEL_GREEN)],
                    mBoardScreen[pixel2Offset(9, fromY, PIXEL_BLUE)]);
        }
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

    public void scrollPixels(boolean down) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = 0; y < mBoardHeight - 1; y++) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
            for (int x = 0; x < kOtherLights; x++) {
                for (int pixel = 0; pixel < mBoardSideLights - 1; pixel++) {
                    mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_RED)] =
                            mBoardOtherlights[pixelOtherlight2Offset(pixel + 1, x, PIXEL_RED)];
                    mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_GREEN)] =
                            mBoardOtherlights[pixelOtherlight2Offset(pixel + 1, x, PIXEL_GREEN)];
                    mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_BLUE)] =
                            mBoardOtherlights[pixelOtherlight2Offset(pixel + 1, x, PIXEL_BLUE)];
                }
            }
        } else {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = mBoardHeight - 2; y >= 0; y--) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
            // TODO: scroll up side lights
        }

    }

    public void scrollPixelsExcept(boolean down, int color) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = 0; y < mBoardHeight - 1; y++) {
                    if (getRGB(mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)],
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)],
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)]) != color) {
                        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                        mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                        mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                    }
                }
            }
            for (int x = 0; x < kOtherLights; x++) {
                for (int pixel = 0; pixel < mBoardSideLights - 1; pixel++) {
                    mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_RED)] =
                            mBoardOtherlights[pixelOtherlight2Offset(pixel + 1, x, PIXEL_RED)];
                    mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_GREEN)] =
                            mBoardOtherlights[pixelOtherlight2Offset(pixel + 1, x, PIXEL_GREEN)];
                    mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_BLUE)] =
                            mBoardOtherlights[pixelOtherlight2Offset(pixel + 1, x, PIXEL_BLUE)];
                }
            }
        } else {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = mBoardHeight - 2; y >= 0; y--) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
            // TODO: scroll up side lights
        }

    }


    public int[] getPixelBuffer() {
        return mBoardScreen;
    }

    // TODO: gamma correction
    // encoded = ((original / 255) ^ (1 / gamma)) * 255
    // original = ((encoded / 255) ^ gamma) * 255


    /*
    private int pixelColorCorrectionRed(int red) {
        // convert signed byte to double
        return red;
    }

    private int pixelColorCorrectionGreen(int green) {
        return (green * 450) / 1000;
    }

    private int pixelColorCorrectionBlue(int blue) {
        // convert signed byte to double
        double correctedBlue = blue & 0xff;
        correctedBlue = correctedBlue * .2;
        return (blue * 350) / 1000;
    }
*/
    private int pixelColorCorrectionRed(int red) {
        return gammaCorrect(red);
    }

    private int pixelColorCorrectionGreen(int green) {
        return gammaCorrect(green);
    }

    private int pixelColorCorrectionBlue(int blue) {
        return gammaCorrect(blue);
    }


    public void flush() {

        // Suppress updating when displaying a text message
        if (isTextDisplaying > 0) {
            isTextDisplaying--;
        } else {
            int[] rowPixels = new int[mBoardWidth * 3];
            for (int y = 0; y < mBoardHeight; y++) {
                //for (int y = 30; y < 31; y++) {
                for (int x = 0; x < mBoardWidth; x++) {
                    if (y < mBoardHeight) {
                        rowPixels[x * 3 + 0] = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                        rowPixels[x * 3 + 1] = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                        rowPixels[x * 3 + 2] = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                    }
                }

                //rowPixels[0] = rowPixels[3];
                //rowPixels[1] = rowPixels[4];
                //rowPixels[2] = rowPixels[5];
                //rowPixels[33] = rowPixels[30];
                //rowPixels[34] = rowPixels[31];
                //rowPixels[35] = rowPixels[32];
            /*
            if (rowPixels[0] ==0) {

                rowPixels[0]= 33;
                rowPixels[1]= 32;
                rowPixels[2]= 1;
                rowPixels[3]= 59;
                rowPixels[4]= 0;
                rowPixels[4]= 33;
                */
                setRow(y, rowPixels);
                //update();
            }


            for (int x = 0; x < kOtherLights; x++) {
                int[] otherPixels = new int[mBoardSideLights * 3];
                for (int pixel = 0; pixel < mBoardSideLights; pixel++) {
                    otherPixels[pixelOtherlight2Offset(pixel, 0, PIXEL_RED)] =
                            mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_RED)];
                    otherPixels[pixelOtherlight2Offset(pixel, 0, PIXEL_GREEN)] =
                            mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_GREEN)];
                    otherPixels[pixelOtherlight2Offset(pixel, 0, PIXEL_BLUE)] =
                            mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_BLUE)];
                }
                setOtherlight(x, otherPixels);
            }
            setOtherlightsAutomatically();
            update();
            flush2Board();
        }
    }

}
