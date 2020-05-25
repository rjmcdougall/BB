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
        this.boardWidth = width;
        this.boardHeight = height;
        this.textSizeVertical = 0;
        this.textSizeHorizontal = 0;
        this.boardScreen = new int[boardWidth * boardHeight * 3];
        this.boardDisplay = new BoardDisplay(this.service, this);
        this.pixelOffset = new PixelOffset(this);
        //map2board
        this.appDisplay = new AppDisplay(service, this);
        this.textBuilder = new TextBuilder(service, this);
        this.lineBuilder = new LineBuilder(service, this);
        initUsb();
    }

    @Override
    public int getMultiplier4Speed() {
        return 3;
    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardDirectMap.BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(false);
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
                stripPixels[y * 3] = mOutputScreen[this.pixelOffset.Map(s, y, PIXEL_RED)];
                stripPixels[y * 3 + 1] = mOutputScreen[this.pixelOffset.Map(s, y, PIXEL_GREEN)];
                stripPixels[y * 3 + 2] = mOutputScreen[this.pixelOffset.Map(s, y, PIXEL_BLUE)];
            }
            setStrip(s, stripPixels);
            // Send to board
            flush2Board();
        }

        // Render on board
        update();
        flush2Board();

    }
}
