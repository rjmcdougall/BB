package com.richardmcdougall.bb;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.SentenceValidator;
import net.sf.marineapi.nmea.io.ExceptionListener;
import net.sf.marineapi.provider.PositionProvider;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.PositionListener;
import net.sf.marineapi.nmea.util.Time;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by rmc on 2/6/18.
 */

public class Gps {

    private static final String TAG = "BB.Gps";
    public GpsEvents mGpsCallback = null;
    public Context mContext = null;
    private PipedInputStream mSentenceInput;
    private PipedOutputStream mSentenceOutput;
    private SentenceReader mSR;
    PositionProvider provider;

    public interface GpsEvents {
        void timeEvent(net.sf.marineapi.nmea.util.Time time);
        void positionEvent(net.sf.marineapi.provider.event.PositionEvent gps);
    }

    public void attach(GpsEvents newfunction) {

        mGpsCallback = newfunction;
    }


    public Gps(Context context) {
        mContext = context;
        l("Gps starting");

        try {
            mSentenceInput = new PipedInputStream();
            mSentenceOutput = new PipedOutputStream(mSentenceInput);
        } catch (Exception e) {
            l("Gps pipe failed: " + e.getMessage());
        }
        try {
            mSR = new SentenceReader(mSentenceInput);
            mSR.setExceptionListener(new ExceptionListener() {
                public void onException(Exception e) {
                    l("exc: " + e.getMessage());
                }});
            provider = new PositionProvider(mSR);
            provider.addListener(new PositionListener() {
                public void providerUpdate(PositionEvent evt) {
                    // do something with the data..
                    l("TPV: " + evt.toString());
                    if (mGpsCallback != null) {
                        mGpsCallback.positionEvent(evt);
                    }
                }
            });
            mSR.addSentenceListener(new SentenceListener() {
                @Override
                public void readingPaused() {
                    l("readingPaused");
                }

                @Override
                public void readingStarted() {
                    l("readingStarted");
                }

                @Override
                public void readingStopped() {
                    l("readingStopped");
                }

                @Override
                public void sentenceRead(SentenceEvent event) {
                    // here we receive each sentence read from the port
                    //l(event.getSentence().toString());
                    GGASentence s = (GGASentence) event.getSentence();
                    if (s.isValid()) {
                        l("Sat Time: " + s.getTime().toString());
                        if (mGpsCallback != null) {
                            mGpsCallback.timeEvent(s.getTime());
                        }
                    }
                }

            }, SentenceId.GGA);

            mSR.start();
            l("SentenceListener started");
        } catch (Exception e) {
            l("Gps start failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void addStr(String str) {
        try {
            mSentenceOutput.write((str + "\n").getBytes());
            mSentenceOutput.flush();
        } catch (Exception e) {
            l("Gps addStr failed: " + e.getMessage());
        }
    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

}
