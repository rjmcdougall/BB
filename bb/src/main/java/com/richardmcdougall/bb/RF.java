package com.richardmcdougall.bb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v4.content.LocalBroadcastManager;

import timber.log.Timber;

import android.app.PendingIntent;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rmc on 2/5/18.
 */


/*
BN: Adafruit Feather M0
VID: 239A
PID: 800B
SN: 0000000A0026
 */

public class RF {

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
            // Register to receive attach/detached messages that are proxied from MainActivity
            IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
            this.service.registerReceiver(mUsbReceiver, filter);
            filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
            this.service.registerReceiver(mUsbReceiver, filter);
            initUsb();
            service.gps.attach(new Gps.GpsEvents() {
                public void timeEvent(net.sf.marineapi.nmea.util.Time time) {
                    Timber.d("Radio Time: " + time.toString());
                    if (mRadioCallback != null) {
                        mRadioCallback.timeEvent(time);
                    }
                }

                ;

                public void positionEvent(net.sf.marineapi.provider.event.PositionEvent gps) {
                    Timber.d("Radio Position: " + gps.toString());
                    if (mRadioCallback != null) {
                        mRadioCallback.GPSevent(gps);
                    }
                }

                ;
            });
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }

        Timber.d("GPS Constructor Completed");
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
        Timber.d("Radio Sending Packet: len(" + packet.length + "), data: " + RFUtil.bytesToHex(packet));
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
        Timber.d("RF: onDeviceStateChange()");
        stopIoManager();
        if (sPort != null) {
            startIoManager();
        }
    }

    private boolean checkUsbDevice(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        Timber.d("checking device " + device.describeContents() + ", pid:" + pid + ", vid: " + vid);
        if ((pid == 0x800B) && (vid == 0x239A)) {
            Timber.d("Found Adafruit RADIO module");
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
        Timber.d("RF: initUsb()");

        if (mUsbDevice != null) {
            Timber.d("initUsb: already have a device");
            return;
        }

        mUsbManager = (UsbManager) service.getSystemService(Context.USB_SERVICE);

        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
        if (availableDrivers.isEmpty()) {
            Timber.d("USB: No USB Devices");
            return;
        }

        // Find the Radio device by pid/vid
        mUsbDevice = null;
        Timber.d("There are " + availableDrivers.size() + " drivers");
        for (int i = 0; i < availableDrivers.size(); i++) {
            mDriver = availableDrivers.get(i);

            // See if we can find the adafruit M0 which is the Radio
            mUsbDevice = mDriver.getDevice();

            if (checkUsbDevice(mUsbDevice)) {
                Timber.d("found RF");
                break;
            } else {
                mUsbDevice = null;
            }
        }

        if (mUsbDevice == null) {
            Timber.d("No Radio device found");
            return;
        }

        if (!mUsbManager.hasPermission(mUsbDevice)) {
            //ask for permissionB
            // DIRTY HACK FOR THE MONITOR.
            if (service.boardState.BOARD_ID.equals("VIM2")) {
                PendingIntent pi = PendingIntent.getBroadcast(service.context, 0, new Intent(GET_USB_PERMISSION), 0);
                service.context.registerReceiver(new RF.PermissionReceiver(), new IntentFilter(GET_USB_PERMISSION));
                mUsbManager.requestPermission(mUsbDevice, pi);
                Timber.d("USB: No Permission");
            }
            return;
        } else {
            usbConnect(mUsbDevice);
        }
    }

    private void usbConnect(UsbDevice device) {

        if (checkUsbDevice(mUsbDevice)) {
            Timber.d("found RF");
        } else {
            Timber.d("not RF");
            return;
        }

        UsbDeviceConnection connection = mUsbManager.openDevice(mDriver.getDevice());
        if (connection == null) {
            Timber.d("open device failed");
            return;
        }

        try {
            sPort = (UsbSerialPort) mDriver.getPorts().get(0);//Most have just one port (port 0)
            sPort.open(connection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            sPort.setDTR(true);
        } catch (IOException e) {
            Timber.d("USB: Error setting up device: " + e.getMessage());
            try {
                sPort.close();
            } catch (IOException e2) {/*ignore*/}
            sPort = null;
            Timber.d(("USB Device Error"));
            return;
        }

        Timber.d("USB: Connected");
        startIoManager();
    }


    public void stopIoManager() {
        synchronized (mSerialConn) {
            //status.setText("Disconnected");
            if (mSerialIoManager != null) {
                Timber.d("Stopping io manager ..");
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
            Timber.d("USB Disconnected");

        }
    }

    public void startIoManager() {

        synchronized (mSerialConn) {
            if (sPort != null) {
                Timber.d("Starting io manager ..");
                //mListener = new BBListenerAdapter();
                mListener = new CmdMessenger(sPort, ',', ';', '\\');
                mSerialIoManager = new SerialInputOutputManager(sPort, mListener, null);
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

                Timber.d("USB Connected to radio");
            }
        }
    }

    public class BBRadioCallbackDefault implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {

            Timber.d( "ardunio default callback:" + str);
        }
    }

    public class BBRadioCallbackReceive implements CmdMessenger.CmdEvents {
        public void CmdAction(String str) {

            int sigStrength = mListener.readIntArg();
            int len = mListener.readIntArg();
            Timber.d("radio receive callback: sigstrength" + sigStrength + ", " + len + " bytes");
            ByteArrayOutputStream recvBytes = new ByteArrayOutputStream();
            for (int i = 0; i < len; i++) {
                recvBytes.write(Math.min(mListener.readIntArg(), 255));
            }

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

    // We use this to catch the USB accessory detached message
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            final String TAG = "mUsbReceiver";
            Timber.d("onReceive entered");
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                Timber.d("A USB Accessory was detached (" + device + ")");
                if (device != null) {
                    if (mUsbDevice == device) {
                        Timber.d("It's this device, shutting down");
                        mUsbDevice = null;
                        stopIoManager();
                    }
                }
            }
            if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Timber.d("USB Accessory attached (" + device + ")");
                if (mUsbDevice == null) {
                    Timber.d("Calling initUsb to check if we should add this device");
                    usbConnect(device);
                    ;
                } else {
                    Timber.d("USB already attached");
                }
            }
            Timber.d("onReceive exited");
        }
    };

    // Receive permission if it's being asked for (typically for the first time)
    private class PermissionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            service.context.unregisterReceiver(this);
            if (intent.getAction().equals(GET_USB_PERMISSION)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Timber.d("USB we got permission");
                    if (device != null) {
                        usbConnect(device);
                        ;
                    } else {
                        Timber.d("USB perm receive device==null");
                    }

                } else {
                    Timber.d("USB no permission");
                }
            }
        }
    }

}
