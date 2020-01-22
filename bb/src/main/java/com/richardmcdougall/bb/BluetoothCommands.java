package com.richardmcdougall.bb;

import android.bluetooth.BluetoothDevice;

import org.json.JSONArray;
import org.json.JSONObject;

public class BluetoothCommands {
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;

    public BluetoothCommands(BBService service) {
        this.service = service;
    }

    public void init() {

        service.bLEServer.addCallback("getboards",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got getboards OnAction");
                    JSONObject response = new JSONObject();
                    try {
                        response.put("command", command);
                        response.put("boards", service.allBoards.MinimizedBoards());
                    } catch (Exception e) {
                        BLog.e(TAG, "error: " + e.getMessage());
                        try {
                            response.put("error", "");
                        } catch (Exception ex) {
                        }
                    }

                    service.bLEServer.tx(device, (String.format("%s;", response.toString())).getBytes());

                    BLog.d(TAG, "BBservice done getboards command: " + response.toString());

                });

        service.bLEServer.addCallback("getaudio",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got getaudio OnAction");
                    String error = null;
                    JSONObject response = new JSONObject();
                    try {
                        response.put("command", command);
                        JSONArray audio = service.mediaManager.MinimizedAudio();
                        if (audio == null)
                            BLog.d(TAG, "Empty Audio");
                        else
                            response.put("audio", audio);
                    } catch (Exception e) {
                        BLog.e(TAG, "error: " + e.getMessage());
                        try {
                            response.put("error", "");
                        } catch (Exception ex) {
                        }
                    }

                    service.bLEServer.tx(device, (String.format("%s;", response.toString())).getBytes());

                    BLog.d(TAG, "BBservice done getaudio command" + response.toString());

                });

        service.bLEServer.addCallback("getvideo",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got getvideo OnAction");
                    String error = null;
                    JSONObject response = new JSONObject();
                    try {
                        response.put("command", command);
                        JSONArray video = service.mediaManager.MinimizedVideo();
                        if (video == null)
                            BLog.d(TAG, "Empty video");
                        else
                            response.put("video", video);
                    } catch (Exception e) {
                        BLog.e(TAG, "error: " + e.getMessage());
                        try {
                            response.put("error", "");
                        } catch (Exception ex) {
                        }
                    }

                    service.bLEServer.tx(device, (String.format("%s;", response.toString())).getBytes());

                    BLog.d(TAG, "BBservice done getvideo command" + response.toString());

                });

        service.bLEServer.addCallback("getall",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got getall OnAction");
                    String error = null;
                    JSONObject response = new JSONObject();
                    try {
                        response.put("command", command);
                        response.put("boards", service.allBoards.MinimizedBoards());
                        JSONArray audio = service.mediaManager.MinimizedAudio();
                        if (audio == null)
                            BLog.d(TAG, "Empty Audio");
                        else
                            response.put("audio", audio);
                        JSONArray video = service.mediaManager.MinimizedVideo();
                        if (video == null)
                            BLog.d(TAG, "Empty VideoMedia");
                        else
                            response.put("video", video);
                        JSONArray btdevs = service.bluetoothConnManager.getDeviceListJSON();
                        if (btdevs == null)
                            BLog.d(TAG, "Could not get bt devs (null)");
                        else
                            response.put("btdevices", btdevs);

                        JSONObject state = service.boardState.MinimizedState();
                        response.put("state", state);
                    } catch (Exception e) {
                        BLog.e(TAG, "error: " + e.getMessage());
                        try {
                            response.put("error", "");
                        } catch (Exception ex) {
                        }
                    }
                    service.bLEServer.tx(device,
                            (String.format("%s;", response.toString())).getBytes());
                    BLog.d(TAG, "BBservice done getall command" + response.toString());

                });

        service.bLEServer.addCallback("EnableMaster",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got EnableMaster command:" + payload.toString());
                    try {
                        boolean isMaster = payload.getBoolean("arg");
                        service.masterController.enableMaster(isMaster);

                    } catch (Exception e) {
                        BLog.e(TAG, "error setting Master: " + e.getMessage());
                    }
                    sendStateResponse(command, device);

                });

        service.bLEServer.addCallback("EnableGTFO",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got EnableGTFO command:" + payload.toString());
                    try {
                        boolean isGTFO = payload.getBoolean("arg");
                        service.gtfoController.enableGTFO(isGTFO);

                    } catch (Exception e) {
                        BLog.e(TAG, "error setting EnableGTFO: " + e.getMessage());
                    }
                    sendStateResponse(command, device);

                });

        service.bLEServer.addCallback("BlockMaster",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got BlockMaster command:" + payload.toString());
                    try {
                        boolean blockMaster = payload.getBoolean("arg");
                        service.boardState.blockMaster = blockMaster;

                    } catch (Exception e) {
                        BLog.e(TAG, "error setting BlockMaster: " + e.getMessage());
                    }
                    sendStateResponse(command, device);

                });

        service.bLEServer.addCallback("Video",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got Video command:" + payload.toString());
                    try {
                        int track = payload.getInt("arg") + 1;
                        service.boardVisualization.setMode(track);
                    } catch (Exception e) {
                        BLog.e(TAG, "error setting video track: " + e.getMessage());
                    }
                    sendStateResponse(command, device);

                });
        service.bLEServer.addCallback("getwifi",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got getwifi OnAction");
                    String error = null;
                    JSONObject response = new JSONObject();
                    try {
                        response.put("command", command);
                        JSONArray wifi = service.wifi.getScanResults();
                        if (wifi == null)
                            BLog.d(TAG, "Empty wifi");
                        else
                            response.put("wifi", wifi);
                    } catch (Exception e) {
                        BLog.e(TAG, "error: " + e.getMessage());
                        try {
                            response.put("error", "");
                        } catch (Exception ex) {
                        }
                    }

                    // Send payload back to requesting device
                    service.bLEServer.tx(device,
                            (String.format("%s;", response.toString())).getBytes());

                    BLog.d(TAG, "BBservice done wifi command: " + response.toString());

                });
        service.bLEServer.addCallback("Wifi",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got Wifi command:" + payload.toString());
                    try {
                        String SSIS = payload.getString("arg");
                        if (SSIS != "") {
                            String[] parts = SSIS.split("__");
                            service.boardState.setSSISAndPassword(parts[0], parts[1]);
                        }
                    } catch (Exception e) {
                        BLog.e(TAG, "error setting wifi: " + e.getMessage());
                    }
                    sendStateResponse(command, device);

                });

        service.bLEServer.addCallback("Volume",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got Volume command:" + payload.toString());
                    try {
                        int volume = payload.getInt("arg");
                        service.musicPlayer.setBoardVolume(volume);
                    } catch (Exception e) {
                        BLog.e(TAG, "error setting volume: " + e.getMessage());
                    }
                    sendStateResponse(command, device);

                });

        service.bLEServer.addCallback("Audio",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got Audio command:" + payload.toString());
                    try {
                        int track = payload.getInt("arg");
                        service.musicPlayer.SetRadioChannel(track + 1);
                    } catch (Exception e) {
                        BLog.e(TAG, "error setting audio track: " + e.getMessage());
                    }
                    sendStateResponse(command, device);

                });

        service.bLEServer.addCallback("getstate",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice get state command:" + payload.toString());
                    sendStateResponse(command, device);

                });

        service.bLEServer.addCallback("Location",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got Location command:" + payload.toString());

                    JSONObject response = new JSONObject();
                    try {
                        int age = age = payload.getInt("arg");
                        // Default to all if no age specified
                        if (age == 0)
                            age = 999999999;

                        response.put("command", command);
                        JSONArray locations = service.boardLocations.getBoardLocationsJSON(age);
                        if (locations == null)
                            BLog.e(TAG, "Could not get bt locations (null)");
                        else
                            response.put("locations", locations);
                    } catch (Exception e) {
                        BLog.e(TAG, "error: " + e.getMessage());
                        try {
                            response.put("error", "");
                        } catch (Exception ex) {
                        }
                    }

                    // Send payload back to requesting device
                    service.bLEServer.tx(device,
                            (String.format("%s;", response.toString())).getBytes());
                    BLog.d(TAG, "BBservice done sendlocation command");

                });

        service.bLEServer.addCallback("BTScan",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got BTScan command:" + payload.toString());

                    service.bluetoothConnManager.discoverDevices();

                    JSONObject response = new JSONObject();
                    try {
                        response.put("command", command);
                        JSONArray btdevs = service.bluetoothConnManager.getDeviceListJSON();
                        if (btdevs == null)
                            BLog.d(TAG, "Could not get bt devs (null)");
                        else
                            response.put("btdevices", btdevs);

                        JSONObject state = service.boardState.MinimizedState();
                        response.put("state", state);
                    } catch (Exception e) {
                        BLog.e(TAG, "error: " + e.getMessage());
                        try {
                            response.put("error", "");
                        } catch (Exception ex) {
                        }
                    }

                    service.bLEServer.tx(device,
                            (String.format("%s;", response.toString())).getBytes());
                    BLog.d(TAG, "BBservice done sendBTScanResponse command");

                });

        service.bLEServer.addCallback("BTSelect",
                (String clientId, BluetoothDevice device, String command, JSONObject payload) -> {
                    BLog.d(TAG, "BBservice got BTSelect command:" + payload.toString());
                    try {
                        String address = payload.getString("arg");
                        service.bluetoothConnManager.togglePairDevice(address);
                    } catch (Exception e) {
                        BLog.e(TAG, "error setting BTSelect: " + e.getMessage());
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
            BLog.e(TAG, "error: " + e.getMessage());
            try {
                response.put("error", "");
            } catch (Exception ex) {
            }
        }

        // Send payload back to requesting device
        service.bLEServer.tx(device,
                (String.format("%s;", response.toString())).getBytes());

        BLog.d(TAG, "BBservice done sendStateResponse command");
    }
}
