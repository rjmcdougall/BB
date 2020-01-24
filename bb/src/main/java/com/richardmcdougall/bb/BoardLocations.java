package com.richardmcdougall.bb;

import android.content.ContentValues;
import android.os.SystemClock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.richardmcdougall.bb.rf.RFUtil;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BoardLocations {
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;
    private HashMap<Integer, boardLocation> mBoardLocations = new HashMap<>();
    private HashMap<Integer, boardLocationHistory> mBoardLocationHistory = new HashMap<>();

    BoardLocations(BBService service){
        this.service = service;
    }

    public void updateBoardLocations(int address, int sigstrength, double lat, double lon, int bat, byte[] locationPacket, boolean inCrisis) {

        boardLocation loc = new boardLocation();

        try {
            loc.address = address;
            loc.lastHeard = SystemClock.elapsedRealtime();
            loc.sigStrength = sigstrength;
            loc.lastHeardDate = System.currentTimeMillis();
            loc.latitude = lat;
            loc.longitude = lon;
            loc.inCrisis = inCrisis;
            mBoardLocations.put(address, loc); // add to current locations
        } catch (Exception e) {
            BLog.e(TAG, "Error storing the current location for " + address + " " + e.getMessage());
        }

        try {
            boardLocationHistory blh;

            if (mBoardLocationHistory.containsKey(address)) {
                blh = mBoardLocationHistory.get(address);
            } else {
                blh = new boardLocationHistory();
            }
            blh.a = loc.address;
            blh.b = bat;
            blh.AddLocationHistory(loc.lastHeardDate, lat, lon);
            mBoardLocationHistory.put(address, blh);

            // Update the JSON blob in the ContentProvider. Used in integration with BBMoblie for the Panel.
            JSONArray ct = getBoardLocationsJSON(15);
            ContentValues v = new ContentValues(ct.length());
            for (int i = 0; i < ct.length(); i++) {
                v.put(String.valueOf(i), ct.getJSONObject(i).toString());
            }

            service.context.getContentResolver().update(Contract.CONTENT_URI, v, null, null);

            for (int addr : mBoardLocations.keySet()) {
                boardLocation l = mBoardLocations.get(addr);
                BLog.d(TAG, "Location Entry:" + service.allBoards.boardAddressToName(addr) + ", age:" + (SystemClock.elapsedRealtime() - l.lastHeard));
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error storing the board location history for " + address + " " + e.getMessage());
        }

    }

    public ArrayList<Integer> BoardsInCrisis(){
        ArrayList<Integer> i = new ArrayList<>();

        for(Map.Entry<Integer, boardLocation> b : mBoardLocations.entrySet()) {
            Integer key = b.getKey();
            boardLocation value = b.getValue();

            if(value.inCrisis){
                i.add(value.address);
            }
        }
        return i;
    }

    public void addTestBoardLocations() {

     //     updateBoardLocations(1, -53, 37.476222, -122.1551087,0, "testdata".getBytes(),true);
    }

    public List<boardLocation> getBoardLocations() {
        List<boardLocation> list = new ArrayList<boardLocation>(mBoardLocations.values());
        return list;
    }

    public HashMap<Integer, boardLocationHistory> DeepCloneHashMap(HashMap<Integer, boardLocationHistory> blh) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(blh);

        java.lang.reflect.Type type = new TypeToken<HashMap<Integer, boardLocationHistory>>() {
        }.getType();
        HashMap<Integer, boardLocationHistory> clonedMap = gson.fromJson(jsonString, type);
        return clonedMap;

    }

    // Create device list in JSON format for app use
    public JSONArray getBoardLocationsJSON(int age) {
        JsonArray list = null;
        HashMap<Integer, boardLocationHistory> blh = DeepCloneHashMap(mBoardLocationHistory);

        List<boardLocationHistory> locs = new ArrayList<>(blh.values());
        try {
            for (boardLocationHistory b : locs) {
                b.board = service.allBoards.boardAddressToName(b.a);

                Iterator<locationHistory> iter = b.locations.iterator();
                while (iter.hasNext()) {
                    locationHistory lll = iter.next();
                    long d = lll.d;
                    long min = System.currentTimeMillis() - (age * 60 * 1000);
                    if (d < min)
                        iter.remove();
                }
            }
            //list = new JSONArray(valuesList);
            list = (JsonArray) new Gson().toJsonTree(locs, new TypeToken<List<boardLocationHistory>>() {
            }.getType());

        } catch (Exception e) {
            BLog.e(TAG, "Error building board location json");
            return null;
        }

        JSONArray json = null;
        try {
            json = new JSONArray(list.toString());
        } catch (Exception e) {
            BLog.e(TAG, "Cannot convert locations to json: " + e.getMessage());
        }

        BLog.d(TAG, "location JSON max age " + age + " : " + json.toString());

        return (json);
    }

    // keep a historical list of minimal location data
    public class locationHistory {
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
        public boolean inCrisis;
    }

    private class boardLocationHistory {
        public String board;
        public int a;
        public int b;

        List<locationHistory> locations = new ArrayList<>();

        public void AddLocationHistory(long lastHeardDate, double latitude, double longitude) {

            try {
                // we only want to store one location every minute and drop everything older than 15 minutes
                // not using a hash because it dorks the json.
                long minute = lastHeardDate / (1000 * 60 * RFUtil.LOCATION_INTERVAL_MINUTES);

                boolean found = false;
                for (locationHistory l : locations) {
                    long lMinutes = l.d / (1000 * 60 * RFUtil.LOCATION_INTERVAL_MINUTES);
                    if (minute == lMinutes) {
                        l.d = lastHeardDate;
                        l.a = latitude;
                        l.o = longitude;
                        found = true;
                    }
                }
                if (!found) {
                    locationHistory lh = new locationHistory();
                    lh.d = lastHeardDate;
                    lh.a = latitude;
                    lh.o = longitude;
                    locations.add(lh);
                }

                //remove locations older than 30 minutes.
                long maxAge = System.currentTimeMillis() - (RFUtil.MAX_LOCATION_STORAGE_MINUTES * 1000 * 60);

                Iterator<locationHistory> iter = locations.iterator();
                while (iter.hasNext()) {
                    if (iter.next().d < maxAge)
                        iter.remove();
                }

            } catch (Exception e) {
                BLog.e(TAG, "Error Adding a Location History for " + a + " " + e.getMessage());
            }
        }
    }

}
