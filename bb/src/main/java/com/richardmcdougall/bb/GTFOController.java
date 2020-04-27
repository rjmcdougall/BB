package com.richardmcdougall.bb;

import android.graphics.Color;
import android.speech.tts.TextToSpeech;

import com.richardmcdougall.bb.visualization.BBColor;

public class GTFOController {

    private BBService service = null;
    private int stashedAndroidVolumePercent;

    public GTFOController(BBService service) {
        this.service = service;
    }

    public void enableGTFO(boolean enable) {

        service.boardState.isGTFO = enable;
        if (enable) {

            service.boardVisualization.inhibitVisualGTFO = true;
            service.burnerBoard.setText90("Get The Fuck Off!", 5000, new BBColor().getColor("white"));
            service.musicPlayer.Mute();
            stashedAndroidVolumePercent = service.musicPlayer.getAndroidVolumePercent();
            service.musicPlayer.setAndroidVolumePercent(100);
            service.speak("Hey, Get The Fuck Off!", "GTFO");
        } else {
            service.boardVisualization.inhibitVisualGTFO = false;
            service.musicPlayer.setAndroidVolumePercent(stashedAndroidVolumePercent);
            service.musicPlayer.Unmute();
        }
    }

}
