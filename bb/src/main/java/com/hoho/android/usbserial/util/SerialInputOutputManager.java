/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.util;

import android.hardware.usb.UsbRequest;
import android.os.Process;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.richardmcdougall.bb.util.LatencyHistogram;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import android.hardware.usb.UsbDeviceConnection;

/**
 * Utility class which services a {@link UsbSerialPort} in its {@link #run()} method.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialInputOutputManager implements Runnable {

    public enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    public static boolean DEBUG = false;

    private static final String TAG = SerialInputOutputManager.class.getSimpleName();
    private static final int BUFSIZ = 16384;

    private String name = "";

    /**
     * default read timeout is infinite, to avoid data loss with bulkTransfer API
     */
    private int mReadTimeout = 0;
    private int mWriteTimeout = 0;

    private final Object mReadBufferLock = new Object();
    private final Object mWriteBufferLock = new Object();
    Queue<byte[]> mWriteBufferQueue = new LinkedList<>();

    private ByteBuffer mReadBuffer; // default size = getReadEndpoint().getMaxPacketSize()
    private ByteBuffer mWriteBuffer = ByteBuffer.allocate(BUFSIZ);

    Thread mThread = null;
    private int mThreadPriority = Process.THREAD_PRIORITY_URGENT_AUDIO;
    private State mState = State.STOPPED; // Synchronized by 'this'
    private Listener mListener; // Synchronized by 'this'
    private final UsbSerialPort mSerialPort;
    private final LatencyHistogram latencyHistogram = new LatencyHistogram(TAG);

    public interface Listener {
        /**
         * Called when new incoming data is available.
         */
        void onNewData(byte[] data);

        /**
         * Called when {@link SerialInputOutputManager#run()} aborts due to an error.
         */
        void onRunError(Exception e);
    }

    public SerialInputOutputManager(UsbSerialPort serialPort) {
        mSerialPort = serialPort;
        mReadBuffer = ByteBuffer.allocate(serialPort.getReadEndpoint().getMaxPacketSize());
        latencyHistogram.registerMethod("serialWrite");
        latencyHistogram.enableAutoLogging(30); // Log every 30 seconds for serial operations
    }

    public void setName(String name) {
        this.name = name;
    }

    public SerialInputOutputManager(UsbSerialPort serialPort, Listener listener) {
        mSerialPort = serialPort;
        mListener = listener;
        mReadBuffer = ByteBuffer.allocate(serialPort.getReadEndpoint().getMaxPacketSize());
        latencyHistogram.registerMethod("serialWrite");
        latencyHistogram.enableAutoLogging(30); // Log every 30 seconds for serial operations
    }

    public synchronized void setListener(Listener listener) {
        mListener = listener;
    }

    public synchronized Listener getListener() {
        return mListener;
    }

    /**
     * setThreadPriority. By default a higher priority than UI thread is used to prevent data loss
     *
     * @param threadPriority see {@link Process#setThreadPriority(int)}
     */
    public void setThreadPriority(int threadPriority) {
        if (mState != State.STOPPED)
            throw new IllegalStateException("threadPriority only configurable before SerialInputOutputManager is started");
        mThreadPriority = threadPriority;
    }

    /**
     * read/write timeout
     */
    public void setReadTimeout(int timeout) {
        // when set if already running, read already blocks and the new value will not become effective now
        if (mReadTimeout == 0 && timeout != 0 && mState != State.STOPPED)
            throw new IllegalStateException("readTimeout only configurable before SerialInputOutputManager is started");
        mReadTimeout = timeout;
    }

    public int getReadTimeout() {
        return mReadTimeout;
    }

    public void setWriteTimeout(int timeout) {
        mWriteTimeout = timeout;
    }

    public int getWriteTimeout() {
        return mWriteTimeout;
    }

    /**
     * read/write buffer size
     */
    public void setReadBufferSize(int bufferSize) {
        if (getReadBufferSize() == bufferSize)
            return;
        synchronized (mReadBufferLock) {
            mReadBuffer = ByteBuffer.allocate(bufferSize);
        }
    }

    public int getReadBufferSize() {
        return mReadBuffer.capacity();
    }

    public void setWriteBufferSize(int bufferSize) {
        if (getWriteBufferSize() == bufferSize)
            return;
        synchronized (mWriteBufferLock) {
            ByteBuffer newWriteBuffer = ByteBuffer.allocate(bufferSize);
            if (mWriteBuffer.position() > 0)
                newWriteBuffer.put(mWriteBuffer.array(), 0, mWriteBuffer.position());
            mWriteBuffer = newWriteBuffer;
        }
    }

    public int getWriteBufferSize() {
        return mWriteBuffer.capacity();
    }

    /**
     * when using writeAsync, it is recommended to use readTimeout != 0,
     * else the write will be delayed until read data is available
     */
    public void writeAsync(byte[] data) {
        if (DEBUG) {

            Log.d(TAG, "writeAsync " + data.length);
        }
        synchronized (mWriteBufferLock) {
            if (mWriteBufferQueue.size() > 100) {
                BLog.d(TAG, "large write buffer " + mWriteBufferQueue.size());
                try {
                    mWriteBufferQueue.clear();
                    Thread.sleep(10);
                } catch (Exception e) {

                }
            } else {
                mWriteBufferQueue.add(data);
            }
        }
        try {
            if (mThread != null) {
                mThread.interrupt();
            }
        } catch (Exception e) {
        }
    }

    /**
     * start SerialInputOutputManager in separate thread
     */
    public void start() {
        if (mState != State.STOPPED)
            throw new IllegalStateException("already started");
        new Thread(this, this.getClass().getSimpleName()).start();
    }

    /**
     * stop SerialInputOutputManager thread
     * <p>
     * when using readTimeout == 0 (default), additionally use usbSerialPort.close() to
     * interrupt blocking read
     */
    public synchronized void stop() {
        if (getState() == State.RUNNING) {
            Log.i(TAG, "Stop requested");
            mState = State.STOPPING;
        }
    }

    public synchronized State getState() {
        return mState;
    }

    /**
     * Continuously services the read and write buffers until {@link #stop()} is
     * called, or until a driver exception is raised.
     */
    @Override
    public void run() {
        synchronized (this) {
            if (getState() != State.STOPPED) {
                throw new IllegalStateException("Already running");
            }
            mState = State.RUNNING;
            mThread = Thread.currentThread();
        }
        mThread.setName("SIO" + name);
        Log.i(TAG, "Running ...");
        try {
            if (mThreadPriority != Process.THREAD_PRIORITY_DEFAULT)
                Process.setThreadPriority(mThreadPriority);
            while (true) {
                if (getState() != State.RUNNING) {
                    Log.i(TAG, "Stopping mState=" + getState());
                    break;
                }
                step();
            }
        } catch (Exception e) {
            Log.w(TAG, "Run ending due to exception: " + e.getMessage(), e);
            final Listener listener = getListener();
            if (listener != null) {
                listener.onRunError(e);
            }
        } finally {
            synchronized (this) {
                mState = State.STOPPED;
                Log.i(TAG, "Stopped");
                // Clean up latency histogram
                latencyHistogram.shutdown();
            }
        }
    }

    private void step() throws IOException {
        // Log.d(TAG, "step");

        // Handle incoming data.
        byte[] buffer;

        synchronized (mReadBufferLock) {
            buffer = mReadBuffer.array();
        }
        int len = 0;

        // If there are writes to do, skip reading for now
        if (mWriteBufferQueue.isEmpty()) {
            if (mReadTimeout > 0) {
                len = mSerialPort.read(buffer, mReadTimeout);
            } else {
                try {
                    if (DEBUG) {
                        Log.d(TAG, "sleep");
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (DEBUG) {
                        Log.d(TAG, "interrupt");
                    }
                }

            }
            if (len > 0) {
                if (DEBUG) {
                    Log.d(TAG, "Read data len=" + len);
                }
                final Listener listener = getListener();
                if (listener != null) {
                    final byte[] data = new byte[len];
                    System.arraycopy(buffer, 0, data, 0, len);
                    listener.onNewData(data);
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "continue after interrupt");
        }

        // Handle outgoing data.
        buffer = null;
        synchronized (mWriteBufferLock) {
            if (!mWriteBufferQueue.isEmpty()) {
                buffer = mWriteBufferQueue.remove();
            }
        }

        if (buffer != null) {
            long latency = MonotonicClock.millis();

            if (DEBUG) {
                Log.d(TAG, "Writing data len=" + len);
            }
            try {
                long writeStart = MonotonicClock.millis();
                mSerialPort.write(buffer, mWriteTimeout);
                long writeEnd = MonotonicClock.millis();
                latencyHistogram.recordLatency("serialWrite", writeEnd - writeStart);
            } catch (BufferOverflowException e) {
                // Flow control; put back on buffer and try again.
                if (DEBUG) {
                    Log.d(TAG, "Buffer Overflow");
                }
                //mWriteBuffer.put(buffer);
            } catch (Exception e) {
                if (DEBUG) {
                    Log.d(TAG, "IO Exception");
                }
            }
            if (DEBUG) {
                Log.d(TAG, "Wrote data len=" + len + " latency = " + (MonotonicClock.millis() - latency));
            }
        }
    }

}

