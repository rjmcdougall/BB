package com.richardmcdougall.bb;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import timber.log.Timber;

public class AllBoards {

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
        Thread t = new Thread(new Runnable() {
            public void run()
            {
                Thread.currentThread().setName("AllBoards");
                StartDownloadManager();
            }
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
                   Timber.d("Dir " + origDir);
                }
            }

        } catch (Throwable er) {
            Timber.e(er.getMessage()) ;
        }
    }

    public JSONArray MinimizedBoards() {
        JSONArray boards = dataBoards;
        JSONArray boards2 = null;
        if (boards == null) {
           Timber.d( "Could not get boards directory (null)");
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
               Timber.d( "Could not get boards directory: " + e.getMessage());
            }
        }
        return boards2;
    }

    public int getBoardAddress(String boardId) {

        JSONObject board;
        int myAddress = -1;

        try {

             if (dataBoards == null) {
               Timber.d("Could not find board address data");
            } else {
                for (int i = 0; i <  dataBoards.length(); i++) {
                    board =  dataBoards.getJSONObject(i);
                    if (board.getString("name").equals(boardId)) {
                        myAddress = board.getInt("address");
                    }
                }
            }
           Timber.d("Address " + String.valueOf(myAddress));
            return myAddress;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return myAddress;
        } catch (Exception e) {
            Timber.e(e.getMessage());
            return myAddress;
        }

    }

    public BoardState.TeensyType getDisplayTeensy(String boardId) {

        JSONObject board;
        BoardState.TeensyType displayTeensy = BoardState.TeensyType.teensy3;

        try {

            if (dataBoards == null) {
                Timber.d("Could not find board address data");
            } else {
                for (int i = 0; i <  dataBoards.length(); i++) {
                    board =  dataBoards.getJSONObject(i);
                    if (board.getString("name").equals(boardId)) {
                        if(!board.getString("displayTeensy").isEmpty())
                            displayTeensy = BoardState.TeensyType.valueOf(board.getString("displayTeensy"));
                    }
                }
            }
            Timber.d("Address " + String.valueOf(displayTeensy));
            return displayTeensy;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
            return displayTeensy;
        } catch (Exception e) {
            Timber.e(e.getMessage());
            return displayTeensy;
        }

    }

    public String boardAddressToName(int address) {

        JSONObject board;
        String boardId = "unknown";

        try {

             if ( dataBoards == null) {
               Timber.d("Could not find board name data");
            } else {
                for (int i = 0; i < dataBoards.length(); i++) {
                    board = dataBoards.getJSONObject(i);
                    if (board.getInt("address") == address) {
                        boardId = board.getString("name");
                    }
                }
            }

           Timber.d("Address " +  address + " To Name " + boardId);
            return boardId;

        } catch (JSONException e) {
           Timber.e(e.getMessage());
        } catch (Exception e) {
           Timber.e(e.getMessage());
        }

        return boardId;
    }

    public String boardAddressToColor(int address) {

        JSONObject board;
        String boardColor = "unknown";

        try {

            if ( dataBoards == null) {
               Timber.d("Could not find board color data");
            } else {
                for (int i = 0; i <  dataBoards.length(); i++) {
                    board =  dataBoards.getJSONObject(i);
                    if (board.getInt("address") == address) {
                        boardColor = board.getString("color");
                    }
                }
            }

           Timber.d("Color " + boardColor);
            return boardColor;
        } catch (JSONException e) {
           Timber.e(e.getMessage());
            return boardColor;
        } catch (Exception e) {
           Timber.e(e.getMessage());
            return boardColor;
        }
    }

    String getPublicName(String deviceID) {
        String name = deviceID;
        try {
            for (int i = 0; i <  dataBoards.length(); i++) {
                JSONObject obj =  dataBoards.getJSONObject(i);

                if (obj.has("bootName")) {
                    if (obj.getString("bootName").equals(deviceID)) {
                       Timber.d("Found bootName: " + deviceID);
                        name = obj.getString("name");

                       Timber.d("Found publicName: " + name);
                        return name;
                    }
                }
            }

           Timber.d("No special publicName found for: " + deviceID);
        } catch (JSONException e) {
           Timber.e( "Could not find publicName for: " + deviceID + " " + e.toString());
        }

        // We got here, we got nothing...
        return name;
    }

    public BurnerBoardUtil.BoardType getBoardType() {

        JSONObject board;
        BurnerBoardUtil.BoardType type = BurnerBoardUtil.BoardType.unknown;

        try {

            if ( dataBoards == null) {
               Timber.d("Could not find board type");
            } else {
                for (int i = 0; i < dataBoards.length(); i++) {
                    board =  dataBoards.getJSONObject(i);
                    if (board.getString("name").equals(service.boardState.BOARD_ID)) {
                        type = BurnerBoardUtil.BoardType.valueOf(board.getString("type"));
                    }
                }
            }
           Timber.d("Board Type " + String.valueOf(type));
            return type;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }
        finally {
            return type;
        }
    }

    public boolean GetNewBoardsJSON() {

        try {

            boolean returnValue = true;

           Timber.d(DIRECTORY_URL);

            long ddsz = -1;
            ddsz = FileHelpers.DownloadURL(DIRECTORY_URL, BOARDS_JSON_TEMP2_FILENAME, "Boards JSON", onProgressCallback, service.filesDir);

            if (ddsz < 0) {
               Timber.d("Unable to Download Boards JSON.  Likely because of no internet.  Sleeping for 5 seconds. ");
                returnValue = false;
            }
            else {
                new File(service.filesDir, BOARDS_JSON_TEMP2_FILENAME).renameTo(new File(service.filesDir, BOARDS_JSON_TMP_FILENAME));

                String dirTxt = FileHelpers.LoadTextFile(BOARDS_JSON_TMP_FILENAME, service.filesDir);
                JSONArray dir = new JSONArray(dirTxt);

               Timber.d("Downloaded Boards JSON: " + dirTxt);

                if (onProgressCallback != null) {
                    if (dataBoards == null || dir.length() != dataBoards.length()) {
                       Timber.d("A new board was discovered in Boards JSON.");
                        onProgressCallback.onVoiceCue("New Boards available for syncing.");
                    } else
                       Timber.d("A minor change was discovered in Boards JSON.");
                }

                // got new boards.  Update!
                dataBoards = dir;

                // now that you have the media, update the directory so the board can use it.
                new File(service.filesDir, BOARDS_JSON_TMP_FILENAME).renameTo(new File(service.filesDir, BOARDS_JSON_FILENAME));

                // XXX this may not be the right location for it, post refactor. but for now it's the best hook -jib
               Timber.d("Determining public name based on: " + service.boardState.DEVICE_ID);

                String newPN = getPublicName(service.boardState.DEVICE_ID);
                String existingPN = service.boardState.getPublicName();

               Timber.d("Checking if Public Name should be updated: Existing: " + existingPN + " New: " + newPN);
                if (newPN != null) {
                    if (existingPN == null || !existingPN.equals(newPN)) {
                        service.boardState.setPublicName(newPN);
                       Timber.d("Public name updated to: " + newPN);
                    }
                }

                if (dataBoards != null)
                    if (dir.toString().length() == dataBoards.toString().length()) {
                       Timber.d("No Changes to Boards JSON.");
                        returnValue = true;
                    }
            }


            return returnValue;
        } catch (JSONException jse) {
           Timber.e( jse.getMessage());
            return false;
        } catch (Throwable th) {
           Timber.e( th.getMessage());
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
