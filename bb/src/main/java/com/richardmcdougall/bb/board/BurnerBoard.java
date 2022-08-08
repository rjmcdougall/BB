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
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.io.IOException;
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
    public static int boardWidth = 1;
    public static int boardHeight = 1;
    public static int textSizeHorizontal = 1;
    public static int textSizeVertical = 1;
    public static boolean enableBatteryMonitoring = false;
    public static boolean enableIOTReporting = false;
    public static boolean renderTextOnScreen = false;
    public static boolean renderLineOnScreen = false;
    public static BoardState.BoardType boardType = null;
    private static String TAG = "BurnerBoard";
    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    protected final Object mSerialConn = new Object();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    public CmdMessenger mListener = null;
    public BBService service = null;
    public int[] boardScreen;
    public int[] mBatteryStats = new int[16];
    public TextBuilder textBuilder = null;
    public ArcBuilder arcBuilder = null;
    public LineBuilder lineBuilder = null;
    protected AppDisplay appDisplay = null;
    protected BoardDisplay boardDisplay = null;
    protected PixelOffset pixelOffset = null;
    long lastFlushTime = java.lang.System.currentTimeMillis();
    private SerialInputOutputManager mSerialIoManager;
    public UsbDevice mUsbDevice = null;
    private BoardUSBReceiver boardUSBReceiver = null;

    public abstract int getFrameRate();
    public abstract int getMultiplier4Speed();
    public abstract void flush();
    public abstract void setOtherlightsAutomatically();
    public abstract void start();

    private int flushCnt = 0;

    public BurnerBoard(BBService service) {
        this.service = service;
        this.boardUSBReceiver = new BoardUSBReceiver(this);
        // Register to receive attach/detached messages that are proxied from MainActivity
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        this.service.registerReceiver(this.boardUSBReceiver, filter);
        filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        this.service.registerReceiver(this.boardUSBReceiver, filter);

        this.boardScreen = new int[boardWidth * boardHeight * 3];
        this.boardDisplay = new BoardDisplay(this);
        this.pixelOffset = new PixelOffset(this);
        this.appDisplay = new AppDisplay(this.service,this);
        this.textBuilder = new TextBuilder(this);
        this.lineBuilder = new LineBuilder(this);
        this.arcBuilder = new ArcBuilder(this);
        initUsb();
    }

    static final public int colorDim(int dimValue, int color) {
        int b = (dimValue * (color & 0xff)) / 255;
        int g = (dimValue * ((color & 0xff00) >> 8) / 255);
        int r = (dimValue * ((color & 0xff0000) >> 16) / 255);
        return (RGB.getARGBInt(r, g, b));
    }

    static final int gammaCorrect(int value) {
        //return ((value / 255) ^ (1 / gamma)) * 255;
        if (value > 255) value = 255;
        if (value < 0) value = 0;
        return GammaCorrection.gamma8[value];
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
        } else if (service.boardState.GetBoardType() == BoardState.BoardType.v4) {
            BLog.d(TAG, "Visualization: Using V4");
            burnerBoard = new BurnerBoardV4(service);
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

    public final void clearPixels() {
        Arrays.fill(boardScreen, 0);
    }

    public final void setPixel(int x, int y, int color) {

        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        setPixel(x, y, r, g, b);
    }

    public final void setPixel(int x, int y, int r, int g, int b) {

        if (x < 0 || x >= boardWidth || y < 0 || y >= boardHeight) {
            BLog.d(TAG, "setPixel out of range: " + x + "," + y);
            return;
        }
        boardScreen[this.pixelOffset.Map(x, y, PIXEL_RED)] = r;
        boardScreen[this.pixelOffset.Map(x, y, PIXEL_GREEN)] = g;
        boardScreen[this.pixelOffset.Map(x, y, PIXEL_BLUE)] = b;
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

    protected final int pixelColorCorrectionRed(int red) {
        return gammaCorrect(red);
    }

    protected final int pixelColorCorrectionGreen(int green) {
        return gammaCorrect(green);
    }

    protected final int pixelColorCorrectionBlue(int blue) {
        return gammaCorrect(blue);
    }// Send a strip of pixels to the board

    protected final void setStrip(int strip, int[] pixels) {

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

    public final void flush2Board() {

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
            int it;
            BLog.i("Str", str);
            for (int i = 0; i < mBatteryStats.length; i++) {
                it = mListener.readIntArg();
                BLog.i("Stats: ", String.valueOf(it));
                mBatteryStats[i] = it;
            }

            if (!this.requireBMS) {
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
