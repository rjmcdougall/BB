package com.richardmcdougall.bb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;

import timber.log.Timber;

public class ButtonReceiver extends BroadcastReceiver {

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
            Timber.d("Received BlueTooth key press");
            BBService.buttons actionType = (BBService.buttons) intent.getSerializableExtra("buttonType");
            switch (actionType) {
                case BUTTON_KEYCODE:
                    Timber.d("BUTTON_KEYCODE ");
                    int keyCode = intent.getIntExtra("keyCode", 0);
                    KeyEvent event = (KeyEvent) intent.getParcelableExtra("keyEvent");
                    onKeyDown(keyCode, event);
                    break;
                case BUTTON_TRACK:
                    Timber.d("BUTTON_TRACK");
                    service.musicPlayer.NextStream();
                    break;
                case BUTTON_MODE_UP:
                    Timber.d("BUTTON_MODE_UP");
                    service.boardVisualization.setMode(99);
                    break;
                case BUTTON_MODE_DOWN:
                    Timber.d("BUTTON_MODE_DOWN");
                    service.boardVisualization.setMode(98);
                    break;
                case BUTTON_DRIFT_DOWN:
                    Timber.d("BUTTON_DRIFT_DOWN");
                    service.musicPlayer.MusicOffset(-10);
                    break;
                case BUTTON_DRIFT_UP:
                    Timber.d("BUTTON_DRIFT_UP");
                    service.musicPlayer.MusicOffset(10);
                    break;
                case BUTTON_VOL_DOWN:
                    Timber.d("BUTTON_VOL_DOWN");
                    service.musicPlayer.onVolDown();
                    break;
                case BUTTON_VOL_UP:
                    Timber.d("BUTTON_VOL_UP");
                    service.musicPlayer.onVolUp();
                    break;
                case BUTTON_VOL_PAUSE:
                    Timber.d("BUTTON_VOL_PAUSE");
                    service.musicPlayer.onVolPause();
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
            Timber.d("BurnerBoard Keycode:" + keyCode);
        }

        switch (keyCode) {
            case 100:
            case 87: // satachi right button
                service.musicPlayer.NextStream();
                break;
            case 97:
            case 20:
                service.musicPlayer.MusicOffset(-10);
                break;
            case 19:
                service.musicPlayer.MusicOffset(10);
                break;
            case 24:   // native volume up button
            case 21:
                service.musicPlayer.onVolUp();

                return false;
            case 25:  // native volume down button
            case 22:
                service.musicPlayer.onVolDown();
                return false;
            case 85: // Play button - show battery
                onBatteryButton();
                break;
            case 99:
                service.boardVisualization.setMode(99);
                break;
            case 98:
                service.boardVisualization.setMode(98);
                break;
            case 88: //satachi left button
                service.boardVisualization.setMode(99);
                break;
        }
        return true;
    }

    public boolean onKeyDownRPI(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getRepeatCount() == 0) {
            Timber.d("RPI Keycode:" + keyCode);
        }

        switch (keyCode) {
            case 85: // satachi Play button
                service.boardVisualization.NextVideo();
                break;

            /* Audio stream control */
            case 87: // satachi right button
                Timber.d("RPI Bluetooth Right Button");
                service.musicPlayer.NextStream();
                break;
            case 88: //satachi left button
                Timber.d("RPI Bluetooth Left Button");
                service.musicPlayer.PreviousStream();
                break;

            /* Volume control */
            case 24:   // satachi & native volume up button
                Timber.d("RPI Bluetooth Volume Up Button");
                service.musicPlayer.onVolUp();
                return false;
            case 25:  // satachi & native volume down button
                Timber.d("RPI Bluetooth Volume Down Button");
                service.musicPlayer.onVolDown();
                return false;
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
                                service.boardVisualization.showMap();
                            } else if (pressCnt == 3) {
                                // Toggle master mode
                                if (service.boardState.masterRemote == true) {
                                    service.masterRemote.enableMaster(false);
                                } else {
                                    service.masterRemote.enableMaster(true);
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
