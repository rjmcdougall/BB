package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.provider.event.PositionEvent;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;

/**
 * Created by rmc on 2/7/18.
 */

public class FindMyFriends {

    private static final String TAG = "BB.FMF";
    private Context mContext;
    private RF mRadio;
    private RFAddress mRFAddress = null;
    private Gps mGps;
    private IoTClient mIotClient;
    private BBService mBBService;
    private findMyFriendsCallback mFindMyFriendsCallback = null;
    long mLastFix = 0;
    static final int kMaxFixAge = 30000;
    static final int kMagicNumberLen = 2;
    static final int krepeatedBy = 0;
    static final int [] kTrackerMagicNumber = new int[] {0x02, 0xcb};
    static final int [] kGPSMagicNumber = new int[] {0xbb, 0x01};
    int mLat;
    int mLon;
    int mAlt;
    int mSpeed;
    int mHeading;
    int mAmIAccurate;
    int mTheirAddress;
    double mTheirLat;
    double mTheirLon;
    double mTheirAlt;
    int mTheirBatt;
    double mTheirSpeed;
    double mTheirHeading;
    int mThereAccurate;
    long mLastSend = 0;
    long mLastRecv = 0;
    private int mBoardAddress = 0;
    String mBoardId;
    byte[] mLastHeardLocation;

    public FindMyFriends(Context context, BBService service,
                         final RF radio, Gps gps, IoTClient iotclient) {
        mContext = context;
        mRadio = radio;
        mGps = gps;
        mIotClient = iotclient;
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
                d("FMF Position: " + gps.toString());
                Position pos = gps.getPosition();
                mLat = (int)(pos.getLatitude() * 1000000);
                mLon = (int)(pos.getLongitude() * 1000000);
                mAlt = (int)(pos.getAltitude() * 1000000);
                long sinceLastFix = System.currentTimeMillis() - mLastFix;

                // since the address is loaded from a JSON you may get a race condition
                // so try to find the address of this board each time until you do.
                if(mBoardAddress<=0)
                    mBoardAddress = mRFAddress.getBoardAddress(mBBService.getBoardId());

                if (sinceLastFix > kMaxFixAge) {
                    d("FMF: sending GPS update");
                    mIotClient.sendUpdate("bbevent", "[" +
                            mRFAddress.boardAddressToName(mBoardAddress) + "," + 0 + "," + mLat  / 1000000.0 + "," + mLon  / 1000000.0 + "]");
                    broadcastGPSpacket(mLat, mLon, mAlt, mAmIAccurate, 0, 0);
                    mLastFix = System.currentTimeMillis();

                }
            }

            @Override
            public void timeEvent(Time time) {
                d("FMF Time: " + time.toString());
            }
        });
        addTestBoardLocations();


     }

    // GPS Packet format =
    //         [0] kGPSMagicNumber byte one
    //         [1] kGPSMagicNumber byte two
    //         [2] board address bits 0-7
    //         [3] board address bits 8-15
    //         [4] repeatedBy
    //         [5] lat bits 0-7
    //         [6] lat bits 8-15
    //         [7] lat bits 16-23
    //         [8] lat bits 24-31
    //         [9] lon bits 0-7
    //         [10] lon bits 8-15
    //         [11] lon bits 16-23
    //         [12] lon bits 24-31
    //         [13] elev bits 0-7
    //         [14] elev bits 8-15
    //         [15] elev bits 16-23
    //         [16] elev bits 24-31
    //         [17] i'm accurate flag

    //         [18] heading bits 0-7
    //         [19] heading bits 8-15
    //         [20] heading bits 16-23
    //         [21] heading bits 24-31
    //         [22] speed bits 0-7
    //         [23] speed bits 8-15
    //         [24] speed bits 16-23
    //         [25] speed bits 24-31


    public final static int BBGPSPACKETSIZE = 18;

    private void broadcastGPSpacket(int lat, int lon, int elev, int iMAccurate,
                                    int heading, int speed) {

        int batt = mBBService.getBatteryLevel();

        // Check GPS data is not stale
        int len = 2 * 4 + 1 + kMagicNumberLen + 1;
        ByteArrayOutputStream radioPacket = new ByteArrayOutputStream();

        for (int i = 0; i < kMagicNumberLen; i++) {
            radioPacket.write(kGPSMagicNumber[i]);
        }

        radioPacket.write(mBoardAddress & 0xFF);
        radioPacket.write((mBoardAddress >> 8) & 0xFF);
        radioPacket.write(krepeatedBy);
        radioPacket.write(lat & 0xFF);
        radioPacket.write((lat >> 8) & 0xFF);
        radioPacket.write((lat >> 16) & 0xFF);
        radioPacket.write((lat >> 24) & 0xFF);
        radioPacket.write(lon & 0xFF);
        radioPacket.write((lon >> 8) & 0xFF);
        radioPacket.write((lon >> 16) & 0xFF);
        radioPacket.write((lon >> 24) & 0xFF);
        radioPacket.write(batt & 0xFF);
        radioPacket.write((0) & 0xFF); // spare
        radioPacket.write((0) & 0xFF); // spare
        radioPacket.write((0) & 0xFF); // spare

        radioPacket.write(iMAccurate);
        radioPacket.write(0);

        d("Sending packet...");
        mRadio.broadcast(radioPacket.toByteArray());
        mLastSend = System.currentTimeMillis();
        d("Sent packet...");
        updateBoardLocations(mBoardAddress, 999,
                lat / 1000000.0, lon / 1000000.0, radioPacket.toByteArray());
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
        sendLogMsg(s);
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

        try {
            ByteArrayInputStream bytes = new ByteArrayInputStream(packet);

            int recvMagicNumber = magicNumberToInt(
                    new int[] { bytes.read(), bytes.read()});

            if (recvMagicNumber == magicNumberToInt(kGPSMagicNumber)) {
                d("BB GPS Packet");
                mTheirAddress = (int) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8));
                int repeatedBy = bytes.read();
                mTheirLat = (double) ((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                mTheirLon = (double)((bytes.read() & 0xff) +
                        ((bytes.read() & 0xff) << 8) +
                        ((bytes.read() & 0xff) << 16) +
                        ((bytes.read() & 0xff) << 24)) / 1000000.0;
                mTheirBatt = (int)(bytes.read() & 0xff);
                bytes.read(); // spare
                bytes.read(); // spare
                bytes.read(); // spare
                mThereAccurate = bytes.read();
                mLastRecv = System.currentTimeMillis();
                mLastHeardLocation = packet.clone();
                l(mRFAddress.boardAddressToName(mTheirAddress) +
                        " strength " + sigStrength +
                        "theirLat = " + mTheirLat + ", " +
                        "theirLon = " + mTheirLon +
                        "theirBatt = " + mTheirBatt);
                mIotClient.sendUpdate("bbevent", "[" +
                        mRFAddress.boardAddressToName(mTheirAddress) + "," +
                        sigStrength + "," + mTheirLat + "," + mTheirLon + "]");
                updateBoardLocations(mTheirAddress, sigStrength, mTheirLat, mTheirLon, packet.clone());
                return true;
            } else if (recvMagicNumber == magicNumberToInt(kTrackerMagicNumber)) {
                d("tracker packet");
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
            } else {
                d("rogue packet not for us!");
            }
            return false;
        }
        catch(Exception e){
            l("Error processing a received packet " + e.getMessage());
            return false;
        }


    }

    private static final int magicNumberToInt(int[] magic) {
        int magicNumber = 0;
        for (int i = 0; i < kMagicNumberLen; i++) {
            magicNumber = magicNumber + (magic[i] << ((kMagicNumberLen - 1 - i) * 8));
        }
        return (magicNumber);
    }

    // keep a historical list of minimal location data
    public class locationHistory{
        public long d; // dateTime
        public double a; // latitude
        public double o; // longitude
    }

    // Keep a list of board GPS locations
    public class boardLocation {
        public int address;
        public int sigStrength;
        public long lastHeard;
        public double latitude;
        public double longitude;
        public long lastHeardDate;
        public String board;
    }
    private HashMap<Integer, boardLocation> mBoardLocations = new HashMap<>();

    private class boardLocationHistory {
        public String board;
        public int address;
        List<locationHistory> locations = new ArrayList<>();
        public void AddLocationHistory(long lastHeardDate, double latitude, double longitude){
            int MAX_AGE = 15;
            int MINUTE_INTERVAL = 1;

            try{
                // we only want to store one location every minute and drop everything older than 15 minutes
                // not using a hash because it dorks the json.
                long minute =  lastHeardDate/(1000*60*MINUTE_INTERVAL);

                boolean found = false;
                for(locationHistory l: locations) {
                    long lMinutes = l.d/(1000*60*MINUTE_INTERVAL);
                    if(minute==lMinutes) {
                        l.d = lastHeardDate;
                        l.a = latitude;
                        l.o = longitude;
                        found = true;
                    }
                }
                if(!found){
                    locationHistory lh = new locationHistory();
                    lh.d = lastHeardDate;
                    lh.a = latitude;
                    lh.o = longitude;
                    locations.add(lh);
                }

                //remove locations older than 30 minutes.
                long maxAge = System.currentTimeMillis()-(MAX_AGE*1000*60);
                for(locationHistory l: locations) {
                    if (l.d < maxAge) {
                        locations.remove(l);
                    }
                }
            }
            catch(Exception e){
                l("Error Adding a Location History for " + address + " " + e.getMessage());
            }
        }
    }
    private HashMap<Integer, boardLocationHistory> mBoardLocationHistory = new HashMap<>();



    public void updateBoardLocations(int address, int sigstrength, double lat, double lon, byte[] locationPacket) {

        boardLocation loc = new boardLocation();

        try {
            loc.address = address;
            loc.lastHeard = SystemClock.elapsedRealtime();
            loc.sigStrength = sigstrength;
            loc.lastHeardDate = System.currentTimeMillis();
            loc.latitude = lat;
            loc.longitude = lon;
            mBoardLocations.put(address, loc); // add to current locations
        }
        catch(Exception e){
            l("Error storing the current location for " + address + " " + e.getMessage());
        }

        try{
            boardLocationHistory blh;

            if(mBoardLocationHistory.containsKey(address)) {
                blh = mBoardLocationHistory.get(address);
            }
            else {
                blh = new boardLocationHistory();
            }
            blh.address = loc.address;
            blh.AddLocationHistory(loc.lastHeardDate,lat,lon);
            mBoardLocationHistory.put(address,blh);

            // Update the JSON blob in the ContentProvider. Used in integration with BBMoblie for the Panel.
            ContentValues v = new ContentValues(1);
            v.put("0",getBoardLocationsJSON().toString());
            mContext.getContentResolver().update(Contract.CONTENT_URI, v, null, null);


            for (int addr: mBoardLocations.keySet()) {
                boardLocation l = mBoardLocations.get(addr);
                d("Location Entry:" + mRFAddress.boardAddressToName(addr) + ", age:" + (SystemClock.elapsedRealtime() - l.lastHeard));
            }
        }
        catch(Exception e){
            l("Error storing the board location history for " + address + " " + e.getMessage());
        }

    }

    private void addTestBoardLocations() {

     //   updateBoardLocations(51230, -53, 37.476222, -122.1551087, "testdata".getBytes());
    }

    public List<boardLocation> getBoardLocations() {
        List<boardLocation> list = new ArrayList<boardLocation>(mBoardLocations.values());
        return list;
    }

    // Create device list in JSON format for app use
    public JSONArray getBoardLocationsJSON() {
        JsonArray list = null;
        List<boardLocationHistory> locs = new ArrayList<>(mBoardLocationHistory.values());
        try {
            for (boardLocationHistory b : locs){
                b.board = mRFAddress.boardAddressToName(b.address);
            }
            //list = new JSONArray(valuesList);
            list = (JsonArray) new Gson().toJsonTree(locs, new TypeToken<List<boardLocationHistory>>() {}.getType());
        } catch (Exception e) {
            l("Error creating device list");
            return null;
        }

        JSONArray json = null;
        try {
            json = new JSONArray(list.toString());
        } catch (Exception e)  {
            l("Cannot convert devices to json: " + e.getMessage());
        }

        l("devJson1: " + list.toString());
        l("devJson2: " + json.toString());

        return (json);
    }

    public String getBoardColor(int address) {
        return mRFAddress.boardAddressToColor(address);
    }

}
