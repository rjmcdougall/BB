package com.richardmcdougall.bb;

import android.os.Handler;
import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;



import android.os.Handler;
import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


public class BBListenerAdapter implements SerialInputOutputManager.Listener {

    public final static String TAG = "BBListenerAdapter";
    private PipedInputStream mPipedInputStream;
    private PipedOutputStream mPipedOutputStream;
    private InputStreamReader mInputStreamReader;
    private BufferedReader mBufferedReader;
    private MainActivity mParent;
    private Handler mHandler;
    private Runnable mReadLineRunnable;
    private Runnable mLogLineRunnable;

    public BBListenerAdapter(MainActivity parent) {
        mParent = parent;
        mHandler = new Handler();

        try {//div setup
            mPipedInputStream = new PipedInputStream();
            mPipedOutputStream = new PipedOutputStream(mPipedInputStream);//connect input <=> output
            mInputStreamReader = new InputStreamReader(mPipedInputStream);
            mBufferedReader = new BufferedReader(mInputStreamReader);
        } catch (Exception e) {
            e("div setup failed: " + e.getMessage());
        }

    }

    void onDestroy() {
        mHandler.removeCallbacks(null);
    }

    //----------------------------------
    public void readLineInNewThread() {
        mReadLineRunnable = new readLineRunnable();
        mHandler.post(mReadLineRunnable);

        //Thread myThread = new Thread(new readLineRunnable());
        //myThread.start();

    }

    class readLineRunnable implements Runnable {

        @Override
        public void run() {
            String temp = readLine();//readLine should be in new thread according to pipeline documentation
            if (temp != null)
                mParent.receiveCommand(temp);//forward to parent to show on report view
            return;
        }

    }


    public String readLine() {

        synchronized (mPipedInputStream) {
            String ret = null;
            try {
                if (mBufferedReader.ready()) {
                    ret = mBufferedReader.readLine();
                }
            } catch (Exception e) {
                e("readLine failed");
            }
            return ret;
        }//sync

    }

    //----------------------------------

    public void logInNewThread(String s) {
        mLogLineRunnable = new logLineRunnable(s);
        mHandler.post(mLogLineRunnable);

        //Thread myThread = new Thread(new readLineRunnable());
        //myThread.start();

    }

    class logLineRunnable implements Runnable {

        private String mStr = null;

        public logLineRunnable(String s) {
            mStr = s;
        }


        @Override
        public void run() {
            if (mStr != null) mParent.receiveCommand(">>>USB " + mStr);//forward to parent to log

            return;
        }

    }


    private void l(String str) {

        Log.d(TAG, ">>>>>" + str);
        logInNewThread(">>>USB " + str);

    }

    private void e(String str) {

        Log.e(TAG, ">>>>>" + str);
        logInNewThread(">>>USB err " + str);

    }

    //listener interface

    @Override
    public void onNewData(byte[] data) {
        if (data.length > 0) {

            synchronized (mPipedInputStream) {
                for (int i = 0; i < data.length; i++) {
                    try {
                        mPipedOutputStream.write(data[i]);
                        mPipedOutputStream.flush();
                        if (data[i] == 10) {//newline
                            readLineInNewThread();//read newline and forward to application
                        }

                        if (data[i] == 0) {
                            //if we receive data 0, it is definitely binary data. so send the Arduino program 'r' commmand to switch to 'r'eadable
                            mParent.sendCommand("binary");
                            break;
                        }

                    } catch (Exception e) {
                        e("onNewData.write failed");
                    }
                }
            }//sync

        }//length >0

    }


    @Override
    public void onRunError(Exception e) {
        e("onRunError: " + e.getMessage());
    }

}
