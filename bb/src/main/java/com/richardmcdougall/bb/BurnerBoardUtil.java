package com.richardmcdougall.bb;

import android.os.Build;
import java.io.*;
import java.util.ArrayList;


public class BurnerBoardUtil {
    /*
        Feature flag section here
    */

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
        String publicName = getPublicName();


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

    // Switch any of these to 'true' to force identification as that board type.
    // Only useful for debugging! -jib
    private static boolean kForceBBTypeAzul = false;
    private static boolean kForceBBTypeClassic = false;
    private static boolean kForceBBTypeDirectMap = false;
    private static boolean kForceBBTypeMast = false;
    private static boolean kForceBBTypePanel = false;

    public static final boolean isBBAzul() {
        return (kForceBBTypeAzul || BOARD_TYPE.contains("Azul")) ? true : false;
    }

    public static final boolean isBBClassic() {
        return (kForceBBTypeClassic || BOARD_TYPE.contains("Classic")) ? true : false;
    }

    public static final boolean isBBDirectMap() {
        return (kForceBBTypeDirectMap || BurnerBoardUtil.kIsRPI || BOARD_ID.contains("mickey")) ? true : false;
    }

    public static final boolean isBBMast() {
        return (kForceBBTypeMast || BOARD_TYPE.contains("Mast") || BOARD_ID.contains("test")) ? true : false;
    }

    /*  rjmcdougall says:
        Obviously we need a better persistent way of setting board types on android things.

        The embedded android 6 we deploy allows us to set the board type at provision time via USB.

        With android things we either need to allow config through the cloud or through the app.
    */
    public static final boolean isBBPanel() {
        return (kForceBBTypePanel
            || BOARD_TYPE.contains("Panel")
            || BOARD_ID.contains("Panel")
            || BOARD_ID.contains("cranky")
            //|| BOARD_ID.contains("grumpy")
            || BOARD_ID.contains("imx7d_pico")
        ) ? true : false;
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
