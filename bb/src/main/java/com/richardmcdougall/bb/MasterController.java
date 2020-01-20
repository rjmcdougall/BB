package com.richardmcdougall.bb;

import android.speech.tts.TextToSpeech;

import com.richardmcdougall.bb.rf.RFMasterClientServer;
import com.richardmcdougall.bb.rf.RFUtil;

public class MasterController {

    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;

    MasterController(BBService service) {
        this.service = service;
    }

    public void enableMaster(boolean enable) {
        service.boardState.masterRemote = enable;
        if (enable) {
            // Let everyone else know we just decided to be the master
            // Encoding the BOARD_ID as the payload; it's not really needed as we can read that
            // from the client data. XXX is there a better/more useful payload?
            service.rfMasterClientServer.sendRemote(RFUtil.REMOTE_MASTER_NAME_CODE, MediaManager.hashTrackName(service.boardState.BOARD_ID), RFMasterClientServer.kRemoteMasterName);

            service.burnerBoard.setText("Master", 2000);
            service.voice.speak("Master Remote is: " + service.boardState.BOARD_ID, TextToSpeech.QUEUE_ADD, null, "enableMaster");
        } else {
            // You explicitly disabled the master. Stop any broadcasting.
            service.rfMasterClientServer.disableMasterBroadcast();

            service.burnerBoard.setText("Solo", 2000);
            service.voice.speak("Disabling Master Remote: " + service.boardState.BOARD_ID, TextToSpeech.QUEUE_ADD, null, "disableMaster");
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
        for (int i = 1; i <= service.mediaManager.GetTotalVideo(); i++) {
            String name = service.mediaManager.GetVideoFileLocalName(i - 1);
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
            service.voice.speak(diag, TextToSpeech.QUEUE_ADD, null, "master reset");
            enableMaster(false);
        }

    }
}
