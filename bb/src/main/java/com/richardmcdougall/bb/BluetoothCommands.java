package com.richardmcdougall.bb;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BluetoothCommands {
    private static final String TAG = "BB.BluetoothCommands";
    private BBService service = null;
    private Handler mHandler;

    public BluetoothCommands(BBService service) {
        this.service = service;
        mHandler = new Handler(Looper.getMainLooper());

        // Register to receive button messages
        IntentFilter filter;
        filter = new IntentFilter(ACTION.BB_VOLUME);
        LocalBroadcastManager.getInstance(service).registerReceiver(mBBEventReciever, filter);
        filter = new IntentFilter(ACTION.BB_AUDIOCHANNEL);
        LocalBroadcastManager.getInstance(service).registerReceiver(mBBEventReciever, filter);
        filter = new IntentFilter(ACTION.BB_VIDEOMODE);
        LocalBroadcastManager.getInstance(service).registerReceiver(mBBEventReciever, filter);
        filter = new IntentFilter(ACTION.BB_LOCATION);
        LocalBroadcastManager.getInstance(service).registerReceiver(mBBEventReciever, filter);
    }

    public void init() {

        service.bLEServer.addCallback("getboards",
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
                        d("BBservice got getboards OnAction");
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                            response.put("boards", service.allBoards.MinimizedBoards());
                        } catch (Exception e) {
                            e("error: " + e.getMessage());
                            try {
                                response.put("error", "");
                            } catch (Exception ex) {
                            }
                        }

                        service.bLEServer.tx(device, (String.format("%s;", response.toString())).getBytes());

                        d("BBservice done getboards command: " + response.toString());
                    }
                });

        service.bLEServer.addCallback("getaudio",
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
                        d("BBservice got getaudio OnAction");
                        String error = null;
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                            JSONArray audio = service.mediaManager.MinimizedAudio();
                            if (audio == null)
                                d("Empty Audio");
                            else
                                response.put("audio", audio);
                        } catch (Exception e) {
                            e("error: " + e.getMessage());
                            try {
                                response.put("error", "");
                            } catch (Exception ex) {
                            }
                        }

                        service.bLEServer.tx(device, (String.format("%s;", response.toString())).getBytes());

                        d("BBservice done getaudio command" + response.toString());
                    }
                });


        service.bLEServer.addCallback("getvideo",
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
                        d("BBservice got getvideo OnAction");
                        String error = null;
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                            JSONArray video = service.mediaManager.MinimizedVideo();
                            if (video == null)
                                d("Empty video");
                            else
                                response.put("video", video);
                        } catch (Exception e) {
                            e("error: " + e.getMessage());
                            try {
                                response.put("error", "");
                            } catch (Exception ex) {
                            }
                        }

                        service.bLEServer.tx(device, (String.format("%s;", response.toString())).getBytes());

                        d("BBservice done getvideo command" + response.toString());
                    }
                });

        service.bLEServer.addCallback("getall",
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
                        d("BBservice got getall OnAction");
                        String error = null;
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                            response.put("boards", service.allBoards.MinimizedBoards());
                            JSONArray audio = service.mediaManager.MinimizedAudio();
                            if (audio == null)
                                d("Empty Audio");
                            else
                                response.put("audio", audio);
                            JSONArray video = service.mediaManager.MinimizedVideo();
                            if (video == null)
                                d("Empty VideoMedia");
                            else
                                response.put("video", video);
                            JSONArray btdevs = service.bluetoothConnManager.getDeviceListJSON();
                            if (btdevs == null)
                                d("Could not get bt devs (null)");
                            else
                                response.put("btdevices", btdevs);

                            JSONObject state = service.boardState.MinimizedState();
                            response.put("state", state);
                        } catch (Exception e) {
                            e("error: " + e.getMessage());
                            try {
                                response.put("error", "");
                            } catch (Exception ex) {
                            }
                        }
                        service.bLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());
                        d("BBservice done getall command" + response.toString());
                    }
                });

        service.bLEServer.addCallback("EnableMaster",
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
                        d("BBservice got EnableMaster command:" + payload.toString());
                        try {
                            boolean isMaster = payload.getBoolean("arg");
                            service.masterRemote.enableMaster(isMaster);

                        } catch (Exception e) {
                            e("error setting Master: " + e.getMessage());
                        }
                        sendStateResponse(command, device);
                    }
                });

        service.bLEServer.addCallback("EnableGTFO",
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
                        d("BBservice got EnableGTFO command:" + payload.toString());
                        try {
                            boolean isGTFO = payload.getBoolean("arg");
                            service.gtfo.enableGTFO(isGTFO);

                        } catch (Exception e) {
                            e("error setting EnableGTFO: " + e.getMessage());
                        }
                        sendStateResponse(command, device);
                    }
                });

        service.bLEServer.addCallback("BlockMaster",
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
                        d("BBservice got BlockMaster command:" + payload.toString());
                        try {
                            boolean blockMaster = payload.getBoolean("arg");
                            service.boardState.blockMaster = blockMaster;

                        } catch (Exception e) {
                            e("error setting BlockMaster: " + e.getMessage());
                        }
                        sendStateResponse(command, device);
                    }
                });

        service.bLEServer.addCallback("Video",
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
                        d("BBservice got Video command:" + payload.toString());
                        try {
                            int track = payload.getInt("arg") + 1;
                            service.boardVisualization.setMode(track);
                        } catch (Exception e) {
                            e("error setting video track: " + e.getMessage());
                        }
                        sendStateResponse(command, device);
                    }
                });
        service.bLEServer.addCallback("getwifi",
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
                        d("BBservice got getwifi OnAction");
                        String error = null;
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                            JSONArray wifi = service.wifi.getScanResults();
                            if (wifi == null)
                                d("Empty wifi");
                            else
                                response.put("wifi", wifi);
                        } catch (Exception e) {
                            e("error: " + e.getMessage());
                            try {
                                response.put("error", "");
                            } catch (Exception ex) {
                            }
                        }

                        // Send payload back to requesting device
                        service.bLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());

                        d("BBservice done wifi command: " + response.toString());
                    }
                });
        service.bLEServer.addCallback("Wifi",
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
                        d("BBservice got Wifi command:" + payload.toString());
                        try {
                            String SSIS = payload.getString("arg");
                            if (SSIS != "") {
                                String[] parts = SSIS.split("__");
                                service.boardState.setSSISAndPassword(parts[0], parts[1]);
                            }
                        } catch (Exception e) {
                            e("error setting wifi: " + e.getMessage());
                        }
                        sendStateResponse(command, device);
                    }
                });

        service.bLEServer.addCallback("Volume",
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
                        d("BBservice got Volume command:" + payload.toString());
                        try {
                            int volume = payload.getInt("arg");
                            service.musicPlayer.setBoardVolume(volume);
                        } catch (Exception e) {
                            e("error setting volume: " + e.getMessage());
                        }
                        sendStateResponse(command, device);
                    }
                });

        service.bLEServer.addCallback("Audio",
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
                        d("BBservice got Audio command:" + payload.toString());
                        try {
                            int track = payload.getInt("arg");
                            service.musicPlayer.SetRadioChannel(track + 1);
                        } catch (Exception e) {
                            e("error setting audio track: " + e.getMessage());
                        }
                        sendStateResponse(command, device);
                    }
                });

        // Register Video command on bluetooth server
        service.bLEServer.addCallback("getstate",
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
                        d("BBservice get state command:" + payload.toString());
                        sendStateResponse(command, device);
                    }
                });

        service.bLEServer.addCallback("Location",
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
                        d("BBservice got Location command:" + payload.toString());

                        JSONObject response = new JSONObject();
                        try {
                            int age = age = payload.getInt("arg");
                            // Default to all if no age specified
                            if (age == 0)
                                age = 999999999;

                            response.put("command", command);
                            JSONArray locations = service.findMyFriends.getBoardLocationsJSON(age);
                            if (locations == null)
                                e("Could not get bt locations (null)");
                            else
                                response.put("locations", locations);
                        } catch (Exception e) {
                            e("error: " + e.getMessage());
                            try {
                                response.put("error", "");
                            } catch (Exception ex) {
                            }
                        }

                        // Send payload back to requesting device
                        service.bLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());
                        d("BBservice done sendlocation command");
                    }
                });

        service.bLEServer.addCallback("BTScan",
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
                        d("BBservice got BTScan command:" + payload.toString());

                        service.bluetoothConnManager.discoverDevices();

                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                            JSONArray btdevs = service.bluetoothConnManager.getDeviceListJSON();
                            if (btdevs == null)
                                d("Could not get bt devs (null)");
                            else
                                response.put("btdevices", btdevs);

                            JSONObject state = service.boardState.MinimizedState();
                            response.put("state", state);
                        } catch (Exception e) {
                            e("error: " + e.getMessage());
                            try {
                                response.put("error", "");
                            } catch (Exception ex) {
                            }
                        }

                        service.bLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());
                        d("BBservice done sendBTScanResponse command");
                    }
                });

        service.bLEServer.addCallback("BTSelect",
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
                        d("BBservice got BTSelect command:" + payload.toString());
                        try {
                            String address = payload.getString("arg");
                            service.bluetoothConnManager.togglePairDevice(address);
                        } catch (Exception e) {
                            e("error setting BTSelect: " + e.getMessage());
                        }
                    }
                });
    }

    void sendStateResponse(String command, BluetoothDevice device) {

        JSONObject response = new JSONObject();
        try {
            response.put("command", command);
            JSONObject state = service.boardState.MinimizedState();
            response.put("state", state);
        } catch (Exception e) {
            e("error: " + e.getMessage());
            try {
                response.put("error", "");
            } catch (Exception ex) {
            }
        }

        // Send payload back to requesting device
        service.bLEServer.tx(device,
                (String.format("%s;", response.toString())).getBytes());

        d("BBservice done sendStateResponse command");
    }

    public void sendStateResponseAll() {
        //TODO: get to list of connected devices,
        //      else it will get grabbed on location poll
        //sendStateResponse("unsolicited", device);
    }

    public void d(String s) {
        if (DebugConfigs.DEBUG_BLUETOOTH_COMMANDS) {
            Log.d(TAG, s);
            service.sendLogMsg(s);
        }
    }

    public void e(String s) {
        Log.e(TAG, s);
        service.sendLogMsg(s);
    }

    // We use this to catch the board events
    private final BroadcastReceiver mBBEventReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String TAG = "mBBEventReciever";

            String action = intent.getAction();

            Log.d(TAG, "onReceive entered:" + action);

            if (ACTION.BB_VOLUME.equals(action)) {
                Log.d(TAG, "Got volume");
                float volume = (float) intent.getSerializableExtra("volume");

            }
            sendStateResponseAll();
        }
    };
}
