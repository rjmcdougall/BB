package com.richardmcdougall.bb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;

import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

public class ButtonReceiver extends BroadcastReceiver {
    private String TAG = this.getClass().getSimpleName();

    private BBService service;
    // Single press to show battery
    // Double press to show map
    // Tripple click to toggle master
    private long lastPressed = SystemClock.elapsedRealtime();
    private int pressCnt = 1;

    ButtonReceiver(BBService service) {
        this.service = service;
    }

    public void onReceive(Context context, Intent intent) {
        final String TAG = "mButtonReceiver";

        //Log.d(TAG, "onReceive entered");
        String action = intent.getAction();

        if (ACTION.BUTTONS.equals(action)) {
            BLog.d(TAG, "Received BlueTooth key press");
            BBService.buttons actionType = (BBService.buttons) intent.getSerializableExtra("buttonType");
            switch (actionType) {
                case BUTTON_KEYCODE:
                    BLog.d(TAG, "BUTTON_KEYCODE ");
                    int keyCode = intent.getIntExtra("keyCode", 0);
                    KeyEvent event = (KeyEvent) intent.getParcelableExtra("keyEvent");
                    onKeyDown(keyCode, event);
                    break;
                case BUTTON_TRACK:
                    BLog.d(TAG, "BUTTON_TRACK");
                    service.musicController.NextStream();
                    break;
                case BUTTON_MODE_UP:
                    BLog.d(TAG, "BUTTON_MODE_UP");
                    service.visualizationController.setMode(99);
                    break;
                case BUTTON_MODE_DOWN:
                    BLog.d(TAG, "BUTTON_MODE_DOWN");
                    service.visualizationController.setMode(98);
                    break;
                default:
                    break;
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (!BoardState.kIsRPI) {
            return onKeyDownBurnerBoard(keyCode, event);
        } else {
            return onKeyDownRPI(keyCode, event);
        }
    }

    public boolean onKeyDownBurnerBoard(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            BLog.d(TAG, "BurnerBoard Keycode:" + keyCode);
        }

        switch (keyCode) {
            case 100:
            case 87: // satachi right button
                service.musicController.NextStream();
                break;
            case 97:
            case 20:
                //service.musicPlayer.MusicOffset(-10);
                break;
            case 19:
                //service.musicPlayer.MusicOffset(10);
                break;
            case 85: // Play button - show battery
                onBatteryButton();
                break;
            case 99:
                service.visualizationController.setMode(99);
                break;
            case 98:
                service.visualizationController.setMode(98);
                break;
            case 88: //satachi left button
                service.visualizationController.setMode(99);
                break;
        }
        return true;
    }

    public boolean onKeyDownRPI(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            BLog.d(TAG, "RPI Keycode:" + keyCode);
        }

        switch (keyCode) {
            case 85: // satachi Play button
                service.visualizationController.NextVideo();
                break;

            /* Audio stream control */
            case 87: // satachi right button
                BLog.d(TAG, "RPI Bluetooth Right Button");
                service.musicController.NextStream();
                break;
            case 88: //satachi left button
                BLog.d(TAG, "RPI Bluetooth Left Button");
                service.musicController.PreviousStream();
                break;
        }
        return true;
    }

    public void onBatteryButton() {
        if (service.burnerBoard != null) {
            service.burnerBoard.showBattery();
            if ((SystemClock.elapsedRealtime() - lastPressed) < 600) {
                if (pressCnt == 1) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (pressCnt == 2) {
                                service.visualizationController.showMap();
                            } else if (pressCnt == 3) {
                                // Toggle master mode
                                if (service.boardState.masterRemote == true) {
                                    service.masterController.enableMaster(false);
                                } else {
                                    service.masterController.enableMaster(true);
                                }
                            }
                        }
                    }, 700);
                }
                pressCnt++;
            } else {
                pressCnt = 1;
            }
        }
        lastPressed = SystemClock.elapsedRealtime();
    }
}
