package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

public class BurnerBoardV4 extends BurnerBoard {

    private String TAG = this.getClass().getSimpleName();
    public int kStrips = 1;
    public int[] pixelsPerStrip = new int[1];
    static int[][] mapPixelsToStips = new int[1][4096];
    private TranslationMap[] boardMap;
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
        return 40;
    }
    public void setOtherlightsAutomatically() {
    }
    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = mDimmer.Dim(1, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[pixelsPerStrip[s] * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; ) {
                stripPixels[offset] = mOutputScreen[mapPixelsToStips[s][offset++]];
                stripPixels[offset] = mOutputScreen[mapPixelsToStips[s][offset++]];
                stripPixels[offset] = mOutputScreen[mapPixelsToStips[s][offset++]];
            }
            setStrip(s, stripPixels);
            if ((s % 3) == 0) {
                flush2Board();
            }

            // Send to board
            if(s % 3 == 0)
                flush2Board();
        }
        // Render on board
        update();
        flush2Board();

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
        pixelsPerStrip = new int[kStrips];
        mapPixelsToStips = new int[kStrips][4096];
        pixelOffset = new PixelOffset(this);

        try {
            for (int s = 0; s < kStrips; s++) {
                pixelsPerStrip[s] = 0;
                // Search strips and find longest pixel count
                for (int i = 0; i < boardMap.length; i++) {
                    int endPixel = java.lang.Math.abs(boardMap[i].endX - boardMap[i].startX) + 1 + boardMap[i].stripOffset;
                    if (s == (boardMap[i].stripNumber - 1) && endPixel > pixelsPerStrip[s]) {
                        pixelsPerStrip[s] = endPixel;
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
        }
        catch (Exception e){
            BLog.e(TAG, e.getMessage());
        }

        for (int s = 0; s < kStrips; s++) {
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; offset++) {
     //           BLog.d(TAG, "Strip " + s + " offset " + offset + " =  pixel offset " + pixelMap2BoardTable[s][offset]);
            }
        }
    }
}