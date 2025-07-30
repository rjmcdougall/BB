package com.richardmcdougall.bb.board;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.nio.ByteBuffer;

//  cmdMessenger.sendCmdStart(BBGetBatteryLevel);
//          cmdMessenger.sendCmdArg(batteryControl);
//          cmdMessenger.sendCmdArg(batteryStateOfCharge);
//          cmdMessenger.sendCmdArg(batteryMaxError);
//          cmdMessenger.sendCmdArg(batteryRemainingCapacity);
//          cmdMessenger.sendCmdArg(batteryFullChargeCapacity);
//          cmdMessenger.sendCmdArg(batteryVoltage);
//          cmdMessenger.sendCmdArg(batteryAverageCurrent);
//          cmdMessenger.sendCmdArg(batteryTemperature);
//          cmdMessenger.sendCmdArg(batteryFlags);
//          cmdMessenger.sendCmdArg(batteryCurrent);
//          cmdMessenger.sendCmdArg(batteryFlagsB);
//          cmdMessenger.sendCmdEnd();

public class BurnerBoardLittleWing extends BurnerBoard {

    private String TAG = this.getClass().getSimpleName();
    static int kStrips = 16;
    static int[] pixelsPerStrip = new int[16];
    static int[][] pixelMap2BoardTable = new int[16][4096];
    private TranslationMap[] boardMap;
    private PixelDimmer mDimmer = new PixelDimmer();


    private ByteBuffer mWriteBuffer = ByteBuffer.allocate(16384);
    private static final byte[] teensyPrefix = "10,".getBytes();
    private static final byte[] teensyPostfix = ";".getBytes();
    private static final byte[] teensyUpdate = "6;".getBytes();


    static {
        textSizeHorizontal = 14;
        textSizeVertical = 20;
        enableBatteryMonitoring = true;
        enableIOTReporting = true;
        renderTextOnScreen = true;
        boardType = BoardState.BoardType.littlewing;
        renderLineOnScreen = false;
    }

    public BurnerBoardLittleWing(BBService service) {
        super(service);
        init(boardWidth, boardHeight);
        initpixelMap2Board();
        boardWidth = 32;
        boardHeight = 255;
        BLog.i(TAG, "Burner Board Little Wing initting...");
    }

    public int getMultiplier4Speed() {
        return 5; // dkw need to config this

    }

    public void start() {

        // attach getBatteryLevel cmdMessenger callback
        BoardCallbackGetBatteryLevel getBatteryLevelCallback = new BoardCallbackGetBatteryLevel(true);
        mListener.attach(8, getBatteryLevelCallback);

    }

    public int getFrameRate() {
            return 20;
    }

    public void setOtherlightsAutomatically() {
    }

    public void flush() {

        this.logFlush();
        int[] mOutputScreen = boardScreen.clone();
        mOutputScreen = this.textBuilder.renderText(mOutputScreen);
        mOutputScreen = this.lineBuilder.renderLine(mOutputScreen);
        mOutputScreen = mDimmer.Dim(3, mOutputScreen);

        // Walk through each strip and fill from the graphics buffer
        // last two strips are emulated side strips
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[pixelsPerStrip[s] * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; ) {
                stripPixels[offset] = pixelWorkaround(mOutputScreen[pixelMap2BoardTable[s][offset++]]);
                stripPixels[offset] = pixelWorkaround(mOutputScreen[pixelMap2BoardTable[s][offset++]]);
                stripPixels[offset] = pixelWorkaround( mOutputScreen[pixelMap2BoardTable[s][offset++]]);
            }
            setStrip(s, stripPixels);
            // TODO: something takes a shit upstream if too much data is outstanding before a flush
            //if (s == 8) {
                flush2Board();
            //}
        }
        // Render on board
        update();
        flush2Board();

    }

    private void pixelRemap(int x, int y, int stripOffset) {
        pixelMap2BoardTable[boardMap[x].stripNumber - 1][stripOffset] = this.pixelOffset.Map(x, y, PIXEL_RED);
        pixelMap2BoardTable[boardMap[x].stripNumber - 1][stripOffset + 1] = this.pixelOffset.Map(x, y, PIXEL_GREEN);
        pixelMap2BoardTable[boardMap[x].stripNumber - 1][stripOffset + 2] = this.pixelOffset.Map( x,  y, PIXEL_BLUE);
    }

    public void initpixelMap2Board() {
        int x, y;

        boardWidth = 32;
        boardHeight = 255;
        BLog.d(TAG, "initpixelMap2Board");
        boardMap = TranslationMapBag.littlewing;
        pixelOffset = new PixelOffset(this);
        this.pixelOffset.initPixelOffset();
        this.clearPixels();

        // Walk through all the strips and find the number of pixels in the strip
        for (int s = 0; s < kStrips; s++) {
            pixelsPerStrip[s] = 0;
            // Search strips and find longest pixel count
            for (int i = 0; i < boardMap.length; i++) {
                int endPixel = Math.abs(boardMap[i].endY - boardMap[i].startY) + 1 + boardMap[i].stripOffset;
                if (s == (boardMap[i].stripNumber - 1) && endPixel > pixelsPerStrip[s]) {
                    pixelsPerStrip[s] = endPixel;
                    //BLog.i(TAG, "boardmap: strip " + s + " has " + pixelsPerStrip[s] + " pixels" );
                }
            }
        }

        for (x = 0; x < boardMap.length; x++) {
            if (boardMap[x].stripDirection == 1) {
                // Strip has y1 ... y2
                for (y = boardMap[x].startY; y <= boardMap[x].endY; y++) {
                    int stripOffset = boardMap[x].stripOffset + y - boardMap[x].startY;
                    pixelRemap(x, y, stripOffset * 3);
                }
            } else {
                // Strip has y2 ... y1 (reverse order)
                for (y = boardMap[x].endY; y <= boardMap[x].startY; y++) {
                    int stripOffset = boardMap[x].stripOffset - y + boardMap[x].startY;
                    pixelRemap(x, y, stripOffset * 3);
                }
            }
        }
        /*
        for (int s = 0; s < kStrips; s++) {
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s] * 3; offset++) {
                BLog.i(TAG, "Strip " + s + " offset " + offset + " =  pixel offset " + pixelMap2BoardTable[s][offset]);
            }
        }
        */
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

    public boolean update() {
        try {
            mWriteBuffer.put(teensyUpdate);

        } catch (Exception e) {
        }
        return true;
    }

    public void setStrip(int strip, int[] pixels) {
        //BLog.d(TAG, "setstrip " + strip + " pixels length " + pixels.length);
        int len = Math.min(600 * 3, pixels.length);
        byte[] newPixels = new byte[len];
        for (int pixel = 0; pixel < len; pixel++) {
            newPixels[pixel] = (byte) pixels[pixel];
        }

        mWriteBuffer.put(teensyPrefix);
        mWriteBuffer.put(String.format("%d,", strip).getBytes());
        mWriteBuffer.put(newPixels);
        mWriteBuffer.put(teensyPostfix);

    }

    // Work around that teensy cmd essenger can't deal with 0, ',', ';', '\\'
    // Optimized version with minimal branching for high-frequency calls
    private static int pixelWorkaround(int level) {
        // Fast path for most common case (0-255 values except problematic ones)
        if (level == 0) return 1;
        if (level == 44) return 45;  // ',' = 44
        if (level == 59) return 60;  // ';' = 59
        if (level == 92) return 93;  // '\\' = 92
        return level;
    }
}