package com.richardmcdougall.bb.board;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
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
    // Max board pixel size limited by the following: need to make dynamic, or adjustable.
    public static int[][][] pixel2OffsetTable = new int[512][512][3];
    private static String TAG = "BurnerBoard";
    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    protected final Object mSerialConn = new Object();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    public int boardWidth = 1;
    public int boardHeight = 1;
    public CmdMessenger mListener = null;
    public BBService service = null;
    public int[] boardScreen;
    public int mRefreshRate = 15;
    public int[] mBatteryStats = new int[16];
    private SerialInputOutputManager mSerialIoManager;
    private UsbDevice mUsbDevice = null;
    protected TextBuilder textBuilder = null;
    public ArcBuilder arcBuilder = null;
    long lastFlushTime = java.lang.System.currentTimeMillis();
    private int flushCnt = 0;
    protected AppDisplay appDisplay = null;

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
    }

    static public int colorDim(int dimValue, int color) {
        int b = (dimValue * (color & 0xff)) / 255;
        int g = (dimValue * ((color & 0xff00) >> 8) / 255);
        int r = (dimValue * ((color & 0xff0000) >> 16) / 255);
        return (RGB.getARGBInt(r, g, b));
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

    public void setText(String text, int delay, RGB color){
        this.textBuilder.setText(text, delay, mRefreshRate, color);
    }
    public void setText90(String text, int delay , RGB color){
        this.textBuilder.setText90(text, delay, mRefreshRate, color);
    }

    public static BurnerBoard Builder(BBService service) {

        BurnerBoard burnerBoard = null;

        if (service.boardState.GetBoardType() == BoardState.BoardType.classic) {
            BLog.d(TAG, "Visualization: Using Classic");
            burnerBoard = new BurnerBoardClassic(service);
        } else if (BoardState.BoardType.mast == service.boardState.GetBoardType()) {
            BLog.d(TAG, "Visualization: Using Mast");
            burnerBoard = new BurnerBoardMast(service);
        } else if (BoardState.BoardType.panel == service.boardState.GetBoardType()) {
            BLog.d(TAG, "Visualization: Using Panel");
            burnerBoard = new BurnerBoardPanel(service);
        } else if (BoardState.BoardType.wspanel == service.boardState.GetBoardType()) {
                BLog.d(TAG, "Visualization: Using WSPanel");
                burnerBoard = new BurnerBoardWSPanel(service);
        } else if (BoardState.BoardType.backpack == service.boardState.GetBoardType()) {
            BLog.d(TAG, "Visualization: Using Direct Map");
            burnerBoard = new BurnerBoardDirectMap(
                    service,
                    BurnerBoardDirectMap.kVisualizationDirectMapWidth,
                    BurnerBoardDirectMap.kVisualizationDirectMapHeight
            );
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.azul) {
            BLog.d(TAG, "Visualization: Using Azul");
            burnerBoard = new BurnerBoardAzul(service);
        } else {
            BLog.d(TAG, "Could not identify board type! Falling back to Azul for backwards compatibility");
            burnerBoard = new BurnerBoardAzul(service);
        }

        return burnerBoard;
    }

    public int getFrameRate() {
        return 12;
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

    public void clearPixels() {
        Arrays.fill(boardScreen, 0);
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
        boardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
        boardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
        boardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
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

    public void scrollPixels(boolean down) {

        if (boardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight - 1; y++) {
                    boardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            boardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    boardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            boardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    boardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            boardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
        } else {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = boardHeight - 2; y >= 0; y--) {
                    boardScreen[pixel2Offset(x, y + 1, PIXEL_RED)] =
                            boardScreen[pixel2Offset(x, y, PIXEL_RED)];
                    boardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)] =
                            boardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                    boardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)] =
                            boardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                }
            }
        }
    }

    public boolean update() {

        this.appDisplay.sendVisual(8);

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

    protected void logFlush(){
        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            BLog.d(TAG, "Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
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
        int r = boardScreen[pixel2Offset(x, y, PIXEL_RED)];
        int g = boardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
        int b = boardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
        return RGB.getARGBInt(r, g, b);
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

    protected void setStrip(int strip, int[] pixels) {

        // Do color correction on burner board display pixels
        byte[] newPixels = new byte[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel = pixel + 3) {
            newPixels[pixel] = (byte) pixelColorCorrectionRed(pixels[pixel]);
            newPixels[pixel + 1] = (byte) pixelColorCorrectionGreen(pixels[pixel + 1]);
            newPixels[pixel + 2] = (byte) pixelColorCorrectionBlue(pixels[pixel + 2]);
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

    public void fadePixels(int amount) {

        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {
                int r = boardScreen[pixel2Offset(x, y, PIXEL_RED)];
                //System.out.println("br = " + br);
                if (r >= amount) {
                    r -= amount;
                } else {
                    r = 0;
                }
                boardScreen[pixel2Offset(x, y, PIXEL_RED)] = r;
                int g = boardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                if (g >= amount) {
                    g -= amount;
                } else {
                    g = 0;
                }
                boardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = g;
                int b = boardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                if (b >= amount) {
                    b -= amount;
                } else {
                    b = 0;
                }
                boardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = b;
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

    public int[] getmBatteryStats() {
        return mBatteryStats;
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
        BLog.d(TAG, "testTeensy: ");

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

    // TODO: Implement generic layers
    public void fillScreenLayer(int[] layer, int r, int g, int b) {

        for (int x = 0; x < boardWidth * boardHeight; x++) {
            layer[x * 3 + 0] = r;
            layer[x * 3 + 1] = g;
            layer[x * 3 + 2] = b;
        }
    }

    public int rgbToArgb(int color) {
        int r = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int b = ((color & 0xff0000) >> 16);
        int a = 0xff;
        return (a << 24) | color;
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
