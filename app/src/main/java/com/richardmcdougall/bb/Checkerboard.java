package com.richardmcdougall.bb;

/**
 * Created by d.wilson on 8/6/17.
 */

public class Checkerboard {

    static int flipColorCounter = 0;
    static int displayColor1 = BurnerBoard.getRGB(0, 0, 0);
    static int displayColor2 = BurnerBoard.getRGB(255, 255, 255);

    private BurnerBoard mBurnerBoard = null;

    private int mBoardWidth = 0;
    private int mBoardHeight = 0;

    Checkerboard(BurnerBoard board, int boardHeight, int boardWidth){

        mBoardHeight = boardHeight;
        mBoardWidth = boardWidth;
        mBurnerBoard = board;

    }
    void modeCheckerboard() {

        // checkerboard pattern.
        // 46 Wide we will have 3 each for the left and right ends and all others will be 5 wide.


        int x;
        int y;
        int colorSwap;

        y = mBoardHeight- 1;
        int[] pixels = new int[mBoardWidth];

        // flop the colors
        if(flipColorCounter>=5){
            int tempDisplayColor = displayColor2;
            displayColor2 = displayColor1;
            displayColor1 = tempDisplayColor;
            flipColorCounter = 0;
        }
        else
            flipColorCounter++;

        for (x = -2; x < mBoardWidth+5; x++){ // start off of the edge of the board. offset by 2 to center the checkerboard

            colorSwap = ((x+2) / 5) % 2;

            if (colorSwap == 0) {
                if(x >=0 && x < mBoardWidth) // prevent out of range exceptions and allow partial rendering.
                    mBurnerBoard.setPixel(x, y, displayColor1);
            } else if (colorSwap == 1) {
                if(x >=0 && x < mBoardWidth) // prevent out of range exceptions and allow partial rendering.
                    mBurnerBoard.setPixel(x, y, displayColor2);
            }
        }

        mBurnerBoard.scrollPixels(true);
        mBurnerBoard.flush();

        return;

    }
}
