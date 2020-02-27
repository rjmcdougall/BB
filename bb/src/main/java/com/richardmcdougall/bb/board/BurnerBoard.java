package com.richardmcdougall.bb.board;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextPaint;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.richardmcdougall.bb.ACTION;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.DebugConfigs;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rmc on 3/5/17.
 */

public abstract class BurnerBoard {
    protected static final String GET_USB_PERMISSION = "GetUsbPermission";
    static final int PIXEL_RED = 0;
    static final int PIXEL_GREEN = 1;
    static final int PIXEL_BLUE = 2;
    public static int[][][] pixel2OffsetTable = new int[255][255][3];
    private static String TAG = "BurnerBoard";
    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    protected final Object mSerialConn = new Object();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    public int boardWidth = 1;
    public int boardHeight = 1;
    public CmdMessenger mListener = null;
    public BBService service = null;
    public String mEchoString = "";
    public int[] mBoardScreen;
    public String boardType;
    public int isTextDisplaying = 0;
    public int mRefreshRate = 15;
    public int mTextSizeHorizontal = 12;
    public int mTextSizeVerical = 12;
    public IntBuffer mTextBuffer = null;
    public int isFlashDisplaying = 0;
    public IntBuffer mDrawBuffer = null;
    public int[] mBatteryStats = new int[16];
    public int mDimmerLevel = 255;
    private SerialInputOutputManager mSerialIoManager;
    private UsbDevice mUsbDevice = null;
    boolean renderTextOnScreen = false;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            final String TAG = "mUsbReceiver";
            BLog.d(TAG, "onReceive entered");
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                BLog.d(TAG, "A USB Accessory was detached (" + device + ")");
                if (device != null) {
                    if (mUsbDevice == device) {
                        BLog.d(TAG, "It's this device");
                        mUsbDevice = null;
                        stopIoManager();
                    }
                }
            }
            if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                BLog.d(TAG, "USB Accessory attached (" + device + ")");
                if (mUsbDevice == null) {
                    BLog.d(TAG, "Calling initUsb to check if we should add this device");
                    initUsb();
                } else {
                    BLog.d(TAG, "this USB already attached");
                }
            }
            BLog.d(TAG, "onReceive exited");
        }
    };

    public BurnerBoard(BBService service) {
        this.service = service;
        // Register to receive attach/detached messages that are proxied from MainActivity
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        this.service.registerReceiver(mUsbReceiver, filter);
        filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        this.service.registerReceiver(mUsbReceiver, filter);

        renderTextOnScreen = (this.service.boardState.boardType== BoardState.BoardType.azul || this.service.boardState.boardType== BoardState.BoardType.panel);
    
    }
    
    static public int getRGB(int r, int g, int b) {

        return (r * 65536 + g * 256 + b);
    }

    static public int colorDim(int dimValue, int color) {
        int b = (dimValue * (color & 0xff)) / 255;
        int g = (dimValue * ((color & 0xff00) >> 8) / 255);
        int r = (dimValue * ((color & 0xff0000) >> 16) / 255);
        return (BurnerBoard.getRGB(r, g, b));
    }

    static int pixel2Offset(int x, int y, int rgb) {
        return pixel2OffsetTable[x][y][rgb];
    }

    static int gammaCorrect(int value) {
        //return ((value / 255) ^ (1 / gamma)) * 255;
        if (value > 255) value = 255;
        if (value < 0) value = 0;
        return GammaCorrection.gamma8[value];
    }

    public static BurnerBoard Builder(BBService service) {

        BurnerBoard burnerBoard = null;

        if (DebugConfigs.OVERRIDE_BOARD_TYPE != null) {
            switch (DebugConfigs.OVERRIDE_BOARD_TYPE) {
                case classic:
                    BLog.d(TAG, "Visualization: Using Classic");
                    burnerBoard = new BurnerBoardClassic(service);
                    break;
                case azul:
                    BLog.d(TAG, "Visualization: Using Azul");
                    burnerBoard = new BurnerBoardAzul(service);
                    break;
                case mast:
                    BLog.d(TAG, "Visualization: Using Mast");
                    burnerBoard = new BurnerBoardMast(service);
                    break;
                case panel:
                    BLog.d(TAG, "Visualization: Using Panel");
                    burnerBoard = new BurnerBoardPanel(service);
                    break;
                case backpack:
                    BLog.d(TAG, "Visualization: Using Direct Map");
                    burnerBoard = new BurnerBoardDirectMap(
                            service,
                            BurnerBoardDirectMap.kVisualizationDirectMapWidth,
                            BurnerBoardDirectMap.kVisualizationDirectMapHeight
                    );
                    break;
            }
        } else {
            if (service.boardState.boardType == BoardState.BoardType.classic) {
                BLog.d(TAG, "Visualization: Using Classic");
                burnerBoard = new BurnerBoardClassic(service);
            } else if (BoardState.BoardType.mast == service.boardState.boardType) {
                BLog.d(TAG, "Visualization: Using Mast");
                burnerBoard = new BurnerBoardMast(service);
            } else if (BoardState.BoardType.panel == service.boardState.boardType) {
                BLog.d(TAG, "Visualization: Using Panel");
                burnerBoard = new BurnerBoardPanel(service);
            } else if (BoardState.BoardType.backpack == service.boardState.boardType) {
                BLog.d(TAG, "Visualization: Using Direct Map");
                burnerBoard = new BurnerBoardDirectMap(
                        service,
                        BurnerBoardDirectMap.kVisualizationDirectMapWidth,
                        BurnerBoardDirectMap.kVisualizationDirectMapHeight
                );
            } else if (service.boardState.boardType == BoardState.BoardType.azul) {
                BLog.d(TAG, "Visualization: Using Azul");
                burnerBoard = new BurnerBoardAzul(service);
            } else {
                BLog.d(TAG, "Could not identify board type! Falling back to Azul for backwards compatibility");
                burnerBoard = new BurnerBoardAzul(service);
            }
        }
        return burnerBoard;
    }

    public void setTextBuffer(int width, int height) {
        mTextBuffer = IntBuffer.allocate(width * height);
        mDrawBuffer = IntBuffer.allocate(width * height);
    }

    public int getFrameRate() {
        return 12;
    }

    public void setPixel(int pixel, int r, int g, int b) {

        if (pixel < 0 || pixel >= (boardWidth * boardHeight)) {
            BLog.d(TAG, "setPixel out of range: " + pixel);
            return;
        }
        mBoardScreen[pixel * 3] = r;
        mBoardScreen[pixel * 3 + 1] = g;
        mBoardScreen[pixel * 3 + 2] = b;
    }

    public abstract int getMultiplier4Speed();

    // Convert from xy to buffer memory
    int pixel2OffsetCalc(int x, int y, int rgb) {
        return (y * boardWidth + x) * 3 + rgb;
    }

    protected void initPixelOffset() {
        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {
                for (int rgb = 0; rgb < 3; rgb++) {
                    pixel2OffsetTable[x][y][rgb] = pixel2OffsetCalc(x, y, rgb);
                }
            }
        }
    }

    // Instant current in milliamps
    public int getBatteryCurrent() {
        int codedLevel = mBatteryStats[6];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    public int getBatteryCurrentInstant() {
        int codedLevel = mBatteryStats[9];
        if (codedLevel > 32768) {
            return 10 * (codedLevel - 65536);
        } else {
            return 10 * codedLevel;
        }
    }

    // Voltage in millivolts
    public int getBatteryVoltage() {
        return 0;
    }

    public String getBatteryStats() {
        return Arrays.toString(mBatteryStats);
    }

    public void showBattery() {

    }

    public int[] getPixelBuffer() {
        return mBoardScreen;
    }

    public void resetParams() {
        mDimmerLevel = 255;
    }

    public void clearPixels() {
        Arrays.fill(mBoardScreen, 0);
    }

    public void setPixel(int x, int y, int color) {

        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        setPixel(x, y, r, g, b);
    }

    public void setPixel(int x, int y, int r, int g, int b) {

        if (x < 0 || x >= boardWidth || y < 0 || y >= boardHeight) {
            BLog.d(TAG, "setPixel out of range: " + x + "," + y);
            return;
        }
        mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
        mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
        mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
    }

    public void fillScreen(int r, int g, int b) {
        int x;
        int y;
        for (x = 0; x < boardWidth; x++) {
            for (y = 0; y < boardHeight; y++) {
                setPixel(x, y, r, g, b);
            }
        }
    }

    public void fillScreen(int color) {

        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        fillScreen(r, g, b);
    }

    public void scrollPixels(boolean down) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight - 1; y++) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
        } else {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = boardHeight - 2; y >= 0; y--) {
                    mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                }
            }
        }
    }

    public boolean update() {

        sendVisual(8);

        //l("sendCommand: 5");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmd(6);
                mListener.sendCmdEnd();
                return true;
            } else {
                // Emulate board's 30ms refresh time
                try {
                    Thread.sleep(5);
                } catch (Throwable e) {
                }
            }
        }
        return false;
    }

    public void scrollPixelsExcept(boolean down, int color) {

        if (mBoardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight - 1; y++) {
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
            for (int x = 0; x < boardWidth; x++) {
                for (int y = boardHeight - 2; y >= 0; y--) {
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

    public void flush() {
    }

    public void fillScreenMask(int r, int g, int b) {
        //System.out.println("Fillscreen " + r + "," + g + "," + b);
        int x;
        int y;
        for (x = 0; x < boardWidth; x++) {
            for (y = 0; y < boardHeight; y++) {
                if (getPixel(x, y) > 0) {
                    setPixel(x, y, r, g, b);
                }
            }
        }
    }

    public void fillScreenMask(int color) {
        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        fillScreenMask(r, g, b);
    }

    public int getPixel(int x, int y) {
        int r = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
        int g = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
        int b = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
        return BurnerBoard.getRGB(r, g, b);
    }

    public void setOtherlightsAutomatically() {
    }

    public void setPixelOtherlight(int pixel, int other, int r, int g, int b) {
    }

    protected int pixelColorCorrectionRed(int red) {
        return gammaCorrect(red);
    }

    protected int pixelColorCorrectionGreen(int green) {
        return gammaCorrect(green);
    }

    protected int pixelColorCorrectionBlue(int blue) {
        return gammaCorrect(blue);
    }// Send a strip of pixels to the board

    protected void setStrip(int strip, int[] pixels, int powerLimitMultiplierPercent) {

        int[] dimPixels = new int[pixels.length];

        for (int pixel = 0; pixel < pixels.length; pixel++) {
            dimPixels[pixel] =
                    (mDimmerLevel * pixels[pixel]) / 256 * powerLimitMultiplierPercent / 100;
        }

        // Do color correction on burner board display pixels
        byte[] newPixels = new byte[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel = pixel + 3) {
            newPixels[pixel] = (byte) pixelColorCorrectionRed(dimPixels[pixel]);
            newPixels[pixel + 1] = (byte) pixelColorCorrectionGreen(dimPixels[pixel + 1]);
            newPixels[pixel + 2] = (byte) pixelColorCorrectionBlue(dimPixels[pixel + 2]);
        }

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

    protected int findPowerLimitMultiplierPercent(int subtract){
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
        powerLimitMultiplierPercent = 100 - java.lang.Math.max(powerPercent - subtract, 0);

        return powerLimitMultiplierPercent;

    }

    protected void setRowVisual(int row, int[] pixels) {

        int[] dimPixels = new int[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            dimPixels[pixel] =
                    (mDimmerLevel * pixels[pixel]) / 255;
        }

        // Send pixel row to in-app visual display
        sendVisual(14, row, dimPixels);
    }

    public void dimPixels(int level) {
        mDimmerLevel = level;
    }

    public void fadePixels(int amount) {

        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {
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

    private boolean checkUsbDevice(UsbDevice device) {

        int vid = device.getVendorId();
        int pid = device.getProductId();
        BLog.d(TAG, "checking device " + device.describeContents() + ", pid:" + pid + ", vid: " + vid);
        if ((pid == 1155) && (vid == 5824)) {
            return true;
        } else {
            return false;
        }

    }

    public void initUsb() {
        BLog.d(TAG, "BurnerBoard: initUsb()");

        if (mUsbDevice != null) {
            BLog.d(TAG, "initUsb: already have a device");
            return;
        }

        UsbManager manager = (UsbManager) service.getSystemService(Context.USB_SERVICE);

        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            BLog.d(TAG, "USB: No device/driver");
            return;
        }

        // Register to receive detached messages
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        service.registerReceiver(mUsbReceiver, filter);

        // Find the Radio device by pid/vid
        mUsbDevice = null;
        for (int i = 0; i < availableDrivers.size(); i++) {
            mDriver = availableDrivers.get(i);

            // See if we can find the adafruit M0 which is the Radio
            mUsbDevice = mDriver.getDevice();

            if (checkUsbDevice(mUsbDevice)) {
                BLog.d(TAG, "found Burnerboard");
                break;
            } else {
                mUsbDevice = null;
            }
        }

        if (mUsbDevice == null) {
            BLog.d(TAG, "No BurnerBoard USB device found");
            return;
        }

        if (!manager.hasPermission(mUsbDevice)) {
            //ask for permission

            PendingIntent pi = PendingIntent.getBroadcast(service.context, 0, new Intent(GET_USB_PERMISSION), 0);
            service.context.registerReceiver(new PermissionReceiver(), new IntentFilter(GET_USB_PERMISSION));
            //manager.requestPermission(mUsbDevice, pi);
            //return;

            BLog.d(TAG, "USB: No Permission");
            return;
        }

        UsbDeviceConnection connection = manager.openDevice(mDriver.getDevice());
        if (connection == null) {
            BLog.d(TAG, "USB connection == null");
            return;
        }

        try {
            sPort = (UsbSerialPort) mDriver.getPorts().get(0);//Most have just one port (port 0)
            sPort.open(connection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            sPort.setDTR(true);
        } catch (IOException e) {
            BLog.d(TAG, "USB: Error setting up device: " + e.getMessage());
            try {
                sPort.close();
            } catch (IOException e2) {/*ignore*/}
            sPort = null;
            return;
        }

        BLog.d(TAG, "USB: Connected");
        startIoManager();
    }

    public void stopIoManager() {
        synchronized (mSerialConn) {
            //status.setText("Disconnected");
            if (mSerialIoManager != null) {
                BLog.d(TAG, "Stopping io manager ..");
                mSerialIoManager.stop();
                mSerialIoManager = null;
                mListener = null;
            }
            if (sPort != null) {
                try {
                    sPort.close();
                } catch (IOException e) {
                    // Ignore.
                }
                sPort = null;
            }
            BLog.d(TAG, "USB Disconnected");
        }
    }

    public void startIoManager() {

        synchronized (mSerialConn) {
            if (sPort != null) {
                BLog.d(TAG, "Starting io manager ..");
                //mListener = new BBListenerAdapter();
                mListener = new CmdMessenger(sPort, ',', ';', '\\');
                mSerialIoManager = new SerialInputOutputManager(sPort, mListener, this.service);
                mExecutor.submit(mSerialIoManager);

                start();

                BLog.d(TAG, "USB Connected to " + service.boardState.BOARD_ID);
                // Perf Tests thare are useful during debugging
                //setMode(50);
                //testTeensy();
                //testPerf();
            }
        }
    }

    public void start() {

    }

    private void testPerf() {
        byte[] testRow1 = "0123456789012345678901234567890".getBytes();
        long startTime = java.lang.System.currentTimeMillis();
        final int Iters = 100;
        final int Rows = 86;

        for (int iters = 0; iters < Iters; iters++) {
            for (int i = 0; i < Rows; i++) {
                pingRow(0, testRow1);
            }
            flush();
        }
        int elapsedTime = (int) (java.lang.System.currentTimeMillis() - startTime);
        int bytes = Iters * Rows * testRow1.length;

        BLog.d(TAG, "USB Benchmark: " + bytes + " bytes in " + elapsedTime + ", " +
                (bytes * 1000 / elapsedTime / 1024) + " kbytes/sec");
        return;
    }

    private void testTeensy() {
        byte[] testRow1 = "0123456789012345".getBytes();

        for (int c = 0; c < 256; ) {
            for (int i = 0; i < 16; i++, c++) {
                testRow1[i] = (byte) c;
            }
            echoRow(0, testRow1);
        }
        flush();
        try {
            Thread.sleep(1000);
        } catch (Throwable e) {
        }
        BLog.d(TAG, "testTeensy: " + mEchoString);

        return;
    }

    public boolean pingRow(int row, byte[] pixels) {

        //l("sendCommand: 16,n,...");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(16);
                mListener.sendCmdArg(row);
                mListener.sendCmdEscArg(pixels);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }

    public boolean echoRow(int row, byte[] pixels) {

        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(17);
                mListener.sendCmdEscArg(pixels);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }// We use this to catch the USB accessory detached message

    public void flush2Board() {

        if (mListener != null) {
            mListener.flushWrites();
        } else {
            // Emulate board's 30ms refresh time
            try {
                Thread.sleep(2);
            } catch (Throwable e) {
            }
        }
    }

    public void sendVisual(int visualId) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        Intent in = new Intent(ACTION.GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
    }

    public void sendVisual(int visualId, int arg) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        Intent in = new Intent(ACTION.GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg", arg);
        LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
    }

    public void sendVisual(int visualId, int arg1, int arg2, int arg3) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        Intent in = new Intent(ACTION.GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg1", arg1);
        in.putExtra("arg2", arg2);
        in.putExtra("arg3", arg3);
        LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
    }

    public void sendVisual(int visualId, int arg1, int[] arg2) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        final byte[] pixels = new byte[arg2.length];
        for (int i = 0; i < arg2.length; i++) {
            pixels[i] = (byte) arg2[i];
        }
        Intent in = new Intent(ACTION.GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg1", arg1);
        //java.util.Arrays.fill(arg2, (byte) 128);
        in.putExtra("arg2", pixels);
        LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
    }

    public void aRGBtoBoardScreen(Buffer buf, int[] sourceScreen, int[] destScreen) {

        if (buf == null) {
            return;
        }

        int[] buffer = (int[]) buf.array();

        for (int pixelNo = 0; pixelNo < (boardWidth * boardHeight); pixelNo++) {
            int pixel_offset = pixelNo * 3;
            int pixel = buffer[pixelNo];
            int a = pixel & 0xff;
            // Render the new text over the original
            if (pixel != 0) {
                destScreen[pixel_offset] = (pixel >> 16) & 0xff;    //r
                destScreen[pixel_offset + 1] = (pixel >> 8) & 0xff; //g
                destScreen[pixel_offset + 2] = pixel & 0xff;        //b
            } else {
                destScreen[pixel_offset] = sourceScreen[pixel_offset];
                destScreen[pixel_offset + 1] = sourceScreen[pixel_offset + 1];
                destScreen[pixel_offset + 2] = sourceScreen[pixel_offset + 2];
            }
        }
    }

    // Draw text on screen and delay for n seconds
    public void setText(String text, int delay) {
        isTextDisplaying = delay * mRefreshRate / 1000;

        // Lowres boards only display horizontal
        if (boardWidth < 15) {
            setText90(text, delay);
            return;
        }

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boardWidth, boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, boardWidth / 2, boardHeight / 2);
        Paint textPaint = new TextPaint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE); // Text Color
        textPaint.setTypeface(Typeface.create("Courier", Typeface.BOLD));
        textPaint.setTextSize(mTextSizeVerical); // Text Size
        canvas.drawText(text.substring(0, Math.min(text.length(), 4)),
                (boardWidth / 2), 30, textPaint);//boardHeight / 2 + (mTextSizeVerical / 3), textPaint);
        if (mTextBuffer != null) {
            mTextBuffer.rewind();
            bitmap.copyPixelsToBuffer(mTextBuffer);
        }
    }

    // Draw text on screen and delay for n seconds
    public void setText90(String text, int delay) {
        isTextDisplaying = delay * mRefreshRate / 1000;

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boardWidth, boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, boardWidth / 2, boardHeight / 2);
        canvas.rotate(90, boardWidth / 2, boardHeight / 2);
        Paint textPaint = new TextPaint();
        textPaint.setDither(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE); // Text Color
        textPaint.setTextSize(mTextSizeHorizontal); // Text Size
        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
        canvas.drawText(text, (boardWidth / 2), boardHeight / 2 + (mTextSizeHorizontal / 3), textPaint);
        if (mTextBuffer != null) {
            mTextBuffer.rewind();
            bitmap.copyPixelsToBuffer(mTextBuffer);
        }
    }

    // TODO: Implement generic layers
    public void fillScreenLayer(int[] layer, int r, int g, int b) {

        for (int x = 0; x < boardWidth * boardHeight; x++) {
            layer[x * 3 + 0] = r;
            layer[x * 3 + 1] = g;
            layer[x * 3 + 2] = b;
        }
    }

    // render text on screen
    public int[] renderText(int[] newScreen, int[] origScreen) {
        // Suppress updating when displaying a text message
        if (isTextDisplaying > 0) {
            isTextDisplaying--;
            aRGBtoBoardScreen(mTextBuffer, origScreen, newScreen);
            return newScreen;
        } else if (isFlashDisplaying > 0) {
            isFlashDisplaying--;
            fillScreenLayer(newScreen, 0, 0, 128);
            return newScreen;
        } else {
            return null;
        }
    }

    public void flashScreen(int delay) {
        isFlashDisplaying = delay * mRefreshRate / 1000;
    }

    public int rgbToArgb(int color) {
        int r = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int b = ((color & 0xff0000) >> 16);
        int a = 0xff;
        return (a << 24) | color;
    }

    public void drawArc(float left, float top, float right, float bottom,
                        float startAngle, float sweepAngle, boolean useCenter,
                        boolean fill, int color) {

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(boardWidth, boardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, boardWidth / 2, boardHeight / 2);
        Paint arcPaint = new Paint();
        arcPaint.setColor(rgbToArgb(color)); //  Color
        arcPaint.setStrokeWidth(1);
        if (fill) {
            arcPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        } else {
            arcPaint.setStyle(Paint.Style.STROKE);
        }

        canvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, arcPaint);

        if (mDrawBuffer != null) {
            mDrawBuffer.rewind();
            bitmap.copyPixelsToBuffer(mDrawBuffer);
        }
        aRGBtoBoardScreen(mDrawBuffer, mBoardScreen, mBoardScreen);
    }

    private class PermissionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            service.context.unregisterReceiver(this);
            if (intent.getAction().equals(GET_USB_PERMISSION)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    BLog.d(TAG, "USB we got permission");
                    if (device != null) {
                        initUsb();
                    } else {
                        BLog.d(TAG, "USB perm receive device==null");
                    }

                } else {
                    BLog.d(TAG, "USB no permission");
                }
            }
        }
    }
}
