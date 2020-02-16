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

public class BurnerBoardPanel extends BurnerBoard {
    long lastFlushTime = java.lang.System.currentTimeMillis();
    private String TAG = this.getClass().getSimpleName();
    private int[] mLayeredScreen;
    private int flushCnt = 0;

    public BurnerBoardPanel(BBService service) {
        super(service);
        boardWidth = 32;
        boardHeight = 64;
        super.setTextBuffer(boardWidth, boardHeight);
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

    public void flush() {

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            BLog.d(TAG, "Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }

        // Here we calculate the total power percentage of the whole board
        // We want to limit the board to no more than 50% of pixel output total
        // This is because the board is setup to flip the breaker at 200 watts
        // Output is percentage multiplier for the LEDs
        // At full brightness we limit to 30% of their output
        // Power is on-linear to pixel brightness: 37% = 50% power.
        // powerPercent = 100: 15% multiplier
        // powerPercent <= 15: 100% multiplier
        int totalBrightnessSum = 0;
        int powerLimitMultiplierPercent = 100;
        for (int pixel = 0; pixel < mBoardScreen.length; pixel++) {
            // R
            if (pixel % 3 == 0) {
                totalBrightnessSum += mBoardScreen[pixel];
            } else if (pixel % 3 == 1) {
                totalBrightnessSum += mBoardScreen[pixel];
            } else {
                totalBrightnessSum += mBoardScreen[pixel] / 2;
            }
        }

        final int powerPercent = totalBrightnessSum / mBoardScreen.length * 100 / 255;
        powerLimitMultiplierPercent = 100 - java.lang.Math.max(powerPercent - 15, 0);

        // Render text on board
        int[] mOutputScreen;
        if (renderText(mLayeredScreen, mBoardScreen) != null) {
            mOutputScreen = mLayeredScreen;
        } else {
            mOutputScreen = mBoardScreen;
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
            //setRowVisual(y, rowPixels);
            setRow(y, rowPixels);
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
}