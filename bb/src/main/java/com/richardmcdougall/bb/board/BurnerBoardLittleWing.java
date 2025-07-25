package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

//  cmdMessenger.sendCmdStart(BBGetBatteryLevel);
//          cmdMessenger.sendCmdArg(batteryControl);
//          cmdMessenger.sendCmdArg(batteryStateOfCharge);
//          cmdMessenger.sendCmdArg(batteryMaxError);
//          cmdMessenger.sendCmdArg(batteryRemainingCapacity);
//          cmdMessenger.sendCmdArg(batteryFullChargeCapacity);
//          cmdMessenger.sendCmdArg(batteryVoltage);
//          cmdMessenger.sendCmdArg(batteryAverageCurrent);
//          cmdMessenger.sendCmdArg(batteryTemperature);
//          cmdMessenger.sendCmdArg(batteryFlags);
//          cmdMessenger.sendCmdArg(batteryCurrent);
//          cmdMessenger.sendCmdArg(batteryFlagsB);
//          cmdMessenger.sendCmdEnd();

public class BurnerBoardLittleWing extends BurnerBoard {

    private String TAG = this.getClass().getSimpleName();
    static int kStrips = 16;
    static int[] pixelsPerStrip = new int[16];
    static int[][] pixelMap2BoardTable = new int[16][4096];
    private TranslationMap[] boardMap;
    private PixelDimmer mDimmer = new PixelDimmer();


    static {
        textSizeHorizontal = 14;
        textSizeVertical = 20;
        enableBatteryMonitoring = true;
        enableIOTReporting = true;
        renderTextOnScreen = true;
        boardType = BoardState.BoardType.littlewing;
        renderLineOnScreen = false;
    }

    public BurnerBoardLittleWing(BBService service) {
        super(service);
        init(boardWidth, boardHeight);
        initpixelMap2Board();
        boardWidth = 32;
        boardHeight = 255;
        BLog.i(TAG, "Burner Board Little Wing initting...");
    }

    public int getMultiplier4Speed() {
        return 5; // dkw need to config this

    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(true);
        mListener.attach(8, getBatteryLevelCallback);

    }

    public int getFrameRate() {
            return 35;
    }

    public void setOtherlightsAutomatically() {
    }

    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = mDimmer.Dim(3, mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        // last two strips are emulated side strips
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[pixelsPerStrip[s] * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; ) {
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
            }
            setStrip(s, stripPixels);
            // TODO: something takes a shit upstream if too much data is outstanding before a flush
            if (s == 8) {
                flush2Board();
            }
        }
        // Render on board
        update();
        flush2Board();

    }

    private void pixelRemap(int x, int y, int stripOffset) {
        pixelMap2BoardTable[boardMap[x].stripNumber - 1][stripOffset] = this.pixelOffset.Map(x, y, PIXEL_RED);
        pixelMap2BoardTable[boardMap[x].stripNumber - 1][stripOffset + 1] = this.pixelOffset.Map(x, y, PIXEL_GREEN);
        pixelMap2BoardTable[boardMap[x].stripNumber - 1][stripOffset + 2] = this.pixelOffset.Map( x,  y, PIXEL_BLUE);
    }

    public void initpixelMap2Board() {
        int x, y;

        boardWidth = 32;
        boardHeight = 255;
        BLog.d(TAG, "initpixelMap2Board");
        boardMap = TranslationMapBag.littlewing;
        pixelOffset = new PixelOffset(this);
        this.pixelOffset.initPixelOffset();
        this.clearPixels();

        // Walk through all the strips and find the number of pixels in the strip
        for (int s = 0; s < kStrips; s++) {
            pixelsPerStrip[s] = 0;
            // Search strips and find longest pixel count
            for (int i = 0; i < boardMap.length; i++) {
                int endPixel = Math.abs(boardMap[i].endY - boardMap[i].startY) + 1 + boardMap[i].stripOffset;
                if (s == (boardMap[i].stripNumber - 1) && endPixel > pixelsPerStrip[s]) {
                    pixelsPerStrip[s] = endPixel;
                    //BLog.i(TAG, "boardmap: strip " + s + " has " + pixelsPerStrip[s] + " pixels" );
                }
            }
        }

        for (x = 0; x < boardMap.length; x++) {
            if (boardMap[x].stripDirection == 1) {
                // Strip has y1 ... y2
                for (y = boardMap[x].startY; y <= boardMap[x].endY; y++) {
                    int stripOffset = boardMap[x].stripOffset + y - boardMap[x].startY;
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
        /*
        for (int s = 0; s < kStrips; s++) {
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; offset++) {
                BLog.i(TAG, "Strip " + s + " offset " + offset + " =  pixel offset " + pixelMap2BoardTable[s][offset]);
            }
        }
        */
    }
}