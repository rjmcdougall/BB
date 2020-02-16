package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;

import java.nio.IntBuffer;

/*

   Back
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
 Front

 */
public class BurnerBoardAzul extends BurnerBoard {

    private static final String TAG = "BB.BurnerBoardAzul";
    private static final int kAzulBatteryMah = 38000;
    // Two primary mapping functions
    static int kStrips = 8;
    static int[] pixelsPerStrip = new int[8];
    static int[][] pixelMap2BoardTable = new int[8][4096];
    public int[] mLayeredScreen;
    long lastFlushTime = java.lang.System.currentTimeMillis();
    private int flushCnt = 0;
    private TranslationMap[] boardMap;

    public BurnerBoardAzul(BBService service) {
        super(service);
        boardWidth = 46;
        boardHeight = 118;
        boardType = "Burner Board Azul";
        BLog.d(TAG, "Burner Board Azul initing...");
        mBoardScreen = new int[boardWidth * boardHeight * 3];
        initPixelOffset();
        initpixelMap2Board();
        initUsb();
        mLayeredScreen = new int[boardWidth * boardHeight * 3];
        mTextBuffer = IntBuffer.allocate(boardWidth * boardHeight * 4);
        mDrawBuffer = IntBuffer.allocate(boardWidth * boardHeight * 4);
        mTextSizeHorizontal = 14;
        mTextSizeVerical = 10;
    }

    public int getMultiplier4Speed() {
        if (service.boardState.displayTeensy == BoardState.TeensyType.teensy4)
            return 1; // dkw need to config this
        else
            return 2; // dkw need to config this

    }

    public int getFrameRate() {

        if (this.service.boardState.boardType == BoardState.BoardType.boombox)
            return 15;
        else
            return 30;
    }

    public void flush() {

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            android.util.Log.d("BB.BurnerBoardAzul", "Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }

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
        powerLimitMultiplierPercent = 100 - java.lang.Math.max(powerPercent - 15, 0);

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

        // Render text on board
        int[] mOutputScreen;
        if (renderText(mLayeredScreen, mBoardScreen) != null) {
            mOutputScreen = mLayeredScreen;
        } else {
            mOutputScreen = mBoardScreen;
        }

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[pixelsPerStrip[s] * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; ) {
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
            }
            setStrip(s, stripPixels, powerLimitMultiplierPercent);
            // Send to board
            if (this.service.boardState.displayTeensy == BoardState.TeensyType.teensy3)
                flush2Board();
        }
        // Render on board
        update();
        flush2Board();

    }

    private void pixelRemap(int x, int y, int stripOffset) {
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset + 1] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset + 2] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
    }

    private void initpixelMap2Board() {
        int x, y;

        boardMap = TranslationMapBag.azul;


        // Walk through all the strips and find the number of pixels in the strip
        for (int s = 0; s < kStrips; s++) {
            pixelsPerStrip[s] = 0;
            // Search strips and find longest pixel count
            for (int i = 0; i < boardMap.length; i++) {
                int endPixel = java.lang.Math.abs(boardMap[i].endX -
                        boardMap[i].startX) + 1 + boardMap[i].stripOffset;
                if (s == (boardMap[i].stripNumber - 1) && endPixel > pixelsPerStrip[s]) {
                    pixelsPerStrip[s] = endPixel;
                    //l("boardmap: strip " + s + " has " + pixelsPerStrip[s] + " pixels" );
                }
            }
        }

        for (y = 0; y < boardMap.length; y++) {
            if (boardMap[y].stripDirection == 1) {
                // Strip has x1 ... x2
                for (x = boardMap[y].startX; x <= boardMap[y].endX; x++) {
                    int stripOffset = boardMap[y].stripOffset + x - boardMap[y].startX;
                    pixelRemap(x, y, stripOffset * 3);
                }
            } else {
                // Strip has x2 ... x1 (reverse order)
                for (x = boardMap[y].endX; x <= boardMap[y].startX; x++) {
                    int stripOffset = boardMap[y].stripOffset - x + boardMap[y].startX;
                    pixelRemap(x, y, stripOffset * 3);
                }
            }
        }
        for (int s = 0; s < kStrips; s++) {
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; offset++) {
                //l("Strip " + s + " offset " + offset + " =  pixel offset " + pixelMap2BoardTable[s][offset]);
            }
        }
    }
}
