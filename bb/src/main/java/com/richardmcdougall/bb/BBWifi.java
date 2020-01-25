package com.richardmcdougall.bb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.json.JSONArray;

import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BBWifi {
    private String TAG = this.getClass().getSimpleName();

    public boolean enableWifiReconnect = true;
    public String ipAddress = "";
    private int mWifiReconnectEveryNSeconds = 60;
    private BBService service = null;
    private WifiManager mWiFiManager = null;
    private List<ScanResult> mScanResults;
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mScanResults = mWiFiManager.getScanResults();
                BLog.i(TAG, "wifi scan results" + mScanResults.toString());
            }
        }
    };

    BBWifi(BBService service) {

        this.service = service;
        mWiFiManager = (WifiManager) service.context.getSystemService(Context.WIFI_SERVICE);
        service.context.registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (checkWifiOnAndConnected(mWiFiManager) == false) {

            BLog.d(TAG, "Enabling Wifi...");
            setupWifi();

        }
        ScanWifi();

        BLog.d(TAG, "Enable WiFi reconnect? " + enableWifiReconnect);

        sch.scheduleWithFixedDelay(wifiSupervisor, 10, mWifiReconnectEveryNSeconds, TimeUnit.SECONDS);
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
                                                         BLog.d(TAG, "WIFI STATE DISABLED");
                                                         break;
                                                     case WifiManager.WIFI_STATE_DISABLING:
                                                         BLog.d(TAG, "WIFI STATE DISABLING");
                                                         break;
                                                     case WifiManager.WIFI_STATE_ENABLED:
                                                         BLog.d(TAG, "WIFI STATE ENABLED");
                                                         int mfs = mWiFiManager.getWifiState();
                                                         BLog.d(TAG, "Wifi state is " + mfs);
                                                         BLog.d(TAG, "Checking wifi");
                                                         if (checkWifiSSid(service.boardState.SSID) == false) {
                                                             BLog.d(TAG, "adding wifi: " + service.boardState.SSID);
                                                             addWifi(service.boardState.SSID, service.boardState.password);
                                                         }
                                                         BLog.d(TAG, "Connecting to wifi");
                                                         if (!checkWifiOnAndConnected(mWiFiManager))
                                                             connectWifi(service.boardState.SSID);
                                                         break;
                                                     case WifiManager.WIFI_STATE_ENABLING:
                                                         BLog.d(TAG, "WIFI STATE ENABLING");
                                                         break;
                                                     case WifiManager.WIFI_STATE_UNKNOWN:
                                                         BLog.d(TAG, "WIFI STATE UNKNOWN");
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

            BLog.d(TAG, "Wifi SSIDs" + wifiInfo.getSSID() + " " + fixWifiSSidAndPass(service.boardState.SSID));
            if (!wifiInfo.getSSID().equals(fixWifiSSidAndPass(service.boardState.SSID))) {
                ipAddress = null;
                return false; // configured for wrong access point.
            }

            ipAddress = getWifiIpAddress(wifiMgr);
            if (ipAddress != null) {
                BLog.d(TAG, "WIFI IP Address: " + ipAddress);
                // Text to speach is not set up yet at this time; move it to init loop.
            } else {
                BLog.d(TAG, "Could not determine WIFI IP at this time");
            }

            return true; // Connected to an access point
        } else {
            ipAddress = null;
            return false; // Wi-Fi adapter is OFF
        }
    }

    public void checkWifiReconnect() {
        if (checkWifiOnAndConnected(mWiFiManager) == false) {
            BLog.d(TAG, "Enabling Wifi...");
            if (mWiFiManager.setWifiEnabled(true) == false) {
                BLog.d(TAG, "Failed to enable wifi");
            }
            if (mWiFiManager.reassociate() == false) {
                BLog.d(TAG, "Failed to associate wifi");
            }
        }
    }

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
                    BLog.d(TAG, "Found wifi:" + newSSID + " == " + aWifi + " ?");
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

                    BLog.d(TAG, "Found wifi:" + newSSID + " == " + aWifi + " ?");

                    if (aWifi.equalsIgnoreCase(newSSID)) {
                        BLog.d(TAG, "connecting wifi:" + newSSID);
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
            BLog.e(TAG, "Unable to get host address: " + ex.toString());
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public void ScanWifi() {
        mWiFiManager.startScan();
    }

    Runnable wifiSupervisor = () -> {
        if (service.wifi.enableWifiReconnect) {
            BLog.d(TAG, "Check Wifi");
            checkWifiReconnect();
        }
    };

}
