package com.richardmcdougall.bbcommon;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AllBoards {

    private String TAG = this.getClass().getSimpleName();

    private static final String BOARDS_JSON_FILENAME = "boards.json";
    private static final String BOARDS_JSON_TMP_FILENAME = "boards.json.tmp";
    private static final String BOARDS_JSON_TEMP2_FILENAME = "boardsTemp";
    private static final String DIRECTORY_URL = "https://us-central1-burner-board.cloudfunctions.net/boards/";
    private static final String CREATE_URL = "https://us-central1-burner-board.cloudfunctions.net/boards/CreateBoard/";


    private Context context = null;
    public JSONArray dataBoards;
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private String filesDir = null;
    private TextToSpeech voice = null;

    public AllBoards(Context context, TextToSpeech voice) {
        this.context = context;

        filesDir = context.getFilesDir().getAbsolutePath();
        LoadInitialBoardsDirectory();
        this.voice = voice;

        // wait 5 seconds to hopefully get wifi before starting the download.
        Runnable initialCheckForBoards = () -> initialCheck();
        Runnable periodicCheckForBoards = () -> periodicCheck();

        sch.scheduleWithFixedDelay(initialCheckForBoards, 3, 5, TimeUnit.SECONDS);
        sch.scheduleWithFixedDelay(periodicCheckForBoards, 1, 1, TimeUnit.MINUTES);

        this.onProgressCallback = new FileHelpers.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    voice.speak("Downloading " + file + "", TextToSpeech.QUEUE_ADD, null,"downloading");
                    lastTextTime = curTime;
                    BLog.d(TAG, "Downloading " + file + ", " + percent + " Percent");
                }
            }

            public void onVoiceCue(String msg) {
                //voice.speak(msg, TextToSpeech.QUEUE_ADD, null,"Download Message");
            }
        };
    }


    boolean downloadSuccessBoards = false;
    private int  maxFailedChecks = 60;
    private int currentFailedChecks = 0;

    private void initialCheck(){
        currentFailedChecks++;

        if (!downloadSuccessBoards && currentFailedChecks < maxFailedChecks)
            downloadSuccessBoards = GetNewBoardsJSON();
    }

    private void periodicCheck(){
        downloadSuccessBoards = GetNewBoardsJSON();
    }

    public FileHelpers.OnDownloadProgressType onProgressCallback = null;

    public void LoadInitialBoardsDirectory() {
        try {
            File[] flist = new File(filesDir).listFiles();
            if (flist != null) {

                // if files are no longer referenced in the Data Directory, delete them.
                String origDir = FileHelpers.LoadTextFile(BOARDS_JSON_FILENAME, filesDir);
                if (origDir != null) {
                    JSONArray dir = new JSONArray(origDir);
                    dataBoards = dir;
                    BLog.d(TAG, "Dir " + origDir);
                }
            }

        } catch (Throwable er) {
            BLog.e(TAG, er.getMessage());
        }
    }

    public JSONArray MinimizedBoards() {
        JSONArray boards = dataBoards;
        JSONArray boards2 = null;
        if (boards == null) {
            BLog.d(TAG, "Could not get boards directory (null)");
        }
        if (boards != null) {
            try {

                boards2 = new JSONArray();
                for (int i = 0; i < boards.length(); i++) {
                    JSONObject original = boards.getJSONObject(i);
                    
                    // Filter out the special 'default' board from external consumers
                    if (original.has("name") && "default".equals(original.getString("name"))) {
                        continue; // Skip the default board
                    }
                    
                    // Create a copy and remove sensitive fields
                    JSONObject a = new JSONObject(original.toString());
                    if (a.has("address")) a.remove("address");
                    if (a.has("bootName")) a.remove("bootName");
                    if (a.has("isProfileGlobal")) a.remove("isProfileGlobal");
                    if (a.has("isProfileGlobal2")) a.remove("isProfileGlobal2");
                    if (a.has("profile")) a.remove("profile");
                    if (a.has("profile2")) a.remove("profile2");
                    if (a.has("type")) a.remove("type");
                    if (a.has("displayTeensy")) a.remove("displayTeensy");
                    if (a.has("displayDebug")) a.remove("displayDebug");
                    if (a.has("createdDate")) a.remove("createdDate");
                    if (a.has("videoContrastMultiplier")) a.remove("videoContrastMultiplier");
                    
                    boards2.put(a);
                }
            } catch (Exception e) {
                BLog.d(TAG, "Could not get boards directory: " + e.getMessage());
            }
        }
        return boards2;
    }

    public String getBoardColor(String boardID) {

        JSONObject board;
        String color = "";

        try {
            board = getBoardByID(boardID);
            if (board != null && board.has("color")) {
                color = board.getString("color");
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error getting board color for " + boardID + ": " + e.getMessage());
        }
        return color;
    }

    public int getBoardAddress(String boardID) {

        JSONObject board;
        int myAddress = -1;

        try {
            board = getBoardByID(boardID);
            if (board != null && board.has("address")) {
                myAddress = board.getInt("address");
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error getting board address for " + boardID + ": " + e.getMessage());
        }
        return myAddress;
    }

    public String getProfile(String boardID) {

        JSONObject board;
        String profile = "";

        try {
            board = getBoardByID(boardID);
            if (board != null && board.has("profile")) {
                profile = board.getString("profile");
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error getting profile for " + boardID + ": " + e.getMessage());
        }
        return profile;
    }

    public BoardState.TeensyType getDisplayTeensy(String boardID) {

        JSONObject board;
        BoardState.TeensyType displayTeensy = BoardState.TeensyType.teensy3;

        try {
            board = getBoardByID(boardID);
            if (board != null && board.has("displayTeensy"))
                displayTeensy = BoardState.TeensyType.valueOf(board.getString("displayTeensy"));
        } catch (Exception e) {
            BLog.e(TAG, "Error getting display teensy for " + boardID + ": " + e.getMessage());
        }
        return displayTeensy;
    }

    public String boardAddressToName(int address) {

        JSONObject board;
        String boardId = "unknown";

        try {

            if (dataBoards == null) {
                BLog.d(TAG, "Could not find board name data");
            } else {
                for (int i = 0; i < dataBoards.length(); i++) {
                    board = dataBoards.getJSONObject(i);
                    // Check if board has required fields before accessing them
                    if (board != null && board.has("address") && board.has("name")) {
                        if (board.getInt("address") == address) {
                            String name = board.getString("name");
                            // Filter out the special 'default' board from general consumers
                            if (name != null && !"default".equals(name)) {
                                boardId = name;
                                break; // Found the board, exit loop
                            }
                        }
                    }
                }
            }
            return boardId;

        } catch (Exception e) {
            BLog.e(TAG, "Error in boardAddressToName: " + e.getMessage());
        }

        return boardId;
    }

    public int videoContrastMultiplier(String boardID) {

        JSONObject board;
        int videoContrastMultiplier = 0;

        try {
            board = getBoardByID(boardID);
            if (board != null && board.has("videoContrastMultiplier")) {
                videoContrastMultiplier = board.getInt("videoContrastMultiplier");
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error getting video contrast multiplier for " + boardID + ": " + e.getMessage());
        }
        return videoContrastMultiplier;
    }

    public int pixelSlow(String boardID) {

        JSONObject board;
        int pixelSlow = 0;

        try {
            board = getBoardByID(boardID);
            if (board != null && board.has("pixelSlow")) {
                pixelSlow = board.getInt("pixelSlow");
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error getting pixel slow for " + boardID + ": " + e.getMessage());
        }
        return pixelSlow;
    }
    public int targetAPKVersion(String boardID) {

        JSONObject board;
        int targetAPKVersion = 0;

        try {
            board = getBoardByID(boardID);
            if (board != null && board.has("targetAPKVersion")) {
                targetAPKVersion = board.getInt("targetAPKVersion");
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error getting target APK version for " + boardID + ": " + e.getMessage());
        }
        return targetAPKVersion;
    }

    public boolean displayDebug(String boardID) {

        JSONObject board;
        boolean displayDebug = false;

        try {
            board = getBoardByID(boardID);
            if (board != null && board.has("displayDebug")) {
                displayDebug = board.getBoolean("displayDebug");
            }
        } catch (Exception e) {
            BLog.e(TAG, "Error getting display debug for " + boardID + ": " + e.getMessage());
        }
        return displayDebug;
    }

    public JSONObject getMeshParams(String boardID) {

        JSONObject board;
        JSONObject meshParams = null;

        try {
            // Try to get meshparams from the specified board
            board = getBoardByID(boardID);
            if (board != null && board.has("meshParams")) {
                meshParams = getMeshParamsFromBoard(board);
                if (meshParams != null) {
                    BLog.d(TAG, "Found meshParams for board: " + boardID);
                    return meshParams;
                }
            }
            
            // If not found, try to get meshparams from the special 'default' board
            JSONObject defaultBoard = getBoardByID("default");
            if (defaultBoard != null && defaultBoard.has("meshParams")) {
                meshParams = getMeshParamsFromBoard(defaultBoard);
                if (meshParams != null) {
                    BLog.d(TAG, "Using meshParams from 'default' board for: " + boardID);
                    return meshParams;
                }
            }
            
            // If still not found, use hard-coded defaults
            BLog.d(TAG, "No meshparams found for board: " + boardID + ", using hard-coded defaults");
            meshParams = getDefaultMeshParams();
            
        } catch (Exception e) {
            BLog.e(TAG, "Error getting meshparams: " + e.getMessage());
            // Return default meshparams on error
            meshParams = getDefaultMeshParams();
        }
        return meshParams;
    }
    
    private JSONObject getMeshParamsFromBoard(JSONObject board) {
        try {
            Object meshParamsObj = board.get("meshParams");
            
            // Check if it's already a JSONObject
            if (meshParamsObj instanceof JSONObject) {
                return (JSONObject) meshParamsObj;
            }
            
            // Check if it's a String that needs to be parsed
            if (meshParamsObj instanceof String) {
                String meshParamsStr = (String) meshParamsObj;
                // Only try to parse if it looks like JSON (starts with { or [)
                if (meshParamsStr.trim().startsWith("{") || meshParamsStr.trim().startsWith("[")) {
                    return new JSONObject(meshParamsStr);
                } else {
                    BLog.w(TAG, "meshparams is a string but doesn't appear to be JSON: " + meshParamsStr);
                    return null;
                }
            }
            
            BLog.w(TAG, "meshparams is of unexpected type: " + meshParamsObj.getClass().getSimpleName());
            return null;
            
        } catch (Exception e) {
            BLog.e(TAG, "Error parsing meshparams from board: " + e.getMessage());
            return null;
        }
    }

    private JSONObject getDefaultMeshParams() {
        JSONObject defaultParams = new JSONObject();
        try {
            defaultParams.put("meshupdateinterval", 900); // 30 seconds in milliseconds
            defaultParams.put("channelnum", 1); // Primary channel
            defaultParams.put("modempreset", "MEDIUM_SLOW"); // Default modem preset
            defaultParams.put("hoplimit", 7); // Default hop limit
            defaultParams.put("channel1name", "Blinkything"); // Private channel name
            defaultParams.put("channel1key", "AQ=="); // Base64 encoded key (null key)
            defaultParams.put("positionaccuracy", 32); // Position accuracy in meters
            defaultParams.put("supersleep", 43200); // Super deep sleep in seconds (12 hours)
        } catch (Exception e) {
            BLog.e(TAG, "Error creating default mesh params: " + e.getMessage());
        }
        return defaultParams;
    }

    public BoardState.BoardType getBoardType(String boardID) {

        JSONObject board;
        BoardState.BoardType type = BoardState.BoardType.unknown;

        try {
            board = getBoardByID(boardID);
            if (board != null && board.has("type")) {
                type = BoardState.BoardType.valueOf(board.getString("type"));
            }
        } catch (Exception e) {
            BLog.w(TAG, "Error getting board type for " + boardID + ": " + e.getMessage());
        }
        return type;
    }

    JSONObject getBoardByID(String boardID){

        JSONObject board = null;

        try {
            for (int i = 0; i < dataBoards.length(); i++) {
                JSONObject obj = dataBoards.getJSONObject(i);

                if (obj.has("name")) {
                    if (obj.getString("name").equals(boardID)) {
                        board = obj;
                    }
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, "Could not find board for: " + boardID + " " + e.toString());
        }

        return board;
    }

    public void createBoard(String deviceID){

        Runnable checkForBoards = () -> httpCreateBoard(deviceID);

        sch.schedule(checkForBoards, 1, TimeUnit.SECONDS);

        Runnable downloadUpdates = () -> GetNewBoardsJSON();

        sch.schedule(downloadUpdates, 2, TimeUnit.SECONDS);

    }

    public void httpCreateBoard(String deviceID){
        String content = "", line;
        try{
            URL url = new URL(CREATE_URL + deviceID);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            while ((line = rd.readLine()) != null) {
                content += line + "\n";
            }
        }
        catch (Exception e){
            BLog.e(TAG, "Could not create board for: " + deviceID + " " + e.toString());
        }

    }

    public JSONObject getBoardByDeviceID(String deviceID){

        JSONObject board = null;

        try {
            for (int i = 0; i < dataBoards.length(); i++) {
                JSONObject obj = dataBoards.getJSONObject(i);

                if (obj.has("bootName")) {
                    if (obj.getString("bootName").equals(deviceID)) {
                        board = obj;
                    }
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, "Could not find board for: " + deviceID + " " + e.toString());
        }

        return board;
    }

    String getBOARD_ID(String deviceID) {

        String name = deviceID;
        try {
            JSONObject board = getBoardByDeviceID(deviceID);
            if (board != null && board.has("name")) {
                name = board.getString("name");
            }
        } catch (Exception e) {
            BLog.e(TAG, "Could not find publicName for: " + deviceID + " " + e.toString());
        }

        return name;
    }

    public boolean GetNewBoardsJSON() {

        try {

            boolean returnValue = true;

            BLog.d(TAG, DIRECTORY_URL);

            long ddsz = -1;
            ddsz = FileHelpers.DownloadURL(DIRECTORY_URL, BOARDS_JSON_TEMP2_FILENAME, "Boards JSON", onProgressCallback, this.filesDir);

            if (ddsz < 0) {
                BLog.d(TAG, "Unable to Download Boards JSON.  Likely because of no internet.  Sleeping for 5 seconds. ");
                returnValue = false;
            } else {
                new File(filesDir, BOARDS_JSON_TEMP2_FILENAME).renameTo(new File(filesDir, BOARDS_JSON_TMP_FILENAME));

                String dirTxt = FileHelpers.LoadTextFile(BOARDS_JSON_TMP_FILENAME, filesDir);
                JSONArray dir = new JSONArray(dirTxt);

                BLog.d(TAG, "Downloaded Boards JSON: " + dirTxt);

                if (onProgressCallback != null) {
                    if (dataBoards == null || dir.length() != dataBoards.length()) {
                        BLog.d(TAG, "A new board was discovered in Boards JSON.");
                        onProgressCallback.onVoiceCue("New Boards available for syncing.");
                    } else
                        BLog.d(TAG, "A minor change was discovered in Boards JSON.");
                }

                // got new boards.  Update!
                dataBoards = dir;

                // now that you have the media, update the directory so the board can use it.
                new File(filesDir, BOARDS_JSON_TMP_FILENAME).renameTo(new File(filesDir, BOARDS_JSON_FILENAME));

                if (dataBoards != null)
                    if (dir.toString().length() == dataBoards.toString().length()) {
                        BLog.d(TAG, "No Changes to Boards JSON.");
                        returnValue = true;
                    }
            }

            return returnValue;
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
            return false;
        }
    }
}
