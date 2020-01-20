package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.BurnerBoard;
import com.richardmcdougall.bb.board.BurnerBoardDirectMap;

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

    private final static int kLEDS = BurnerBoardDirectMap.kVisualizationDirectMapWidth * BurnerBoardDirectMap.kVisualizationDirectMapHeight;

    public JosPack(BBService service) {
        super(service);
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
        service.burnerBoard.setPixel(n / service.burnerBoard.boardHeight, (kLEDS - n - 1) % service.burnerBoard.boardHeight, color);
    }

    void modeJPGold() {
        //service.burnerBoard.fillScreen(255, 147, 41);

        service.burnerBoard.fillScreen(255, 215, 0);

        service.burnerBoard.flush();
    }

    private int jpSparkleNo = 0;
    private final static int kJPSparkleMiddle = kLEDS / 2;

    void modeJPSparkle() {

        int ledNo;

        //service.burnerBoard.fadePixels(10);

        if (jpSparkleNo > kJPSparkleMiddle) {
            service.burnerBoard.fadePixels(30);
            service.burnerBoard.flush();
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
                    (float)service.boardVisualization.mRandom.nextInt(100) / (float)100.0));
        }

        for (ledNo = kJPSparkleMiddle; ledNo > kJPSparkleMiddle - jpSparkleNo; ledNo--) {
            jpSetPixel(ledNo, mWheel.wheelDim(35,
                    (float) service.boardVisualization.mRandom.nextInt(100) / (float) 100.0));
        }

        service.burnerBoard.flush();
        jpSparkleNo+= 1;
    }

    /* This is a relic from MenloMickey - I'm unclear what it is trying to do, and it doesn't
       look good on the JosPacks atm, so commenting out for now.
    void modeJP2a() {

        int ledNo;

        service.burnerBoard.fadePixels(10);

        if (jpSparkleNo > kJPSparkleMiddle) {
            service.burnerBoard.flush();
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

        service.burnerBoard.flush();
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
        service.burnerBoard.flush();
        jpColor ++;
        jpColor %= 360;
    }

    void modeJPBlank() {
        service.burnerBoard.fillScreen(0, 0, 0);
        service.burnerBoard.flush();
    }

    private int bgRotate = 0;
    void modeJPBlueGold() {

        int ledNo;

        for (ledNo = 0; ledNo < kLEDS; ledNo++) {
            int index = (bgRotate + ledNo) % 10; //* kJPPhaseShift / 2) % 360;
            jpSetPixel(ledNo, index < 5 ? BurnerBoard.getRGB(255, 147, 41) :
                    BurnerBoard.getRGB(0, 0, 255));
        }
        service.burnerBoard.flush();
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
        service.burnerBoard.flush();
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

        service.burnerBoard.flush();
        triRotate++;
        triRotate %= 360;

    }
}
