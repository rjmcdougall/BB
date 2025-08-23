package com.richardmcdougall.bbcommon;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.util.HashMap;
import java.util.Map;

public class BBmDNSService {
    private String TAG = this.getClass().getSimpleName();
    
    private Context context;
    private NsdManager nsdManager;
    private NsdServiceInfo serviceInfo;
    private String serviceName;
    private String serviceType;
    private int servicePort;
    private boolean isRegistered = false;
    
    private NsdManager.RegistrationListener registrationListener;
    
    public BBmDNSService(Context context) {
        this.context = context;
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        
        // Default service configuration
        this.serviceName = "BB-Device";
        this.serviceType = "_bb._tcp"; // Custom service type for BB devices
        this.servicePort = 8080; // Default port - can be changed
        
        initializeRegistrationListener();
    }
    
    /**
     * Configure the mDNS service parameters
     * @param serviceName The name that will appear in mDNS discovery
     * @param serviceType The service type (e.g., "_http._tcp", "_bb._tcp")
     * @param port The port number the service is running on
     */
    public void configureService(String serviceName, String serviceType, int port) {
        if (isRegistered) {
            BLog.w(TAG, "Cannot configure service while it's registered. Unregister first.");
            return;
        }
        
        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.servicePort = port;
        BLog.i(TAG, "Service configured: " + serviceName + " on " + serviceType + ":" + port);
    }
    
    /**
     * Configure service with board-specific information
     * @param boardState BoardState containing device information
     */
    public void configureFromBoardState(BoardState boardState) {
        String deviceName = boardState.BOARD_ID + "-BB";
        configureService(deviceName, "_bb._tcp", 8080);
        
        // You can add more specific configuration based on boardState here
        BLog.i(TAG, "Configured mDNS service for board: " + boardState.BOARD_ID);
    }
    
    /**
     * Register the mDNS service on the network
     */
    public void registerService() {
        if (isRegistered) {
            BLog.w(TAG, "Service already registered");
            return;
        }
        
        if (nsdManager == null) {
            BLog.e(TAG, "NsdManager is null, cannot register service");
            return;
        }
        
        try {
            serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(serviceName);
            serviceInfo.setServiceType(serviceType);
            serviceInfo.setPort(servicePort);
            
            // Add TXT records with device information
            // Note: TXT records are optional and may not be supported on all Android versions
            // The basic service registration will work without them
            
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
            BLog.i(TAG, "Registering mDNS service: " + serviceName);
            
        } catch (Exception e) {
            BLog.e(TAG, "Failed to register mDNS service: " + e.getMessage());
        }
    }
    
    /**
     * Unregister the mDNS service from the network
     */
    public void unregisterService() {
        if (!isRegistered || nsdManager == null || registrationListener == null) {
            BLog.d(TAG, "Service not registered or already unregistered");
            return;
        }
        
        try {
            nsdManager.unregisterService(registrationListener);
            BLog.i(TAG, "Unregistering mDNS service: " + serviceName);
        } catch (Exception e) {
            BLog.e(TAG, "Failed to unregister mDNS service: " + e.getMessage());
        }
    }
    
    /**
     * Check if the service is currently registered
     */
    public boolean isServiceRegistered() {
        return isRegistered;
    }
    
    /**
     * Get the current service name
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Get the current service type
     */
    public String getServiceType() {
        return serviceType;
    }
    
    /**
     * Get the current service port
     */
    public int getServicePort() {
        return servicePort;
    }
    
    private void initializeRegistrationListener() {
        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                // Save the service name. Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                serviceName = nsdServiceInfo.getServiceName();
                isRegistered = true;
                BLog.i(TAG, "mDNS service registered successfully: " + serviceName);
            }
            
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed! Put debugging code here to determine why.
                isRegistered = false;
                BLog.e(TAG, "mDNS service registration failed with error code: " + errorCode);
            }
            
            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                // Service has been unregistered. This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                isRegistered = false;
                BLog.i(TAG, "mDNS service unregistered: " + serviceInfo.getServiceName());
            }
            
            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed. Put debugging code here to determine why.
                BLog.e(TAG, "mDNS service unregistration failed with error code: " + errorCode);
            }
        };
    }
    
    /**
     * Clean up resources when the service is no longer needed
     */
    public void cleanup() {
        unregisterService();
        // Give some time for unregistration to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
