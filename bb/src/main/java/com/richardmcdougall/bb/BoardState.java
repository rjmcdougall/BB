package com.richardmcdougall.bb;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

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

    private static final String WIFI_JSON = "wifi.json";
    private static final String WIFI_SSID = "burnerboard";
    private static final String WIFI_PASS = "firetruck";
    private static final String TAG = "BoardState";
    private static final String PUBLIC_NAME_FILE = "publicName.txt";

    // Raspberry PIs have some subtle different behaviour. Use this Boolean to toggle
    public static final boolean kIsRPI = Build.MODEL.contains("rpi3");
    public static final boolean kIsNano = Build.MODEL.contains("NanoPC-T4");
    public static String BOARD_ID = "";
    public static String DEVICE_ID = "";

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

    private void d(String logMsg) {
        if (DebugConfigs.DEBUG_BOARD_STATE) {
            Log.d(TAG, logMsg);
        }
    }

    private void e(String logMsg) {
        Log.e(TAG, logMsg);
    }

    BoardState(BBService service) {
        this.service = service;

        try{
            PackageInfo pinfo = service.context.getPackageManager().getPackageInfo(service.context.getPackageName(), 0);
            version = pinfo.versionCode;
            apkUpdatedDate = new Date(pinfo.lastUpdateTime);
        }
        catch(PackageManager.NameNotFoundException e){
            e(e.getMessage());
        }

        String serial = Build.SERIAL;
        String publicName = "";

        if (DebugConfigs.OVERRIDE_PUBLIC_NAME != "")
            publicName = DebugConfigs.OVERRIDE_PUBLIC_NAME;
        else
            publicName = getPublicName();

        // look for an SSID and password in file system. If it is not there default to firetruck.
        getSSIDAndPassword();
        if (SSID == "") {
            setSSISAndPassword(WIFI_SSID, WIFI_PASS);
            getSSIDAndPassword();
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

        BOARD_ID = (publicName == null || publicName.equals("")) ? DEVICE_ID : publicName;
    }

    public JSONObject MinimizedState() {
        JSONObject state = new JSONObject();
        try {
            state.put("acn",currentRadioChannel - 1);
            state.put("vcn",currentVideoMode - 1);
            state.put("v", service.musicPlayer.getBoardVolumePercent());
            state.put("b", batteryLevel);
            state.put("am", masterRemote);
            state.put("apkd", apkUpdatedDate.toString());
            state.put("apkv", version);
            state.put("ip", service.wifi.ipAddress);
            state.put("g", isGTFO);
            state.put("bm" , blockMaster);
            state.put("s", service.wifi.getConnectedSSID());
            state.put("c", service.boardState.SSID);
            state.put("p", service.boardState.password);

        } catch (Exception e) {
            e("Could not get state: " + e.getMessage());
        }
        return state;
    }

    public boolean setPublicName(String name) {
        try {
            FileWriter fw = new FileWriter(service.filesDir + "/" + PUBLIC_NAME_FILE);
            fw.write(name);
            fw.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    // Cargo culted from Download manager.
    public String getPublicName() {

        try {
            File f = new File(service.filesDir, PUBLIC_NAME_FILE);
            if (!f.exists())
                return null;
            InputStream is = null;
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e(e.getMessage());
            }
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();

            // We only expect one line - this is a shortcut! -jib
            while (line != null) {
                sb.append(line);
                line = buf.readLine();
            }

            return sb.toString();

        } catch (Throwable e) {
            e(e.getMessage());
            return null;
        }
    }

    public boolean setSSISAndPassword(String SSID, String password) {

        try {
            JSONObject wifiSettings = new JSONObject();
            wifiSettings.put("SSID", SSID);
            wifiSettings.put("password", password);

            setSSISAndPassword(wifiSettings);

        } catch (JSONException e) {
            e(e.getMessage());
            return false;
        }
        return true;
    }

    public boolean setSSISAndPassword(JSONObject wifiSettings) {

        try {
            FileWriter fw = new FileWriter(service.filesDir + "/" + WIFI_JSON);
            fw.write(wifiSettings.toString());
            fw.close();
            service.boardState.SSID = wifiSettings.getString("SSID");
            service.boardState.password = wifiSettings.getString("password");
        } catch (JSONException e) {
            e(e.getMessage());
            return false;
        } catch (IOException e) {
            e(e.getMessage());
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
                e(e.getMessage());
            }
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder(buf.readLine());
            d("contents of wifi.json: " + sb.toString());
            JSONObject j = new JSONObject(sb.toString());

            service.boardState.SSID = j.getString("SSID");
            service.boardState.password = j.getString("password");

        } catch (Throwable e) {
            e(e.getMessage());
        }
    }
}
