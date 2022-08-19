package com.richardmcdougall.bb.board;

/*

   Back
         32,64
 +------+
 |      |
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
 |      |
 +------+
 0,0 X
 Front

 */

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.lang.reflect.Array;

public class BurnerBoardDynamicPanel extends BurnerBoard {
    private String TAG = this.getClass().getSimpleName();
    public int kStrips = 1;
    static int[] pixelsPerStrip = new int[64];
    static int[][] pixelMap2BoardTable = new int[64][2048];
    private TranslationMap[] boardMap;
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
        BLog.i(TAG, "Burner Board Panel initting...");
        initpixelMap2Board();
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
        mOutputScreen = PixelDimmer.Dim(15, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[pixelsPerStrip[s] * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; ) {
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
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
    
    private void pixelRemap(int x, int y, int stripOffset) {
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset + 1] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset + 2] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
    }

    public void initpixelMap2Board(){
        int x, y;


        boardMap = this.service.displayMapManager.GetDisplayMap();
        boardWidth = this.service.displayMapManager.boardWidth;
        boardHeight = this.service.displayMapManager.boardHeight;
        kStrips = this.service.displayMapManager.numberOfStrips;
        boardScreen = new int[this.boardWidth * this.boardHeight * 3];
        pixelOffset = new PixelOffset(this);

        for (int s = 0; s < kStrips; s++) {
            pixelsPerStrip[s] = 0;
            // Search strips and find longest pixel count
            for (int i = 0; i < boardMap.length; i++) {
                int endPixel = Math.abs(boardMap[i].endY - boardMap[i].startY) + 1 + boardMap[i].stripOffset;
                if (s == (boardMap[i].stripNumber - 1) && endPixel > pixelsPerStrip[s]) {
                    pixelsPerStrip[s] = endPixel;
 //                   BLog.i(TAG, "boardmap: strip " + s + " has " + pixelsPerStrip[s] + " pixels" );
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
        
//        for (int s = 0; s < kStrips; s++) {
//            // Walk through all the pixels in the strip
//            for (int offset = 0; offset < pixelsPerStrip[s] * 3; offset++) {
//                //l("Strip " + s + " offset " + offset + " =  pixel offset " + pixelMap2BoardTable[s][offset]);
//            }
//        }
    }
}