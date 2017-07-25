package com.richardmcdougall.bb;
import java.net.*;


import android.content.Intent;
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
import android.app.Activity;
import android.support.v4.content.LocalBroadcastManager;



/**
 * Created by Jonathan on 8/20/2016.
 */
public class UDPClientServer {
    private static final int UDP_SERVER_PORT = 8099;
    private static final int MAX_UDP_DATAGRAM_LEN = 1500;
    BBService mMain;
    public long tSentPackets = 0;
    private long replyCount = 0;
    private static final String TAG = "BB.UDPClientServer";


    /* class Record {
        long
    };

    class UDPAddress {
        InetAddress addr;
        ArrayList<>
    }; */

    public void l(String s) {

        Log.v(TAG, s);
        sendLogMsg(s);
    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(com.richardmcdougall.bb.BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mMain).sendBroadcast(in);
    }


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

    // TODO: Check if try can cause server loop to exit
    void ServerLoop() {
        String lText;
        byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
        DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
        DatagramSocket ds = null;

        l("Receiver running on IP Address : " + getIPAddress(true));


        DatagramSocket socket = null;
        try {
            //socket = new DatagramSocket(UDP_SERVER_PORT, InetAddress.getByName("192.168.49.1"));
            socket = new DatagramSocket(UDP_SERVER_PORT, InetAddress.getByName("0.0.0.0"));
            socket.setSoTimeout(500);

            socket.setSoTimeout(0);
            while (true) {
                if (amServer()) {
                    replyCount++;
                    Intent in = new Intent(BBService.ACTION_STATS);
                    in.putExtra("resultCode", Activity.RESULT_OK);
                    in.putExtra("msgType", 2);
                    // Put extras into the intent as usual
                    in.putExtra("stateReplies", replyCount);
                    in.putExtra("stateMsgWifi", "Server");

                    // Fire the broadcast with intent packaged
                    LocalBroadcastManager.getInstance(mMain).sendBroadcast(in);
                }
                socket.receive(dp);
                if (dp.getLength() >= 12 && (dp.getData()[0] == 'B' && dp.getData()[1] == 'B' && dp.getData()[2] == 'C' && dp.getData()[3] == 'L')) {
                    // send the client's timestamp back along with ours so it can figure out how to adjust it's clock
                    long clientTimestamp = bytesToLong(dp.getData(), 4);
                    long curTimeStamp = mMain.GetCurrentClock();
                    int boardMode = mMain.getCurrentBoardMode();
                    int boardVol = mMain.getCurrentBoardVol();

                    byte[] rp = new byte[23];
                    rp[0] = 'B';
                    rp[1] = 'B';
                    rp[2] = 'S';
                    rp[3] = 'V';
                    longToBytes(clientTimestamp, rp, 4);
                    longToBytes(curTimeStamp, rp, 12);
                    rp[20] = (byte) mMain.currentRadioStream;   // tell clients the current radio stream to play
                    rp[21] = (byte) boardMode; // tell clients which graphics mode to use
                    rp[22] = (byte) boardVol; // tell clients audio volume

                    DatagramPacket sendPacket = new DatagramPacket(rp, rp.length, dp.getAddress(), UDP_SERVER_PORT);
                    socket.send(sendPacket);
                    tSentPackets++;

                    l("UDP packet received from " + dp.getAddress().toString() + " drift=" + (clientTimestamp - curTimeStamp));
                } else {
                    l("*malformed* UDP packet received from " + dp.getAddress().toString());
                }

            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (socket != null)
                socket.close();
        }
    }




    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi =
                (WifiManager) mMain.getApplicationContext().getSystemService(mMain.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    InetAddress GetServerAddress() {
        try {
            return InetAddress.getByName("10.10.10.200");
        } catch (Exception e){
            return null;
        }
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
        l("Staring");
        SharedPreferences prefs = mMain.getSharedPreferences("driftInfo", mMain.MODE_PRIVATE);
        long drift = prefs.getLong("drift", 0);
        long rtt = prefs.getLong("rtt", 100);
        mMain.SetServerClockOffset(drift, rtt);

        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SharedPreferences.Editor editor = mMain.getSharedPreferences("driftInfo", mMain.MODE_PRIVATE).edit();

        DatagramSocket socket = null;

        DatagramPacket recvPacket = null;

        try {

            byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
            recvPacket = new DatagramPacket(lMsg, lMsg.length);
            // InetAddress.getByName("0.0.0.0")
            socket = new DatagramSocket(UDP_SERVER_PORT, InetAddress.getByName("0.0.0.0"));
            socket.setSoTimeout(500);
        } catch (Throwable e) {

            //if (mMain.mWifi.state == MyWifiDirect.StateType.STATE_TALKING_TO_SERVER)
            //    mMain.mWifi.SetState(MyWifiDirect.StateType.STATE_RESET, "Error: " + e.getMessage());

            if (socket != null) {
                socket.close();
            }
            l("Client loop crashed");

        }

        Sample lastSample = null;
        while (true) {

            if (amServer() == false) {
                try {

                    l("Client loop");

                    InetAddress addr = GetServerAddress();
                    if (addr != null) {


                        byte[] d = new byte[12];
                        d[0] = 'B';
                        d[1] = 'B';
                        d[2] = 'C';
                        d[3] = 'L';
                        longToBytes(mMain.GetCurrentClock(), d, 4);
                        DatagramPacket dp = new DatagramPacket(d, d.length, addr, UDP_SERVER_PORT);

                        l("UDP send ");
                        socket.send(dp);
                        l("Sent from " + getIPAddress(true) + " to " + addr);
                            l("UDP receive ");
                            socket.receive(recvPacket);
                            l("UDP receive done ");

                            if (recvPacket.getLength() >= 21) {
                                l("UDP packet received from " + recvPacket.getAddress().toString());
                                byte[] b = recvPacket.getData();
                                if (b[0] == 'B' && b[1] == 'B' && b[2] == 'S' && b[3] == 'V') {
                                    long myTimeStamp = bytesToLong(b, 4);
                                    long svTimeStamp = bytesToLong(b, 12);
                                    long curTime = mMain.GetCurrentClock();
                                    long adjDrift;
                                    long roundTripTime = (curTime - myTimeStamp);
                                    int serverStreamIndex = b[20];
                                    int boardMode = (int) b[21];
                                    int boardVol = (int) b[22];

                                    if (serverStreamIndex != mMain.currentRadioStream) {
                                        mMain.SetRadioStream(serverStreamIndex);
                                    }

                                    if (boardMode > 0 &&
                                            boardMode != mMain.getCurrentBoardMode()) {
                                        mMain.setMode(boardMode);
                                    }

                                    if (boardVol != mMain.getCurrentBoardVol()) {
                                        //System.out.println("UDP: set vol = " + boardVol);
                                        mMain.setBoardVolume(boardVol);
                                    }

                                    if (roundTripTime > 200) {      // probably we are a packet behind, try to read an extra packet
                                        socket.receive(recvPacket);
                                    } else if (roundTripTime < 40) {
                                        if (svTimeStamp < myTimeStamp) {
                                            adjDrift = (curTime - myTimeStamp) / 2 + (svTimeStamp - myTimeStamp);
                                        } else
                                            adjDrift = (svTimeStamp - myTimeStamp) - (curTime - myTimeStamp) / 2;

                                        l("Drift is " + (svTimeStamp - myTimeStamp) + " round trip = " + (curTime - myTimeStamp) + " adjDrift = " + adjDrift);

                                        AddSample(adjDrift, roundTripTime);

                                        Sample s = BestSample();

                                        replyCount++;
                                        Intent in = new Intent(BBService.ACTION_STATS);
                                        in.putExtra("resultCode", Activity.RESULT_OK);
                                        in.putExtra("msgType", 2);
                                        // Put extras into the intent as usual
                                        in.putExtra("stateReplies", replyCount);

                                        in.putExtra("stateMsgWifi", "Client");
                                        //mActivity.setStateMsgConn("Connected");

                                        // Fire the broadcast with intent packaged
                                        LocalBroadcastManager.getInstance(mMain).sendBroadcast(in);

                                        if (lastSample == null || !s.equals(lastSample)) {
                                            editor.putLong("drift", s.drift);
                                            editor.putLong("rtt", s.roundTripTime);
                                            editor.commit();
                                        }

                                        l("Drift=" + s.drift + " RTT=" + s.roundTripTime);


                                        mMain.SetServerClockOffset(s.drift, s.roundTripTime);
                                    }
                                }
                            }


                    }

                } catch(Throwable e) {
                    l("Client UDP failed");
                }
            } else{
                //if (!MyWifiDirect.isP2PIpAddressAvailable()) {
                //    System.out.println("Server IP not initialized yet");
                // }
                //else {
                socket.close();
                socket = null;
                l("Server loop");
                ServerLoop();
                //}
            }
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }

    }


    public static boolean amServer() {
        String addr = getIPAddress(true);
        if (addr.equals("10.10.10.200")) {
            return true;
        } else {
            return false;
        }
    }
}
