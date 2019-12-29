package com.richardmcdougall.bb;

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
