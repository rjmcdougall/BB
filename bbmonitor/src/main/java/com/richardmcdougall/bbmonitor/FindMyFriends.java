package com.richardmcdougall.bbmonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.provider.event.PositionEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by rmc on 2/7/18.
 */

public class FindMyFriends {

    private static final String TAG = "BB.FMF";
    private Context mContext;
    private RF mRadio;
    private RFAddress mRFAddress = null;
    private BBService mBBService;
    private findMyFriendsCallback mFindMyFriendsCallback = null;
    long mLastFix = 0;
    static final int kMaxFixAge = 30000;
    static final int kMagicNumberLen = 2;
    static final int krepeatedBy = 0;
    static final int [] kTrackerMagicNumber = new int[] {0x02, 0xcb};
    static final int [] kGPSMagicNumber = new int[] {0xbb, 0x01};
    static final int [] kRPTStatsMagicNumber = new int[] {0xbb, 0x02};
    int mLat;
    int mLon;
    int mAlt;
    int mSpeed;
    int mHeading;
    int mAmIAccurate;
    long mLastSend = 0;
    long mLastRecv = 0;
    private int mBoardAddress = 0;
    String mBoardId;
    byte[] mLastHeardLocation;

    public FindMyFriends(Context context, BBService service,
                         final RF radio) {
        mContext = context;
        mRadio = radio;
        mBBService = service;
        l("Starting FindMyFriends");

        if (mRadio == null) {
            l("No Radio!");
            return;
        }
        mRFAddress = mRadio.mRFAddress;

        mRadio.attach(new RF.radioEvents() {
            @Override
            public void receivePacket(byte[] bytes, int sigStrength) {
                l("FMF Packet: len(" + bytes.length + "), data: " + bytesToHex(bytes));
                if (processReceive(bytes, sigStrength)) {

                }
            }

            @Override
            public void GPSevent(PositionEvent gps) {
            }

            @Override
            public void timeEvent(Time time) {
                d("FMF Time: " + time.toString());
            }
        });


     }


    public interface findMyFriendsCallback {

        public void audioTrack(int track);
        public void videoTrack(int track);
        public void globalAlert(int alert);

    }

    public void attach(FindMyFriends.findMyFriendsCallback newfunction) {

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
    }

    public void d(String s) {
        if (BBService.debug == true) {
            Log.v(TAG, s);
            sendLogMsg(s);
        }
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

    boolean processReceive(byte [] packet, int sigStrength) {

        int address;
        double lat;
        double lon;
        int accurate;
        int batt;

        ByteArrayInputStream bytes = new ByteArrayInputStream(packet);

        int recvMagicNumber = magicNumberToInt(
                new int[] { bytes.read(), bytes.read()});

        if (recvMagicNumber == magicNumberToInt(kGPSMagicNumber)) {
            d("BB GPS Packet");
            address = (int) ((bytes.read() & 0xff) +
                    ((bytes.read() & 0xff) << 8));
            int repeatedBy = bytes.read();
            lat = (double) ((bytes.read() & 0xff) +
                    ((bytes.read() & 0xff) << 8) +
                    ((bytes.read() & 0xff) << 16) +
                    ((bytes.read() & 0xff) << 24)) / 1000000.0;
            lon = (double)((bytes.read() & 0xff) +
                    ((bytes.read() & 0xff) << 8) +
                    ((bytes.read() & 0xff) << 16) +
                    ((bytes.read() & 0xff) << 24)) / 1000000.0;
            batt = bytes.read();
            bytes.read();
            bytes.read();
            bytes.read();
            accurate = bytes.read();
            mLastRecv = System.currentTimeMillis();
            mLastHeardLocation = packet.clone();
            l("BB " + mRFAddress.boardAddressToName(address) + " strength " +
                    sigStrength + " theirLat = " + lat + ", theirLon = " + lon +
            " batt " + batt + "%");
            updateBoardLocations(address, sigStrength, lat, lon, batt, false);
            return true;
        } else if (recvMagicNumber == magicNumberToInt(kTrackerMagicNumber)) {
            d("tracker packet");
            lat = (double) ((bytes.read() & 0xff) +
                    ((bytes.read() & 0xff) << 8) +
                    ((bytes.read() & 0xff) << 16) +
                    ((bytes.read() & 0xff) << 24)) / 1000000.0;
            lon = (double)((bytes.read() & 0xff) +
                    ((bytes.read() & 0xff) << 8) +
                    ((bytes.read() & 0xff) << 16) +
                    ((bytes.read() & 0xff) << 24)) / 1000000.0;
            accurate = bytes.read();
            mLastRecv = System.currentTimeMillis();
            l( "Tracker! "  + " strength " + sigStrength + "theirLat = " + lat + ", theirLon = " + lon);
            return true;
        } else if (recvMagicNumber == magicNumberToInt(kRPTStatsMagicNumber)) {
            d("repeater stats packet");
            address = (int) ((bytes.read() & 0xff) +
                    ((bytes.read() & 0xff) << 8));
            int battery = bytes.read();
            int packetsForwarded = (int) ((bytes.read() & 0xff) +
                    ((bytes.read() & 0xff) << 8) +
                    ((bytes.read() & 0xff) << 16) +
                    ((bytes.read() & 0xff) << 24));
            int packetsIgnored = (int)((bytes.read() & 0xff) +
                    ((bytes.read() & 0xff) << 8) +
                    ((bytes.read() & 0xff) << 16) +
                    ((bytes.read() & 0xff) << 24));
            mLastRecv = System.currentTimeMillis();
            l( "repeater "  + " address " + address +
                    "battery = " + battery + ", " +
                    "packets-repeated = " + packetsForwarded +
                    "packets-ignored = " + packetsIgnored);
            l("repeater " + address + " battery " + battery);
            updateBoardLocations(address,
                    sigStrength,
                    0, 0,
                    battery, true);
        } else {
            d("rogue packet not for us!");
        }
        return false;
    }

    private static final int magicNumberToInt(int[] magic) {
        int magicNumber = 0;
        for (int i = 0; i < kMagicNumberLen; i++) {
            magicNumber = magicNumber + (magic[i] << ((kMagicNumberLen - 1 - i) * 8));
        }
        return (magicNumber);
    }

    int log2(int number) {
        return (int)(Math.log(number)/Math.log(2));
    }

    private void updateBoardLocations(int address, int sigstrength,
                                      double lat, double lon, int batt, boolean isRpt) {

        Intent in = new Intent(BBService.ACTION_BB_LOCATION);
        if (isRpt) {
            in.putExtra("name", "repeater-" + log2(32768 - address));
        } else {
            in.putExtra("name", mRFAddress.boardAddressToName(address));
        }
        in.putExtra("sig", sigstrength);
        in.putExtra("lat", lat);
        in.putExtra("lon", lon);
        in.putExtra("batt", batt);
        LocalBroadcastManager.getInstance(mBBService).sendBroadcast(in);
    }

}
