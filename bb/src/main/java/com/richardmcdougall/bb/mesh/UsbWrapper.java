package com.richardmcdougall.bb.mesh;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class UsbWrapper implements SerialInputOutputManager.Listener {
    private String TAG = this.getClass().getSimpleName();

    private SerialInputOutputManager mSerialIoManager;
    Runnable usbSupervisor = this::usbSupervisor;
    protected final Object mSerialConn = new Object();
    UsbDeviceConnection mDeviceConnection = null;
    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    private UsbDevice mUsbDevice = null;
    private UsbManager mUsbManager = null;
    protected static final String GET_USB_PERMISSION = "GetUsbPermission";

    usbThreadFactory usbThreadFactory = new usbThreadFactory();

    class usbThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("usbWrapper");
            return t;
        }
    }
    private ScheduledThreadPoolExecutor sch = (java.util.concurrent.ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, usbThreadFactory);
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public interface UsbWrapperCallback {
        public void onConnect();

        public void onNewData(byte[] data);

        boolean checkDevice(int vid, int pid);

    }

    private UsbWrapperCallback mCallback;
    private BBService service = null;

    public UsbWrapper(BBService s, String name, UsbWrapperCallback callback) {
        mCallback = callback;
        service = s;
        TAG = TAG + "." + name;
        sch.scheduleWithFixedDelay(usbSupervisor, 1, 1, TimeUnit.SECONDS);
    }

    public void writeAsync(byte[] bytes) {
                mSerialIoManager.writeAsync(bytes);
    }

    private void usbSupervisor() {
        //BLog.e(TAG, "supervisor");

        // Check in case iomanager left serial port open but manager shutdown on error
        try {
            if (mSerialIoManager.getState() != SerialInputOutputManager.State.RUNNING) {
                if (sPort != null) {
                    BLog.d(TAG, "supervisor closing serial port");
                    sPort.close();
                }
                mDeviceConnection.close();
                mUsbDevice = null;
            }
        } catch (Exception e) {}

        try {
            if (mUsbDevice == null) {
                BLog.d(TAG, "supervisor initUsb");
                initUsb();
            }
        } catch (Exception e) {
            BLog.e(TAG, "supervisor failed init  of USB: " + e.getMessage());
        }
        try {
            if (mUsbDevice != null &&
                    (sPort == null ||
                            (sPort != null && sPort.isOpen() == false))) {
                BLog.d(TAG, "supervisor usbConnect");
                usbConnect(mUsbDevice);
            }
        } catch (Exception e) {
            BLog.d(TAG, "supervisor failed connect of USB: " + e.getMessage());
        }
        try {
            if (mSerialIoManager == null || mSerialIoManager.getState() == SerialInputOutputManager.State.STOPPED) {
                BLog.d(TAG, "supervisor startIoManager");
                startIoManager();
                BLog.d(TAG, "started device");
                mCallback.onConnect();
            }
        } catch (Exception e) {
            BLog.e(TAG, "supervisor failed starting iomanager and sending init: " + e.getMessage());
        }
        //BLog.d(TAG, "sport: " + sPort.toString() + " " + sPort.isOpen() + " iomanager: " + mSerialIoManager.getState().toString());
    }

    @Override
    public void onRunError(Exception e) {
        BLog.e(TAG, "Serial Error: " + e.getMessage());
    }

    @Override
    public void onNewData(byte[] data) {
        mCallback.onNewData(data);
    }

    private boolean checkUsbDevice(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        BLog.d(TAG, "checking device " + device.describeContents() + ", pid:" + pid + ", vid: " + vid);
        if (mCallback.checkDevice(vid, pid)) {
            return true;
        }

        return false;
    }

    public void initUsb() {
        BLog.d(TAG, "initUsb()");

        if (mUsbDevice != null) {
            BLog.d(TAG, "initUsb: already have a device");
            return;
        }

        mUsbManager = (UsbManager) service.getSystemService(Context.USB_SERVICE);

        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
        if (availableDrivers.isEmpty()) {
            BLog.d(TAG, "USB: No USB Devices");
            return;
        }

        // Find the Radio device by pid/vid
        mUsbDevice = null;
        BLog.d(TAG, "There are " + availableDrivers.size() + " drivers");
        for (int i = 0; i < availableDrivers.size(); i++) {
            mDriver = availableDrivers.get(i);

            // See if we can find the adafruit M0 which is the Radio
            mUsbDevice = mDriver.getDevice();

            if (checkUsbDevice(mUsbDevice)) {
                BLog.d(TAG, "found Meshtastic");
                break;
            } else {
                mUsbDevice = null;
            }
        }

        if (mUsbDevice == null) {
            BLog.d(TAG, "No Meshtastic device found");
            return;
        }

        if (!mUsbManager.hasPermission(mUsbDevice)) {
            BLog.d(TAG, "USB: No Permission");
            return;
        }
    }

    private void usbConnect(UsbDevice device) {

        if (checkUsbDevice(mUsbDevice)) {
            BLog.d(TAG, "found Meshtastic");
        } else {
            BLog.d(TAG, "not Meshtastic");
            return;
        }

        // Close any lingering open ports
        try {
            if (sPort != null) {
                sPort.close();
            }
        } catch (IOException e) {}

        mDeviceConnection = mUsbManager.openDevice(mDriver.getDevice());

        if (mDeviceConnection == null) {
            BLog.d(TAG, "open device failed");
            return;
        }

        try {
            sPort = (UsbSerialPort) mDriver.getPorts().get(0);//Most have just one port (port 0)
            sPort.open(mDeviceConnection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            sPort.setDTR(true);


        } catch (IOException e) {
            BLog.d(TAG, "USB: Error setting up device: " + e.getMessage());
            try {
                sPort.close();
            } catch (IOException e2) {/*ignore*/}
            sPort = null;
            BLog.d(TAG, ("USB Device Error"));
            return;
        }
        BLog.d(TAG, "USB: Connected");
    }

    public void startIoManager() {

        synchronized (mSerialConn) {
            if (sPort != null) {
                BLog.d(TAG, "Starting io manager ..");
                mSerialIoManager = new SerialInputOutputManager(sPort, this);
                mSerialIoManager.setReadTimeout(100);
                mSerialIoManager.setName("Meshtastic");

                //mSerialIoManager = new SerialInputOutputManager(sPort, mListener, this.service);
                mExecutor.submit(mSerialIoManager);
                BLog.d(TAG, "USB Connected to Meshtastic");

            }

        }
    }
}
