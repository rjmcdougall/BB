package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by rmc on 6/21/18.
 */

// Menlo Mickey
public class Mickey extends Visualization {

    public static final int kMickeyGold = 1;
    public static final int kMickey2a = 2;
    public static final int kMickeySparkle = 3;
    public static final int kMickeyColors = 4;
    public static final int kMickeyBlank = 5;
    public static final int kMickeyBlueGold = 6;

    private Wheel mWheel = new Wheel();

    private final static int kLEDS1 = 99;
    private final static int kLEDS2 = 137;

    public Mickey(BurnerBoard bb, BoardVisualization visualization) {
        super(bb, visualization);
    }

    public void update(int mode) {
        switch (mode) {
            case kMickeyGold:
                modeMickeyGold();
                break;
            case kMickey2a:
                modeMickey2a();
                break;
            case kMickeySparkle:
                modeMickeySparkle();
                break;
            case kMickeyColors:
                modeMickeyColors();
                break;
            case kMickeyBlank:
                modeMickeyBlank();
                break;
            case kMickeyBlueGold:
                modeMickeyBlueGold();
                break;
            default:
                break;
        }
    }

    public void setMode(int mode) {
        mickeySparkleNo = 0;
    }

    void mickeySetPixel(int n, int color) {
        if (n < 0 || n >= (kLEDS1 + kLEDS2)) {
            return;
        }
        if (n < kLEDS1) {
            mBurnerBoard.setPixel(n / mBoardHeight, (kLEDS1 - n - 1) % mBoardHeight, color);
        } else {
            mBurnerBoard.setPixel(1 + kLEDS1 / mBoardHeight +
                    ((n - kLEDS1) / mBoardHeight),  (n - kLEDS1) % mBoardHeight, color);
        }
    }

    void modeMickeyGold() {
        mBurnerBoard.fillScreen(255, 147, 41);
        mBurnerBoard.flush();
    }


    private int mickeySparkleNo = 0;
    private final static int kMikeySparkleMiddle = (kLEDS1 + kLEDS2)/ 2;

    void modeMickey2a() {

        int ledNo;

        mBurnerBoard.fadePixels(10);

        if (mickeySparkleNo > kMikeySparkleMiddle) {
            mBurnerBoard.flush();
            return;
        }

        for (ledNo = kMikeySparkleMiddle + mickeySparkleNo;
             ledNo < kMikeySparkleMiddle + mickeySparkleNo + 6; ledNo++) {
            mickeySetPixel(ledNo, mWheel.wheelDim(35,
                    (float)mBoardVisualizion.mRandom.nextInt(100) / (float)100.0));
        }

        for (ledNo = kMikeySparkleMiddle - mickeySparkleNo;
             ledNo > kMikeySparkleMiddle - mickeySparkleNo - 6; ledNo--) {
            mickeySetPixel(ledNo, mWheel.wheelDim(35,
                    (float) mBoardVisualizion.mRandom.nextInt(100) / (float) 100.0));
        }

        mBurnerBoard.flush();
        mickeySparkleNo+= 1;
    }

    void modeMickeySparkle() {

        int ledNo;

        //mBurnerBoard.fadePixels(10);

        if (mickeySparkleNo > kMikeySparkleMiddle) {
            mBurnerBoard.fadePixels(20);
            mBurnerBoard.flush();
            mickeySparkleNo+= 1;
            if (mickeySparkleNo > kMikeySparkleMiddle + 20) {
                mickeySparkleNo = 0;
            }
            return;
        }

        for (ledNo = kMikeySparkleMiddle; ledNo < kMikeySparkleMiddle + mickeySparkleNo; ledNo++) {
            mickeySetPixel(ledNo, mWheel.wheelDim(35,
                    (float)mBoardVisualizion.mRandom.nextInt(100) / (float)100.0));
        }

        for (ledNo = kMikeySparkleMiddle; ledNo > kMikeySparkleMiddle - mickeySparkleNo; ledNo--) {
            mickeySetPixel(ledNo, mWheel.wheelDim(35,
                    (float) mBoardVisualizion.mRandom.nextInt(100) / (float) 100.0));
        }

        mBurnerBoard.flush();
        mickeySparkleNo+= 1;
    }

    private final static int kMickeyPhaseShift = 10;
    private int mickeyColor = 0;

    void modeMickeyColors() {

        int ledNo;

        for (ledNo = 0; ledNo < (kLEDS1 + kLEDS2); ledNo++) {
            int index = kMickeyPhaseShift * (mickeyColor + ledNo) % 360; //* kMickeyPhaseShift / 2) % 360;
            mickeySetPixel(ledNo, mWheel.wheel(index));
        }
        mBurnerBoard.flush();
        mickeyColor ++;
        mickeyColor %= 360;
    }

    void modeMickeyBlank() {

        mBurnerBoard.fillScreen(0, 0, 0);
        mBurnerBoard.flush();
    }


    private int mickeyRotate = 0;
    void modeMickeyBlueGold() {

        int ledNo;

        for (ledNo = 0; ledNo < (kLEDS1 + kLEDS2); ledNo++) {
            int index = (mickeyRotate + ledNo) % 10; //* kMickeyPhaseShift / 2) % 360;
            mickeySetPixel(ledNo, index < 5 ? BurnerBoard.getRGB(255, 147, 41) :
                    BurnerBoard.getRGB(0, 0, 255));
        }
        mBurnerBoard.flush();
        mickeyRotate ++;
        mickeyRotate %= 360;
    }
}
