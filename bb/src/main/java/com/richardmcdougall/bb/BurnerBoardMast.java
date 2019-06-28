package com.richardmcdougall.bb;

import android.content.Context;
import android.util.Log;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Created by rmc on 7/25/17.
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


public class BurnerBoardMast extends BurnerBoard {
    //public int[] mBoardScreen;
    private int mDimmerLevel = 255;
    private static final String TAG = "BB.BurnerBoardMast";
    public int mBatteryLevel;
    public int [] mBatteryStats = new int[16];
    //public String boardId = Build.MODEL;


    public BurnerBoardMast(BBService service, Context context) {
        super(service, context);
        mBoardWidth = 24;
        mBoardHeight = 159;
        mMultipler4Speed = 3;
        boardId = BBService.getBoardId();
        boardType = "Burner Board Mast";
        l("Burner Board Mast initing...");
        mBoardScreen = new int[mBoardWidth * mBoardHeight * 3];
        mBBService = service;
        mContext = context;
        initPixelOffset();
        initpixelMap2Board();
        initUsb();
        mTextBuffer = IntBuffer.allocate(mBoardWidth * mBoardHeight * 4);
    }

    public int getFrameRate() {
        return 18;
    }

    public void start() {

        // attach default cmdMessenger callback
        BurnerBoardMast.BoardCallbackDefault defaultCallback =
                new BurnerBoardMast.BoardCallbackDefault();
        mListener.attach(defaultCallback);

        // attach Test cmdMessenger callback
        BurnerBoardMast.BoardCallbackTest testCallback =
                new BurnerBoardMast.BoardCallbackTest();
        mListener.attach(5, testCallback);

        // attach Mode cmdMessenger callback
        BurnerBoardMast.BoardCallbackMode modeCallback =
                new BurnerBoardMast.BoardCallbackMode();
        mListener.attach(4, modeCallback);

        // attach Board ID cmdMessenger callback
        BurnerBoardMast.BoardCallbackBoardID boardIDCallback =
                new BurnerBoardMast.BoardCallbackBoardID();
        mListener.attach(11, boardIDCallback);

        // attach echoRow cmdMessenger callback
        BurnerBoardMast.BoardCallbackEchoRow echoCallback =
                new BurnerBoardMast.BoardCallbackEchoRow();
        mListener.attach(17, echoCallback);

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardMast.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardMast.BoardCallbackGetBatteryLevel();
        mListener.attach(8, getBatteryLevelCallback);

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
            String boardId = mListener.readStringArg();
            boardCallback.BoardId(boardId);
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


    public void resetParams() {
        mDimmerLevel = 255;
    }

    // Convert from xy to buffer memory
    int pixel2OffsetCalc(int x, int y, int rgb) {
        return (y * mBoardWidth + x) * 3 + rgb;
    }


    static int pixel2Offset(int x, int y, int rgb) {
        return pixel2OffsetTable[x][y][rgb];
    }

    static int PIXEL_RED = 0;
    static int PIXEL_GREEN = 1;
    static int PIXEL_BLUE = 2;

    static int [][][] pixel2OffsetTable = new int[255][255][3];
    private void initPixelOffset() {
        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {
                for (int rgb = 0; rgb < 3; rgb++) {
                    pixel2OffsetTable[x][y][rgb] = pixel2OffsetCalc(x, y, rgb);
                }
            }
        }
    }

    public void setPixel(int x, int y, int r, int g, int b) {
        //System.out.println("Setting pixel " + x + "," + y + " : " + pixel2Offset(x, y, PIXEL_RED) + " to " + r +  "," + g + "," + b);
        //l("setPixel(" + x + "," + y + "," + r + "," + g + "," + b + ")");
        //Sstem.out.println("setpixel r = " + r);
        if (x < 0 || x >= mBoardWidth || y < 0 || y >= mBoardHeight) {
            l("setPixel out of range: " + x + "," + y);
            return;
        }
        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
        mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
        mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
    }

    public void setPixel(int pixel, int r, int g, int b) {

        if (pixel < 0 || pixel >= (mBoardWidth * mBoardHeight)) {
            l("setPixel out of range: " + pixel);
            return;
        }
        mBoardScreen[pixel * 3] = r;
        mBoardScreen[pixel * 3 + 1] = g;
        mBoardScreen[pixel * 3 + 2] = b;
    }

    public void fillScreenMask(int r, int g, int b) {
        //System.out.println("Fillscreen " + r + "," + g + "," + b);
        int x;
        int y;
        for (x = 0; x < mBoardWidth; x++) {
            for (y = 0; y < mBoardHeight; y++) {
                if (getPixel(x, y) > 0) {
                    setPixel(x, y, r, g, b);
                }
            }
        }
    }

    public int getPixel(int x, int y) {
        int r = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
        int g = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
        int b = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
        return BurnerBoard.getRGB(r, g, b);
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
            powerLimitMultiplierPercent = 100 - java.lang.Math.max(powerPercent - 12, 0);

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
                setRowVisual(y, rowPixels);
            }

            // Walk through each strip and fill from the graphics buffer
            for (int s = 0; s < kStrips; s++) {
                int[] stripPixels = new int[mBoardHeight * 3 * 3];
                // Walk through all the pixels in the strip
                for (int offset = 0; offset < mBoardHeight * 3 * 3; ) {
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
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

        //newPixels[30]=(byte)128;
        //newPixels[31]=(byte)128;
        //newPixels[32]=(byte)128;
        //newPixels[3]=2;
        //newPixels[4]=40;
        //newPixels[5]=2;
        //newPixels[6]=2;
        //newPixels[7]=2;
        //newPixels[8]=40;
        //newPixels[9]=2;
        //newPixels[10]=2;
        //newPixels[11]=40;

        //System.out.println("flushPixels row:" + y + "," + bytesToHex(newPixels));

        //l("sendCommand: 14,n,...");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(10);
                mListener.sendCmdArg(strip);
                mListener.sendCmdEscArg(newPixels);
                mListener.sendCmdEnd();
            }
        }
    }


    public static class TranslationMap {
        int y;
        int startX;
        int endX;
        int stripDirection;
        int stripNumber;
        int stripOffset;

        private TranslationMap(
                int y,
                int startX,
                int endX,
                int stripDirection,
                int stripNumber,
                int stripOffset) {
            this.y = y;
            this.startX = startX;
            this.endX = endX;
            this.stripDirection = stripDirection;
            this.stripNumber = stripNumber;
            this.stripOffset = stripOffset;
        }
    }

    static int pixelMap2Board(int s, int offset)
    {
        return pixelMap2BoardTable[s][offset];
    }

    private void pixelRemap(int x, int y, int stripNo, int stripOffset) {
        pixelMap2BoardTable[stripNo][stripOffset] =
                pixel2Offset(mBoardWidth - 1 - x, mBoardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[stripNo][stripOffset + 1] =
                pixel2Offset(mBoardWidth - 1 - x, mBoardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[stripNo][stripOffset + 2] =
                pixel2Offset(mBoardWidth - 1 - x, mBoardHeight - 1 - y, PIXEL_BLUE);
    }


    // Two primary mapping functions
    static int kStrips = 8;
    static int [][] pixelMap2BoardTable = new int[8][4096];
    private TranslationMap[] boardMap;

    private void initpixelMap2Board() {

        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {

                final int subStrip = x % 3;
                final int stripNo = x / 3;
                final boolean stripUp = subStrip % 2 == 0;
                int stripOffset;

                if (stripUp) {
                    stripOffset = subStrip * mBoardHeight + y;
                } else {
                    stripOffset = subStrip * mBoardHeight + (mBoardHeight - 1 - y);
                }
                pixelRemap(x, y, stripNo, stripOffset * 3);
            }

        }

    }
}

