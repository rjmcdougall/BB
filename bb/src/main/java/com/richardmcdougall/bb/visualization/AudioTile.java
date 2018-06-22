package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.BoardVisualization;
import com.richardmcdougall.bb.BurnerBoard;

/**
 * Created by rmc on 6/21/18.
 */

public class AudioTile extends Visualization {

    private Wheel mWheel = new Wheel();

    public AudioTile(BurnerBoard bb, BoardVisualization visualization) {
        super(bb, visualization);

    }

    public void update(int mode) {

        int mTileHeight = mBoardWidth > 10 ? mBoardWidth  / 3 :  mBoardWidth;
        final int tiles = 2 * mBoardHeight / mTileHeight + 1;

        //int [] dbLevels = getLevels();
        mBurnerBoard.fadePixels(5);
        //if (dbLevels == null)
        //    return;
        //dbLevels = getLevels();
        //int level = java.lang.Math.max(0, (dbLevels[5] / 5 - 8));
        //if (dbLevels == null)
        //    return;
        if (mBoardVisualizion.getLevel() > 110) {
            for (int tile = 0; tile < tiles; tile++) {
                int c = mWheel.wheelState();
                //drawRectTile(tile, 255 * mRandom.nextInt(65536));
                drawRectTile(tile, mWheel.wheel(mBoardVisualizion.mRandom.nextInt(255)));
                //drawRectTile(tile, wheel((wheel_color)));
                mWheel.wheelInc(59);
            }
        }
        //mBurnerBoard.fadePixels(1);
        mBurnerBoard.setOtherlightsAutomatically();
        mBurnerBoard.flush();
        return;
    }

    private void drawRectTile(int tileNo, int color) {
        int x1 = 0;
        int y1 = 0;
        int mTileHeight = mBoardWidth > 10 ? mBoardWidth  / 3 :  mBoardWidth;
        final int tiles = mBoardHeight / mTileHeight + 1;
        if (tileNo >= tiles * 2)
            return;
        if (tileNo < tiles) {
            x1 = 0;
            y1 = tileNo * mTileHeight;
        } else {
            x1 = mBoardWidth / 2;
            y1 = (tileNo - tiles) * mTileHeight;
        }
        int x2 = x1 + mBoardWidth / 2 - 1;
        int y2 = y1 + mTileHeight - 1;
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (x == x1 || x == x2 || y == y1 || y == y2) {
                    mBurnerBoard.setPixel(x,
                            java.lang.Math.min(y, mBoardHeight - 1),
                            BurnerBoard.colorDim(100, color));
                } else {
                    mBurnerBoard.setPixel(x,
                            java.lang.Math.min(y, mBoardHeight - 1), color);
                }
            }
        }
    }
}
