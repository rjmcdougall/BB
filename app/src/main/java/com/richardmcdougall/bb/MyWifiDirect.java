package com.richardmcdougall.bb;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.wifi.WifiManager;
import android.content.*;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.*;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.content.IntentFilter;

import java.net.InetAddress;
import java.util.*;
import android.util.*;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import java.lang.reflect.*;
import java.net.NetworkInterface;

import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;

/**
 * Created by Jonathan on 8/21/2016.
 */
public class MyWifiDirect {
    public MainActivity mActivity;
    public WifiP2pManager mManager;

    WiFiDirectBroadcastReceiver mWiFiDirectBroadcastReceiver = null;
    public Channel channel;
    private final IntentFilter intentFilter = new IntentFilter();
    UDPClientServer mClientServer;

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_wifidemotest";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static String myDeviceName = null;
    public static String myDeviceMAC = null;
    WifiP2pGroup mGroup = null;
    Timer timer = null;
    public boolean isConnectedToP2PNetwork = false;
    long startStartTime = 0;

    public List peers = new ArrayList();

    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    public enum StateType {

        STATE_WAITING_MY_NAME,
        STATE_RESET,
        STATE_CREATING_GROUP,
        STATE_GET_GROUP_INFO,
        STATE_CREATE_GROUP,
        STATE_WAITING_GROUP,
        STATE_START_FIND_PEERS,
        STATE_FINDING_PEERS,
        STATE_START_CONNECT,
        STATE_CONNECTING,
        STATE_WAITING_UDP_REPLY,
        STATE_TALKING_TO_SERVER,
        STATE_SERVING     // state only for server
    };
    StateType state = StateType.STATE_WAITING_MY_NAME;

    boolean isConnectedState() {
        return state==StateType.STATE_WAITING_UDP_REPLY || state==StateType.STATE_TALKING_TO_SERVER;
    }

    void SetState(StateType s, String why) {
        StateType oldState = state;
        state = s;
        if (state != StateType.STATE_TALKING_TO_SERVER)
            replyCount=0;

        startStartTime = mActivity.GetCurrentClock();

        synchronized (mActivity.bleStatus) {
            String ip = UDPClientServer.getIPAddress(true);
            mActivity.bleStatus = "IP="+ip+" "+myDeviceName+"\n"+oldState.toString() + " -> " + state.toString() + ":" + why;
            System.out.println(mActivity.bleStatus);
        }

    }

    public static boolean isServer(String deviceName) {
        return deviceName.equals("blu2");
    }

    public static boolean amServer() {
        if (myDeviceName==null)
            return false;
        return isServer(myDeviceName);
    }

    public void NextState() {

/*        Timer t = new Timer(false);

        t.schedule(new TimerTask() {
            @Override
            public void run() {
                Poll();
            }
        }, 250); */
    }


    private GroupInfoListener groupListener = new GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable (WifiP2pGroup group) {


            if (group==null) {
                //if (amServer())
                    SetState(StateType.STATE_CREATE_GROUP, "onGroupInfoAvailable group=null");
                //else
//                    SetState(StateType.STATE_GET_GROUP_INFO, "onGroupInfoAvailable group=null");
            }
            else {
                mGroup = group;
                SetState(StateType.STATE_START_FIND_PEERS, "Group found " + group.getNetworkName());
                if (amServer())
                    SetState(StateType.STATE_START_CONNECT, "onGroupInfoAvailable group=null");
            }
            NextState();
        }

    };

    private ConnectionInfoListener connectListener = new ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
        /*    if (isConnectedState() && info.groupFormed == false) {
                SetState(StateType.STATE_RESET, "onConnectionInfoAvailable reports groupFormed == false");
                /NextState();
            } */
            mActivity.l("onConnectionInfoAvailable: " + info.toString());
        }
    };


    void RequestGroups() {
        mGroup = null;
        SetState(StateType.STATE_WAITING_GROUP, "requestGroupInfo called");
        mManager.requestGroupInfo(channel, groupListener);
    }

    void CreateGroup() {
        if (isServer(myDeviceName)) {
            SetState(StateType.STATE_CREATING_GROUP, " myDeviceName=blu2");

            mManager.createGroup(channel, new ActionListener() {
                @Override
                public void onSuccess() {
                    if (isServer(myDeviceName))
                        SetState(StateType.STATE_SERVING, "createGroup->onSuccess");
                    else
                        SetState(StateType.STATE_GET_GROUP_INFO, "createGroup->onSuccess");
                }

                @Override
                public void onFailure(int reason) {
                    if (reason == WifiP2pManager.BUSY)
                        SetState(StateType.STATE_RESET, "onFailure->BUSY");
                    else // try to create group again?
                        SetState(StateType.STATE_RESET, "createGroup->onFailure " + reason);
                }
            });
            NextState();
        }
        else {
            mManager.removeGroup(channel, null);
            SetState(StateType.STATE_START_FIND_PEERS, " skipping createGroup for non-server");
            NextState();
        }
    }

    public int failedConnects=0;
    void ConnectToGroup(WifiP2pGroup group) {
        if (isServer(myDeviceName)) {
            SetState(StateType.STATE_SERVING, "ConnectToGroup: I'm the server");
            NextState();
        }
        else  {
            int serverIndex = -1;
            for (int i=0; i<peers.size(); i++) {
                WifiP2pDevice d = (WifiP2pDevice)peers.get(i);
                if (d.deviceName.equals("blu2"))
                    serverIndex = i;
            }

            if (serverIndex != -1) {
                WifiP2pDevice d = (WifiP2pDevice) peers.get(serverIndex);

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = d.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                final MyWifiDirect me = this;

                WifiP2pManager.ActionListener listener = new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        SetState(StateType.STATE_WAITING_UDP_REPLY, "connect.onSuccess");
                        //mManager.requestConnectionInfo(channel, connectListener);
                        //mManager.requestGroupInfo(channel, groupListener);


                        failedConnects=0;
                        boolean p2pOk = isP2PIpAddressAvailable();
                        if (!p2pOk) {
                            SetState(StateType.STATE_GET_GROUP_INFO,
                                    "connect.onSuccess but no IP Address ");
                        } else {
                            mClientServer.UpdateServerAddress("192.168.49.1", me);
                        }
                        NextState();
                        //mActivity.l("Connect: success");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        failedConnects++;
                        if (failedConnects>10) {
                            failedConnects = 0;
                            SetState(StateType.STATE_RESET,
                                    "connect.onFailure more than 10 times " + reasonCode);
                        }
                        else
                            SetState(StateType.STATE_GET_GROUP_INFO,
                                    "connect.onFailure " + reasonCode);
                        NextState();
                        //mActivity.l("Connect: failed with code " + reasonCode);
                    }
                };

                SetState(StateType.STATE_CONNECTING, "connect issued");
                mManager.connect(channel, config, listener);
                NextState();
            }
            else {
                SetState(StateType.STATE_GET_GROUP_INFO, "ConnectToGroup - no server found");
                NextState();
            }
        }
    }

    private void Reset() {
        onPause();
        onResume();
        // Call WifiP2pManager.requestPeers() to get a list of current peers
        mManager.requestConnectionInfo(channel, connectListener);
    }

    long replyCount = 0;
    public void UdpReplySeen() {
        replyCount++;

        SetState(StateType.STATE_TALKING_TO_SERVER, "UDP replies: " + replyCount);
    }

    private void Poll() {

        switch (state) {
            case STATE_WAITING_MY_NAME :
                if (myDeviceName!=null) {
                    SetState(StateType.STATE_RESET, "myDeviceName!=null");
                    NextState();
                }

                break;
            case STATE_RESET: {
//                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 200);
                Reset();
                if  (myDeviceName != null) {
                    SetState(StateType.STATE_GET_GROUP_INFO, "MyName!=null");
                    NextState();
                }
            } break;
            case STATE_START_FIND_PEERS: {
/*                if (mActivity.GetCurrentClock()-lastReplyTime>10*1000)
                    SetState(StateType.STATE_RESET, "requestPeers called");
                else */ {
                    SetState(StateType.STATE_FINDING_PEERS, "requestPeers called");
                    mManager.requestPeers(channel, peerListListener);
                }
                NextState();
            } break;


            case STATE_GET_GROUP_INFO : {
                RequestGroups();
            } break;
            case STATE_START_CONNECT : {
                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 200);
                ConnectToGroup(mGroup);
            } break;
            case STATE_CREATE_GROUP : {
                CreateGroup();
            } break;
            case STATE_WAITING_UDP_REPLY: {
                if (mActivity.GetCurrentClock()-startStartTime > 5000) {
                    SetState(StateType.STATE_RESET, "No udp reply seen after 5 seconds");
                }
            } break;
        }
    }


    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections
                    .list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName))
                        continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null)
                    return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0)
                    buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
        /*
         * try { // this is so Linux hack return
         * loadFileAsString("/sys/class/net/" +interfaceName +
         * "/address").toUpperCase().trim(); } catch (IOException ex) { return
         * null; }
         */
    }

    public static boolean isP2PIpAddressAvailable() {
        try {
            List<NetworkInterface> interfaces = Collections
                    .list(NetworkInterface.getNetworkInterfaces());
        /*
         * for (NetworkInterface networkInterface : interfaces) { Log.v(TAG,
         * "interface name " + networkInterface.getName() + "mac = " +
         * getMACAddress(networkInterface.getName())); }
         */

            for (NetworkInterface intf : interfaces) {
                if (!getMACAddress(intf.getName()).equalsIgnoreCase(
                        myDeviceMAC)) {
                    // Log.v(TAG, "ignore the interface " + intf.getName());
                    // continue;
                }
                if (!intf.getName().contains("p2p"))
                    continue;


                List<InetAddress> addrs = Collections.list(intf
                        .getInetAddresses());
                return addrs.size()>0;

/*
                for (InetAddress addr : addrs) {
                    // Log.v(TAG, "inside");

                    if (!addr.isLoopbackAddress()) {
                        // Log.v(TAG, "isnt loopback");
                        String sAddr = addr.getHostAddress().toUpperCase();



                        if (sAddr.contains("192.168.49.")) {
                            return sAddr;
                        }

                    }

                } */
            }

        } catch (Exception ex) {

        } // for now eat exceptions

        return false;
    }



    private PeerListListener peerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (peerList.getDeviceList().size()==0) {
                //SetState(StateType.STATE_GET_GROUP_INFO, "no peers found");
                if (!amServer())
                    SetState(StateType.STATE_RESET, "no peers found");
                else
                    SetState(StateType.STATE_GET_GROUP_INFO, "no peers found");
                return ;
            }

            synchronized (peers) {


                // Out with the old, in with the new.
                peers.clear();
                peers.addAll(peerList.getDeviceList());


                if (state != StateType.STATE_START_CONNECT && state != StateType.STATE_CONNECTING)
                    SetState(StateType.STATE_START_CONNECT, "peersAvailable " + peers.size());

                // If an AdapterView is backed by this data, notify it
                // of the change.  For instance, if you have a ListView of available
                // peers, trigger an update.

                if (peers.size() == 0) {
                    mActivity.l("No devices found");
                    NextState();
                    return;
                }

                /*
                synchronized (mActivity.bleStatus) {
                    mActivity.bleStatus = "";
                    for (int i = 0; i < peers.size(); i++) {
                        //mActivity.l("Dev" + i + " " + peers.get(i).toString());
                        WifiP2pDevice d = (WifiP2pDevice) peers.get(i);
                        String me = "";
                        if (d.deviceAddress == myDeviceMAC)
                            me = "(me)";

                        mActivity.bleStatus += d.deviceName + " " + d.deviceAddress + me + "\n";
                        mActivity.l(null);

                    }
                } */

                NextState();
            }

        }
    };


    public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

        private WifiP2pManager mManager;
        private Channel mChannel;
        private MyWifiDirect mMyWifiDirect;

        public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                           MyWifiDirect _mMyWifiDirect) {
            super();
            this.mManager = manager;
            this.mChannel = channel;
            this.mMyWifiDirect = _mMyWifiDirect;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            WifiP2pGroup groupInfo = (WifiP2pGroup) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);


            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                mActivity.l(action.toString());

            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (amServer()==false && isConnectedState()) {
                    //SetState(StateType.STATE_START_CONNECT, "WIFI_P2P_PEERS_CHANGED_ACTION and group already found");
                } else if (mGroup == null) {
                    if (state!=StateType.STATE_GET_GROUP_INFO)
                        SetState(StateType.STATE_GET_GROUP_INFO, "WIFI_P2P_PEERS_CHANGED_ACTION");
                }
                else
                    SetState(StateType.STATE_START_FIND_PEERS, "WIFI_P2P_PEERS_CHANGED_ACTION");
                NextState();
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                // Respond to new connection or disconnections
                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                String log = networkInfo.getDetailedState().toString();
                if (log.equals("CONNECTED")) {
                    isConnectedToP2PNetwork = true;
                    /* if (isP2PIpAddressAvailable() && peers.size()!=0)
                        SetState(StateType.STATE_TALKING_TO_SERVER, "Connected to p2p network"); */
                }
                if (log.equals("DISCONNECTED")) {
                    if (isConnectedToP2PNetwork) {
                        SetState(StateType.STATE_RESET, "Disconnect from p2p network");
                        isConnectedToP2PNetwork = false;
                    }
                }

                mActivity.l(log);

                if (log.equals("FAILED"))
                    SetState(StateType.STATE_GET_GROUP_INFO, "WIFI_P2P_CONNECTION_CHANGED_ACTION");


                mActivity.l(action.toString());
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                myDeviceName = device.deviceName;
                myDeviceMAC = device.deviceAddress;
                mActivity.l(action.toString());
            }
        }
    }
    private void appendStatus(String s) {

    }


    MyWifiDirect(MainActivity activity, UDPClientServer cs) {
        mClientServer = cs;
        mActivity = activity;
        mManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);

        channel = mManager.initialize(activity, activity.getMainLooper(), null);

        try {
            Method[] m = mManager.getClass().getMethods();
            for (int i=0; i<m.length;i++) {
                mActivity.l(m[i].toString());
            }
            //Method method1 = mManager.getClass().getMethod("enableP2p", Channel.class);
            //method1.invoke(mManager, channel);
        } catch (Throwable e)
        {

        }




        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        onResume();
    }


    public void onResume() {
        if (mWiFiDirectBroadcastReceiver!=null)
            onPause();

        mWiFiDirectBroadcastReceiver = new WiFiDirectBroadcastReceiver(mManager, channel, this);
        mActivity.registerReceiver(mWiFiDirectBroadcastReceiver, intentFilter);
        WifiP2pManager.ActionListener listener = new  WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mActivity.l("DiscoverPeers: success");
            }

            @Override
            public void onFailure(int reasonCode) {
                mActivity.l("DiscoverPeers: failed with code " + reasonCode);
            }
        };

        mManager.discoverPeers(channel, listener);

        timer = new Timer(false);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Poll();
            }
        }, 2000, 2000);

    }

    public void onPause() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (mWiFiDirectBroadcastReceiver!=null) {
            mActivity.unregisterReceiver(mWiFiDirectBroadcastReceiver);
            mWiFiDirectBroadcastReceiver = null;
        }
    }


}
