package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by rmc on 6/18/18.
 */

public class TestColors extends Visualization {
    int testColor = 0;
    private int[][] mFireColors = FireColors.colors();

    public TestColors(BurnerBoard bb, BoardVisualization visualization) {
        super(bb, visualization);
    }

    public void update(int mode) {

        System.out.println("Firecolor " + mFireColors[testColor][0] + "," + mFireColors[testColor][1] + "," + mFireColors[testColor][2]);

        mBurnerBoard.fillScreen(
                mFireColors[testColor][0],
                mFireColors[testColor][1],
                mFireColors[testColor][2]);
        mBurnerBoard.flush();
        testColor++;
        if (testColor > 255) {
            testColor = 0;
        }

    }

    int testColorState = 0;

    private void modeTestColors() {
        switch (testColorState) {
            case 0:
                mBurnerBoard.fillScreen(128, 0, 0);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flush();
                testColorState++;
                break;
            case 1:
                mBurnerBoard.fillScreen(0, 128, 0);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flush();
                testColorState++;
                break;
            case 2:
                mBurnerBoard.fillScreen(0, 0, 128);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flush();
                testColorState++;
                break;

            case 3:
                mBurnerBoard.fillScreen(64, 64, 64);
                mBurnerBoard.setOtherlightsAutomatically();
                mBurnerBoard.flush();
                testColorState++;
                break;

            default:
                testColorState = 0;
                break;
        }

    }
}
