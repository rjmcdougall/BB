package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;

import java.nio.IntBuffer;

// Specific for the Woodsons mask.
public class BurnerBoardMast extends BurnerBoard {

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

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardMast.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardMast.BoardCallbackGetBatteryLevel();
        mListener.attach(8, getBatteryLevelCallback);

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

            int powerLimitMultiplierPercent = findPowerLimitMultiplierPercent(12);

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

    public class BoardCallbackGetBatteryLevel implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            for (int i = 0; i < mBatteryStats.length; i++) {
                mBatteryStats[i] = mListener.readIntArg();
            }
            if (mBatteryStats[1] != -1) {
                service.boardState.batteryLevel = mBatteryStats[1];
            } else {
                service.boardState.batteryLevel = 100;
            }
            BLog.d(TAG, "getBatteryLevel: " + service.boardState.batteryLevel);
        }
    }
}

