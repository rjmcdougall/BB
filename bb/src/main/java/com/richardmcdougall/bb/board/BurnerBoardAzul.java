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

public class BurnerBoardAzul extends BurnerBoard {

    // Was 50, was popping reg. Try 30...
    private static final int kMaxAzulDisplayPower = 20;
    private String TAG = this.getClass().getSimpleName();
    static int kStrips = 8;
    static int[] pixelsPerStrip = new int[8];
    static int[][] pixelMap2BoardTable = new int[8][4096];
    private TranslationMap[] boardMap;
    //public int boardWidth = 46;
    //public int boardHeight = 118;
    private PixelDimmer mDimmer = new PixelDimmer();
    private GammaCorrection mGammaCorrection = new GammaCorrection();


    static {
        textSizeHorizontal = 14;
        textSizeVertical = 10;
        enableBatteryMonitoring = true;
        enableMotionMonitoring = true;
        enableIOTReporting = true;
        renderTextOnScreen = true;
        boardType = BoardState.BoardType.azul;
        renderLineOnScreen = false;
    }

    public BurnerBoardAzul(BBService service) {
        super(service);

        BLog.i(TAG, "Burner Board Azul initing...");

        boardWidth = 46;
        boardHeight = 118;
        init(boardWidth, boardHeight);
        initpixelMap2Board();
    }

    public int getMultiplier4Speed() {
        if (service.boardState.displayTeensy == BoardState.TeensyType.teensy4)
            return 1; // dkw need to config this
        else
            return 2; // dkw need to config this

    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardAzul.BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(true);
        mListener.attach(8, getBatteryLevelCallback);

    }

    public int getFrameRate() {
            return 15;
    }

    public void setOtherlightsAutomatically() {
    }



    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = this.batteryOverlayBuilder.renderBattery(mOutputScreen);
        mOutputScreen = mGammaCorrection.Correct(mOutputScreen);
        mOutputScreen = mDimmer.Dim(kMaxAzulDisplayPower, mOutputScreen);

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
//            if (this.service.boardState.displayTeensy == BoardState.TeensyType.teensy3)
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

    public void initpixelMap2Board() {
        int x, y;

        boardMap = TranslationMapBag.azul;
        boardWidth = 46;
        boardHeight = 118;
        kStrips = 8;
        boardScreen = new int[this.boardWidth * this.boardHeight * 3];
        pixelsPerStrip = new int[kStrips];
        pixelMap2BoardTable = new int[kStrips][2048];

        // Walk through all the strips and find the number of pixels in the strip
        for (int s = 0; s < kStrips; s++) {
            pixelsPerStrip[s] = 0;
            // Search strips and find longest pixel count
            for (int i = 0; i < boardMap.length; i++) {
                int endPixel = java.lang.Math.abs(boardMap[i].endX - boardMap[i].startX) + 1 + boardMap[i].stripOffset;
                if (s == (boardMap[i].stripNumber - 1) && endPixel > pixelsPerStrip[s]) {
                    pixelsPerStrip[s] = endPixel;
                   //BLog.i(TAG, "boardmap: strip " + s + " has " + pixelsPerStrip[s] + " pixels" );
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
