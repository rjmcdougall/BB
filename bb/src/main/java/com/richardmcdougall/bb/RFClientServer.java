package com.richardmcdougall.bb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by jonathan
 * Converted to use RF network by rmc on 4/12/18.
 */

public class RFClientServer {
    private String TAG = this.getClass().getSimpleName();

    private BBService service;
    public long tSentPackets = 0;
    private long replyCount = 0;
    static final int[] kServerBeaconMagicNumber = new int[]{0xbb, 0x05};
    static final int kThreadSleepTime = 5000;
    public static final int kRemoteAudio = 0;
    public static final int kRemoteVideo = 1;
    public static final int kRemoteMasterName = 2;
    private DriftCalculator driftCalculator = new DriftCalculator();

    // Use this to store the client packet the master needs to send to take over audio & video
    byte[][] kMasterToClientPacket = new byte[3][];

    // How long does the master need to keep communicating that it's the master (in milliseconds)?
    static final int kMasterBroadcastTime = 5 * 60 * 1000;

    // How many iterations are LEFT for the master to tell the client? Initialize at 0 and have
    // the value set when a master command is issued
    int kMasterBroadcastsLeft = 0;

    private PipedInputStream mReceivedPacketInput;
    private PipedOutputStream mReceivedPacketOutput;
    long mDrift;
    long mRtt;
    DriftCalculator.Sample mLastSample = null;
    SharedPreferences.Editor mPrefsEditor;
    private long mLatency = 150;
    private DatagramSocket mUDPSocket;

    private void setupUDPLogger() {
        if (DebugConfigs.DEBUG_RF_CLIENT_SERVER) {
            // InetAddress.getByName("0.0.0.0")
            try {
                mUDPSocket = new DatagramSocket(9999, InetAddress.getByName("0.0.0.0"));
            } catch (Exception e) {
                BLog.e(TAG, "Cannot setup UDP logger socket");
            }
        }
    }

    public void logUDP(long timestamp, String msg) {

        if (DebugConfigs.DEBUG_RF_CLIENT_SERVER) {
            ByteArrayOutputStream logPacketTmp = new ByteArrayOutputStream();

            RFUtil.stringToPacket(logPacketTmp, String.valueOf(timestamp));
            RFUtil.stringToPacket(logPacketTmp, msg);

            final byte[] logPacket = logPacketTmp.toByteArray();

            service.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        DatagramPacket dp = new DatagramPacket(logPacket, logPacket.length, InetAddress.getByName("10.0.6.255"), 9999);
                        mUDPSocket.send(dp);
                    } catch (Exception e) {
                        BLog.e(TAG, "UDP Logger Socket send failed:" + e.toString());
                    }
                }
            });
        }
    }

    RFClientServer(BBService service) {

        this.service = service;

        // Make a pipe for packet receive
        try {
            mReceivedPacketInput = new PipedInputStream();
            mReceivedPacketOutput = new PipedOutputStream(mReceivedPacketInput);
        } catch (Exception e) {
            BLog.e(TAG, "Receiver pipe failed: " + e.getMessage());
        }

        try {
            // Register for the particular broadcast based on Graphics Action
            IntentFilter packetFilter = new IntentFilter(ACTION.BB_PACKET);
            LocalBroadcastManager.getInstance(this.service).registerReceiver(RFReceiver, packetFilter);
        } catch (Exception e) {

        }
        setupUDPLogger();
    
    }

    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("BB RF Client/Server");
                RunBroadcastLoop();
            }
        });
        t.start();
    }

    // Send time-sync reply to specific client
    void ProcessServerReply(byte[] packet, int toClient, long clientTimestamp, long curTimeStamp) {

        BLog.d(TAG, "Server reply : " +
                service.allBoards.boardAddressToName(service.serverElector.serverAddress) + "(" + service.serverElector.serverAddress + ")" +
                " -> " + service.allBoards.boardAddressToName(toClient) + "(" + toClient + ")");

        ByteArrayOutputStream replyPacket = new ByteArrayOutputStream();

        for (int i = 0; i < RFUtil.kMagicNumberLen; i++) {
            replyPacket.write(RFUtil.kServerSyncMagicNumber[i]);
        }

        // Address of this server (just put this in now?)
        RFUtil.int16ToPacket(replyPacket, service.boardState.address);

        // Address this packet is for
        RFUtil.int16ToPacket(replyPacket, toClient);

        // send the client's timestamp back along with ours so it can figure out how to adjust it's clock
        RFUtil.int64ToPacket(replyPacket, clientTimestamp);
        RFUtil.int64ToPacket(replyPacket, curTimeStamp);

        // Broadcast - client will filter for it's address
        service.radio.broadcast(replyPacket.toByteArray());
        tSentPackets++;
        replyCount++;
    }

    // Define the callback for what to do when stats are received
    private BroadcastReceiver RFReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int sigStrength = intent.getIntExtra("sigStrength", 0);
            byte[] packet = intent.getByteArrayExtra("packet").clone();
            processReceive(packet, sigStrength);
        }
    };

    void processReceive(byte[] packet, int sigstrength) {
        ByteArrayInputStream bytes = new ByteArrayInputStream(packet);

        int recvMagicNumber = RFUtil.magicNumberToInt(new int[]{bytes.read(), bytes.read()});

        int clientAddress = 0;

        //if you requested a time sync from the server, you will receive this packet.
        // there you will adjust your time to match the servers time.
       if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kServerSyncMagicNumber)) {
            int serverAddress = (int) RFUtil.int16FromPacket(bytes);
            clientAddress = (int) RFUtil.int16FromPacket(bytes);

            if (clientAddress == service.boardState.address) {
                BLog.d(TAG, "BB Sync Packet from Server: len(" + packet.length + "), data: " + RFUtil.bytesToHex(packet));
                BLog.d(TAG, "BB Sync Packet from Server " + serverAddress +  " (" + service.allBoards.boardAddressToName(serverAddress) + ")");
                // Send to client loop to process the server's response
                ProcessTimeFromServer(packet);
            }
            // Try to re-elect server based on the heard board
            service.serverElector.tryElectServer(serverAddress, sigstrength);
        } 

        // if you receive a client sync request AND YOU are the server you need to reply back
        // and tell the client what the correct time offset is.
        else if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kClientSyncMagicNumber)) {
            clientAddress = (int) RFUtil.int16FromPacket(bytes);

            BLog.d(TAG, "BB Sync Packet from Client: len(" + packet.length + "), data: " + RFUtil.bytesToHex(packet));
            BLog.d(TAG, "BB Sync Packet from Client " + clientAddress +  " (" + service.allBoards.boardAddressToName(clientAddress) + ")");
            long clientTimestamp = RFUtil.int64FromPacket(bytes);
            long curTimeStamp = TimeSync.GetCurrentClock();
            mLatency = RFUtil.int64FromPacket(bytes);
            // Try to re-elect server based on the heard board
           service.serverElector.tryElectServer(clientAddress, sigstrength);
            if (service.serverElector.amServer()) {
                // Send response back to client
                ProcessServerReply(packet, clientAddress, clientTimestamp, curTimeStamp);
            }
            logUDP(curTimeStamp, "Server: curTimeStamp: " + curTimeStamp);

        } 

        // receive a beacon from a server, nothing to do except increment your elections
        // this is because you dont want the server to adjust its own time, but you want
        // to keep including it in the algorithm.
        else if (recvMagicNumber == RFUtil.magicNumberToInt(kServerBeaconMagicNumber)) {
            int serverAddress = (int) RFUtil.int16FromPacket(bytes);
            BLog.d(TAG, "BB Server Beacon packet: len(" + packet.length + "), data: " + RFUtil.bytesToHex(packet));
            BLog.d(TAG, "BB Server Beacon packet from Server " + serverAddress +  " (" + service.allBoards.boardAddressToName(serverAddress) + ")");
            // Try to re-elect server based on the heard board
           service.serverElector.tryElectServer(serverAddress, sigstrength);

        }        
        
        //master is configured in the app. If you receive a master command, you must do it.
        //this is not related to the others in this method.
        else if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kRemoteControlMagicNumber)) {
            int address = (int) RFUtil.int16FromPacket(bytes);
            int cmd = (int) RFUtil.int16FromPacket(bytes);
            int value = (int) RFUtil.int32FromPacket(bytes);
            BLog.d(TAG, "Received Remote Control " + cmd + ", " + value + " from " + address);
            receiveRemoteControl(address, cmd, value);
        } else {
            BLog.d(TAG, "packet not for sync server!");
        }
        return;
    }

    private void ProcessTimeFromServer(byte[] recvPacket) {

        BLog.d(TAG, "BB Sync Packet receive from server len (" + recvPacket.length + ") " +
                service.allBoards.boardAddressToName(service.serverElector.serverAddress) + "(" + service.serverElector.serverAddress + ")" +
                " -> " + service.allBoards.boardAddressToName(service.boardState.address) + "(" + service.boardState.address + ")");
        ByteArrayInputStream packet = new ByteArrayInputStream(recvPacket);

        long myTimeStamp = RFUtil.int64FromPacket(packet);
        long svTimeStamp = RFUtil.int64FromPacket(packet);
        long curTime = TimeSync.GetCurrentClock();
        long adjDrift;
        long roundTripTime = (curTime - myTimeStamp);

        long driftAdjust = -00;

        if (roundTripTime < 300) {
            //if (svTimeStamp < myTimeStamp) {
            //    adjDrift = (curTime - myTimeStamp) / 2 + (svTimeStamp - myTimeStamp);
            //} else
            // adjDrift = mytime delta from server
            // 4156 - 2208
            adjDrift = (svTimeStamp - myTimeStamp) - (curTime - myTimeStamp) / 2;

            BLog.d(TAG, "Pre-calc Drift is " + (svTimeStamp - myTimeStamp) + " round trip = " + (curTime - myTimeStamp) + " adjDrift = " + adjDrift);

            driftCalculator.AddSample(adjDrift, roundTripTime);

            DriftCalculator.Sample s = driftCalculator.BestSample();
            mLatency = s.roundTripTime;

            replyCount++;
 
            BLog.d(TAG, "Final Drift=" + (s.drift + driftAdjust) + " RTT=" + s.roundTripTime);
 
            if (mLastSample == null || !s.equals(mLastSample)) {
                mPrefsEditor.putLong("drift", s.drift);
                mPrefsEditor.putLong("rtt", s.roundTripTime);
                mPrefsEditor.commit();
            }

            TimeSync.SetServerClockOffset(s.drift + driftAdjust, s.roundTripTime);
            logUDP(TimeSync.CurrentClockAdjusted(), "client: CurrentClockAdjusted: " + TimeSync.CurrentClockAdjusted());
        }
    }

    // Thread/ loop to send out requests
    void RunBroadcastLoop() {
        BLog.d(TAG, "Sync Thread Staring");
        SharedPreferences prefs = service.getSharedPreferences("driftInfo", service.MODE_PRIVATE);
        mDrift = prefs.getLong("drift", 0);
        mRtt = prefs.getLong("rtt", 100);
        TimeSync.SetServerClockOffset(mDrift, mRtt);

        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mPrefsEditor = service.getSharedPreferences("driftInfo", service.MODE_PRIVATE).edit();

        while (true) {

            /*
                MEDIA MASTER SECTION
             */

            // Do we need to tell nearby clients who the media master is still?
            if (kMasterBroadcastsLeft > 0) {
                BLog.d(TAG, "Resending master client packet. Iterations remaining: " + kMasterBroadcastsLeft);

                for (int i = 0; i < kMasterToClientPacket.length; i++) {

                    byte[] packet = kMasterToClientPacket[i];
                    if (packet != null && packet.length > 0) {
                        BLog.d(TAG, "Resending master client packet type: " + i);
                        service.radio.broadcast(packet);

                        // To make sure we don't have collissions with the following TIME broadcast, so
                        // sleep for a few ms
                        try {
                            Thread.sleep(50);
                        } catch (Exception e) {
                        }
                    }
                }

                kMasterBroadcastsLeft--;
            }

            /*
                TIME SYNC MASTER SECTION
             */

            // This section is for TIME syncing. NOT media syncing!!!
            if (service.serverElector.amServer() == false) {
                try {

                    BLog.d(TAG, "I'm a client " + service.allBoards.boardAddressToName(service.boardState.address) + "(" + service.boardState.address + ")");

                    ByteArrayOutputStream clientPacket = new ByteArrayOutputStream();

                    RFUtil.WriteMagicNumber(clientPacket,RFUtil.kClientSyncMagicNumber);

                    // My Client Address
                    RFUtil.int16ToPacket(clientPacket, service.boardState.address);
                    // My timeclock
                    RFUtil.int64ToPacket(clientPacket, TimeSync.GetCurrentClock());
                    // Send latency so server knows how much to delay sync'ed start
                    RFUtil.int64ToPacket(clientPacket, mLatency);
                    // Pad to balance send-receive round trip time for average calculation
                    RFUtil.int16ToPacket(clientPacket, 0);
                    BLog.d(TAG, "send packet " + RFUtil.bytesToHex(clientPacket.toByteArray()));
                    // Broadcast, but only server will pick up
                    service.radio.broadcast(clientPacket.toByteArray());
                    BLog.d(TAG, "BB Sync Packet broadcast to server, ts=" + String.format("0x%08X", TimeSync.GetCurrentClock()) +
                            service.allBoards.boardAddressToName(service.boardState.address) + "(" + service.boardState.address + ")" +
                            " -> " + service.allBoards.boardAddressToName(service.serverElector.serverAddress) + "(" + service.serverElector.serverAddress + ")");

                } catch (Throwable e) {
                    //l("Client UDP failed");
                }
            } else {
                BLog.d(TAG, "I'm a server: broadcast Server beacon");
                mRtt = 0;
                mDrift = 0;
                TimeSync.SetServerClockOffset(0, 0);

                ByteArrayOutputStream replyPacket = new ByteArrayOutputStream();

                RFUtil.WriteMagicNumber(replyPacket,RFUtil.kServerBeaconMagicNumber);

                // Address of this server (just put this in now?)
                replyPacket.write(service.boardState.address & 0xFF);
                replyPacket.write((service.boardState.address >> 8) & 0xFF);

                // Broadcast - client will filter for it's address
                service.radio.broadcast(replyPacket.toByteArray());
            }

            try {
                Thread.sleep(kThreadSleepTime);
            } catch (Exception e) {
            }
        }
    }

    public void sendRemote(int cmd, long value, int type) {

        if (service.radio == null) {
            return;
        }
        BLog.d(TAG, "Sending remote control command: " + cmd + ", " + value + ", " + type + ", " + service.boardState.address);

        ByteArrayOutputStream clientPacket = new ByteArrayOutputStream();

        RFUtil.WriteMagicNumber(clientPacket,RFUtil.kRemoteControlMagicNumber);

        // Client
        RFUtil.int16ToPacket(clientPacket, service.boardState.address);
        // Command
        RFUtil.int16ToPacket(clientPacket, cmd);
        // Value
        RFUtil.int32ToPacket(clientPacket, value);

        // Send 10 times now, and let the supervisor thread send it periodically still
        byte[] packet = clientPacket.toByteArray();

        for (int i = 0; i < 10; i++) {
            service.radio.broadcast(packet);
        }

        kMasterToClientPacket[type] = packet;

        // This is the amount of iterations LEFT of broadcasting the client packet.
        // When this routine is invoked again, it'll restart the iteration counter.
        kMasterBroadcastsLeft = kMasterBroadcastTime / kThreadSleepTime;
        BLog.d(TAG, "Master client packet will be sent this many more times: " + kMasterBroadcastsLeft);
    }

    // Method to abandon rebroadcasts. Needed if a new master shows up.
    public void disableMasterBroadcast() {
        kMasterBroadcastsLeft = 0;
    }

    // TODO: Put this back as a remote control packet
    public void receiveRemoteControl(int address, int cmd, long value) {
        //l( "Received command: " + cmd + ", " + value + ", " + address);

        String client = service.allBoards.boardAddressToName(address);
        decodeRemoteControl(client, cmd, value);
    }// TODO: Put this back as a remote control packet
    // Change value -> hash lookup
    public void decodeRemoteControl(String client, int cmd, long value) {

        if (service.boardState.blockMaster) {
            BLog.d(TAG, "BLOCKED remote cmd, value " + cmd + ", " + value + " from: " + client);
        } else {

            BLog.d(TAG, "Received remote cmd, value " + cmd + ", " + value + " from: " + client);

            switch (cmd) {
                case RFUtil.REMOTE_AUDIO_TRACK_CODE:
                    this.service.masterController.RemoteAudio(value);
                    break;
                case RFUtil.REMOTE_VIDEO_TRACK_CODE:
                    this.service.masterController.RemoteVideo(value);
                    break;
                case RFUtil.REMOTE_MUTE_CODE:
                    this.service.masterController.RemoteVolume(value);
                    break;
                case RFUtil.REMOTE_MASTER_NAME_CODE:
                    this.service.masterController.NameMaster(client);
                default:
                    break;
            }
        }
    }

    public long getLatency() {
        return mLatency / 2;
    }

}
