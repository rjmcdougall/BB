package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.TimeSync;
import com.richardmcdougall.bb.board.RGB;
import com.richardmcdougall.bbcommon.BoardState;

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
    public static final int kMatrixMezcal = 20;

    private int diagonal = 4;
    private int mexcal_color = mWheel.wheelState();

    private int mezcal_row = 2;
    private static RGBList rgbList = new RGBList();

    public void update(int mode) {

        int color;
        int x;
        int y;
        int pixelSkip = 1;
        int multiplier4Speed = service.visualizationController.mMultipler4Speed;

        if (service.boardState.GetBoardType() == BoardState.BoardType.v4) {
            pixelSkip = 2;
            if (multiplier4Speed == 1) {
                multiplier4Speed = 2;
            }
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.azul) {
            pixelSkip = 2;
            if (multiplier4Speed == 1) {
                multiplier4Speed = 2;
            }
        }

        // Specific params for Mezcal pattern
        if (mode == kMatrixMezcal) {
            pixelSkip = 1;
            multiplier4Speed = 4;
        }

        if (mode == kMatrixMermaid) {
            pixelSkip = 1;
            multiplier4Speed = 1;
        }

        y = service.burnerBoard.boardHeight - 1;

        for (x = 0; x < service.burnerBoard.boardWidth / pixelSkip; x += pixelSkip) {
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
                    for (int p = 0; p < pixelSkip; p++) {
                        service.burnerBoard.setPixel(pixelSkip * x + p, y, color);
                    }
                    break;

                case kMatrixMermaid:
                    if (service.visualizationController.mRandom.nextInt(3) == 0) {
                        color = rgbList.getColor("mediumseagreen").getARGBInt();
                    } else if (service.visualizationController.mRandom.nextInt(3) == 1) {
                        color = rgbList.getColor("green").getARGBInt();
                    } else {
                        color = rgbList.getColor("green").getARGBInt();
                    }
                    for (int p = 0; p < pixelSkip; p++) {
                        service.burnerBoard.setPixel(pixelSkip * x + p, y, color);
                    }
                    break;


                case kMatrixLunarian:
                    color = service.visualizationController.mRandom.nextInt(2) == 0 ?
                            RGB.getARGBInt(0, 0, 0) : RGB.getARGBInt(255, 255, 255);
                    for (int p = 0; p < pixelSkip; p++) {
                        service.burnerBoard.setPixel(pixelSkip * x + p, y, color);
                    }
                    break;
                case kMatrixMezcal:
                    if ((x == diagonal) || ((service.burnerBoard.boardWidth - x) == diagonal)) {
                        service.burnerBoard.setPixel(pixelSkip * x, y, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 1, y, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 2, y, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x, y - 1, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 1, y - 1, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 2, y - 1, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x, y - 2, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 1, y - 2, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 2, y - 2, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x, y - 3, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 1, y - 3, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 2, y - 3, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x, y - 4, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 1, y - 4, mexcal_color);
                        service.burnerBoard.setPixel(pixelSkip * x - 2, y - 4, mexcal_color);
                    } else {
                        service.burnerBoard.setPixel(pixelSkip * x, y, RGB.getARGBInt(0, 0, 0));
                    }

                default:
            }

            fireColor += 1;
            if (fireColor > 180) {
                fireColor = 40;
            }
        }

        diagonal += 3;
        if (diagonal >= service.burnerBoard.boardWidth / 2) {
            diagonal = 5;
            mexcal_color = mWheel.wheelState();
            mWheel.wheelInc(60);
        }

        for (int i = 0; i < multiplier4Speed; i++) {
            service.burnerBoard.scrollPixels(true);
        }

        switch (mode) {
            case kMatrixSync:
                int syncColor =
                        mWheel.wheel((int) (TimeSync.GetCurrentClock() / 5) % 360);
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
