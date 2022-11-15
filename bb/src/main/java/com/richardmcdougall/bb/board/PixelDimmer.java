package com.richardmcdougall.bb.board;

import com.richardmcdougall.bbcommon.BLog;

public class PixelDimmer {

    private int powerHistory[] = new int[10];

    public int[] Dim(int subtract, int[] boardScreen){

        int timeslot = (int) ((System.currentTimeMillis() / 1000 ) % powerHistory.length);
        BLog.i("BB.PixelDimmer", "ts = " + timeslot);

        //public static int[] Dim(int subtract, int[] boardScreen){
        // Here we calculate the total power percentage of the whole board
        // We want to limit the board to no more than 50% of pixel output total
        // This is because the board is setup to flip the breaker at 200 watts
        // Output is percentage multiplier for the LEDs
        // At full brightness we limit to 30% of their output
        // Power is on-linear to pixel brightness: 37% = 50% power.
        // powerPercent = 100: 15% multiplier
        // powerPercent <= 15: 100% multiplier
        int totalBrightnessSum = 0;
        int powerLimitMultiplierPercent = 100;
        for (int pixel = 0; pixel < boardScreen.length; pixel++) {
            // R
            if (pixel % 3 == 0) {
                totalBrightnessSum += boardScreen[pixel];
            } else if (pixel % 3 == 1) {
                totalBrightnessSum += boardScreen[pixel];
            } else {
                totalBrightnessSum += boardScreen[pixel] / 2;
            }
        }

        final int powerPercentTmp = totalBrightnessSum / boardScreen.length * 100 / 255;

        powerHistory[timeslot] = powerPercentTmp;

        int powerPercent = 0;
        for (int i = 0; i < powerHistory.length; i++) {
            powerPercent = powerPercent + powerHistory[i];
        }
        powerPercent = powerPercent / powerHistory.length;

        powerLimitMultiplierPercent = (int) (100 - Math.min(90, Math.max((powerPercent * .5) - subtract, 0)));
     //   BLog.i("BB.PixelDimmer", "pt/pp = " + powerPercentTmp + "/" + powerPercent + ", plmp = " + powerLimitMultiplierPercent);


        for (int pixel = 0; pixel < boardScreen.length; pixel++) {
            boardScreen[pixel] =  boardScreen[pixel] * powerLimitMultiplierPercent / 100;
        }

        return boardScreen;
    }

}
