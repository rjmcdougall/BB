package com.richardmcdougall.bb;

import android.os.Build;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BoardState {

    public static String BOARD_ID = "";
    public static String DEVICE_ID = "";
    public static final String publicNameDir = "/data/data/com.richardmcdougall.bb/files";
    public static final String publicNameFile = "publicName.txt";
    public JSONArray dataBoards;

    BoardState() {
        String serial = Build.SERIAL;
        String publicName = "";

        if(DebugConfigs.OVERRIDE_PUBLIC_NAME != "")
            publicName =  DebugConfigs.OVERRIDE_PUBLIC_NAME;
        else
            publicName = getPublicName();

        if (BurnerBoardUtil.kIsRPI) {
            DEVICE_ID = "pi" + serial.substring(Math.max(serial.length() - 6, 0),
                    serial.length());
        } else if (BurnerBoardUtil.kIsNano) {
            DEVICE_ID = "npi" + serial.substring(Math.max(serial.length() - 5, 0),
                    serial.length());
        }
        else {
            DEVICE_ID = Build.MODEL;
        }

        BOARD_ID = (publicName == null || publicName.equals("")) ? DEVICE_ID : publicName;
    }

    public boolean setPublicName (String name) {
        try {
            FileWriter fw = new FileWriter(publicNameDir + "/" + publicNameFile);
            fw.write(name);
            fw.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    // Cargo culted from Download manager.
    public String getPublicName () {

        try {
            File f = new File(publicNameDir, publicNameFile);
            if (!f.exists())
                return null;
            InputStream is = null;
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
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
            e.printStackTrace();
            return null;
        }
    }
}
