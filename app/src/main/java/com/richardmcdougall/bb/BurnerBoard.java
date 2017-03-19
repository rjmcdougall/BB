package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Arrays;

/**
 * Created by rmc on 3/5/17.
 */

public class BurnerBoard {

    private CmdMessenger mListener = null;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    protected final Object mSerialConn = new Object();
    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    protected static final String GET_USB_PERMISSION = "GetUsbPermission";
    private static final String TAG = "BurnerBoard";
    private Context mContext = null;
    public String boardId;
    private BBService mBBService = null;
    private int mBoardWidth = 10;
    private int mBoardHeight = 70;
    private int mBoardSideLights = 79;
    private byte[] mBoardScreen;
    private byte[] mBoardOtherlights;
    private BurnerBoard.BoardEvents boardCallback = null;

    public interface BoardEvents {
        void BoardId(String msg);

        void BoardMode(int mode);
    }

    public BurnerBoard(BBService service, Context context) {
        // Std board e.g. is 10 x 70 + 2 rows of sidelights of 79
        mBoardScreen = new byte[mBoardWidth * mBoardHeight * 3];
        mBoardOtherlights = new byte[mBoardSideLights * 3 * 2];
        mBBService = service;
        mContext = context;
        initUsb();
    }

    public void attach(BoardEvents newfunction) {
        boardCallback = newfunction;
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }


    private void updateUsbStatus(String status) {
        Intent in = new Intent(BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 3);
        // Put extras into the intent as usual
        in.putExtra("ledStatus", status);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }


    private void onDeviceStateChange() {
        l("BurnerBoard: onDeviceStateChange()");

        stopIoManager();
        startIoManager();
    }

    public void initUsb() {
        l("BurnerBoard: initUsb()");

        stopIoManager();
        UsbManager manager = (UsbManager) mBBService.getSystemService(Context.USB_SERVICE);

        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            l("No device/driver");
            updateUsbStatus(("No BB Plugged in"));
            return;
        }

        // Register to receive detached messages
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        mBBService.registerReceiver(mUsbReceiver, filter);

        // Open a connection to the first available driver.
        UsbSerialDriver mDriver = availableDrivers.get(0);

        //are we allowed to access?
        UsbDevice device = mDriver.getDevice();

        if (!manager.hasPermission(device)) {
            //ask for permission
            //PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(GET_USB_PERMISSION), 0);
            //mContext.registerReceiver(mPermissionReceiver, new IntentFilter(GET_USB_PERMISSION));
            //manager.requestPermission(device, pi);
            l("No USB Permission");
            updateUsbStatus(("No USB Permission"));
            return;
        }


        UsbDeviceConnection connection = manager.openDevice(mDriver.getDevice());
        if (connection == null) {
            l("USB connection == null");
            updateUsbStatus(("No USB device"));
            return;
        }

        try {
            sPort = (UsbSerialPort) mDriver.getPorts().get(0);//Most have just one port (port 0)
            sPort.open(connection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            sPort.setDTR(true);
        } catch (IOException e) {
            l("Error setting up device: " + e.getMessage());
            try {
                sPort.close();
            } catch (IOException e2) {/*ignore*/}
            sPort = null;
            updateUsbStatus(("USB Device Error"));
            return;
        }

        updateUsbStatus(("Connected to BB"));
        sendLogMsg("USB Connected");
        startIoManager();
    }

    public void stopIoManager() {
        synchronized (mSerialConn) {
            //status.setText("Disconnected");
            if (mSerialIoManager != null) {
                l("Stopping io manager ..");
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
            updateUsbStatus(("Disconnected(1)"));
            sendLogMsg("USB Disconnected");

        }
    }

    public void startIoManager() {

        synchronized (mSerialConn) {
            if (sPort != null) {
                l("Starting io manager ..");
                //mListener = new BBListenerAdapter();
                mListener = new CmdMessenger(sPort, ',', ';', '\\');
                mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
                mExecutor.submit(mSerialIoManager);

                // attach default cmdMessenger callback
                BurnerBoard.ArdunioCallbackDefault defaultCallback =
                        new BurnerBoard.ArdunioCallbackDefault();
                mListener.attach(defaultCallback);

                // attach Test cmdMessenger callback
                BurnerBoard.ArdunioCallbackTest testCallback =
                        new BurnerBoard.ArdunioCallbackTest();
                mListener.attach(5, testCallback);

                // attach Mode cmdMessenger callback
                BurnerBoard.ArdunioCallbackMode modeCallback =
                        new BurnerBoard.ArdunioCallbackMode();
                mListener.attach(4, modeCallback);

                // attach Mode cmdMessenger callback
                BurnerBoard.ArdunioCallbackBoardID boardIDCallback =
                        new BurnerBoard.ArdunioCallbackBoardID();
                mListener.attach(11, boardIDCallback);

                getBoardId();
                getMode();
                updateUsbStatus(("Connected to ") + boardId);
                sendLogMsg("USB Connected to " + boardId);
                setMode(50);
            }
        }
    }

    public class ArdunioCallbackDefault implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            Log.d(TAG, "ardunio default callback:" + str);
        }
    }

    public class ArdunioCallbackTest implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            l("ardunio test callback:" + str);
        }
    }

    public class ArdunioCallbackMode implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            int boardMode = mListener.readIntArg();
            boardCallback.BoardMode(boardMode);
        }
    }

    public class ArdunioCallbackBoardID implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            String boardId = mListener.readStringArg();
            boardCallback.BoardId(boardId);
        }
    }


    //    cmdMessenger.attach(BBGetVoltage, OnGetVoltage);      // 10
    public float getVoltage() {
        return (float) 0;
    }

    //    cmdMessenger.attach(BBGetBoardID, OnGetBoardID);      // 11
    public String getBoardId() {

        String id;
        if (mListener != null) {
            mListener.sendCmdStart(11);
            mListener.sendCmdEnd(true, 0, 1000);
            flush();
            id = mListener.readStringArg();
            return (id);
        }
        return "";
    }


    //    cmdMessenger.attach(BBsetmode, Onsetmode);            // 4
    public boolean setMode(int mode) {

        l("sendCommand: 4," + mode);
        if (mListener == null) {
            initUsb();
        }

        if (mListener == null)
            return false;

        synchronized (mSerialConn) {

            if (mListener != null) {
                mListener.sendCmdStart(4);
                mListener.sendCmdArg(mode);
                mListener.sendCmdEnd();
                flush();
                return true;
            }
        }
        return true;
    }


    //    cmdMessenger.attach(BBFade, OnFade);                  // 7
    public boolean fadeBoard(int amount) {

        //l("sendCommand: 7");
        sendVisual(7, amount);
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmd(7);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }

    public void fadePixels(int amount) {

        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {
                byte br = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                int r = (br & 0xFF);
                //System.out.println("br = " + br);
                if (r >= amount) {
                    r -= amount;
                } else {
                        r = 0;
                }
                br = (byte) r;
                mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] = (byte) br;
                int g = (mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] & 0xFF);
                if (g >= amount) {
                    g -= amount;
                } else {
                    g = 0;
                }
                byte bg = (byte) g;
                mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] = bg;
                int b = (mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] & 0xFF);
                if (b >= amount) {
                    b -= amount;
                } else {
                    b = 0;
                }
                mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] = (byte) b;
            }
        }
    }


    public void fuzzPixels(int amount) {

        for (int x = 0; x < mBoardWidth; x++) {
            for (int y = 0; y < mBoardHeight; y++) {
                mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                        (byte) (mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] - amount);
                mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                        (byte) (mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] - amount);
                mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                        (byte) (mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] - amount);

            }
        }
    }


    //    cmdMessenger.attach(BBUpdate, OnUpdate);              // 8
    public boolean update() {

        sendVisual(8);
        //l("sendCommand: 8");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmd(8);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }


    //    cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 9
    public boolean showBattery() {

        sendVisual(9);
        l("sendCommand: 9");
        if (mListener != null) {
            mListener.sendCmd(9);
            mListener.sendCmdEnd();
            flush();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBsetheadlight, Onsetheadlight);  // 3
    public boolean setHeadlight(boolean state) {

        sendVisual(3);
        l("sendCommand: 3,1");
        if (mListener != null) {
            mListener.sendCmdStart(3);
            mListener.sendCmdArg(state == true ? 1 : 0);
            mListener.sendCmdEnd();
            flush();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBClearScreen, OnClearScreen);    // 5
    public boolean clearScreen() {

        sendVisual(5);
        l("sendCommand: 5");
        if (mListener != null) {
            mListener.sendCmd(5);
            mListener.sendCmdEnd();
            return true;
        }
        return false;
    }

    //    cmdMessenger.attach(BBScroll, OnScroll);              // 6
    public boolean scroll(boolean down) {

        sendVisual(6, down == true ? 1 : 0);
        //l("sendCommand: 6,1");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(6);
                mListener.sendCmdArg(down == true ? 1 : 0);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }

    //    cmdMessenger.attach(BBGetMode, OnGetMode);            // 12
    public int getMode() {

        l("sendCommand: 12");
        int mode;

        if (mListener != null) {
            mListener.sendCmd(12);
            mListener.sendCmdEnd();
            flush();
            mode = mListener.readIntArg();
            return (mode);
        }
        return -1;
    }

    public void fillScreen(int color) {

        byte r = (byte) (color & 0xff);
        byte g = (byte) ((color & 0xff00) >> 8);
        byte b = (byte) ((color & 0xff0000) >> 16);
        fillScreen(r, g, b);
    }

    public void fillScreen(byte r, byte g, byte b) {

        //System.out.println("Fillscreen " + r + "," + g + "," + b);
        int x;
        int y;
        for (x = 0; x < mBoardWidth; x++) {
            for (y = 0; y < mBoardHeight; y++) {
                setPixel(x, y, r, g, b);
            }
        }
    }


    //    cmdMessenger.attach(BBSetRow, OnSetRow);      // 16
    // row is 12 pixels : board has 10
    public boolean setRow(int row, byte[] pixels) {

        // Send pixel row to in-app visual display
        sendVisual(14, row, pixels);

        // Do color correction on burner board display pixels
        byte [] newPixels = new byte[mBoardWidth * 3];
        for (int pixel = 0; pixel < mBoardWidth * 3; pixel = pixel + 3) {
            newPixels[pixel] = pixelColorCorrectionRed(pixels[pixel]);
            newPixels[pixel + 1] = pixelColorCorrectionGreen(pixels[pixel + 1]);
            newPixels[pixel + 2] = pixelColorCorrectionBlue(pixels[pixel + 2]);
        }

        //System.out.println("flushPixels row:" + y + "," + bytesToHex(newPixels));

        //l("sendCommand: 14,n,...");
        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(14);
                mListener.sendCmdArg(row);
                mListener.sendCmdEscArg(newPixels);
                mListener.sendCmdEnd();
                return true;
            }
        }
        return false;
    }



    //    cmdMessenger.attach(BBSetRow, OnSetRow);      // 16
    // row is 12 pixels : board has 10
    public boolean setOtherlight(int other, byte[] pixels) {


        // Send pixel row to in-app visual display
        sendVisual(15, other, pixels);

        // Do color correction on burner board display pixels
        //byte [] newPixels = new byte[pixels.length];
        byte [] newPixels = new byte[pixels.length];
        for (int pixel = 0; pixel < pixels.length; pixel = pixel + 3) {
            newPixels[pixel] = pixelColorCorrectionRed(pixels[pixel]);
            newPixels[pixel + 1] = pixelColorCorrectionGreen(pixels[pixel + 1]);
            newPixels[pixel + 2] = pixelColorCorrectionBlue(pixels[pixel + 2]);
        }

        //System.out.println("flushPixels row:" + y + "," + bytesToHex(newPixels));

        //byte[] testRow1 = "01234567890123456789012345678900123456789012345678901234567890123450123456789012345678901234567890012345678901234567890123456789012345".getBytes();

        //l("sendCommand: 15,n,...");

        synchronized (mSerialConn) {
            if (mListener != null) {
                mListener.sendCmdStart(15);
                mListener.sendCmdArg(other);
                mListener.sendCmdEscArg(newPixels);
                mListener.sendCmdEnd();
                return true;
            }
        }


        return false;
    }


    //    cmdMessenger.attach(BBSetRow, OnSetRow);      // 16
    public void flush() {

        if (mListener != null) {
            mListener.flushWrites();
        }
    }

    public void clearPixels() {

        Arrays.fill(mBoardScreen, (byte) 0);
    }


    static public int getRGB(int r, int g, int b) {

        return (b * 65536 + g * 256 + r);
    }

    public void setPixel(int x, int y, int color) {

        byte r = (byte) (color & 0xff);
        byte g = (byte) ((color & 0xff00) >> 8);
        byte b = (byte) ((color & 0xff0000) >> 16);
        setPixel(x, y, r, g, b);
    }

    // Other lights
    // Side lights: other = 1 left, other = 2 right
    public void setPixelOtherlight(int pixel, int other, int color) {

        byte r = (byte) (color & 0xff);
        byte g = (byte) ((color & 0xff00) >> 8);
        byte b = (byte) ((color & 0xff0000) >> 16);
        setPixelOtherlight(pixel, other, r, g, b);
    }

    public void setPixelOtherlight(int pixel, int other, byte r, byte g, byte b) {

        //System.out.println("setpixelotherlight pixel:" + pixel + " light:" + other);

        if (other < 0 || other >= 2 || pixel < 0 || pixel >= mBoardSideLights) {
            l("setPixel out of range: " + other + "," + pixel);
            return;
        }
        mBoardOtherlights[pixelOtherlight2Offset(pixel, other, PIXEL_RED)] = r;
        mBoardOtherlights[pixelOtherlight2Offset(pixel, other, PIXEL_GREEN)] = g;
        mBoardOtherlights[pixelOtherlight2Offset(pixel, other, PIXEL_BLUE)] = b;
    }

    static int PIXEL_RED = 0;
    static int PIXEL_GREEN = 1;
    static int PIXEL_BLUE = 2;

    final int pixel2Offset(int x, int y, int rgb) {

        return (y * mBoardWidth + x) * 3 + rgb;
    }

    // Convert other lights to pixel buffer address
    private static final int kOtherLights = 2;
    private static final int kLeftSightlight = 0;
    private static final int kRightSidelight = 1;
    int pixelOtherlight2Offset(int pixel, int other, int rgb) {

        return (other * mBoardSideLights + pixel) * 3 + rgb;
    }

    public void setPixel(int x, int y, byte r, byte g, byte b) {
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

    public void setPixel(int pixel, byte r, byte g, byte b) {

        if (pixel < 0 || pixel >= (mBoardWidth * mBoardHeight)) {
            l("setPixel out of range: " + pixel);
            return;
        }
        mBoardScreen[pixel * 3] = r;
        mBoardScreen[pixel * 3 + 1] = g;
        mBoardScreen[pixel * 3 + 2] = b;
    }

    public void setSideLight(int leftRight, int pos, int color) {
        /// TODO: implement these
    }

    public void setOtherlightsAutomatically() {
        for (int pixel = 0; pixel < mBoardSideLights; pixel++) {
            // Calculate sidelight proportional to lengths
            int fromY = (int) ((float) pixel * (float) mBoardHeight / (float)mBoardSideLights);
            setPixelOtherlight(pixel, kLeftSightlight,
                    mBoardScreen[pixel2Offset(0, fromY, PIXEL_RED)],
                    mBoardScreen[pixel2Offset(0, fromY, PIXEL_GREEN)],
                    mBoardScreen[pixel2Offset(0, fromY, PIXEL_BLUE)]);
            setPixelOtherlight(pixel, kRightSidelight,
                    mBoardScreen[pixel2Offset(9, fromY, PIXEL_RED)],
                    mBoardScreen[pixel2Offset(9, fromY, PIXEL_GREEN)],
                    mBoardScreen[pixel2Offset(9, fromY, PIXEL_BLUE)]);
        }
    }


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void scrollPixels(boolean down) {

        if (mBoardScreen == null) {
            return;
        }
        int x;
        int y;
        if (down) {
            for (x = 0; x < mBoardWidth; x++) {
                for (y = 0; y < mBoardHeight - 1; y++) {
                    mBoardScreen[pixel2Offset(x, y, PIXEL_RED)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_RED)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_GREEN)];
                    mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)] =
                            mBoardScreen[pixel2Offset(x, y + 1, PIXEL_BLUE)];
                }
            }
        } else {
            for (x = 0; x < mBoardWidth; x++) {
                for (y = mBoardHeight - 2; y >= 0; y--) {
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

    byte [] getPixelBuffer() {
        return mBoardScreen;
    }

    // TODO: make faster by using ints
    private byte pixelColorCorrectionRed(byte red) {
        // convert signed byte to double
        double correctedRed = red & 0xff;
        correctedRed = correctedRed * 1;
        return (byte)correctedRed;
    }

    private byte pixelColorCorrectionGreen(byte green) {
        // convert signed byte to double
        double correctedGreen = green & 0xff;
        correctedGreen = correctedGreen * .3;
        return (byte)correctedGreen;
    }

    private byte pixelColorCorrectionBlue(byte blue) {
        // convert signed byte to double
        double correctedBlue = blue & 0xff;
        correctedBlue = correctedBlue * .2;
        return (byte)correctedBlue;
    }


    public void flushPixels() {
        byte[] rowPixels = new byte[mBoardWidth * 3];
        for (int y = 0; y < mBoardHeight; y++) {
            //for (int y = 30; y < 31; y++) {
            for (int x = 0; x < mBoardWidth; x++) {
                if (y < mBoardHeight) {
                    rowPixels[x * 3 + 0] = mBoardScreen[pixel2Offset(x, y, PIXEL_RED)];
                    rowPixels[x * 3 + 1] = mBoardScreen[pixel2Offset(x, y, PIXEL_GREEN)];
                    rowPixels[x * 3 + 2] = mBoardScreen[pixel2Offset(x, y, PIXEL_BLUE)];
                }
            }

            //rowPixels[0] = rowPixels[3];
            //rowPixels[1] = rowPixels[4];
            //rowPixels[2] = rowPixels[5];
            //rowPixels[33] = rowPixels[30];
            //rowPixels[34] = rowPixels[31];
            //rowPixels[35] = rowPixels[32];
            /*
            if (rowPixels[0] ==0) {

                rowPixels[0]= 33;
                rowPixels[1]= 32;
                rowPixels[2]= 1;
                rowPixels[3]= 59;
                rowPixels[4]= 0;
                rowPixels[4]= 33;
                */
            setRow(y, rowPixels);
            //update();
        }


        for (int x = 0; x < kOtherLights; x++) {
            byte [] otherPixels = new byte[mBoardSideLights * 3];
            for (int pixel = 0; pixel < mBoardSideLights; pixel++) {
                otherPixels[pixelOtherlight2Offset(pixel, 0, PIXEL_RED)] =
                        mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_RED)];
                otherPixels[pixelOtherlight2Offset(pixel, 0, PIXEL_GREEN)] =
                        mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_GREEN)];
                otherPixels[pixelOtherlight2Offset(pixel, 0, PIXEL_BLUE)] =
                        mBoardOtherlights[pixelOtherlight2Offset(pixel, x, PIXEL_BLUE)];
            }
            setOtherlight(x, otherPixels);
        }

        update();
        flush();
    }

    // We use this to catch the USB accessory detached message
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            final String TAG = "mUsbReceiver";
            l("onReceive entered");
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                l("USB Accessory detached");
                mBBService.unregisterReceiver(mUsbReceiver);
                if (device != null) {
                    stopIoManager();
                }
            }
            l("onReceive exited");
        }
    };


    public PermissionReceiver mPermissionReceiver = new PermissionReceiver();


    private class PermissionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mContext.unregisterReceiver(this);
            if (intent.getAction().equals(GET_USB_PERMISSION)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    l("USB we got permission");
                    if (device != null) {
                        initUsb();
                    } else {
                        l("USB perm receive device==null");
                    }

                } else {
                    l("USB no permission");
                }
            }
        }

    }

    private void sendVisual(int visualId) {
        Intent in = new Intent(BBService.ACTION_GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    private void sendVisual(int visualId, int arg) {
        Intent in = new Intent(BBService.ACTION_GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg", arg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    private void sendVisual(int visualId, int arg1, int arg2, int arg3) {
        Intent in = new Intent(BBService.ACTION_GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg1", arg1);
        in.putExtra("arg2", arg2);
        in.putExtra("arg3", arg3);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    private void sendVisual(int visualId, int arg1, byte[] arg2) {
        Intent in = new Intent(BBService.ACTION_GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg1", arg1);
        //java.util.Arrays.fill(arg2, (byte) 128);
        in.putExtra("arg2", arg2.clone());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

}
