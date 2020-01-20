package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
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

    public Mickey(BBService service) {
        super(service);
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
            service.burnerBoard.setPixel(n / mBoardHeight, (kLEDS1 - n - 1) % mBoardHeight, color);
        } else {
            service.burnerBoard.setPixel(1 + kLEDS1 / mBoardHeight +
                    ((n - kLEDS1) / mBoardHeight),  (n - kLEDS1) % mBoardHeight, color);
        }
    }

    void modeMickeyGold() {
        service.burnerBoard.fillScreen(255, 147, 41);
        service.burnerBoard.flush();
    }private int mickeySparkleNo = 0;
    private final static int kMikeySparkleMiddle = (kLEDS1 + kLEDS2)/ 2;

    void modeMickey2a() {

        int ledNo;

        service.burnerBoard.fadePixels(10);

        if (mickeySparkleNo > kMikeySparkleMiddle) {
            service.burnerBoard.flush();
            return;
        }

        for (ledNo = kMikeySparkleMiddle + mickeySparkleNo;
             ledNo < kMikeySparkleMiddle + mickeySparkleNo + 6; ledNo++) {
            mickeySetPixel(ledNo, mWheel.wheelDim(35,
                    (float)service.boardVisualization.mRandom.nextInt(100) / (float)100.0));
        }

        for (ledNo = kMikeySparkleMiddle - mickeySparkleNo;
             ledNo > kMikeySparkleMiddle - mickeySparkleNo - 6; ledNo--) {
            mickeySetPixel(ledNo, mWheel.wheelDim(35,
                    (float) service.boardVisualization.mRandom.nextInt(100) / (float) 100.0));
        }

        service.burnerBoard.flush();
        mickeySparkleNo+= 1;
    }

    void modeMickeySparkle() {

        int ledNo;

        //service.burnerBoard.fadePixels(10);

        if (mickeySparkleNo > kMikeySparkleMiddle) {
            service.burnerBoard.fadePixels(20);
            service.burnerBoard.flush();
            mickeySparkleNo+= 1;
            if (mickeySparkleNo > kMikeySparkleMiddle + 20) {
                mickeySparkleNo = 0;
            }
            return;
        }

        for (ledNo = kMikeySparkleMiddle; ledNo < kMikeySparkleMiddle + mickeySparkleNo; ledNo++) {
            mickeySetPixel(ledNo, mWheel.wheelDim(35,
                    (float)service.boardVisualization.mRandom.nextInt(100) / (float)100.0));
        }

        for (ledNo = kMikeySparkleMiddle; ledNo > kMikeySparkleMiddle - mickeySparkleNo; ledNo--) {
            mickeySetPixel(ledNo, mWheel.wheelDim(35,
                    (float) service.boardVisualization.mRandom.nextInt(100) / (float) 100.0));
        }

        service.burnerBoard.flush();
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
        service.burnerBoard.flush();
        mickeyColor ++;
        mickeyColor %= 360;
    }

    void modeMickeyBlank() {

        service.burnerBoard.fillScreen(0, 0, 0);
        service.burnerBoard.flush();
    }private int mickeyRotate = 0;
    void modeMickeyBlueGold() {

        int ledNo;

        for (ledNo = 0; ledNo < (kLEDS1 + kLEDS2); ledNo++) {
            int index = (mickeyRotate + ledNo) % 10; //* kMickeyPhaseShift / 2) % 360;
            mickeySetPixel(ledNo, index < 5 ? BurnerBoard.getRGB(255, 147, 41) :
                    BurnerBoard.getRGB(0, 0, 255));
        }
        service.burnerBoard.flush();
        mickeyRotate ++;
        mickeyRotate %= 360;
    }
}
