package com.richardmcdougall.bb.rf;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v4.content.LocalBroadcastManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.richardmcdougall.bb.ACTION;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bb.CmdMessenger;
import com.richardmcdougall.bb.Gps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
BN: Adafruit Feather M0
VID: 239A
PID: 800B
SN: 0000000A0026
 */

public class RF {
    private String TAG = this.getClass().getSimpleName();

    public CmdMessenger mListener = null;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    protected final Object mSerialConn = new Object();
    private static UsbSerialPort sPort = null;
    private static UsbSerialDriver mDriver = null;
    private UsbDevice mUsbDevice = null;
    private UsbManager mUsbManager = null;
    protected static final String GET_USB_PERMISSION = "GetUsbPermission";
    private BBService service = null;
    public RF.radioEvents mRadioCallback = null;

    public RF(BBService service) {
        try {
            this.service = service;
            initUsb();
            service.gps.attach(new Gps.GpsEvents() {
                public void timeEvent(net.sf.marineapi.nmea.util.Time time) {
                    BLog.d(TAG, "Radio Time: " + time.toString());
                    if (mRadioCallback != null) {
                        mRadioCallback.timeEvent(time);
                    }
                }

                public void positionEvent(net.sf.marineapi.provider.event.PositionEvent gps) {
                    BLog.d(TAG, "Radio Position: " + gps.toString());
                    if (mRadioCallback != null) {
                        mRadioCallback.GPSevent(gps);
                    }
                }

                ;
            });
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }

        BLog.d(TAG, "GPS Constructor Completed");
    }

    public interface radioEvents {
        void receivePacket(byte[] bytes, int sigStrength);

        void GPSevent(net.sf.marineapi.provider.event.PositionEvent gps);

        void timeEvent(net.sf.marineapi.nmea.util.Time time);
    }

    public void attach(radioEvents newfunction) {
        mRadioCallback = newfunction;
    }

    public void broadcast(byte[] packet) {
        BLog.d(TAG, "Radio Sending Packet: len(" + packet.length + "), data: " + RFUtil.bytesToHex(packet));
        if (mListener != null) {
            synchronized (mSerialConn) {
                mListener.sendCmdStart(5);
                mListener.sendCmdArg(packet.length);
                for (int i = 0; i < packet.length; i++) {
                    mListener.sendCmdArg((int) packet[i]);
                }
                mListener.sendCmdEnd();
                mListener.flushWrites();
            }
        }
    }

    private void onDeviceStateChange() {
        BLog.d(TAG, "RF: onDeviceStateChange()");
        stopIoManager();
        if (sPort != null) {
            startIoManager();
        }
    }

    private boolean checkUsbDevice(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        BLog.d(TAG, "checking device " + device.describeContents() + ", pid:" + pid + ", vid: " + vid);
        if ((pid == 0x800B) && (vid == 0x239A)) {
            BLog.d(TAG, "Found Adafruit RADIO module");
            return true;
        }
        /*
        if ((pid == 1155) && (vid == 5824)) {
            l("Found Teensy RADIO module");
            return true;
        }
        */
        return false;
    }

    public void initUsb() {
        BLog.d(TAG, "RF: initUsb()");

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
                BLog.d(TAG, "found RF");
                break;
            } else {
                mUsbDevice = null;
            }
        }

        if (mUsbDevice == null) {
            BLog.d(TAG, "No Radio device found");
            return;
        }

        if (!mUsbManager.hasPermission(mUsbDevice)) {
            BLog.d(TAG, "USB: No Permission");
            return;
        } else {
            usbConnect(mUsbDevice);
        }
    }

    private void usbConnect(UsbDevice device) {

        if (checkUsbDevice(mUsbDevice)) {
            BLog.d(TAG, "found RF");
        } else {
            BLog.d(TAG, "not RF");
            return;
        }

        UsbDeviceConnection connection = mUsbManager.openDevice(mDriver.getDevice());
        if (connection == null) {
            BLog.d(TAG, "open device failed");
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
            BLog.d(TAG, ("USB Device Error"));
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
                mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
                mSerialIoManager.setReadTimeout(5);
                mSerialIoManager.setName("RF");

                //mSerialIoManager = new SerialInputOutputManager(sPort, mListener, this.service);

                mExecutor.submit(mSerialIoManager);

                // attach default cmdMessenger callback
                RF.BBRadioCallbackDefault defaultCallback =
                        new RF.BBRadioCallbackDefault();
                mListener.attach(defaultCallback);

                // attach Radio Receive cmdMessenger callback
                RF.BBRadioCallbackReceive radioReceiveCallback =
                        new RF.BBRadioCallbackReceive();
                mListener.attach(4, radioReceiveCallback);

                // attach Visualization cmdMessenger callback
                RF.BBRadioCallbackGPS gpsCallback =
                        new RF.BBRadioCallbackGPS();
                mListener.attach(2, gpsCallback);

                BLog.d(TAG, "USB Connected to radio");
            }
        }
    }

    public class BBRadioCallbackDefault implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {

            BLog.d(TAG, "ardunio default callback:" + str);
        }
    }

    public class BBRadioCallbackReceive implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {

            int sigStrength = mListener.readIntArg();
            int len = mListener.readIntArg();
            BLog.d(TAG, "radio receive callback: sigstrength" + sigStrength + ", " + len + " bytes");
            ByteArrayOutputStream recvBytes = new ByteArrayOutputStream();
            for (int i = 0; i < len; i++) {
                recvBytes.write(Math.min(mListener.readIntArg(), 255));
            }
            BLog.d(TAG, "Radio Receive Packet: len(" + recvBytes.toByteArray().length + "), data: " + RFUtil.bytesToHex(recvBytes.toByteArray()));

            Intent in = new Intent(ACTION.BB_PACKET);
            in.putExtra("sigStrength", sigStrength);
            in.putExtra("packet", recvBytes.toByteArray());
            LocalBroadcastManager.getInstance(service).sendBroadcast(in);

            if (mRadioCallback != null) {
                mRadioCallback.receivePacket(recvBytes.toByteArray(), sigStrength);
            }
        }
    }

    public class BBRadioCallbackGPS implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {
            /* This page describes all the output and diagnostics we can get from the below log line,
                which are called "GPS Sentences": http://aprs.gids.nl/nmea/     -jib
             */
            //l("GPS callback:" + str);
            String gpsStr = mListener.readStringArg().replaceAll("_", ",");
            //l("GPS: " + gpsStr);
            // TODO: strip string
            try {
                service.gps.addStr(gpsStr);
            } catch (Exception e) {

            }
        }
    }
}
