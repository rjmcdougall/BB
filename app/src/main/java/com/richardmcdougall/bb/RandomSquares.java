package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.Visualizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Random;

/**
 * Created by d.wilson on 8/6/17.
 */

public class RandomSquares {

    private static int sfColorPointer=0;
    private static int USE_SOLID_SQUARE=1;
    private static int USE_HOLLOW_SQUARE=2;
    private static int USE_MIXTURE_SQUARE=3;
    private Random mRandom = new Random();
    BurnerBoard mBurnerBoard = null;

    private int mBoardWidth = 0;
    private int mBoardHeight = 0;

    private static final int[] sfColors = {
            BurnerBoard.getRGB(0, 161, 224), // light blue
            BurnerBoard.getRGB(255, 255, 255), // white
            BurnerBoard.getRGB(220, 115, 10), // orange (marketing)
            BurnerBoard.getRGB(125, 30, 171), // purple
            BurnerBoard.getRGB(80, 176, 50), // green
            BurnerBoard.getRGB(235, 180, 40), // yellow
            BurnerBoard.getRGB(20,155,155), // analytics blue
            BurnerBoard.getRGB(53,120,214), // apps blue

    };

    RandomSquares(BurnerBoard board, int boardHeight, int boardWidth) {

        mBoardHeight = boardHeight;
        mBoardWidth = boardWidth;
        mBurnerBoard = board;

    }

    // This will create random placement of colored squares across the entire surface of the board.
    void modeRandomSquares() {

        // 1 is solid, 2 is hollow, 3 is a mixture.
        int squareMode = USE_HOLLOW_SQUARE;

        //set line thickness for hollow squares
        int thick = 1;

        // pick a random amount to draw in 1 flush
        int count = mRandom.nextInt(5);
        for(int i=0;i<count;i++){
            drawRandomSquare(squareMode, thick);
        }

        //Richard wtf is this stuff?
        mBurnerBoard.setOtherlightsAutomatically();
        mBurnerBoard.flush();
    }

    // Draw a single square
    void drawRandomSquare(int squareMode, int thick){

        int x;
        int row;
        int lengthOfSquare = mRandom.nextInt(20);
        int xPos = mRandom.nextInt(mBoardWidth ) - lengthOfSquare / 2; // allow placement of partial visuals
        int yPos = mRandom.nextInt(mBoardHeight  ) - lengthOfSquare / 2; // allow placement of partial visuals

        // loop back when you get to the end of the color list.  Random was nasty looking.
        if(sfColorPointer >= sfColors.length) { // restart at the beginning.
            sfColorPointer = 0;
        }
        int color = sfColors[sfColorPointer];

        if(squareMode==USE_SOLID_SQUARE) {
            DrawSolidSquare(lengthOfSquare, xPos, yPos, color);
        }
        else if (squareMode==USE_HOLLOW_SQUARE) {
            DrawHollowSquare(lengthOfSquare, xPos, yPos, color,thick);
        }
        else if (squareMode==USE_MIXTURE_SQUARE) {
            if(mRandom.nextBoolean())
                DrawSolidSquare(lengthOfSquare, xPos, yPos, color);
            else
                DrawHollowSquare(lengthOfSquare, xPos, yPos, color,thick);
        }

        sfColorPointer++;

    }

    // Draw a single solid square.
    private void DrawSolidSquare(int lengthOfSquare, int xPos, int yPos, int color){

        for (int row = 0; row < lengthOfSquare ; row++) {
            for (int x = 0; x < lengthOfSquare ; x++) {

                // prevent out of range exceptions and allow partial rendering.
                if(x+xPos >=0 && x+xPos < mBoardWidth && row + yPos >=0 && row + yPos < mBoardHeight){
                    mBurnerBoard.setPixel(x + xPos, row + yPos, color);
                }
            }
        }
    }

    // Draw a single Hollow Square
    void DrawHollowSquare(int lengthOfSquare, int xPos, int yPos, int color, int lineThickness){
        int x;
        int row;

        // draw the top line.
        row = 0;
        for (x = 0; x < lengthOfSquare ; x++) {
            if(x+xPos >=0 && x+xPos < mBoardWidth && row + yPos >=0 && row + yPos < mBoardHeight){
                for(int thick=0;thick<lineThickness;thick++){
                    mBurnerBoard.setPixel(x + xPos, row + thick + yPos, color);
                }
            }
        }
        // now at the top-right, turn downward to the lower right.
        for (row = 0; row < lengthOfSquare-1 ; row++) {
            // prevent out of range exceptions and allow partial rendering.
            if(x+xPos >=0 && x+xPos < mBoardWidth && row + yPos >=0 && row + yPos < mBoardHeight){
                for(int thick=0;thick<lineThickness;thick++) {
                    mBurnerBoard.setPixel(x-thick + xPos, row + yPos, color);
                }
            }
        }
        //  now at the bottom right, go left.
        for (x = x; x >= 0 ; x--) {
            // prevent out of range exceptions and allow partial rendering.
            if(x+xPos >=0 && x+xPos < mBoardWidth && row + yPos >=0 && row + yPos < mBoardHeight){
                for(int thick=0;thick<lineThickness;thick++) {
                    mBurnerBoard.setPixel(x + xPos, row-thick + yPos, color);
                }
            }
        }
        //now at the bottom left. back to the top left.
        for (row = row; row >= 0 ; row--) {
            // prevent out of range exceptions and allow partial rendering.
            if(x+xPos >=0 && x+xPos < mBoardWidth && row + yPos >=0 && row + yPos < mBoardHeight){
                for(int thick=0;thick<lineThickness;thick++) {
                    mBurnerBoard.setPixel(x + thick + xPos, row + yPos, color);
                }
            }
        }

    }
}
