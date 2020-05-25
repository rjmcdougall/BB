package com.richardmcdougall.bb;

import com.richardmcdougall.bb.visualization.RGBList;

public class GTFOController {

    private BBService service = null;
    private int stashedAndroidVolumePercent;

    public GTFOController(BBService service) {
        this.service = service;
    }

    public void enableGTFO(boolean enable) {

        service.boardState.isGTFO = enable;
        if (enable) {

            service.visualizationController.inhibitVisualGTFO = true;
            service.burnerBoard.setText90("Get The Fuck Off!", 5000, new RGBList().getColor("white"));
            service.musicController.Mute();
            stashedAndroidVolumePercent = service.musicController.getAndroidVolumePercent();
            service.musicController.setAndroidVolumePercent(100);
            service.speak("Hey, Get The Fuck Off!", "GTFO");
        } else {
            service.visualizationController.inhibitVisualGTFO = false;
            service.musicController.setAndroidVolumePercent(stashedAndroidVolumePercent);
            service.musicController.Unmute();
        }
    }

}
