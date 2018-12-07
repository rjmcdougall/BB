package com.richardmcdougall.bb;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

public class BluetoothCommands {
    private static final String TAG = "BB.BluetoothCommands";
    public Context mContext = null;
    public BBService mBBService = null;
    String mBoardId;
    BluetoothLEServer mBLEServer;
    BluetoothConnManager mBluetoothConnManager;
    public FindMyFriends mFindMyFriends = null;
    private Handler mHandler;

    public BluetoothCommands(BBService service, Context context, BluetoothLEServer ble,
                             BluetoothConnManager connmgr, FindMyFriends fmf) {
        mBBService = service;
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mBoardId = service.getBoardId();
        mBLEServer = ble;
        mBluetoothConnManager = connmgr;
        mFindMyFriends = fmf;
    }

    public void init() {

        // Register getstate command on bluetooth server
        mBLEServer.addCallback("getall",
                new BluetoothLEServer.BLECallback() {
                    @Override
                    public void onConnected(String clientId) {
                    }

                    @Override
                    public void onDisconnected(String clientId) {
                    }

                    @Override
                    public void OnAction(String clientId, BluetoothDevice device,
                                         String command, JSONObject payload) {
                        l("BBservice got getall OnAction");

                        String error = null;
                        JSONObject response = new JSONObject();

                        try {
                            response.put("command", command);
                        } catch (Exception e) {
                            error = "Could not insert command: " + e.getMessage();
                        }

                        // Add audio + video media lists
                        JSONObject media = mBBService.dlManager.GetDataDirectory();
                        if (media == null) {
                            error = "Could not get media directory (null)";
                        }
                        if (media != null) {
                            try {
                                JSONArray audio = media.getJSONArray("audio");
                                JSONArray video = media.getJSONArray("video");
                                response.put("audio", audio);
                                response.put("video", video);
                            } catch (Exception e) {
                                error = "Could not get media directory: " + e.getMessage();
                            }
                        }

                        // Add board list
                        JSONArray boards = mBBService.dlManager.GetDataBoards();
                        if (boards == null) {
                            error = "Could not get boards directory (null)";
                        }
                        if (boards != null) {
                            try {
                                response.put("boards", boards);
                            } catch (Exception e) {
                                error = "Could not get boards directory: " + e.getMessage();
                            }
                        }

                        // Bluetooth devices
                        JSONArray btdevs = mBluetoothConnManager.getDeviceList();
                        if (btdevs == null) {
                            error = "Could not get bt devs (null)";
                        }
                        if (btdevs != null) {
                            try {
                                response.put("btdevices", btdevs);
                            } catch (Exception e) {
                                error = "Could not get btdevs: " + e.getMessage();
                            }
                        }

                        // Locations
                        JSONArray locations = null;
                        try {
                            locations = new JSONArray(mFindMyFriends.getBoardLocations(300));
                        } catch (Exception e) {
                            error = "Could not get bt locations (empty)";
                        }
                        if (locations == null) {
                            error = "Could not get bt locations (null)";
                        }
                        if (locations != null) {
                            try {
                                response.put("locations", locations);
                            } catch (Exception e) {
                                error = "Could not get locations: " + e.getMessage();
                            }
                        }

                        // Current board state
                        JSONObject state = new JSONObject();
                        try {
                            state.put("audioChannelNo", mBBService.getRadioChannel() - 1);
                            state.put("videoChannelNo", mBBService.getVideoMode());
                            state.put("battery", mBBService.getBatteryLevel());
                            state.put("audioMaster", mBBService.isMaster());
                            state.put("APKUpdateDate", mBBService.getAPKUpdatedDate());
                            state.put("APKVersion", mBBService.getVersion());
                            state.put("IPAddress", mBBService.getIPAddress());
                            response.put("state", state);
                        } catch (Exception e) {
                            error = "Could not get state: " + e.getMessage();
                        }

                        if (error != null) {
                            try {
                                response.put("error", error);
                            } catch (Exception e) {
                            }
                        } else {
                            try {
                                response.put("error", "");
                            } catch (Exception e) {
                            }
                        }
                        // Send payload back to requesting device
                        mBLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());

                        l("BBservice done getall command");

                    }

                });

        // Register Volume command on bluetooth server
        mBLEServer.addCallback("Volume",
                new BluetoothLEServer.BLECallback() {
                    @Override
                    public void onConnected(String clientId) {
                    }

                    @Override
                    public void onDisconnected(String clientId) {
                    }

                    @Override
                    public void OnAction(String clientId, BluetoothDevice device,
                                         String command, JSONObject payload) {
                        l("BBservice got Volume command");
                    }
                });

        // Register Audio command on bluetooth server
        mBLEServer.addCallback("Audio",
                new BluetoothLEServer.BLECallback() {
                    @Override
                    public void onConnected(String clientId) {
                    }

                    @Override
                    public void onDisconnected(String clientId) {
                    }

                    @Override
                    public void OnAction(String clientId, BluetoothDevice device,
                                         String command, JSONObject payload) {
                        l("BBservice got Audio command");
                    }
                });

        // Register Video command on bluetooth server
        mBLEServer.addCallback("Video",
                new BluetoothLEServer.BLECallback() {
                    @Override
                    public void onConnected(String clientId) {
                    }

                    @Override
                    public void onDisconnected(String clientId) {
                    }

                    @Override
                    public void OnAction(String clientId, BluetoothDevice device,
                                         String command, JSONObject payload) {
                        l("BBservice got Video command");
                    }
                });
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

    public void d(String s) {
        Log.d(TAG, s);
        sendLogMsg(s);
    }

}
