package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;

/**
 * Created by rmc on 6/18/18.
 */

public class TestColors extends Visualization {
    int testColor = 0;
    private int[][] mFireColors = FireColors.colors();

    public TestColors(BBService service) {
        super(service);
    }

    public void update(int mode) {

        System.out.println("Firecolor " + mFireColors[testColor][0] + "," + mFireColors[testColor][1] + "," + mFireColors[testColor][2]);

        service.burnerBoard.fillScreen(
                mFireColors[testColor][0],
                mFireColors[testColor][1],
                mFireColors[testColor][2]);
        service.burnerBoard.flush();
        testColor++;
        if (testColor > 255) {
            testColor = 0;
        }

    }

    int testColorState = 0;

    private void modeTestColors() {
        switch (testColorState) {
            case 0:
                service.burnerBoard.fillScreen(128, 0, 0);
                service.burnerBoard.setOtherlightsAutomatically();
                service.burnerBoard.flush();
                testColorState++;
                break;
            case 1:
                service.burnerBoard.fillScreen(0, 128, 0);
                service.burnerBoard.setOtherlightsAutomatically();
                service.burnerBoard.flush();
                testColorState++;
                break;
            case 2:
                service.burnerBoard.fillScreen(0, 0, 128);
                service.burnerBoard.setOtherlightsAutomatically();
                service.burnerBoard.flush();
                testColorState++;
                break;

            case 3:
                service.burnerBoard.fillScreen(64, 64, 64);
                service.burnerBoard.setOtherlightsAutomatically();
                service.burnerBoard.flush();
                testColorState++;
                break;

            default:
                testColorState = 0;
                break;
        }

    }
}
