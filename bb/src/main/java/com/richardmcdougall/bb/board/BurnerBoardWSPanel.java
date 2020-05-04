package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        BLog.i(TAG, "Burner Board WSPanel initting...");
        boardWidth = kBoardWidth;
        boardHeight = kBoardHeight;
        boardScreen = new int[boardWidth * boardHeight * 3];
        initPixelOffset();
        initpixelMap2Board();
        this.appDisplay = new AppDisplay(service, boardWidth, boardHeight, this.pixel2OffsetTable);
        this.textBuilder = new TextBuilder(service, boardWidth, boardHeight, 20, 10);
        this.lineBuilder = new LineBuilder(service,boardWidth, boardHeight);
        initUsb();
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

    public static Map<Integer, Integer> remap;
    static {
        remap = new HashMap<>();
        remap.put(0, 0);
        remap.put(1, 1);
        remap.put(2, 2);
        remap.put(3, 3);
        remap.put(4, 12);
        remap.put(5, 13);
        remap.put(6, 14);
        remap.put(7, 15);
        remap.put(8, 8);
        remap.put(9, 9);
        //remap.put(10, 6);
        //remap.put(11, 7);
        //remap.put(12, 15);
        //remap.put(13, 15);
        //remap.put(14, 15);
        //remap.put(15, 15);
        remap.put(16, 16);
        remap.put(17, 17);
        remap.put(18, 18);
        remap.put(19, 19);
    }

    private int doRemap(int i){

        if(remap.containsKey(i)){
            return remap.get(i);
        }
        else {
            BLog.e(TAG, "missing strip " + i);
            return 0;
        }
    }

    public void flush() {

        this.logFlush();
        int[] mOutputScreen = this.textBuilder.renderText(boardScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = PixelDimmer.Dim(20, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

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

            setStrip(doRemap(s), stripPixels);

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

