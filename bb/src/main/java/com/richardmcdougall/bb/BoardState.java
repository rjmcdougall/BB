package com.richardmcdougall.bb;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BoardState {

    public static String BOARD_ID = "";
    public static String DEVICE_ID = "";
    public static final String publicNameDir = "/data/data/com.richardmcdougall.bb/files";
    public static final String publicNameFile = "publicName.txt";
    public JSONArray dataBoards;
    public JSONObject dataDirectory;
    private static final String TAG = "BoardState";
    private BBService service = null;

    private void d(String logMsg) {
        if (DebugConfigs.DEBUG_BOARD_STATE) {
            Log.d(TAG, logMsg);
        }
    }

    private void e(String logMsg) {
        Log.e(TAG, logMsg);
    }

    BoardState(BBService service) {
        this.service = service;

        String serial = Build.SERIAL;
        String publicName = "";

        if (DebugConfigs.OVERRIDE_PUBLIC_NAME != "")
            publicName = DebugConfigs.OVERRIDE_PUBLIC_NAME;
        else
            publicName = getPublicName();

        if (BurnerBoardUtil.kIsRPI) {
            DEVICE_ID = "pi" + serial.substring(Math.max(serial.length() - 6, 0),
                    serial.length());
        } else if (BurnerBoardUtil.kIsNano) {
            DEVICE_ID = "npi" + serial.substring(Math.max(serial.length() - 5, 0),
                    serial.length());
        } else {
            DEVICE_ID = Build.MODEL;
        }

        BOARD_ID = (publicName == null || publicName.equals("")) ? DEVICE_ID : publicName;
    }

    public boolean setPublicName(String name) {
        try {
            FileWriter fw = new FileWriter(publicNameDir + "/" + publicNameFile);
            fw.write(name);
            fw.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    // Cargo culted from Download manager.
    public String getPublicName() {

        try {
            File f = new File(publicNameDir, publicNameFile);
            if (!f.exists())
                return null;
            InputStream is = null;
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();

            // We only expect one line - this is a shortcut! -jib
            while (line != null) {
                sb.append(line);
                line = buf.readLine();
            }

            return sb.toString();

        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }


    String GetAudioFile(int index) {
        try {
            String fn = service.filesDir + "/" + GetAudio(index).getString("localName");
            return fn;
        } catch (JSONException e) {
            e(e.getMessage());
            return null;
        }
    }


    String GetAudioFileLocalName(int index) {
        if (index >= 0 && index < GetTotalAudio()) {
            try {
                String fn = GetAudio(index).getString("localName");
                return fn;
            } catch (JSONException e) {
                e(e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }


    String GetVideoFileLocalName(int index) {
        if (index >= 0 && index < GetTotalVideo()) {
            try {
                String fn = "";
                if (GetVideo(index).has("friendlyName")) {
                    fn = GetVideo(index).getString("friendlyName");
                } else {
                    if (GetVideo(index).has("algorithm"))
                        fn = GetVideo(index).getString("algorithm");
                    else
                        fn = GetVideo(index).getString("localName");
                }
                return fn;
            } catch (JSONException e) {
                e(e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String GetVideoFile(int index) {
        try {
            String fn = service.filesDir + "/" + GetVideo(index).getString("localName");
            return fn;
        } catch (JSONException e) {
            e(e.getMessage());
            return null;
        }
    }


    int GetTotalAudio() {
        if (dataDirectory == null)
            return 0;
        else {
            try {
                //Log.d(TAG, dataDirectory.getJSONArray("audio").length() + " audio files");
                return dataDirectory.getJSONArray("audio").length();
            } catch (JSONException e) {
                e(e.getMessage());
                return 0;
            }
        }
    }

    public int GetTotalVideo() {
        if (dataDirectory == null)
            return 0;
        else {
            try {
                //Log.d(TAG, dataDirectory.getJSONArray("audio").length() + " video files");
                return dataDirectory.getJSONArray("video").length();
            } catch (JSONException e) {
                e(e.getMessage());
                return 0;
            }
        }
    }


    JSONObject GetVideo(int index) {
        if (dataDirectory == null)
            return null;
        if (dataDirectory.has("video")) {
            try {
                return dataDirectory.getJSONArray("video").getJSONObject(index);
            } catch (JSONException e) {
                e(e.getMessage());
                return null;
            }
        } else
            return null;
    }

    String GetAlgorithm(int index) {
        if (dataDirectory == null)
            return null;
        if (dataDirectory.has("video")) {
            try {
                return dataDirectory.getJSONArray("video").getJSONObject(index).getString("algorithm");
            } catch (JSONException e) {
                e(e.getMessage());
                return null;
            }
        } else
            return null;
    }

    JSONObject GetAudio(int index) {
        if (dataDirectory == null)
            return null;
        if (dataDirectory.has("audio")) {
            try {
                return dataDirectory.getJSONArray("audio").getJSONObject(index);
            } catch (JSONException e) {
                e(e.getMessage());
                return null;
            }
        } else
            return null;
    }

    long GetAudioLength(int index) {
        try {
            return GetAudio(index).getLong("Length");
        } catch (JSONException e) {
            e(e.getMessage());
            return 1000;   // return a dummy value
        }
    }


}
