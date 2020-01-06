package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by rmc on 6/21/18.
 */

public class Fire extends Visualization {

    public static final int kModeFireNormal = 1;
    public static final int kModeFireDistrikt = 2;
    public static final int kModeFireTheMan = 3;

    private int mFireHeight;
    private int[] mFireScreen;
    private int[][] mFireColors = FireColors.colors();

    public Fire(BurnerBoard bb, BoardVisualization visualization) {
        super(bb, visualization);

        mFireHeight = mBoardHeight;
        mFireScreen = new int[mBoardWidth * 2 * (mFireHeight + 6)];
    }

    private void getFirePixels(int color) {

    }

    private final int[] kFireColorsOcto = {
            BurnerBoard.getRGB(0, 0, 0),
            BurnerBoard.getRGB(0, 0, 0),
            BurnerBoard.getRGB(1, 0, 0),
            BurnerBoard.getRGB(2, 0, 0),
            BurnerBoard.getRGB(3, 0, 0),
            BurnerBoard.getRGB(4, 0, 0),
            BurnerBoard.getRGB(6, 0, 0),
            BurnerBoard.getRGB(7, 0, 0),
            BurnerBoard.getRGB(9, 0, 0),
            BurnerBoard.getRGB(10, 0, 0),
            BurnerBoard.getRGB(12, 0, 0),
            BurnerBoard.getRGB(14, 0, 0),
            BurnerBoard.getRGB(17, 0, 0),
            BurnerBoard.getRGB(19, 0, 0),
            BurnerBoard.getRGB(21, 0, 0),
            BurnerBoard.getRGB(24, 0, 0),
            BurnerBoard.getRGB(26, 0, 0),
            BurnerBoard.getRGB(29, 0, 0),
            BurnerBoard.getRGB(32, 0, 0),
            BurnerBoard.getRGB(35, 0, 0),
            BurnerBoard.getRGB(38, 0, 0),
            BurnerBoard.getRGB(42, 0, 0),
            BurnerBoard.getRGB(45, 0, 0),
            BurnerBoard.getRGB(48, 0, 0),
            BurnerBoard.getRGB(52, 0, 0),
            BurnerBoard.getRGB(56, 0, 0),
            BurnerBoard.getRGB(59, 0, 0),
            BurnerBoard.getRGB(63, 0, 0),
            BurnerBoard.getRGB(67, 0, 0),
            BurnerBoard.getRGB(72, 0, 0),
            BurnerBoard.getRGB(76, 0, 0),
            BurnerBoard.getRGB(80, 0, 0),
            BurnerBoard.getRGB(84, 0, 0),
            BurnerBoard.getRGB(89, 0, 0),
            BurnerBoard.getRGB(93, 0, 0),
            BurnerBoard.getRGB(98, 0, 0),
            BurnerBoard.getRGB(102, 0, 0),
            BurnerBoard.getRGB(107, 0, 0),
            BurnerBoard.getRGB(112, 0, 0),
            BurnerBoard.getRGB(117, 0, 0),
            BurnerBoard.getRGB(122, 0, 0),
            BurnerBoard.getRGB(127, 0, 0),
            BurnerBoard.getRGB(132, 0, 0),
            BurnerBoard.getRGB(137, 0, 0),
            BurnerBoard.getRGB(142, 0, 0),
            BurnerBoard.getRGB(148, 0, 0),
            BurnerBoard.getRGB(155, 0, 0),
            BurnerBoard.getRGB(160, 0, 0),
            BurnerBoard.getRGB(166, 0, 0),
            BurnerBoard.getRGB(172, 0, 0),
            BurnerBoard.getRGB(176, 0, 0),
            BurnerBoard.getRGB(179, 0, 0),
            BurnerBoard.getRGB(182, 1, 0),
            BurnerBoard.getRGB(185, 2, 0),
            BurnerBoard.getRGB(188, 3, 0),
            BurnerBoard.getRGB(191, 4, 0),
            BurnerBoard.getRGB(194, 5, 0),
            BurnerBoard.getRGB(197, 7, 0),
            BurnerBoard.getRGB(200, 9, 0),
            BurnerBoard.getRGB(203, 10, 0),
            BurnerBoard.getRGB(206, 12, 0),
            BurnerBoard.getRGB(209, 14, 0),
            BurnerBoard.getRGB(212, 17, 0),
            BurnerBoard.getRGB(216, 19, 0),
            BurnerBoard.getRGB(219, 21, 0),
            BurnerBoard.getRGB(222, 24, 0),
            BurnerBoard.getRGB(225, 26, 0),
            BurnerBoard.getRGB(228, 29, 0),
            BurnerBoard.getRGB(232, 32, 0),
            BurnerBoard.getRGB(235, 35, 0),
            BurnerBoard.getRGB(238, 38, 0),
            BurnerBoard.getRGB(243, 42, 0),
            BurnerBoard.getRGB(247, 45, 0),
            BurnerBoard.getRGB(250, 48, 0),
            BurnerBoard.getRGB(253, 52, 0),
            BurnerBoard.getRGB(255, 56, 0),
            BurnerBoard.getRGB(255, 62, 0),
            BurnerBoard.getRGB(255, 68, 0),
            BurnerBoard.getRGB(255, 74, 1),
            BurnerBoard.getRGB(255, 81, 1),
            BurnerBoard.getRGB(255, 88, 1),
            BurnerBoard.getRGB(255, 94, 2),
            BurnerBoard.getRGB(255, 101, 2),
            BurnerBoard.getRGB(255, 108, 3),
            BurnerBoard.getRGB(255, 115, 3),
            BurnerBoard.getRGB(255, 123, 4),
            BurnerBoard.getRGB(255, 131, 4),
            BurnerBoard.getRGB(255, 140, 5),
            BurnerBoard.getRGB(255, 148, 6),
            BurnerBoard.getRGB(255, 156, 6),
            BurnerBoard.getRGB(255, 164, 7),
            BurnerBoard.getRGB(255, 173, 8),
            BurnerBoard.getRGB(255, 182, 9),
            BurnerBoard.getRGB(255, 191, 10),
            BurnerBoard.getRGB(255, 200, 10),
            BurnerBoard.getRGB(255, 209, 11),
            BurnerBoard.getRGB(255, 220, 13),
            BurnerBoard.getRGB(255, 230, 14),
            BurnerBoard.getRGB(255, 240, 15),
            BurnerBoard.getRGB(255, 250, 16)
    };

    /*
    * From PixelController Fire.java
    * Originally modeled from http://lodev.org/cgtutor/fire.html
    */

    int fireCnt = 0;

    public void update(int mode) {
        int j = mBoardWidth * 2 * (mFireHeight + 1);
        int random;
        for (int i = 0; i < (mBoardWidth * 2); i++) {
            random = mBoardVisualizion.mRandom.nextInt(16);
            // the lower the value, the intense the fire,
            // compensate a lower value with a higher decay value
            // BB Classuc us 8
            if (random > 9) {
				/*maximum heat*/
                mFireScreen[j + i] = 1023;
            } else {
                mFireScreen[j + i] = 0;
            }
        }

		/* move fire upwards, start at bottom*/
        int temp;
        int[] lastTemps = new int[mBoardWidth * 2];
        for (int index = 0; index < mFireHeight + 1; index++) {
            for (int i = 0; i < mBoardWidth * 2; i++) {
                if (i == 0) {
					/* at the left border*/
                    temp = mFireScreen[j];
                    temp += mFireScreen[j + 1];
                    temp += mFireScreen[j - mBoardWidth * 2];
                    temp /= 3;
                } else if (i == mBoardWidth * 2) {
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
                    temp--;
                    //temp *= 2;
                    //temp /= 3;
                }

                int dofs = j - mBoardWidth * 2 + i;
                mFireScreen[dofs] = temp;
                if (dofs < (mBoardWidth * 2 * mFireHeight)) {
                    int x = dofs % (mBoardWidth * 2);
                    int y = java.lang.Math.min(java.lang.Math.max(0,
                            (dofs / (mBoardWidth * 2)) + 1), mBoardHeight - 1);
                    // Bigger number leaves more flame tails
                    // BB Classic was 55
                    if (true || lastTemps[i] > 50) {
                        //mBurnerBoard.setPixel(x / 2, y * 2,
                        //int templ0 = (int)((float)temp * (float)150 / (float)255);
                        //temp100 = java.lang.Math.min(99, temp100);
                        //mBurnerBoard.setPixel(x / 2, y, kFireColorsOcto[temp100]);
                        mBurnerBoard.setPixel(x / 2, y,
                                mFireColors[temp / 4][0],
                                mFireColors[temp / 4][1],
                                mFireColors[temp / 4][2]);

                        //mBurnerBoard.setPixel(x / 2, y * 2 + 1,
                        //mFireColors[temp][0],
                        //mFireColors[temp][1],
                        //mFireColors[temp][2]);
                    } else {
                        //mBurnerBoard.setPixel(x / 2, y, 0, 255,  0);
                    }
                    lastTemps[i] = temp;
                }
            }
            j -= mBoardWidth * 2;
        }
        //for (j = 1; j < mBoardHeight - 1; j++) {
        //    System.arraycopy(mFireScreen, j * mBoardWidth * 2,
        //            mFireScreen, (j + 1) * mBoardWidth * 2, mBoardWidth * 2);
        //}

        switch (mode) {
            case kModeFireDistrikt:
                mBurnerBoard.scrollPixelsExcept(true, BurnerBoard.getRGB(255,255, 255));
                Distrikt.drawDistrikt(mBurnerBoard, BurnerBoard.getRGB(255, 255, 255), mBoardWidth);
                break;
            case kModeFireTheMan:
                TheMan.drawTheMan(mBurnerBoard, BurnerBoard.getRGB(mFireColors[200][0],
                        mFireColors[100][1], mFireColors[200][2]), mBoardWidth);
                mBurnerBoard.scrollPixelsExcept(true,
                        BurnerBoard.getRGB(mFireColors[200][0],
                                mFireColors[100][1], mFireColors[200][2]));
                break;
            default:
                mBurnerBoard.scrollPixels(true);
                break;

        }
        mBurnerBoard.setOtherlightsAutomatically();
        fireCnt++;
        if (fireCnt % 3 == 0) {
            mBurnerBoard.flush();
        }
    }
}
