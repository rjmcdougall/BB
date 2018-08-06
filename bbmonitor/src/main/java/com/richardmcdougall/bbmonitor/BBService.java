package com.richardmcdougall.bbmonitor;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcelable;

import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;


public class BBService extends Service {

    public static final boolean debug = false;

    private static final String TAG = "BB.BBService";


    public static final String ACTION_BB_PACKET = "com.richardmcdougall.bbmonitor.ACTION_BB_PACKET";
    public static final String ACTION_STATS = "com.richardmcdougall.bbmonitor.ACTION_STATS";
    public static final String ACTION_BB_LOCATION = "com.richardmcdougall.bbmonitor.ACTION_BB_LOCATION";

    public com.richardmcdougall.bbmonitor.DownloadManager dlManager;


    public Handler mHandler = null;
    private Context mContext;

    /* XXX TODO this string is accessed both directly here in this class, as well as used via getBoardId() on the object it provides. refactor -jib */
    public static String boardId = BurnerBoardUtil.BOARD_ID;

    WifiManager mWiFiManager = null;
    public RF mRadio = null;
    public FindMyFriends mFindMyFriends = null;

    public BBService() {
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }

    public void d(String s) {
        if (BBService.debug == true) {
            Log.v(TAG, s);
            sendLogMsg(s);
        }
    }

    /* XXX TODO this is here for backwards compat; this used to be computed here and is now in bbutil -jib */
    public static String getBoardId() {
        return BurnerBoardUtil.BOARD_ID;
    }

    /**
     * interface for clients that bind
     */
    IBinder mBinder;

    /**
     * indicates whether onRebind should be used
     */
    boolean mAllowRebind;

    @Override
    public void onCreate() {

        super.onCreate();

        mContext = getApplicationContext();

        mWiFiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        if (checkWifiOnAndConnected(mWiFiManager) == false) {

            l("Enabling Wifi...");
            setupWifi();
        }


        l("BBService: onCreate");
        l("I am " + Build.MANUFACTURER + " / " + Build.MODEL + " / " + Build.SERIAL);


        // Start the RF Radio
        startServices();

        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }


        dlManager = new com.richardmcdougall.bbmonitor.DownloadManager(
                getApplicationContext().getFilesDir().getAbsolutePath(),
                boardId);

        dlManager.StartDownloads();

        HandlerThread mHandlerThread = null;
        mHandlerThread = new HandlerThread("BBServiceHandlerThread");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper());
    }


    /**
     * The service is starting, due to a call to startService()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        l("BBService: onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * A client is binding to the service with bindService()
     */
    @Override
    public IBinder onBind(Intent intent) {
        l("BBService: onBind");
        return mBinder;
    }

    /**
     * Called when all clients have unbound with unbindService()
     */
    @Override
    public boolean onUnbind(Intent intent) {
        l("BBService: onUnbind");
        return mAllowRebind;
    }

    /**
     * Called when a client is binding to the service with bindService()
     */
    @Override
    public void onRebind(Intent intent) {
        l("BBService: onRebind");

    }

    /**
     * Called when The service is no longer used and is being destroyed
     */
    @Override
    public void onDestroy() {

        l("BBService: onDesonDestroy");
    }



    private void startServices() {

        l("StartServices");

        mRadio = new RF(this, mContext);

        if (mRadio == null) {
            l("startServices: null RF object");
            return;
        }

        mFindMyFriends = new FindMyFriends(mContext, this, mRadio);

    }

    public FindMyFriends getFindMyFriends() {
        return mFindMyFriends;
    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }



    private void setupWifi() {
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
                return false; // Not connected to an access point
            }
            return true; // Connected to an access point
        }
        else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    private void checkWifiReconnect() {
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

}



