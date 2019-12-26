package com.richardmcdougall.bb;

import android.os.Build;
import java.io.*;


public class BurnerBoardUtil {
    /*
        Feature flag section here
    */

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

    // This enabled GPS Time being polled
    public static final boolean fEnableGpsTime = false;

    /* Step one to make WiFi configurable is to pull out the strings */
    public static final String WIFI_SSID = "burnerboard";
    public static final String WIFI_PASS = "firetruck";

    // Known board types we have
    public static final String BOARD_TYPE = Build.MANUFACTURER;

    // String by which to identify the Satechi remotes; it's their mac address, which always
    // starts with DC:
    public static final String MEDIA_CONTROLLER_MAC_ADDRESS_PREFIX = "DC:";

    // radio packet codes
    public static final int kRemoteAudioTrack = 0x01;
    public static final int kRemoteVideoTrack = 0x02;
    public static final int kRemoteMute = 0x03;
    public static final int kRemoteMasterName = 0x04;

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

    // DEVICE_ID is whatever the device identifies as; BOARD_ID is the 'pretty name' we may have given it
    // BOARD_ID generally gets used as the name everywhere.
    // To set a custom name, set the data store 'boards' "bootname" value to the device ID, and the "name"
    // to the public name you want. That's it. Takes a reboot cycle to take effect. --jib
    public static final String BOARD_ID;
    public static final String DEVICE_ID;
    static {
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

    /* DIRECT MAP SETTINGS */
    public static final int kVisualizationDirectMapDefaultWidth = 8;
    public static final int kVisualizationDirectMapDefaultHeight = 256;

    // JosPacks have 1x166 strands of LEDs. Currently RPI == JosPack
    public static final int kVisualizationDirectMapWidth = BurnerBoardUtil.kIsRPI ? 1 : kVisualizationDirectMapDefaultWidth;
    public static final int kVisualizationDirectMapHeight = BurnerBoardUtil.kIsRPI ? 166 : kVisualizationDirectMapDefaultHeight;

    /* JosPacks have more of a power constraint, so we don't want to set it to full brightness. Empirically tested
        with with a rapidly refreshing pattern (BlueGold):
        100 -> 1.90a draw
        50  -> 0.50a draw
        25  -> 0.35a draw
    */
    public static final int kVisualizationDirectMapPowerMultiplier = BurnerBoardUtil.kIsRPI ? 25 : 100; // should be ok for nano

    /*

    THIS SETS UP PRETTY / HUMAN NAMES FOR ANY DEVICES

     */
    public static final String publicNameFile = "publicName.txt";
    public static final String wifiJSON = "wifi.json";
    public static final String favoritesJSON = "favorites.json";

    /*
        XXX this doesn't work as a static method:
        error: non-static method getApplicationContext() cannot be referenced from a static context

        public static final String publicNameDir = Service.getApplicationContext().getFilesDir().getAbsolutePath();

        Hardcoding for now, which sucks :( Suggestions welcome -jib
     */
    public static final String publicNameDir = "/data/data/com.richardmcdougall.bb/files";


    public static final boolean setPublicName (String name) {
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
    public static final String getPublicName () {

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
