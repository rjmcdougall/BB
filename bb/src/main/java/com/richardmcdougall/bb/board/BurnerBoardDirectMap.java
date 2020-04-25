package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;

import java.nio.IntBuffer;

// used by the now-defunct josPaks
public class BurnerBoardDirectMap extends BurnerBoard {

    private static final String TAG = "BB.BurnerBoardDirectMap";

    /* JosPacks have more of a power constraint, so we don't want to set it to full brightness. Empirically tested
        with with a rapidly refreshing pattern (BlueGold):
        100 -> 1.90a draw
        50  -> 0.50a draw
        25  -> 0.35a draw
    */
    private static final int kVisualizationDirectMapPowerMultiplier = BoardState.kIsRPI ? 25 : 100; // should be ok for nano
    private static final int mPowerMultiplier = kVisualizationDirectMapPowerMultiplier;

    /* DIRECT MAP SETTINGS */
    // JosPacks have 1x166 strands of LEDs. Currently RPI == JosPack
    private static final int kVisualizationDirectMapDefaultWidth = 8;
    public static final int kVisualizationDirectMapWidth = BoardState.kIsRPI ? 1 : kVisualizationDirectMapDefaultWidth;
    private static final int kVisualizationDirectMapDefaultHeight = 256;
    public static final int kVisualizationDirectMapHeight = BoardState.kIsRPI ? 166 : kVisualizationDirectMapDefaultHeight;
    static int kStrips = 8;
    long lastFlushTime = java.lang.System.currentTimeMillis();
    private int flushCnt = 0;

    public BurnerBoardDirectMap(BBService service, int width, int height) {
        super(service);
        boardWidth = width;
        boardHeight = height;

        mBoardScreen = new int[boardWidth * boardHeight * 3];

        boardType = "Burner Board DirectMap";
        BLog.d(TAG, boardType + " initializing at: " + boardWidth + " x " + boardHeight);

        this.textBuilder = new TextBuilder(boardWidth, boardHeight, 0,0);

        initPixelOffset();
        initUsb();
    }

    @Override
    public int getMultiplier4Speed() {
        return 3;
    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardDirectMap.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardDirectMap.BoardCallbackGetBatteryLevel();
        mListener.attach(8, getBatteryLevelCallback);

    }

    public void flush() {

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            BLog.d(TAG, "Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }

        // Suppress updating when displaying a text message
        if (this.textBuilder.isTextDisplaying > 0) {
            this.textBuilder.isTextDisplaying--;
        } else {

            int powerLimitMultiplierPercent = mPowerMultiplier;

            int[] rowPixels = new int[boardWidth * 3];
            for (int y = 0; y < boardHeight; y++) {
                //for (int y = 30; y < 31; y++) {
                for (int x = 0; x < boardWidth; x++) {
                    if (y < boardHeight) {
                        rowPixels[x * 3 + 0] = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                        rowPixels[x * 3 + 1] = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                        rowPixels[x * 3 + 2] = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                    }
                }
                //setRowVisual(y, rowPixels);
            }

            // Walk through each strip and fill from the graphics buffer
            for (int s = 0; s < kStrips; s++) {
                int[] stripPixels = new int[boardHeight * 3];
                // Walk through all the pixels in the strip
                for (int y = 0; y < boardHeight; y++) {
                    stripPixels[y * 3] = mBoardScreen[pixel2Offset(s, y, PIXEL_RED)];
                    stripPixels[y * 3 + 1] = mBoardScreen[pixel2Offset(s, y, PIXEL_GREEN)];
                    stripPixels[y * 3 + 2] = mBoardScreen[pixel2Offset(s, y, PIXEL_BLUE)];
                }
                setStrip(s, stripPixels, powerLimitMultiplierPercent);
                // Send to board
                flush2Board();
            }

            // Render on board
            update();
            flush2Board();
        }
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
}
