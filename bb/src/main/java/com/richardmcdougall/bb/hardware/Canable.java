package com.richardmcdougall.bb.hardware;

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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


/*
    Canable serial CANBUS

    We use Canable 1 hardware
    Must be running sclan firmware
    Update the device with https://canable.io/updater/canable1.html

    https://github.com/normaldotcom/cantact-fw

    O - Open channel
    C - Close channel
    S0 - Set bitrate to 10 kbps
    S1 - Set bitrate to 20 kbps
    S2 - Set bitrate to 50 kbps
    S3 - Set bitrate to 100 kbps
    S4 - Set bitrate to 125 kbps
    S5 - Set bitrate to 250 kbps
    S6 - Set bitrate to 500 kbps
    S7 - Set bitrate to 750 kbps
    S8 - Set bitrate to 1 Mbps
    M0 - Set mode to normal mode (default)
    M1 - Set mode to silent mode
    A0 - Disable automatic retransmission
    A1 - Enable automatic retransmission (default)
    TIIIIIIIILDD... - Transmit data frame (Extended ID) [ID, length, data]
    tIIILDD... - Transmit data frame (Standard ID) [ID, length, data]
    RIIIIIIIIL - Transmit remote frame (Extended ID) [ID, length]
    rIIIL - Transmit remote frame (Standard ID) [ID, length]
    V - Returns firmware version and remote path as a string

 */
public
class Canable implements SerialInputOutputManager.Listener {
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    CanableThreadFactory canableThreadFactory= new CanableThreadFactory();
    private ScheduledThreadPoolExecutor sch = (java.util.concurrent.ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, canableThreadFactory);
    Runnable usbSupervisor = () -> usbSupervisor();

    protected final Object mSerialConn = new Object();
    UsbDeviceConnection mDeviceConnection = null;
    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    private UsbDevice mUsbDevice = null;
    private UsbManager mUsbManager = null;
    protected static final String GET_USB_PERMISSION = "GetUsbPermission";

    private static final int kCanbusSpeed = 6; // canable 500k
    private final static ArrayList<CanListener> canListeners = new ArrayList<>();

    List<Byte> frameBytes = new ArrayList<>();

    public Canable(BBService service) {
        BLog.e(TAG, "creating service");
        try {
            this.service = service;
            sch.scheduleWithFixedDelay(usbSupervisor, 1, 1, TimeUnit.SECONDS);

        } catch (Exception e) {

        }
    }

    class CanableThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("canable");
            return t;
        }
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
                BLog.d(TAG, "supervisor initCanableDevice");
                initCanableDevice();
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
        if (data.length > 0) {
            //BLog.d(TAG, "Received " + data.length + "bytes: " + new String(data));
            try {

                for (byte b : data) {
                    if (b == '\r') {
                        // end of frame data received
                        CanFrame f = slcanToFrame(frameBytes.toArray(new Byte[frameBytes.size()]));
                        //BLog.d(TAG, "CAN frame " + f.str());
                        giveFrame(f);
                        frameBytes.clear();
                    } else {
                        // byte received, add to buffer
                        frameBytes.add(b);
                    }
                }
            } catch (Exception e) {
                BLog.e(TAG, e.getMessage());
            }
        }
    }

    private CanFrame slcanToFrame(Byte[] slcanData) {
        CanFrame result = new CanFrame();

        Byte type = slcanData[0];

        int id;
        int dlc;
        Byte[] idBytes;
        Byte[] dlcBytes;
        Byte[] dataBytes;
        if (type == 't') {
            // standard ID
            idBytes = Arrays.copyOfRange(slcanData, 1, 4);
            dlcBytes = Arrays.copyOfRange(slcanData, 4, 5);
            dataBytes = Arrays.copyOfRange(slcanData, 5, slcanData.length);
        } else if (type == 'T') {
            // extended ID
            idBytes = Arrays.copyOfRange(slcanData, 1, 9);
            dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
            dataBytes = Arrays.copyOfRange(slcanData, 10, slcanData.length);
        } else {
            // this isn't a valid frame
            return null;
        }
        String idString = byteArrayToString(idBytes);
        id = Integer.valueOf(idString, 16);
        result.setId(id);


        dlc = Integer.valueOf(byteArrayToString(dlcBytes));
        result.setDlc(dlc);

        int[] data = {0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < dlc; i++) {
            String byteString;
            byteString = byteArrayToString(Arrays.copyOfRange(dataBytes,
                    i * 2, i * 2 + 2));
            data[i] = Integer.valueOf(byteString, 16);
        }
        result.setData(data);

        return result;
    }

    private String frameToSlcan(CanFrame frame) {
        String result = "";

        result += "t";
        result += String.format("%03X", frame.getId());
        result += Integer.toString(frame.getDlc());

        for (int i : frame.getData()) {
            result += String.format("%02X", i);
        }

        result += "\r";

        return result;
    }

    private String byteArrayToString(Byte[] array) {
        byte[] bytes = new byte[array.length];
        int i = 0;
        for (byte b : array) {
            bytes[i++] = b;
        }

        return new String(bytes);
    }

    public void sendFrame(CanFrame frame) {
        String slcanString = frameToSlcan(frame);
        try {
            mSerialIoManager.writeAsync(slcanString.getBytes());
        } catch (Exception e) {
            BLog.e(TAG, "Error sending CAN frame");
        }
    }

    public void addListener(CanListener l) {
        canListeners.add(l);
    }

    public void removeListener(CanListener l) {
        canListeners.remove(l);
    }

    public void transmit(CanFrame txFrame) {
        sendFrame(txFrame);
        for (CanListener l : canListeners) {
            l.canReceived(txFrame);
        }

    }

    static void giveFrame(CanFrame f) {
        for (CanListener l : canListeners) {
            l.canReceived(f);
        }
    }

    private void initCanableDevice() {
        try {
            mSerialIoManager.writeAsync("C\r".getBytes());
            // set the CANBUS bitrate
            mSerialIoManager.writeAsync(("S" + kCanbusSpeed + "\r").getBytes());
            // open the CANBUS device
            mSerialIoManager.writeAsync("O\r".getBytes());
            mSerialIoManager.writeAsync("C\r".getBytes());
            // set the CANBUS bitrate
            mSerialIoManager.writeAsync(("S" + kCanbusSpeed + "\r").getBytes());
            // open the CANBUS device
            mSerialIoManager.writeAsync("O\r".getBytes());
        } catch (Exception e) {
            BLog.d(TAG, "Error sending Canable setup" + e.getMessage());
        }
    }

    private boolean checkUsbDevice(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        BLog.d(TAG, "checking device " + device.describeContents() + ", pid:" + pid + ", vid: " + vid);
        if ((pid == 0x60c4) && (vid == 0xad50)) {
            BLog.d(TAG, "Found Canable device");
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
                BLog.d(TAG, "found Canable");
                break;
            } else {
                mUsbDevice = null;
            }
        }

        if (mUsbDevice == null) {
            BLog.d(TAG, "No Canable device found");
            return;
        }

        if (!mUsbManager.hasPermission(mUsbDevice)) {
            BLog.d(TAG, "USB: No Permission");
            return;
        }
    }

    private void usbConnect(UsbDevice device) {

        if (checkUsbDevice(mUsbDevice)) {
            BLog.d(TAG, "found Canable");
        } else {
            BLog.d(TAG, "not Canable");
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
                mSerialIoManager.setName("Canable");

                //mSerialIoManager = new SerialInputOutputManager(sPort, mListener, this.service);
                mExecutor.submit(mSerialIoManager);
                BLog.d(TAG, "USB Connected to Canable CANBUS");

            }

        }
    }
}
