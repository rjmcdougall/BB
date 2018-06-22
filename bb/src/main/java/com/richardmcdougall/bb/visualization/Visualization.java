package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by rmc on 6/18/18.
 */

public class Visualization {

    BurnerBoard mBurnerBoard;
    public int mBoardWidth;
    public int mBoardHeight;
    BoardVisualization mBoardVisualizion;
    public static final int kDefault = 0;


    public Visualization(BurnerBoard bb, BoardVisualization visualization) {
        mBurnerBoard = bb;
        mBoardWidth = bb.getWidth();
        mBoardHeight = bb.getHeight();
        mBoardVisualizion = visualization;
    }

    public void update(int mode) {
    }

    public void setMode(int mode) {
    }

}