package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;
import com.richardmcdougall.bb.BurnerBoardUtil;

/**
 * Created by jib on 8/15/18.
 */

public class JosPack extends Visualization {

    public static final int kJPGold = 1;
    public static final int kJPTriColor = 2;
    public static final int kJPSparkle = 3;
    public static final int kJPColors = 4;
    public static final int kJPBlank = 5;
    public static final int kJPBlueGold = 6;
    public static final int kJPBluePurple = 7;

    private Wheel mWheel = new Wheel();

    private final static int kLEDS = BurnerBoardUtil.kVisualizationDirectMapWidth * BurnerBoardUtil.kVisualizationDirectMapHeight;

    public JosPack(BurnerBoard bb, BoardVisualization visualization) {
        super(bb, visualization);
    }

    public void update(int mode) {
        switch (mode) {
            case kJPGold:
                modeJPGold();
                break;
            case kJPTriColor:
                modeTriColor();
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
            case kJPBluePurple:
                modeJPBluePurple();
                break;
            default:
                break;
        }
    }

    public void setMode(int mode) {
        jpSparkleNo = 0;
    }

    void jpSetPixel(int n, int color) {
        if (n < 0 || n >= kLEDS) {
            return;
        }
        mBurnerBoard.setPixel(n / mBoardHeight, (kLEDS - n - 1) % mBoardHeight, color);
    }

    void modeJPGold() {
        //mBurnerBoard.fillScreen(255, 147, 41);

        mBurnerBoard.fillScreen(255, 215, 0);

        mBurnerBoard.flush();
    }


    private int jpSparkleNo = 0;
    private final static int kJPSparkleMiddle = kLEDS / 2;

    void modeJPSparkle() {

        int ledNo;

        //mBurnerBoard.fadePixels(10);

        if (jpSparkleNo > kJPSparkleMiddle) {
            mBurnerBoard.fadePixels(30);
            mBurnerBoard.flush();
            jpSparkleNo+= 1;

            if(jpSparkleNo >= kLEDS) {
                jpSparkleNo = 0;
            }

            /*
            if (jpSparkleNo > kJPSparkleMiddle + 20) {
                jpSparkleNo = 0;
            }
            return;
            */
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

    /* This is a relic from MenloMickey - I'm unclear what it is trying to do, and it doesn't
       look good on the JosPacks atm, so commenting out for now.
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
    */

    private final static int kJPPhaseShift = 10;
    private int jpColor = 0;

    void modeJPColors() {

        int ledNo;

        for (ledNo = 0; ledNo < kLEDS; ledNo++) {
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


    private int bgRotate = 0;
    void modeJPBlueGold() {

        int ledNo;

        for (ledNo = 0; ledNo < kLEDS; ledNo++) {
            int index = (bgRotate + ledNo) % 10; //* kJPPhaseShift / 2) % 360;
            jpSetPixel(ledNo, index < 5 ? BurnerBoard.getRGB(255, 147, 41) :
                    BurnerBoard.getRGB(0, 0, 255));
        }
        mBurnerBoard.flush();
        bgRotate++;
        bgRotate %= 360;
    }


    private int bpRotate = 0;
    void modeJPBluePurple() {

        int ledNo;

        for (ledNo = 0; ledNo < kLEDS; ledNo++) {
            int index = (bpRotate + ledNo) % 10; //* kJPPhaseShift / 2) % 360;
            jpSetPixel(ledNo, index < 5 ? BurnerBoard.getRGB(41, 147, 255) :
                    BurnerBoard.getRGB(0, 0, 255));
        }
        mBurnerBoard.flush();
        bpRotate++;
        bpRotate %= 360;
    }

    private int triRotate = 0;
    void modeTriColor() {

        int ledNo;

        for (ledNo = 0; ledNo < kLEDS; ledNo++) {
            int index = (triRotate + ledNo) % 15;
            int rgb = index < 5 ? BurnerBoard.getRGB(255, 147, 41) :
                      index < 10 ? BurnerBoard.getRGB(41, 147, 255) :
                      BurnerBoard.getRGB(0, 0, 255);
            jpSetPixel(ledNo, rgb);
        }

        mBurnerBoard.flush();
        triRotate++;
        triRotate %= 360;

    }
}
