package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.net.wifi.ScanResult;

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
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public class BBWifi {

    /**
     * Indicates whether Wifi reconnecting is enabled and how often
     */
    public boolean mEnableWifiReconnect = true;
    public int mWifiReconnectEveryNSeconds = 60;

    private Context mContext;
    WifiManager mWiFiManager = null;
    // IP address of the device
    public String mIPAddress = null;
    private static final String TAG = "BB.BBWifi";
    public static final String ACTION_STATS = "com.richardmcdougall.bb.BBServiceStats";
    List<ScanResult> mScanResults;
    public String SSID = "";
    public String password = "";

    public String getSSID() {
        return mWiFiManager.getConnectionInfo().getSSID();
    }
    public String getConfiguredSSID(){
        return SSID;
    }
    public JSONArray getScanResults(){
        JSONArray a = new JSONArray();
        for(ScanResult s: mScanResults){
            a.put(s.SSID);
        }
        return a;
    }
    public String getConfiguredPassword(){
        return password;
    }
    BBWifi(BBService service) {

        mContext = service.context;
        mWiFiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mContext.registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // look for an SSID and password in file system. If it is not there default to firetruck.
        getSSIDAndPassword();
        if (SSID == "") {
            setSSISAndPassword(BurnerBoardUtil.WIFI_SSID, BurnerBoardUtil.WIFI_PASS);
            getSSIDAndPassword();
        }

        if (checkWifiOnAndConnected(mWiFiManager) == false) {

            l("Enabling Wifi...");
            setupWifi();

        }
        ScanWifi();
    }

    public String getIPAddress() {
        return mIPAddress;
    }

    private void setupWifi() {

        mContext.registerReceiver(new BroadcastReceiver() {
                                      @Override
                                      public void onReceive(Context context, Intent intent) {
                                          int extraWifiState =
                                                  intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                                                          WifiManager.WIFI_STATE_UNKNOWN);

                                          switch (extraWifiState) {
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
                                                  if (checkWifiSSid(SSID) == false) {
                                                      l("adding wifi: " + SSID);
                                                      addWifi(SSID, password);
                                                  }
                                                  l("Connecting to wifi");
                                                  if(!checkWifiOnAndConnected(mWiFiManager))
                                                        connectWifi(SSID);
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
    }

    private boolean checkWifiOnAndConnected(WifiManager wifiMgr) {

        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if (wifiInfo.getNetworkId() == -1) {
                mIPAddress = null;
                return false; // Not connected to an access point
            }

            l("Wifi SSIDs" + wifiInfo.getSSID() + " " + fixWifiSSidAndPass(SSID));
            if(!wifiInfo.getSSID().equals(fixWifiSSidAndPass(SSID))){
                mIPAddress = null;
                return false; // configured for wrong access point.
            }

            mIPAddress = getWifiIpAddress(wifiMgr);
            if (mIPAddress != null) {
                l("WIFI IP Address: " + mIPAddress);
                // Text to speach is not set up yet at this time; move it to init loop.
                //voice.speak("My WIFI IP is " + mIPAddress, TextToSpeech.QUEUE_ADD, null, "wifi ip");
            } else {
                l("Could not determine WIFI IP at this time");
            }

            return true; // Connected to an access point
        } else {
            mIPAddress = null;
            return false; // Wi-Fi adapter is OFF
        }
    }

    public void checkWifiReconnect() {
        if (checkWifiOnAndConnected(mWiFiManager) == false) {
            l("Enabling Wifi...");
            if (mWiFiManager.setWifiEnabled(true) == false) {
                l("Failed to enable wifi");
            }
            if (mWiFiManager.reassociate() == false) {
                l("Failed to associate wifi");
            }
        }
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    /* On android, wifi SSIDs and passwords MUST be passed quoted. This fixes up the raw SSID & Pass -jib */
    /* XXX TODO this code doesn't get exercised if the SSID is already in the known config it seems
       which makes this code VERY hard to test. What's the right strategy? -jib

        Here's some test code to run to manually verify:

        String ssid = BurnerBoardUtil.WIFI_SSID;
        String pass = BurnerBoardUtil.WIFI_PASS;
        String qssid = "\"" + ssid + "\"";
        String qpass = "\"" + pass + "\"";

        String fssid = fixWifiSSidAndPass((ssid));
        String fpass = fixWifiSSidAndPass(pass);
        String fqssid = fixWifiSSidAndPass(qssid);
        String fqpass = fixWifiSSidAndPass(qpass);

        boolean ssid_eq = fssid.equals(fqssid);
        boolean pass_eq = fpass.equals(fqpass);

        l("ssid: " + ssid + " - qssid: " + qssid + " - fssid: " + fssid + " - fqssid: " + fqssid + " - equals? " + ssid_eq);
        l("pass: " + pass + " - qpass: " + qpass + " - fpass: " + fpass + " - fqpass: " + fqpass + " - equals? " + pass_eq);

     */
    private String fixWifiSSidAndPass(String ssid) {
        String fixedSSid = ssid;
        fixedSSid = ssid.startsWith("\"") ? fixedSSid : "\"" + fixedSSid;
        fixedSSid = ssid.endsWith("\"") ? fixedSSid : fixedSSid + "\"";
        return fixedSSid;
    }

    private boolean checkWifiSSid(String ssid) {

        String aWifi = fixWifiSSidAndPass(ssid);

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

        String aWifi = fixWifiSSidAndPass(ssid);

        try {
            List<WifiConfiguration> wifiList = mWiFiManager.getConfiguredNetworks();
            if (wifiList != null) {
                for (WifiConfiguration config : wifiList) {
                    String newSSID = config.SSID;

                    l("Found wifi:" + newSSID + " == " + aWifi + " ?");

                    if (aWifi.equalsIgnoreCase(newSSID)) {
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
            conf.SSID = fixWifiSSidAndPass(ssid);
            conf.preSharedKey = fixWifiSSidAndPass(pass);

            mWiFiManager.addNetwork(conf);
            mWiFiManager.disconnect();
            mWiFiManager.enableNetwork(conf.networkId, false);
            mWiFiManager.reconnect();
        } catch (Exception e) {
        }
        return;
    }

    // Cargo culted directly off of stack overflow: https://stackoverflow.com/questions/16730711/get-my-wifi-ip-address-android
    public String getWifiIpAddress(WifiManager wifiManager) {
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (Exception ex) {
            l("Unable to get host address: " + ex.toString());
            ipAddressString = null;
        }

        return ipAddressString;
    }



    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mScanResults = mWiFiManager.getScanResults();
                l("wifi scan results" + mScanResults.toString());
            }
        }
    };

    public void ScanWifi() {
        mWiFiManager.startScan();
    }

    public boolean setSSISAndPassword(String SSID, String password) {

        try {
            JSONObject wifiSettings = new JSONObject();
            wifiSettings.put("SSID", SSID);
            wifiSettings.put("password", password);

            setSSISAndPassword(wifiSettings);

        } catch (JSONException e) {
            l(e.getMessage());
            return false;
        }

        return true;
    }

    public boolean setSSISAndPassword(JSONObject wifiSettings) {

        try {
            FileWriter fw = new FileWriter(BurnerBoardUtil.publicNameDir + "/" + BurnerBoardUtil.wifiJSON);
            fw.write(wifiSettings.toString());
            fw.close();
            SSID = wifiSettings.getString("SSID");
            password = wifiSettings.getString("password");
        } catch (JSONException e) {
            l(e.getMessage());
            return false;
        } catch (IOException e) {
            l(e.getMessage());
            return false;
        }

        return true;
    }

    public void getSSIDAndPassword() {
        try {
            ArrayList<String> r = new ArrayList();

            File f = new File(BurnerBoardUtil.publicNameDir + "/" + BurnerBoardUtil.wifiJSON);
            InputStream is = null;
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder(buf.readLine());
            l("contents of wifi.json: " + sb.toString());
            JSONObject j = new JSONObject(sb.toString());

            SSID = j.getString("SSID");
            password = j.getString("password");


        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
