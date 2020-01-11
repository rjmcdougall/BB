package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;
import com.richardmcdougall.bb.TimeSync;

public class SyncLights extends Visualization {
    private Wheel mWheel = new Wheel();

    public SyncLights(BurnerBoard bb, BoardVisualization visualization) {
        super(bb, visualization);
    }

    public void update(int mode) {
        int color = mWheel.wheel((int) TimeSync.GetCurrentClock() % 256);
        mBurnerBoard.fillScreen(color);
    }

}