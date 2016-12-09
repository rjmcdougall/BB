package com.richardmcdougall.bb;
import java.net.*;


import android.provider.SyncStateContract;
import android.util.Log;
import java.io.*;
import java.net.*;
import java.util.*;
import android.os.*;
import android.net.wifi.*;
import android.net.*;
import android.content.SharedPreferences;

import android.net.wifi.p2p.*;



/**
 * Created by Jonathan on 8/20/2016.
 */
public class UDPClientServer {
    private static final int UDP_SERVER_PORT = 8099;
    private static final int MAX_UDP_DATAGRAM_LEN = 1500;
    BBService mMain;
    public long tSentPackets = 0;
    /* class Record {
        long
    };

    class UDPAddress {
        InetAddress addr;
        ArrayList<>
    }; */


    InetAddress serverAddress = null;

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }




    UDPClientServer(BBService mainActivity) {
        mMain = mainActivity;
    }

    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run()
            {
                Start();
            }
        });
        t.start();
    }

    public MyWifiDirect mNotifyClient = null;

    void UpdateServerAddress(String s, MyWifiDirect notifyClient) {
        try {
            serverAddress = InetAddress.getByName(s);
            mNotifyClient = notifyClient;
        } catch (Throwable e) {
            serverAddress = null;
            mNotifyClient= null;
        }
    }


    public static byte[] longToBytes(long l, byte [] result, int offset) {
        for (int i = 7; i >= 0; i--) {
            result[i+offset] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }


    public static long bytesToLong(byte[] b, int offset) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i + offset] & 0xFF);
        }
        return result;
    }


    void ServerLoop() {
        String lText;
        byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
        DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
        DatagramSocket ds = null;

        mMain.l("Receiver running on IP Address : " + getIPAddress(true));

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(UDP_SERVER_PORT, InetAddress.getByName("192.168.49.1"));
            socket.setSoTimeout(500);

            socket.setSoTimeout(0);
            while (true) {
                socket.receive(dp);
                if (dp.getLength()>=12 && (dp.getData()[0]=='B' && dp.getData()[1]=='B' && dp.getData()[2]=='C' && dp.getData()[3]=='L')) {
                    // send the client's timestamp back along with ours so it can figure out how to adjust it's clock
                    long clientTimestamp = bytesToLong(dp.getData(), 4);
                    long curTimeStamp = mMain.GetCurrentClock();

                    byte[] rp = new byte[21];
                    rp[0] = 'B';
                    rp[1] = 'B';
                    rp[2] = 'S';
                    rp[3] = 'V';
                    longToBytes(clientTimestamp, rp, 4);
                    longToBytes(curTimeStamp, rp, 12);
                    rp[20] = (byte)mMain.currentRadioStream;   // tell clients the current radio stream to play

                    DatagramPacket sendPacket = new DatagramPacket(rp, rp.length, dp.getAddress(), UDP_SERVER_PORT);
                    socket.send(sendPacket);
                    tSentPackets++;

                    System.out.println("UDP packet received from " + dp.getAddress().toString() + " drift=" + (clientTimestamp-curTimeStamp));
                }
                else
                    System.out.println("*malformed* UDP packet received from " + dp.getAddress().toString());

            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (socket!=null)
                socket.close();
        }
    }




    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) mMain.getSystemService(mMain.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    InetAddress GetServerAddress() {
        return serverAddress;
    }


    class Sample {
        long drift;
        long roundTripTime;
    }

    ArrayList<Sample> samples = new ArrayList<Sample>();

    void AddSample(long drift, long rtt) {
        Sample s = new Sample();
        s.drift = drift;
        s.roundTripTime = rtt;
        samples.add(samples.size(), s);
        if (samples.size()>100)
            samples.remove(0);
    }

    Sample BestSample() {
        long rtt = Long.MAX_VALUE;
        Sample ret = null;
        for (int i=0; i<samples.size(); i++) {
            if (samples.get(i).roundTripTime<rtt) {
                rtt = samples.get(i).roundTripTime;
                ret = samples.get(i);
            }
        }
        return ret;
    }


    void Start() {
        SharedPreferences prefs = mMain.getSharedPreferences("driftInfo", mMain.MODE_PRIVATE);
        long drift = prefs.getLong("drift", 0);
        long rtt = prefs.getLong("rtt", 100);
        mMain.SetServerClockOffset(drift, rtt);


        while (true) {
            try {
                while (MyWifiDirect.isP2PIpAddressAvailable() == false) {
                    Thread.sleep(1000);
                }
            } catch (Throwable e) {

            }
            RunThread();
        }

    }

    void RunThread() {
        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SharedPreferences.Editor editor = mMain.getSharedPreferences("driftInfo", mMain.MODE_PRIVATE).edit();

        DatagramSocket socket = null;
        try {

            byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
            DatagramPacket recvPacket = new DatagramPacket(lMsg, lMsg.length);
            // InetAddress.getByName("0.0.0.0")
            socket = new DatagramSocket(UDP_SERVER_PORT, InetAddress.getByName("0.0.0.0"));
            socket.setSoTimeout(500);

            Sample lastSample = null;
            while (true) {
                if (MyWifiDirect.amServer()==false) {

                    InetAddress addr = GetServerAddress();
                    if (addr != null) {


                        byte[] d = new byte[12];
                        d[0]='B';
                        d[1]='B';
                        d[2]='C';
                        d[3]='L';
                        longToBytes(mMain.GetCurrentClock(), d, 4);
                        DatagramPacket dp = new DatagramPacket(d, d.length, addr, UDP_SERVER_PORT);

                        socket.send(dp);
                        try {
                            socket.receive(recvPacket);

                            if (recvPacket.getLength()>=21) {
                                byte[] b = recvPacket.getData();
                                if (b[0]=='B' && b[1]=='B' && b[2]=='S' && b[3]=='V') {
                                    long myTimeStamp = bytesToLong(b, 4);
                                    long svTimeStamp = bytesToLong(b, 12);
                                    long curTime = mMain.GetCurrentClock();
                                    long adjDrift;
                                    long roundTripTime = (curTime - myTimeStamp);
                                    int serverStreamIndex = b[20];

                                    if (serverStreamIndex!=mMain.currentRadioStream) {
                                        mMain.SetRadioStream(serverStreamIndex);
                                    }

                                    if (roundTripTime > 200) {      // probably we are a packet behind, try to read an extra packet
                                        socket.receive(recvPacket);
                                    }
                                    else if (roundTripTime<40) {
                                        if (svTimeStamp < myTimeStamp) {
                                            adjDrift = (curTime - myTimeStamp) / 2 + (svTimeStamp - myTimeStamp);
                                        } else
                                            adjDrift = (svTimeStamp - myTimeStamp) - (curTime - myTimeStamp) / 2;

                                        //mMain.l("Drift is " + (svTimeStamp - myTimeStamp) + " round trip = " + (curTime - myTimeStamp) + " adjDrift = " + adjDrift);

                                        AddSample(adjDrift, roundTripTime);

                                        Sample s=BestSample();
                                        if (mNotifyClient!=null) {
                                            mNotifyClient.UdpReplySeen();
                                        }

                                        if (lastSample==null || !s.equals(lastSample)) {
                                            editor.putLong("drift", s.drift);
                                            editor.putLong("rtt", s.roundTripTime);
                                            editor.commit();
                                        }

                                        System.out.println("Drift=" + s.drift + " RTT=" + s.roundTripTime);


                                        mMain.SetServerClockOffset(s.drift, s.roundTripTime);
                                    }
                                }
                            }

                        } catch (SocketTimeoutException e) {
                            System.out.println("receive timeout");
                        }

                        //mMain.l("Sent from " + getIPAddress(true) + " to " + saddr);
                    }
                } else {
                    if (!MyWifiDirect.isP2PIpAddressAvailable()) {
                        System.out.println("Server IP not initialized yet");
                    }
                    else {
                         socket.close();
                        socket = null;
                        ServerLoop();
                    }
                }
                Thread.sleep(500);
            }
        } catch (Throwable e) {
            if (mMain.mWifi.state==MyWifiDirect.StateType.STATE_TALKING_TO_SERVER)
                mMain.mWifi.SetState(MyWifiDirect.StateType.STATE_RESET, "Error: " + e.getMessage());

            if (socket!=null)
                socket.close();


        }
    }
}
