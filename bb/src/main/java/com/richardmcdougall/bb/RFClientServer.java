package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by jonathan
 * Converted to use RF network by rmc on 4/12/18.
 */

public class RFClientServer {
    BBService mMain;
    public long tSentPackets = 0;
    private long replyCount = 0;
    private static final String TAG = "BB.RFClientServer";
    static final int [] kClientSyncMagicNumber = new int[] {0xbb, 0x03};
    static final int [] kServerSyncMagicNumber = new int[] {0xbb, 0x04};
    static final int [] kServerBeaconMagicNumber = new int[] {0xbb, 0x05};
    static final int kMagicNumberLen = 2;
    private int mServerAddress = 0;
    private int mBoardAddress;
    private RF mRF;
    private RFAddress mRFAddress = null;
    private PipedInputStream mReceivedPacketInput;
    private PipedOutputStream mReceivedPacketOutput;
    long mDrift;
    long mRtt;
    Sample mLastSample = null;
    SharedPreferences.Editor mPrefsEditor;

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

    RFClientServer(BBService service, RF rfRadio) {

        mMain = service;
        mRF = rfRadio;
        mRFAddress = rfRadio.mRFAddress;
        mBoardAddress = mRFAddress.getBoardAddress(service.getBoardId());

        // Make a pipe for packet receive
        try {
            mReceivedPacketInput = new PipedInputStream();
            mReceivedPacketOutput = new PipedOutputStream(mReceivedPacketInput);
        } catch (Exception e) {
            l("Receiver pipe failed: " + e.getMessage());
        }

        try {
            // Register for the particular broadcast based on Graphics Action
            IntentFilter packetFilter = new IntentFilter(BBService.ACTION_BB_PACKET);
            LocalBroadcastManager.getInstance(mMain).registerReceiver(RFReceiver, packetFilter);
        } catch (Exception e) {

        }

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

        l("Server reply : " +
                mRFAddress.boardAddressToName(mServerAddress) + "(" + mServerAddress + ")" +
                " -> " + mRFAddress.boardAddressToName(toClient) + "(" + toClient + ")");

        ByteArrayOutputStream replyPacket = new ByteArrayOutputStream();

        for (int i = 0; i < kMagicNumberLen; i++) {
            replyPacket.write(kServerSyncMagicNumber[i]);
        }

        // Address of this server (just put this in now?)
        int16ToPacket(replyPacket,mBoardAddress);

        // Address this packet is for
        int16ToPacket(replyPacket, toClient);

        // send the client's timestamp back along with ours so it can figure out how to adjust it's clock
        int64ToPacket(replyPacket, clientTimestamp);
        int64ToPacket(replyPacket, curTimeStamp);

        // Broadcast - client will filter for it's address
        mRF.broadcast(replyPacket.toByteArray());
        tSentPackets++;
        replyCount++;

        Intent in = new Intent(BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 2);
        // Put extras into the intent as usual
        in.putExtra("stateReplies", replyCount);
        in.putExtra("stateMsgWifi", "Server");
        LocalBroadcastManager.getInstance(mMain).sendBroadcast(in);
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

        int recvMagicNumber = magicNumberToInt(
                new int[] { bytes.read(), bytes.read()});

        int clientAddress = 0;

        if (recvMagicNumber == magicNumberToInt(kServerSyncMagicNumber)) {
            int serverAddress = (int)int16FromPacket(bytes);
            clientAddress = (int)int16FromPacket(bytes);

            if (clientAddress == mBoardAddress) {
                l("BB Sync Packet from Server: len(" + packet.length + "), data: " + bytesToHex(packet));
                l("BB Sync Packet from Server " + serverAddress +
                        " (" + mRFAddress.boardAddressToName(serverAddress) + ")");
                // Send to client loop to process the server's response
                processSyncResponse(packet);
            }
            // Try to re-elect server based on the heard board
            tryElectServer(serverAddress, sigstrength);
        } else if (recvMagicNumber == magicNumberToInt(kClientSyncMagicNumber)) {
            clientAddress = (int)int16FromPacket(bytes);

            l("BB Sync Packet from Client: len(" + packet.length + "), data: " + bytesToHex(packet));
            l("BB Sync Packet from Client " + clientAddress +
                    " (" + mRFAddress.boardAddressToName(clientAddress) + ")");
            long clientTimestamp = int64FromPacket(bytes);
            long curTimeStamp = mMain.GetCurrentClock();
            // Try to re-elect server based on the heard board
            tryElectServer(clientAddress, sigstrength);
            if (amServer()) {
                // Send response back to client
                ServerReply(packet, clientAddress, clientTimestamp, curTimeStamp);
            }
        } else if (recvMagicNumber == magicNumberToInt(kServerBeaconMagicNumber)) {
            int serverAddress = (int)int16FromPacket(bytes);
            l("BB Server Beacon packet: len(" + packet.length + "), data: " + bytesToHex(packet));
            l("BB Server Beacon packet from Server " + serverAddress +
                    " (" + mRFAddress.boardAddressToName(serverAddress) + ")");
            // Try to re-elect server based on the heard board
            tryElectServer(serverAddress, sigstrength);
        } else {
            l("packet not for sync server!");
        }
        return;
    }

    private static final int magicNumberToInt(int[] magic) {
        int magicNumber = 0;
        for (int i = 0; i < kMagicNumberLen; i++) {
            magicNumber = magicNumber + (magic[i] << ((kMagicNumberLen - 1 - i) * 8));
        }
        return (magicNumber);
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


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    private void processSyncResponse(byte[] recvPacket) {

        l("BB Sync Packet receive from server len (" + recvPacket.length + ") " +
                mRFAddress.boardAddressToName(mServerAddress) + "(" + mServerAddress + ")" +
                " -> " + mRFAddress.boardAddressToName(mBoardAddress) + "(" + mBoardAddress + ")");
        ByteArrayInputStream packet = new ByteArrayInputStream(recvPacket);

        long packetHeader = int16FromPacket(packet);
        long clientAddress = int16FromPacket(packet);
        long serverAddress = int16FromPacket(packet);
        long myTimeStamp = int64FromPacket(packet);
        long svTimeStamp = int64FromPacket(packet);
        long curTime = mMain.GetCurrentClock();
        long adjDrift;
        long roundTripTime = (curTime - myTimeStamp);
        //l("client time stamp " + String.format("0x%16X", myTimeStamp));
        //l("server time stamp " + String.format("0x%16X", svTimeStamp));
        //l("Round trip time is " + roundTripTime);

        if (roundTripTime < 300) {
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

            if (mLastSample == null || !s.equals(mLastSample)) {
                mPrefsEditor.putLong("drift", s.drift);
                mPrefsEditor.putLong("rtt", s.roundTripTime);
                mPrefsEditor.commit();
            }

            l("Drift=" + s.drift + " RTT=" + s.roundTripTime);

            mMain.SetServerClockOffset(s.drift, s.roundTripTime);
        }
    }

    // Thread/ loop to send out requests
    void Start() {
        l("Sync Thread Staring");
        SharedPreferences prefs = mMain.getSharedPreferences("driftInfo", mMain.MODE_PRIVATE);
        mDrift = prefs.getLong("drift", 0);
        mRtt = prefs.getLong("rtt", 100);
        mMain.SetServerClockOffset(mDrift, mRtt);

        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mPrefsEditor = mMain.getSharedPreferences("driftInfo", mMain.MODE_PRIVATE).edit();


        while (true) {

            if (amServer() == false) {
                try {

                    l("I'm a client " + mRFAddress.boardAddressToName(mBoardAddress) + "(" + mBoardAddress + ")");

                    ByteArrayOutputStream clientPacket = new ByteArrayOutputStream();

                    for (int i = 0; i < kMagicNumberLen; i++) {
                        clientPacket.write(kClientSyncMagicNumber[i]);
                    }

                    // My Client Address
                    int16ToPacket(clientPacket, mBoardAddress);
                    // My timeclock
                    int64ToPacket(clientPacket, mMain.GetCurrentClock());
                    // Pad to balance send-receive round trip time for average calculation
                    int64ToPacket(clientPacket, 0);
                    int16ToPacket(clientPacket, 0);
                    l("send packet " + bytesToHex(clientPacket.toByteArray()));
                    // Broadcast, but only server will pick up
                    mRF.broadcast(clientPacket.toByteArray());
                    l("BB Sync Packet broadcast to server, ts=" + String.format("0x%08X", mMain.GetCurrentClock()) +
                             mRFAddress.boardAddressToName(mBoardAddress) + "(" + mBoardAddress + ")" +
                    " -> " + mRFAddress.boardAddressToName(mServerAddress) + "(" + mServerAddress + ")");

                } catch(Throwable e) {
                    //l("Client UDP failed");
                }
            } else {

                l("I'm a server: broadcast Server beacon");

                ByteArrayOutputStream replyPacket = new ByteArrayOutputStream();

                for (int i = 0; i < kMagicNumberLen; i++) {
                    replyPacket.write(kServerBeaconMagicNumber[i]);
                }

                // Address of this server (just put this in now?)
                replyPacket.write(mBoardAddress & 0xFF);
                replyPacket.write((mBoardAddress >> 8) & 0xFF);

                // Broadcast - client will filter for it's address
                mRF.broadcast(replyPacket.toByteArray());
            }

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
            }
        }
    }

    // TODO: Put this back as a remote control packet
    public void receiveRemoteControl() {

        int serverStreamIndex = 0;
        int boardMode = 0;
        int boardVol = 0;

        if (serverStreamIndex != mMain.currentRadioChannel) {
            mMain.SetRadioChannel(serverStreamIndex);
        }

        if (boardMode > 0 &&
                boardMode != mMain.getCurrentBoardMode()) {
            mMain.setMode(boardMode);
        }

        if (boardVol != mMain.getCurrentBoardVol()) {
            //System.out.println("UDP: set vol = " + boardVol);
            mMain.setBoardVolume(boardVol);
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

    private void tryElectServer(int address, int sigstrength) {

        // Ignore server if it's far away
        // 80db is typically further than you can hear the audio
        //if (sigstrength > 80) {
        //    return;
        //}

        // Always vote for myself.
        // I'll get knocked out if there is a higher ranked address with votes
        boardVote meVote = new boardVote();
        meVote.votes = 999;
        meVote.lastHeard = mMain.GetCurrentClock();
        boardVote me = mBoardVotes.put(mBoardAddress, meVote);

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

        // Increment the vote for the heard board
        boardVote vote = mBoardVotes.get(address);
        if (vote == null) {
            vote = new boardVote();
            vote.votes = 0;
        }
        vote.votes = vote.votes + 3;
        if (vote.votes > 12) {
            vote.votes = 12;
        }
        vote.lastHeard = mMain.GetCurrentClock();
        mBoardVotes.put(address, vote);

        // Find the leader to elect
        int lowest = 65535;
        for (int board: mBoardVotes.keySet()) {
            boardVote v = mBoardVotes.get(board);
            // Not a leader if we haven't heard from you in the last 5 mins
            if ((mMain.GetCurrentClock() - v.lastHeard) > 300000) {
                continue;
            }
            // Not a leader if you aren't reliably there
            if (v.votes < 6) {
                continue;
            }
            // Elect you if you are the lowest heard from
            if (board < lowest) {
                lowest = board;
            }
        }
        if (lowest < 65536) {
            mServerAddress = lowest;
        }

        // Dump the list of votes
        for (int board: mBoardVotes.keySet()) {
            boardVote v = mBoardVotes.get(board);
            if (board == mServerAddress) {
                l("Vote: Server " + mRFAddress.boardAddressToName(board) + "(" + board + ") : " + v.votes
                        + ", lastheard: " + (mMain.GetCurrentClock() - v.lastHeard));
            } else {
                l("Vote: Client " + mRFAddress.boardAddressToName(board) + "(" + board + ") : " + v.votes
                        + ", lastheard: " + (mMain.GetCurrentClock() - v.lastHeard));
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
}
