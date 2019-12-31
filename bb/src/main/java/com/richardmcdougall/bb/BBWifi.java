package com.richardmcdougall.bb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;

import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteOrder;
import java.util.List;

import org.json.JSONArray;

import timber.log.Timber;

public class BBWifi {

    public boolean enableWifiReconnect = true;
    public String ipAddress = "";
    private int mWifiReconnectEveryNSeconds = 60;
    private BBService service = null;
    private WifiManager mWiFiManager = null;
    private List<ScanResult> mScanResults;

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mScanResults = mWiFiManager.getScanResults();
                Timber.i("wifi scan results" + mScanResults.toString());
            }
        }
    };

    BBWifi(BBService service) {

        this.service = service;
        mWiFiManager = (WifiManager) service.context.getSystemService(Context.WIFI_SERVICE);
        service.context.registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (checkWifiOnAndConnected(mWiFiManager) == false) {

            Timber.d("Enabling Wifi...");
            setupWifi();

        }
        ScanWifi();
    }

    public String getConnectedSSID() {
        return mWiFiManager.getConnectionInfo().getSSID();
    }

    public JSONArray getScanResults() {
        JSONArray a = new JSONArray();
        for (ScanResult s : mScanResults) {
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
                                                         Timber.d("WIFI STATE DISABLED");
                                                         break;
                                                     case WifiManager.WIFI_STATE_DISABLING:
                                                         Timber.d("WIFI STATE DISABLING");
                                                         break;
                                                     case WifiManager.WIFI_STATE_ENABLED:
                                                         Timber.d("WIFI STATE ENABLED");
                                                         int mfs = mWiFiManager.getWifiState();
                                                         Timber.d("Wifi state is " + mfs);
                                                         Timber.d("Checking wifi");
                                                         if (checkWifiSSid(service.boardState.SSID) == false) {
                                                             Timber.d("adding wifi: " + service.boardState.SSID);
                                                             addWifi(service.boardState.SSID, service.boardState.password);
                                                         }
                                                         Timber.d("Connecting to wifi");
                                                         if (!checkWifiOnAndConnected(mWiFiManager))
                                                             connectWifi(service.boardState.SSID);
                                                         break;
                                                     case WifiManager.WIFI_STATE_ENABLING:
                                                         Timber.d("WIFI STATE ENABLING");
                                                         break;
                                                     case WifiManager.WIFI_STATE_UNKNOWN:
                                                         Timber.d("WIFI STATE UNKNOWN");
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

            Timber.d("Wifi SSIDs" + wifiInfo.getSSID() + " " + fixWifiSSidAndPass(service.boardState.SSID));
            if (!wifiInfo.getSSID().equals(fixWifiSSidAndPass(service.boardState.SSID))) {
                ipAddress = null;
                return false; // configured for wrong access point.
            }

            ipAddress = getWifiIpAddress(wifiMgr);
            if (ipAddress != null) {
                Timber.d("WIFI IP Address: " + ipAddress);
                // Text to speach is not set up yet at this time; move it to init loop.
                //voice.speak("My WIFI IP is " + ipAddress, TextToSpeech.QUEUE_ADD, null, "wifi ip");
            } else {
                Timber.d("Could not determine WIFI IP at this time");
            }

            return true; // Connected to an access point
        } else {
            ipAddress = null;
            return false; // Wi-Fi adapter is OFF
        }
    }

    public void checkWifiReconnect() {
        if (checkWifiOnAndConnected(mWiFiManager) == false) {
            Timber.d("Enabling Wifi...");
            if (mWiFiManager.setWifiEnabled(true) == false) {
                Timber.d("Failed to enable wifi");
            }
            if (mWiFiManager.reassociate() == false) {
                Timber.d("Failed to associate wifi");
            }
        }
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
                    Timber.d("Found wifi:" + newSSID + " == " + aWifi + " ?");
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

                    Timber.d("Found wifi:" + newSSID + " == " + aWifi + " ?");

                    if (aWifi.equalsIgnoreCase(newSSID)) {
                        Timber.d("connecting wifi:" + newSSID);
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
            Timber.e("Unable to get host address: " + ex.toString());
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public void ScanWifi() {
        mWiFiManager.startScan();
    }

    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
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
                    Timber.d("Check Wifi");
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
