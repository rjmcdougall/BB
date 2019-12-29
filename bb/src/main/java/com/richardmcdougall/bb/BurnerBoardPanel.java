package com.richardmcdougall.bb;

import android.util.Log;
import java.util.Arrays;

/**
 * Created by rmc on 5/20/18.
 */


/*

   Back
         32,64
 +------+
 |      |
 |      |
 |      |
 |      |
 |      |
 |      | Y
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 +------+
 0,0 X
 Front

 */


public class BurnerBoardPanel extends BurnerBoard {
    //public int[] mBoardScreen;
    private static final String TAG = "BB.BurnerBoardPanel";
    public int mBatteryLevel = -1;
    public int [] mLayeredScreen;


    public BurnerBoardPanel(BBService service) {
        super(service);
        mBoardWidth = 32;
        mBoardHeight = 64;
        super.setTextBuffer(mBoardWidth, mBoardHeight);
        mMultipler4Speed = 3;
        boardId = service.boardState.BOARD_ID;
        boardType = "Burner Board Panel";
        l("Burner Board Panel initting...");
        mBoardScreen = new int[mBoardWidth * mBoardHeight * 3];
        initPixelOffset();
        initUsb();
        mLayeredScreen = new int[mBoardWidth * mBoardHeight * 3];
    }

    public int getFrameRate() {
        return 12;
    }

    public void start() {

        // attach default cmdMessenger callback
        BurnerBoardPanel.BoardCallbackDefault defaultCallback =
                new BurnerBoardPanel.BoardCallbackDefault();
        mListener.attach(defaultCallback);

        // attach Test cmdMessenger callback
        BurnerBoardPanel.BoardCallbackTest testCallback =
                new BurnerBoardPanel.BoardCallbackTest();
        mListener.attach(5, testCallback);

        // attach Mode cmdMessenger callback
        BurnerBoardPanel.BoardCallbackMode modeCallback =
                new BurnerBoardPanel.BoardCallbackMode();
        mListener.attach(4, modeCallback);

        // attach Board ID cmdMessenger callback
        BurnerBoardPanel.BoardCallbackBoardID boardIDCallback =
                new BurnerBoardPanel.BoardCallbackBoardID();
        mListener.attach(11, boardIDCallback);

        // attach echoRow cmdMessenger callback
        BurnerBoardPanel.BoardCallbackEchoRow echoCallback =
                new BurnerBoardPanel.BoardCallbackEchoRow();
        mListener.attach(17, echoCallback);

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardPanel.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardPanel.BoardCallbackGetBatteryLevel();
        mListener.attach(8, getBatteryLevelCallback);

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

    static int[][][] pixel2OffsetTable = new int[255][255][3];

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
        } else {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = mBoardHeight - 2; y >= 0; y--) {
                    mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                }
            }
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
        }
    }

    public int[] getPixelBuffer() {
        return mBoardScreen;
    }

    private int flushCnt = 0;
    long lastFlushTime = java.lang.System.currentTimeMillis();

    public void flush() {

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            l("Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }



        // Here we calculate the total power percentage of the whole board
        // We want to limit the board to no more than 50% of pixel output total
        // This is because the board is setup to flip the breaker at 200 watts
        // Output is percentage multiplier for the LEDs
        // At full brightness we limit to 30% of their output
        // Power is on-linear to pixel brightness: 37% = 50% power.
        // powerPercent = 100: 15% multiplier
        // powerPercent <= 15: 100% multiplier
        int totalBrightnessSum = 0;
        int powerLimitMultiplierPercent = 100;
        for (int pixel = 0; pixel < mBoardScreen.length; pixel++) {
            // R
            if (pixel % 3 == 0) {
                totalBrightnessSum += mBoardScreen[pixel];
            } else if (pixel % 3 == 1) {
                totalBrightnessSum += mBoardScreen[pixel];
            } else {
                totalBrightnessSum += mBoardScreen[pixel] / 2;
            }
        }

        final int powerPercent = totalBrightnessSum / mBoardScreen.length * 100 / 255;
        powerLimitMultiplierPercent = 100 - java.lang.Math.max(powerPercent - 15, 0);

        // Render text on board
        int [] mOutputScreen;
        if (renderText(mLayeredScreen, mBoardScreen) != null) {
            mOutputScreen = mLayeredScreen;
        } else {
            mOutputScreen = mBoardScreen;
        }

        int[] rowPixels = new int[mBoardWidth * 3];
        for (int y = 0; y < mBoardHeight; y++) {
            //for (int y = 30; y < 31; y++) {
            for (int x = 0; x < mBoardWidth; x++) {
                if (y < mBoardHeight) {
                    rowPixels[(mBoardWidth - 1 - x) * 3 + 0] = mOutputScreen[pixel2Offset(x, y, PIXEL_RED)];
                    rowPixels[(mBoardWidth - 1 - x) * 3 + 1] = mOutputScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                    rowPixels[(mBoardWidth - 1 - x) * 3 + 2] = mOutputScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                }
            }
            //setRowVisual(y, rowPixels);
            setRow(y, rowPixels);
        }

        update();
        flush2Board();

    }

    private boolean setRow(int row, int[] pixels) {

        int [] dimPixels = new int [pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            dimPixels[pixel] =
                    (mDimmerLevel * pixels[pixel]) / 255;
        }

        // Do color correction on burner board display pixels
        byte [] newPixels = new byte[mBoardWidth * 3];
        for (int pixel = 0; pixel < mBoardWidth * 3; pixel = pixel + 3) {
            newPixels[pixel] = (byte)pixelColorCorrectionRed(dimPixels[pixel]);
            newPixels[pixel + 1] = (byte)pixelColorCorrectionGreen(dimPixels[pixel + 1]);
            newPixels[pixel + 2] = (byte)pixelColorCorrectionBlue(dimPixels[pixel + 2]);
        }

        //System.out.println("flush row:" + y + "," + bytesToHex(newPixels));

        //l("sendCommand: 10,n,...");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(10);
                mListener.sendCmdArg(row);
                mListener.sendCmdEscArg(newPixels);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }

    boolean haveUpdated = false;
    public void setMsg(String msg) {
        //l("sendCommand: 10,n,...");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(13);
                mListener.sendCmdArg(msg);
                mListener.sendCmdEnd();
            }
            if (!haveUpdated) {
                haveUpdated = true;
                update();
            }
            flush2Board();
        }
    }


    //    cmdMessenger.attach(BBUpdate, OnUpdate);              // 6
    public boolean update() {

        sendVisual(8);

        //l("sendCommand: 5");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmd(6);
                mListener.sendCmdEnd();
                return true;
            } else {
                // Emulate board's 30ms refresh time
                try {
                    Thread.sleep(5);
                } catch (Throwable e) {
                }
            }
        }

        return false;
    }


    //    cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 7
    public void showBattery() {

        sendVisual(9);
        l("sendCommand: 7");
        if (mListener != null) {
            mListener.sendCmd(7);
            mListener.sendCmdEnd();
            flush2Board();
            return;
        }
        return;
    }

}