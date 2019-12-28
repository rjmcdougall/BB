package com.richardmcdougall.bb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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

    private static final String WIFI_JSON = "wifi.json";
    private static final String TAG = "BB.BBWifi";
    private static final String WIFI_SSID = "burnerboard";
    private static final String WIFI_PASS = "firetruck";
    public boolean enableWifiReconnect = true;
    public String ipAddress = "";
    private int mWifiReconnectEveryNSeconds = 60;
    private BBService service = null;
    private WifiManager mWiFiManager = null;
    private List<ScanResult> mScanResults;
    public String SSID = "";
    public String password = "";

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mScanResults = mWiFiManager.getScanResults();
                d("wifi scan results" + mScanResults.toString());
            }
        }
    };
    
    BBWifi(BBService service) {

        this.service = service;
        mWiFiManager = (WifiManager) service.context.getSystemService(Context.WIFI_SERVICE);
        service.context.registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // look for an SSID and password in file system. If it is not there default to firetruck.
        getSSIDAndPassword();
        if (SSID == "") {
            setSSISAndPassword(WIFI_SSID, WIFI_PASS);
            getSSIDAndPassword();
        }

        if (checkWifiOnAndConnected(mWiFiManager) == false) {

            d("Enabling Wifi...");
            setupWifi();

        }
        ScanWifi();
    }

    public String getConnectedSSID() {
        return mWiFiManager.getConnectionInfo().getSSID();
    }

    public JSONArray getScanResults(){
        JSONArray a = new JSONArray();
        for(ScanResult s: mScanResults){
            a.put(s.SSID);
        }
        return a;
    }

    private void setupWifi() {

        service.context.registerReceiver(new BroadcastReceiver() {
                                      @Override
                                      public void onReceive(Context context, Intent intent) {
                                          int extraWifiState =
                                                  intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                                                          WifiManager.WIFI_STATE_UNKNOWN);

                                          switch (extraWifiState) {
                                              case WifiManager.WIFI_STATE_DISABLED:
                                                  d("WIFI STATE DISABLED");
                                                  break;
                                              case WifiManager.WIFI_STATE_DISABLING:
                                                  d("WIFI STATE DISABLING");
                                                  break;
                                              case WifiManager.WIFI_STATE_ENABLED:
                                                  d("WIFI STATE ENABLED");
                                                  int mfs = mWiFiManager.getWifiState();
                                                  d("Wifi state is " + mfs);
                                                  d("Checking wifi");
                                                  if (checkWifiSSid(SSID) == false) {
                                                      d("adding wifi: " + SSID);
                                                      addWifi(SSID, password);
                                                  }
                                                  d("Connecting to wifi");
                                                  if(!checkWifiOnAndConnected(mWiFiManager))
                                                        connectWifi(SSID);
                                                  break;
                                              case WifiManager.WIFI_STATE_ENABLING:
                                                  d("WIFI STATE ENABLING");
                                                  break;
                                              case WifiManager.WIFI_STATE_UNKNOWN:
                                                  d("WIFI STATE UNKNOWN");
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
                ipAddress = null;
                return false; // Not connected to an access point
            }

            d("Wifi SSIDs" + wifiInfo.getSSID() + " " + fixWifiSSidAndPass(SSID));
            if(!wifiInfo.getSSID().equals(fixWifiSSidAndPass(SSID))){
                ipAddress = null;
                return false; // configured for wrong access point.
            }

            ipAddress = getWifiIpAddress(wifiMgr);
            if (ipAddress != null) {
                d("WIFI IP Address: " + ipAddress);
                // Text to speach is not set up yet at this time; move it to init loop.
                //voice.speak("My WIFI IP is " + ipAddress, TextToSpeech.QUEUE_ADD, null, "wifi ip");
            } else {
                d("Could not determine WIFI IP at this time");
            }

            return true; // Connected to an access point
        } else {
            ipAddress = null;
            return false; // Wi-Fi adapter is OFF
        }
    }

    public void checkWifiReconnect() {
        if (checkWifiOnAndConnected(mWiFiManager) == false) {
            d("Enabling Wifi...");
            if (mWiFiManager.setWifiEnabled(true) == false) {
                d("Failed to enable wifi");
            }
            if (mWiFiManager.reassociate() == false) {
                d("Failed to associate wifi");
            }
        }
    }

    public void d(String logMsg) {
        if (DebugConfigs.DEBUG_WIFI) {
            Log.d(TAG, logMsg);
        }
    }

    public void e(String logMsg) {
        Log.e(TAG, logMsg);
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
                    d("Found wifi:" + newSSID + " == " + aWifi + " ?");
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

                    d("Found wifi:" + newSSID + " == " + aWifi + " ?");

                    if (aWifi.equalsIgnoreCase(newSSID)) {
                        d("connecting wifi:" + newSSID);
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
            e("Unable to get host address: " + ex.toString());
            ipAddressString = null;
        }

        return ipAddressString;
    }

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
            SSID = wifiSettings.getString("SSID");
            password = wifiSettings.getString("password");
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
                e.printStackTrace();
            }
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder(buf.readLine());
            d("contents of wifi.json: " + sb.toString());
            JSONObject j = new JSONObject(sb.toString());

            SSID = j.getString("SSID");
            password = j.getString("password");


        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run()
            {
                Thread.currentThread().setName("Supervisor");
                runSupervisor();
            }
        });
        t.start();
    }

    private void runSupervisor() {

        while (true) {

            // Every 60 seconds check WIFI
            if (service.wifi.enableWifiReconnect) {
                if (service.wifi != null) {
                    d("Check Wifi");
                    checkWifiReconnect();
                }
            }

            try {
                Thread.sleep(mWifiReconnectEveryNSeconds * 1000);
            } catch (Throwable e) {
            }

        }
    }
}
