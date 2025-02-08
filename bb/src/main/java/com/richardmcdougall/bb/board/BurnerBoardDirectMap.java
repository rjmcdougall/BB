package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

// used by the now-defunct josPaks
public class BurnerBoardDirectMap extends BurnerBoard {

    private String TAG = this.getClass().getSimpleName();
    private static int kStrips = 8;
    private PixelDimmer mDimmer = new PixelDimmer();


    static {
        textSizeVertical = 0;
        textSizeHorizontal = 0;
        enableBatteryMonitoring = false;
        enableIOTReporting = false;
        renderTextOnScreen = false;
        boardType = BoardState.BoardType.backpack;
        renderLineOnScreen = false;
    }

    public BurnerBoardDirectMap(BBService service) {
        super(service);
        boardWidth = 1;
        boardHeight = 166;
        BLog.i(TAG, " Direct Map initing ");
    }

    @Override
    public int getMultiplier4Speed() {
        return 3;
    }

    public int getFrameRate() {
        return 12;
    }

    public void setOtherlightsAutomatically() {
    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardDirectMap.BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(false);
        mListener.attach(8, getBatteryLevelCallback);

    }
    public void initpixelMap2Board() {
        
    }
    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = mDimmer.Dim(200, mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[boardHeight * 3];
            // Walk through all the pixels in the strip
            for (int y = 0; y < boardHeight; y++) {
                stripPixels[y * 3] = mOutputScreen[this.pixelOffset.Map(s, y, PIXEL_RED)];
                stripPixels[y * 3 + 1] = mOutputScreen[this.pixelOffset.Map(s, y, PIXEL_GREEN)];
                stripPixels[y * 3 + 2] = mOutputScreen[this.pixelOffset.Map(s, y, PIXEL_BLUE)];
            }
            setStrip(s, stripPixels);
            // Send to board
            flush2Board();
        }

        // Render on board
        update();
        flush2Board();

    }
}
