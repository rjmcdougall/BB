package com.richardmcdougall.bb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class AllBoards {

    private String TAG = this.getClass().getSimpleName();

    private static final String BOARDS_JSON_FILENAME = "boards.json";
    private static final String BOARDS_JSON_TMP_FILENAME = "boards.json.tmp";
    private static final String BOARDS_JSON_TEMP2_FILENAME = "boardsTemp";
    private static final String DIRECTORY_URL = "https://us-central1-burner-board.cloudfunctions.net/boards/";

    private BBService service = null;
    public MediaManager.OnDownloadProgressType onProgressCallback = null;
    public JSONArray dataBoards;

    public AllBoards(BBService service) {
        this.service = service;
        LoadInitialBoardsDirectory();
    }

    void Run() {
        Thread t = new Thread(() -> {
            Thread.currentThread().setName("AllBoards");
            StartDownloadManager();
        });
        t.start();
    }

    public void LoadInitialBoardsDirectory() {
        try {
            File[] flist = new File(service.filesDir).listFiles();
            if (flist != null) {

                // if files are no longer referenced in the Data Directory, delete them.
                String origDir = FileHelpers.LoadTextFile(BOARDS_JSON_FILENAME, service.filesDir);
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
                }
            } catch (Exception e) {
                BLog.d(TAG, "Could not get boards directory: " + e.getMessage());
            }
        }
        return boards2;
    }

    public int getBoardAddress(String boardId) {

        JSONObject board;
        int myAddress = -1;

        try {

            if (dataBoards == null) {
                BLog.d(TAG, "Could not find board address data");
            } else {
                for (int i = 0; i < dataBoards.length(); i++) {
                    board = dataBoards.getJSONObject(i);
                    if (board.getString("name").equals(boardId)) {
                        myAddress = board.getInt("address");
                    }
                }
            }
            BLog.d(TAG, "Address " + String.valueOf(myAddress));
            return myAddress;
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return myAddress;
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
            return myAddress;
        }

    }

    public BoardState.TeensyType getDisplayTeensy(String boardId) {

        JSONObject board;
        BoardState.TeensyType displayTeensy = BoardState.TeensyType.teensy3;

        try {

            if (dataBoards == null) {
                BLog.d(TAG, "Could not find board address data");
            } else {
                for (int i = 0; i < dataBoards.length(); i++) {
                    board = dataBoards.getJSONObject(i);
                    if (board.getString("name").equals(boardId)) {
                        if (board.has("displayTeensy"))
                            displayTeensy = BoardState.TeensyType.valueOf("displayTeensy");
                    }
                }
            }
            return displayTeensy;
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return displayTeensy;
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
            return displayTeensy;
        }

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

            BLog.d(TAG, "Address " + address + " To Name " + boardId);
            return boardId;

        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }

        return boardId;
    }

    public String boardAddressToColor(int address) {

        JSONObject board;
        String boardColor = "white";

        try {

            if (dataBoards == null) {
                BLog.d(TAG, "Could not find board color data");
            } else {
                for (int i = 0; i < dataBoards.length(); i++) {
                    board = dataBoards.getJSONObject(i);
                    if (board.getInt("address") == address) {
                        boardColor = board.getString("color");
                    }
                }
            }

            BLog.d(TAG, "Color " + boardColor);
            return boardColor;
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return boardColor;
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
            return boardColor;
        }
    }

    public BoardState.BoardType getBoardType(String boardID) {

        JSONObject board;
        BoardState.BoardType type = BoardState.BoardType.unknown;

        try {

            if (dataBoards == null) {
                BLog.d(TAG, "Could not find board type");
            } else {
                for (int i = 0; i < dataBoards.length(); i++) {
                    board = dataBoards.getJSONObject(i);
                    if (board.getString("name").equals(boardID)) {
                        type = BoardState.BoardType.valueOf(board.getString("type"));
                    }
                }
            }
            BLog.d(TAG, "Board Type " + String.valueOf(type));
            return type;
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        } finally {
            return type;
        }
    }

    String getBOARD_ID(String deviceID) {
        String name = deviceID;
        try {
            for (int i = 0; i < dataBoards.length(); i++) {
                JSONObject obj = dataBoards.getJSONObject(i);

                if (obj.has("bootName")) {
                    if (obj.getString("bootName").equals(deviceID)) {
                        BLog.d(TAG, "Found bootName: " + deviceID);
                        name = obj.getString("name");

                        BLog.d(TAG, "Found publicName: " + name);
                        return name;
                    }
                }
            }

            BLog.d(TAG, "No special publicName found for: " + deviceID);
        } catch (JSONException e) {
            BLog.e(TAG, "Could not find publicName for: " + deviceID + " " + e.toString());
        }

        // We got here, we got nothing...
        return name;
    }

    public boolean GetNewBoardsJSON() {

        try {

            boolean returnValue = true;

            BLog.d(TAG, DIRECTORY_URL);

            long ddsz = -1;
            ddsz = FileHelpers.DownloadURL(DIRECTORY_URL, BOARDS_JSON_TEMP2_FILENAME, "Boards JSON", onProgressCallback, service.filesDir);

            if (ddsz < 0) {
                BLog.d(TAG, "Unable to Download Boards JSON.  Likely because of no internet.  Sleeping for 5 seconds. ");
                returnValue = false;
            } else {
                new File(service.filesDir, BOARDS_JSON_TEMP2_FILENAME).renameTo(new File(service.filesDir, BOARDS_JSON_TMP_FILENAME));

                String dirTxt = FileHelpers.LoadTextFile(BOARDS_JSON_TMP_FILENAME, service.filesDir);
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
                new File(service.filesDir, BOARDS_JSON_TMP_FILENAME).renameTo(new File(service.filesDir, BOARDS_JSON_FILENAME));

                if (dataBoards != null)
                    if (dir.toString().length() == dataBoards.toString().length()) {
                        BLog.d(TAG, "No Changes to Boards JSON.");
                        returnValue = true;
                    }
            }

            return returnValue;
        } catch (JSONException jse) {
            BLog.e(TAG, jse.getMessage());
            return false;
        } catch (Throwable th) {
            BLog.e(TAG, th.getMessage());
            return false;
        }
    }

    public void StartDownloadManager() {

        try {
            boolean downloadSuccessBoards = false;
            int i = 0;

            // On boot it can take some time to get an internet connection.  Check every 5 seconds for 5 minutes
            while (!downloadSuccessBoards && i < 60) { // try this for 5 minutes.
                i++;

                if (!downloadSuccessBoards)
                    downloadSuccessBoards = GetNewBoardsJSON();

                Thread.sleep(5000);   // no internet, wait 5 seconds before we try again
            }

            // After the first boot check periodically in case the profile has changed.
            while (true) {
                GetNewBoardsJSON();
                Thread.sleep(120000);   // no internet, wait 2 minutes before we try again
            }
        } catch (Throwable th) {
            if (onProgressCallback != null)
                onProgressCallback.onVoiceCue(th.getMessage());
            try {
                Thread.sleep(10000);   // wait 10 second if unexpected error
            } catch (Throwable er) {

            }
        }
    }
}
