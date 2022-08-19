package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.util.HashMap;
import java.util.Map;

//BBWSPanel is a string of WS28xx leds that go up and down
public class BurnerBoardWSPanel extends BurnerBoard {

    
    private String TAG = this.getClass().getSimpleName();
     // Limits of the hardware strips:
    private static final int kMaxStrips = 24;
    private static final int kMaxStripLength = 1024;
    public static Map<Integer, Integer> remap;
    // Serpentine in Y/Height direction
    // Number of logical strips in each strip
    static int logicalStripsPerPhsyicalStrip = 2;
    // Number of hardware strips
    static int kStrips = 10; //boardHeight / panel height
    static int[][] pixelMap2BoardTable = new int[kMaxStrips][kMaxStripLength * 3];

    static {
        textSizeHorizontal = 20;
        textSizeVertical = 10;
        enableBatteryMonitoring = false;
        enableIOTReporting = false;
        renderTextOnScreen = true;
        boardType = BoardState.BoardType.wspanel;
        renderLineOnScreen = true;
    }

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
        remap.put(9, 9); // top row goes away
    }

    public BurnerBoardWSPanel(BBService service) {
        super(service);
        boardWidth = 20;
        boardHeight = 180;
        BLog.i(TAG, "Burner Board WSPanel initting...");
        initpixelMap2Board();
    }

    public int getFrameRate() {
        return 50;
    }

    public void setOtherlightsAutomatically() {
    }

    public int getMultiplier4Speed() {
        if (service.boardState.displayTeensy == BoardState.TeensyType.teensy4)
            return 1; // dkw need to config this
        else
            return 2; // dkw need to config this

    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardWSPanel.BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(false);
        mListener.attach(8, getBatteryLevelCallback);

    }

    private int doRemap(int i) {

        if (remap.containsKey(i)) {
            return remap.get(i);
        } else {
            BLog.e(TAG, "missing strip " + i);
            return -1;
        }
    }

    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = PixelDimmer.Dim(20, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            //BLog.d(TAG, "strip:" + s);
            int[] stripPixels = new int[boardHeight * 3 * logicalStripsPerPhsyicalStrip];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < boardHeight * 3 * logicalStripsPerPhsyicalStrip; ) {
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mOutputScreen[pixelMap2BoardTable[s][offset++]];
            }

            stripPixels = reverseLogicalStripHalves(stripPixels);

            int remappedStrip = doRemap(s);

            if (remappedStrip == -1) {
                BLog.e(TAG, "Remapped strip out of range: " + remappedStrip);
            } else {
                setStrip(doRemap(s), stripPixels);
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

    private void pixelRemap(int x, int y, int stripNo, int stripOffset) {
        pixelMap2BoardTable[stripNo][stripOffset] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_RED);
        pixelMap2BoardTable[stripNo][stripOffset + 1] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_GREEN);
        pixelMap2BoardTable[stripNo][stripOffset + 2] = this.pixelOffset.Map(boardWidth - 1 - x, boardHeight - 1 - y, PIXEL_BLUE);
    }

    private int[] reverseLogicalStripHalves(int[] strip) {

        int[] newStrip = new int[boardHeight * 3 * logicalStripsPerPhsyicalStrip];

        int n = strip.length;

        int[] a = new int[(n + 1) / 2];
        int[] b = new int[n - a.length];

        System.arraycopy(strip, 0, a, 0, a.length);
        System.arraycopy(strip, a.length, b, 0, b.length);

        System.arraycopy(b, 0, newStrip, 0, b.length);
        System.arraycopy(a, 0, newStrip, b.length, a.length);

        return newStrip;

    }

    public void initpixelMap2Board() {

        BLog.d(TAG, "initmap");

        BLog.d(TAG, "kstacked");
        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {

                final int subStrip = x % logicalStripsPerPhsyicalStrip;
                final int stripNo = x / logicalStripsPerPhsyicalStrip;
                final boolean stripUp = subStrip % 2 == 1;
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
    }
}

