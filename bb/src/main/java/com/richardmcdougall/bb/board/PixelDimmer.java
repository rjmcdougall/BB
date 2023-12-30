package com.richardmcdougall.bb.board;

import com.richardmcdougall.bbcommon.BLog;

public class PixelDimmer {

    private int powerHistory[] = new int[10];

    public int[] Dim(int maxPowerPct, int[] boardScreen){

        int timeslot = (int) ((System.currentTimeMillis() / 1000 ) % powerHistory.length);
  //      BLog.i("BB.PixelDimmer", "ts = " + timeslot);

        //public static int[] Dim(int subtract, int[] boardScreen){
        // Here we calculate the total power percentage of the whole board
        // Each board hardware has a different max power setting due to the number of LEDS
        // compared to the output of the regulator.
        // v4 has 13,000 LEDs, could use 1200W but regulator is only 400w.
        // Output is percentage multiplier for the LEDs
        // TODO: it might be better for contrast to limit power by selectively switching off pixels
        // instead of dimming every pixel.
        int totalBrightnessSum = 0;
        int powerLimitMultiplierPercent = 100;
        for (int pixel = 0; pixel < boardScreen.length; pixel++) {
            // R is 30% more power
            if (pixel % 3 == 0) {
                totalBrightnessSum += (13 * boardScreen[pixel]) / 10;
            } else if (pixel % 3 == 1) {
                totalBrightnessSum += boardScreen[pixel];
            } else {
                totalBrightnessSum += boardScreen[pixel];
            }
        }

        final int powerPercentNow = totalBrightnessSum / boardScreen.length * 100 / 255;

        powerHistory[timeslot] = powerPercentNow;

        // Average over last n samples
        int powerPercent = 0;
        for (int i = 0; i < powerHistory.length; i++) {
            powerPercent = powerPercent + powerHistory[i];
        }
        powerPercent = powerPercent / powerHistory.length;

        if (powerPercent < maxPowerPct) {
            powerLimitMultiplierPercent = 100;
        } else {
            powerLimitMultiplierPercent = (100 * maxPowerPct) / powerPercent;
        }
        BLog.i("BB.PixelDimmer", "pt/pp = " + powerPercentNow + "/" + powerPercent + ", plmp = " + powerLimitMultiplierPercent);


        for (int pixel = 0; pixel < boardScreen.length; pixel++) {
            boardScreen[pixel] =  boardScreen[pixel] * powerLimitMultiplierPercent / 100;
        }

        return boardScreen;
    }

}
