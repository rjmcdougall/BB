package com.richardmcdougall.bb;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.io.ExceptionListener;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.provider.PositionProvider;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.PositionListener;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Created by rmc on 2/6/18.
 */

public class Gps {

    // This enabled GPS Time being polled
    public static final boolean ENABLE_GPS_TIME = true;
    private static final String TAG = "BB.Gps";
    public GpsEvents mGpsCallback = null;
    private PipedInputStream mSentenceInput;
    private PipedOutputStream mSentenceOutput;
    private SentenceReader mSR;
    PositionProvider provider;
    private BBService service;

    public interface GpsEvents {
        void timeEvent(net.sf.marineapi.nmea.util.Time time);
        void positionEvent(net.sf.marineapi.provider.event.PositionEvent gps);
    }

    public void attach(GpsEvents newfunction) {

        mGpsCallback = newfunction;
    }


    public Gps(BBService service, Context context) {
        this.service = service;
        l("Gps starting");

        try {
            mSentenceInput = new PipedInputStream();
            mSentenceOutput = new PipedOutputStream(mSentenceInput);
        } catch (Exception e) {
            e("Pipe failed: " + e.getMessage());
        }
        try {
            mSR = new SentenceReader(mSentenceInput);
            mSR.setExceptionListener(new ExceptionListener() {
                public void onException(Exception e) {
                    e("Exception Listener " + e.getMessage() + " " + e.getStackTrace());
                }
            });
            provider = new PositionProvider(mSR);
            provider.addListener(new PositionListener() {
                public void providerUpdate(PositionEvent evt) {
                    try {
                        // do something with the data..
                        d("TPV: " + evt.toString());
                        int i = evt.getDate().getYear(); // leave this here DKW
                        if (mGpsCallback != null) {
                            mGpsCallback.positionEvent(evt);
                        }
                        Position pos = evt.getPosition();
                        if (pos != null) {
                            Intent in = new Intent(ACTION.BB_LOCATION);
                            in.putExtra("lat", evt.getPosition().getLatitude());
                            in.putExtra("lon", evt.getPosition().getLatitude());
                            LocalBroadcastManager.getInstance(Gps.this.service).sendBroadcast(in);
                        }
                    } catch (Exception e) {
                        e("Position Event failed: " + e.getMessage() + " " + e.getStackTrace());
                    }
                }
            });

            if (ENABLE_GPS_TIME) {
                l( "Enabling GPS Time collection");
                mSR.addSentenceListener(new SentenceListener() {
                    @Override
                    public void readingPaused() {
                        l("Sentence Listener Paused");
                    }

                    @Override
                    public void readingStarted() {
                        l("Sentence Listener Started");
                    }

                    @Override
                    public void readingStopped() {
                        l("Sentence Listener Stopped");
                    }

                    @Override
                    public void sentenceRead(SentenceEvent event) {
                        // here we receive each sentence read from the port
                        d( "Sentence read: " + event.getSentence().toString());
                        GGASentence s = (GGASentence) event.getSentence();
                        if (s.isValid()) {
                            d("Sat Time: " + s.getTime().toString());
                            if (mGpsCallback != null) {
                                mGpsCallback.timeEvent(s.getTime());
                            }
                        }
                    }
                }, SentenceId.GGA);
            }

            mSR.start();
            l("SentenceListener started");
        } catch (Exception e) {
            e("Gps start failed: " + e.getMessage());
        }
    }

    public void e(String logMsg) {
        Log.e(TAG, logMsg);
    }

    public void addStr(String str) {
        try {
            mSentenceOutput.write((str + "\n").getBytes());
            mSentenceOutput.flush();
        } catch (Exception e) {
            e("Gps addStr failed: " + e.getMessage());
        }
    }

    public void l(String s) {
        Log.v(TAG, s);
        service.sendLogMsg(s);
    }

    public void d(String s) {
        if (DebugConfigs.DEBUG_GPS) {
            Log.v(TAG, s);
            service.sendLogMsg(s);
        }
    }

}
