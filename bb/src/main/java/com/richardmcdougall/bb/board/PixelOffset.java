package com.richardmcdougall.bb.board;

import com.richardmcdougall.bbcommon.BLog;


public class PixelOffset {
    // Max board pixel size limited by the following: need to make dynamic, or adjustable.
    private int[][][] pixel2OffsetTable = new int[768][768][3];

    private String TAG = this.getClass().getSimpleName();

    private BurnerBoard board = null;

    // Convert from xy to buffer memory
    int pixel2OffsetCalc(int x, int y, int rgb) {
        return (y * this.board.boardWidth + x) * 3 + rgb;
    }

    public PixelOffset(BurnerBoard board) {
        BLog.d(TAG, "width: " + board.boardWidth + " height: " + board.boardHeight);
        this.board = board;
        this.initPixelOffset();
    }

    private void initPixelOffset() {
        for (int x = 0; x < this.board.boardWidth; x++) {
            for (int y = 0; y < this.board.boardHeight; y++) {
                for (int rgb = 0; rgb < 3; rgb++) {
                    pixel2OffsetTable[x][y][rgb] = pixel2OffsetCalc(x, y, rgb);
                }
            }
        }
    }

    public int Map(int x, int y, int rgb) {
        return pixel2OffsetTable[x][y][rgb];
    }
}
