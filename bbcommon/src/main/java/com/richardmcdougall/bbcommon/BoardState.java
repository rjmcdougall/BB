package com.richardmcdougall.bbcommon;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
    private boolean funMode = false;
    private boolean blockAutoRotation = false;

    public String SSID = "";
    public String password = "";
    public TeensyType displayTeensy;
    private BoardType boardType;

    public static String serial = Build.SERIAL;
    public PlatformType platformType = PlatformType.npi;
    public boolean inCrisis = false;
    private Context context = null;
    private String filesDir = "";
    private AllBoards allBoards = null;
    public int targetAPKVersion = 0;
    public int videoContrastMultiplier = 1;
    public boolean rotatingDisplay = false;
    public String profile = "";
    public boolean displayDebug = false;
    private PersistentCache persistentCache = null;

    public int pixelSlow;

    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    public BoardType GetBoardType() {

        if (DebugConfigs.OVERRIDE_BOARD_TYPE != null)
            return DebugConfigs.OVERRIDE_BOARD_TYPE;
        else
            return boardType;

    }

    public void SetFunMode(boolean funMode){
        this.persistentCache.saveData("funMode",String.valueOf(funMode));
        this.funMode = funMode;
    }

    public void SetBlockAutoRotation(boolean blockAutoRotation){
        this.persistentCache.saveData("blockAutoRotation",String.valueOf(blockAutoRotation));
        this.blockAutoRotation = blockAutoRotation;
    }

    public boolean GetBlockAudtoRotation() {return this.blockAutoRotation; }

    public boolean GetFunMode(){
        return this.funMode;
    }

    public static PlatformType GetPlatformType() {
        if (DebugConfigs.OVERRIDE_PLATFORM_TYPE != PlatformType.none) {
            return DebugConfigs.OVERRIDE_PLATFORM_TYPE;
        } else {
            if (Build.MODEL.contains("NanoPC-T4"))
                return PlatformType.npi;
            else
                return PlatformType.dragonboard;
        }
    }


    public String GetDeviceID() {

        if (DebugConfigs.OVERRIDE_DEVICE_ID != "") {
            return DebugConfigs.OVERRIDE_DEVICE_ID;
        } else {
            if (GetPlatformType() == PlatformType.npi) {
                String androidId = GetRockChipSerial();
                return "n" + androidId.toUpperCase();
            } else { // dragonboard
                return Build.MODEL;
            }
        }
    }

    public static String GetBoardID(AllBoards allBoards, String deviceID) {
        String b = "";
        if (DebugConfigs.OVERRIDE_PUBLIC_NAME != "")
            b = DebugConfigs.OVERRIDE_PUBLIC_NAME;
        else {
            b = allBoards.getBOARD_ID(deviceID);
            if (b == "")
                b = deviceID;
        }
        return b;
    }

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

        platformType = GetPlatformType();
        DEVICE_ID = GetDeviceID();
        BOARD_ID = GetBoardID(this.allBoards, DEVICE_ID);

        UpdateFromAllBoards();

        // look for an SSID and password in file system. If it is not there default to firetruck.
        getSSIDAndPassword();
        if (SSID == "") {
            setSSISAndPassword(WIFI_SSID, WIFI_PASS);
            getSSIDAndPassword();
        }

        this.persistentCache = new PersistentCache(context);
        if(persistentCache.exists("funMode"))
            this.funMode = Boolean.parseBoolean(persistentCache.getData("funMode")) ;
        if(persistentCache.exists("blockAutoRotation"))
            this.blockAutoRotation = Boolean.parseBoolean(persistentCache.getData("blockAutoRotation")) ;

        // check every minute to see if AllBoards updates happened.
        Runnable checkForUpdates = () -> UpdateFromAllBoards();
        sch.scheduleWithFixedDelay(checkForUpdates, 10, 60, TimeUnit.SECONDS);

    }

    private void UpdateFromAllBoards() {
        BOARD_ID = GetBoardID(this.allBoards, DEVICE_ID);
        address = this.allBoards.getBoardAddress(BOARD_ID);
        displayTeensy = this.allBoards.getDisplayTeensy(BOARD_ID);
        boardType = this.allBoards.getBoardType(BOARD_ID);
        targetAPKVersion = this.allBoards.targetAPKVersion(BOARD_ID);
        videoContrastMultiplier = this.allBoards.videoContrastMultiplier(BOARD_ID);
        profile = this.allBoards.getProfile(BOARD_ID);
        displayDebug = this.allBoards.displayDebug(BOARD_ID);
        pixelSlow = this.allBoards.pixelSlow(BOARD_ID);


        BLog.i(TAG, "Updating Board State: " + BOARD_ID + " " + address + " " + displayTeensy + " " + boardType + " " + targetAPKVersion + " " + videoContrastMultiplier + " " + this.profile);
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
        } catch (Exception e) {
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
            is = new FileInputStream(f);

            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder(buf.readLine());
            BLog.d(TAG, "contents of wifi.json: " + sb.toString());
            JSONObject j = new JSONObject(sb.toString());

            SSID = j.getString("SSID");
            password = j.getString("password");
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

    public enum BatteryState {
        NORMAL,
        LOW,
        CRITICAL
    }
    public enum BoardType {
        littlewing("littlewing"),
        azul("azul"),
        panel("panel"),
        wspanel("wspanel"),
        mast("mast"),
        classic("classic"),
        boombox("boombox"),
        backpack("backpack"),
        mezcal("mezcal"),
        dynamicPanel("dynamicPanel"),
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

    private static String GetRockChipSerial() {

        String rockChipSerial = "";
        String result = "";

        try {
            String[] args = {"/system/bin/cat", "/proc/cpuinfo"};
            ProcessBuilder cmd = new ProcessBuilder(args);

            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[1024];
            while (in.read(re) != -1) {
                result += new String(re);
            }
            in.close();

            String results[] = result.split("\n");
            for (String line : results) {
                if (line.startsWith("Serial\t\t:")) {
                    rockChipSerial = line.replace("Serial\t\t:", "").trim();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            return rockChipSerial;
        }
    }

    public enum PlatformType {
        npi("npi"),
        dragonboard("dragonboard"),
        none("none");

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
