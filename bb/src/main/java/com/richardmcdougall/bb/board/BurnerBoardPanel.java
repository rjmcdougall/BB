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
import java.nio.ByteBuffer;

public class BurnerBoardPanel extends BurnerBoard {
    private String TAG = this.getClass().getSimpleName();
    private PixelDimmer mDimmer = new PixelDimmer();

    static {
        textSizeHorizontal = 32;
        textSizeVertical = 32;
        enableBatteryMonitoring = true;
        enableIOTReporting = true;
        enableMotionMonitoring = true;
        renderTextOnScreen = true;
        boardType = BoardState.BoardType.panel;
        renderLineOnScreen = true;
    }

    public BurnerBoardPanel(BBService service) {
        super(service);
        boardWidth = 32;
        boardHeight = 64;
        init(boardWidth, boardHeight);
        initpixelMap2Board();
        BLog.i(TAG, "Burner Board Panel initting...");
    }

    @Override
    public int getMultiplier4Speed() {
        return 1;
    }

    public int getFrameRate() {
        return 12;
    }

    public void setOtherlightsAutomatically() {
    }

    ;

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardPanel.BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(false);
        mListener.attach(8, getBatteryLevelCallback);

    }

    public void initpixelMap2Board() {
        //pixelOffset = new PixelOffset(this);
    }

    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = this.batteryOverlayBuilder.renderBattery(mOutputScreen);
        mOutputScreen = mDimmer.Dim(15, mOutputScreen);
        //this.appDisplay.send(mOutputScreen);

        try {
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
                flush2Board();
            }
        } catch (Exception e) {
            BLog.e(TAG, "flush for " + boardWidth + "," + boardHeight + " failed: " + e.getMessage());
        }
        update();
        flush2Board();

    }


    private ByteBuffer mWriteBuffer = ByteBuffer.allocate(16384);
    private static final byte[] teensyPrefix = "10,".getBytes();
    private static final byte[] teensyPostfix = ";".getBytes();
    private static final byte[] teensyUpdate = "6;".getBytes();

    public void setStrip(int strip, int[] pixels) {
        int len = Math.min(boardWidth * 3, pixels.length);
        byte[] newPixels = new byte[len];
        for (int pixel = 0; pixel < len; pixel++) {
            newPixels[pixel] = (byte) (((pixels[pixel] == 0) ||
                    (pixels[pixel] == ';') ||
                    (pixels[pixel] == ',') ||
                    (pixels[pixel] == '\\')) ? pixels[pixel] + 1 : pixels[pixel]);
        }

        mWriteBuffer.put(teensyPrefix);
        mWriteBuffer.put(String.format("%d,", strip).getBytes());
        mWriteBuffer.put(newPixels);
        mWriteBuffer.put(teensyPostfix);

    }

    public boolean update() {
        try {
            mWriteBuffer.put(teensyUpdate);

        } catch (Exception e) {
        }
        return true;
    }

    public void flush2Board() {
        try {
            int len = mWriteBuffer.position();
            if (len > 0) {
                byte[] buffer = new byte[len];
                mWriteBuffer.rewind();
                mWriteBuffer.get(buffer, 0, len);
                mWriteBuffer.clear();
                //BLog.d(TAG, "write " + len + " bytes");
                mSerialIoManager.writeAsync(buffer);
                //mSerialIoManager.flush();
            }
        } catch (Exception e) {
        }
    }

}