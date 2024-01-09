/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.hoho.android.usbserial.util.MonotonicClock;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.concurrent.TimeoutException;

/**
 * A base class shared by several driver implementations.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public abstract class CommonUsbSerialPort implements UsbSerialPort {

    public static boolean DEBUG = false;

    private static final String TAG = CommonUsbSerialPort.class.getSimpleName();
    private static final int MAX_READ_SIZE = 16 * 1024; // = old bulkTransfer limit

    protected final UsbDevice mDevice;
    protected final int mPortNumber;

    // non-null when open()
    protected UsbDeviceConnection mConnection = null;
    protected UsbEndpoint mReadEndpoint;
    protected UsbEndpoint mWriteEndpoint;
    protected UsbRequest mUsbRequest;
    protected UsbRequest mUsbWriteRequest;

    private boolean mEnableAsyncWrites;
    private boolean mEnableAsyncReads;


    /**
     * Internal write buffer.
     * Guarded by {@link #mWriteBufferLock}.
     * Default length = mReadEndpoint.getMaxPacketSize()
     **/
    protected byte[] mWriteBuffer;
    protected final Object mWriteBufferLock = new Object();
    protected final Object mReadBufferLock = new Object();


    public CommonUsbSerialPort(UsbDevice device, int portNumber) {
        mDevice = device;
        mPortNumber = portNumber;
        mEnableAsyncWrites = true;
        mEnableAsyncReads = true;

    }

    @Override
    public String toString() {
        return String.format("<%s device_name=%s device_id=%s port_number=%s>",
                getClass().getSimpleName(), mDevice.getDeviceName(),
                mDevice.getDeviceId(), mPortNumber);
    }

    @Override
    public UsbDevice getDevice() {
        return mDevice;
    }

    @Override
    public int getPortNumber() {
        return mPortNumber;
    }

    @Override
    public UsbEndpoint getWriteEndpoint() {
        return mWriteEndpoint;
    }

    @Override
    public UsbEndpoint getReadEndpoint() {
        return mReadEndpoint;
    }

    /**
     * Returns the device serial number
     *
     * @return serial number
     */
    @Override
    public String getSerial() {
        return mConnection.getSerial();
    }

    /**
     * Sets the size of the internal buffer used to exchange data with the USB
     * stack for write operations.  Most users should not need to change this.
     *
     * @param bufferSize the size in bytes, <= 0 resets original size
     */
    public final void setWriteBufferSize(int bufferSize) {
        synchronized (mWriteBufferLock) {
            if (bufferSize <= 0) {
                if (mWriteEndpoint != null) {
                    bufferSize = mWriteEndpoint.getMaxPacketSize();
                } else {
                    mWriteBuffer = null;
                    return;
                }
            }
            if (mWriteBuffer != null && bufferSize == mWriteBuffer.length) {
                return;
            }
            mWriteBuffer = new byte[bufferSize];
        }
    }

    @Override
    public void open(UsbDeviceConnection connection) throws IOException {
        if (mEnableAsyncWrites) {
            Log.d(TAG, "Async writes enabled");
        } else {
            Log.d(TAG, "Async writes disabled.");
        }
        if (mConnection != null) {
            throw new IOException("Already open");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Connection is null");
        }
        mConnection = connection;
        try {
            openInt();
            if (mReadEndpoint == null || mWriteEndpoint == null) {
                throw new IOException("Could not get read & write endpoints");
            }
            mUsbRequest = new UsbRequest();
            mUsbRequest.initialize(mConnection, mReadEndpoint);
            mUsbWriteRequest = new UsbRequest();
            mUsbWriteRequest.initialize(mConnection, mWriteEndpoint);
        } catch (Exception e) {
            try {
                close();
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    protected abstract void openInt() throws IOException;

    @Override
    public void close() throws IOException {
        if (mConnection == null) {
            throw new IOException("Already closed");
        }
        try {
            mUsbRequest.cancel();
        } catch (Exception ignored) {
        }
        mUsbRequest = null;
        try {
            closeInt();
        } catch (Exception ignored) {
        }
        try {
            mConnection.close();
        } catch (Exception ignored) {
        }
        mConnection = null;
    }

    protected abstract void closeInt();

    /**
     * use simple USB request supported by all devices to test if connection is still valid
     */
    protected void testConnection() throws IOException {
        byte[] buf = new byte[2];
        int len = mConnection.controlTransfer(0x80 /*DEVICE*/, 0 /*GET_STATUS*/, 0, 0, buf, buf.length, 200);
        if (len < 0)
            throw new IOException("USB get_status request failed");
    }

    @Override
    public int read(final byte[] dest, final int timeout) throws IOException {
        return read(dest, timeout, true);
    }

    protected int read(final byte[] dest, final int timeout, boolean testConnection) throws IOException {
        if (mConnection == null) {
            throw new IOException("Connection closed");
        }
        if (dest.length <= 0) {
            throw new IllegalArgumentException("Read buffer to small");
        }
        final int nread;

        if (mEnableAsyncReads) {

            if (timeout != 0) {
                // bulkTransfer will cause data loss with short timeout + high baud rates + continuous transfer
                //   https://stackoverflow.com/questions/9108548/android-usb-host-bulktransfer-is-losing-data
                // but mConnection.requestWait(timeout) available since Android 8.0 es even worse,
                // as it crashes with short timeout, e.g.
                //   A/libc: Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x276a in tid 29846 (pool-2-thread-1), pid 29618 (.usbserial.test)
                //     /system/lib64/libusbhost.so (usb_request_wait+192)
                //     /system/lib64/libandroid_runtime.so (android_hardware_UsbDeviceConnection_request_wait(_JNIEnv*, _jobject*, long)+84)
                // data loss / crashes were observed with timeout up to 200 msec
                long endTime = testConnection ? MonotonicClock.millis() + timeout : 0;
                int readMax = Math.min(dest.length, MAX_READ_SIZE);
                nread = mConnection.bulkTransfer(mReadEndpoint, dest, readMax, timeout);
                // Android error propagation is improvable:
                //  nread == -1 can be: timeout, connection lost, buffer to small, ???
                if (nread == -1 && testConnection && MonotonicClock.millis() < endTime)
                    testConnection();

            } else {
                final ByteBuffer buf = ByteBuffer.wrap(dest);
                if (!mUsbRequest.queue(buf, dest.length)) {
                    throw new IOException("Queueing USB request failed");
                }
                final UsbRequest response = mConnection.requestWait();
                if (response == null) {
                    throw new IOException("Waiting for USB request failed");
                }
                nread = buf.position();
                // Android error propagation is improvable:
                //   response != null & nread == 0 can be: connection lost, buffer to small, ???
                if (nread == 0) {
                    testConnection();
                }
            }
        } else {
            final int numBytesRead;
            synchronized (mReadBufferLock) {
                final ByteBuffer buf = ByteBuffer.wrap(dest);
                int readAmt = Math.min(dest.length, dest.length);
                numBytesRead = mConnection.bulkTransfer(mReadEndpoint, dest, readAmt,
                        timeout);
                if (numBytesRead < 0) {
                    // This sucks: we get -1 on timeout, not 0 as preferred.
                    // We *should* use UsbRequest, except it has a bug/api oversight
                    // where there is no way to determine the number of bytes read
                    // in response :\ -- http://b.android.com/28023
                    if (timeout == Integer.MAX_VALUE) {
                        // Hack: Special case "~infinite timeout" as an error.
                        return -1;
                    }
                    return 0;
                }
                System.arraycopy(buf, 0, dest, 0, numBytesRead);
            }
            nread = numBytesRead;
        }
        return Math.max(nread, 0);
    }

    // https://android.googlesource.com/kernel/common/+/android-trusty-3.10/Documentation/usb/usbmon.txt
    // cat /sys/kernel/debug/usb/devices
    // T:  Bus=02 Lev=01 Prnt=01 Port=00 Cnt=01 Dev#=  2 Spd=480  MxCh= 0
    //D:  Ver= 2.00 Cls=ef(misc ) Sub=02 Prot=01 MxPS=64 #Cfgs=  1
    //P:  Vendor=16c0 ProdID=0483 Rev= 2.80
    //S:  Manufacturer=Teensyduino
    //S:  Product=USB Serial
    //S:  SerialNumber=11964340
    //C:* #Ifs= 2 Cfg#= 1 Atr=c0 MxPwr=100mA
    //A:  FirstIf#= 0 IfCount= 2 Cls=02(comm.) Sub=02 Prot=01
    //I:* If#= 0 Alt= 0 #EPs= 1 Cls=02(comm.) Sub=02 Prot=01 Driver=(none)
    //E:  Ad=82(I) Atr=03(Int.) MxPS=  16 Ivl=2ms
    //I:* If#= 1 Alt= 0 #EPs= 2 Cls=0a(data ) Sub=00 Prot=00 Driver=(none)
    //E:  Ad=03(O) Atr=02(Bulk) MxPS= 512 Ivl=0ms
    //E:  Ad=84(I) Atr=02(Bulk) MxPS= 512 Ivl=0ms
    //
    // cat /sys/kernel/debug/usb/usbmon/2u
    @Override
    public void write(final byte[] src, final int timeout) throws IOException, TimeoutException, BufferOverflowException {
        int offset = 0;
        long latency = 0;
        latency = MonotonicClock.millis();

        if (DEBUG) {
            Log.d(TAG, "write() length " + src.length);
        }
        final long endTime = (timeout == 0) ? 0 : (MonotonicClock.millis() + timeout);

        if (mConnection == null) {
            throw new IOException("Connection closed");
        }

        //Log.d(TAG, "DTR: " + getDTR() +  "RTS: " + getRTS());

        if (mEnableAsyncWrites) {

            // Teensy default: see https://forum.pjrc.com/index.php?threads/increase-the-usb-buffer-size-in-teensy-4-1.73656/
            if (src.length > 4096) {
                if (DEBUG) {
                    Log.d(TAG, "write() too large " + src.length);
                }
                return;
            }

            if (DEBUG) {
                Log.d(TAG, "write() newUsbRequest " + src.length);
            }

            try {

                    //if (!writeRequest.queue(ByteBuffer.wrap(src), src.length)) {
                    if (DEBUG) {
                        Log.d(TAG, "write() queue " + src.length);
                    }
                    //if (!writeRequest.queue(ByteBuffer.wrap(src), src.length)) {
                    ByteBuffer data = ByteBuffer.allocateDirect(src.length);
                    data.put(src);
                    //if (!writeRequest.queue(data)) {
                    if (!mUsbWriteRequest.queue(ByteBuffer.wrap(src))) {
                        throw new IOException("Error queueing request.");
                    }
                    latency = MonotonicClock.millis() - latency;
                    UsbRequest response = mConnection.requestWait();
                    if (response == null) {
                        throw new IOException("request response is null");
                    }


            } finally {
                //mUsbWriteRequest.close();
            }
            if (DEBUG) {
                if (latency > 0) {
                    Log.d(TAG, "Wrote " + src.length + "latency " + latency);
                }

            }
        } else {

            while (offset < src.length) {
                int requestTimeout;
                final int requestLength;
                final int actualLength;

                synchronized (mWriteBufferLock) {
                    final byte[] writeBuffer;

                    if (mWriteBuffer == null) {
                        mWriteBuffer = new byte[mWriteEndpoint.getMaxPacketSize()];
                    }
                    if (DEBUG) {
                        Log.d(TAG, "write mWritebufferlen = " + mWriteBuffer.length);
                    }

                    requestLength = Math.min(src.length - offset, mWriteBuffer.length);
                    if (offset == 0) {
                        writeBuffer = src;
                    } else {
                        // bulkTransfer does not support offsets, make a copy.
                        System.arraycopy(src, offset, mWriteBuffer, 0, requestLength);
                        writeBuffer = mWriteBuffer;
                    }

                    if (timeout == 0 || offset == 0) {
                        requestTimeout = timeout;
                    } else {
                        requestTimeout = (int) (endTime - MonotonicClock.millis());
                        if (requestTimeout == 0)
                            requestTimeout = -1;
                    }
                    if (requestTimeout < 0) {
                        actualLength = -2;
                    } else {
                        latency = MonotonicClock.millis();
                        actualLength = mConnection.bulkTransfer(mWriteEndpoint, writeBuffer, requestLength, requestTimeout);
                        latency = MonotonicClock.millis() - latency;
                    }

                    if (DEBUG) {
                        Log.d(TAG, "Wrote " + actualLength + "/" + requestLength + " offset " + offset + "/" + src.length + " timeout " + requestTimeout + "latency " + latency);
                    }
                    if (actualLength <= 0) {
                        if (timeout != 0 && MonotonicClock.millis() >= endTime) {
                            SerialTimeoutException ex = new SerialTimeoutException("Error writing " + requestLength + " bytes at offset " + offset + " of total " + src.length + ", rc=" + actualLength);
                            ex.bytesTransferred = offset;
                            throw ex;
                        } else {
                            throw new IOException("Error writing " + requestLength + " bytes at offset " + offset + " of total " + src.length);
                        }
                    }
                }
                offset += actualLength;
            }
        }
    }

    @Override
    public boolean isOpen() {
        return mConnection != null;
    }

    @Override
    public abstract void setParameters(int baudRate, int dataBits, int stopBits, @Parity int parity) throws IOException;

    @Override
    public boolean getCD() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getCTS() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getDSR() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getDTR() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDTR(boolean value) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getRI() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getRTS() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRTS(boolean value) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract EnumSet<ControlLine> getControlLines() throws IOException;

    @Override
    public abstract EnumSet<ControlLine> getSupportedControlLines() throws IOException;

    @Override
    public void purgeHwBuffers(boolean purgeWriteBuffers, boolean purgeReadBuffers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBreak(boolean value) throws IOException {
        throw new UnsupportedOperationException();
    }

}
