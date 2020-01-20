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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by jonathan
 * Converted to use RF network by rmc on 4/12/18.
 */

public class RFMasterClientServer {
    public static final int kRemoteAudio = 0;
    public static final int kRemoteVideo = 1;
    public static final int kRemoteMasterName = 2;
    static final int kThreadSleepTime = 5000;
    // How long does the master need to keep communicating that it's the master (in milliseconds)?
    static final int kMasterBroadcastTime = 5 * 60 * 1000;
    // Use this to store the client packet the master needs to send to take over audio & video
    byte[][] kMasterToClientPacket = new byte[3][];
    // How many iterations are LEFT for the master to tell the client? Initialize at 0 and have
    // the value set when a master command is issued
    int kMasterBroadcastsLeft = 0;
    private String TAG = this.getClass().getSimpleName();
    private BBService service;
    // Define the callback for what to do when stats are received
    private BroadcastReceiver RFReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int sigStrength = intent.getIntExtra("sigStrength", 0);
            byte[] packet = intent.getByteArrayExtra("packet").clone();
            processReceive(packet, sigStrength);
        }
    };

    public RFMasterClientServer(BBService service) {

        this.service = service;

        try {
            IntentFilter packetFilter = new IntentFilter(ACTION.BB_PACKET);
            LocalBroadcastManager.getInstance(this.service).registerReceiver(RFReceiver, packetFilter);
        } catch (Exception e) {

        }

    }

    public void Run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("BB RF Client/Server");
                RunBroadcastLoop();
            }
        });
        t.start();
    }

    void processReceive(byte[] packet, int sigstrength) {
        ByteArrayInputStream bytes = new ByteArrayInputStream(packet);

        int recvMagicNumber = RFUtil.magicNumberToInt(new int[]{bytes.read(), bytes.read()});

        //master is configured in the app. If you receive a master command, you must do it.
        //this is not related to the others in this method.
        if (recvMagicNumber == RFUtil.magicNumberToInt(RFUtil.kRemoteControlMagicNumber)) {
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

    // Thread/ loop to send out requests
    void RunBroadcastLoop() {
        BLog.d(TAG, "Master Thread Staring");

        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        while (true) {

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

        RFUtil.WriteMagicNumber(clientPacket, RFUtil.kRemoteControlMagicNumber);

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
    }

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
                case RFUtil.REMOTE_VOLUME_CODE:
                    this.service.masterController.RemoteVolume(value);
                    break;
                case RFUtil.REMOTE_MASTER_NAME_CODE:
                    this.service.masterController.NameMaster(client);
                default:
                    break;
            }
        }
    }

}
