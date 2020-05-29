package com.richardmcdougall.bb.board;

public class PixelOffset {
    // Max board pixel size limited by the following: need to make dynamic, or adjustable.
    private int[][][] pixel2OffsetTable = new int[512][512][3];
    private BurnerBoard board = null;

    // Convert from xy to buffer memory
    static int pixel2OffsetCalc(int boardWidth, int x, int y, int rgb) {
        return (y * boardWidth + x) * 3 + rgb;
    }

    public PixelOffset(BurnerBoard board){
        this.board = board;
        this.initPixelOffset();
    }

    private void initPixelOffset() {
        for (int x = 0; x < this.board.boardWidth; x++) {
            for (int y = 0; y < this.board.boardHeight; y++) {
                for (int rgb = 0; rgb < 3; rgb++) {
                    pixel2OffsetTable[x][y][rgb] = pixel2OffsetCalc(this.board.boardWidth, x, y, rgb);
                }
            }
        }
    }

    public int Map(int x, int y, int rgb) {
        return pixel2OffsetTable[x][y][rgb];
    }
}
