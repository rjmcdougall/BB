package com.richardmcdougall.bb.board;

import com.hoho.android.usbserial.util.MonotonicClock;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

public class BurnerBoardV4 extends BurnerBoard {

    private String TAG = this.getClass().getSimpleName();
    public int kStrips = 1;
    static int[][] mapPixelsToStips = new int[1][4096];
    private TranslationMap[] boardMap;
    private GammaCorrection mGammaCorrection = new GammaCorrection();
    private PixelDimmer mDimmer = new PixelDimmer();

    static {
        textSizeHorizontal = 14;
        textSizeVertical = 20;
        enableBatteryMonitoring = true;
        enableIOTReporting = true;
        renderTextOnScreen = false;
        boardType = BoardState.BoardType.v4;
    }

    public BurnerBoardV4(BBService service) {
        super(service);
        initpixelMap2Board();
    }

    public int getMultiplier4Speed() {
            return 1;
    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(true);
        mListener.attach(8, getBatteryLevelCallback);

    }

    public int getFrameRate() {
        return 25;
    }
    public void setOtherlightsAutomatically() {
    }

    // Work around that teensy cmd essenger can't deal with 0, ',', ';', '\\'
    private static int pixelWorkaround(int level) {
        switch (level) {
            case 0:
                level = 1;
                break;
            case ',':
                level += 1;
            case ';':
                level += 1;
            case '\\':
                level += 1;
            default:
        }
        return level;
    }
    public void flush() {

        long latency = 0;
        latency = MonotonicClock.millis();
        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        // TODO: gamma correction should be here before dimmer
        mOutputScreen = mGammaCorrection.Correct(mOutputScreen);
        mOutputScreen = mDimmer.Dim(10, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[600 * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < 600 * 3; ) {
                stripPixels[offset] = pixelWorkaround(mOutputScreen[mapPixelsToStips[s][offset++]]);
                stripPixels[offset] = pixelWorkaround(mOutputScreen[mapPixelsToStips[s][offset++]]);
                stripPixels[offset] = pixelWorkaround(mOutputScreen[mapPixelsToStips[s][offset++]]);
            }
            latency = MonotonicClock.millis();
            setStrip(s, stripPixels);
            //BLog.d(TAG, "setstrip latency = " + (MonotonicClock.millis()- latency));
            if ((s % 2) == 0) {
                latency = MonotonicClock.millis();
                flush2Board();
                //BLog.d(TAG, "flush latency = " + (MonotonicClock.millis()- latency));
            }

        }
        // Render on board
        latency = MonotonicClock.millis();
        update();
        //BLog.d(TAG, "update latency = " + (MonotonicClock.millis()- latency));
        latency = MonotonicClock.millis();
        flush2Board();
        //BLog.d(TAG, "flush latency = " + (MonotonicClock.millis()- latency));
    }
    private void pixelRemap(int x, int y, int stripOffset) {
        mapPixelsToStips[boardMap[y].stripNumber - 1][stripOffset] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        mapPixelsToStips[boardMap[y].stripNumber - 1][stripOffset + 1] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        mapPixelsToStips[boardMap[y].stripNumber - 1][stripOffset + 2] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
    }

    public void initpixelMap2Board() {
        int x, y;

        this.clearPixels();

        boardMap = this.service.displayMapManager.GetDisplayMap();
        boardWidth = this.service.displayMapManager.boardWidth;
        boardHeight = this.service.displayMapManager.boardHeight;
        kStrips = this.service.displayMapManager.numberOfStrips;
        boardScreen = new int[this.boardWidth * this.boardHeight * 3];
        mapPixelsToStips = new int[kStrips][4096];
        pixelOffset = new PixelOffset(this);

        try {
            for (y = 0; y < boardMap.length; y++) {
                // Strip has x2 ... x1 (reverse order)
                int stripOffset = boardMap[y].stripOffset;
                for (x = boardMap[y].endX; x <= boardMap[y].startX; x++) {
                    pixelRemap(x, y, stripOffset * 3);
                    stripOffset++;
                }
            }
        }
        catch (Exception e){
            BLog.e(TAG, e.getMessage());
        }
    }
}