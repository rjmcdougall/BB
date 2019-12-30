package com.richardmcdougall.bb;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

public class BoardState {

    /* XXX TODO refactor out the string use cases and transform to constants -jib
    public static final String BB_TYPE_AZUL = "Burner Board Azul";
    public static final String BB_TYPE_CLASSIC = "Burner Board Classi";
    public static final String BB_TYPE_DIRECT_MAP = "Burner Board DirectMap";
    public static final String BB_TYPE_MAST = "Burner Board Mast";
    public static final String BB_TYPE_PANEL = "Burner Board Panel";
    /*

     */
    // Raspberry PIs have some subtle different behaviour. Use this Boolean to toggle
    public static final boolean kIsRPI = Build.MODEL.contains("rpi3");
    public static final boolean kIsNano = Build.MODEL.contains("NanoPC-T4");
    public static String BOARD_ID = "";
    public static String DEVICE_ID = "";
    public static final String publicNameFile = "publicName.txt";
    public boolean masterRemote = false;
    private static final String TAG = "BoardState";
    private BBService service = null;
    public boolean isGTFO = false;
    public boolean blockMaster = false;
    public int version = 0;
    public Date apkUpdatedDate;
    public int batteryLevel = -1;
    public int currentRadioChannel = 1;
    public int currentVideoMode = 1;

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
            state.put("c", service.wifi.SSID);
            state.put("p", service.wifi.password);

        } catch (Exception e) {
            e("Could not get state: " + e.getMessage());
        }
        return state;
    }

    public boolean setPublicName(String name) {
        try {
            FileWriter fw = new FileWriter(service.filesDir + "/" + publicNameFile);
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
            File f = new File(service.filesDir, publicNameFile);
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




}
