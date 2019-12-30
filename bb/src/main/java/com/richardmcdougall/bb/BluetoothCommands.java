package com.richardmcdougall.bb;

import android.app.Activity;
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
    public BBService service = null;
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

    public JSONArray Boards() {
        JSONArray boards = service.allBoards.dataBoards;
        JSONArray boards2 = null;
        if (boards == null) {
           l( "Could not get boards directory (null)");
        }
        if (boards != null) {
            try {

                boards2 = new JSONArray(boards.toString()) ;
                for (int i = 0; i < boards2.length(); i++) {
                    JSONObject a = boards2.getJSONObject(i);
                    if(a.has("address"))  a.remove("address");
                    if(a.has("isProfileGlobal"))  a.remove("isProfileGlobal");
                    if(a.has("profile"))   a.remove("profile");
                    if(a.has("isProfileGlobal2"))  a.remove("isProfileGlobal2");
                    if(a.has("profile2"))   a.remove("profile2");
                    if(a.has("type"))  a.remove("type");
                }
            } catch (Exception e) {
                l( "Could not get boards directory: " + e.getMessage());
            }
        }
        return boards2;
    }

    public JSONArray AudioMedia(){

        // Add audio + video media lists. remove unecessary attributes to reduce ble message length.
        JSONArray audio = null;
        if (service.dlManager.dataDirectory == null) {
            l("Could not get media directory (null)");
        }
        else {
            try {

                 audio = new JSONArray(service.dlManager.dataDirectory.getJSONArray("audio").toString()) ;
                for (int i = 0; i < audio.length(); i++) {
                    JSONObject a = audio.getJSONObject(i);
                    if(a.has("URL"))  a.remove("URL");
                    if(a.has("ordinal"))  a.remove("ordinal");
                    if(a.has("Size"))   a.remove("Size");
                    if(a.has("Length"))  a.remove("Length");
                }

            } catch (Exception e) {
               l( "Could not get media directory: " + e.getMessage());
            }
        }
        return audio;
    }

    public JSONArray VideoMedia(){

        // Add audio + video media lists. remove unecessary attributes to reduce ble message length.
         JSONArray video = null;

        if (service.dlManager.dataDirectory == null) {
            l("Could not get media directory (null)");
        }
        else {
            try {

                video = new JSONArray(service.dlManager.dataDirectory.getJSONArray("video").toString());
                for (int i = 0; i < video.length(); i++) {
                    JSONObject v = video.getJSONObject(i);
                    if(v.has("URL")) v.remove("URL");
                    if(v.has("ordinal"))v.remove("ordinal");
                    if(v.has("Size"))v.remove("Size");
                    if(v.has("SpeachCue"))v.remove("SpeachCue");
                    if(v.has( "Length"))v.remove( "Length");
                }

            } catch (Exception e) {
                l( "Could not get media directory: " + e.getMessage());
            }
        }
        return video;
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
                        l("BBservice got getboards OnAction");
                        String error = null;
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                        } catch (Exception e) {
                            error = "Could not insert command: " + e.getMessage();
                        }
                        try{
                            response.put("boards", Boards());
                        }
                        catch(JSONException e){
                            l(e.getMessage());
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
                        service.bLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());

                        l("BBservice done getboards command");
                        l(response.toString());
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
                        l("BBservice got getaudio OnAction");
                        String error = null;
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                        } catch (Exception e) {
                            error = "Could not insert command: " + e.getMessage();
                        }

                        try{
                            if(AudioMedia()==null)
                                l("Empty Audio");
                            else
                                response.put("audio", AudioMedia());
                        }
                        catch(JSONException e){
                            l(e.getMessage());
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
                        service.bLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());

                        l("BBservice done getaudio command");
                        l(response.toString());
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
                        l("BBservice got getvideo OnAction");
                        String error = null;
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                        } catch (Exception e) {
                            error = "Could not insert command: " + e.getMessage();
                        }

                        try {
                            if(VideoMedia()==null)
                                l("Empty VideoMedia");
                            else
                                response.put("video", VideoMedia());
                        }
                        catch(JSONException e){
                            l(e.getMessage());
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
                        service.bLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());

                        l("BBservice done getvideo command");
                        l(response.toString());
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
                        l("BBservice got getall OnAction");
                        String error = null;
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                        } catch (Exception e) {
                            error = "Could not insert command: " + e.getMessage();
                        }
                        try{
                            response.put("boards", Boards());
                        }
                        catch(JSONException e){
                            l(e.getMessage());
                        }
                        try{
                            JSONArray audio = AudioMedia();
                            if(audio==null)
                                l("Empty Audio");
                            else
                                response.put("audio", audio);
                        }
                        catch(JSONException e){
                            l(e.getMessage());
                        }
                        try {
                            JSONArray video = VideoMedia();
                            if(video==null)
                                l("Empty VideoMedia");
                            else
                                response.put("video", video);
                        }
                        catch(JSONException e){
                            l(e.getMessage());
                        }

                        // Bluetooth devices
                        JSONArray btdevs = getBTDevs();
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

                        // Current board state
                        JSONObject state = getState();
                        try {
                            response.put("state", state);
                        } catch (Exception e) {
                            error = "Could not update state: " + e.getMessage();
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
                        service.bLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());

                        l("BBservice done getall command");
                        l(response.toString());

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
                        l("BBservice got EnableMaster command:" + payload.toString());
                        try {
                            boolean isMaster = payload.getBoolean("arg");
                            service.masterRemote.enableMaster(isMaster);

                        } catch (Exception e) {
                            l("error setting Master: " + e.getMessage());
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
                        l("BBservice got EnableGTFO command:" + payload.toString());
                        try {
                            boolean isGTFO = payload.getBoolean("arg");
                            service.gtfo.enableGTFO(isGTFO);

                        } catch (Exception e) {
                            l("error setting EnableGTFO: " + e.getMessage());
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
                        l("BBservice got BlockMaster command:" + payload.toString());
                        try {
                            boolean blockMaster = payload.getBoolean("arg");
                            service.boardState.blockMaster = blockMaster;

                        } catch (Exception e) {
                            l("error setting BlockMaster: " + e.getMessage());
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
                        l("BBservice got Video command:" + payload.toString());
                        try {
                            int track = payload.getInt("arg") + 1;
                            service.boardVisualization.setMode(track);
                        } catch (Exception e) {
                            l("error setting video track: " + e.getMessage());
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
                        l("BBservice got getwifi OnAction");
                        String error = null;
                        JSONObject response = new JSONObject();
                        try {
                            response.put("command", command);
                        } catch (Exception e) {
                            error = "Could not insert command: " + e.getMessage();
                        }

                        try {
                            JSONArray wifi = service.wifi.getScanResults();
                            if(wifi==null)
                                l("Empty wifi");
                            else
                                response.put("wifi", wifi);
                        }
                        catch(JSONException e){
                            l(e.getMessage());
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
                        service.bLEServer.tx(device,
                                (String.format("%s;", response.toString())).getBytes());

                        l("BBservice done wifi command");
                        l(response.toString());
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
                        l("BBservice got Wifi command:" + payload.toString());
                        try {
                            String SSIS = payload.getString("arg");
                            if (SSIS != "") {
                                String[] parts = SSIS.split("__");
                                service.wifi.setSSISAndPassword(parts[0], parts[1]);
                            }
                        } catch (Exception e) {
                            l("error setting wifi: " + e.getMessage());
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
                        l("BBservice got Volume command:" + payload.toString());
                        try {
                            int volume = payload.getInt("arg");
                            if (volume >= 0 && volume <= 100) {
                                service.musicPlayer.setBoardVolume(volume);
                            }
                        } catch (Exception e) {
                            l("error setting volume: " + e.getMessage());
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
                        l("BBservice got Audio command:" + payload.toString());
                        try {
                            int track = payload.getInt("arg");
                            service.musicPlayer.SetRadioChannel(track + 1);
                        } catch (Exception e) {
                            l("error setting audio track: " + e.getMessage());
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
                        l("BBservice get state command:" + payload.toString());
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
                        l("BBservice got Location command:" + payload.toString());
                        // Default to all if no age specified
                        int age = 0;
                        try {
                            age = payload.getInt("arg");
                        } catch (Exception e) {
                        }
                        if (age == 0) {
                            age = 999999999;
                        }
                        sendLocationResponse(command, device, age);
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
                        l("BBservice got BTScan command:" + payload.toString());
                        service.bluetoothConnManager.discoverDevices();
                        sendBTScanResponse(command, device);
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
                        l("BBservice got BTSelect command:" + payload.toString());
                        try {
                            String address = payload.getString("arg");
                            service.bluetoothConnManager.togglePairDevice(address);
                        } catch (Exception e) {
                            l("error setting BTSelect: " + e.getMessage());
                        }
                    }
                });
    }

    // Send state update response
    boolean sendStateResponse(String command, BluetoothDevice device) {
        String error = null;
        JSONObject response = new JSONObject();
        try {
            response.put("command", command);
            // Current board state
            JSONObject state = getState();
            response.put("state", state);
        } catch (Exception e) {
            error = "Could not update state: " + e.getMessage();
        }

        try {
            if (error != null) {
                response.put("error", error);
            } else {
                response.put("error", "");
            }
        } catch (Exception e) {
        }
        // Send payload back to requesting device
        service.bLEServer.tx(device,
                (String.format("%s;", response.toString())).getBytes());

        l("BBservice done sendStateResponse command");
        return (error != null);
    }

    JSONObject getState() {
        JSONObject state = new JSONObject();
        try {
            state.put("acn", service.musicPlayer.getRadioChannel() - 1);
            state.put("vcn", service.boardVisualization.getMode() - 1);
            state.put("v", service.musicPlayer.getBoardVolumePercent());
            state.put("b", service.boardState.batteryLevel);
            state.put("am", service.boardState.masterRemote);
            state.put("apkd", service.boardState.apkUpdatedDate.toString());
            state.put("apkv", service.boardState.version);
            state.put("ip", service.wifi.ipAddress);
            state.put("g", service.boardState.isGTFO);
            state.put("bm" , service.boardState.blockMaster);
            state.put("s", service.wifi.getConnectedSSID());
            state.put("c", service.wifi.SSID);
            state.put("p", service.wifi.password);

        } catch (Exception e) {
            l("Could not get state: " + e.getMessage());
        }
        return state;

    }

    JSONArray getBTDevs() {
        // Bluetooth devices
        JSONArray btdevs = service.bluetoothConnManager.getDeviceListJSON();
        if (btdevs == null) {
            l("Could not get bt devs (null)");
        } else {
            l("bt devs " + btdevs.toString());
        }
        return btdevs;
    }

    // Send location + state
    boolean sendLocationResponse(String command, BluetoothDevice device, int age) {
        String error = null;
        JSONObject response = new JSONObject();
        try {
            response.put("command", command);
            // Locations
            JSONArray locations = service.findMyFriends.getBoardLocationsJSON(age);
            if (locations == null) {
                error = "Could not get bt locations (null)";
            }
            if (locations != null) {
                response.put("locations", locations);

            }
            // Current board state
           // JSONObject state = getState();
           // response.put("state", state);

        } catch (Exception e) {
            error = "Could not get locations: " + e.getMessage();
        }
        // Send payload back to requesting device
        service.bLEServer.tx(device,
                (String.format("%s;", response.toString())).getBytes());
        l("BBservice done sendStateResponse command");
        return (error != null);
    }

    // Send BT Devices
    boolean sendBTScanResponse(String command, BluetoothDevice device) {
        String error = null;
        JSONObject response = new JSONObject();
        try {
            response.put("command", command);
            // Locations
            JSONArray btdevs = getBTDevs();
            if (btdevs == null) {
                error = "Could not get btdevs (null)";
            }
            if (btdevs != null) {
                response.put("btdevs", btdevs);

            }
            // Current board state
            JSONObject state = getState();
            response.put("state", state);

        } catch (Exception e) {
            error = "Could not get btdevs: " + e.getMessage();
        }
        // Send payload back to requesting device
        service.bLEServer.tx(device,
                (String.format("%s;", response.toString())).getBytes());
        l("BBservice done sendBTScanResponse command");
        return (error != null);
    }

    public void sendStateResponseAll() {
        //TODO: get to list of connected devices,
        //      else it will get grabbed on location poll
        //sendStateResponse("unsolicited", device);
    }

    public void l(String s) {
        Log.v(TAG, s);
        service.sendLogMsg(s);
    }

    public void d(String s) {
        Log.d(TAG, s);
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
