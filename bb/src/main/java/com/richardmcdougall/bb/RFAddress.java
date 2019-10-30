package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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

        JSONObject board;
        int myAddress = -1;

        try {

            if (mBBService.dlManager == null) {
                l("Could not find board address data");
            } else if (mBBService.dlManager.dataBoards == null) {
                l("Could not find board address data");
            } else {
                for (int i = 0; i < mBBService.dlManager.dataBoards.length(); i++) {
                    board = mBBService.dlManager.dataBoards.getJSONObject(i);
                    if (board.getString("name").equals(boardId)) {
                        myAddress = board.getInt("address");
                    }
                }
            }
            return myAddress;
        } catch (JSONException e) {
            l(e.getMessage());
            return myAddress;
        } catch (Exception e) {
            l(e.getMessage());
            return myAddress;
        }

    }

    // TODO: should have a cache of boardnames that comes from the cloud.
    // TODO: alt is to have each board broadcast their name repeatedly
    // DKW this api is available now GET burnerboard.com/boards/
    public String boardAddressToName(int address) {

        JSONObject board;
        String boardId = "unknown";

        try {

            if (mBBService.dlManager == null) {
                l("Could not find board name data");
            } else if (mBBService.dlManager.dataBoards == null) {
                l("Could not find board name data");
            } else {
                for (int i = 0; i < mBBService.dlManager.dataBoards.length(); i++) {
                    board = mBBService.dlManager.dataBoards.getJSONObject(i);
                    if (board.getInt("address") == address) {
                        boardId = board.getString("name");
                    }
                }
            }

            return boardId;

        } catch (JSONException e) {
            l(e.getMessage());
        } catch (Exception e) {
            l(e.getMessage());
        }

        return boardId;
    }

    public String boardAddressToColor(int address) {

        JSONObject board;
        String boardColor = "unknown";

        try {

            if (mBBService.dlManager == null) {
                l("Could not find board color data");
            } else if (mBBService.dlManager.dataBoards == null) {
                l("Could not find board color data");
            } else {
                for (int i = 0; i < mBBService.dlManager.dataBoards.length(); i++) {
                    board = mBBService.dlManager.dataBoards.getJSONObject(i);
                    if (board.getInt("address") == address) {
                        boardColor = board.getString("color");
                    }
                }
            }

            return boardColor;
        } catch (JSONException e) {
            l(e.getMessage());
            return boardColor;
        } catch (Exception e) {
            l(e.getMessage());
            return boardColor;
        }
    }

    private void sendLogMsg(String msg) {
        Intent in = new Intent(ACTION.STATS);
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
