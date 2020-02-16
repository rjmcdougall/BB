package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.nio.IntBuffer;

public class BurnerBoardDirectMap extends BurnerBoard {

    private static final String TAG = "BB.BurnerBoardDirectMap";

    /* JosPacks have more of a power constraint, so we don't want to set it to full brightness. Empirically tested
        with with a rapidly refreshing pattern (BlueGold):
        100 -> 1.90a draw
        50  -> 0.50a draw
        25  -> 0.35a draw
    */
    private static final int kVisualizationDirectMapPowerMultiplier = BoardState.kIsRPI ? 25 : 100; // should be ok for nano
    private static final int mPowerMultiplier = kVisualizationDirectMapPowerMultiplier;

    /* DIRECT MAP SETTINGS */
    // JosPacks have 1x166 strands of LEDs. Currently RPI == JosPack
    private static final int kVisualizationDirectMapDefaultWidth = 8;
    public static final int kVisualizationDirectMapWidth = BoardState.kIsRPI ? 1 : kVisualizationDirectMapDefaultWidth;
    private static final int kVisualizationDirectMapDefaultHeight = 256;
    public static final int kVisualizationDirectMapHeight = BoardState.kIsRPI ? 166 : kVisualizationDirectMapDefaultHeight;
    static int kStrips = 8;
    long lastFlushTime = java.lang.System.currentTimeMillis();
    private int flushCnt = 0;

    public BurnerBoardDirectMap(BBService service, int width, int height) {
        super(service);
        boardWidth = width;
        boardHeight = height;

        mBoardScreen = new int[boardWidth * boardHeight * 3];

        boardType = "Burner Board DirectMap";
        BLog.d(TAG, boardType + " initializing at: " + boardWidth + " x " + boardHeight);

        mTextBuffer = IntBuffer.allocate(boardWidth * boardHeight * 4);
        initPixelOffset();
        initUsb();
    }

    @Override
    public int getMultiplier4Speed() {
        return 3;
    }

    public void flush() {

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            BLog.d(TAG, "Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }

        // Suppress updating when displaying a text message
        if (isTextDisplaying > 0) {
            isTextDisplaying--;
        } else {

            // Here we calculate the total power percentage of the whole board
            // We want to limit the board to no more than 50% of pixel output total
            // This is because the board is setup to flip the breaker at 200 watts
            // Output is percentage multiplier for the LEDs
            // At full brightness we limit to 30% of their output
            // Power is on-linear to pixel brightness: 37% = 50% power.
            // powerPercent = 100: 15% multiplier
            // powerPercent <= 15: 100% multiplier
            int powerLimitMultiplierPercent = mPowerMultiplier;

            /* None of this code was actively used; see the last line that simply re-sets this value
               to 100. So, let's comment out this code and just use the constant from bbutil -jib

            int totalBrightnessSum = 0;
            for (int pixel = 0; pixel < mBoardScreen.length; pixel++) {
                // R
                if (pixel % 3 == 0) {
                    totalBrightnessSum += mBoardScreen[pixel];
                } else if (pixel % 3 == 1) {
                    totalBrightnessSum += mBoardScreen[pixel];
                } else {
                    totalBrightnessSum += mBoardScreen[pixel] / 2;
                }
            }

            final int powerPercent = totalBrightnessSum / mBoardScreen.length * 100 / 255;
            //powerLimitMultiplierPercent = 100;// - java.lang.Math.max(powerPercent - 12, 0);
            */

            int[] rowPixels = new int[boardWidth * 3];
            for (int y = 0; y < boardHeight; y++) {
                //for (int y = 30; y < 31; y++) {
                for (int x = 0; x < boardWidth; x++) {
                    if (y < boardHeight) {
                        rowPixels[x * 3 + 0] = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                        rowPixels[x * 3 + 1] = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                        rowPixels[x * 3 + 2] = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                    }
                }
                //setRowVisual(y, rowPixels);
            }

            // Walk through each strip and fill from the graphics buffer
            for (int s = 0; s < kStrips; s++) {
                int[] stripPixels = new int[boardHeight * 3];
                // Walk through all the pixels in the strip
                for (int y = 0; y < boardHeight; y++) {
                    stripPixels[y * 3] = mBoardScreen[pixel2Offset(s, y, PIXEL_RED)];
                    stripPixels[y * 3 + 1] = mBoardScreen[pixel2Offset(s, y, PIXEL_GREEN)];
                    stripPixels[y * 3 + 2] = mBoardScreen[pixel2Offset(s, y, PIXEL_BLUE)];
                }
                setStrip(s, stripPixels, powerLimitMultiplierPercent);
                // Send to board
                flush2Board();
            }

            // Render on board
            update();
            flush2Board();
        }
    }
}

