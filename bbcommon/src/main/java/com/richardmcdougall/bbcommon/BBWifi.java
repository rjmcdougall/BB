package com.richardmcdougall.bbcommon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
//import android.support.v4.app.ActivityCompat;
import android.text.format.Formatter;

import org.json.JSONArray;

import java.security.spec.ECField;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BBWifi {
    private String TAG = this.getClass().getSimpleName();

    public boolean enableWifiReconnect = true;
    public String ipAddress = "0.0.0.0";
    private String previousIpAddress = "0.0.0.0";
    private int wifiReconnectEveryNSeconds = 20;
    private WifiManager wifiManager = null;
    private List<ScanResult> scanResults;
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private Context context = null;
    private BoardState boardState = null;
    private BBmDNSService mdnsService = null;

    public BBWifi(Context context, BoardState boardState) {
        this.context = context;
        this.boardState = boardState;

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        BLog.d(TAG, "Enable WiFi reconnect? " + enableWifiReconnect);

        // Initialize mDNS service
        initializeMDNS();
        
        // Set DHCP hostname based on board name
        setDHCPHostname();

        sch.scheduleWithFixedDelay(wifiSupervisor, 30, wifiReconnectEveryNSeconds, TimeUnit.SECONDS);
    }


    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                scanResults = wifiManager.getScanResults();
                BLog.i(TAG, "wifi scan results" + scanResults.toString());
            }
        }
    };
    public String getConnectedSSID() {
        return wifiManager.getConnectionInfo().getSSID();
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
                    Thread.sleep(10000);
                    if (!wifiManager.reassociate()) {
                        throw new Exception("Failed to associate wifi");
                    }
                    Thread.sleep(10000);
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
            
            // Handle mDNS service based on IP address changes
            handleMDNSService(newIPAddress);
            
            previousIpAddress = ipAddress;
            ipAddress = newIPAddress;

        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    }

    public JSONArray getScanResults() {
        JSONArray a = new JSONArray();
        for (ScanResult s : scanResults) {
            a.put(s.SSID);
        }
        return a;
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

    /**
     * Initialize mDNS service
     */
    private void initializeMDNS() {
        try {
            mdnsService = new BBmDNSService(context);
            if (boardState != null) {
                mdnsService.configureFromBoardState(boardState);
            }
            BLog.i(TAG, "mDNS service initialized");
        } catch (Exception e) {
            BLog.e(TAG, "Failed to initialize mDNS service: " + e.getMessage());
        }
    }

    /**
     * Handle mDNS service registration based on WiFi connection status
     */
    private void handleMDNSService(String currentIpAddress) {
        if (mdnsService == null) {
            BLog.w(TAG, "mDNS service not initialized");
            return;
        }

        try {
            // If we have a valid IP address and it changed from disconnected state
            if (!currentIpAddress.equals("0.0.0.0") && previousIpAddress.equals("0.0.0.0")) {
                // WiFi just connected - register mDNS service
                BLog.i(TAG, "WiFi connected, registering mDNS service");
                mdnsService.registerService();
            }
            // If we lost IP address (disconnected)
            else if (currentIpAddress.equals("0.0.0.0") && !previousIpAddress.equals("0.0.0.0")) {
                // WiFi disconnected - unregister mDNS service
                BLog.i(TAG, "WiFi disconnected, unregistering mDNS service");
                mdnsService.unregisterService();
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error handling mDNS service: " + e.getMessage());
        }
    }

    /**
     * Get the mDNS service instance
     */
    public BBmDNSService getMDNSService() {
        return mdnsService;
    }

    /**
     * Manually register mDNS service
     */
    public void registerMDNSService() {
        if (mdnsService != null && !ipAddress.equals("0.0.0.0")) {
            mdnsService.registerService();
        }
    }

    /**
     * Manually unregister mDNS service
     */
    public void unregisterMDNSService() {
        if (mdnsService != null) {
            mdnsService.unregisterService();
        }
    }

    /**
     * Clean up resources including mDNS service
     */
    public void cleanup() {
        if (mdnsService != null) {
            mdnsService.cleanup();
        }
    }

    /**
     * Set DHCP hostname (Option 12) based on board name
     * This attempts to set the hostname that will be sent in DHCP requests
     */
    private void setDHCPHostname() {
        if (boardState == null || boardState.BOARD_ID == null) {
            BLog.w(TAG, "No board state available for hostname setting");
            return;
        }

        String hostname = sanitizeHostname(boardState.BOARD_ID);
        BLog.i(TAG, "Setting DHCP hostname to: " + hostname);

        try {
            // Method 1: Try to set system property (requires root on most devices)
            setSystemHostname(hostname);
            
            // Method 2: Try to set hostname via WifiConfiguration if supported
            setWifiConfigHostname(hostname);
            
        } catch (Exception e) {
            BLog.e(TAG, "Failed to set DHCP hostname: " + e.getMessage());
        }
    }

    /**
     * Sanitize hostname to comply with RFC standards
     * - Only alphanumeric characters and hyphens
     * - Cannot start or end with hyphen
     * - Maximum 63 characters
     */
    private String sanitizeHostname(String boardId) {
        if (boardId == null) {
            return "bb-device";
        }
        
        // Convert to lowercase and replace invalid characters with hyphens
        String hostname = boardId.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-"); // Replace multiple hyphens with single hyphen
        
        // Remove leading/trailing hyphens
        hostname = hostname.replaceAll("^-+|-+$", "");
        
        // Ensure it's not empty and not too long
        if (hostname.isEmpty()) {
            hostname = "bb-device";
        }
        if (hostname.length() > 63) {
            hostname = hostname.substring(0, 63);
        }
        
        return hostname;
    }

    /**
     * Attempt to set system hostname using system properties
     * Note: This typically requires root access on Android
     */
    private void setSystemHostname(String hostname) {
        try {
            // Try setting net.hostname system property
            java.lang.reflect.Method setProperty = System.class.getMethod("setProperty", String.class, String.class);
            setProperty.invoke(null, "net.hostname", hostname);
            BLog.i(TAG, "Set net.hostname system property to: " + hostname);
        } catch (Exception e) {
            BLog.d(TAG, "Could not set net.hostname system property (normal on non-root devices): " + e.getMessage());
        }

        try {
            // Try using reflection to call SystemProperties.set (Android internal API)
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method set = systemProperties.getMethod("set", String.class, String.class);
            set.invoke(null, "net.hostname", hostname);
            BLog.i(TAG, "Set SystemProperties net.hostname to: " + hostname);
        } catch (Exception e) {
            BLog.d(TAG, "Could not set SystemProperties net.hostname (normal on newer Android): " + e.getMessage());
        }
    }

    /**
     * Try to set hostname through WifiConfiguration
     * Note: Limited support in Android API
     */
    private void setWifiConfigHostname(String hostname) {
        try {
            // For newer Android versions, try setting via WifiNetworkSpecifier if available
            // This is mainly for reference - actual implementation depends on Android version
            BLog.d(TAG, "Hostname will be: " + hostname + " (WiFi config method not fully supported in all Android versions)");
            
            // Store hostname for potential use in custom DHCP implementations
            setSystemProperty("dhcp.hostname", hostname);
            
        } catch (Exception e) {
            BLog.d(TAG, "WiFi config hostname setting not supported: " + e.getMessage());
        }
    }

    /**
     * Attempt to set a system property
     */
    private void setSystemProperty(String key, String value) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method set = systemProperties.getMethod("set", String.class, String.class);
            set.invoke(null, key, value);
            BLog.i(TAG, "Set system property " + key + " = " + value);
        } catch (Exception e) {
            BLog.d(TAG, "Could not set system property " + key + ": " + e.getMessage());
        }
    }

    /**
     * Get the current hostname that would be used for DHCP
     */
    public String getDHCPHostname() {
        if (boardState != null && boardState.BOARD_ID != null) {
            return sanitizeHostname(boardState.BOARD_ID);
        }
        return "bb-device";
    }

}
