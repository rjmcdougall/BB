package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;

// used by the now-defunct josPaks
public class BurnerBoardDirectMap extends BurnerBoard {

    private String TAG = this.getClass().getSimpleName();

    /* DIRECT MAP SETTINGS */
    // JosPacks have 1x166 strands of LEDs. Currently RPI == JosPack
    private static final int kVisualizationDirectMapDefaultWidth = 8;
    public static final int kVisualizationDirectMapWidth = BoardState.kIsRPI ? 1 : kVisualizationDirectMapDefaultWidth;
    private static final int kVisualizationDirectMapDefaultHeight = 256;
    public static final int kVisualizationDirectMapHeight = BoardState.kIsRPI ? 166 : kVisualizationDirectMapDefaultHeight;
    static int kStrips = 8;

    public BurnerBoardDirectMap(BBService service, int width, int height) {
        super(service);
        BLog.i(TAG," Direct Map initing ");
        boardWidth = width;
        boardHeight = height;
        boardScreen = new int[boardWidth * boardHeight * 3];
        this.boardDisplay = new BoardDisplay(this.service, boardWidth, boardHeight);
        initPixelOffset();
        //map2board
        this.appDisplay = new AppDisplay(service, boardWidth, boardHeight, this.pixel2OffsetTable);
        this.textBuilder = new TextBuilder(service, boardWidth, boardHeight, 0,0);
        this.lineBuilder = new LineBuilder(service,boardWidth, boardHeight);
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

        this.logFlush();
        int[] mOutputScreen = this.textBuilder.renderText(boardScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = PixelDimmer.Dim(200, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[boardHeight * 3];
            // Walk through all the pixels in the strip
            for (int y = 0; y < boardHeight; y++) {
                stripPixels[y * 3] = mOutputScreen[pixel2Offset(s, y, PIXEL_RED)];
                stripPixels[y * 3 + 1] = mOutputScreen[pixel2Offset(s, y, PIXEL_GREEN)];
                stripPixels[y * 3 + 2] = mOutputScreen[pixel2Offset(s, y, PIXEL_BLUE)];
            }
            setStrip(s, stripPixels);
            // Send to board
            flush2Board();
        }

        // Render on board
        update();
        flush2Board();

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
