package com.richardmcdougall.bb;

import android.os.Handler;
import android.os.Looper;

import com.richardmcdougall.bb.rf.RFMasterClientServer;
import com.richardmcdougall.bb.rf.RFUtil;
import com.richardmcdougall.bbcommon.BLog;

public class MasterController implements Runnable {

    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;

    private Handler handler;

    MasterController(BBService service) {
        this.service = service;
    }

    public void run() {

        Looper.prepare();
        handler = new Handler(Looper.myLooper());

        Looper.loop();
    }

    public void enableMaster(boolean enable) {
        this.handler.post(() -> mEnableMaster(enable));
    }

    private void mEnableMaster(boolean enable) {
        service.boardState.masterRemote = enable;
        if (enable) {
            // Let everyone else know we just decided to be the master
            // Encoding the BOARD_ID as the payload; it's not really needed as we can read that
            // from the client data. XXX is there a better/more useful payload?

            //prime with current values
            mSendVideo();
            mSendAudio();
            mSendVolume();
            mSendMasterInfo();

            service.burnerBoard.setText("Master", 2000);
            service.speak("Master Remote is: " + service.boardState.BOARD_ID, "enableMaster");
        } else {
            // You explicitly disabled the master. Stop any broadcasting.
            service.rfMasterClientServer.disableMasterBroadcast();

            service.burnerBoard.setText("Solo", 2000);
            service.speak("Disabling Master Remote: " + service.boardState.BOARD_ID, "disableMaster");
        }
    }

    public void RemoteAudio(long value){

        for (int i = 1; i <= service.mediaManager.GetTotalAudio(); i++) {
            String name = service.musicPlayer.getRadioChannelInfo(i);
            long hashed = MediaManager.hashTrackName(name);
            if (hashed == value) {
                BLog.d(TAG, "Remote Audio " + service.boardState.currentRadioChannel + " -> " + i);
                if (service.boardState.currentRadioChannel != i) {
                    service.musicPlayer.SetRadioChannel((int) i);
                    BLog.d(TAG, "Received remote audio switch to track " + i + " (" + name + ")");
                } else {
                    BLog.d(TAG, "Ignored remote audio switch to track " + i + " (" + name + ")");
                }
                break;
            }
        }
    }

    public void RemoteVideo(long value){
        for (int i = 0; i < service.mediaManager.GetTotalVideo(); i++) {
            String name = service.mediaManager.GetVideoFileLocalName(i);
            long hashed = MediaManager.hashTrackName(name);
            if (hashed == value) {
                BLog.d(TAG, "Remote Video " + service.boardState.currentVideoMode + " -> " + i);
                if (service.boardState.currentVideoMode != i) {
                    service.boardVisualization.setMode((int) i);
                    BLog.d(TAG, "Received remote video switch to mode " + i + " (" + name + ")");
                } else {
                    BLog.d(TAG, "Ignored remote video switch to mode " + i + " (" + name + ")");
                }
                break;
            }
        }
    }

    public void RemoteVolume(long value){
        if (value != service.musicPlayer.getCurrentBoardVol()) {
            service.musicPlayer.setBoardVolume((int) value);
        }
    }

    public void NameMaster(String client){
        if (service.boardState.masterRemote) {
            // This board thinks it's the master, but apparently it's no longer. Reset master
            // mode and follow the new master
            String diag = service.boardState.BOARD_ID + " is no longer the master. New master: " + client;
            BLog.d(TAG, diag);
            service.speak(diag,"master reset");
            enableMaster(false);
        }

    }

    public void SendVolume() {
        this.handler.post(() -> mSendVolume());
    }

    private void mSendVolume() {

        BLog.d(TAG, "Sending remote volume");
        service.rfMasterClientServer.sendRemote(RFUtil.REMOTE_VOLUME_CODE, service.musicPlayer.getCurrentBoardVol(), RFMasterClientServer.kRemoteVolume);
        try {
            Thread.sleep(service.rfClientServer.getLatency());
        } catch (Exception e) {
        }

    }

    public void SendAudio() {
        this.handler.post(() -> mSendAudio());
    }

    private void mSendAudio() {

        BLog.d(TAG, "Sending remote audio");

        String fileName = service.musicPlayer.getRadioChannelInfo(service.boardState.currentRadioChannel);
        service.rfMasterClientServer.sendRemote(RFUtil.REMOTE_AUDIO_TRACK_CODE, MediaManager.hashTrackName(fileName), RFMasterClientServer.kRemoteAudio);

        try {
            Thread.sleep(service.rfClientServer.getLatency());
        } catch (Exception e) {
        }
    }

    public void SendVideo() {
        this.handler.post(() -> mSendVideo());
    }

    private void mSendVideo() {
        String name = service.mediaManager.GetVideoFileLocalName(service.boardState.currentVideoMode);
        BLog.d(TAG, "Sending video remote for video " + name);
        service.rfMasterClientServer.sendRemote(RFUtil.REMOTE_VIDEO_TRACK_CODE, MediaManager.hashTrackName(name), RFMasterClientServer.kRemoteVideo);

        try {
            Thread.sleep(service.rfClientServer.getLatency());
        } catch (Exception e) {
        }
    }

    public void SendMasterInfo() {
        this.handler.post(() -> mSendMasterInfo());
    }

    private void mSendMasterInfo() {

        BLog.d(TAG, "Sending master name " + service.boardState.BOARD_ID);
        service.rfMasterClientServer.sendRemote(RFUtil.REMOTE_MASTER_NAME_CODE, MediaManager.hashTrackName(service.boardState.BOARD_ID), RFMasterClientServer.kRemoteMasterName);

        try {
            Thread.sleep(service.rfClientServer.getLatency());
        } catch (Exception e) {
        }
    }

}
