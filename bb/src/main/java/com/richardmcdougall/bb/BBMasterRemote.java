package com.richardmcdougall.bb;

import android.speech.tts.TextToSpeech;

public class BBMasterRemote {

    private BBService service = null;

    BBMasterRemote(BBService service) {
        this.service = service;
    }

    public void enableMaster(boolean enable) {
        service.boardState.masterRemote = enable;
        if (enable) {
            // Let everyone else know we just decided to be the master
            // Encoding the BOARD_ID as the payload; it's not really needed as we can read that
            // from the client data. XXX is there a better/more useful payload?
            service.rfClientServer.sendRemote(RFUtil.REMOTE_MASTER_NAME_CODE, BurnerBoardUtil.hashTrackName(service.boardState.BOARD_ID), RFClientServer.kRemoteMasterName);

            service.burnerBoard.setText("Master", 2000);
            service.voice.speak("Master Remote is: " + service.boardState.BOARD_ID, TextToSpeech.QUEUE_ADD, null, "enableMaster");
        } else {
            // You explicitly disabled the master. Stop any broadcasting.
            service.rfClientServer.disableMasterBroadcast();

            service.burnerBoard.setText("Solo", 2000);
            service.voice.speak("Disabling Master Remote: " + service.boardState.BOARD_ID, TextToSpeech.QUEUE_ADD, null, "disableMaster");
        }
    }
}
