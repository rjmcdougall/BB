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
    static int kStrips = 1;
    static int[] pixelsPerStrip = new int[64];
    static int[][] pixelMap2BoardTable = new int[64][2048];
    private TranslationMap[] boardMap;

    static {
        boardWidth = 32;
        boardHeight = 64;
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
        BurnerBoardPanel.BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(false);
        mListener.attach(8, getBatteryLevelCallback);

    }

    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = PixelDimmer.Dim(15, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[pixelsPerStrip[s] * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; ) {
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
            }
            setStrip(s, stripPixels);
        }

        update();
        flush2Board();

    }
    private void pixelRemap(int x, int y, int stripOffset) {
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset + 1] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset + 2] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
    }

    private void initpixelMap2Board(){
        int x, y;

        TranslationMap[] panel = {
//X,StartY,End Y,Direction,Strip #,Offset in strip
                new TranslationMap(0, 0, 32, -1, 1, 0),
                new TranslationMap(1, 0, 32, -1, 2, 0),
                new TranslationMap(2, 0, 32, -1, 3, 0),
                new TranslationMap(3, 0, 32, -1, 4, 0),
                new TranslationMap(4, 0, 32, -1, 5, 0)
        };

        boardMap = panel; //this.service.displayMapManager.GetDisplayMap();

        // Walk through all the strips and find the number of pixels in the strip
        for (int s = 0; s < kStrips; s++) {
            pixelsPerStrip[s] = 0;
            // Search strips and find longest pixel count
            for (int i = 0; i < boardMap.length; i++) {
                int endPixel = Math.abs(boardMap[i].endY - boardMap[i].startY) + 1 + boardMap[i].stripOffset;
                if (s == (boardMap[i].stripNumber - 1) && endPixel > pixelsPerStrip[s]) {
                    pixelsPerStrip[s] = endPixel;
                    BLog.i(TAG, "boardmap: strip " + s + " has " + pixelsPerStrip[s] + " pixels" );
                }
            }
        }

        for (x = 0; x < boardMap.length; x++) {
            if (boardMap[x].stripDirection == 1) {
                // Strip has y1 ... y2
                for (y = boardMap[x].startY; y <= boardMap[x].endY; y++) {
                    int stripOffset = boardMap[x].stripOffset + y - boardMap[x].startY;
                    BLog.i("x " + x + "y " + y + "offset " + stripOffset, TAG);
                    pixelRemap(x, y, stripOffset * 3);
                }
            } else {
                // Strip has y2 ... y1 (reverse order)
                for (y = boardMap[x].endY; y <= boardMap[x].startY; y++) {
                    int stripOffset = boardMap[x].stripOffset - y + boardMap[x].startY;
                    pixelRemap(x, y, stripOffset * 3);
                }
            }
        }

//        for (int s = 0; s < kStrips; s++) {
//            // Walk through all the pixels in the strip
//            for (int offset = 0; offset < pixelsPerStrip[s] * 3; offset++) {
//                BLog.i(TAG, "Strip " + s + " offset " + offset + " =  pixel offset " + pixelMap2BoardTable[s][offset]);
//            }
//        }

        int i = 1;
    }
}