package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.board.BurnerBoard;
import com.richardmcdougall.bb.TimeSync;

/**
 * Created by rmc on 6/21/18.
 */

public class Matrix extends Visualization {

    public Matrix(BBService service) {
        super(service);
    }

    //TODO: Make board class specific
    private int mBoardSideLights = 79;

    private Wheel mWheel = new Wheel();
    private int[][] mFireColors = FireColors.colors();

    public static final int kMatrixBurnerColor = 1;
    public static final int kMatrixFire = 2;
    public static final int kMatrixLunarian = 3;
    public static final int kMatrixGoogle = 4;
    public static final int kMatrixEsperanto = 5;
    public static final int kMatrixIrukandji = 6;
    public static final int kMatrixFireFull = 7;
    public static final int kMatrixReverse = 8;
    public static final int kMatrixMickey = 9;
    public static final int kMatrixSync = 10;

    private static final int[] googleColors = {
            BurnerBoard.getRGB(60, 186, 84),
            BurnerBoard.getRGB(244, 194, 13),
            BurnerBoard.getRGB(219, 50, 54),
            BurnerBoard.getRGB(72, 133, 237),
            BurnerBoard.getRGB(255, 255, 255)
    };

    private static final int[] googleColorsClassicBoard = {
            BurnerBoard.getRGB(40, 200, 60),
            BurnerBoard.getRGB(200, 194, 13),
            BurnerBoard.getRGB(255, 10, 10),
            BurnerBoard.getRGB(20, 50, 237),
            BurnerBoard.getRGB(255, 255, 255)
    };

    private int fireColor = 40;
    private int googleColor = 0;

    private int colorState = 0;

    public void update(int mode) {

        // Row plus two pixels for side lights
        int[] pixels = new int[service.burnerBoard.boardWidth * 3 + 6];

        int color;
        int colorDim;
        int x;
        int y;
        int sideLight;
        int pixelSkip;
        boolean isReverse = (mode == kMatrixReverse) || (mode ==kMatrixMickey);

        if (service.burnerBoard.boardWidth > 10) {
            pixelSkip = 3;
        } else {
            pixelSkip = 1;
        }
        pixelSkip = 1;

        if (isReverse) {
            y = 0;
        } else {
            y = service.burnerBoard.boardHeight - 1;
        }
        sideLight = mBoardSideLights - 1;

        for (x = 0; x < service.burnerBoard.boardWidth / pixelSkip; x++) {
            //Chance of 1/3rd
            switch (mode) {
                case kMatrixBurnerColor:
                case kMatrixReverse:
                case kMatrixSync:

                    if (service.boardVisualization.mRandom.nextInt(3) != 0) {
                        color = BurnerBoard.getRGB(0, 0, 0);
                    } else {
                        color = mWheel.wheelState();
                        mWheel.wheelInc(1);
                    }
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);
                    break;

                case kMatrixMickey:

                    if (service.boardVisualization.mRandom.nextInt(3) != 0) {
                        color = BurnerBoard.getRGB(0, 0, 0);
                    } else {
                        if (colorState < 10) {
                            color = BurnerBoard.getRGB(255, 147, 41);
                            colorState++;
                        } else {
                            color = BurnerBoard.getRGB(0, 0, 255);
                            colorState++;
                        }
                        if (colorState > 20) {
                            colorState = 0;
                        }
                    }
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);
                    break;

                case kMatrixEsperanto:

                case kMatrixLunarian:
                    color = service.boardVisualization.mRandom.nextInt(2) == 0 ?
                            BurnerBoard.getRGB(0, 0, 0) : BurnerBoard.getRGB(255, 255, 255);
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);
                    //service.burnerBoard.setPixel(pixelSkip * x + 1, y, color);
                    //service.burnerBoard.setPixel(pixelSkip * x, y - 1, color);
                    //service.burnerBoard.setPixel(pixelSkip * x + 1, y - 1, color);
                    break;
                case kMatrixFire:
                    color = service.boardVisualization.mRandom.nextInt(2) == 0 ?
                            BurnerBoard.getRGB(0, 0, 0) :
                            BurnerBoard.getRGB(
                                    mFireColors[fireColor][0],
                                    mFireColors[fireColor][1],
                                    mFireColors[fireColor][2]);
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);
                    break;
                case kMatrixGoogle:
                    color = service.boardVisualization.mRandom.nextInt(2) == 0 ?
                            BurnerBoard.getRGB(0, 0, 0) : googleColors[googleColor / 8];
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);
                    //service.burnerBoard.setPixel(pixelSkip * x + 1, y, color);
                    //mBurnerBoard.setPixel(pixelSkip * x, y - 1, color);
                    //mBurnerBoard.setPixel(pixelSkip * x + 1, y - 1, color);
                    break;
                case kMatrixIrukandji:
                    color = mWheel.wheelState();
                    if (x > 0 || x < service.burnerBoard.boardWidth) {
                        service.burnerBoard.setPixel(pixelSkip * x, y, BurnerBoard.getRGB(0, 0, 0));
                    }
                    mWheel.wheelInc(1);
                    break;
                case kMatrixFireFull:
                    color = BurnerBoard.getRGB(
                            mFireColors[fireColor][0],
                            mFireColors[fireColor][1],
                            mFireColors[fireColor][2]);
                    service.burnerBoard.setPixel(pixelSkip * x, y, color);
                    break;
                default:
                    color = 0;
            }

            fireColor += 1;
            if (fireColor > 180) {
                fireColor = 40;
            }

        }
        googleColor++;
        if (googleColor >= googleColors.length * 8) {
            googleColor = 0;
        }

        for (int i = 0; i < service.boardVisualization.mMultipler4Speed; i++) {
            service.burnerBoard.scrollPixels(!isReverse);
        }

        switch (mode) {
            case kMatrixEsperanto:
                int level = java.lang.Math.max(0, (service.boardVisualization.getLevel() * 3) - 80);
                service.burnerBoard.dimPixels(level);
                break;

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
