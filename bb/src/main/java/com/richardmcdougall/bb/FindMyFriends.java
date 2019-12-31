package com.richardmcdougall.bb;

import android.os.SystemClock;
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
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;

/**
 * Created by rmc on 2/7/18.
 */

public class FindMyFriends {

    private static final String TAG = "BB.FMF";
    private BBService service;
    private findMyFriendsCallback mFindMyFriendsCallback = null;
    private long mLastFix = 0;
    private static final int kMaxFixAge = 30000;
    private static final int krepeatedBy = 0;
    private int mLat;
    private int mLon;
    private int mAlt;
    private int mAmIAccurate;
    private int mTheirAddress;
    private double mTheirLat;
    private double mTheirLon;
    private int mTheirBatt;
    private int mThereAccurate;
    private long mLastSend = 0;
    private long mLastRecv = 0;
    private byte[] mLastHeardLocation;

    public FindMyFriends(BBService service) {
        this.service = service;
        l("Starting FindMyFriends");


        if (service.radio == null) {
            l("No Radio!");
            return;
        }

        service.radio.attach(new RF.radioEvents() {
            @Override
            public void receivePacket(byte[] bytes, int sigStrength) {
                l("FMF Packet: len(" + bytes.length + "), data: " + RFUtil.bytesToHex(bytes));
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

                if (sinceLastFix > kMaxFixAge) {
                    d("FMF: sending GPS update");
                    service.iotClient.sendUpdate("bbevent", "[" +
                            service.boardState.BOARD_ID + "," + 0 + "," + mLat  / 1000000.0 + "," + mLon  / 1000000.0 + "]");
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

        // Check GPS data is not stale
        int len = 2 * 4 + 1 + RFUtil.kMagicNumberLen + 1;
        ByteArrayOutputStream radioPacket = new ByteArrayOutputStream();

        for (int i = 0; i < RFUtil.kMagicNumberLen; i++) {
            radioPacket.write(RFUtil.kGPSMagicNumber[i]);
        }

        radioPacket.write(service.boardState.address & 0xFF);
        radioPacket.write((service.boardState.address >> 8) & 0xFF);
        radioPacket.write(krepeatedBy);
        radioPacket.write(lat & 0xFF);
        radioPacket.write((lat >> 8) & 0xFF);
        radioPacket.write((lat >> 16) & 0xFF);
        radioPacket.write((lat >> 24) & 0xFF);
        radioPacket.write(lon & 0xFF);
        radioPacket.write((lon >> 8) & 0xFF);
        radioPacket.write((lon >> 16) & 0xFF);
        radioPacket.write((lon >> 24) & 0xFF);
        radioPacket.write(service.boardState.batteryLevel & 0xFF);
        radioPacket.write((0) & 0xFF); // spare
        radioPacket.write((0) & 0xFF); // spare
        radioPacket.write((0) & 0xFF); // spare

        radioPacket.write(iMAccurate);
        radioPacket.write(0);

        d("Sending packet...");
        service.radio.broadcast(radioPacket.toByteArray());
        mLastSend = System.currentTimeMillis();
        d("Sent packet...");
        updateBoardLocations(service.boardState.address, 999,
                lat / 1000000.0, lon / 1000000.0, service.boardState.batteryLevel, radioPacket.toByteArray());
    }


    public interface findMyFriendsCallback {

        public void audioTrack(int track);
        public void videoTrack(int track);
        public void globalAlert(int alert);

    }

    public void attach(FindMyFriends.findMyFriendsCallback newfunction) {

        mFindMyFriendsCallback = newfunction;
    }

    public void l(String s) {
        if (DebugConfigs.DEBUG_FMF) {
            Log.v(TAG, s);
            service.sendLogMsg(s);
        }
    }

    public void d(String s) {
        if (DebugConfigs.DEBUG_FMF) {
            Log.v(TAG, s);
            service.sendLogMsg(s);
        }
    }

    boolean processReceive(byte [] packet, int sigStrength) {

        try {
            ByteArrayInputStream bytes = new ByteArrayInputStream(packet);

            int recvMagicNumber = RFUtil.magicNumberToInt(
                    new int[] { bytes.read(), bytes.read()});

            if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kGPSMagicNumber)) {
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
                l(service.allBoards.boardAddressToName(mTheirAddress) +
                        " strength " + sigStrength +
                        "theirLat = " + mTheirLat + ", " +
                        "theirLon = " + mTheirLon +
                        "theirBatt = " + mTheirBatt);
                service.iotClient.sendUpdate("bbevent", "[" +
                        service.allBoards.boardAddressToName(mTheirAddress) + "," +
                        sigStrength + "," + mTheirLat + "," + mTheirLon + "]");
                updateBoardLocations(mTheirAddress, sigStrength, mTheirLat, mTheirLon, mTheirBatt ,packet.clone());
                return true;
            } else if (recvMagicNumber == RFUtil.magicNumberToInt( RFUtil.kTrackerMagicNumber)) {
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
        public int a;
        public int b;

        List<locationHistory> locations = new ArrayList<>();

        public void AddLocationHistory(long lastHeardDate, double latitude, double longitude){

            try{
                // we only want to store one location every minute and drop everything older than 15 minutes
                // not using a hash because it dorks the json.
                long minute =  lastHeardDate/(1000*60 * RFUtil.LOCATION_INTERVAL_MINUTES);

                boolean found = false;
                for(locationHistory l: locations) {
                    long lMinutes = l.d/(1000*60 * RFUtil.LOCATION_INTERVAL_MINUTES);
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
                long maxAge = System.currentTimeMillis()-(RFUtil.MAX_LOCATION_STORAGE_MINUTES*1000*60);

                Iterator<locationHistory> iter = locations.iterator();
                while(iter.hasNext()){
                    if(iter.next().d < maxAge)
                        iter.remove();
                }

            }
            catch(Exception e){
                l("Error Adding a Location History for " + a + " " + e.getMessage());
            }
        }
    }
    private HashMap<Integer, boardLocationHistory> mBoardLocationHistory = new HashMap<>();



    public void updateBoardLocations(int address, int sigstrength, double lat, double lon, int bat, byte[] locationPacket) {

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
            blh.a = loc.address;
            blh.b = bat;
            blh.AddLocationHistory(loc.lastHeardDate,lat,lon);
            mBoardLocationHistory.put(address,blh);

            // Update the JSON blob in the ContentProvider. Used in integration with BBMoblie for the Panel.
            JSONArray ct = getBoardLocationsJSON(15);
            ContentValues v = new ContentValues(ct.length());
            for (int i = 0; i < ct.length(); i++) {
                v.put(String.valueOf(i),ct.getJSONObject(i).toString());
            }

            service.context.getContentResolver().update(Contract.CONTENT_URI, v, null, null);

            for (int addr: mBoardLocations.keySet()) {
                boardLocation l = mBoardLocations.get(addr);
                d("Location Entry:" + service.allBoards.boardAddressToName(addr) + ", age:" + (SystemClock.elapsedRealtime() - l.lastHeard));
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

    public HashMap<Integer, boardLocationHistory>  DeepCloneHashMap(HashMap<Integer, boardLocationHistory> blh){
        Gson gson = new Gson();
        String jsonString = gson.toJson(blh);

        java.lang.reflect.Type type = new TypeToken<HashMap<Integer, boardLocationHistory>>(){}.getType();
        HashMap<Integer, boardLocationHistory> clonedMap = gson.fromJson(jsonString, type);
        return clonedMap;

    }


    // Create device list in JSON format for app use
    public JSONArray getBoardLocationsJSON(int age) {
        JsonArray list = null;
        HashMap<Integer, boardLocationHistory> blh =  DeepCloneHashMap(mBoardLocationHistory);

        List<boardLocationHistory> locs = new ArrayList<>(blh.values());
        try {
            for (boardLocationHistory b : locs){
                b.board = service.allBoards.boardAddressToName(b.a);

                Iterator<locationHistory> iter = b.locations.iterator();
                while(iter.hasNext()){
                    locationHistory lll = iter.next();
                    long d = lll.d;
                    long min =  System.currentTimeMillis()-(age*60*1000);
                    if(d < min)
                        iter.remove();
                }
            }
            //list = new JSONArray(valuesList);
            list = (JsonArray) new Gson().toJsonTree(locs, new TypeToken<List<boardLocationHistory>>() {}.getType());


        } catch (Exception e) {
            l("Error building board location json");
            return null;
        }

        JSONArray json = null;
        try {
            json = new JSONArray(list.toString());
        } catch (Exception e)  {
            l("Cannot convert locations to json: " + e.getMessage());
        }

        l("location JSON max age " + age + " : " + json.toString());

        return (json);
    }

    public String getBoardColor(int address) {
        return service.allBoards.boardAddressToColor(address);
    }

}
