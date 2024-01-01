package com.richardmcdougall.bb.hardware;

import android.content.Context;

import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;

import java.io.IOException;

public class CanManager {
    private final static String TAG = "CanManager";
    private MCP2515 mMcp;

    public CanManager(Context context, BoardState boardState) {

        BLog.e(TAG, "Can starting");
        try {
            BLog.e(TAG, "Can opening" + mMcp);
            mMcp = mMcp.open("SPI1.0");
            BLog.e(TAG, "Can opened" + mMcp);

        } catch (IOException e) {
            BLog.e(TAG, "Cannot open Can");
        }
    }
}
