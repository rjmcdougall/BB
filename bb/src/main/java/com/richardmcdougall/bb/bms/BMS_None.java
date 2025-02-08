package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

public class BMS_None extends BMS {
    private String TAG = this.getClass().getSimpleName();
    public BMS_None(BBService service) {
        super(service);

        BLog.e(TAG, "Fake BMS");
    }
    
    public void update() {
    }
    
    public float getVoltage() {
        return 40;
    }

    public float getCurrent() {
        return 2;
    }

    public float getLevel() {
        return 90;
    }
}

