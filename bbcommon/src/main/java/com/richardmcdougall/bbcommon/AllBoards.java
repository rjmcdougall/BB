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
        sch.scheduleWithFixedDelay(periodicCheckForBoards, 1, 2, TimeUnit.MINUTES);

        this.onProgressCallback = new FileHelpers.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    voice.speak("Downloading " + file + ", " + percent + " Percent", TextToSpeech.QUEUE_ADD, null,"downloading");
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

                boards2 = new JSONArray(boards.toString());
                for (int i = 0; i < boards2.length(); i++) {
                    JSONObject a = boards2.getJSONObject(i);
                    if (a.has("address")) a.remove("address");
                    if (a.has("isProfileGlobal")) a.remove("isProfileGlobal");
                    if (a.has("profile")) a.remove("profile");
                    if (a.has("isProfileGlobal2")) a.remove("isProfileGlobal2");
                    if (a.has("profile2")) a.remove("profile2");
                    if (a.has("type")) a.remove("type");
                    if (a.has("displayTeensy")) a.remove("displayTeensy");
                }
            } catch (Exception e) {
                BLog.d(TAG, "Could not get boards directory: " + e.getMessage());
            }
        }
        return boards2;
    }

    public int getBoardAddress(String boardID) {

        JSONObject board;
        int myAddress = -1;

        try {
            board = getBoardByID(boardID);
            myAddress = board.getInt("address");
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
        return myAddress;
    }

    public String getProfile(String boardID) {

        JSONObject board;
        String profile = "";

        try {
            board = getBoardByID(boardID);
            profile = board.getString("profile");
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
        return profile;
    }

    public BoardState.TeensyType getDisplayTeensy(String boardID) {

        JSONObject board;
        BoardState.TeensyType displayTeensy = BoardState.TeensyType.teensy3;

        try {
            board = getBoardByID(boardID);
            if (board.has("displayTeensy"))
                displayTeensy = BoardState.TeensyType.valueOf(board.getString("displayTeensy"));
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
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
                    if (board.getInt("address") == address) {
                        boardId = board.getString("name");
                    }
                }
            }
            return boardId;

        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }

        return boardId;
    }

    public int videoContrastMultiplier(String boardID) {

        JSONObject board;
        int videoContrastMultiplier = 0;

        try {
            board = getBoardByID(boardID);
            videoContrastMultiplier = board.getInt("videoContrastMultiplier");
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
        return videoContrastMultiplier;
    }

    public int targetAPKVersion(String boardID) {

        JSONObject board;
        int targetAPKVersion = 0;

        try {
            board = getBoardByID(boardID);
            targetAPKVersion = board.getInt("targetAPKVersion");

        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
        return targetAPKVersion;
    }

    public BoardState.BoardType getBoardType(String boardID) {

        JSONObject board;
        BoardState.BoardType type = BoardState.BoardType.unknown;

        try {
            board = getBoardByID(boardID);
            type = BoardState.BoardType.valueOf(board.getString("type"));
        } catch (Exception e) {
            BLog.w(TAG, e.getMessage());
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
            name = board.getString("name");
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
