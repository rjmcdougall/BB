package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

public class BurnerBoardClassic extends BurnerBoard {

    // Convert other lights to pixel buffer address
    public static final int kOtherLights = 2;
    public static final int kLeftSightlight = 0;
    public static final int kRightSidelight = 1;
    private String TAG = this.getClass().getSimpleName();
    private int mBoardSideLights = 79;
    private int[] mBoardOtherlights;
    private PixelDimmer mDimmer = new PixelDimmer();

    private GammaCorrection mGammaCorrection = new GammaCorrection();

    static {
        textSizeHorizontal = 6;
        textSizeVertical = 12;
        enableBatteryMonitoring = true;
        enableIOTReporting = true;
        renderTextOnScreen = false;
        boardType = BoardState.BoardType.classic;
        renderLineOnScreen = false;
    }

    public BurnerBoardClassic(BBService service) {
        super(service);
        BLog.i(TAG, "Burner Board Classic initing...");
        boardWidth = 10;
        boardHeight = 70;
        mBoardOtherlights = new int[mBoardSideLights * 3 * 2];

    }

    public int getMultiplier4Speed() {
        return 1;
    }

    public int getFrameRate() {
        return 16;
    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardClassic.BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(true);
        mListener.attach(10, getBatteryLevelCallback);

    }

    @Override
    public boolean update() {

        this.appDisplay.sendVisual(8);
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

    @Override
    public void showBattery(batteryType type) {

        this.appDisplay.sendVisual(9);
        BLog.d(TAG, "sendCommand: 9");
        if (mListener != null) {
            mListener.sendCmd(9);
            mListener.sendCmdEnd();
            flush2Board();
            return;
        }
        return;
    }

    public int getBatteryVoltage() {
        return mBatteryStats[5];
    }

    public boolean setOtherlight(int other, int[] pixels) {

        // Send pixel row to in-app visual display
        this.appDisplay.sendVisual(15, other, pixels);

        byte[] newPixels = new byte[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            newPixels[pixel] = (byte) pixels[pixel];
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

    public void initpixelMap2Board() {

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

    int pixelOtherlight2Offset(int pixel, int other, int rgb) {

        return (other * mBoardSideLights + pixel) * 3 + rgb;
    }

    public void setOtherlightsAutomatically() {
        for (int pixel = 0; pixel < mBoardSideLights; pixel++) {
            // Calculate sidelight proportional to lengths
            int fromY = (int) ((float) pixel * (float) boardHeight / (float) mBoardSideLights);
            setPixelOtherlight(pixel, kLeftSightlight,
                    boardScreen[this.pixelOffset.Map(0, fromY, PIXEL_RED)],
                    boardScreen[this.pixelOffset.Map(0, fromY, PIXEL_GREEN)],
                    boardScreen[this.pixelOffset.Map(0, fromY, PIXEL_BLUE)]);
            setPixelOtherlight(pixel, kRightSidelight,
                    boardScreen[this.pixelOffset.Map(9, fromY, PIXEL_RED)],
                    boardScreen[this.pixelOffset.Map(9, fromY, PIXEL_GREEN)],
                    boardScreen[this.pixelOffset.Map(9, fromY, PIXEL_BLUE)]);
        }
    }

    @Override // looks like the side lights are controlled differently so this varies
    public void scrollPixels(boolean down) {

        if (boardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight - 1; y++) {
                    boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)] =
                            boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_RED)];
                    boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)] =
                            boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_GREEN)];
                    boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)] =
                            boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_BLUE)];
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
                    boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)] =
                            boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_RED)];
                    boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)] =
                            boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_GREEN)];
                    boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)] =
                            boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_BLUE)];
                }
            }
        }
    }

    private boolean setRow(int row, int[] pixels) {

        // Send pixel row to in-app visual display
        this.appDisplay.sendVisual(14, row, pixels);

        byte[] newPixels = new byte[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            newPixels[pixel] = (byte) pixels[pixel];
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

    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = mGammaCorrection.Correct(mOutputScreen);
        mOutputScreen = mDimmer.Dim(0, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

        // Suppress updating when displaying a text message
        int[] rowPixels = new int[boardWidth * 3];
        for (int y = 0; y < boardHeight; y++) {
            //for (int y = 30; y < 31; y++) {
            for (int x = 0; x < boardWidth; x++) {
                if (y < boardHeight) {
                    rowPixels[x * 3 + 0] = boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)];
                    rowPixels[x * 3 + 1] = boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)];
                    rowPixels[x * 3 + 2] = boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)];
                }
            }

            setRow(y, rowPixels);

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