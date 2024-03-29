package com.richardmcdougall.bb.bms;

import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.hardware.BQ34Z100;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;

public class BMS_None extends BMS {
    private String TAG = this.getClass().getSimpleName();
    private BQ34Z100 mBQ = null;

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

