package com.richardmcdougall.bbinstaller;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.text.format.Formatter;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.R.attr.action;
import static android.R.attr.path;
import static android.R.attr.versionCode;
import static android.content.ContentValues.TAG;

public class Installer extends Service {

    public BBDownloadManager dlManager;
    IoTClient iotClient = null;
    int mVersion = 0;
    TextToSpeech voice;
    private Context mContext;
    WifiManager mWiFiManager = null;
    List<ApplicationInfo> packages = null;
    String TAG = "BBI.Installer";
    int mUpgradeToVersion = 0;
    boolean mDoReboot = false;
    private ConnectivityManager mConnMan;
    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    IntentFilter intentf = new IntentFilter();

    public Installer() {
    }

    public void l(String s) {
        Log.v(TAG, s);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static String getBoardId() {

        String id;
        String serial = Build.SERIAL;

        if (Build.MODEL.contains("rpi3")) {
            id = "pi" + serial.substring(Math.max(serial.length() - 6, 0),
                    serial.length());
        } else {
            id = Build.MODEL;
        }

        return id;
    }

    private boolean checkWifiSSid(String ssid) {

        String aWifi = "\"" + ssid + "\"";

        try {
            List<WifiConfiguration> wifiList = mWiFiManager.getConfiguredNetworks();
            if (wifiList != null) {
                for (WifiConfiguration config : wifiList) {
                    String newSSID = config.SSID;
                    l("Found wifi:" + newSSID + " == " + aWifi + " ?");
                    if (aWifi.equals(newSSID)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }


    private void connectWifi(String ssid) {

        String aWifi = "\"" + ssid + "\"";

        try {
            List<WifiConfiguration> wifiList = mWiFiManager.getConfiguredNetworks();
            if (wifiList != null) {
                for (WifiConfiguration config : wifiList) {
                    String newSSID = config.SSID;

                    l("Found wifi:" + newSSID + " == " + aWifi + " ?");

                    if (aWifi.equals(newSSID)) {
                        l("connecting wifi:" + newSSID);
                        mWiFiManager.disconnect();
                        mWiFiManager.enableNetwork(config.networkId, true);
                        mWiFiManager.reconnect();

                        return;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private void addWifi(String ssid, String pass) {
        try {

            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + ssid + "\"";
            conf.preSharedKey = "\"" + pass + "\"";
            mWiFiManager.addNetwork(conf);
            mWiFiManager.disconnect();
            mWiFiManager.enableNetwork(conf.networkId, false);
            mWiFiManager.reconnect();
        } catch (Exception e) {
        }
        return;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        l("Starting BB Installer");

        mContext = getApplicationContext();

        mWiFiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWiFiManager.setWifiEnabled(true);

        this.registerReceiver(new BroadcastReceiver() {
                                  @Override
                                  public void onReceive(Context context, Intent intent) {
                                      int extraWifiState =
                                              intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE ,
                                                      WifiManager.WIFI_STATE_UNKNOWN);

                                      switch(extraWifiState){
                                          case WifiManager.WIFI_STATE_DISABLED:
                                              l("WIFI STATE DISABLED");
                                              break;
                                          case WifiManager.WIFI_STATE_DISABLING:
                                              l("WIFI STATE DISABLING");
                                              break;
                                          case WifiManager.WIFI_STATE_ENABLED:
                                              l("WIFI STATE ENABLED");
                                              int mfs = mWiFiManager.getWifiState();
                                              l("Wifi state is " + mfs);
                                              l("Checking wifi");
                                              if (checkWifiSSid(new String("burnerboard")) == false) {
                                                  l("adding wifi");
                                                  addWifi("burnerboard", "firetruck");
                                              }
                                              l("Connecting to wifi");
                                              connectWifi("burnerboard");
                                              break;
                                          case WifiManager.WIFI_STATE_ENABLING:
                                              l("WIFI STATE ENABLING");
                                              break;
                                          case WifiManager.WIFI_STATE_UNKNOWN:
                                              l("WIFI STATE UNKNOWN");
                                              break;
                                      }
                                  }
                              },
                new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

        intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(new MQTTBroadcastReceiver(),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mConnMan = (ConnectivityManager) mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);

        getPackageVersions();

        if (iotClient == null) {
            iotClient = new IoTClient(mContext, new IoTClient.IoTAction() {
                @Override
                public void onAction(String action) {
                    JSONObject theActions;

                    if (action == null) {
                        return;
                    }
                    try {
                        theActions = new JSONObject(action);

                        if (!theActions.has("commands")) {
                            l("No commands in action");
                            return;
                        }
                        JSONArray cmds = theActions.getJSONArray("commands");

                        for (int cmdNo = 0; cmdNo < cmds.length(); cmdNo++) {
                            JSONObject commandObj = cmds.getJSONObject(cmdNo);
                            String command = commandObj.getString("command");
                            if (command.contentEquals("upgrade")) {
                                if (!commandObj.has("version")) {
                                    l("No upgrade version");
                                }
                                mUpgradeToVersion = commandObj.getInt("version");
                                l("you will be upgraded to " + mUpgradeToVersion + ", do not resist!!!!");
                                if (commandObj.has("reboot")) {
                                    mDoReboot = commandObj.getBoolean("reboot");
                                    l("Reboot: " + mDoReboot);
                                }
                            }
                        }

                    } catch (Exception e) {
                        l("Cannot parse actions:" + e.getMessage());
                        return;
                    }

                }
            });
        }

        dlManager = new BBDownloadManager(getApplicationContext().getFilesDir().getAbsolutePath(), mVersion);
        dlManager.onProgressCallback = new BBDownloadManager.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    if (fileSize > 1048576) {
                        voice.speak("Downloading New Software " + file + ", " + String.valueOf(percent) + " Percent", TextToSpeech.QUEUE_ADD, null, "downloading");
                    }
                    lastTextTime = curTime;
                    l(String.format("Downloading %02x%% %s", bytesDownloaded * 100 / fileSize, file));
                }
            }

            public void onVoiceCue(String msg) {
                voice.speak(msg, TextToSpeech.QUEUE_ADD, null, "Download Message");
            }

        };

        dlManager.StartDownloads();

        Thread installerThread = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("BBInstaller Main");
                installerThread();
            }
        });

        installerThread.start();

        voice = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // check for successful instantiation
                if (status == TextToSpeech.SUCCESS) {
                    if (voice.isLanguageAvailable(Locale.UK) == TextToSpeech.LANG_AVAILABLE)
                        voice.setLanguage(Locale.US);
                }
            }
        });

    }

    protected String wifiIpAddress(WifiManager wifiManager) {

        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            l("Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public void installerThread() {

        String wifiAddress = "";

        while (true) {
            try {
                // Check WIFI
                android.net.wifi.SupplicantState s = mWiFiManager.getConnectionInfo().getSupplicantState();
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);
                if (state != NetworkInfo.DetailedState.CONNECTED) {
                    if (!mWiFiManager.isWifiEnabled()) {
                        mWiFiManager.setWifiEnabled(true);
                    }
                    mWiFiManager.reassociate();
                } else {

                }
                String ip = wifiIpAddress(mWiFiManager);
                try {
                    Intent intent = new Intent("com.android.server.NetworkTimeUpdateService.action.POLL", null);
                    mContext.sendBroadcast(intent);
                } catch (Exception e) {
                    Log.d(TAG, "Cannot send force-time-sync");
                }

                int currentBBVersion = getBBversion();
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("ip", ip);
                    obj.put("version", currentBBVersion);
                    obj.put("upgradeTo", mUpgradeToVersion);
                    l("state:" +  obj.toString());
                    iotClient.sendUpdate(obj.toString());
                } catch (Exception e) {
                }


                if (mUpgradeToVersion == -1) {
                    int latestBBFileId = dlManager.GetAPKByVersion("bb", 0);
                    int availableBBVersion = dlManager.GetAPKVersion(latestBBFileId);
                    l("Latest Downloaded APK is version " + availableBBVersion + ", installed is " + currentBBVersion);
                    if (availableBBVersion > currentBBVersion) {
                        String apkFile = dlManager.GetAPKFile(latestBBFileId);
                        if (installApk(apkFile)) {
                            l("Installed BB version " + currentBBVersion);
                            // Should we reboot?
                        } else {
                            l("Failed installing BB version " + currentBBVersion);
                        }
                    }
                } else if (mUpgradeToVersion > 0){
                    if (mUpgradeToVersion > currentBBVersion) {
                        l("IoT says upgrade to version " + mUpgradeToVersion);
                        String apkFile = null;
                        int thisBFileId = 0;
                        try {
                            thisBFileId = dlManager.GetAPKByVersion("bb", mUpgradeToVersion);
                            apkFile = dlManager.GetAPKFile(thisBFileId);
                        } catch (Exception e) {
                            l("Version not yet downloaded");
                        }
                        if (apkFile != null) {
                            if (installApk(apkFile)) {
                                l("Installed BB version " + mUpgradeToVersion);
                                voice.speak("Installed Software version " + mUpgradeToVersion, TextToSpeech.QUEUE_ADD, null, "swvers");
                                Thread.sleep(5000);
                                // Should we reboot?
                                if (mDoReboot) {
                                    voice.speak("Re booting", TextToSpeech.QUEUE_ADD, null, "swvers");
                                    Thread.sleep(3000);
                                    doReboot("Upgrade");
                                }
                            } else {
                                l("Failed installing BB version " + mUpgradeToVersion);
                                voice.speak("Failed upgrade of software version " + mUpgradeToVersion, TextToSpeech.QUEUE_ADD, null, "swversfail");

                            }
                        }

                    }
                }

                dlManager.StartDownloads();

            } catch (Exception e) {
                l("Unknown exception: " + e.getMessage());
            }

            try {
                Thread.sleep(60000);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Reboot the device.  Will not return if the reboot is successful.
     * <p>
     * Requires the {@link android.Manifest.permission#REBOOT} permission.
     * </p>
     *
     * @param reason code to pass to the kernel (e.g., "recovery") to
     *               request special boot modes, or null.
     */
    public void doReboot(String reason) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        pm.reboot(reason);
    }


    public boolean installApk(String path)
    {
        Log.i(TAG, "Installing software update " + path);

        final String libs = "LD_LIBRARY_PATH=/vendor/lib64:/system/lib64 ";

        final String[] commands = {
//                libs + "cp  " + path + " /data/local/tmp 2>&1 >>/data/local/tmp/bb.log" ,
//                libs + "chmod 666 /data/local/tmp/bb.apk 2>&1 >>/data/local/tmp/bb.log",
//                libs + "pm install -r /data/local/tmp/bb.apk",
                libs + "chmod 666 " + path,
                libs + "pm install -rg " + path
        };

        execute_as_root(commands);

        /*
        if (true || Shell.SU.available()) {
            //if(Shell.SU.run("pm install -r " + path) == null) {
            List<String> output = Shell.SH.run("su");
            if(output == null) {
                    return false;
            } else {
                Log.i(TAG, output.toString());
                return true;
            }
        } else {
            Log.i(TAG, "sudo not available for " + path);
            return false;
        }
        */
        return true;

    }

    private boolean execute_as_root( String[] commands ) {
        try {
            // Do the magic
            Process p = Runtime.getRuntime().exec( "sh" );
            InputStream es = p.getErrorStream();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            for( String command : commands ) {
                Log.i(TAG,command);
                os.writeBytes(command + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            os.close();

            int read;
            byte[] buffer = new byte[4096];
            String output = new String();
            while ((read = es.read(buffer)) > 0) {
                output += new String(buffer, 0, read);
            }

            p.waitFor();
            Log.e(TAG, output + " (" + p.exitValue() + ")");
            return true;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    public int getBBversion() {
        final PackageManager pm = getPackageManager();
        int versionCode = 0;
        try {
            PackageInfo info = pm.getPackageInfo("com.richardmcdougall.bb", 0);
            versionCode = info.versionCode;
            l("Version: " + versionCode);
        } catch (Exception e) {
        }
        return versionCode;
    }

    //get a list of installed apps.
    public void getPackageVersions() {
        final PackageManager pm = getPackageManager();

        packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            try {
                PackageInfo info = pm.getPackageInfo(packageInfo.packageName, 0);
                //l("Installed package :" + packageInfo.packageName + ", version: " + info.versionCode);
            } catch (Exception e) {
            }

        }
    }

    // Notify on connectivity
    class MQTTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean hasConnectivity = false;
            boolean hasChanged = false;
            NetworkInfo infos[] = mConnMan.getAllNetworkInfo();
            l("Connectivity changed");

            for (int i = 0; i < infos.length; i++) {
                if (infos[i].getTypeName().equalsIgnoreCase("MOBILE")) {
                    if ((infos[i].isConnected() != hasMmobile)) {
                        hasChanged = true;
                        hasMmobile = infos[i].isConnected();
                    }
                    l(infos[i].getTypeName() + " is " + infos[i].isConnected());
                } else if (infos[i].getTypeName().equalsIgnoreCase("WIFI")) {
                    if ((infos[i].isConnected() != hasWifi)) {
                        hasChanged = true;
                        hasWifi = infos[i].isConnected();
                    }
                    l(infos[i].getTypeName() + " is " + infos[i].isConnected());
                }
            }

            hasConnectivity = hasMmobile || hasWifi;
            l("hasConn: " + hasConnectivity + " hasChange: " + hasChanged);
            if (hasConnectivity && hasChanged) {
                //voice.speak("Network Connected", TextToSpeech.QUEUE_ADD, null, "swversfail");

            } else if (!hasConnectivity) {
                l("doDisconnect()");
                //voice.speak("Network Disconnected", TextToSpeech.QUEUE_ADD, null, "swversfail");
            }
        }
    }

    public void WifiStateChangedReceiver() {

    }

}



