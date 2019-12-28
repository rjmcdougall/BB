package com.richardmcdougall.bb;

import android.speech.tts.TextToSpeech;

public class GTFO {

    BBService service = null;
    private int stashedAndroidVolumePercent;

    public GTFO(BBService service) {
        this.service = service;
    }

    public void enableGTFO(boolean enable) {

        service.boardState.isGTFO = enable;
        if (enable) {

            service.boardVisualization.inhibitGTFO(true);
            service.burnerBoard.setText90("Get The Fuck Off!", 5000);
            service.musicPlayer.setVolume(0, 0);
            stashedAndroidVolumePercent = service.musicPlayer.getAndroidVolumePercent();
            service.musicPlayer.setAndroidVolumePercent(100);
            service.voice.speak("Hey, Get The Fuck Off!", TextToSpeech.QUEUE_ADD, null, "GTFO");
        } else {
            service.boardVisualization.inhibitGTFO(false);
            service.musicPlayer.setAndroidVolumePercent(stashedAndroidVolumePercent);
            service.musicPlayer.setVolume(1, 1);
        }
    }

}
