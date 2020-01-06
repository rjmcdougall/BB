package com.richardmcdougall.bb;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;



public class BoardState {
    private String TAG = this.getClass().getSimpleName();

    private static final String WIFI_JSON = "wifi.json";
    private static final String WIFI_SSID = "burnerboard";
    private static final String WIFI_PASS = "firetruck";

    // Raspberry PIs have some subtle different behaviour. Use this Boolean to toggle
    public static final boolean kIsRPI = Build.MODEL.contains("rpi3");
    public static final boolean kIsNano = Build.MODEL.contains("NanoPC-T4");
    public String BOARD_ID = "";
    public String DEVICE_ID = "";
    public int address = -1;

    public boolean masterRemote = false;

    private BBService service = null;
    public boolean isGTFO = false;
    public boolean blockMaster = false;
    public int version = 0;
    public Date apkUpdatedDate;
    public int batteryLevel = -1;
    public int currentRadioChannel = 1;
    public int currentVideoMode = 1;
    public String SSID = "";
    public String password = "";
    public TeensyType displayTeensy = BoardState.TeensyType.teensy3;
    public BoardType boardType = null;
    public String serial = Build.SERIAL;

    BoardState(BBService service) {
        this.service = service;

        try {
            PackageInfo pinfo = service.context.getPackageManager().getPackageInfo(service.context.getPackageName(), 0);
            version = pinfo.versionCode;
            apkUpdatedDate = new Date(pinfo.lastUpdateTime);
        } catch (PackageManager.NameNotFoundException e) {
            BLog.e(TAG,e.getMessage());
        }

        if (kIsRPI) {
            DEVICE_ID = "pi" + serial.substring(Math.max(serial.length() - 6, 0),
                    serial.length());
        } else if (kIsNano) {
            DEVICE_ID = "npi" + serial.substring(Math.max(serial.length() - 5, 0),
                    serial.length());
        } else {
            DEVICE_ID = Build.MODEL;
        }

        if (DebugConfigs.OVERRIDE_PUBLIC_NAME != "")
            BOARD_ID = DebugConfigs.OVERRIDE_PUBLIC_NAME;
        else {
            BOARD_ID = service.allBoards.getBOARD_ID(DEVICE_ID);
            if(BOARD_ID=="")
                BOARD_ID = DEVICE_ID;
        }

        address = service.allBoards.getBoardAddress(BOARD_ID);
        displayTeensy = service.allBoards.getDisplayTeensy(BOARD_ID);
        boardType = service.allBoards.getBoardType(BOARD_ID);

        // look for an SSID and password in file system. If it is not there default to firetruck.
        getSSIDAndPassword();
        if (SSID == "") {
            setSSISAndPassword(WIFI_SSID, WIFI_PASS);
            getSSIDAndPassword();
        }

    }

    public JSONObject MinimizedState() {
        JSONObject state = new JSONObject();
        try {
            state.put("acn", currentRadioChannel - 1);
            state.put("vcn", currentVideoMode - 1);
            state.put("v", service.musicPlayer.getBoardVolumePercent());
            state.put("b", batteryLevel);
            state.put("am", masterRemote);
            state.put("apkd", apkUpdatedDate.toString());
            state.put("apkv", version);
            state.put("ip", service.wifi.ipAddress);
            state.put("g", isGTFO);
            state.put("bm", blockMaster);
            state.put("s", service.wifi.getConnectedSSID());
            state.put("c", service.boardState.SSID);
            state.put("p", service.boardState.password);

        } catch (Exception e) {
            BLog.e(TAG,"Could not get state: " + e.getMessage());
        }
        return state;
    }

    public boolean setSSISAndPassword(String SSID, String password) {

        try {
            JSONObject wifiSettings = new JSONObject();
            wifiSettings.put("SSID", SSID);
            wifiSettings.put("password", password);

            setSSISAndPassword(wifiSettings);

        } catch (JSONException e) {
            BLog.e(TAG,e.getMessage());
            return false;
        }
        return true;
    }

    public boolean setSSISAndPassword(JSONObject wifiSettings) {

        try {
            FileWriter fw = new FileWriter(service.filesDir + "/" + WIFI_JSON);
            fw.write(wifiSettings.toString());
            fw.close();
            SSID = wifiSettings.getString("SSID");
            password = wifiSettings.getString("password");
        } catch (JSONException e) {
            BLog.e(TAG,e.getMessage());
            return false;
        } catch (IOException e) {
            BLog.e(TAG,e.getMessage());
            return false;
        }

        return true;
    }

    public void getSSIDAndPassword() {
        try {
            ArrayList<String> r = new ArrayList();

            File f = new File(service.filesDir + "/" + WIFI_JSON);
            InputStream is = null;
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                BLog.e(TAG,e.getMessage());
            }
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder(buf.readLine());
            BLog.d(TAG,"contents of wifi.json: " + sb.toString());
            JSONObject j = new JSONObject(sb.toString());

            SSID = j.getString("SSID");
            password = j.getString("password");

        } catch (Throwable e) {
            BLog.e(TAG,e.getMessage());
        }
    }

    public enum TeensyType {
        teensy3("teensy3"),
        teensy4("teensy4");

        private String stringValue;

        TeensyType(final String toString) {
            stringValue = toString;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    public enum BoardType {
        azul("azul"),
        panel("panel"),
        mast("mast"),
        classic("classic"),
        boombox("boombox"),
        backpack("backpack"),
        unknown("unknown");

        private String stringValue;

        BoardType(final String toString) {
            stringValue = toString;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }
}
