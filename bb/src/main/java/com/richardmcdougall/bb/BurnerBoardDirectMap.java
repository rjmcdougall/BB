
package com.richardmcdougall.bb;
import android.util.Log;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Created by rmc on 5/28/18.
 */


/*

   bottom
         46,118
 +------+
 |      | strip 1
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
 |      | strip 8
 +------+
 0,0 X
 top

 */


public class BurnerBoardDirectMap extends BurnerBoard {

    private static final String TAG = "BB.BurnerBoardDirectMap";

    /* JosPacks have more of a power constraint, so we don't want to set it to full brightness. Empirically tested
        with with a rapidly refreshing pattern (BlueGold):
        100 -> 1.90a draw
        50  -> 0.50a draw
        25  -> 0.35a draw
    */
    private static final int kVisualizationDirectMapPowerMultiplier = BoardState.kIsRPI ? 25 : 100; // should be ok for nano
    private static final int mPowerMultiplier = kVisualizationDirectMapPowerMultiplier;

    /* DIRECT MAP SETTINGS */
    // JosPacks have 1x166 strands of LEDs. Currently RPI == JosPack
    private static final int kVisualizationDirectMapDefaultWidth = 8;
    public static final int kVisualizationDirectMapWidth = BoardState.kIsRPI ? 1 : kVisualizationDirectMapDefaultWidth;
    private static final int kVisualizationDirectMapDefaultHeight = 256;
    public static final int kVisualizationDirectMapHeight = BoardState.kIsRPI ? 166 : kVisualizationDirectMapDefaultHeight;

    public BurnerBoardDirectMap(BBService service, int width, int height) {
        super(service);
        mBoardWidth = width;
        mBoardHeight = height;
        mMultipler4Speed = 3;
        boardId = service.boardState.BOARD_ID;

        mBoardScreen = new int[mBoardWidth * mBoardHeight * 3];
        this.service = service;

        boardType = "Burner Board DirectMap";
        l(boardType + " initializing at: " + mBoardWidth + " x " + mBoardHeight);

        mTextBuffer = IntBuffer.allocate(mBoardWidth * mBoardHeight * 4);
        initPixelOffset();
        initUsb();
    }

    public void start() {

        // attach default cmdMessenger callback
        BurnerBoardDirectMap.BoardCallbackDefault defaultCallback =
                new BurnerBoardDirectMap.BoardCallbackDefault();
        mListener.attach(defaultCallback);

        // attach Test cmdMessenger callback
        BurnerBoardDirectMap.BoardCallbackTest testCallback =
                new BurnerBoardDirectMap.BoardCallbackTest();
        mListener.attach(5, testCallback);

        // attach Mode cmdMessenger callback
        BurnerBoardDirectMap.BoardCallbackMode modeCallback =
                new BurnerBoardDirectMap.BoardCallbackMode();
        mListener.attach(4, modeCallback);

        // attach Board ID cmdMessenger callback
        BurnerBoardDirectMap.BoardCallbackBoardID boardIDCallback =
                new BurnerBoardDirectMap.BoardCallbackBoardID();
        mListener.attach(11, boardIDCallback);

        // attach echoRow cmdMessenger callback
        BurnerBoardDirectMap.BoardCallbackEchoRow echoCallback =
                new BurnerBoardDirectMap.BoardCallbackEchoRow();
        mListener.attach(17, echoCallback);

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardDirectMap.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardDirectMap.BoardCallbackGetBatteryLevel();
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


    public int [] getPixelBuffer() {
        return mBoardScreen;
    }

    // TODO: gamma correction
    // encoded = ((original / 255) ^ (1 / gamma)) * 255
    // original = ((encoded / 255) ^ gamma) * 255

    // TODO: make faster by using ints
    private int pixelColorCorrectionRed(int red) {
        return gammaCorrect(red) ;
    }

    private int pixelColorCorrectionGreen(int green) {
        return gammaCorrect(green);
    }

    private int pixelColorCorrectionBlue(int blue) {
        return gammaCorrect(blue);
    }

    public void clearPixels() {
        Arrays.fill(mBoardScreen, 0);
    }

    public void dimPixels(int level) {
        mDimmerLevel = level;
    }

    private int flushCnt = 0;
    long lastFlushTime = java.lang.System.currentTimeMillis();
    static int kStrips = 8;

    public void flush() {

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            l("Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }

        // Suppress updating when displaying a text message
        if (isTextDisplaying > 0) {
            isTextDisplaying--;
        } else {

            // Here we calculate the total power percentage of the whole board
            // We want to limit the board to no more than 50% of pixel output total
            // This is because the board is setup to flip the breaker at 200 watts
            // Output is percentage multiplier for the LEDs
            // At full brightness we limit to 30% of their output
            // Power is on-linear to pixel brightness: 37% = 50% power.
            // powerPercent = 100: 15% multiplier
            // powerPercent <= 15: 100% multiplier
            int powerLimitMultiplierPercent = mPowerMultiplier;

            /* None of this code was actively used; see the last line that simply re-sets this value
               to 100. So, let's comment out this code and just use the constant from bbutil -jib

            int totalBrightnessSum = 0;
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
            //powerLimitMultiplierPercent = 100;// - java.lang.Math.max(powerPercent - 12, 0);
            */

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
                //setRowVisual(y, rowPixels);
            }

            // Walk through each strip and fill from the graphics buffer
            for (int s = 0; s < kStrips; s++) {
                int[] stripPixels = new int[mBoardHeight * 3];
                // Walk through all the pixels in the strip
                for (int y = 0; y < mBoardHeight; y++) {
                    stripPixels[y * 3] = mBoardScreen[pixel2Offset(s, y, PIXEL_RED)];
                    stripPixels[y * 3 + 1] = mBoardScreen[pixel2Offset(s, y, PIXEL_GREEN)];
                    stripPixels[y * 3 + 2] = mBoardScreen[pixel2Offset(s, y, PIXEL_BLUE)];
                }
                setStrip(s, stripPixels, powerLimitMultiplierPercent);
                // Send to board
                flush2Board();
            }

            // Render on board
            update();
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
            }
            else {
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

    public void setMsg(String msg) {
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

    //    cmdMessenger.attach(BBClearScreen, OnClearScreen);    // 4
    public boolean clearScreen() {

        sendVisual(5);
        l("sendCommand: 4");
        if (mListener != null) {
            mListener.sendCmd(4);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }

    private void setRowVisual(int row, int[] pixels) {

        int[] dimPixels = new int[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            dimPixels[pixel] =
                    (mDimmerLevel * pixels[pixel]) / 255;
        }

        // Send pixel row to in-app visual display
        sendVisual(14, row, dimPixels);
    }

    // Send a strip of pixels to the board
    private void setStrip(int strip, int[] pixels, int powerLimitMultiplierPercent) {

        int[] dimPixels = new int[pixels.length];

        for (int pixel = 0; pixel < pixels.length; pixel++) {
            dimPixels[pixel] =
                    (mDimmerLevel * pixels[pixel]) / 256 * powerLimitMultiplierPercent / 100;
        }

        // Do color correction on burner board display pixels
        byte [] newPixels = new byte[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel = pixel + 3) {
            newPixels[pixel] = (byte)pixelColorCorrectionRed(dimPixels[pixel]);
            newPixels[pixel + 1] = (byte)pixelColorCorrectionGreen(dimPixels[pixel + 1]);
            newPixels[pixel + 2] = (byte)pixelColorCorrectionBlue(dimPixels[pixel + 2]);
        }

        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(10);
                mListener.sendCmdArg(strip);
                mListener.sendCmdEscArg(newPixels);
                mListener.sendCmdEnd();
            }
        }
    }


}

