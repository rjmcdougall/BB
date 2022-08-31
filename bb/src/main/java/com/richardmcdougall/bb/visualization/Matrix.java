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
    public static final int kMatrixMermaid = 7;

    private int fireColor = 40;

    private static RGBList rgbList = new RGBList();

    public void update(int mode) {

        int color;
        int x;
        int y;
        int pixelSkip;

        pixelSkip = 1;

        y = service.burnerBoard.boardHeight - 1;

        for (x = 0; x < service.burnerBoard.boardWidth / pixelSkip; x++) {
            //Chance of 1/3rd
            switch (mode) {
                case kMatrixBurnerColor:
                case kMatrixSync:

                    if (service.visualizationController.mRandom.nextInt(3) != 0) {
                        color = RGB.getARGBInt(0, 0, 0);
                    } else {
                        color = mWheel.wheelState();
                        mWheel.wheelInc(1);
                    }
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);
                    break;

                case kMatrixMermaid:
                    if (service.visualizationController.mRandom.nextInt(3) == 0) {
                        color = rgbList.getColor("mediumseagreen").getARGBInt();
                    } else if (service.visualizationController.mRandom.nextInt(3) == 1) {
                        color = rgbList.getColor("black").getARGBInt();
                    } else {
                        color = rgbList.getColor("green").getARGBInt();
                    }
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);
                    break;

                case kMatrixLunarian:
                    color = service.visualizationController.mRandom.nextInt(2) == 0 ?
                            RGB.getARGBInt(0, 0, 0) : RGB.getARGBInt(255, 255, 255);
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);

                    break;

                default:
            }

            fireColor += 1;
            if (fireColor > 180) {
                fireColor = 40;
            }

        }

        for (int i = 0; i < service.visualizationController.mMultipler4Speed; i++) {
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
