package com.richardmcdougall.bbcommon;

import android.content.Context;
import android.net.wifi.WifiManager;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class BBJmDNSService {
    private String TAG = this.getClass().getSimpleName();
    
    private Context context;
    private JmDNS jmdns;
    private ServiceInfo serviceInfo;
    private String serviceName;
    private String serviceType;
    private String hostname;
    private int servicePort;
    private boolean isStarted = false;
    private boolean isServiceRegistered = false;
    
    // Default configuration
    private static final String DEFAULT_SERVICE_TYPE = "_bb._tcp.local.";
    private static final int DEFAULT_PORT = 8080;
    
    public BBJmDNSService(Context context) {
        this.context = context;
        this.serviceName = "BB-Device";
        this.serviceType = DEFAULT_SERVICE_TYPE;
        this.servicePort = DEFAULT_PORT;
        this.hostname = "bb-device";
    }
    
    /**
     * Configure the mDNS service parameters
     * @param serviceName The name that will appear in mDNS discovery
     * @param hostname The hostname for .local resolution (without .local suffix)
     * @param port The port number the service is running on
     */
    public void configureService(String serviceName, String hostname, int port) {
        if (isServiceRegistered) {
            BLog.w(TAG, "Cannot configure service while it's registered. Stop first.");
            return;
        }
        
        this.serviceName = serviceName;
        this.hostname = sanitizeHostname(hostname);
        this.servicePort = port;
        BLog.i(TAG, "Service configured: " + serviceName + " at " + this.hostname + ".local:" + port);
    }
    
    /**
     * Configure service with board-specific information
     * @param boardState BoardState containing device information
     */
    public void configureFromBoardState(BoardState boardState) {
        String deviceName = boardState.BOARD_ID + "-BB";
        String hostnameBase = sanitizeHostname(boardState.BOARD_ID);
        configureService(deviceName, hostnameBase, DEFAULT_PORT);
        
        BLog.i(TAG, "Configured JmDNS service for board: " + boardState.BOARD_ID);
    }
    
    /**
     * Start the JmDNS service and register both hostname and service
     */
    public void start() {
        if (isStarted) {
            BLog.w(TAG, "JmDNS service already started");
            return;
        }
        
        try {
            // Get the device's WiFi IP address
            InetAddress localAddress = getLocalInetAddress();
            if (localAddress == null) {
                BLog.e(TAG, "Could not get local IP address");
                return;
            }
            
            // Create JmDNS instance
            jmdns = JmDNS.create(localAddress, hostname);
            isStarted = true;
            
            BLog.i(TAG, "JmDNS started with hostname: " + hostname + ".local (" + localAddress.getHostAddress() + ")");
            
            // Register the service
            registerService();
            
        } catch (IOException e) {
            BLog.e(TAG, "Failed to start JmDNS: " + e.getMessage());
            isStarted = false;
        }
    }
    
    /**
     * Register the service for discovery
     */
    private void registerService() {
        if (!isStarted || jmdns == null) {
            BLog.e(TAG, "JmDNS not started, cannot register service");
            return;
        }
        
        if (isServiceRegistered) {
            BLog.w(TAG, "Service already registered");
            return;
        }
        
        try {
            // Create TXT records with device information
            Map<String, String> txtRecords = new HashMap<>();
            txtRecords.put("version", "1.0");
            txtRecords.put("device", "BB");
            txtRecords.put("hostname", hostname);
            
            // Create and register the service
            serviceInfo = ServiceInfo.create(
                serviceType,
                serviceName,
                servicePort,
                0, // weight
                0, // priority
                txtRecords
            );
            
            jmdns.registerService(serviceInfo);
            isServiceRegistered = true;
            
            BLog.i(TAG, "Registered service: " + serviceName + " on " + serviceType + 
                   " at " + hostname + ".local:" + servicePort);
            
        } catch (IOException e) {
            BLog.e(TAG, "Failed to register service: " + e.getMessage());
            isServiceRegistered = false;
        }
    }
    
    /**
     * Unregister the service
     */
    private void unregisterService() {
        if (isServiceRegistered && jmdns != null && serviceInfo != null) {
            try {
                jmdns.unregisterService(serviceInfo);
                isServiceRegistered = false;
                BLog.i(TAG, "Unregistered service: " + serviceName);
            } catch (Exception e) {
                BLog.e(TAG, "Failed to unregister service: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stop the JmDNS service and clean up
     */
    public void stop() {
        if (!isStarted) {
            BLog.d(TAG, "JmDNS service not started");
            return;
        }
        
        try {
            // Unregister service first
            unregisterService();
            
            // Close JmDNS
            if (jmdns != null) {
                jmdns.close();
                BLog.i(TAG, "JmDNS service stopped");
            }
            
        } catch (IOException e) {
            BLog.e(TAG, "Error stopping JmDNS: " + e.getMessage());
        } finally {
            isStarted = false;
            isServiceRegistered = false;
            jmdns = null;
            serviceInfo = null;
        }
    }
    
    /**
     * Get the device's WiFi IP address
     */
    private InetAddress getLocalInetAddress() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                int ipInt = wifiManager.getConnectionInfo().getIpAddress();
                if (ipInt != 0) {
                    byte[] ipBytes = new byte[4];
                    ipBytes[0] = (byte) (ipInt & 0xff);
                    ipBytes[1] = (byte) ((ipInt >> 8) & 0xff);
                    ipBytes[2] = (byte) ((ipInt >> 16) & 0xff);
                    ipBytes[3] = (byte) ((ipInt >> 24) & 0xff);
                    return InetAddress.getByAddress(ipBytes);
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error getting WiFi IP address: " + e.getMessage());
        }
        
        // Fallback: try to get local host address
        try {
            return InetAddress.getLocalHost();
        } catch (Exception e) {
            BLog.e(TAG, "Error getting localhost address: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Sanitize hostname to comply with RFC standards
     * - Only alphanumeric characters and hyphens
     * - Cannot start or end with hyphen
     * - Maximum 63 characters
     */
    private String sanitizeHostname(String input) {
        if (input == null || input.isEmpty()) {
            return "bb-device";
        }
        
        // Convert to lowercase and replace invalid characters with hyphens
        String hostname = input.toLowerCase()
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
     * Check if the service is currently registered
     */
    public boolean isServiceRegistered() {
        return isServiceRegistered;
    }
    
    /**
     * Check if JmDNS is started
     */
    public boolean isStarted() {
        return isStarted;
    }
    
    /**
     * Get the current service name
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Get the current hostname
     */
    public String getHostname() {
        return hostname;
    }
    
    /**
     * Get the current service port
     */
    public int getServicePort() {
        return servicePort;
    }
    
    /**
     * Clean up resources when the service is no longer needed
     */
    public void cleanup() {
        stop();
    }
}
