package com.richardmcdougall.bbcommon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import org.json.JSONArray;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BBWifi {
    private String TAG = this.getClass().getSimpleName();

    public boolean enableWifiReconnect = true;
    public String ipAddress = "0.0.0.0";
    private int wifiReconnectEveryNSeconds = 20;
    private WifiManager wifiManager = null;
    private List<ScanResult> scanResults;
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private Context context = null;
    private BoardState boardState = null;

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                scanResults = wifiManager.getScanResults();
                BLog.i(TAG, "wifi scan results" + scanResults.toString());
            }
        }
    };

    public BBWifi(Context context, BoardState boardState) {
        this.context = context;
        this.boardState = boardState;

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        context.registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifiManager.startScan();

        BLog.d(TAG, "Enable WiFi reconnect? " + enableWifiReconnect);

        sch.scheduleWithFixedDelay(wifiSupervisor, 10, wifiReconnectEveryNSeconds, TimeUnit.SECONDS);
    }

    public String getConnectedSSID() {
        return wifiManager.getConnectionInfo().getSSID();
    }

    public JSONArray getScanResults() {
        JSONArray a = new JSONArray();
        for (ScanResult s : scanResults) {
            a.put(s.SSID);
        }
        return a;
    }

    public void checkWifiReconnect() {

        String newIPAddress = null;

        try {

            newIPAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            if (!newIPAddress.equals("0.0.0.0")){
                BLog.i(TAG, "Connected to " +  wifiManager.getConnectionInfo().getSSID() + "IP Address: " + newIPAddress);
            }
            else {

                BLog.i(TAG, "No IP Address Found, attempting to Connect");

                if (!wifiManager.isWifiEnabled()) {
                    BLog.d(TAG, "Enabling Wifi...");

                    if (!wifiManager.setWifiEnabled(true)) {
                        throw new Exception("Failed to enable wifi");
                    }
                    if (!wifiManager.reassociate()) {
                        throw new Exception("Failed to associate wifi");
                    }
                }

                // are you on a network?
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getNetworkId() != -1) {
                    BLog.d(TAG, "Connected to Wifi");
                } else {
                    connectToExistingWIFI();
                    if (wifiInfo.getNetworkId() != -1) {
                        BLog.d(TAG, "Reconnected to Existing Wifi");
                    } else {
                        BLog.d(TAG, "Adding wifi: " + boardState.SSID);
                        addwifiFromFileAndConnect();
                        BLog.d(TAG, "Connected to Wifi: " + boardState.SSID);
                    }
                }

                Thread.sleep(1000);
                newIPAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            }
            ipAddress = newIPAddress;

        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    }

    private String fixWifiSSidAndPass(String ssid) {
        String fixedSSid = ssid;
        fixedSSid = ssid.startsWith("\"") ? fixedSSid : "\"" + fixedSSid;
        fixedSSid = ssid.endsWith("\"") ? fixedSSid : fixedSSid + "\"";
        return fixedSSid;
    }

    private void connectToExistingWIFI() {

        try {
            List<WifiConfiguration> wifiList = wifiManager.getConfiguredNetworks();
            if (wifiList != null) {
                for (WifiConfiguration config : wifiList) {
                    String newSSID = config.SSID;

                    BLog.d(TAG, "connecting wifi:" + newSSID);
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(config.networkId, true);
                    wifiManager.reconnect();

                    return;
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error attempting to connect to configured Wifi");
        }
    }

    private void addwifiFromFileAndConnect() {
        try {

            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = fixWifiSSidAndPass(this.boardState.SSID);
            conf.preSharedKey = fixWifiSSidAndPass(this.boardState.password);

            wifiManager.addNetwork(conf);
            wifiManager.disconnect();
            wifiManager.enableNetwork(conf.networkId, false);
            wifiManager.reconnect();
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
        return;
    }

    Runnable wifiSupervisor = () -> {
        if (enableWifiReconnect) {
            checkWifiReconnect();
        }
    };

}
