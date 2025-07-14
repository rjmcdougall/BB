package com.richardmcdougall.bb;

import android.content.ContentValues;
import android.content.Intent;
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

    private int nextAddr = 200;
    private HashMap<String, Integer> mFakeAddresses = new HashMap<>();
    BoardLocations(BBService service){
        this.service = service;
    }

    public void updateBoardLocations(String board, int address, int sigstrength, double lat, double lon, int bat, byte[] locationPacket, boolean inCrisis) {

        // Manufacture a fake address for Meshtastic locations to keep BBMobile app happy
        if (address == 0) {
            address = mFakeAddresses.getOrDefault(board, nextAddr++);
            mFakeAddresses.put(board, nextAddr);
        }

        BLog.d(TAG, "updateBoardLocations: " + board + "," + sigstrength + "," + lat + "," + lon + "," + bat);
        boardLocation loc = new boardLocation();

        try {
            loc.board = board;
            loc.address = address;
            //loc.lastHeard = SystemClock.elapsedRealtime();
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
            blh.a = address;
            blh.board = board;
            blh.b = bat;
            blh.AddLocationHistory(loc.lastHeardDate, lat, lon);
            mBoardLocationHistory.put(board, blh);

            // Update the JSON blob in the ContentProvider. Used in integration with BBMoblie for the Panel.
            // Grab 30 minutes of history for Monitor
            JSONArray ct = getBoardLocationsJSON(30);
            ContentValues v = new ContentValues(ct.length());
            for (int i = 0; i < ct.length(); i++) {
                v.put(String.valueOf(i), ct.getJSONObject(i).toString());
            }
            service.context.getContentResolver().update(Contract.CONTENT_URI, v, null, null);

            for (String thisboard : mBoardLocations.keySet()) {
                boardLocation l = mBoardLocations.get(thisboard);
                //BLog.d(TAG, "Location Entry:" + thisboard + ", age:" + (System.currentTimeMillis() - l.lastHeardDate) / 1000 + " seconds");
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

    // Check boardname for non-ASCII characters
    public static boolean containsNonStandardAsciiChars(String inputText) {
        if (inputText == null || inputText.isEmpty()) {
            return false;
        }

        for (int i = 0; i < inputText.length(); i++) {
            char c = inputText.charAt(i);
            if (c > 127) {
                return true;
            }
        }
        return false;
    }

    // Create device list in JSON format for app use
    public JSONArray getBoardLocationsJSON(int age) {

        JsonArray list = null;
        HashMap<String, boardLocationHistory> blh = DeepCloneHashMap(mBoardLocationHistory);

        // Force the BBMobile app to gather at least 10 min of history
        if (age < 10) {
                age = 10;
        }

        List<boardLocationHistory> locs = new ArrayList<>(blh.values());
        try {
            Iterator<boardLocationHistory> boardIter = locs.iterator(); // Use an explicit iterator
            while (boardIter.hasNext()) {
                boardLocationHistory thisboard = boardIter.next();

                if (containsNonStandardAsciiChars(thisboard.board)) {
                    thisboard.board = String.valueOf(thisboard.a);
                }

                Iterator<locationHistory> iter = thisboard.locations.iterator();
                while (iter.hasNext()) {
                    locationHistory location = iter.next();
                    long datetime_this = location.d;
                    long min = System.currentTimeMillis() - (age * 60 * 1000);
                    if (datetime_this < min) {
                        iter.remove();
                    }
                }
                // If there are no valid locations for this board, remove it from JSON
                // Keeps the JSON smaller, and also the BBMobile app thinks any blank
                // location list means there are no more boards in the list...
                if (thisboard.locations.isEmpty()) {
                    boardIter.remove();
                }
            }
            //list = new JSONArray(valuesList);
            list = (JsonArray) new Gson().toJsonTree(locs, new TypeToken<List<boardLocationHistory>>() {
            }.getType());

        } catch (Exception e) {
            BLog.e(TAG, "Error building board location json: " + e.getMessage());
            return null;
        }

        JSONArray json = null;
        try {
            json = new JSONArray(list.toString());
        } catch (Exception e) {
            BLog.e(TAG, "Cannot convert locations to json: " + e.getMessage());
        }

        //BLog.d(TAG, "location JSON max age " + age + " : " + json.toString());

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
        public String board;
        public int address;
        public int sigStrength;
        //public long lastHeard;
        public double latitude;
        public double longitude;
        public long lastHeardDate;
        public boolean inCrisis;
    }

    private class boardLocationHistory {
        public String board;
        public int a;
        public int b;

        List<locationHistory> locations = new ArrayList<>();

        public void AddLocationHistory(long lastHeardDate, double latitude, double longitude) {

            try {
                // we only want to store one location n minutes and drop everything older than 30 minutes
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
                BLog.e(TAG, "Error Adding a Location History for " + board + " " + e.getMessage());
            }
        }
    }

}
