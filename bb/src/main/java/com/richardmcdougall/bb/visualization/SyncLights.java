package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

public class SyncLights extends Visualization {
    private Wheel mWheel = new Wheel();
    BurnerBoard mBurnerBoard = null;

    public SyncLights(BurnerBoard bb, BoardVisualization visualization) {
        super(bb, visualization);
        mBurnerBoard = bb;
    }

    public void update(int mode) {
        int color = mWheel.wheel((int)mBurnerBoard.getCurrentClock() % 256);
        mBurnerBoard.fillScreen(color);
    }

}