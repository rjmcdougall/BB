package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;

// Specific for the Woodsons mask.
public class BurnerBoardMast extends BurnerBoard {

    static int kStrips = 8;
    static int[][] pixelMap2BoardTable = new int[8][4096];
    private String TAG = this.getClass().getSimpleName();

    static {
        boardWidth = 24;
        boardHeight = 159;
        textSizeHorizontal = 12;
        textSizeVertical = 12;
    }

    public BurnerBoardMast(BBService service) {
        super(service);
        BLog.i(TAG, "Burner Board Mast initing...");

        initpixelMap2Board();
    }

    public int getMultiplier4Speed() {
        return 3;
    }
    public int getFrameRate() {
        return 18;
    }
    public void setOtherlightsAutomatically(){};

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardMast.BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(false);
        mListener.attach(8, getBatteryLevelCallback);

    }

    public void flush() {

        this.logFlush();
        int[] mOutputScreen = this.textBuilder.renderText(boardScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = PixelDimmer.Dim(12, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[boardHeight * 3 * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < boardHeight * 3 * 3; ) {
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
            }
            setStrip(s, stripPixels);
            // Send to board
            flush2Board();
        }// Render on board
        update();
        flush2Board();

    }

    private void pixelRemap(int x, int y, int stripNo, int stripOffset) {
        pixelMap2BoardTable[stripNo][stripOffset] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[stripNo][stripOffset + 1] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[stripNo][stripOffset + 2] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
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

