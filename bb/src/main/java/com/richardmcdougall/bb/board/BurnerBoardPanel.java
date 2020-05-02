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

//test panel used by supersex, cranky, and others.
public class BurnerBoardPanel extends BurnerBoard {
    long lastFlushTime = java.lang.System.currentTimeMillis();
    private String TAG = this.getClass().getSimpleName();
    private int[] mLayeredScreen;
    private int flushCnt = 0;

    public BurnerBoardPanel(BBService service) {
        super(service);
        boardWidth = 32;
        boardHeight = 64;
        this.textBuilder = new TextBuilder(boardWidth, boardHeight, 12, 12);
        super.setDrawBuffer(boardWidth, boardHeight);
        
        boardType = "Burner Board Panel";
        BLog.d(TAG, "Burner Board Panel initting...");
        mBoardScreen = new int[boardWidth * boardHeight * 3];
        initPixelOffset();
        initUsb();
        mLayeredScreen = new int[boardWidth * boardHeight * 3];
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

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            BLog.d(TAG, "Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }
 
        int powerLimitMultiplierPercent = findPowerLimitMultiplierPercent(15);

        int[] mOutputScreen = mBoardScreen;
        if(renderTextOnScreen){
            // Render text on board
            if (renderText(mLayeredScreen, mBoardScreen) != null) {
                mOutputScreen = mLayeredScreen;
            }
        }

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
            setRowVisual(y, rowPixels);
        }

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

        sendVisual(9);
        BLog.d(TAG, "sendCommand: 7");
        if (mListener != null) {
            mListener.sendCmd(7);
            mListener.sendCmdEnd();
            flush2Board();
            return;
        }
        return;
    }

    private boolean setRow(int row, int[] pixels) {

        int[] dimPixels = new int[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            dimPixels[pixel] =
                    (mDimmerLevel * pixels[pixel]) / 255;
        }

        // Do color correction on burner board display pixels
        byte[] newPixels = new byte[boardWidth * 3];
        for (int pixel = 0; pixel < boardWidth * 3; pixel = pixel + 3) {
            newPixels[pixel] = (byte) pixelColorCorrectionRed(dimPixels[pixel]);
            newPixels[pixel + 1] = (byte) pixelColorCorrectionGreen(dimPixels[pixel + 1]);
            newPixels[pixel + 2] = (byte) pixelColorCorrectionBlue(dimPixels[pixel + 2]);
        }

        //System.out.println("flush row:" + y + "," + bytesToHex(newPixels));

        //l("sendCommand: 10,n,...");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(10);
                mListener.sendCmdArg(row);
                mListener.sendCmdEscArg(newPixels);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
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