package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.Visualizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Random;

/**
 * Created by rmc on 3/8/17.
 */

public class BoardVisualization {

    private static final String TAG = "BB.BoardVisualization";
    private int testState = 0;
    private Random mRandom = new Random();
    private byte[] testRow1 = "012345678901234567890123456789012345".getBytes();
    private byte[] testRow2 = "567890123456789012345678901234567890".getBytes();
    public byte[] mBoardFFT;
    private int mBoardWidth = 10;
    private int mBoardHeight = 70;
    private int mBoardSideLights = 79;
    private int [][]  mFireColors = new int[256][3];
    private int [] mBoardScreen;
    private int [] mFireScreen;

    int mBoardMode;
    Context mContext;

    BurnerBoard mBurnerBoard = null;

    BoardVisualization(Context context, BurnerBoard board) {
        mBurnerBoard = board;
        mContext = context;
        // Start Board Display
        Thread boardDisplay = new Thread(new Runnable() {
            public void run() {
                boardDisplayThread();
            }
        });
        boardDisplay.start();
        mBoardScreen = board.getPixelBuffer();
        initFire();
    }

    private int boardDisplayCnt = 0;
    private Visualizer mVisualizer;
    private int mAudioSessionId;

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    public void setMode(int mode) {

        mBoardMode = mode;
        mBurnerBoard.resetParams();
    }

    public void attachAudio(int audioSessionId) {
        int vSize;

        l("session=" + audioSessionId);
        mAudioSessionId = audioSessionId;
        // Create the Visualizer object and attach it to our media player.
        if (mVisualizer != null) {
            mVisualizer.release();
        }
        try {
            mVisualizer = new Visualizer(audioSessionId);
        } catch (Exception e) {
            l("Error enabling visualizer: " + e.getMessage());
            //System.out.println("Error enabling visualizer:" + e.getMessage());
            return;
        }
        vSize = Visualizer.getCaptureSizeRange()[1];
        mVisualizer.setEnabled(false);
        mBoardFFT = new byte[vSize];
        mVisualizer.setCaptureSize(vSize);
        mVisualizer.setEnabled(true);
        l("Enabled visualizer with " + vSize + " bytes");

    }


    int sleepTime = 30;

    // Main thread to drive the Board's display & get status (mode, voltage,...)
    void boardDisplayThread() {

        l("Starting board display thread...");


        while (true) {
            switch (mBoardMode) {

                case 1:
                    sleepTime = 5;
                    modeMatrix(kMatrixBurnerColor);
                    break;

                case 2:
                    sleepTime = 5;
                    modeMatrix(kMatrixFire);
                    break;

                case 3:
                    sleepTime = 5;
                    modeMatrix(kMatrixLunarian);
                    break;

                case 4:
                    sleepTime = 5;
                    modeAudioCenter();
                    break;

                case 5:
                    sleepTime = 5;
                    modeFire();
                    break;


                case 6:
                    sleepTime = 5;
                    modeTheMan();
                    break;

                case 7:
                    sleepTime = 5;
                    modeAudioBarV();
                    break;

                case 8:
                    sleepTime = 5;
                    modeMatrix(kMatrixGoogle);
                    break;

                case 9:
                    sleepTime = 5;
                    modeMatrix(kMatrixEsperanto);
                    break;

                case 10:
                    sleepTime = 5;
                    modeMatrix(kMatrixIrukandji);
                    break;

                case 11:
                    sleepTime = 5;
                    modeMatrix(kMatrixFireFull);
                    break;

                case 12:
                    sleepTime = 5;
                    modeAudioFront();
                    break;

                case 13:
                    sleepTime = 5;
                    modeAudioTile();
                    break;

                default:
                    break;
            }

            try {
                Thread.sleep(sleepTime);
            } catch (Throwable e) {
            }

            boardDisplayCnt++;
            if (boardDisplayCnt > 1000) {
                //updateStatus();
            }
        }

    }

    /*
    void modeTestRow() {
        int [] testRow = new int[36];

        switch (testColorState) {
            case 0:
                for (int i = 0; i < 12; i += 3) {
                    testRow[i] =  255;
                    testRow[i+1] =  0;
                    testRow[i+2] =  0;
                }
                mBurnerBoard.setRow(0, testRow);
                mBurnerBoard.flush();
                testColorState++;
                break;
            case 1:
                for (int i = 0; i < 12; i += 3) {
                    testRow[i] =  0;
                    testRow[i+1] =  255;
                    testRow[i+2] =  0;
                }
                mBurnerBoard.setRow(0, testRow);
                mBurnerBoard.flush();
                testColorState++;
                break;
            case 2:
                for (int i = 0; i < 12; i += 3) {
                    testRow[i] =  0;
                    testRow[i+1] =  0;
                    testRow[i+2] =  255;
                }
                mBurnerBoard.setRow(0, testRow);
                mBurnerBoard.flush();
                testColorState++;
                break;
            case 3:
                for (int i = 0; i < 12; i += 3) {
                    testRow[i] =  100;
                    testRow[i+1] =  100;
                    testRow[i+2] =  100;
                }
                mBurnerBoard.setRow(0, testRow);
                mBurnerBoard.flush();
                testColorState = 0;
                break;
            default:
                testColorState = 0;
                break;
        }

    }
    */

    int testColor = 0;

    void modeTest() {

        System.out.println("Firecolor " + mFireColors[testColor][0] + "," + mFireColors[testColor][1] + "," + mFireColors[testColor][2]);

        mBurnerBoard.fillScreen(
                mFireColors[testColor][0],
                mFireColors[testColor][1],
                mFireColors[testColor][2]);
        mBurnerBoard.flushPixels();
        testColor++;
        if (testColor > 255) {
            testColor = 0;
        }

    }

    int testColorState = 0;
    private void modeTestColors() {
        switch (testColorState) {
            case 0:
                mBurnerBoard.fillScreen(255, 0, 0);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 1:
                mBurnerBoard.fillScreen(0, 255, 0);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 2:
                mBurnerBoard.fillScreen(0, 0, 255);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 3:
                mBurnerBoard.fillScreen(10, 0, 0);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 4:
                mBurnerBoard.fillScreen(0, 10, 0);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 5:
                mBurnerBoard.fillScreen(0, 0, 10);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 6:
                mBurnerBoard.fillScreen(0, 0, 10);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 7:
                mBurnerBoard.fillScreen(255, 128, 0);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 8:
                mBurnerBoard.fillScreen(204, 0, 204);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flushPixels();
                testColorState = 0;
                break;

            default:
                break;
        }

    }




    int wheel_color = 0;
    // Input a value 0 to 255 to get a color value.
    // The colours are a transition r - g -b - back to r
    int wheel(int WheelPos)
    {
        if (WheelPos < 85) {
            return BurnerBoard.getRGB(WheelPos * 3, 255 - WheelPos * 3, 0);
        } else if (WheelPos < 170) {
            WheelPos -= 85;
            return BurnerBoard.getRGB(255 - WheelPos * 3, 0, WheelPos * 3);
        } else {
            WheelPos -= 170;
            return BurnerBoard.getRGB(0, WheelPos * 3, 255 - WheelPos * 3);
        }
    }

    void wheelInc(int amount) {
        wheel_color = wheel_color + amount;
        if (wheel_color > 255)
            wheel_color = 0;
    }



    public static final int kMatrixBurnerColor = 1;
    public static final int kMatrixFire = 2;
    public static final int kMatrixLunarian = 3;
    public static final int kMatrixGoogle = 4;
    public static final int kMatrixEsperanto = 5;
    public static final int kMatrixIrukandji = 6;
    public static final int kMatrixFireFull = 7;

    private static final int[] googleColorsOrig = {
            BurnerBoard.getRGB(60, 186, 84),
            BurnerBoard.getRGB(244, 194, 13),
            BurnerBoard.getRGB(219, 50, 54),
            BurnerBoard.getRGB(72, 133, 237),
            BurnerBoard.getRGB(255, 255, 255)
    };

    private static final int[] googleColors = {
            BurnerBoard.getRGB(40, 200, 60),
            BurnerBoard.getRGB(200, 194, 13),
            BurnerBoard.getRGB(255, 10, 10),
            BurnerBoard.getRGB(20, 50, 237),
            BurnerBoard.getRGB(255, 255, 255)
    };

    private int fireColor = 40;
    private int googleColor = 0;

    void modeMatrix(int mode) {

        // Row plus two pixels for side lights
        int[] pixels = new int[mBoardWidth * 3 + 6];

        int color;
        int x;
        int y;
        int sideLight;

        y = 69;
        sideLight = mBoardSideLights  - 1;


        for (x = 0; x < 10; x++) {
            //Chance of 1/3rd
            switch (mode) {
                case kMatrixEsperanto:
                case kMatrixBurnerColor:
                    color = mRandom.nextInt(2) == 0 ?
                            BurnerBoard.getRGB(0, 0, 0): wheel(wheel_color);
                    mBurnerBoard.setPixel(x, y, color);
                    break;
                case kMatrixLunarian:
                    color = mRandom.nextInt(2) == 0 ?
                            BurnerBoard.getRGB(0, 0, 0): BurnerBoard.getRGB(255, 255, 255);
                    mBurnerBoard.setPixel(x, y, color);
                    break;
                case kMatrixFire:
                    color = mRandom.nextInt(2) == 0 ?
                            BurnerBoard.getRGB(0, 0, 0):
                            BurnerBoard.getRGB(
                                    mFireColors[fireColor][0],
                                    mFireColors[fireColor][1],
                                    mFireColors[fireColor][2]);
                    mBurnerBoard.setPixel(x, y, color);
                    break;
                case kMatrixGoogle:
                    color = mRandom.nextInt(2) == 0 ?
                            BurnerBoard.getRGB(0, 0, 0): googleColors[googleColor / 8];
                    mBurnerBoard.setPixel(x, y, color);
                    break;
                case kMatrixIrukandji:
                    color = wheel(wheel_color);
                    if (x > 0 || x < 10) {
                        mBurnerBoard.setPixel(x, y, BurnerBoard.getRGB(0, 0, 0));
                    }
                    break;
                case kMatrixFireFull:
                    color = BurnerBoard.getRGB(
                                    mFireColors[fireColor][0],
                                    mFireColors[fireColor][1],
                                    mFireColors[fireColor][2]);
                    mBurnerBoard.setPixel(x, y, color);
                    break;


                default:
                    color = 0;
            }
            // Ripple down the side lights with the same color as the edges
            if (x == 0) {
                mBurnerBoard.setPixelOtherlight(sideLight, BurnerBoard.kLeftSightlight, color);
            }
            if (x == 9) {
                mBurnerBoard.setPixelOtherlight(sideLight, BurnerBoard.kRightSidelight, color);
            }
            fireColor += 1;
            if (fireColor > 180) {
                fireColor = 40;
            }

            wheelInc(1);
        }
        googleColor++;
        if (googleColor >= googleColors.length * 8) {
            googleColor = 0;
        }

        mBurnerBoard.scrollPixels(true);
        switch (mode) {
            case kMatrixEsperanto:
                int level = java.lang.Math.max(0, (getLevel() * 3) - 80);
                mBurnerBoard.dimPixels(level);
                break;
            default:
                break;
        }
        mBurnerBoard.flushPixels();

        return;

    }


    /*
    int testValue = 0;

    void modeAudioMatrix() {

        int[] pixels = new int[36];

        if (mBoardFFT == null)
            return;

        if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {

            int pixel = 0;
            byte rfk;
            byte ifk;
            int dbValue = 0;
            // There are 1024 values - 512 x real, imaginary
            for (int i = 0; i < 512; i++) {
                rfk = mBoardFFT[i];
                ifk = mBoardFFT[i + 1];
                float magnitude = (rfk * rfk + ifk * ifk);
                dbValue += java.lang.Math.max(0, 30 * Math.log10(magnitude) - 1);

                // Aggregate each 8 values to give 64 bars
                if ((i & 7) == 0) {
                    dbValue -= 50;
                    int value = java.lang.Math.max(dbValue, 0);
                    value = java.lang.Math.min(value, 255);
                    dbValue = 0;

                    // Take the 4th through 16th values
                    if ((i / 8) >= 12 && (i / 8) < 48) {
                        ;
                        pixels[pixel] =  java.lang.Math.max(0, value);
//                            pixels[pixel] = testValue;
                        pixel++;
                        //System.out.println("modeAudioMatrix Value[" + pixel + "] = " + value);
                    }
                }
            }
            mBurnerBoard.setRow(69, pixels);
            mBurnerBoard.update();
            mBurnerBoard.scroll(true);
            mBurnerBoard.flush();
        } else {
            l("visualizer failued");
        }

        testValue++;
        if (testValue > 255)
            testValue = 0;
        return;

    }


    private int discoState = 0;
    private Random discoRandom = new Random();

    void modeAudioBeat() {

        if (mBoardFFT == null)
            return;
        int r = 0;
        int b = 0;
        int g = 0;

        if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {


            byte rfk;
            byte ifk;
            int dbValue = 0;
            for (int i = 0; i < 512; i++) {
                rfk = mBoardFFT[i];
                ifk = mBoardFFT[i + 1];
                float magnitude = (rfk * rfk + ifk * ifk);
                dbValue += java.lang.Math.max(0, 128 * Math.log10(magnitude));
                if (dbValue < 0)
                    dbValue = 0;

                if ((i & 63) == 0) {
                    int value = java.lang.Math.min(dbValue / 64, 255);
                    dbValue = 0;
                    //System.out.println("Visualizer Value[" + i / 64 + "] = " + value);

                    if (i == 64)
                        r = value;
                    else if (i == 128)
                        g = value;
                    else if (i == 256)
                        b = value;
                }
            }

            //mBurnerBoard.fillScreen( r,  g,  b);
            //mBurnerBoard.fillScreen( 255,  140,  0);
            mBurnerBoard.fillScreen(wheel(85));
            mBurnerBoard.setOtherlightsAutomatically();
            mBurnerBoard.flushPixels();
            return;
        }
    }
    */


    // Pick classic VU meter colors based on volume
    int vuColor(int amount) {
        if (amount < 11)
            return BurnerBoard.getRGB(0, 255, 0);
        if (amount < 21)
            return BurnerBoard.getRGB(255, 165, 0);
        return BurnerBoard.getRGB(255, 0, 0);
    }

    // Get levels from Android visualizer engine
    // 16 int buckets of frequency levels from low to high
    // Range is 0-255 per bucket
    int [] getLevels() {
        if (mBoardFFT == null)
            return null;
        if (mVisualizer.getFft(mBoardFFT) != mVisualizer.SUCCESS)
            return null;

        int [] dbLevels = new int[16];
        byte rfk;
        byte ifk;
        int dbValue = 0;
        // There are 1024 values - 512 x real, imaginary
        for (int i = 0; i < 512; i+= 8) {
            if ((i % 32) == 0) {
                dbValue = 0;
            }
            rfk = mBoardFFT[i];
            ifk = mBoardFFT[i + 1];
            float magnitude = (rfk * rfk + ifk * ifk);
            dbValue += java.lang.Math.max(0, 50 * (Math.log10(magnitude) - 1));
            if (dbValue < 0)
                dbValue = 0;
            dbLevels[i / 32] += dbValue;
            dbLevels[i / 32] = java.lang.Math.min(dbLevels[i / 32], 255);

        }
        return dbLevels;
    }

    private static final int kNumLevels = 8;
    private int [] oldLevels = new int[kNumLevels];

    private int getLevel() {

        int level = 0;
        int delta;
        int[] dbLevels = getLevels();
        if (dbLevels == null)
            return (0);
        for (int i = 0; i < kNumLevels; i++) {
            if ((delta = dbLevels[i] - oldLevels[i]) > 0) {
                level += delta;
            }
        }
        System.arraycopy(dbLevels, 0, oldLevels, 0, kNumLevels);
        level = (int)(25.01 * java.lang.Math.log((float)(level + 1)));
        //System.out.println("level: " + level);
        return level;
    }


    void modeAudioBarV() {

        int [] dbLevels = getLevels();
        if (dbLevels == null)
            return;
        mBurnerBoard.fadePixels(50);
        // Iterate through frequency bins: dbLevels[0] is lowest, [15] is highest
        int row = 0;
        for (int value = 3; value < 15; value += 2) {
            //System.out.println("level " + dbLevels[value]);
            int level = java.lang.Math.min(dbLevels[value-1] / 6, 35);
            //l("level " + value + ":" + level + ":" + dbLevels[value]);
            for (int y = 0; y < level; y++) {
                if (value == 3  ) {
                    //mBurnerBoard.setSideLight(0, 39 + y, vuColor(y));
                    //mBurnerBoard.setSideLight(0, 38 - y, vuColor(y));
                    //mBurnerBoard.setSideLight(1, 39 + y, vuColor(y));
                    //mBurnerBoard.setSideLight(1, 38 - y, vuColor(y));
                } else {
                    mBurnerBoard.setPixel(((value / 2) - 2), 35 + y, vuColor(y));
                    mBurnerBoard.setPixel(((value / 2) - 2), 34 - y, vuColor(y));
                    mBurnerBoard.setPixel(9 - ((value / 2) - 2), 35 + y, vuColor(y));
                    mBurnerBoard.setPixel(9 - ((value / 2) - 2), 34 - y, vuColor(y));
                }
            }
        }
        mBurnerBoard.setOtherlightsAutomatically();
        mBurnerBoard.flushPixels();
        return;
    }

    void modeAudioFuzzer() {

        int [] dbLevels = getLevels();
        if (dbLevels == null)
            return;
        mBurnerBoard.fuzzPixels(10);
        // Iterate through frequency bins: dbLevels[0] is lowest, [15] is highest
        int row = 0;
        for (int value = 3; value < 15; value += 2) {
            int level = java.lang.Math.min(dbLevels[value] / 7, 35);
            //l("level " + value + ":" + level + ":" + dbLevels[value]);
            for (int y = 0; y < level; y++) {
                if (value == 3) {
                    //mBurnerBoard.setSideLight(0, 39 + y, vuColor(y));
                    //mBurnerBoard.setSideLight(0, 38 - y, vuColor(y));
                    //mBurnerBoard.setSideLight(1, 39 + y, vuColor(y));
                    //mBurnerBoard.setSideLight(1, 38 - y, vuColor(y));
                } else {
                    mBurnerBoard.setPixel(((value / 2) - 2), 35 + y, BurnerBoard.getRGB(0, 255, 0));
                    mBurnerBoard.setPixel(((value / 2) - 2), 34 - y, BurnerBoard.getRGB(0, 255, 0));
                    mBurnerBoard.setPixel(9 - ((value / 2) - 2), 35 + y, BurnerBoard.getRGB(0, 255, 0));
                    mBurnerBoard.setPixel(9 - ((value / 2) - 2), 34 - y, BurnerBoard.getRGB(0, 255, 0));
                }
            }
        }
        mBurnerBoard.setOtherlightsAutomatically();
        mBurnerBoard.flushPixels();
        return;
    }

    /*
    private void drawRectCenter(int size, int [] color) {
        if (size == 0)
            return;
        int x1 = mBoardWidth / 2 - 1; // 4
        int y1 = mBoardHeight / 2 - 1;  // 34
        int xSizeLim = java.lang.Math.min(size, 5);
        for (int x = x1 - (xSizeLim - 1); x <= x1 + xSizeLim ; x++) { // 1: 4...5
            mBurnerBoard.setPixel(x, y1 - (size * 3 - 1) , color[size]); // 1: 34
            mBurnerBoard.setPixel(x, y1 + (size * 3 - 1) + 1, color[size]); // 1: 35
            mBurnerBoard.setPixel(x, y1 - (size * 3 - 1) - 1, color[size + 1]); // 1: 34
            mBurnerBoard.setPixel(x, y1 + (size * 3 - 1) + 1 + 1, color[size + 1]); // 1: 35
            mBurnerBoard.setPixel(x, y1 - (size * 3 - 1) - 2, color[size + 2]); // 1: 34
            mBurnerBoard.setPixel(x, y1 + (size * 3 - 1) + 1 + 2, color[size + 2]); // 1: 35
        }
        for (int y = y1 - (size * 3 - 1); y <= y1 + size * 3; y++) { // 1: 34..35
            if(size > (mBoardWidth / 2))
                continue;
            mBurnerBoard.setPixel(x1 - (xSizeLim - 1), y, color[size]); //
            mBurnerBoard.setPixel(x1 + (xSizeLim - 1) + 1, y, color[size]);
        }
    }
    */

    private void drawRectCenter(int size, int color) {
        if (size == 0)
            return;
        int x1 = mBoardWidth / 2 - 1; // 4
        int y1 = mBoardHeight / 2 - 1;  // 34
        int xSizeLim = java.lang.Math.min((size + 3) / 4, 5);
        for (int x = x1 - (xSizeLim - 1); x <= x1 + xSizeLim ; x++) { // 1: 4...5
            mBurnerBoard.setPixel(x, y1 - (size - 1) , color); // 1: 34
            mBurnerBoard.setPixel(x, y1 + (size - 1) + 1, color); // 1: 35
        }
        for (int y = y1 - (size - 1); y <= y1 + size; y++) { // 1: 34..35
            if((size + 3) / 4 > (mBoardWidth / 2))
                continue;
            mBurnerBoard.setPixel(x1 - (xSizeLim - 1), y, color); //
            mBurnerBoard.setPixel(x1 + (xSizeLim - 1) + 1, y, color);
        }
    }


    void modeAudioCenter() {

        int level;
        level = getLevel();
        mBurnerBoard.fadePixels(15);
        if (level > 130) {
            for (int x = 0; x < 35; x++) {
                int c = wheel(wheel_color);
                drawRectCenter(x, c);
                wheelInc(4);
            }
            //System.out.println(x + ":" + level);

            //mBurnerBoard.fadePixels(1);

        }
        mBurnerBoard.setOtherlightsAutomatically();
        mBurnerBoard.flushPixels();
        return;
    }

    private void drawRectFront(int size, int color) {
        if (size == 0)
            return;
        int x1 = mBoardWidth / 2 - 1; // 4
        int y1 = mBoardHeight - 6;
        int xSizeLim = java.lang.Math.min((size + 1), 5);
        for (int x = x1 - (xSizeLim - 1); x <= x1 + xSizeLim ; x++) { // 1: 4...5
            mBurnerBoard.setPixel(x, y1 - (size - 1) , color); // 1: 34
            mBurnerBoard.setPixel(x, y1 + (size - 1) + 1, color); // 1: 35
        }
        for (int y = y1 - (size - 1); y <= y1 + size; y++) { // 1: 34..35
            if((size + 3) > (mBoardWidth / 2))
                continue;
            mBurnerBoard.setPixel(x1 - (xSizeLim - 1), y, color); //
            mBurnerBoard.setPixel(x1 + (xSizeLim - 1) + 1, y, color);

        }
    }

    void modeAudioFront() {

        int level;
        level = getLevel();
        for (int x = 0; x < 6; x++) {
            if (level > 50) {
                int c = wheel(wheel_color);
                drawRectFront(x, c);
                wheelInc(level / 30);
            }
            //System.out.println(x + ":" + level);

        }
        //mBurnerBoard.fadePixels(1);
        mBurnerBoard.setOtherlightsAutomatically();
        mBurnerBoard.flushPixels();
        mBurnerBoard.scrollPixels(true);

        return;
    }

    private void drawRectTile(int tileNo, int color) {
        int x1 = 0;
        int y1 = 0;
        final int tiles = mBoardHeight / mBoardWidth;
        if (tileNo >= tiles * 2)
            return;
        if (tileNo < tiles) {
            x1 = 0;
            y1 = tileNo * mBoardWidth;
        } else {
            x1 = mBoardWidth / 2;
            y1 = (tileNo - tiles) * mBoardWidth;
        }
        int x2 = x1 + mBoardWidth / 2 - 1;
        int y2 = y1 + mBoardWidth - 1;
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (x == x1 || x == x2 || y == y1 || y == y2) {
                    mBurnerBoard.setPixel(x, y, BurnerBoard.colorDim(100, color));
                } else {
                    mBurnerBoard.setPixel(x, y, color);
                }
            }
        }
    }


    void modeAudioTile() {

        //int [] dbLevels = getLevels();
        mBurnerBoard.fadePixels(5);
        //if (dbLevels == null)
        //    return;
            //dbLevels = getLevels();
            //int level = java.lang.Math.max(0, (dbLevels[5] / 5 - 8));
            //if (dbLevels == null)
            //    return;
            if (getLevel() > 140) {
                for (int tile = 0; tile < mBoardHeight / mBoardWidth * 2; tile++) {
                    int c = wheel(wheel_color);
                    //drawRectTile(tile, 255 * mRandom.nextInt(65536));
                    drawRectTile(tile, wheel(mRandom.nextInt(255)));
                    //drawRectTile(tile, wheel((wheel_color)));
                    wheelInc(59);
            }
        }
        //mBurnerBoard.fadePixels(1);
        mBurnerBoard.setOtherlightsAutomatically();
        mBurnerBoard.flushPixels();
        return;
    }

    /*

    void modeAudioBarH() {

        if (mBoardFFT == null)
            return;

        synchronized (mSerialConn) {
            if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {


                if (mListener != null)
                    mListener.sendCmdStart(15);

                byte rfk;
                byte ifk;
                int dbValue = 0;
                for (int i = 0; i < 512; i++) {
                    rfk = mBoardFFT[i];
                    ifk = mBoardFFT[i + 1];
                    float magnitude = (rfk * rfk + ifk * ifk);
                    dbValue += java.lang.Math.max(0, 5 * Math.log10(magnitude));
                    if (dbValue < 0)
                        dbValue = 0;

                    if ((i & 63) == 0) {
                        int value = java.lang.Math.min(dbValue / 64, 255);
                        dbValue = 0;
                        //System.out.println("Visualizer Value[" + i / 64 + "] = " + value);

                        if (mListener != null && (i > 63))
                            mListener.sendCmdArg(value);
                    }
                }
                if (mListener != null) {
                    mListener.sendCmdArg((int) 0);
                    mListener.sendCmdEnd();
                }
            }
        }
        boardFlush();
        return;

    }

    void modeEsperanto() {

        if (mBoardFFT == null)
            return;

        synchronized (mSerialConn) {
            if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {


                if (mListener != null)
                    mListener.sendCmdStart(17);

                byte rfk;
                byte ifk;
                int dbValue = 0;
                for (int i = 0; i < 512; i++) {
                    rfk = mBoardFFT[i];
                    ifk = mBoardFFT[i + 1];
                    float magnitude = (rfk * rfk + ifk * ifk);
                    dbValue += java.lang.Math.max(0, 5 * Math.log10(magnitude));
                    if (dbValue < 0)
                        dbValue = 0;

                    if ((i & 63) == 0) {
                        int value = java.lang.Math.min(dbValue / 64, 255);
                        dbValue = 0;
                        //System.out.println("Visualizer Value[" + i / 64 + "] = " + value);

                        if (mListener != null && (i > 63))
                            mListener.sendCmdArg(value);
                    }
                }
                if (mListener != null) {
                    mListener.sendCmdArg((int) 0);
                    mListener.sendCmdEnd();
                }
            }
        }
        boardFlush();
        return;

    }


    void modeDisco() {

        if (mBoardFFT == null)
            return;

        synchronized (mSerialConn) {
            if (mVisualizer.getFft(mBoardFFT) == mVisualizer.SUCCESS) {


                if (mListener != null)
                    mListener.sendCmdStart(18);

                byte rfk;
                byte ifk;
                int dbValue = 0;
                for (int i = 0; i < 512; i++) {
                    rfk = mBoardFFT[i];
                    ifk = mBoardFFT[i + 1];
                    float magnitude = (rfk * rfk + ifk * ifk);
                    dbValue += java.lang.Math.max(0, 5 * Math.log10(magnitude));
                    if (dbValue < 0)
                        dbValue = 0;

                    if ((i & 63) == 0) {
                        int value = java.lang.Math.min(dbValue / 64, 255);
                        dbValue = 0;
                        //System.out.println("Visualizer Value[" + i / 64 + "] = " + value);

                        if (mListener != null && (i > 63))
                            mListener.sendCmdArg(value);
                    }
                }
                if (mListener != null) {
                    mListener.sendCmdArg((int) 0);
                    mListener.sendCmdEnd();
                }
            }
        }
        boardFlush();
        return;

    }

    */

    /**
     * Converts HSL components of a color to a set of RGB components.
     *
     * @param hsl  a float array with length equal to
     *             the number of HSL components
     * @param rgb  a float array with length of at least 3
     *             that contains RGB components of a color
     * @return a float array that contains RGB components
     */
    private static float[] HSLtoRGB(float[] hsl, float[] rgb) {
        if (rgb == null) {
            rgb = new float[3];
        }
        float hue = hsl[0];
        float saturation = hsl[1];
        float lightness = hsl[2];

        if (saturation > 0.0f) {
            hue = (hue < 1.0f) ? hue * 6.0f : 0.0f;
            float q = lightness + saturation * ((lightness > 0.5f) ? 1.0f - lightness : lightness);
            float p = 2.0f * lightness - q;
            rgb[0]= normalize(q, p, (hue < 4.0f) ? (hue + 2.0f) : (hue - 4.0f));
            rgb[1]= normalize(q, p, hue);
            rgb[2]= normalize(q, p, (hue < 2.0f) ? (hue + 4.0f) : (hue - 2.0f));
        }
        else {
            rgb[0] = lightness;
            rgb[1] = lightness;
            rgb[2] = lightness;
        }
        return rgb;
    }

    private static float normalize(float q, float p, float color) {
        if (color < 1.0f) {
            return p + (q - p) * color;
        }
        if (color < 3.0f) {
            return q;
        }
        if (color < 4.0f) {
            return p + (q - p) * (4.0f - color);
        }
        return p;
    }

    private void getFirePixels(int color) {

    }

    private final int[] kFireColorsOcto = {
            BurnerBoard.getRGB(0,0,0),
            BurnerBoard.getRGB(0,0,0),
            BurnerBoard.getRGB(1,0,0),
            BurnerBoard.getRGB(2,0,0),
            BurnerBoard.getRGB(3,0,0),
            BurnerBoard.getRGB(4,0,0),
            BurnerBoard.getRGB(6,0,0),
            BurnerBoard.getRGB(7,0,0),
            BurnerBoard.getRGB(9,0,0),
            BurnerBoard.getRGB(10,0,0),
            BurnerBoard.getRGB(12,0,0),
            BurnerBoard.getRGB(14,0,0),
            BurnerBoard.getRGB(17,0,0),
            BurnerBoard.getRGB(19,0,0),
            BurnerBoard.getRGB(21,0,0),
            BurnerBoard.getRGB(24,0,0),
            BurnerBoard.getRGB(26,0,0),
            BurnerBoard.getRGB(29,0,0),
            BurnerBoard.getRGB(32,0,0),
            BurnerBoard.getRGB(35,0,0),
            BurnerBoard.getRGB(38,0,0),
            BurnerBoard.getRGB(42,0,0),
            BurnerBoard.getRGB(45,0,0),
            BurnerBoard.getRGB(48,0,0),
            BurnerBoard.getRGB(52,0,0),
            BurnerBoard.getRGB(56,0,0),
            BurnerBoard.getRGB(59,0,0),
            BurnerBoard.getRGB(63,0,0),
            BurnerBoard.getRGB(67,0,0),
            BurnerBoard.getRGB(72,0,0),
            BurnerBoard.getRGB(76,0,0),
            BurnerBoard.getRGB(80,0,0),
            BurnerBoard.getRGB(84,0,0),
            BurnerBoard.getRGB(89,0,0),
            BurnerBoard.getRGB(93,0,0),
            BurnerBoard.getRGB(98,0,0),
            BurnerBoard.getRGB(102,0,0),
            BurnerBoard.getRGB(107,0,0),
            BurnerBoard.getRGB(112,0,0),
            BurnerBoard.getRGB(117,0,0),
            BurnerBoard.getRGB(122,0,0),
            BurnerBoard.getRGB(127,0,0),
            BurnerBoard.getRGB(132,0,0),
            BurnerBoard.getRGB(137,0,0),
            BurnerBoard.getRGB(142,0,0),
            BurnerBoard.getRGB(148,0,0),
            BurnerBoard.getRGB(155,0,0),
            BurnerBoard.getRGB(160,0,0),
            BurnerBoard.getRGB(166,0,0),
            BurnerBoard.getRGB(172,0,0),
            BurnerBoard.getRGB(176,0,0),
            BurnerBoard.getRGB(179,0,0),
            BurnerBoard.getRGB(182,1,0),
            BurnerBoard.getRGB(185,2,0),
            BurnerBoard.getRGB(188,3,0),
            BurnerBoard.getRGB(191,4,0),
            BurnerBoard.getRGB(194,5,0),
            BurnerBoard.getRGB(197,7,0),
            BurnerBoard.getRGB(200,9,0),
            BurnerBoard.getRGB(203,10,0),
            BurnerBoard.getRGB(206,12,0),
            BurnerBoard.getRGB(209,14,0),
            BurnerBoard.getRGB(212,17,0),
            BurnerBoard.getRGB(216,19,0),
            BurnerBoard.getRGB(219,21,0),
            BurnerBoard.getRGB(222,24,0),
            BurnerBoard.getRGB(225,26,0),
            BurnerBoard.getRGB(228,29,0),
            BurnerBoard.getRGB(232,32,0),
            BurnerBoard.getRGB(235,35,0),
            BurnerBoard.getRGB(238,38,0),
            BurnerBoard.getRGB(243,42,0),
            BurnerBoard.getRGB(247,45,0),
            BurnerBoard.getRGB(250,48,0),
            BurnerBoard.getRGB(253,52,0),
            BurnerBoard.getRGB(255,56,0),
            BurnerBoard.getRGB(255,62,0),
            BurnerBoard.getRGB(255,68,0),
            BurnerBoard.getRGB(255,74,1),
            BurnerBoard.getRGB(255,81,1),
            BurnerBoard.getRGB(255,88,1),
            BurnerBoard.getRGB(255,94,2),
            BurnerBoard.getRGB(255,101,2),
            BurnerBoard.getRGB(255,108,3),
            BurnerBoard.getRGB(255,115,3),
            BurnerBoard.getRGB(255,123,4),
            BurnerBoard.getRGB(255,131,4),
            BurnerBoard.getRGB(255,140,5),
            BurnerBoard.getRGB(255,148,6),
            BurnerBoard.getRGB(255,156,6),
            BurnerBoard.getRGB(255,164,7),
            BurnerBoard.getRGB(255,173,8),
            BurnerBoard.getRGB(255,182,9),
            BurnerBoard.getRGB(255,191,10),
            BurnerBoard.getRGB(255,200,10),
            BurnerBoard.getRGB(255,209,11),
            BurnerBoard.getRGB(255,220,13),
            BurnerBoard.getRGB(255,230,14),
            BurnerBoard.getRGB(255,240,15),
            BurnerBoard.getRGB(255,250,16)
    };

    private void initFire() {

        mFireScreen = new int[mBoardWidth * 2 * (mBoardHeight + 6)];

        for (int i = 0; i < 32; ++i) {
            /* black to blue, 32 values*/
            //mFireColors[i][2] = i << 1;

            /* blue to red, 32 values*/
            mFireColors[i + 32][0] = i << 3;
            //mFireColors[i + 32][2] = 64 - (i << 1);

            /*red to yellow, 32 values*/
            mFireColors[i + 64][0] = 255;
            mFireColors[i + 64][1] = i << 3;

            /* yellow to white, 162 */
            mFireColors[i + 96][0] = 255;
            //mFireColors[i + 96][1] = 255;
            mFireColors[i + 96][1] = 192;
            mFireColors[i + 96][2] = i << 2;
            mFireColors[i + 128][0] = 255;
            //mFireColors[i + 128][1] = 255;
            mFireColors[i + 128][1] = 192;
            //mFireColors[i + 128][2] = 64 + (i << 2);
            mFireColors[i + 128][2] = 8 + (i << 2);
            mFireColors[i + 160][0] = 255;
            //mFireColors[i + 160][1] = 255;
            mFireColors[i + 160][1] = 192;
            //mFireColors[i + 160][2] = 128 + (i << 2);
            mFireColors[i + 160][2] = 16 + (i << 2);
            mFireColors[i + 192][0] = 255;
            //mFireColors[i + 192][1] = 255;
            mFireColors[i + 192][1] = 192;
            //mFireColors[i + 192][2] = 192 + i;
            mFireColors[i + 192][2] = 32 + i;
            mFireColors[i + 224][0] = 255;
            //mFireColors[i + 224][1] = 255;
            mFireColors[i + 224][1] = 192;
            //mFireColors[i + 224][2] = 224 + i;
            mFireColors[i + 224][2] = 48 + i;
        }

        for (int i = 0; i < 256; i++) {
            final int fireColor = wheel(90 - (i / 5));
            final int dimmer =  java.lang.Math.max(1, 256 - (int)(java.lang.Math.log10((double)(i / 4 + 1)) * 200));
            mFireColors[i][0] =  (fireColor & 0xff) / dimmer;
            mFireColors[i][1] =  ((fireColor & 0xff00) >> 8) / dimmer;
            mFireColors[i][2] =   ((fireColor & 0xff0000) >> 16) / dimmer;
        }
    }

    /*
    * From PixelController Fire.java
    * Originally modeled from http://lodev.org/cgtutor/fire.html
    */
    public void modeFire() {
        int j = mBoardWidth * 2 * (mBoardHeight + 1);
        int random;
        for (int i = 0; i < (mBoardWidth * 2); i++) {
            random = mRandom.nextInt(16);
			// the lower the value, the intense the fire,
			// compensate a lower value with a higher decay value
            if (random > 8) {
				/*maximum heat*/
                mFireScreen[j + i] = 255;
            } else {
                mFireScreen[j + i] = 0;
            }
        }

        mBurnerBoard.scrollPixels(true);

		/* move fire upwards, start at bottom*/
        int temp;
        int [] lastTemps = new int[mBoardWidth * 2];
        for (int index = 0; index < mBoardHeight + 1; index++) {
            for (int i = 0; i < mBoardWidth * 2; i++) {
                if (i == 0) {
					/* at the left border*/
                    temp = mFireScreen[j];
                    temp += mFireScreen[j + 1];
                    temp += mFireScreen[j - mBoardWidth * 2];
                    temp /=3;
                } else
                if (i == mBoardWidth) {
						/* at the right border*/
                    temp = mFireScreen[j + i];
                    temp += mFireScreen[j - mBoardWidth * 2 + i];
                    temp += mFireScreen[j + i - 1];
                    temp /= 3;
                } else {
                    temp = mFireScreen[j + i];
                    temp += mFireScreen[j + i + 1];
                    temp += mFireScreen[j + i - 1];
                    temp += mFireScreen[j - mBoardWidth * 2 + i];
                    temp >>= 2;
                }
                if (temp > 1) {
					/* decay */
                    temp --;
                }

                int dofs = j - mBoardWidth * 2 + i;
                mFireScreen[dofs] = temp;
                if (dofs<(mBoardWidth * 2 * mBoardHeight)) {
                    int x = dofs % (mBoardWidth * 2);
                    int y = java.lang.Math.min(java.lang.Math.max(0,
                            (dofs / (mBoardWidth * 2 )) + 1), 69);
                    // Bigger number leaves more flame tails
                    if (lastTemps[i] > 55    ) {
                        //mBurnerBoard.setPixel(x / 2, y * 2,
                        //int temp100 = (int)((float)temp * (float)150 / (float)255);
                        //temp100 = java.lang.Math.min(99, temp100);
                        //mBurnerBoard.setPixel(x / 2, y, kFireColorsOcto[temp100]);
                        mBurnerBoard.setPixel(x / 2, y,
                                mFireColors[temp][0],
                                mFireColors[temp][1],
                                mFireColors[temp][2]);

                        //mBurnerBoard.setPixel(x / 2, y * 2 + 1,
                                //mFireColors[temp][0],
                                //mFireColors[temp][1],
                                //mFireColors[temp][2]);
                    } else {
                        //mBurnerBoard.setPixel(x / 2, y, 0, 255,  0);
                    }
                    lastTemps[i] = temp;
                }
            }
            j -= mBoardWidth;
        }
        mBurnerBoard.setOtherlightsAutomatically();
        mBurnerBoard.flushPixels();
    }

    void drawTheMan(int color) {
        int x;
        int row;

        int the_man[] = {
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b1000000001,
                0b1100000011,
                0b1100000011,
                0b0110000110,
                0b0110000110,
                0b0110000110,
                0b0110000110,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0001001000,
                0b0001001000,
                0b0001001000,
                0b0001001000,
                0b0001001000,
                0b0001001000,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0011001100,
                0b0010000100,
                0b0110000110,
                0b0110000110,
                0b0110110110,
                0b1100110011,
                0b1101111011,
                0b1001111001,
                0b0000110000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000,
                0b0000000000};

        //mBoardScreen.clear();
        for (row = 0; row < the_man.length; row++) {
            for (x = 0; x < mBoardWidth; x++) {
                mBurnerBoard.setPixel(x, row, ((the_man[row] & (1 << x)) > 0)?
                        color : BurnerBoard.getRGB(0, 0, 0));
            }
        }
        mBurnerBoard.fillOtherlight(BurnerBoard.kLeftSightlight, color);
        mBurnerBoard.fillOtherlight(BurnerBoard.kRightSidelight, color);
        mBurnerBoard.flushPixels();
    }

    // Thanks Pruss...
    // I see blondes, brunets, redheads...
    private void modeTheMan() {
        int color;
        color = mRandom.nextInt(4) % 2 == 0 ? BurnerBoard.getRGB(80, 80, 80): wheel(wheel_color); //Chance of 1/3rd
        wheelInc(1);
        drawTheMan(color);
    }




}
