package com.richardmcdougall.bb;

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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextPaint;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rmc on 3/5/17.
 */

public class BurnerBoard {

    public int mBoardWidth = 1;
    public int mBoardHeight = 1;
    public int mMultipler4Speed = 1;
    public BurnerBoard.BoardEvents boardCallback = null;
    public CmdMessenger mListener = null;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    protected final Object mSerialConn = new Object();
    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    private UsbDevice mUsbDevice = null;
    protected static final String GET_USB_PERMISSION = "GetUsbPermission";
    private static final String TAG = "BB.BurnerBoard";
    public Context mContext = null;
    public String boardId;
    public BBService mBBService = null;
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


    public BurnerBoard(BBService service, Context context) {
        mBBService = service;
        mContext = context;
        // Register to receive attach/detached messages that are proxied from MainActivity
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        mBBService.registerReceiver(mUsbReceiver, filter);
        filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        mBBService.registerReceiver(mUsbReceiver, filter);
    }

    public BBService getBBService() {
         return mBBService;
    }

    public void setTextBuffer(int width, int height) {
        mTextBuffer = IntBuffer.allocate(width * height);
        mDrawBuffer = IntBuffer.allocate(width * height);
    }

    public int getFrameRate() {
        return 12;
    }

    public int getWidth() {
        return mBoardWidth;
    }

    public int getHeight() {
        return mBoardHeight;
    }
    public int getMultiplier4Speed() {
        return mMultipler4Speed;
    }

    public interface BoardEvents {
        void BoardId(String msg);

        void BoardMode(int mode);
    }

    public int getBattery() {
        return -1;
    }

    // Average current in milliamps
    public int getBatteryCurrentInstant() {
        return 0;
    }

    // Instant current in milliamps
    public int getBatteryCurrent() {
        return 0;
    }

    // Voltage in millivolts
    public int getBatteryVoltage() {return 0; }

    // Battery Pack health 0-100%
    public int getBatteryHealth() {return 0; }


    public String getBatteryStats() { return null;   }



    public void showBattery() {

    }

    public int[] getPixelBuffer() {
        return null;
    }

    public void resetParams() {
    }

    public void clearPixels() {
    }

    static public int getRGB(int r, int g, int b) {

        return (r * 65536 + g * 256 + b);
    }

    public void setPixel(int x, int y, int color) {

        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        setPixel(x, y, r, g, b);
    }

    public void setPixel(int x, int y, int r, int g, int b) {
    }

    public void attach(BurnerBoard.BoardEvents newfunction) {
        boardCallback = newfunction;
    }

    public void fillScreen(int color) {

        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        fillScreen(r, g, b);
    }

    public void scrollPixels(boolean down) {
    }

    public void scrollPixelsExcept(boolean down, int color) {
    }

    public void flush() {
    }

    public void fillScreen(int r, int g, int b) {
    }

    // Fill only active pixels
    public void fillScreenMask(int r, int g, int b) {
    }

    public void fillScreenMask(int color) {
        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        fillScreenMask(r, g, b);
    }

    public int getPixel(int x, int y) {
        return 0;
    }

    public void setOtherlightsAutomatically() {
    }

    public void setPixelOtherlight(int pixel, int other, int color) {

        int b = (color & 0xff);
        int g = ((color & 0xff00) >> 8);
        int r = ((color & 0xff0000) >> 16);
        setPixelOtherlight(pixel, other, r, g, b);
    }

    public void setPixelOtherlight(int pixel, int other, int r, int g, int b) {
    }

    static public int colorDim(int dimValue, int color) {
        int b = (dimValue * (color & 0xff)) / 255;
        int g = (dimValue * ((color & 0xff00) >> 8) / 255);
        int r = (dimValue * ((color & 0xff0000) >> 16) / 255);
        return (BurnerBoard.getRGB(r, g, b));
    }


    public void dimPixels(int level) {
    }

    public void fuzzPixels(int amount) {
    }

    public boolean setMode(int mode) {
        setText(String.valueOf(mode), 2000);
        return true;
    }

    public void fadePixels(int amount) {
    }

    public void setMsg(String msg) {
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
        if (sPort != null) {
            startIoManager();
        }
    }

    private boolean checkUsbDevice(UsbDevice device) {

        int vid = device.getVendorId();
        int pid = device.getProductId();
        l("checking device " + device.describeContents() + ", pid:" + pid + ", vid: " + vid);
        if ((pid == 1155) && (vid == 5824)) {
            return true;
        } else {
            return false;
        }

    }

    public void initUsb() {
        l("BurnerBoard: initUsb()");

        if (mUsbDevice != null) {
            l("initUsb: already have a device");
            return;
        }

        UsbManager manager = (UsbManager) mBBService.getSystemService(Context.USB_SERVICE);

        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            l("USB: No device/driver");
            updateUsbStatus(("No BB Plugged in"));
            return;
        }

        // Register to receive detached messages
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        mBBService.registerReceiver(mUsbReceiver, filter);

        // Find the Radio device by pid/vid
        mUsbDevice = null;
        for (int i = 0; i < availableDrivers.size(); i++) {
            mDriver = availableDrivers.get(i);

            // See if we can find the adafruit M0 which is the Radio
            mUsbDevice = mDriver.getDevice();

            if (checkUsbDevice(mUsbDevice)) {
                l("found Burnerboard");
                break;
            } else {
                mUsbDevice = null;
            }
        }

        if (mUsbDevice == null) {
            l("No BurnerBoard USB device found");
            return;
        }

        if (!manager.hasPermission(mUsbDevice)) {
            //ask for permission

            PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent(GET_USB_PERMISSION), 0);
            mContext.registerReceiver(new PermissionReceiver(), new IntentFilter(GET_USB_PERMISSION));
            //manager.requestPermission(mUsbDevice, pi);
            //return;

            l("USB: No Permission");
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
            l("USB: Error setting up device: " + e.getMessage());
            try {
                sPort.close();
            } catch (IOException e2) {/*ignore*/}
            sPort = null;
            updateUsbStatus(("USB Device Error"));
            return;
        }

        updateUsbStatus(("Connected to BB"));
        l("USB: Connected");
        startIoManager();
    }

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

                start();

                updateUsbStatus(("Connected to ") + boardId);
                l("USB Connected to " + boardId);
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

        l("USB Benchmark: " + bytes + " bytes in " + elapsedTime + ", " +
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
        l("testTeensy: " + mEchoString);

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
    }


    // We use this to catch the USB accessory detached message
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            final String TAG = "mUsbReceiver";
            l("onReceive entered");
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                l("A USB Accessory was detached (" + device + ")");
                if (device != null) {
                    if (mUsbDevice == device) {
                        l("It's this device");
                        mUsbDevice = null;
                        stopIoManager();
                    }
                }
            }
            if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                l("USB Accessory attached (" + device + ")");
                if (mUsbDevice == null) {
                    l("Calling initUsb to check if we should add this device");
                    initUsb();
                } else {
                    l("this USB already attached");
                }
            }
            l("onReceive exited");
        }
    };



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
        Intent in = new Intent(BBService.ACTION_GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    public void sendVisual(int visualId, int arg) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        Intent in = new Intent(BBService.ACTION_GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg", arg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    public void sendVisual(int visualId, int arg1, int arg2, int arg3) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        Intent in = new Intent(BBService.ACTION_GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg1", arg1);
        in.putExtra("arg2", arg2);
        in.putExtra("arg3", arg3);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    public void sendVisual(int visualId, int arg1, int[] arg2) {
        if(!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        final byte[] pixels = new byte[arg2.length];
        for (int i = 0; i < arg2.length; i++) {
            pixels[i] = (byte) arg2[i];
        }
        Intent in = new Intent(BBService.ACTION_GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg1", arg1);
        //java.util.Arrays.fill(arg2, (byte) 128);
        in.putExtra("arg2", pixels);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    public void batteryActions(int level) {

    }

    // Gamma correcttion for LEDs
    static int  gamma8[] = {
            0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  1,  1,  1,
            1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  2,  2,  2,  2,  2,  2,
            2,  3,  3,  3,  3,  3,  3,  3,  4,  4,  4,  4,  4,  5,  5,  5,
            5,  6,  6,  6,  6,  7,  7,  7,  7,  8,  8,  8,  9,  9,  9, 10,
            10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 15, 15, 16, 16,
            17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 24, 24, 25,
            25, 26, 27, 27, 28, 29, 29, 30, 31, 32, 32, 33, 34, 35, 35, 36,
            37, 38, 39, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 50,
            51, 52, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 66, 67, 68,
            69, 70, 72, 73, 74, 75, 77, 78, 79, 81, 82, 83, 85, 86, 87, 89,
            90, 92, 93, 95, 96, 98, 99,101,102,104,105,107,109,110,112,114,
            115,117,119,120,122,124,126,127,129,131,133,135,137,138,140,142,
            144,146,148,150,152,154,156,158,160,162,164,167,169,171,173,175,
            177,180,182,184,186,189,191,193,196,198,200,203,205,208,210,213,
            215,218,220,223,225,228,231,233,236,239,241,244,247,249,252,255 };


    static int gammaCorrect(int value) {
        //return ((value / 255) ^ (1 / gamma)) * 255;
        if (value>255) value = 255;
        if (value < 0) value = 0;
        return gamma8[value];
    }

    public void aRGBtoBoardScreen(Buffer buf, int[] sourceScreen, int [] destScreen) {

        if (buf == null) {
            return;
        }

        int [] buffer = (int [])buf.array();

        for (int pixelNo = 0; pixelNo < (mBoardWidth * mBoardHeight); pixelNo++) {
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

    /**
     * @return text height
     */
    private float getTextHeight(String text, Paint paint) {

        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);
        return rect.height();
    }

    // Draw text on screen and delay for n seconds
    public void setText(String text, int delay) {
        isTextDisplaying = delay * mRefreshRate / 1000;

        // Lowres boards only display horizontal
        if (mBoardWidth < 15) {
            setText90(text, delay);
            return;
        }

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(mBoardWidth, mBoardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, mBoardWidth / 2, mBoardHeight / 2);
        //canvas.translate(0, mBoardHeight - 20);
        //canvas.rotate(90, mBoardWidth / 2, mBoardHeight / 2);
        Paint textPaint = new TextPaint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE); // Text Color
        //textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create("Courier", Typeface.BOLD));
        textPaint.setTextSize(mTextSizeVerical); // Text Size
        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
        canvas.drawText(text.substring(0, Math.min(text.length(), 4)),
                (mBoardWidth / 2), 30 , textPaint);//mBoardHeight / 2 + (mTextSizeVerical / 3), textPaint);
        if (mTextBuffer != null) {
            mTextBuffer.rewind();
            bitmap.copyPixelsToBuffer(mTextBuffer);
        }
    }

    // Draw text on screen and delay for n seconds
    public void setText90(String text, int delay) {
        isTextDisplaying = delay * mRefreshRate / 1000;

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(mBoardWidth, mBoardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, mBoardWidth / 2, mBoardHeight / 2);
        canvas.rotate(90, mBoardWidth / 2, mBoardHeight / 2);
        Paint textPaint = new TextPaint();
        textPaint.setDither(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE); // Text Color
        textPaint.setTextSize(mTextSizeHorizontal); // Text Size
        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)); // Text Overlapping Pattern
        canvas.drawText(text, (mBoardWidth / 2), mBoardHeight / 2 + (mTextSizeHorizontal / 3), textPaint);
        if (mTextBuffer != null) {
            mTextBuffer.rewind();
            bitmap.copyPixelsToBuffer(mTextBuffer);
        }
    }

    // TODO: Implement generic layers
    public void fillScreenLayer(int [] layer, int r, int g, int b) {

        for (int x = 0; x < mBoardWidth * mBoardHeight; x++) {
            layer[x * 3 + 0] = r;
            layer[x * 3 + 1] = g;
            layer[x * 3 + 2] = b;
        }
    }

    // render text on screen
    public int [] renderText(int [] newScreen, int [] origScreen) {
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
        Bitmap bitmap = Bitmap.createBitmap(mBoardWidth, mBoardHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        canvas.scale(-1, -1, mBoardWidth / 2, mBoardHeight / 2);
        Paint arcPaint = new Paint();
        arcPaint.setColor(rgbToArgb(color)); //  Color
        arcPaint.setStrokeWidth(1);
        if (fill) {
            arcPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        } else {
            arcPaint.setStyle(Paint.Style.STROKE);
        }

        //canvas.drawColor(Color.RED);

        canvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, arcPaint);
        //canvas.drawArc(0, 0, canvas.getWidth() / 2, canvas.getHeight() / 2, 0, 90, true, arcPaint);

        //canvas.drawText("hello", (mBoardWidth / 2), mBoardHeight / 2 + (mTextSizeHorizontal / 3), arcPaint);

        if (mDrawBuffer != null) {
            mDrawBuffer.rewind();
            bitmap.copyPixelsToBuffer(mDrawBuffer);
        }
        aRGBtoBoardScreen(mDrawBuffer, mBoardScreen, mBoardScreen);
    }

    public long getCurrentClock() {
        long curTimeStamp = mBBService.GetCurrentClock();
        return curTimeStamp;
    }

}



