package com.richardmcdougall.bb;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by rmc on 7/25/17.
 */


/*

   Back
         46,118
 +------+
 |      | strip 1
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
 |      | strip 8
 +------+
 0,0 X
 Front

 */


public class BurnerBoardAzul extends BurnerBoard {
    private int mBoardWidth = 46;
    private int mBoardHeight = 118;
    //public int[] mBoardScreen;
    private int mDimmerLevel = 255;
    private static final String TAG = "BurnerBoardAzul";
    public int mBatteryLevel;
    public String boardId = Build.MODEL;


    public BurnerBoardAzul(BBService service, Context context) {
        super(service, context);
        boardType = "Burner Board Azul";
        l("Burner Board Azul initing...");
        mBoardScreen = new int[mBoardWidth * mBoardHeight * 3];
        mBBService = service;
        mContext = context;
        initPixelOffset();
        initpixelMap2Board();
        initUsb();
    }

    public void start() {
        
        // attach default cmdMessenger callback
        BurnerBoardAzul.BoardCallbackDefault defaultCallback =
                new BurnerBoardAzul.BoardCallbackDefault();
        mListener.attach(defaultCallback);

        // attach Test cmdMessenger callback
        BurnerBoardAzul.BoardCallbackTest testCallback =
                new BurnerBoardAzul.BoardCallbackTest();
        mListener.attach(5, testCallback);

        // attach Mode cmdMessenger callback
        BurnerBoardAzul.BoardCallbackMode modeCallback =
                new BurnerBoardAzul.BoardCallbackMode();
        mListener.attach(4, modeCallback);

        // attach Mode cmdMessenger callback
        BurnerBoardAzul.BoardCallbackBoardID boardIDCallback =
                new BurnerBoardAzul.BoardCallbackBoardID();
        mListener.attach(11, boardIDCallback);

        // attach echoRow cmdMessenger callback
        BurnerBoardAzul.BoardCallbackEchoRow echoCallback =
                new BurnerBoardAzul.BoardCallbackEchoRow();
        mListener.attach(17, echoCallback);

        // attach getBatteryLevel cmdMessenger callback
        BurnerBoardAzul.BoardCallbackGetBatteryLevel getBatteryLevelCallback =
                new BurnerBoardAzul.BoardCallbackGetBatteryLevel();
        mListener.attach(8, getBatteryLevelCallback);

    }

    public class BoardCallbackDefault implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            Log.d(TAG, "ardunio default callback:" + str);
        }
    }

    public class BoardCallbackTest implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            l("ardunio test callback:" + str);
        }
    }

    public class BoardCallbackMode implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            int boardMode = mListener.readIntArg();
            boardCallback.BoardMode(boardMode);
        }
    }

    public class BoardCallbackBoardID implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            String boardId = mListener.readStringArg();
            boardCallback.BoardId(boardId);
        }
    }

    public class BoardCallbackEchoRow implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            mEchoString = mListener.readStringArg();
            l("echoRow: " + mEchoString);
        }
    }

    public class BoardCallbackGetBatteryLevel implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            mBatteryLevel = mListener.readIntArg();
            l("getBatteryLevel: " + mBatteryLevel);
        }
    }

    public int getBattery() {
        return mBatteryLevel;
    }

    public void fuzzPixels(int amount) {

        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {
                mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                        (mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] - amount);
                mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                        (mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] - amount);
                mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                        (mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] - amount);
            }
        }
    }


    public void resetParams() {
        mDimmerLevel = 255;
    }

    // Convert from xy to buffer memory
    int pixel2OffsetCalc(int x, int y, int rgb) {
        return (y * mBoardWidth + x) * 3 + rgb;
    }


    static int pixel2Offset(int x, int y, int rgb) {
        return pixel2OffsetTable[x][y][rgb];
    }

    static int PIXEL_RED = 0;
    static int PIXEL_GREEN = 1;
    static int PIXEL_BLUE = 2;

    static int [][][] pixel2OffsetTable = new int[255][255][3];
    private void initPixelOffset() {
        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {
                for (int rgb = 0; rgb < 3; rgb++) {
                    pixel2OffsetTable[x][y][rgb] = pixel2OffsetCalc(x, y, rgb);
                }
            }
        }
    }

    public void setPixel(int x, int y, int r, int g, int b) {
        //System.out.println("Setting pixel " + x + "," + y + " : " + pixel2Offset(x, y, PIXEL_RED) + " to " + r +  "," + g + "," + b);
        //l("setPixel(" + x + "," + y + "," + r + "," + g + "," + b + ")");
        //Sstem.out.println("setpixel r = " + r);
        if (x < 0 || x >= mBoardWidth || y < 0 || y >= mBoardHeight) {
            l("setPixel out of range: " + x + "," + y);
            return;
        }
        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
        mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
        mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
    }

    public void setPixel(int pixel, int r, int g, int b) {

        if (pixel < 0 || pixel >= (mBoardWidth * mBoardHeight)) {
            l("setPixel out of range: " + pixel);
            return;
        }
        mBoardScreen[pixel * 3] = r;
        mBoardScreen[pixel * 3 + 1] = g;
        mBoardScreen[pixel * 3 + 2] = b;
    }

    public void scrollPixels(boolean down) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = 0; y < mBoardHeight - 1; y++) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
        } else {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = mBoardHeight - 2; y >= 0; y--) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
        }

    }

    public void scrollPixelsExcept(boolean down, int color) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = 0; y < mBoardHeight - 1; y++) {
                    if (getRGB(mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)],
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)],
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)]) != color) {
                        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                        mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                        mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                                mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                    }
                }
            }
        } else {
            for (int x = 0; x < mBoardWidth; x++) {
                for (int y = mBoardHeight - 2; y >= 0; y--) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
        }
    }

    public void fillScreen(int r, int g, int b) {

        //System.out.println("Fillscreen " + r + "," + g + "," + b);
        int x;
        int y;
        for (x = 0; x < mBoardWidth; x++) {
            for (y = 0; y < mBoardHeight; y++) {
                setPixel(x, y, r, g, b);
            }
        }
    }

    public void fadePixels(int amount) {

        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {
                int r = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                //System.out.println("br = " + br);
                if (r >= amount) {
                    r -= amount;
                } else {
                    r = 0;
                }
                mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
                int g = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                if (g >= amount) {
                    g -= amount;
                } else {
                    g = 0;
                }
                mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
                int b = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                if (b >= amount) {
                    b -= amount;
                } else {
                    b = 0;
                }
                mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
            }
        }
    }


    public int [] getPixelBuffer() {
        return mBoardScreen;
    }

    // TODO: gamma correction
    // encoded = ((original / 255) ^ (1 / gamma)) * 255
    // original = ((encoded / 255) ^ gamma) * 255

    // TODO: make faster by using ints
    private int pixelColorCorrectionRed(int red) {
        return gammaCorrect(red) ;
    }

    private int pixelColorCorrectionGreen(int green) {
        return gammaCorrect(green);
    }

    private int pixelColorCorrectionBlue(int blue) {
        return gammaCorrect(blue);
    }

    public void clearPixels() {
        Arrays.fill(mBoardScreen, 0);
    }

    public void dimPixels(int level) {
        mDimmerLevel = level;
    }

    private int flushCnt = 0;
    long lastFlushTime = java.lang.System.currentTimeMillis();

    public void flush() {

        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            l("Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }

        int[] rowPixels = new int[mBoardWidth * 3];
        for (int y = 0; y < mBoardHeight; y++) {
            //for (int y = 30; y < 31; y++) {
            for (int x = 0; x < mBoardWidth; x++) {
                if (y < mBoardHeight) {
                    rowPixels[x * 3 + 0] = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                    rowPixels[x * 3 + 1] = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                    rowPixels[x * 3 + 2] = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                }
            }
            setRowVisual(y, rowPixels);
        }

        // Walk through each strip and fill from the graphics buffer
        for (int s = 0; s < kStrips; s++) {
            int[] stripPixels = new int[pixelsPerStrip[s] * 3];
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s]* 3;) {
                stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
                stripPixels[offset] = mBoardScreen[pixelMap2BoardTable[s][offset++]];
            }
            setStrip(s, stripPixels);
            // Send to board
            flush2Board();
        }
        // Render on board
        update();
        flush2Board();
    }

    //    cmdMessenger.attach(BBUpdate, OnUpdate);              // 6
    public boolean update() {

        sendVisual(8);

        //l("sendCommand: 5");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmd(6);
                mListener.sendCmdEnd();
                return true;
            }
            else {
                // Emulate board's 30ms refresh time
                try {
                    Thread.sleep(5);
                } catch (Throwable e) {
                }
            }
        }

        return false;
    }


    //    cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 6
    public void showBattery() {

        sendVisual(9);
        l("sendCommand: 5");
        if (mListener != null) {
            mListener.sendCmd(7);
            mListener.sendCmdEnd();
            flush2Board();
            return;
        }
        return;
    }

    //    cmdMessenger.attach(BBsetheadlight, Onsetheadlight);  // 3
    public boolean setHeadlight(boolean state) {

        sendVisual(3);
        l("sendCommand: 3,1");
        if (mListener != null) {
            mListener.sendCmdStart(3);
            mListener.sendCmdArg(state == true ? 1 : 0);
            mListener.sendCmdEnd();
            flush2Board();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBClearScreen, OnClearScreen);    // 4
    public boolean clearScreen() {

        sendVisual(5);
        l("sendCommand: 4");
        if (mListener != null) {
            mListener.sendCmd(4);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }

    private void setRowVisual(int row, int[] pixels) {

        int[] dimPixels = new int[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            dimPixels[pixel] =
                    (mDimmerLevel * pixels[pixel]) / 255;
        }

        // Send pixel row to in-app visual display
        sendVisual(14, row, dimPixels);
    }

    // Send a strip of pixels to the board
    private void setStrip(int strip, int[] pixels) {

        int[] dimPixels = new int[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            dimPixels[pixel] =
                    (mDimmerLevel * pixels[pixel]) / 384;  // Half Brightness
        }

        // Do color correction on burner board display pixels
        byte [] newPixels = new byte[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel = pixel + 3) {
            newPixels[pixel] = (byte)pixelColorCorrectionRed(dimPixels[pixel]);
            newPixels[pixel + 1] = (byte)pixelColorCorrectionGreen(dimPixels[pixel + 1]);
            newPixels[pixel + 2] = (byte)pixelColorCorrectionBlue(dimPixels[pixel + 2]);
        }

        //newPixels[30]=(byte)128;
        //newPixels[31]=(byte)128;
        //newPixels[32]=(byte)128;
        //newPixels[3]=2;
        //newPixels[4]=40;
        //newPixels[5]=2;
        //newPixels[6]=2;
        //newPixels[7]=2;
        //newPixels[8]=40;
        //newPixels[9]=2;
        //newPixels[10]=2;
        //newPixels[11]=40;

        //System.out.println("flushPixels row:" + y + "," + bytesToHex(newPixels));

        //l("sendCommand: 14,n,...");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(10);
                mListener.sendCmdArg(strip);
                mListener.sendCmdEscArg(newPixels);
                mListener.sendCmdEnd();
            }
        }
    }

    public static class TranslationMap {
        int y;
        int startX;
        int endX;
        int stripDirection;
        int stripNumber;
        int stripOffset;

        private TranslationMap(
                int y,
                int startX,
                int endX,
                int stripDirection,
                int stripNumber,
                int stripOffset) {
            this.y = y;
            this.startX = startX;
            this.endX = endX;
            this.stripDirection = stripDirection;
            this.stripNumber = stripNumber;
            this.stripOffset = stripOffset;
        }
    }

    static int pixelMap2Board(int s, int offset)
    {
        return pixelMap2BoardTable[s][offset];
    }

    private void pixelRemap(int x, int y, int stripOffset) {
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset] =
                pixel2Offset(mBoardWidth - x, mBoardHeight - y, PIXEL_RED);
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset + 1] =
                pixel2Offset(mBoardWidth - x, mBoardHeight - y, PIXEL_GREEN);
        pixelMap2BoardTable[boardMap[y].stripNumber - 1][stripOffset + 2] =
                pixel2Offset(mBoardWidth - x, mBoardHeight - y, PIXEL_BLUE);
    }

    // Two primary mapping functions
    static int kStrips = 8;
    static int [] pixelsPerStrip = new int[8];
    static int [][] pixelMap2BoardTable = new int[8][4096];
    private TranslationMap[] boardMap;

    private void initpixelMap2Board() {
        int x, y;

        // Sadly Candy is wired up and sealed with an error
        if (boardId.contains(new String("candy"))) {
            boardMap = boardMapCandy;
        } else {
            boardMap = boardMapStd;
        }

        // Walk through all the strips and find the number of pixels in the strip
        for (int s = 0; s < kStrips; s++) {
            pixelsPerStrip[s] = 0;
            // Search strips and find longest pixel count
            for (int i = 0; i < boardMap.length; i++) {
                int endPixel = java.lang.Math.abs(boardMap[i].endX -
                        boardMap[i].startX) + 1 + boardMap[i].stripOffset;
                if (s == (boardMap[i].stripNumber - 1) && endPixel > pixelsPerStrip[s]) {
                    pixelsPerStrip[s] = endPixel;
                    //l("boardmap: strip " + s + " has " + pixelsPerStrip[s] + " pixels" );
                }
            }
        }

        for (y = 0; y < boardMap.length; y++) {
            if (boardMap[y].stripDirection == 1) {
                // Strip has x1 ... x2
                for (x = boardMap[y].startX; x <= boardMap[y].endX; x++) {
                    int stripOffset = boardMap[y].stripOffset + x - boardMap[y].startX;
                    pixelRemap(x, y, stripOffset * 3);
                }
            } else {
                // Strip has x2 ... x1 (reverse order)
                for (x = boardMap[y].endX; x <= boardMap[y].startX; x++) {
                    int stripOffset = boardMap[y].stripOffset - x + boardMap[y].startX;
                    pixelRemap(x, y, stripOffset * 3);
                }
            }
        }
        for (int s = 0; s < kStrips; s++) {
            // Walk through all the pixels in the strip
            for (int offset = 0; offset < pixelsPerStrip[s]* 3; offset++) {
                //l("Strip " + s + " offset " + offset + " =  pixel offset " + pixelMap2BoardTable[s][offset]);
            }
        }
    }

    static TranslationMap[] boardMapStd = {
//Y,StartX,End X,Direction,Strip #,Offset in strip
            new TranslationMap(0,23,22,-1,8,452),
            new TranslationMap(1,20,25,1,8,446),
            new TranslationMap(2,27,18,-1,8,436),
            new TranslationMap(3,16,29,1,8,422),
            new TranslationMap(4,30,15,-1,8,406),
            new TranslationMap(5,14,31,1,8,388),
            new TranslationMap(6,33,12,-1,8,366),
            new TranslationMap(7,11,34,1,8,342),
            new TranslationMap(8,31,14,-1,8,324),
            new TranslationMap(9,14,31,1,8,306),
            new TranslationMap(10,32,13,-1,8,286),
            new TranslationMap(11,12,33,1,8,264),
            new TranslationMap(12,34,11,-1,8,240),
            new TranslationMap(13,11,34,1,8,216),
            new TranslationMap(14,35,10,-1,8,190),
            new TranslationMap(15,9,36,1,8,162),
            new TranslationMap(16,37,8,-1,8,132),
            new TranslationMap(17,8,37,1,8,102),
            new TranslationMap(18,38,7,-1,8,70),
            new TranslationMap(19,6,39,1,8,36),
            new TranslationMap(20,40,5,-1,8,0),
            new TranslationMap(21,40,5,-1,7,0),
            new TranslationMap(22,7,38,1,7,36),
            new TranslationMap(23,38,7,-1,7,68),
            new TranslationMap(24,6,39,1,7,100),
            new TranslationMap(25,39,6,-1,7,134),
            new TranslationMap(26,5,40,1,7,168),
            new TranslationMap(27,40,5,-1,7,204),
            new TranslationMap(28,4,41,1,7,240),
            new TranslationMap(29,41,4,-1,7,278),
            new TranslationMap(30,4,41,1,7,316),
            new TranslationMap(31,42,3,-1,7,354),
            new TranslationMap(32,3,42,1,7,394),
            new TranslationMap(33,42,3,-1,7,434),
            new TranslationMap(34,2,43,1,7,474),
            new TranslationMap(35,43,2,-1,6,0),
            new TranslationMap(36,2,43,1,6,42),
            new TranslationMap(37,44,1,-1,6,84),
            new TranslationMap(38,1,44,1,6,128),
            new TranslationMap(39,44,1,-1,6,172),
            new TranslationMap(40,1,44,1,6,216),
            new TranslationMap(41,44,1,-1,6,260),
            new TranslationMap(42,1,44,1,6,304),
            new TranslationMap(43,44,1,-1,6,348),
            new TranslationMap(44,0,45,1,6,392),
            new TranslationMap(45,45,0,-1,6,438),
            new TranslationMap(46,0,45,1,6,484),
            new TranslationMap(47,45,0,-1,5,0),
            new TranslationMap(48,0,45,1,5,46),
            new TranslationMap(49,45,0,-1,5,92),
            new TranslationMap(50,0,45,1,5,138),
            new TranslationMap(51,45,0,-1,5,184),
            new TranslationMap(52,0,45,1,5,230),
            new TranslationMap(53,45,0,-1,5,276),
            new TranslationMap(54,0,45,1,5,322),
            new TranslationMap(55,45,0,-1,5,368),
            new TranslationMap(56,0,45,1,5,414),
            new TranslationMap(57,45,0,-1,5,460),
            new TranslationMap(58,0,45,1,5,506),
            new TranslationMap(59,45,0,-1,4,0),
            new TranslationMap(60,0,45,1,4,46),
            new TranslationMap(61,45,0,-1,4,92),
            new TranslationMap(62,0,45,1,4,138),
            new TranslationMap(63,45,0,-1,4,184),
            new TranslationMap(64,0,45,1,4,230),
            new TranslationMap(65,45,0,-1,4,276),
            new TranslationMap(66,0,45,1,4,322),
            new TranslationMap(67,45,0,-1,4,368),
            new TranslationMap(68,0,45,1,4,414),
            new TranslationMap(69,45,0,-1,4,460),
            new TranslationMap(70,1,44,1,4,506),
            new TranslationMap(71,44,1,-1,3,0),
            new TranslationMap(72,1,44,1,3,44),
            new TranslationMap(73,44,1,-1,3,88),
            new TranslationMap(74,1,44,1,3,132),
            new TranslationMap(75,44,1,-1,3,176),
            new TranslationMap(76,1,44,1,3,220),
            new TranslationMap(77,44,1,-1,3,264),
            new TranslationMap(78,1,44,1,3,308),
            new TranslationMap(79,43,2,-1,3,352),
            new TranslationMap(80,2,43,1,3,394),
            new TranslationMap(81,43,2,-1,3,436),
            new TranslationMap(82,2,43,1,3,478),
            new TranslationMap(83,42,3,-1,2,0),
            new TranslationMap(84,3,42,1,2,40),
            new TranslationMap(85,42,3,-1,2,80),
            new TranslationMap(86,3,42,1,2,120),
            new TranslationMap(87,42,3,-1,2,160),
            new TranslationMap(88,4,41,1,2,200),
            new TranslationMap(89,41,4,-1,2,238),
            new TranslationMap(90,4,41,1,2,276),
            new TranslationMap(91,40,5,-1,2,314),
            new TranslationMap(92,5,40,1,2,350),
            new TranslationMap(93,40,5,-1,2,386),
            new TranslationMap(94,6,39,1,2,422),
            new TranslationMap(95,39,6,-1,2,456),
            new TranslationMap(96,6,39,1,2,490),
            new TranslationMap(97,39,6,-1,1,0),
            new TranslationMap(98,7,38,1,1,34),
            new TranslationMap(99,38,7,-1,1,66),
            new TranslationMap(100,8,37,1,1,98),
            new TranslationMap(101,37,8,-1,1,128),
            new TranslationMap(102,9,36,1,1,158),
            new TranslationMap(103,36,9,-1,1,186),
            new TranslationMap(104,10,35,1,1,214),
            new TranslationMap(105,38,7,-1,1,240),
            new TranslationMap(106,8,37,1,1,272),
            new TranslationMap(107,36,9,-1,1,302),
            new TranslationMap(108,9,36,1,1,330),
            new TranslationMap(109,35,10,-1,1,358),
            new TranslationMap(110,10,35,1,1,384),
            new TranslationMap(111,34,11,-1,1,410),
            new TranslationMap(112,12,33,1,1,434),
            new TranslationMap(113,32,13,-1,1,456),
            new TranslationMap(114,14,31,1,1,476),
            new TranslationMap(115,34,11,-1,1,494),
            new TranslationMap(116,14,31,1,1,518),
            new TranslationMap(117,26,19,-1,1,536)
    };


    static TranslationMap[] boardMapCandy = {
//Y,StartX,End X,Direction,Strip #,Offset in strip
            new TranslationMap(0,23,22,-1,8,452),
            new TranslationMap(1,20,25,1,8,446),
            new TranslationMap(2,27,18,-1,8,436),
            new TranslationMap(3,16,29,1,8,422),
            new TranslationMap(4,30,15,-1,8,406),
            new TranslationMap(5,14,31,1,8,388),
            new TranslationMap(6,33,12,-1,8,366),
            new TranslationMap(7,11,34,1,8,342),
            new TranslationMap(8,31,14,-1,8,324),
            new TranslationMap(9,14,31,1,8,306),
            new TranslationMap(10,32,13,-1,8,286),
            new TranslationMap(11,12,33,1,8,264),
            new TranslationMap(12,34,11,-1,8,240),
            new TranslationMap(13,11,34,1,8,216),
            new TranslationMap(14,35,10,-1,8,190),
            new TranslationMap(15,9,36,1,8,162),
            new TranslationMap(16,37,8,-1,8,132),
            new TranslationMap(17,8,37,1,8,102),
            new TranslationMap(18,38,7,-1,8,70),
            new TranslationMap(19,6,39,1,8,36),
            new TranslationMap(20,40,5,-1,8,0),
            new TranslationMap(21,40,5,-1,7,0),
            new TranslationMap(22,7,38,1,7,36),
            new TranslationMap(23,38,7,-1,7,68),
            new TranslationMap(24,6,39,1,7,100),
            new TranslationMap(25,39,6,-1,7,134),
            new TranslationMap(26,5,40,1,7,168),
            new TranslationMap(27,40,5,-1,7,204),
            new TranslationMap(28,4,40,1,7,240), // 41 -> 40
            new TranslationMap(29,41,4,-1,7,277),
            new TranslationMap(30,4,41,1,7,315),
            new TranslationMap(31,42,3,-1,7,353),
            new TranslationMap(32,3,42,1,7,393),
            new TranslationMap(33,42,3,-1,7,433),
            new TranslationMap(34,2,43,1,7,473),
            new TranslationMap(35,43,2,-1,6,0),
            new TranslationMap(36,2,43,1,6,42),
            new TranslationMap(37,44,1,-1,6,84),
            new TranslationMap(38,1,44,1,6,128),
            new TranslationMap(39,45,0,-1,6,172), //fixed
            new TranslationMap(40,1,44,1,6,216),
            new TranslationMap(41,44,1,-1,6,260),
            new TranslationMap(42,1,44,1,6,304),
            new TranslationMap(43,44,1,-1,6,348),
            new TranslationMap(44,0,45,1,6,392),
            new TranslationMap(45,45,0,-1,6,438),
            new TranslationMap(46,0,45,1,6,484),
            new TranslationMap(47,45,0,-1,5,0),
            new TranslationMap(48,0,45,1,5,46),
            new TranslationMap(49,45,0,-1,5,92),
            new TranslationMap(50,0,45,1,5,138),
            new TranslationMap(51,45,0,-1,5,184),
            new TranslationMap(52,0,45,1,5,230),
            new TranslationMap(53,45,0,-1,5,276),
            new TranslationMap(54,0,45,1,5,322),
            new TranslationMap(55,45,0,-1,5,368),
            new TranslationMap(56,0,45,1,5,414),
            new TranslationMap(57,45,0,-1,5,460),
            new TranslationMap(58,0,45,1,5,506),
            new TranslationMap(59,45,0,-1,4,0),
            new TranslationMap(60,0,45,1,4,46),
            new TranslationMap(61,45,0,-1,4,92),
            new TranslationMap(62,0,45,1,4,138),
            new TranslationMap(63,45,0,-1,4,184),
            new TranslationMap(64,0,45,1,4,230),
            new TranslationMap(65,45,0,-1,4,276),
            new TranslationMap(66,0,45,1,4,322),
            new TranslationMap(67,45,0,-1,4,368),
            new TranslationMap(68,0,45,1,4,414),
            new TranslationMap(69,45,0,-1,4,460),
            new TranslationMap(70,1,44,1,4,506),
            new TranslationMap(71,44,1,-1,3,0),
            new TranslationMap(72,1,44,1,3,44),
            new TranslationMap(73,44,1,-1,3,88),
            new TranslationMap(74,1,44,1,3,132),
            new TranslationMap(75,44,1,-1,3,176),
            new TranslationMap(76,1,44,1,3,220),
            new TranslationMap(77,44,1,-1,3,264),
            new TranslationMap(78,1,44,1,3,308),
            new TranslationMap(79,43,2,-1,3,352),
            new TranslationMap(80,2,43,1,3,394),
            new TranslationMap(81,43,2,-1,3,436),
            new TranslationMap(82,2,43,1,3,478),
            new TranslationMap(83,42,3,-1,2,0),
            new TranslationMap(84,3,42,1,2,40),
            new TranslationMap(85,42,3,-1,2,80),
            new TranslationMap(86,3,42,1,2,120),
            new TranslationMap(87,42,3,-1,2,160),
            new TranslationMap(88,4,41,1,2,200),
            new TranslationMap(89,41,4,-1,2,238),
            new TranslationMap(90,4,41,1,2,276),
            new TranslationMap(91,40,5,-1,2,314),
            new TranslationMap(92,5,40,1,2,350),
            new TranslationMap(93,40,5,-1,2,386),
            new TranslationMap(94,6,39,1,2,422),
            new TranslationMap(95,39,6,-1,2,456),
            new TranslationMap(96,6,39,1,2,490),
            new TranslationMap(97,39,6,-1,1,0),
            new TranslationMap(98,7,38,1,1,34),
            new TranslationMap(99,38,7,-1,1,66),
            new TranslationMap(100,8,37,1,1,98),
            new TranslationMap(101,37,8,-1,1,128),
            new TranslationMap(102,9,36,1,1,158),
            new TranslationMap(103,36,9,-1,1,186),
            new TranslationMap(104,10,35,1,1,214),
            new TranslationMap(105,38,7,-1,1,240),
            new TranslationMap(106,8,37,1,1,272),
            new TranslationMap(107,36,9,-1,1,302),
            new TranslationMap(108,9,36,1,1,330),
            new TranslationMap(109,35,10,-1,1,358),
            new TranslationMap(110,10,35,1,1,384),
            new TranslationMap(111,34,11,-1,1,410),
            new TranslationMap(112,12,33,1,1,434),
            new TranslationMap(113,32,13,-1,1,456),
            new TranslationMap(114,14,31,1,1,476),
            new TranslationMap(115,34,11,-1,1,494),
            new TranslationMap(116,14,31,1,1,518),
            new TranslationMap(117,26,19,-1,1,536)
    };
}

/*
void BBattachCommandCallbacks()
{
  // Attach callback methods
  cmdMessenger.attach(BBCommandList, ShowCommands);     // 0
  cmdMessenger.attach(BBCmdOne);                        // 1
  cmdMessenger.attach(BBsetled, Onsetled);              // 2
  cmdMessenger.attach(BBsetheadlight, Onsetheadlight);  // 3
  cmdMessenger.attach(BBClearScreen, OnClearScreen);    // 4
  cmdMessenger.attach(BBUpdate, OnUpdate);              // 5
  cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 6
  cmdMessenger.attach(BBGetBatteryLevel, OnGetBatteryLevel);      // 7
  cmdMessenger.attach(BBFillScreen, OnFillScreen);      // 8
  cmdMessenger.attach(BBSetRow, OnSetRow);              // 9
  cmdMessenger.attach(BBPingRow, OnPingRow);            // 10
  cmdMessenger.attach(BBEchoRow, OnEchoRow);            // 11
}
 */


/*

 */