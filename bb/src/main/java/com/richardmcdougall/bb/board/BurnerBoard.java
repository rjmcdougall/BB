package com.richardmcdougall.bb.board;

import static com.richardmcdougall.bb.board.Sharpener.sharpenMode.NONE;

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
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.io.IOException;
import java.nio.ByteBuffer;
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
    public int boardWidth = 255;
    public int boardHeight = 255;

    public Sharpener.sharpenMode boardSharpenMode = NONE;

    public static int textSizeHorizontal = 1;
    public static int textSizeVertical = 1;
    public static boolean enableBatteryMonitoring = false;
    public static boolean enableMotionMonitoring = false;
    public static boolean enableIOTReporting = false;
    public static boolean renderTextOnScreen = false;
    public static boolean renderLineOnScreen = false;
    public static BoardState.BoardType boardType = null;
    private static String TAG = "BurnerBoard";
    public static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    protected final Object mSerialConn = new Object();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    public CmdMessenger mListener = null;
    public BBService service = null;
    public int[] boardScreen;
    public int[] mBatteryStats = new int[16];
    public TextBuilder textBuilder = null;
    public BatteryOverlayBuilder batteryOverlayBuilder = null;
    public BrakeOverlayBuilder brakeOverlayBuilder = null;

    public Sharpener sharpener = null;
    public ArcBuilder arcBuilder = null;
    public LineBuilder lineBuilder = null;
    protected BoardDisplay boardDisplay = null;
    public PixelDimmer pixelDimmer = null;
    public PixelOffset pixelOffset = null;

    long lastFlushTime = java.lang.System.currentTimeMillis();
    public SerialInputOutputManager mSerialIoManager;
    public UsbDevice mUsbDevice = null;
    private BoardUSBReceiver boardUSBReceiver = null;

    public abstract int getFrameRate();

    public abstract int getMultiplier4Speed();

    public abstract void flush();

    public abstract void setOtherlightsAutomatically();

    public abstract void start();

    public abstract void initpixelMap2Board();

    private int flushCnt = 0;

    public BurnerBoard(BBService service) {
        this.service = service;
        this.boardUSBReceiver = new BoardUSBReceiver(this);
        BLog.d(TAG, "Burnerboard starting...");
        // Register to receive attach/detached messages that are proxied from MainActivity
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        this.service.registerReceiver(this.boardUSBReceiver, filter);
        filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        this.service.registerReceiver(this.boardUSBReceiver, filter);

        initUsb();
    }

    void init (int width, int height) {
        boardWidth = width;
        boardHeight = height;
        this.boardScreen = new int[boardWidth * boardHeight * 3];
        this.boardDisplay = new BoardDisplay(this);
        this.pixelOffset = new PixelOffset(this);
        this.textBuilder = new TextBuilder(this);
        this.batteryOverlayBuilder = new BatteryOverlayBuilder(this.service, this);
        this.brakeOverlayBuilder = new BrakeOverlayBuilder(this.service, this);
        this.lineBuilder = new LineBuilder(this);
        this.arcBuilder = new ArcBuilder(this);
        this.pixelDimmer = new PixelDimmer();
        this.sharpener = new Sharpener(this);
        this.pixelOffset.initPixelOffset();
    }

    static final public int colorDim(int dimValue, int color) {
        int b = (dimValue * (color & 0xff)) / 255;
        int g = (dimValue * ((color & 0xff00) >> 8) / 255);
        int r = (dimValue * ((color & 0xff0000) >> 16) / 255);
        return (RGB.getARGBInt(r, g, b));
    }


    public final static BurnerBoard Builder(BBService service) {

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
        } else if (BoardState.BoardType.dynamicPanel == service.boardState.GetBoardType()) {
            BLog.d(TAG, "Visualization: Using Dynamic Panel");
            burnerBoard = new BurnerBoardDynamicPanel(service);
        } else if (BoardState.BoardType.wspanel == service.boardState.GetBoardType()) {
            BLog.d(TAG, "Visualization: Using WSPanel");
            burnerBoard = new BurnerBoardWSPanel(service);
        } else if (BoardState.BoardType.backpack == service.boardState.GetBoardType()) {
            BLog.d(TAG, "Visualization: Using Direct Map");
            burnerBoard = new BurnerBoardDirectMap(service);
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.azul) {
            BLog.d(TAG, "Visualization: Using Azul");
            burnerBoard = new BurnerBoardAzul(service);
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.littlewing) {
            BLog.d(TAG, "Visualization: Using LittleWing");
            burnerBoard = new BurnerBoardLittleWing(service);
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.mezcal) {
            BLog.d(TAG, "Visualization: Using Mezcal");
            burnerBoard = new BurnerBoardMezcal(service);
        } else {
            BLog.d(TAG, "Could not identify board type! Falling back to Azul for backwards compatibility");
            burnerBoard = new BurnerBoardAzul(service);
        }

        return burnerBoard;
    }

    public final void UnregisterReceivers() {
        try {
            if (this.boardUSBReceiver != null)
                this.service.unregisterReceiver(this.boardUSBReceiver);

            BLog.i(TAG, "Unregistered Receivers");
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    }

    public enum batteryType {LARGE, SMALL, CRITICAL}

    public void showBattery(batteryType type) {

        this.batteryOverlayBuilder.drawBattery(type);
        return;
    }

    public final void clearPixels() {
        Arrays.fill(boardScreen, 0);
    }

    static final int testPix1 = 21;
    static final int testPix2 = 180;
    static final int testX = 4;

    public final void setPixel(int x, int y, int color) {

        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        //y = testPix1;
        //x = testX;
        setPixel(x, y, r, g, b);
        //y = testPix2;
        //r = g = b = 255;
        //setPixel(x, y, r, g, b);
    }

    public final void setPixel(int x, int y, int r, int g, int b) {

        if (x < 0 || x >= boardWidth || y < 0 || y >= boardHeight) {
            BLog.d(TAG, "setPixel out of range: " + x + "," + y);
            return;
        }
        //x = testX;
        //y = testPix1;
        //r = g = b = 255;

        boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)] = r;
        boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)] = g;
        boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)] = b;

        //y = testPix2;
        //boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)] = r;
        //boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)] = g;
        //boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)] = b;

    }

    public final void fillScreen(int r, int g, int b) {
        int x;
        int y;
        for (x = 0; x < boardWidth; x++) {
            for (y = 0; y < boardHeight; y++) {
                setPixel(x, y, r, g, b);
            }
        }
    }

    public void scrollPixels(boolean down) {
        //if (true) return;

        if (boardScreen == null) {
            return;
        }
        if (down) {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight - 1; y++) {
                    boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)] =
                            boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_RED)];
                    boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)] =
                            boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_GREEN)];
                    boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)] =
                            boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_BLUE)];
                }
            }
        } else {
            for (int x = 0; x < boardWidth; x++) {
                for (int y = boardHeight - 2; y >= 0; y--) {
                    boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_RED)] =
                            boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)];
                    boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_GREEN)] =
                            boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)];
                    boardScreen[this.pixelOffset.Map(x, y + 1, PIXEL_BLUE)] =
                            boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)];
                }
            }
        }
    }

    public void zagPixels() {

        if (boardScreen == null) {
            return;
        }
        for (int x = 0; x < boardWidth / 2 - 1; x++) {
            for (int y = 0; y < boardHeight - 1; y++) {
                boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)] =
                        boardScreen[this.pixelOffset.Map(x + 1, y + 1, PIXEL_RED)];
                boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)] =
                        boardScreen[this.pixelOffset.Map(x + 1, y + 1, PIXEL_GREEN)];
                boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)] =
                        boardScreen[this.pixelOffset.Map(x + 1, y + 1, PIXEL_BLUE)];
            }
        }
        for (int x = boardWidth - 1; x > (boardWidth / 2); x--) {
            for (int y = 0; y < boardHeight - 1; y++) {
                boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)] =
                        boardScreen[this.pixelOffset.Map(x - 1, y + 1, PIXEL_RED)];
                boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)] =
                        boardScreen[this.pixelOffset.Map(x - 1, y + 1, PIXEL_GREEN)];
                boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)] =
                        boardScreen[this.pixelOffset.Map(x - 1, y + 1, PIXEL_BLUE)];
            }
        }

    }

    public boolean oupdate() {

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

    protected final void logFlush() {
        flushCnt++;
        if (flushCnt > 100) {
            int elapsedTime = (int) (java.lang.System.currentTimeMillis() - lastFlushTime);
            lastFlushTime = java.lang.System.currentTimeMillis();

            BLog.d(TAG, "Framerate: " + flushCnt + " frames in " + elapsedTime + ", " +
                    (flushCnt * 1000 / elapsedTime) + " frames/sec");
            flushCnt = 0;
        }
    }

    public final void fillScreenMask(int r, int g, int b) {
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

    public final void fillScreenMask(int color) {
        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        fillScreenMask(r, g, b);
    }

    public final int getPixel(int x, int y) {
        int r = boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)];
        int g = boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)];
        int b = boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)];
        return RGB.getARGBInt(r, g, b);
    }

    public void osetStrip(int strip, int[] pixels) {
        byte[] newPixels = new byte[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            newPixels[pixel] = (byte) pixels[pixel];
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




    public void oflush2Board() {

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

    private ByteBuffer mWriteBuffer = ByteBuffer.allocate(16384);
    private static final byte[] teensyPrefix = "10,".getBytes();
    private static final byte[] teensyPostfix = ";".getBytes();
    private static final byte[] teensyUpdate = "6;".getBytes();

    public void setStrip(int strip, int[] pixels) {
        int len = Math.min(600 * 3, pixels.length);
        //BLog.d(TAG, "setstrip " + strip + " pixels length " + pixels.length);
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

        } catch (Exception e) {}
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
            BLog.d(TAG, "flush2Board failed");
        }
    }

    public final void fadePixels(int amount) {

        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {
                int r = boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)];
                //System.out.println("br = " + br);
                if (r >= amount) {
                    r -= amount;
                } else {
                    r = 0;
                }
                boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)] = r;
                int g = boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)];
                if (g >= amount) {
                    g -= amount;
                } else {
                    g = 0;
                }
                boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)] = g;
                int b = boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)];
                if (b >= amount) {
                    b -= amount;
                } else {
                    b = 0;
                }
                boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)] = b;
            }
        }
    }

    private final boolean checkUsbDevice(UsbDevice device) {

        int vid = device.getVendorId();
        int pid = device.getProductId();
        BLog.d(TAG, "checking device " + device.describeContents() + ", pid:" + pid + ", vid: " + vid);
        if ((pid == 1155) && (vid == 5824)) {
            return true;
        } else {
            return false;
        }

    }

    public final void initUsb() {
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
        service.registerReceiver(this.boardUSBReceiver, filter);

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

    public final void stopIoManager() {
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

    public final void startIoManager() {

        synchronized (mSerialConn) {
            if (sPort != null) {
                BLog.d(TAG, "Starting io manager ..");
                //mListener = new BBListenerAdapter();
                mListener = new CmdMessenger(sPort, ',', ';', '\\');
                mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
                mSerialIoManager.setReadTimeout(1);
                mSerialIoManager.setWriteTimeout(1);
                mSerialIoManager.setName("BoardThread");
                //mSerialIoManager = new SerialInputOutputManager(sPort, mListener, this.service);

                // Important for teensy4 performance
                //mSerialIoManager.setWriteBufferSize(65536);

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

    public final int[] getmBatteryStats() {
        return mBatteryStats;
    }

    private final void testPerf() {
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

    private final void testTeensy() {
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

    public final boolean pingRow(int row, byte[] pixels) {

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

    public final boolean echoRow(int row, byte[] pixels) {

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


    private final class PermissionReceiver extends BroadcastReceiver {
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

    public final class BoardCallbackGetBatteryLevel implements CmdMessenger.CmdEvents {

        private boolean requireBMS = false;

        public BoardCallbackGetBatteryLevel(boolean requireBMS) {
            this.requireBMS = requireBMS;
        }

        public void CmdAction(String str) {
            for (int i = 0; i < mBatteryStats.length; i++) {
                mBatteryStats[i] = mListener.readIntArg();
            }
            BLog.d(TAG, "Battery Callback: " + Arrays.toString(mBatteryStats));

            if (!requireBMS) {
                if (mBatteryStats[1] != -1) {
                    service.boardState.batteryLevel = mBatteryStats[1];
                } else {
                    service.boardState.batteryLevel = 100;
                }
                BLog.d(TAG, "getBatteryLevel: " + service.boardState.batteryLevel);
            }
        }
    }
}