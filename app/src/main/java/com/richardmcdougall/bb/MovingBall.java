package com.richardmcdougall.bb;

// some of this code was lifted from http://math.hws.edu/eck/cs124/javanotes6/source/MovingBall.java
public class MovingBall  {

    private double currentX,currentY;      // Current position of the ball.
    private double dx,dy;    // The velocity (speed + direction) of the ball.
    private double radius;   // The radius of the ball.
    private double lastX;
    private double lastY;

    private int mBoardWidth = 0;
    private int mBoardHeight = 0;
    private BurnerBoard mBurnerBoard = null;
    private int backgroundColor = BurnerBoard.getRGB(0, 0, 0);
    private int mDisplayColor = 0;

    public MovingBall(BurnerBoard board, int boardHeight, int boardWidth, int displayColor) {

        mBoardHeight = boardHeight;
        mBoardWidth = 32;//boardWidth;
        mBurnerBoard = board;
        mDisplayColor = displayColor;

        currentX = (mBoardWidth) / 2;
        currentY = (mBoardHeight) / 2;
        radius = 3;

        double angle = 2 * Math.PI * Math.random();  // Random direction.
        double speed = 4 + 4*Math.random();          // Random speed.
        dx = Math.cos(angle) * speed;
        dy = Math.sin(angle) * speed;
    }

    // color the previous position using the background color to 'erase' it.
    public void ClearLastCircle(){
        for(double y=-radius; y<=radius; y++)
            for(double x=-radius; x<=radius; x++)
                if(x*x+y*y <= radius*radius)
                    mBurnerBoard.setPixel((int)(x + lastX), (int)(y + lastY), backgroundColor);
    }

    // draw the new solid-filled circle.
     public void DrawFilledCircle(){
         for(double y=-radius; y<=radius; y++)
             for(double x=-radius; x<=radius; x++)
                 if(x*x+y*y <= radius*radius)
                     mBurnerBoard.setPixel((int)(x + currentX), (int)(y + currentY), mDisplayColor);
     }

    // move the ball
    public void travel(double time) {

        // Don't do anything if the rectangle is too small.

        if (mBoardWidth - 0 < 2*radius || mBoardHeight - 0 < 2*radius)
            return;

        // last x and y are used to 'erase' the previous location.

        lastX = currentX;
        lastY = currentY;

        // First, if the ball has gotten outside its rectangle, move it
        // back.  (This will only happen if the rectangle was changed
        // by calling the setLimits() method or if the position of
        // the ball was changed by calling the setLocation() method.)

        if (currentX-radius < 0)
            currentX = 0 + radius;
        else if (currentX+radius > mBoardWidth)
            currentX = mBoardWidth - radius;
        if (currentY - radius < 0)
            currentY = 0 + radius;
        else if (currentY + radius > mBoardHeight)
            currentY = mBoardHeight - radius;

       //Compute the new position, possibly outside the rectangle.

        double newx = currentX + dx*time;
        double newy = currentY + dy*time;

        //If the new position lies beyond one of the sides of the rectangle,
        //"reflect" the new point through the side of the rectangle, so it
        //lies within the rectangle.

        if (newy < 0 + radius) {
            newy = 2*(0+radius) - newy;
            dy = Math.abs(dy);
        }
        else if (newy > mBoardHeight - radius) {
            newy = 2*(mBoardHeight-radius) - newy;
            dy = -Math.abs(dy);
        }
        if (newx < 0 + radius) {
            newx = 2*(0+radius) - newx;
            dx = Math.abs(dx);
        }
        else if (newx > mBoardWidth - radius) {
            newx = 2*(mBoardWidth-radius) - newx;
            dx = -Math.abs(dx);
        }

        // We have the new values for currentX and currentY.

        currentX = newx;
        currentY = newy;

    }
}