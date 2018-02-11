package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.provider.event.PositionEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by rmc on 2/7/18.
 */

public class BBFindMyFriends {

    private static final String TAG = "BB.Gps";
    Context mContext;
    BBRadio mRadio;
    BBGps mGps;
    IoTClient mIotClient;
    findMyFriendsCallback mFindMyFriendsCallback = null;
    long mLastFix = 0;
    static final int kMaxFixAge = 5000;
    static final int kMagicNumberLen = 2;
    static final int [] kMagicNumber = new int[] {0x02, 0xcb};
    int mLat;
    int mLon;
    int mAmIAccurate;
    double mTheirLat;
    double mTheirLon;
    int mThereAccurate;
    long mLastSend = 0;
    long mLastRecv = 0;

    public BBFindMyFriends(Context context, final BBRadio radio, BBGps gps, IoTClient iotclient) {
        mContext = context;
        mRadio = radio;
        mGps = gps;
        mIotClient = iotclient;
        l("Starting FindMyFriends");

        if (mRadio == null) {
            l("No Radio!");
            return;
        }

        mRadio.attach(new BBRadio.radioEvents() {
            @Override
            public void receivePacket(byte[] bytes, int sigStrength) {
                l("FMF Packet: len(" + bytes.length + "), data: " + bytesToHex(bytes));
                if (processReceive(bytes)) {
                    l("theirLat = " + mTheirLat + ", theirLon = " + mTheirLon);
                    mIotClient.sendUpdate("bbevent", "[" +
                            "remote," + sigStrength + "," + mTheirLat + "," + mTheirLon + "]");
                }
            }

            @Override
            public void GPSevent(PositionEvent gps) {
                l("FMF Position: " + gps.toString());
                Position pos = gps.getPosition();
                mLat = (int)(pos.getLatitude() * 1000);
                mLon = (int)(pos.getLongitude() * 1000);
                long sinceLastFix = System.currentTimeMillis() - mLastFix;

                if (sinceLastFix < kMaxFixAge) {
                    mIotClient.sendUpdate("bbevent", "[" +
                                "local," + 0 + "," + mTheirLat + "," + mTheirLon + "]");

                    // Check GPS data is not stale
                    int len = 2 * 4 + 1 + kMagicNumberLen + 1;
                    ByteArrayOutputStream radioPacket = new ByteArrayOutputStream();

                    for (int i = 0; i < kMagicNumberLen; i++) {
                        radioPacket.write(kMagicNumber[i]);
                    }
                    radioPacket.write(mLat & 0xFF);
                    radioPacket.write((mLat >> 8) & 0xFF);
                    radioPacket.write(mLon & 0xFF);
                    radioPacket.write((mLon >> 8) & 0xFF);

                    radioPacket.write(mAmIAccurate);
                    radioPacket.write(0);

                    l("Sending packet...");
                    mRadio.broadcast(radioPacket.toByteArray());
                    mLastSend = System.currentTimeMillis();
                    l("Sent packet...");
                }
            }

            @Override
            public void timeEvent(Time time) {
                l("FMF Time: " + time.toString());
            }
        });
    }

    public interface findMyFriendsCallback {
        public void audioTrack(int track);
        public void videoTrack(int track);
        public void globalAlert(int alert);
    }

    public void attach(BBFindMyFriends.findMyFriendsCallback newfunction) {

        mFindMyFriendsCallback = newfunction;
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

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    boolean processReceive(byte [] packet) {
        ByteArrayInputStream bytes = new ByteArrayInputStream(packet);
        for (int i = 0; i < kMagicNumberLen; i++) {
            int b = bytes.read() & 0xff;
            if (kMagicNumber[i] != b) {
                l("rogue packet not for us!");
                return false;
            }
        }
        mTheirLat = (double) ((bytes.read() & 0xff) +
                ((bytes.read() & 0xff) << 8) +
                ((bytes.read() & 0xff) << 16) +
                ((bytes.read() & 0xff) << 24)) / 1000000.0;
        mTheirLon = (double)((bytes.read() & 0xff) +
                ((bytes.read() & 0xff) << 8) +
                ((bytes.read() & 0xff) << 16) +
                ((bytes.read() & 0xff) << 24)) / 1000000.0;
        mThereAccurate = bytes.read();
        mLastRecv = System.currentTimeMillis();
        return true;
    }

}
