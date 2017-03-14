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
    private int mBoardSideLights = 78;
    private int [][]  mFireColors = new int[256][3];
    private byte [] mBoardScreen;
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
    }

    public void attachAudio(int audioSessionId) {
        int vSize;

        l("session=" + audioSessionId);
        mAudioSessionId = audioSessionId;
        // Create the Visualizer object and attach it to our media player.
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

                case 0:
                    sleepTime = 20;
                    modeAudioBeat();
                    break;

                case 1:
                    sleepTime = 20;
                    modeMatrix(true);
                    //modeTestRow();
                    break;

                case 2:
                    sleepTime = 20;
                    modeAudioCenter();
                    break;

                case 3:
                    sleepTime = 50;
                    modeFire();
                    break;

                case 4:
                    sleepTime = 20;
                    modeTest();
                    break;
                case 5:
                    sleepTime = 20;
                    modeTheMan();
                    break;
                case 6:
                    sleepTime = 20;
                    modeAudioBarV();
                    break;
                case 7:
                    sleepTime = 20;
                    modeAudioFuzzer();
                    break;
                case 8:
                    sleepTime = 20;
                    //modeTest();
                    modeAudioBeat();
                    break;

                case 9:
                    sleepTime = 20;
                    modeTestColors();
                    break;

                //case 9:
                //sleepTime = 30;
                //modeAudioBeat();
                //break;

                default:
                    //mBurnerBoard.setMode(1);
                    mBoardMode = 1;
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

    int testColorState = 0;
    void modeTestRow() {
        byte [] testRow = new byte[36];

        switch (testColorState) {
            case 0:
                for (int i = 0; i < 12; i += 3) {
                    testRow[i] = (byte) 255;
                    testRow[i+1] = (byte) 0;
                    testRow[i+2] = (byte) 0;
                }
                mBurnerBoard.setRow(0, testRow);
                mBurnerBoard.flush();
                testColorState++;
                break;
            case 1:
                for (int i = 0; i < 12; i += 3) {
                    testRow[i] = (byte) 0;
                    testRow[i+1] = (byte) 255;
                    testRow[i+2] = (byte) 0;
                }
                mBurnerBoard.setRow(0, testRow);
                mBurnerBoard.flush();
                testColorState++;
                break;
            case 2:
                for (int i = 0; i < 12; i += 3) {
                    testRow[i] = (byte) 0;
                    testRow[i+1] = (byte) 0;
                    testRow[i+2] = (byte) 255;
                }
                mBurnerBoard.setRow(0, testRow);
                mBurnerBoard.flush();
                testColorState++;
                break;
            case 3:
                for (int i = 0; i < 12; i += 3) {
                    testRow[i] = (byte) 100;
                    testRow[i+1] = (byte) 100;
                    testRow[i+2] = (byte) 100;
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

    int testColor = 0;

    void modeTest() {

        mBurnerBoard.fillScreen(
                (byte)mFireColors[testColor][0],
                (byte)mFireColors[testColor][1],
                (byte)mFireColors[testColor][2]);
        mBurnerBoard.flushPixels();
        testColor++;
        if (testColor > 255) {
            testColor = 0;
        }

    }

    private void modeTestColors() {
        switch (testColorState) {
            case 0:
                mBurnerBoard.fillScreen((byte)255, (byte)0, (byte)0);
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 1:
                mBurnerBoard.fillScreen((byte)0, (byte)255, (byte)0);
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 2:
                mBurnerBoard.fillScreen((byte)0, (byte)0, (byte)255);
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 3:
                mBurnerBoard.fillScreen((byte)10, (byte)0, (byte)0);
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 4:
                mBurnerBoard.fillScreen((byte)0, (byte)10, (byte)0);
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 5:
                mBurnerBoard.fillScreen((byte)0, (byte)0, (byte)10);
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 6:
                mBurnerBoard.fillScreen((byte)0, (byte)0, (byte)10);
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 7:
                mBurnerBoard.fillScreen((byte)255, (byte)128, (byte)0);
                mBurnerBoard.flushPixels();
                testColorState++;
                break;
            case 8:
                mBurnerBoard.fillScreen((byte)204, (byte)0, (byte)204);
                mBurnerBoard.flushPixels();
                testColorState = 0;
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


    void modeMatrix(boolean isTop) {

        // Row plus two pixels for side lights
        byte[] pixels = new byte[mBoardWidth * 3 + 6];

        int color;
        int x;
        int y;
        int sideLight;

        if (isTop) {
            y = 69;
            sideLight = mBoardSideLights  - 1;
        } else {
            y = 0;
            sideLight = 0;
        }

        for (x = 0; x < 10; x++) {
            //Chance of 1/3rd
            color = mRandom.nextInt(2) == 0 ? BurnerBoard.getRGB(0, 0, 0): wheel(wheel_color);
            mBurnerBoard.setPixel(x, y, color);
            // Ripple down the side lights with the same color as the edges
            if (x == 0) {
                mBurnerBoard.setSideLight(0, sideLight, color);
            }
            if (x == 9) {
                mBurnerBoard.setSideLight(1, sideLight, color);
            }
            wheelInc(1);
        }
        mBurnerBoard.scrollPixels(true);
        mBurnerBoard.flushPixels();

        return;

    }

    int testValue = 0;

    void modeAudioMatrix() {

        byte[] pixels = new byte[36];

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
                        pixels[pixel] = (byte) java.lang.Math.max(0, value);
//                            pixels[pixel] = (byte)testValue;
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

            mBurnerBoard.fillScreen((byte) r, (byte) g, (byte) b);
            mBurnerBoard.flushPixels();
            return;
        }
    }


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
            rfk = mBoardFFT[i];
            ifk = mBoardFFT[i + 1];
            float magnitude = (rfk * rfk + ifk * ifk);
            dbValue += java.lang.Math.max(0, 3 * (Math.log10(magnitude) - 1));
            if (dbValue < 0)
                dbValue = 0;
            dbLevels[i / 32] += dbValue;
            dbLevels[i / 32] = java.lang.Math.min(dbLevels[i / 32], 255);
        }
        return dbLevels;
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
            int level = java.lang.Math.min(dbLevels[value] / 7, 35);
            //l("level " + value + ":" + level + ":" + dbLevels[value]);
            for (int y = 0; y < level; y++) {
                if (value == 3) {
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

        int [] dbLevels = getLevels();
        if (dbLevels == null)
            return;
        for (int x = 0; x < 35; x++) {
            dbLevels = getLevels();
            //mBurnerBoard.fadePixels(10);
            int level = java.lang.Math.max(0, (dbLevels[5] / 8 - 8));
            if (dbLevels == null)
                return;
            //drawRectCenter(x, wheel(wheel_color));
            if (level > 0) {
                int c = wheel(wheel_color);
                drawRectCenter(x, c);
                wheelInc(level);
            }
            //System.out.println(x + ":" + level);

        }
        //mBurnerBoard.fadePixels(1);
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
        for (int index = 0; index < mBoardHeight+1; index++) {
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
                    if (lastTemps[i] > 32    ) {
                        //mBurnerBoard.setPixel(x / 2, y * 2,
                                mBurnerBoard.setPixel(x / 2, y,
                                (byte)mFireColors[temp][0],
                                (byte)mFireColors[temp][1],
                                (byte)mFireColors[temp][2]);
                        //mBurnerBoard.setPixel(x / 2, y * 2 + 1,
                                //(byte)mFireColors[temp][0],
                                //(byte)mFireColors[temp][1],
                                //(byte)mFireColors[temp][2]);
                    } else {
                        //mBurnerBoard.setPixel(x / 2, y, (byte)0, (byte)255, (byte) 0);
                    }
                    lastTemps[i] = temp;
                }
            }
            j -= mBoardWidth;
        }
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
