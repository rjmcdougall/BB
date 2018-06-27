package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Created by rmc on 4/15/18.
 */

public class RFAddress {

    public BBService mBBService = null;
    public Context mContext = null;
    private static final String TAG = "BB.RFAddress";

    public RFAddress(BBService service, Context context) {
        mBBService = service;
        mContext = context;
    }

    // TODO: should have a cache of board name/addresses that comes from the cloud.
    public int getBoardAddress(String boardId) {

        byte[] encoded = null;
        int myAddress;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            encoded = digest.digest(boardId.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            l("Could not calculate boardAddress");
            return -1;
        }
        // Natural board leaders get explicit addresses
        switch (boardId) {

            case "akula":
                myAddress = 1;
                break;

            case "candy":
                myAddress = 2;
                break;

            case "grumpy":
                myAddress = 3;
                break;

            case "cranky":
                myAddress = 4;
                break;

            case "artemis":
                myAddress = 5;
                break;

            case "test_board":
                myAddress = 100;
                break;

            case "handheld1":
                myAddress = 200;
                break;

            default:
                // Otherwise, calculate 16 bit address for radio from name
                myAddress = (encoded[0] & 0xff) * 256 + (encoded[1] & 0xff);
        }
        //l("Radio address for " + boardId + " = " + myAddress);
        return myAddress;
    }

    // TODO: should have a cache of boardnames that comes from the cloud.
    // TODO: alt is to have each board broadcast their name repeatedly
    // DKW this api is available now GET burnerboard.com/boards/
    public String boardAddressToName(int address) {
        String [] boardNames = {
                "proto",
                "akula",
                "boadie",
                "artemis",
                "goofy",
                "joon",
                "biscuit",
                "squeeze",
                "ratchet",
                "pegasus",
                "vega",
                "monaco",
                "candy",
                "test_board",
                "grumpy",
                "cranky",
                "sexy",
                "littleboard",
                "handheld1"};
        for (String name :boardNames) {
            if (address == getBoardAddress(name)) {
                return (name);
            }
        }
        return "unknown";
    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(BBService.ACTION_STATS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("msgType", 4);
        // Put extras into the intent as usual
        in.putExtra("logMsg", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
    }

    public void l(String s) {
        Log.v(TAG, s);
        sendLogMsg(s);
    }
}
