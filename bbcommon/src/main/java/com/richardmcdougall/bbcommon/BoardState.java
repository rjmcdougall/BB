package com.richardmcdougall.bbcommon;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.richardmcdougall.bbcommon.BLog;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    public boolean isGTFO = false;
    public boolean blockMaster = false;
    public int version = 0;
    public Date apkUpdatedDate;
    public int batteryLevel = -1;
    public int currentRadioChannel = 0;
    public int currentVideoMode = 0;
    public String SSID = "";
    public String password = "";
    public TeensyType displayTeensy;
    public BoardType boardType;
    public String serial = Build.SERIAL;
    public PlatformType platformType = PlatformType.dragonboard;
    public boolean inCrisis = false;
    private Context context = null;
    private String filesDir = "";
    private AllBoards allBoards = null;
    public int targetAPKVersion = 0;

    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    public BoardState(Context context, AllBoards allBoards) {
        this.context = context;
        filesDir = context.getFilesDir().getAbsolutePath();
        this.allBoards = allBoards;
        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pinfo.versionCode;
            apkUpdatedDate = new Date(pinfo.lastUpdateTime);
        } catch (PackageManager.NameNotFoundException e) {
            BLog.e(TAG, e.getMessage());
        }

        if (Build.MODEL.contains("rpi3"))
            platformType = PlatformType.rpi;
        else if (Build.MODEL.contains("NanoPC-T4"))
            platformType = PlatformType.npi;
        // else dragonboard

        if (platformType == PlatformType.rpi) {
            DEVICE_ID = "pi" + serial.substring(Math.max(serial.length() - 6, 0),
                    serial.length());
        } else if (platformType == PlatformType.npi) {
            DEVICE_ID = "npi" + serial.substring(Math.max(serial.length() - 5, 0),
                    serial.length());
        } else { // dragonboard
            DEVICE_ID = Build.MODEL;
        }

        if (DebugConfigs.OVERRIDE_PUBLIC_NAME != "")
            BOARD_ID = DebugConfigs.OVERRIDE_PUBLIC_NAME;
        else {
            BOARD_ID = this.allBoards.getBOARD_ID(DEVICE_ID);
            if (BOARD_ID == "")
                BOARD_ID = DEVICE_ID;
        }

        UpdateFromAllBoards();

        // look for an SSID and password in file system. If it is not there default to firetruck.
        getSSIDAndPassword();
        if (SSID == "") {
            setSSISAndPassword(WIFI_SSID, WIFI_PASS);
            getSSIDAndPassword();
        }

        // check every minute to see if AllBoards updates happened.
        Runnable checkForUpdates = () -> UpdateFromAllBoards();
        sch.scheduleWithFixedDelay(checkForUpdates, 10, 60, TimeUnit.SECONDS);

    }

    private void UpdateFromAllBoards(){
        address = this.allBoards.getBoardAddress(BOARD_ID);
        displayTeensy = this.allBoards.getDisplayTeensy(BOARD_ID);
        boardType = this.allBoards.getBoardType(BOARD_ID);
        targetAPKVersion = this.allBoards.targetAPKVersion(BOARD_ID);
    }

    public boolean setSSISAndPassword(String SSID, String password) {

        try {
            JSONObject wifiSettings = new JSONObject();
            wifiSettings.put("SSID", SSID);
            wifiSettings.put("password", password);

            setSSISAndPassword(wifiSettings);

        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean setSSISAndPassword(JSONObject wifiSettings) {

        try {
            FileWriter fw = new FileWriter(filesDir + "/" + WIFI_JSON);
            fw.write(wifiSettings.toString());
            fw.close();
            SSID = wifiSettings.getString("SSID");
            password = wifiSettings.getString("password");
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return false;
        } catch (IOException e) {
            BLog.e(TAG, e.getMessage());
            return false;
        }

        return true;
    }

    public void getSSIDAndPassword() {
        try {
            ArrayList<String> r = new ArrayList();

            File f = new File(filesDir + "/" + WIFI_JSON);
            InputStream is = null;
            try {
                is = new FileInputStream(f);

                BufferedReader buf = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder(buf.readLine());
                BLog.d(TAG, "contents of wifi.json: " + sb.toString());
                JSONObject j = new JSONObject(sb.toString());

                SSID = j.getString("SSID");
                password = j.getString("password");
            } catch (FileNotFoundException e) {
                BLog.e(TAG, e.getMessage());
            }

        } catch (Throwable e) {
            BLog.e(TAG, e.getMessage());
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

    public enum PlatformType {
        rpi("rpi"),
        npi("npi"),
        dragonboard("dragonboard");

        private String stringValue;

        PlatformType(final String toString) {
            stringValue = toString;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }
}
