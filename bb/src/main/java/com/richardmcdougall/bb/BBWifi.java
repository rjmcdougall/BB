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

import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteOrder;
import java.util.List;

public class BBWifi {

    private Context mContext;
    WifiManager mWiFiManager = null;
    // IP address of the device
    public String mIPAddress = null;
    private static final String TAG = "BB.BBWifi";
    public static final String ACTION_STATS = "com.richardmcdougall.bb.BBServiceStats";

    BBWifi(Context context){
        mContext = context;
        mWiFiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        if (checkWifiOnAndConnected(mWiFiManager) == false) {

            l("Enabling Wifi...");
            setupWifi();

        }

    }
    public String getIPAddress() {
        return mIPAddress;
    }

    private void setupWifi() {

        mContext.registerReceiver(new BroadcastReceiver() {
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
                                              if (checkWifiSSid(BurnerBoardUtil.WIFI_SSID) == false) {
                                                  l("adding wifi: " + BurnerBoardUtil.WIFI_SSID);
                                                  addWifi(BurnerBoardUtil.WIFI_SSID, BurnerBoardUtil.WIFI_PASS);
                                              }
                                              l("Connecting to wifi");
                                              connectWifi(BurnerBoardUtil.WIFI_SSID);
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

            if( wifiInfo.getNetworkId() == -1 ){
                mIPAddress = null;
                return false; // Not connected to an access point
            }

            mIPAddress = getWifiIpAddress(wifiMgr);
            if( mIPAddress != null) {
                l("WIFI IP Address: " + mIPAddress);
                // Text to speach is not set up yet at this time; move it to init loop.
                //voice.speak("My WIFI IP is " + mIPAddress, TextToSpeech.QUEUE_ADD, null, "wifi ip");
            } else {
                l( "Could not determine WIFI IP at this time");
            }

            return true; // Connected to an access point
        }
        else {
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

        String aWifi =fixWifiSSidAndPass(ssid);

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
            l( "Unable to get host address: " + ex.toString());
            ipAddressString = null;
        }

        return ipAddressString;
    }

}
