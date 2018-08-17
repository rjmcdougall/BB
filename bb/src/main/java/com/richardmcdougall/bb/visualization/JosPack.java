package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by jib on 8/15/18.
 */

public class JosPack extends Visualization {

    public static final int kJPGold = 1;
    public static final int kJP2a = 2;
    public static final int kJPSparkle = 3;
    public static final int kJPColors = 4;
    public static final int kJPBlank = 5;
    public static final int kJPBlueGold = 6;

    private Wheel mWheel = new Wheel();

    private final static int kLEDS1 = 99;
    private final static int kLEDS2 = 137;

    public JosPack(BurnerBoard bb, BoardVisualization visualization) {
        super(bb, visualization);
    }

    public void update(int mode) {
        switch (mode) {
            case kJPGold:
                modeJPGold();
                break;
            case kJP2a:
                modeJP2a();
                break;
            case kJPSparkle:
                modeJPSparkle();
                break;
            case kJPColors:
                modeJPColors();
                break;
            case kJPBlank:
                modeJPBlank();
                break;
            case kJPBlueGold:
                modeJPBlueGold();
                break;
            default:
                break;
        }
    }

    public void setMode(int mode) {
        jpSparkleNo = 0;
    }

    void jpSetPixel(int n, int color) {
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

    void modeJPGold() {
        mBurnerBoard.fillScreen(255, 147, 41);
        mBurnerBoard.flush();
    }


    private int jpSparkleNo = 0;
    private final static int kJPSparkleMiddle = (kLEDS1 + kLEDS2)/ 2;

    void modeJP2a() {

        int ledNo;

        mBurnerBoard.fadePixels(10);

        if (jpSparkleNo > kJPSparkleMiddle) {
            mBurnerBoard.flush();
            return;
        }

        for (ledNo = kJPSparkleMiddle + jpSparkleNo;
             ledNo < kJPSparkleMiddle + jpSparkleNo + 6; ledNo++) {
            jpSetPixel(ledNo, mWheel.wheelDim(35,
                    (float)mBoardVisualizion.mRandom.nextInt(100) / (float)100.0));
        }

        for (ledNo = kJPSparkleMiddle - jpSparkleNo;
             ledNo > kJPSparkleMiddle - jpSparkleNo - 6; ledNo--) {
            jpSetPixel(ledNo, mWheel.wheelDim(35,
                    (float) mBoardVisualizion.mRandom.nextInt(100) / (float) 100.0));
        }

        mBurnerBoard.flush();
        jpSparkleNo+= 1;
    }

    void modeJPSparkle() {

        int ledNo;

        //mBurnerBoard.fadePixels(10);

        if (jpSparkleNo > kJPSparkleMiddle) {
            mBurnerBoard.fadePixels(20);
            mBurnerBoard.flush();
            jpSparkleNo+= 1;
            if (jpSparkleNo > kJPSparkleMiddle + 20) {
                jpSparkleNo = 0;
            }
            return;
        }

        for (ledNo = kJPSparkleMiddle; ledNo < kJPSparkleMiddle + jpSparkleNo; ledNo++) {
            jpSetPixel(ledNo, mWheel.wheelDim(35,
                    (float)mBoardVisualizion.mRandom.nextInt(100) / (float)100.0));
        }

        for (ledNo = kJPSparkleMiddle; ledNo > kJPSparkleMiddle - jpSparkleNo; ledNo--) {
            jpSetPixel(ledNo, mWheel.wheelDim(35,
                    (float) mBoardVisualizion.mRandom.nextInt(100) / (float) 100.0));
        }

        mBurnerBoard.flush();
        jpSparkleNo+= 1;
    }

    private final static int kJPPhaseShift = 10;
    private int jpColor = 0;

    void modeJPColors() {

        int ledNo;

        for (ledNo = 0; ledNo < (kLEDS1 + kLEDS2); ledNo++) {
            int index = kJPPhaseShift * (jpColor + ledNo) % 360; //* kJPPhaseShift / 2) % 360;
            jpSetPixel(ledNo, mWheel.wheel(index));
        }
        mBurnerBoard.flush();
        jpColor ++;
        jpColor %= 360;
    }

    void modeJPBlank() {

        mBurnerBoard.fillScreen(0, 0, 0);
        mBurnerBoard.flush();
    }


    private int jpRotate = 0;
    void modeJPBlueGold() {

        int ledNo;

        for (ledNo = 0; ledNo < (kLEDS1 + kLEDS2); ledNo++) {
            int index = (jpRotate + ledNo) % 10; //* kJPPhaseShift / 2) % 360;
            jpSetPixel(ledNo, index < 5 ? BurnerBoard.getRGB(255, 147, 41) :
                    BurnerBoard.getRGB(0, 0, 255));
        }
        mBurnerBoard.flush();
        jpRotate ++;
        jpRotate %= 360;
    }
}
