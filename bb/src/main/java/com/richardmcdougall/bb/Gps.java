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

    private static final String TAG = "BB.Gps";
    public GpsEvents mGpsCallback = null;
    public Context mContext = null;
    private PipedInputStream mSentenceInput;
    private PipedOutputStream mSentenceOutput;
    private SentenceReader mSR;
    PositionProvider provider;
    private BBService mBBService;

    public interface GpsEvents {
        void timeEvent(net.sf.marineapi.nmea.util.Time time);
        void positionEvent(net.sf.marineapi.provider.event.PositionEvent gps);
    }

    public void attach(GpsEvents newfunction) {

        mGpsCallback = newfunction;
    }


    public Gps(BBService service, Context context) {
        mContext = context;
        mBBService = service;
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
                    l("exc: " + e.getMessage() + " " + e.getStackTrace());
                }
            });
            provider = new PositionProvider(mSR);
            provider.addListener(new PositionListener() {
                public void providerUpdate(PositionEvent evt) {
                    /* XXX TODO This event can fail with the following stack trace. However, that exception is handled
                       in the SentenceReader class, and not thrown up further. So it's hard to diagnose which part of the
                       calls underneath cause the actual failure -jib
                        07-13 19:05:40.594   690   911 W SentenceReader: Exception caught from SentenceListener
                        07-13 19:05:40.594   690   911 W SentenceReader: java.lang.IllegalArgumentException: Year must be two or four digit value
                        07-13 19:05:40.594   690   911 W SentenceReader: 	at net.sf.marineapi.nmea.util.Date.setYear(Date.java:191)
                        07-13 19:05:40.594   690   911 W SentenceReader: 	at net.sf.marineapi.nmea.util.Date.<init>(Date.java:70)
                        07-13 19:05:40.594   690   911 W SentenceReader: 	at net.sf.marineapi.nmea.parser.RMCParser.getDate(RMCParser.java:93)
                        07-13 19:05:40.594   690   911 W SentenceReader: 	at net.sf.marineapi.provider.PositionProvider.createProviderEvent(PositionProvider.java:91)
                        07-13 19:05:40.594   690   911 W SentenceReader: 	at net.sf.marineapi.provider.PositionProvider.createProviderEvent(PositionProvider.java:57)
                        07-13 19:05:40.594   690   911 W SentenceReader: 	at net.sf.marineapi.provider.AbstractProvider.sentenceRead(AbstractProvider.java:217)
                        07-13 19:05:40.594   690   911 W SentenceReader: 	at net.sf.marineapi.nmea.io.SentenceReader.fireSentenceEvent(SentenceReader.java:206)
                        07-13 19:05:40.594   690   911 W SentenceReader: 	at net.sf.marineapi.nmea.io.AbstractDataReader.run(AbstractDataReader.java:90)
                        07-13 19:05:40.594   690   911 W SentenceReader: 	at java.lang.Thread.run(Thread.java:764)
                     */
                    try {
                        // do something with the data..
                        d("TPV: " + evt.toString());
                        /* XXX So, as long as we look up the year BEFORE letting the callback do it's work, I have not
                           been able to reproduce the above stacktrace. I can't explain why this would be a workaround,
                           but, choose your battles :( For now, let's leave the debug statement in place -jib
                        */
                        d("GPS Date Year: " + evt.getDate().getYear());

                        if (mGpsCallback != null) {
                            mGpsCallback.positionEvent(evt);
                        }
                        Position pos = evt.getPosition();
                        if (pos != null) {
                            double lat = pos.getLatitude();
                            double lon = pos.getLongitude();
                            Intent in = new Intent(BBService.ACTION_BB_LOCATION);
                            in.putExtra("lat", evt.getPosition().getLatitude());
                            in.putExtra("lon", evt.getPosition().getLatitude());
                            LocalBroadcastManager.getInstance(mBBService).sendBroadcast(in);
                        }
                    } catch (Exception e) {
                        l("Position Event failed: " + e.getMessage() + " " + e.getStackTrace());
                    }
                }
            });

            /* XXX TODO this is a RMC experiment, currently not functional; feature flagged -jib */
            if (BurnerBoardUtil.fEnableGpsTime) {
                l( "Enabling GPS Time collection");
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
                        l( "Sentence read: " + event.getSentence().toString());
                        GGASentence s = (GGASentence) event.getSentence();
                        if (s.isValid()) {
                            l("Sat Time: " + s.getTime().toString());
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

    public void d(String s) {
        if (DebugConfigs.DEBUG_GPS) {
            Log.v(TAG, s);
            sendLogMsg(s);
        }
    }

}
