package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jonathan
 * Converted to use RF network by rmc on 4/12/18.
 */

public class RFClientServer {
    private BBService service;
    public long tSentPackets = 0;
    private long replyCount = 0;
    private static final String TAG = "BB.RFClientServer";
    static final int [] kServerBeaconMagicNumber = new int[] {0xbb, 0x05};
    static final int kThreadSleepTime = 5000;
    public static final int kRemoteAudio = 0;
    public static final int kRemoteVideo = 1;
    public static final int kRemoteMasterName = 2;

    // Use this to store the client packet the master needs to send to take over audio & video
    byte[][] kMasterToClientPacket = new byte[3][];

    // How long does the master need to keep communicating that it's the master (in milliseconds)?
    static final int kMasterBroadcastTime = 5 * 60 * 1000;

    // How many iterations are LEFT for the master to tell the client? Initialize at 0 and have
    // the value set when a master command is issued
    int kMasterBroadcastsLeft = 0;

    private int mServerAddress = 0;
    private int mBoardAddress;
    private PipedInputStream mReceivedPacketInput;
    private PipedOutputStream mReceivedPacketOutput;
    long mDrift;
    long mRtt;
    Sample mLastSample = null;
    SharedPreferences.Editor mPrefsEditor;
    private long mLatency = 150;
    private DatagramSocket mUDPSocket;

    public void l(String s) {

        Log.v(TAG, s);
        service.sendLogMsg(s);
    }

    public void d(String s) {
        if (DebugConfigs.DEBUG_RF_CLIENT_SERVER) {
            Log.v(TAG, s);
            service.sendLogMsg(s);
        }
    }

    private void setupUDPLogger(){
        if (DebugConfigs.DEBUG_RF_CLIENT_SERVER) {
            // InetAddress.getByName("0.0.0.0")
            try {
                mUDPSocket = new DatagramSocket(9999, InetAddress.getByName("0.0.0.0"));
            } catch (Exception e) {
                l("Cannot setup UDP logger socket");
            }
        }
    }

    public void logUDP(long timestamp, String msg) {

        if (DebugConfigs.DEBUG_RF_CLIENT_SERVER) {
            ByteArrayOutputStream logPacketTmp = new ByteArrayOutputStream();

            stringToPacket(logPacketTmp, String.valueOf(timestamp));
            stringToPacket(logPacketTmp, msg);

            final byte[] logPacket = logPacketTmp.toByteArray();

            service.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        DatagramPacket dp = new DatagramPacket(logPacket, logPacket.length, InetAddress.getByName("10.0.6.255"), 9999);
                        mUDPSocket.send(dp);
                    } catch (Exception e) {
                        l("UDP Logger Socket send failed:" + e.toString());
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
            l("Receiver pipe failed: " + e.getMessage());
        }

        try {
            // Register for the particular broadcast based on Graphics Action
            IntentFilter packetFilter = new IntentFilter(ACTION.BB_PACKET);
            LocalBroadcastManager.getInstance(this.service).registerReceiver(RFReceiver, packetFilter);
        } catch (Exception e) {

        }
        setupUDPLogger();
        mLastVote = SystemClock.elapsedRealtime();
    }

    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run()
            {
                Thread.currentThread().setName("BB RF Client/Server");
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

    private long int64FromPacket(ByteArrayInputStream bytes) {
        return ((long) ((bytes.read() & (long)0xff) +
                ((bytes.read() & (long)0xff) << 8) +
                ((bytes.read() & (long)0xff) << 16) +
                ((bytes.read() & (long)0xff) << 24) +
                ((bytes.read() & (long)0xff) << 32) +
                ((bytes.read() & (long)0xff) << 40) +
                ((bytes.read() & (long)0xff) << 48) +
                ((bytes.read() & (long)0xff) << 56)));
    }

    private void int64ToPacket(ByteArrayOutputStream bytes, long n) {
        bytes.write((byte) (n & 0xFF));
        bytes.write((byte) ((n >> 8) & 0xFF));
        bytes.write((byte) ((n >> 16) & 0xFF));
        bytes.write((byte) ((n >> 24) & 0xFF));
        bytes.write((byte) ((n >> 32) & 0xFF));
        bytes.write((byte) ((n >> 40) & 0xFF));
        bytes.write((byte) ((n >> 48) & 0xFF));
        bytes.write((byte) ((n >> 56) & 0xFF));
    }

    private long int32FromPacket(ByteArrayInputStream bytes) {
        return ((long) ((bytes.read() & (long)0xff) +
                ((bytes.read() & (long)0xff) << 8) +
                ((bytes.read() & (long)0xff) << 16) +
                ((bytes.read() & (long)0xff) << 24)));
    }

    private void int32ToPacket(ByteArrayOutputStream bytes, long n) {
        bytes.write((byte) (n & 0xFF));
        bytes.write((byte) ((n >> 8) & 0xFF));
        bytes.write((byte) ((n >> 16) & 0xFF));
        bytes.write((byte) ((n >> 24) & 0xFF));
    }

    private void stringToPacket(ByteArrayOutputStream bytes, String s) {
        try {
            bytes.write(s.getBytes());
        } catch (Exception e) {
        }
    }

    private long int16FromPacket(ByteArrayInputStream bytes) {
        return ((long) ((bytes.read() & (long)0xff) +
                ((bytes.read() & (long)0xff) << 8)));
    }

    private void int16ToPacket(ByteArrayOutputStream bytes, int n) {
        bytes.write((byte) (n & 0xFF));
        bytes.write((byte) ((n >> 8) & 0xFF));
    }

    // Send time-sync reply to specific client
    void ServerReply(byte [] packet, int toClient, long clientTimestamp, long curTimeStamp) {

        if(mBoardAddress<=0)
            mBoardAddress = service.allBoards.getBoardAddress(service.boardState.BOARD_ID);
        l("I'm address " + mBoardAddress);

        d("Server reply : " +
                service.allBoards.boardAddressToName(mServerAddress) + "(" + mServerAddress + ")" +
                " -> " + service.allBoards.boardAddressToName(toClient) + "(" + toClient + ")");

        ByteArrayOutputStream replyPacket = new ByteArrayOutputStream();

        for (int i = 0; i < RFUtil.kMagicNumberLen; i++) {
            replyPacket.write(RFUtil.kServerSyncMagicNumber[i]);
        }

        // Address of this server (just put this in now?)
        int16ToPacket(replyPacket,mBoardAddress);

        // Address this packet is for
        int16ToPacket(replyPacket, toClient);

        // send the client's timestamp back along with ours so it can figure out how to adjust it's clock
        int64ToPacket(replyPacket, clientTimestamp);
        int64ToPacket(replyPacket, curTimeStamp);

        // Broadcast - client will filter for it's address
        service.radio.broadcast(replyPacket.toByteArray());
        tSentPackets++;
        replyCount++;

        Intent in = new Intent(ACTION.STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 2);
        // Put extras into the intent as usual
        in.putExtra("stateReplies", replyCount);
        in.putExtra("stateMsgWifi", "Server");
        LocalBroadcastManager.getInstance(service).sendBroadcast(in);
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


    // TODO: Decide if we use static assigned server as the time master
    // TODO: Decode client packet and send to client receive function
    // TODO: Decode server receive packet and send to server receive function
    void processReceive(byte [] packet, int sigstrength) {
        ByteArrayInputStream bytes = new ByteArrayInputStream(packet);

        if(mBoardAddress <= 0)
            mBoardAddress = service.allBoards.getBoardAddress(service.boardState.BOARD_ID);

        int recvMagicNumber = RFUtil.magicNumberToInt(
                new int[] { bytes.read(), bytes.read()});

        int clientAddress = 0;

        if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kServerSyncMagicNumber)) {
            int serverAddress = (int)int16FromPacket(bytes);
            clientAddress = (int)int16FromPacket(bytes);

            if (clientAddress == mBoardAddress) {
                d("BB Sync Packet from Server: len(" + packet.length + "), data: " + RFUtil.bytesToHex(packet));
                d("BB Sync Packet from Server " + serverAddress +
                        " (" + service.allBoards.boardAddressToName(serverAddress) + ")");
                // Send to client loop to process the server's response
                processSyncResponse(packet);
            }
            // Try to re-elect server based on the heard board
            tryElectServer(serverAddress, sigstrength);
        } else if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kClientSyncMagicNumber)) {
            clientAddress = (int)int16FromPacket(bytes);

            d("BB Sync Packet from Client: len(" + packet.length + "), data: " + RFUtil.bytesToHex(packet));
            d("BB Sync Packet from Client " + clientAddress +
                    " (" + service.allBoards.boardAddressToName(clientAddress) + ")");
            long clientTimestamp = int64FromPacket(bytes);
            long curTimeStamp = service.GetCurrentClock();
            mLatency = int64FromPacket(bytes);
            // Try to re-elect server based on the heard board
            tryElectServer(clientAddress, sigstrength);
            if (amServer()) {
                // Send response back to client
                ServerReply(packet, clientAddress, clientTimestamp, curTimeStamp);
            }
            logUDP(curTimeStamp, "Server: curTimeStamp: " + curTimeStamp);

        } else if (recvMagicNumber == RFUtil.magicNumberToInt(kServerBeaconMagicNumber)) {
            int serverAddress = (int)int16FromPacket(bytes);
            d("BB Server Beacon packet: len(" + packet.length + "), data: " + RFUtil.bytesToHex(packet));
            d("BB Server Beacon packet from Server " + serverAddress +
                    " (" + service.allBoards.boardAddressToName(serverAddress) + ")");
            // Try to re-elect server based on the heard board
            tryElectServer(serverAddress, sigstrength);
        } else if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kRemoteControlMagicNumber)) {
            int address = (int) int16FromPacket(bytes);
            int cmd = (int) int16FromPacket(bytes);
            int value = (int) int32FromPacket(bytes);
            d("Received Remote Control " + cmd + ", " + value + " from " + address);
            receiveRemoteControl(address, cmd, value);
        } else {
            d("packet not for sync server!");
        }
        return;
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
        // RMC: trying 10 instead of 100 because of long recovery times when time jumps on master
        if (samples.size()>10)
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

    private void processSyncResponse(byte[] recvPacket) {

        if(mBoardAddress<=0)
            mBoardAddress = service.allBoards.getBoardAddress(service.boardState.BOARD_ID);

        d("BB Sync Packet receive from server len (" + recvPacket.length + ") " +
                service.allBoards.boardAddressToName(mServerAddress) + "(" + mServerAddress + ")" +
                " -> " + service.allBoards.boardAddressToName(mBoardAddress) + "(" + mBoardAddress + ")");
        ByteArrayInputStream packet = new ByteArrayInputStream(recvPacket);

        long packetHeader = int16FromPacket(packet);
        long clientAddress = int16FromPacket(packet);
        long serverAddress = int16FromPacket(packet);
        long myTimeStamp = int64FromPacket(packet);
        long svTimeStamp = int64FromPacket(packet);
        long curTime = service.GetCurrentClock();
        long adjDrift;
        long roundTripTime = (curTime - myTimeStamp);
        //l("client time stamp " + String.format("0x%16X", myTimeStamp));
        //l("server time stamp " + String.format("0x%16X", svTimeStamp));
        //l("Round trip time is " + roundTripTime);

        long driftAdjust = -00;

        if (roundTripTime < 300) {
            //if (svTimeStamp < myTimeStamp) {
            //    adjDrift = (curTime - myTimeStamp) / 2 + (svTimeStamp - myTimeStamp);
            //} else
            // adjDrift = mytime delta from server
            // 4156 - 2208
            adjDrift = (svTimeStamp - myTimeStamp) - (curTime - myTimeStamp) / 2;

            d("Pre-calc Drift is " + (svTimeStamp - myTimeStamp) + " round trip = " + (curTime - myTimeStamp) + " adjDrift = " + adjDrift);

            AddSample(adjDrift, roundTripTime);

            Sample s = BestSample();
            mLatency = s.roundTripTime;

            replyCount++;
            Intent in = new Intent(ACTION.STATS);
            in.putExtra("resultCode", Activity.RESULT_OK);
            in.putExtra("msgType", 2);
            // Put extras into the intent as usual
            in.putExtra("stateReplies", replyCount);

            in.putExtra("stateMsgWifi", "Client");
            //mActivity.setStateMsgConn("Connected");

            // Fire the broadcast with intent packaged
            LocalBroadcastManager.getInstance(service).sendBroadcast(in);

            if (mLastSample == null || !s.equals(mLastSample)) {
                mPrefsEditor.putLong("drift", s.drift);
                mPrefsEditor.putLong("rtt", s.roundTripTime);
                mPrefsEditor.commit();
            }

            d("Final Drift=" + (s.drift + driftAdjust) + " RTT=" + s.roundTripTime);

            service.SetServerClockOffset(s.drift + driftAdjust, s.roundTripTime);
            logUDP(service.CurrentClockAdjusted(), "client: CurrentClockAdjusted: " + service.CurrentClockAdjusted());
        }
    }

    // Thread/ loop to send out requests
    void Start() {
        l("Sync Thread Staring");
        SharedPreferences prefs = service.getSharedPreferences("driftInfo", service.MODE_PRIVATE);
        mDrift = prefs.getLong("drift", 0);
        mRtt = prefs.getLong("rtt", 100);
        service.SetServerClockOffset(mDrift, mRtt);

        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mPrefsEditor = service.getSharedPreferences("driftInfo", service.MODE_PRIVATE).edit();

        if(mBoardAddress<=0)
            mBoardAddress = service.allBoards.getBoardAddress(service.boardState.BOARD_ID);

        while (true) {

            /*
                MEDIA MASTER SECTION
             */

            // Do we need to tell nearby clients who the media master is still?
            if( kMasterBroadcastsLeft > 0 ) {
                l("Resending master client packet. Iterations remaining: " + kMasterBroadcastsLeft);

                for (int i = 0; i < kMasterToClientPacket.length; i++) {

                    byte [] packet = kMasterToClientPacket[i];
                    if (packet != null && packet.length > 0) {
                        l("Resending master client packet type: " + i);
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
            if (amServer() == false) {
                try {

                    d("I'm a client " + service.allBoards.boardAddressToName(mBoardAddress) + "(" + mBoardAddress + ")");

                    ByteArrayOutputStream clientPacket = new ByteArrayOutputStream();

                    for (int i = 0; i < RFUtil.kMagicNumberLen; i++) {
                        clientPacket.write(RFUtil.kClientSyncMagicNumber[i]);
                    }

                    // My Client Address
                    int16ToPacket(clientPacket, mBoardAddress);
                    // My timeclock
                    int64ToPacket(clientPacket, service.GetCurrentClock());
                    // Send latency so server knows how much to delay sync'ed start
                    int64ToPacket(clientPacket, mLatency);
                    // Pad to balance send-receive round trip time for average calculation
                    int16ToPacket(clientPacket, 0);
                    d("send packet " + RFUtil.bytesToHex(clientPacket.toByteArray()));
                    // Broadcast, but only server will pick up
                    service.radio.broadcast(clientPacket.toByteArray());
                    d("BB Sync Packet broadcast to server, ts=" + String.format("0x%08X", service.GetCurrentClock()) +
                            service.allBoards.boardAddressToName(mBoardAddress) + "(" + mBoardAddress + ")" +
                    " -> " + service.allBoards.boardAddressToName(mServerAddress) + "(" + mServerAddress + ")");

                } catch(Throwable e) {
                    //l("Client UDP failed");
                }
            } else {
                d("I'm a server: broadcast Server beacon");
                mRtt = 0;
                mDrift = 0;
                service.SetServerClockOffset(0, 0);

                ByteArrayOutputStream replyPacket = new ByteArrayOutputStream();

                for (int i = 0; i < RFUtil.kMagicNumberLen; i++) {
                    replyPacket.write(RFUtil.kServerBeaconMagicNumber[i]);
                }

                // Address of this server (just put this in now?)
                replyPacket.write(mBoardAddress & 0xFF);
                replyPacket.write((mBoardAddress >> 8) & 0xFF);

                // Broadcast - client will filter for it's address
                service.radio.broadcast(replyPacket.toByteArray());
            }

            try {
                Thread.sleep(kThreadSleepTime);
            } catch (Exception e) {
            }
        }
    }

    // Keep score of boards's we've heard from
    // If in range, vote +2 for it
    // If not in range, vote -1
    // From this list, pick the lowest address with votes = 10
    // If my address is lower than the lowest, then I'm the server!
    class boardVote {
        int votes;
        long lastHeard;
    }
    private HashMap<Integer, boardVote> mBoardVotes = new HashMap<>();

    private void incVote(int address, int amount) {
        // Increment the vote for me
        boardVote meVote = mBoardVotes.get(address);
        if (meVote == null) {
            meVote = new boardVote();
            meVote.votes = 0;
        }
        meVote.votes = meVote.votes + amount;
        if (meVote.votes > kMaxVotes) {
            meVote.votes = kMaxVotes;
        }
        meVote.lastHeard = SystemClock.elapsedRealtime();
        mBoardVotes.put(address, meVote);
    }

    private void decVotes() {
        // Decrement all the board votes
        for (int board: mBoardVotes.keySet()) {
            boardVote vote = mBoardVotes.get(board);
            int votes = vote.votes;
            if (votes > 1) {
                votes = votes - 1;
            }
            vote.votes = votes;
            mBoardVotes.put(board, vote);
        }
    }

    private long mLastVote;

    // Parameters for voting
    // Time we hold on to a valid master server is kMaxVotes * kMinVoteTime
    // 30 * 5 secs = 150 seconds
    // have to hear from a master kIncVote/kMaxVotes times in 150 seconds
    private final static int kMaxVotes    = 30; // max of 30 votes
    private final static int kMinVotes    = 20; // Must have at least this to be a server
    private final static int kIncVote     = 10; // have to hear from a master 3 times in 150 seconds
    private final static int kIncMyVote   = 4;  // inc my vote slower to allow for packet loss
    private final static int kMinVoteTime = 5000;

    private void tryElectServer(int address, int sigstrength) {

        // Ignore server if it's far away
        // 80db is typically further than you can hear the audio
        //if (sigstrength > 80) {
        //    return;
        //}

        // Decrement all votes by one as often as every kMinVoteTime seconds.
        // This makes the stickyness for a heard master
        // kMaxVotes / kMinVoteTime
        long timeSinceVote = SystemClock.elapsedRealtime() - mLastVote;
        if (timeSinceVote > kMinVoteTime) {
            decVotes();
            // Vote for myself
            // Always vote for myself.
            // I'll get knocked out if there is a higher ranked address with votes
            incVote(mBoardAddress, kIncMyVote);
            mLastVote = SystemClock.elapsedRealtime();
        }


        // Vote for the heard board
        incVote(address, kIncVote);

        // Find the leader to elect
        int lowest = 65535;
        for (int board: mBoardVotes.keySet()) {
            boardVote v = mBoardVotes.get(board);
            // Not a leader if we haven't heard from you in the last 5 mins
            if ((SystemClock.elapsedRealtime() - v.lastHeard) > 300000) {
                continue;
            }
            // Not a leader if you aren't reliably there
            if (v.votes < kMinVotes) {
                continue;
            }
            // Elect you if you are the lowest heard from
            if (board < lowest) {
                lowest = board;
            }
        }
        if (lowest < 65535) {
            mServerAddress = lowest;
        }

        // Dump the list of votes
        for (int board: mBoardVotes.keySet()) {
            boardVote v = mBoardVotes.get(board);
            if (board == mServerAddress) {
                l("Vote: Server " + service.allBoards.boardAddressToName(board) + "(" + board + ") : " + v.votes
                        + ", lastheard: " + (SystemClock.elapsedRealtime() - v.lastHeard));
            } else {
                l("Vote: Client " + service.allBoards.boardAddressToName(board) + "(" + board + ") : " + v.votes
                        + ", lastheard: " + (SystemClock.elapsedRealtime() - v.lastHeard));
            }
        }
    }

    // For now; elect the time leader as the server
    // Todo: make it a user-pref driven by a physical switch and the user-app.
    public boolean amServer() {
        if (mBoardAddress == mServerAddress) {
            // I'm the server!!
            return true;
        }
        return false;
    }



    public void sendRemote(int cmd, long value, int type) {

        if (service.radio == null) {
            return;
        }
        l("Sending remote control command: " + cmd + ", " + value + ", " + type + ", " + mBoardAddress);

        ByteArrayOutputStream clientPacket = new ByteArrayOutputStream();

        for (int i = 0; i < RFUtil.kMagicNumberLen; i++) {
            clientPacket.write(RFUtil.kRemoteControlMagicNumber[i]);
        }

        // Client
        int16ToPacket(clientPacket, mBoardAddress);
        // Command
        int16ToPacket(clientPacket, cmd);
        // Value
        int32ToPacket(clientPacket, value);

        // Send 10 times now, and let the supervisor thread send it periodically still
        byte [] packet = clientPacket.toByteArray();

        for (int i = 0; i < 10; i++) {
            service.radio.broadcast(packet);
        }

        kMasterToClientPacket[type] = packet;

        // This is the amount of iterations LEFT of broadcasting the client packet.
        // When this routine is invoked again, it'll restart the iteration counter.
        kMasterBroadcastsLeft = kMasterBroadcastTime / kThreadSleepTime;
        l( "Master client packet will be sent this many more times: " + kMasterBroadcastsLeft);
    }

    // Method to abandon rebroadcasts. Needed if a new master shows up.
    public void disableMasterBroadcast() { kMasterBroadcastsLeft = 0; }

    // TODO: Put this back as a remote control packet
    public void receiveRemoteControl(int address, int cmd, long value) {
        //l( "Received command: " + cmd + ", " + value + ", " + address);

        String client = service.allBoards.boardAddressToName(address);
        decodeRemoteControl(client, cmd, value);
    }


    // TODO: Put this back as a remote control packet
    // Change value -> hash lookup
    public void decodeRemoteControl(String client, int cmd, long value) {


        if (service.boardState.blockMaster) {
            l("BLOCKED remote cmd, value " + cmd + ", " + value + " from: " + client);
        } else {

            l("Received remote cmd, value " + cmd + ", " + value + " from: " + client);

            switch (cmd) {
                case RFUtil.REMOTE_AUDIO_TRACK_CODE:

                    for (int i = 1; i <= service.mediaManager.GetTotalAudio(); i++) {
                        String name = service.musicPlayer.getRadioChannelInfo(i);
                        long hashed = BurnerBoardUtil.hashTrackName(name);
                        if (hashed == value) {
                            l("Remote Audio " + service.boardState.currentRadioChannel+ " -> " + i);
                            if (service.boardState.currentRadioChannel != i) {
                                service.musicPlayer.SetRadioChannel((int) i);
                                l("Received remote audio switch to track " + i + " (" + name + ")");
                            } else {
                                l("Ignored remote audio switch to track " + i + " (" + name + ")");
                            }
                            break;
                        }
                    }
                    break;

                case RFUtil.REMOTE_VIDEO_TRACK_CODE:
                    for (int i = 1; i <= service.mediaManager.GetTotalVideo(); i++) {
                        String name = service.mediaManager.GetVideoFileLocalName(i - 1);
                        long hashed = BurnerBoardUtil.hashTrackName(name);
                        if (hashed == value) {
                            l("Remote Video " + service.boardState.currentVideoMode + " -> " + i);
                            if (service.boardState.currentVideoMode != i) {
                                service.boardVisualization.setMode((int) i);
                                l("Received remote video switch to mode " + i + " (" + name + ")");
                            } else {
                                l("Ignored remote video switch to mode " + i + " (" + name + ")");
                            }
                            break;
                        }
                    }
                    break;
                case RFUtil.REMOTE_MUTE_CODE:
                    if (value != service.musicPlayer.getCurrentBoardVol()) {
                        service.musicPlayer.setBoardVolume((int) value);
                    }
                    break;
                case RFUtil.REMOTE_MASTER_NAME_CODE:
                    if (service.boardState.masterRemote) {
                        // This board thinks it's the master, but apparently it's no longer. Reset master
                        // mode and follow the new master
                        String diag = service.boardState.BOARD_ID + " is no longer the master. New master: " + client;
                        l(diag);
                        service.voice.speak(diag, TextToSpeech.QUEUE_ADD, null, "master reset");
                        service.masterRemote.enableMaster(false);
                    }

                default:
                    break;
            }
        }
    }

    public long getLatency() {
        return mLatency / 2;
    }

}
