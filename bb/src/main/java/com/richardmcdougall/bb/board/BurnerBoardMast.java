package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;

import java.nio.IntBuffer;

/*

   bottom
         46,118
 +------+
 |      | strip 1
 |      |
 |      |
 |      |
 |      |
 |      | Y
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      |
 |      | strip 8
 +------+
 0,0 X
 top

 */

public class BurnerBoardMast extends BurnerBoard {
    // Two primary mapping functions
    static int kStrips = 8;
    static int[][] pixelMap2BoardTable = new int[8][4096];
    long lastFlushTime = java.lang.System.currentTimeMillis();
    private String TAG = this.getClass().getSimpleName();
    private int flushCnt = 0;

    public BurnerBoardMast(BBService service) {
        super(service);
        boardWidth = 24;
        boardHeight = 159;
        boardType = "Burner Board Mast";
        BLog.d(TAG, "Burner Board Mast initing...");
        mBoardScreen = new int[boardWidth * boardHeight * 3];
        initPixelOffset();
        initpixelMap2Board();
        initUsb();
        mTextBuffer = IntBuffer.allocate(boardWidth * boardHeight * 4);
    }

    @Override
    public int getMultiplier4Speed() {
        return 3;
    }

    public int getFrameRate() {
        return 18;
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
            int totalBrightnessSum = 0;
            int powerLimitMultiplierPercent = 100;
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
            powerLimitMultiplierPercent = 100 - java.lang.Math.max(powerPercent - 12, 0);

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
                setRowVisual(y, rowPixels);
            }

            // Walk through each strip and fill from the graphics buffer
            for (int s = 0; s < kStrips; s++) {
                int[] stripPixels = new int[boardHeight * 3 * 3];
                // Walk through all the pixels in the strip
                for (int offset = 0; offset < boardHeight * 3 * 3; ) {
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                    stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                }
                setStrip(s, stripPixels, powerLimitMultiplierPercent);
                // Send to board
                flush2Board();
            }// Render on board
            update();
            flush2Board();
        }
    }

    private void pixelRemap(int x, int y, int stripNo, int stripOffset) {
        pixelMap2BoardTable[stripNo][stripOffset] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[stripNo][stripOffset + 1] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[stripNo][stripOffset + 2] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
    }

    private void initpixelMap2Board() {

        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {

                final int subStrip = x % 3;
                final int stripNo = x / 3;
                final boolean stripUp = subStrip % 2 == 0;
                int stripOffset;

                if (stripUp) {
                    stripOffset = subStrip * boardHeight + y;
                } else {
                    stripOffset = subStrip * boardHeight + (boardHeight - 1 - y);
                }
                pixelRemap(x, y, stripNo, stripOffset * 3);
            }

        }

    }

}

