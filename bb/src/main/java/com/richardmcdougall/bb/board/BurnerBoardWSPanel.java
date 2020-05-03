package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

//BBWSPanel is a string of WS28xx leds that go up and down
public class BurnerBoardWSPanel extends BurnerBoard {

    private String TAG = this.getClass().getSimpleName();

    // Limits of the hardware strips:
    private static final int kMaxStrips = 24;
    private static final int kMaxStripLength = 1024;

    // Woodson's panel
    static int kBoardWidth = 20;
    static int kBoardHeight = 180;
    static int kPanelHeight = kBoardHeight;
    static int kPanelWidth = 2;
    // Serpentine in Y/Height direction
    static boolean kStackedY = true;
    // Number of logical strips in each strip
    static int kSubStrips = kPanelWidth;
    // Number of hardware strips
    static int kStrips = 16; //boardHeight / panel height

    public BurnerBoardWSPanel(BBService service) {
        super(service);

        boardWidth = kBoardWidth;
        boardHeight = kBoardHeight;
        boardType = "Burner Board WSPanel";
        BLog.d(TAG, "Burner Board WSPanel initting...");
        mBoardScreen = new int[boardWidth * boardHeight * 3];
        BLog.d(TAG, "Burner Board WSPanel initPixelOffset...");
        initPixelOffset();
        BLog.d(TAG, "Burner Board WSPanel initpixelMap2Board...");
        initpixelMap2Board();
        this.appDisplay = new AppDisplay(service, boardWidth, boardHeight, this.pixel2OffsetTable);
        BLog.d(TAG, "Burner Board WSPanel initUsb...");
        initUsb();
        this.textBuilder = new TextBuilder(service, boardWidth, boardHeight, 20, 10);
    }

    public int getFrameRate() {
        return 50;
    }

    public int getMultiplier4Speed() {
        if (service.boardState.displayTeensy == BoardState.TeensyType.teensy4)
            return 1; // dkw need to config this
        else
            return 2; // dkw need to config this

    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardWSPanel.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardWSPanel.BoardCallbackGetBatteryLevel();
        mListener.attach(8, getBatteryLevelCallback);

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

    public int[] getPixelBuffer() {
        return mBoardScreen;
    }

    public void flush() {

        this.logFlush();
        int powerLimitMultiplierPercent = findPowerLimitMultiplierPercent(20);
        int[] mOutputScreen = this.textBuilder.renderText(mBoardScreen);
        this.appDisplay.send(mOutputScreen, mDimmerLevel);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            //BLog.d(TAG, "strip:" + s);
            int[] stripPixels = new int[kPanelHeight * 3 * kSubStrips];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < kPanelHeight * 3 * kSubStrips; ) {
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
            }
            if (s == 0) {
                setStrip(0, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 1) {
                setStrip(1, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 2) {
                setStrip(2, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 3) {
                setStrip(3, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 4) {
                setStrip(12, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 5) {
                setStrip(13, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 6) {
                setStrip(14, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 7) {
                setStrip(15, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 8) {
                setStrip(8, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 9) {
                setStrip(9, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 10) {
                //setStrip(6, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 11) {
                setStrip(7, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 12) {
                //setStrip(15, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 13) {
                //setStrip(15, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 14) {
                //setStrip(15, stripPixels, powerLimitMultiplierPercent);
            } else if (s == 15) {
                //setStrip(15, stripPixels, powerLimitMultiplierPercent);
            } else {
                setStrip(s, stripPixels, powerLimitMultiplierPercent);
            }
            // Send to board
            flush2Board();
        }

        // Render on board
        update();
        flush2Board();
        try {
            Thread.sleep(20);
        } catch (Exception e) {
        }

    }

    //    cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 7
    public void showBattery() {

        this.appDisplay.sendVisual(9);
        BLog.d(TAG, "sendCommand: 7");
        if (mListener != null) {
            mListener.sendCmd(7);
            mListener.sendCmdEnd();
            flush2Board();
            return;
        }
        return;
    }

    public void setMsg(String msg) {
    }

    static int pixelMap2Board(int s, int offset) {
        return pixelMap2BoardTable[s][offset];
    }

    private void pixelRemap(int x, int y, int stripNo, int stripOffset) {
        pixelMap2BoardTable[stripNo][stripOffset] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[stripNo][stripOffset + 1] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[stripNo][stripOffset + 2] =
                pixel2Offset(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
    }


    static int[][] pixelMap2BoardTable = new int[kMaxStrips][kMaxStripLength * 3];
    private TranslationMap[] boardMap;

    private void initpixelMap2Board() {

        BLog.d(TAG, "initmap");

        if (kStackedY) {
            BLog.d(TAG, "kstacked");
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight; y++) {

                    final int subStrip = x % kSubStrips;
                    final int stripNo = x / kSubStrips;
                    final boolean stripUp = subStrip % 2 == 0;
                    int stripOffset;

                    if (stripUp) {
                        stripOffset = subStrip * boardHeight + y;
                    } else {
                        stripOffset = subStrip * boardHeight + (boardHeight - 1 - y);
                    }
                    //BLog.d(TAG, "pixel remap " + x + ',' + y + " : " + stripNo + "," + stripOffset * 3);
                    pixelRemap(x, y, stripNo, stripOffset * 3);
                }
            }
        } else {

            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight; y++) {

                    final int nStacks = boardHeight / kPanelHeight;
                    final int subStrip = x % kSubStrips;
                    final int stripNo = y / kPanelHeight;
                    final boolean stripUp = subStrip % 2 == 0;
                    final int panelY = y % kPanelHeight;

                    if (boardHeight < kPanelHeight) {
                        BLog.d(TAG, "Board dims wrong");
                    }
                    int stripOffset;

                    if (stripUp) {
                        stripOffset = subStrip * kPanelHeight + panelY;
                    } else {
                        stripOffset = subStrip * kPanelHeight + (kPanelHeight - 1 - panelY);
                    }
                    //BLog.d(TAG, "pixel remap " + x + ',' + y + " : " + stripNo + "," + stripOffset * 3);
                    pixelRemap(x, y, stripNo, stripOffset * 3);
                }

            }
        }

    }
}

