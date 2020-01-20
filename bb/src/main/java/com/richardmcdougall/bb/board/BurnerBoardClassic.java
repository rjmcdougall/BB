package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.BLog;
import com.richardmcdougall.bb.CmdMessenger;

import java.nio.IntBuffer;

public class BurnerBoardClassic extends BurnerBoard {

    private int mBoardSideLights = 79;
    //private int[] mBoardScreen;
    private int[] mBoardOtherlights;
    private static final String TAG = "BB.BurnerBoardClassic";

    public BurnerBoardClassic(BBService service) {
        super(service);
        boardWidth = 10;
        boardHeight = 70;
        mTextSizeHorizontal = 6;
        boardType = "Burner Board Classic";
        // Std board e.g. is 10 x 70 + 2 rows of sidelights of 79
        mBoardScreen = new int[boardWidth * boardHeight * 3];
        mBoardOtherlights = new int[mBoardSideLights * 3 * 2];
        initPixelOffset();
        initUsb();
        mTextBuffer = IntBuffer.allocate(boardWidth * boardHeight * 4);
    }

    public int getMultiplier4Speed() {
            return 1;
    }

    // Experiments with optimized overlocked Teensy suggest 20 is to high
    // because we don't get battery callbacks
    public int getFrameRate() {
        return 16;
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

        // attach Board ID cmdMessenger callback
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

    public class BoardCallbackGetBatteryLevel implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            for (int i = 0; i < mBatteryStats.length; i++) {
                mBatteryStats[i] = mListener.readIntArg();
            }
            if (mBatteryStats[1] != -1) {
                service.boardState.batteryLevel = mBatteryStats[1];
            } else {
                service.boardState.batteryLevel = 100;
            }
            BLog.d(TAG, "getBatteryLevel: " + service.boardState.batteryLevel);
        }
    }

    private static final int kClassicBatteryMah = 38000;

    public int getBatteryHealth() {
        return 100 * mBatteryStats[5] / kClassicBatteryMah;
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
    }//    cmdMessenger.attach(BBsetmode, Onsetmode);            // 4

    @Override
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
        BLog.d(TAG, "sendCommand: 9");
        if (mListener != null) {
            mListener.sendCmd(9);
            mListener.sendCmdEnd();
            flush2Board();
            return;
        }
        return;
    }

    //    cmdMessenger.attach(BBGetMode, OnGetMode);            // 12
    public int getMode() {

        BLog.d(TAG, "sendCommand: 12");
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
        byte[] newPixels = new byte[boardWidth * 3];
        for (int pixel = 0; pixel < boardWidth * 3; pixel = pixel + 3) {
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
    }//    cmdMessenger.attach(BBSetRow, OnSetRow);      // 16
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
        }return false;
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

    static int[][][] pixel2OffsetTable = new int[255][255][3];

    // Convert other lights to pixel buffer address
    public static final int kOtherLights = 2;
    public static final int kLeftSightlight = 0;
    public static final int kRightSidelight = 1;

    int pixelOtherlight2Offset(int pixel, int other, int rgb) {

        return (other * mBoardSideLights + pixel) * 3 + rgb;
    }

    public void setOtherlightsAutomatically() {
        for (int pixel = 0; pixel < mBoardSideLights; pixel++) {
            // Calculate sidelight proportional to lengths
            int fromY = (int) ((float) pixel * (float) boardHeight / (float) mBoardSideLights);
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

    @Override // looks like the side lights are controlled differently so this varies
    public void scrollPixels(boolean down) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight - 1; y++) {
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
            for (int x = 0; x < boardWidth; x++) {
                for (int y = boardHeight - 2; y >= 0; y--) {
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

    @Override
    public void scrollPixelsExcept(boolean down, int color) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight - 1; y++) {
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
            for (int x = 0; x < boardWidth; x++) {
                for (int y = boardHeight - 2; y >= 0; y--) {
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

    private int flushCnt = 0;
    long lastFlushTime = java.lang.System.currentTimeMillis();

    public void flush() {

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            BLog.d(TAG, "Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }

        // Suppress updating when displaying a text message
        if (isTextDisplaying > 0) {
            isTextDisplaying--;
        } else {
            int[] rowPixels = new int[boardWidth * 3];
            for (int y = 0; y < boardHeight; y++) {
                //for (int y = 30; y < 31; y++) {
                for (int x = 0; x < boardWidth; x++) {
                    if (y < boardHeight) {
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
            }for (int x = 0; x < kOtherLights; x++) {
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
