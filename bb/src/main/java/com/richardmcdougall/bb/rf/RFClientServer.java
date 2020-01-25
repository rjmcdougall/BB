package com.richardmcdougall.bb.rf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;

import com.richardmcdougall.bb.ACTION;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.BLog;
import com.richardmcdougall.bb.TimeSync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by jonathan
 * Converted to use RF network by rmc on 4/12/18.
 */

public class RFClientServer {
    private String TAG = this.getClass().getSimpleName();

    private BBService service;
    public long tSentPackets = 0;
    private long replyCount = 0;
    static final int kThreadSleepTime = 5000;
    private DriftCalculator driftCalculator = new DriftCalculator();

    long mDrift;
    long mRtt;
    private long mLatency = 150;

    public RFClientServer(BBService service) {

        this.service = service;

        try {
            IntentFilter packetFilter = new IntentFilter(ACTION.BB_PACKET);
            LocalBroadcastManager.getInstance(this.service).registerReceiver(RFReceiver, packetFilter);
        } catch (Exception e) {
        }
    }

    public void Run() {
        Thread t = new Thread(() -> {
            Thread.currentThread().setName("BB RF Client/Server");
            RunBroadcastLoop();
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

           long clientTimestamp = RFUtil.int64FromPacket(bytes);
           long curTimeStamp = TimeSync.GetCurrentClock();
           mLatency = RFUtil.int64FromPacket(bytes);
           
            BLog.d(TAG, "Sync Packet from Client " + service.allBoards.boardAddressToName(clientAddress) +
                    " client timestamp: " + clientTimestamp +
                    " my timestamp: " + curTimeStamp +
                    " latency: " + mLatency);
            
            // Try to re-elect server based on the heard board
           service.serverElector.tryElectServer(clientAddress, sigstrength);
            if (service.serverElector.amServer()) {
                // Send response back to client
                ProcessServerReply(packet, clientAddress, clientTimestamp, curTimeStamp);
            }
        } 

        // receive a beacon from a server, nothing to do except increment your elections
        // this is because you dont want the server to adjust its own time, but you want
        // to keep including it in the algorithm.
        else if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kServerBeaconMagicNumber)) {
            int serverAddress = (int) RFUtil.int16FromPacket(bytes);
            
            BLog.d(TAG, "BB Server Beacon packet from Server " + serverAddress +  " (" + service.allBoards.boardAddressToName(serverAddress) + ")");
            // Try to re-elect server based on the heard board
           service.serverElector.tryElectServer(serverAddress, sigstrength);
        }
    }

    private void ProcessTimeFromServer(byte[] recvPacket) {

        BLog.d(TAG, "BB Sync Packet receive from server len (" + recvPacket.length + ") " +
                service.allBoards.boardAddressToName(service.serverElector.serverAddress) + "(" + service.serverElector.serverAddress + ")" +
                " -> " + service.allBoards.boardAddressToName(service.boardState.address) + "(" + service.boardState.address + ")");
        ByteArrayInputStream packet = new ByteArrayInputStream(recvPacket);

        long packetHeader = RFUtil.int16FromPacket(packet);
        long clientAddress = RFUtil.int16FromPacket(packet);
        long serverAddress = RFUtil.int16FromPacket(packet);
        long myTimeStamp = RFUtil.int64FromPacket(packet);
        long svTimeStamp = RFUtil.int64FromPacket(packet);
        long curTime = TimeSync.GetCurrentClock();
        long adjDrift;
        long roundTripTime = (curTime - myTimeStamp);

        if (roundTripTime < 300) {

            //waht is the difference between my time i sent the server and its time?

            adjDrift = (svTimeStamp - myTimeStamp) - roundTripTime / 2;

            BLog.d(TAG, "Pre-calc Drift is " + (svTimeStamp - myTimeStamp) + " round trip = " + (curTime - myTimeStamp) + " adjDrift = " + adjDrift);

            driftCalculator.AddSample(adjDrift, roundTripTime);

            DriftCalculator.Sample s = driftCalculator.BestSample();
            mLatency = s.roundTripTime;

            replyCount++;
 
            BLog.d(TAG, "Final Drift=" + (s.drift) + " RTT=" + s.roundTripTime);

            TimeSync.SetServerClockOffset(s.drift, s.roundTripTime);
        }
    }

    // Thread/ loop to send out requests
    void RunBroadcastLoop() {
        BLog.d(TAG, "Sync Thread Staring");

        mDrift = 0;
        mRtt = 100;

        TimeSync.SetServerClockOffset(mDrift, mRtt);

        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        while (true) {

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

                    if(service.serverElector.serverAddress != 0)
                        BLog.d(TAG, "BB Sync Packet broadcast to server, ts=" + String.format("0x%08X", TimeSync.GetCurrentClock()) +
                                service.allBoards.boardAddressToName(service.boardState.address) + "(" + service.boardState.address + ")" +
                                " -> " + service.allBoards.boardAddressToName(service.serverElector.serverAddress) + "(" + service.serverElector.serverAddress + ")");
                    else
                        BLog.d(TAG, "BB Sync Packet broadcast to server, ts=" + String.format("0x%08X", TimeSync.GetCurrentClock()) +
                                service.allBoards.boardAddressToName(service.boardState.address) + "(" + service.boardState.address + ")" +
                                " -> No Server");


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

    public long getLatency()
    {
        return mLatency / 2;
    }

}
