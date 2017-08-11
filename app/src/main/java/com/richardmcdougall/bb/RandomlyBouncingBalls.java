package com.richardmcdougall.bb;

/**
 * Created by d.wilson on 8/11/17.
 */

public class RandomlyBouncingBalls {

    private BurnerBoard mBurnerBoard = null;

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

    static int sfColorPointer = 0;

    static int numberOfBalls = 20;
    static MovingBall[] movingBalls = new MovingBall[20];

    RandomlyBouncingBalls(BurnerBoard board, int boardHeight, int boardWidth){

        mBurnerBoard = board;

        for(int m=0;m<numberOfBalls;m++) {
            movingBalls[m] = new MovingBall(board, boardHeight, boardWidth, sfColors[sfColorPointer]);
            if(sfColorPointer==7)
                sfColorPointer=0;
            else
                sfColorPointer++;
        }
    }

    public void modeRandomlyBouncingBalls(){

        for(int i=0;i<10;i++){
            for(int m=0;m<numberOfBalls;m++) {
                movingBalls[m].ClearLastCircle();
                movingBalls[m].DrawFilledCircle();
                movingBalls[m].travel(1.0);
            }
            mBurnerBoard.flush();
        }
    }

}
