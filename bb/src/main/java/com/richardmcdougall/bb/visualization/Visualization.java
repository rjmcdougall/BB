package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by rmc on 6/18/18.
 */

public class Visualization {

   public int mBoardWidth;
    public int mBoardHeight;
    public static final int kDefault = 0;
    BBService service = null;

    public Visualization(BBService service) {
        this.service = service;
        mBoardWidth = service.burnerBoard.getWidth();
        mBoardHeight = service.burnerBoard.getHeight();
    }

    public void update(int mode) {
    }

    public void setMode(int mode) {
    }

}