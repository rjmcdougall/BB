package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by rmc on 6/21/18.
 */

public class Meteor extends Visualization {

    private Wheel mWheel = new Wheel();

    public Meteor(BurnerBoard bb, BoardVisualization visualization) {
        super(bb, visualization);
    }

    // Woodson's pool meteor
    private final static int kMeteorLeds = 55;
    private final static int kMeteorIncrementor = 4;
    private final static int kMeteorSize = 10;
    private final static int kFadeAmount = 25;
    private final static int kShiftLimit = 255; // Must be less than kRedStart
    private final static int kRedStart = 255;
    private final static int kGreenStart = 215;
    private final static int kBlueStart = 0;

    private int meteorColorIter = 0;
    private int meteorPixelIter = 0;

    public void update(int mode) {

        int meteorRedShift = kRedStart - meteorColorIter;
        int meteorBlueShift = kBlueStart;
        int meteorGreenShift = kGreenStart;

        // fade brightness all LEDs one step
        mBurnerBoard.fadePixels(kFadeAmount);

        // draw meteor
        for(int j = 0; j < kMeteorSize; j++) {
            if(((meteorPixelIter - j) < kMeteorLeds) && ((meteorPixelIter - j) >= 0)) {
                mBurnerBoard.setPixel(16, meteorPixelIter-j,
                        meteorRedShift, meteorGreenShift, meteorBlueShift);
            }
        }

        meteorPixelIter += kMeteorIncrementor;
        if (meteorPixelIter > (kMeteorLeds * 2)) {
            meteorPixelIter = 0;
            meteorColorIter++;
            meteorColorIter++;
            if (meteorColorIter > kShiftLimit) {
                meteorColorIter = 0;
            }
        }

        mBurnerBoard.flush();

        return;
    }

}
