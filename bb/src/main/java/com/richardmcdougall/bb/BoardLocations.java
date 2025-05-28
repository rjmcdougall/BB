package com.richardmcdougall.bb;

import android.content.ContentValues;
import android.os.SystemClock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.richardmcdougall.bb.rf.RFUtil;
import com.richardmcdougall.bbcommon.BLog;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BoardLocations {
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;
    private HashMap<String, boardLocation> mBoardLocations = new HashMap<>();
    private HashMap<String, boardLocationHistory> mBoardLocationHistory = new HashMap<>();

    BoardLocations(BBService service){
        this.service = service;
    }

    public void updateBoardLocations(String board, int sigstrength, double lat, double lon, int bat, byte[] locationPacket, boolean inCrisis) {

        boardLocation loc = new boardLocation();

        try {
            loc.board = board;
            //loc.address = address;
            loc.lastHeard = SystemClock.elapsedRealtime();
            loc.sigStrength = sigstrength;
            loc.lastHeardDate = System.currentTimeMillis();
            loc.latitude = lat;
            loc.longitude = lon;
            loc.inCrisis = inCrisis;
            mBoardLocations.put(board, loc); // add to current locations
        } catch (Exception e) {
            BLog.e(TAG, "Error storing the current location for " + board + " " + e.getMessage());
        }

        try {
            boardLocationHistory blh;

            if (mBoardLocationHistory.containsKey(board)) {
                blh = mBoardLocationHistory.get(board);
            } else {
                blh = new boardLocationHistory();
            }
            //blh.address = loc.address;
            blh.board = board;
            blh.bat = bat;
            blh.AddLocationHistory(loc.lastHeardDate, lat, lon);
            mBoardLocationHistory.put(board, blh);

            // Update the JSON blob in the ContentProvider. Used in integration with BBMoblie for the Panel.
            JSONArray ct = getBoardLocationsJSON(15);
            ContentValues v = new ContentValues(ct.length());
            for (int i = 0; i < ct.length(); i++) {
                v.put(String.valueOf(i), ct.getJSONObject(i).toString());
            }

            service.context.getContentResolver().update(Contract.CONTENT_URI, v, null, null);

            for (String thisboard : mBoardLocations.keySet()) {
                boardLocation l = mBoardLocations.get(thisboard);
                BLog.d(TAG, "Location Entry:" + thisboard + ", age:" + (SystemClock.elapsedRealtime() - l.lastHeard));
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error storing the board location history for " + board + " " + e.getMessage());
        }

    }

    public ArrayList<String> BoardsInCrisis(){
        ArrayList<String> i = new ArrayList<>();

        for(Map.Entry<String, boardLocation> thisboard : mBoardLocations.entrySet()) {
            String key = thisboard.getKey();
            boardLocation value = thisboard.getValue();

            if(value.inCrisis){
                i.add(value.board);
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

    public HashMap<String, boardLocationHistory> DeepCloneHashMap(HashMap<String, boardLocationHistory> blh) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(blh);

        java.lang.reflect.Type type = new TypeToken<HashMap<String, boardLocationHistory>>() {
        }.getType();
        HashMap<String, boardLocationHistory> clonedMap = gson.fromJson(jsonString, type);
        return clonedMap;

    }

    // Create device list in JSON format for app use
    public JSONArray getBoardLocationsJSON(int age) {
        JsonArray list = null;
        HashMap<String, boardLocationHistory> blh = DeepCloneHashMap(mBoardLocationHistory);

        List<boardLocationHistory> locs = new ArrayList<>(blh.values());
        try {
            for (boardLocationHistory thisboard : locs) {
                //b.board = service.allBoards.boardAddressToName(b.address);

                Iterator<locationHistory> iter = thisboard.locations.iterator();
                while (iter.hasNext()) {
                    locationHistory location = iter.next();
                    long datetime_this = location.datetime;
                    long min = System.currentTimeMillis() - (age * 60 * 1000);
                    if (datetime_this < min)
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
        public long datetime; // dateTime
        public double latitude; // latitude
        public double longitude; // longitude
    }

    // Keep a list of board GPS locations
    public class boardLocation {
        public String board;
        //public int address;
        public int sigStrength;
        public long lastHeard;
        public double latitude;
        public double longitude;
        public long lastHeardDate;
        public boolean inCrisis;
    }

    private class boardLocationHistory {
        public String board;
        //public int address;
        public int bat;

        List<locationHistory> locations = new ArrayList<>();

        public void AddLocationHistory(long lastHeardDate, double latitude, double longitude) {

            try {
                // we only want to store one location every minute and drop everything older than 15 minutes
                // not using a hash because it dorks the json.
                long minute = lastHeardDate / (1000 * 60 * RFUtil.LOCATION_INTERVAL_MINUTES);

                boolean found = false;
                for (locationHistory l : locations) {
                    long lMinutes = l.datetime / (1000 * 60 * RFUtil.LOCATION_INTERVAL_MINUTES);
                    if (minute == lMinutes) {
                        l.datetime = lastHeardDate;
                        l.latitude = latitude;
                        l.longitude = longitude;
                        found = true;
                    }
                }
                if (!found) {
                    locationHistory lh = new locationHistory();
                    lh.datetime = lastHeardDate;
                    lh.latitude = latitude;
                    lh.longitude = longitude;
                    locations.add(lh);
                }

                //remove locations older than 30 minutes.
                long maxAge = System.currentTimeMillis() - (RFUtil.MAX_LOCATION_STORAGE_MINUTES * 1000 * 60);

                Iterator<locationHistory> iter = locations.iterator();
                while (iter.hasNext()) {
                    if (iter.next().datetime < maxAge)
                        iter.remove();
                }

            } catch (Exception e) {
                BLog.e(TAG, "Error Adding a Location History for " + board + " " + e.getMessage());
            }
        }
    }

}
