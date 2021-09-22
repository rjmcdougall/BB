package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.TimeSync;
import com.richardmcdougall.bb.board.RGB;

/**
 * Created by rmc on 6/21/18.
 */

public class Matrix extends Visualization {

    public Matrix(BBService service) {
        super(service);
    }

    private Wheel mWheel = new Wheel();

    public static final int kMatrixBurnerColor = 1;
    public static final int kMatrixLunarian = 3;
    public static final int kMatrixSync = 10;

    private int fireColor = 40;

    public void update(int mode) {

        int color;
        int x;
        int y;
        int pixelSkip;

        pixelSkip = 2;

        y = service.burnerBoard.boardHeight - 1;

        for (x = 0; x < service.burnerBoard.boardWidth / pixelSkip; x++) {
            //Chance of 1/3rd
            switch (mode) {
                case kMatrixBurnerColor:
                case kMatrixSync:

                    if (service.visualizationController.mRandom.nextInt(4) != 0) {
                        color = RGB.getARGBInt(0, 0, 0);
                    } else {
                        color = mWheel.wheelState();
                        mWheel.wheelInc(1);
                    }
                    for (int i = 0; i < pixelSkip; i++) {
                        service.burnerBoard.setPixel(pixelSkip * x + i, y, color);
                    }
                    break;

                case kMatrixLunarian:
                    color = service.visualizationController.mRandom.nextInt(4   ) != 0 ?
                            RGB.getARGBInt(0, 0, 0) : RGB.getARGBInt(255, 255, 255);
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);
                    for (int i = 0; i < pixelSkip; i++) {
                        service.burnerBoard.setPixel(pixelSkip * x + i, y, color);
                    }
                    break;

                default:
            }

            fireColor += 1;
            if (fireColor > 180) {
                fireColor = 40;
            }

        }

        for (int i = 0; i < service.visualizationController.mMultipler4Speed; i++) {
        //for (int i = 0; i <3; i++) {
            service.burnerBoard.scrollPixels(true);
        }

        switch (mode) {
            case kMatrixSync:
                int syncColor =
                        mWheel.wheel((int)(TimeSync.GetCurrentClock() / 5) % 360);
                if (syncColor > 0) {
                    service.burnerBoard.fillScreenMask(syncColor);

                }

                break;

            default:
                break;
        }
        service.burnerBoard.flush();

        return;

    }
}
