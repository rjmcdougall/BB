package com.richardmcdougall.bb;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

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

import timber.log.Timber;

/**
 * Created by rmc on 2/6/18.
 */

public class Gps {

    // This enabled GPS Time being polled
    public static final boolean ENABLE_GPS_TIME = false;
    public GpsEvents mGpsCallback = null;
    private PipedInputStream mSentenceInput;
    private PipedOutputStream mSentenceOutput;
    private SentenceReader mSR;
    private PositionProvider provider;
    private BBService service;

    public interface GpsEvents {
        void timeEvent(net.sf.marineapi.nmea.util.Time time);

        void positionEvent(net.sf.marineapi.provider.event.PositionEvent gps);
    }

    public void attach(GpsEvents newfunction) {

        mGpsCallback = newfunction;
    }


    public Gps(BBService service) {
        this.service = service;
        Timber.d("Gps starting");

        try {
            mSentenceInput = new PipedInputStream();
            mSentenceOutput = new PipedOutputStream(mSentenceInput);
        } catch (Exception e) {
            Timber.e("Pipe failed: " + e.getMessage());
        }
        try {
            mSR = new SentenceReader(mSentenceInput);
            mSR.setExceptionListener(new ExceptionListener() {
                public void onException(Exception e) {
                    Timber.e("Exception Listener " + e.getMessage() + " " + e.getStackTrace());
                }
            });
            provider = new PositionProvider(mSR);
            provider.addListener(new PositionListener() {
                public void providerUpdate(PositionEvent evt) {
                    try {
                        // do something with the data..
                        Timber.d("TPV: " + evt.toString());
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
                        Timber.e("Position Event failed: " + e.getMessage() + " " + e.getStackTrace());
                    }
                }
            });

            if (ENABLE_GPS_TIME) {
                Timber.d("Enabling GPS Time collection");
                mSR.addSentenceListener(new SentenceListener() {
                    @Override
                    public void readingPaused() {
                        Timber.d("Sentence Listener Paused");
                    }

                    @Override
                    public void readingStarted() {
                        Timber.d("Sentence Listener Started");
                    }

                    @Override
                    public void readingStopped() {
                        Timber.d("Sentence Listener Stopped");
                    }

                    @Override
                    public void sentenceRead(SentenceEvent event) {
                        // here we receive each sentence read from the port
                        Timber.d("Sentence read: " + event.getSentence().toString());
                        GGASentence s = (GGASentence) event.getSentence();
                        if (s.isValid()) {
                            Timber.d("Sat Time: " + s.getTime().toString());
                            if (mGpsCallback != null) {
                                mGpsCallback.timeEvent(s.getTime());
                            }
                        }
                    }
                }, SentenceId.GGA);
            }

            mSR.start();
            Timber.d("SentenceListener started");
        } catch (Exception e) {
            Timber.e("Gps start failed: " + e.getMessage());
        }
    }

    public void addStr(String str) {
        try {
            mSentenceOutput.write((str + "\n").getBytes());
            mSentenceOutput.flush();
        } catch (Exception e) {
            Timber.e("Gps addStr failed: " + e.getMessage());
        }
    }


}
