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
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;

public class BurnerBoardPanel extends BurnerBoard {
    private String TAG = this.getClass().getSimpleName();

    public BurnerBoardPanel(BBService service) {
        super(service);
        boardWidth = 32;
        boardHeight = 64;
        this.textBuilder = new TextBuilder(service, boardWidth, boardHeight, 12, 12);

        super.setDrawBuffer(boardWidth, boardHeight);
        
        boardType = "Burner Board Panel";
        BLog.d(TAG, "Burner Board Panel initting...");
        mBoardScreen = new int[boardWidth * boardHeight * 3];
        initPixelOffset();
        this.appDisplay = new AppDisplay(service, boardWidth, boardHeight, this.pixel2OffsetTable);

        initUsb();
    }

    @Override
    public int getMultiplier4Speed() {
        return 3;
    }

    public int getFrameRate() {
        return 12;
    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardPanel.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardPanel.BoardCallbackGetBatteryLevel();
        mListener.attach(8, getBatteryLevelCallback);

    }

    public void flush() {

        this.logFlush();
        int powerLimitMultiplierPercent = findPowerLimitMultiplierPercent(15);
        int[] mOutputScreen = this.textBuilder.renderText(mBoardScreen);
        this.appDisplay.send(mOutputScreen, mDimmerLevel);

        int[] rowPixels = new int[boardWidth * 3];
        for (int y = 0; y < boardHeight; y++) {
            //for (int y = 30; y < 31; y++) {
            for (int x = 0; x < boardWidth; x++) {
                if (y < boardHeight) {
                    rowPixels[(boardWidth - 1 - x) * 3 + 0] = mOutputScreen[pixel2Offset(x, y, PIXEL_RED)];
                    rowPixels[(boardWidth - 1 - x) * 3 + 1] = mOutputScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                    rowPixels[(boardWidth - 1 - x) * 3 + 2] = mOutputScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                }
            }
            setStrip(y, rowPixels, powerLimitMultiplierPercent);
        
        }

        update();
        flush2Board();

    }

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