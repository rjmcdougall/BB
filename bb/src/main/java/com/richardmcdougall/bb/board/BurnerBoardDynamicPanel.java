package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BoardState;

public class BurnerBoardDynamicPanel extends BurnerBoard {

    private String TAG = this.getClass().getSimpleName();
    public int kStrips = 1;
    public int[] pixelsPerStrip = new int[1];
    static int[][] mapPixelsToStips = new int[1][4096];

    private PixelDimmer mDimmer = new PixelDimmer();
    private PixelColorSections mColorSections = new PixelColorSections();

    static {
        textSizeHorizontal = 12;
        textSizeVertical = 12;
        enableBatteryMonitoring = true;
        enableIOTReporting = true;
        renderTextOnScreen = true;
        boardType = BoardState.BoardType.dynamicPanel;
    }

    public BurnerBoardDynamicPanel(BBService service) {
        super(service);
    }

    @Override
    public int getMultiplier4Speed() {
        return 3;
    }
    public int getFrameRate() {
        return 12;
    }
    public void setOtherlightsAutomatically(){};

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardDynamicPanel.BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(false);
        mListener.attach(8, getBatteryLevelCallback);

    }

    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = this.mColorSections.ColorSections(mOutputScreen, this.service.displayMapManager2);
        mOutputScreen = mDimmer.Dim(15, mOutputScreen);
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
            // Send to board
            if (this.service.boardState.displayTeensy == BoardState.TeensyType.teensy3)
                flush2Board();
        }
        // Render on board
        update();
        flush2Board();

    }

    public void initpixelMap2Board() {

        boardWidth = this.service.displayMapManager2.boardWidth;
        boardHeight = this.service.displayMapManager2.boardHeight;
        kStrips = this.service.displayMapManager2.numberOfStrips;
        boardScreen = new int[this.boardWidth * this.boardHeight * 3];
        pixelsPerStrip = this.service.displayMapManager2.pixelsPerStrip();
        mapPixelsToStips = new int[kStrips][2048];

        pixelOffset = new PixelOffset(this);

        for (int y = 0; y < this.service.displayMapManager2.boardHeight; y++) {

            // Strip has x1 ... x2
            for (int x = 0; x < this.service.displayMapManager2.boardWidth; x++) {

                int strip = this.service.displayMapManager2.whichStripAmI(y * this.service.displayMapManager2.boardWidth + x);
                int pixelsInPreviousStrips = this.service.displayMapManager2.stripOffsets.get(strip);
                int stripOffset = 3 * (y * this.service.displayMapManager2.boardWidth + x - pixelsInPreviousStrips);

                // calculate which strip and which location
                mapPixelsToStips[strip][stripOffset] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
                mapPixelsToStips[strip][stripOffset + 1] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
                mapPixelsToStips[strip][stripOffset + 2] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);

            }
        }
    }
}