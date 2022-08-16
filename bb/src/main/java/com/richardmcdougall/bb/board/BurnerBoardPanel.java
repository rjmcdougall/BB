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

public class BurnerBoardPanel extends BurnerBoard {
    private String TAG = this.getClass().getSimpleName();

    static {
        boardWidth = 32;
        boardHeight = 64;
        textSizeHorizontal = 12;
        textSizeVertical = 12;
        enableBatteryMonitoring = true;
        enableIOTReporting = true;
        renderTextOnScreen = true;
        boardType = BoardState.BoardType.panel;
        renderLineOnScreen = true;
    }

    public BurnerBoardPanel(BBService service) {
        super(service);
        BLog.i(TAG, "Burner Board Panel initting...");
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
    public void initpixelMap2Board() {

    }
    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = PixelDimmer.Dim(15, mOutputScreen);
        this.appDisplay.send(mOutputScreen);

        int[] rowPixels = new int[boardWidth * 3];
        for (int y = 0; y < boardHeight; y++) {
            //for (int y = 30; y < 31; y++) {
            for (int x = 0; x < boardWidth; x++) {
                if (y < boardHeight) {
                    rowPixels[(boardWidth - 1 - x) * 3 + 0] = mOutputScreen[this.pixelOffset.Map(x, y, PIXEL_RED)];
                    rowPixels[(boardWidth - 1 - x) * 3 + 1] = mOutputScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)];
                    rowPixels[(boardWidth - 1 - x) * 3 + 2] = mOutputScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)];
                }
            }
            setStrip(y, rowPixels);
        
        }

        update();
        flush2Board();

    }
}