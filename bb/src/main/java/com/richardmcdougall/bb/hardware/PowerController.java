package com.richardmcdougall.bb.hardware;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

public class PowerController {

    BBService service;
    VescController vesc;

    private String TAG = this.getClass().getSimpleName();

    public PowerController(BBService service) {
        this.service = service;
        vesc = service.vesc;
    }

    public void leds(boolean state) {
        try {
            BLog.d(TAG, "leds: " + state);
        } catch (Exception e) {
        }

    }

    public void amp(boolean state) {
        try {
            BLog.d(TAG, "amp: " + state);
        } catch (Exception e) {
        }
    }
    public void fud() {
        try {
            BLog.d(TAG, "fud");
        } catch (Exception e) {
        }
    }
    public void auto(boolean state) {
        try {
            BLog.d(TAG, "auto:" + state);
        } catch (Exception e) {
        }
    }

    public float getVoltage() {
        float value = 0;
        try {
            BLog.d(TAG, "getVoltage");
            value = vesc.getVoltage();
        } catch (Exception e) {
        }
        return value;
    }

    public float getTemp() {
        float value = 0;
        try {
            BLog.d(TAG, "getVoltage");
            value = vesc.getLedTemp();
        } catch (Exception e) {
        }
        return value;
    }

    public float getCurrent() {
        float value = 0;
        try {
            BLog.d(TAG, "getCurrent");
            value = vesc.getLedCurrent();
        } catch (Exception e) {
        }
        return value;
    }

}
