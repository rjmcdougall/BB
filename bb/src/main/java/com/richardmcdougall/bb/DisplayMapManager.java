package com.richardmcdougall.bb;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.richardmcdougall.bb.board.TranslationMap;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.FileHelpers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DisplayMapManager {

    private String TAG = this.getClass().getSimpleName();

    private String displauMapJSONFilename;
    private static final String DISPLAY_MAP_TMP_FILENAME = "displayMap.json.tmp";
    private static final String DISPLAY_MAP_TEMP2_FILENAME = "displayMapTemp";
    private static final String DIRECTORY_URL = "https://storage.googleapis.com/burner-board/BurnerBoardDisplayTranslation/";

    private BBService service;
    public JSONArray dataDisplayMap;
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private String filesDir;
    boolean downloadSuccessDisplayMap = false;
    private int  maxFailedChecks = 60;
    private int currentFailedChecks = 0;
    public FileHelpers.OnDownloadProgressType onProgressCallback = null;
    public ArrayList<TranslationMap> displayMap = new ArrayList<>();


    public DisplayMapManager(BBService service) {
        this.service = service;


        filesDir = this.service.context.getFilesDir().getAbsolutePath();

        this.displauMapJSONFilename = this.service.boardState.BOARD_ID + ".json";
        LoadInitialDisplayMap();

        // wait 5 seconds to hopefully get wifi before starting the download.
        Runnable initialCheckForDisplayMap = () -> initialCheck();
        Runnable periodicCheckForDisplayMap = () -> periodicCheck();

        sch.scheduleWithFixedDelay(initialCheckForDisplayMap, 3, 5, TimeUnit.SECONDS);
        sch.scheduleWithFixedDelay(periodicCheckForDisplayMap, 1, 2, TimeUnit.MINUTES);

        this.onProgressCallback = new FileHelpers.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    service.speak("Downloading " + file + ", " + percent + " Percent", "downloading display");
                    lastTextTime = curTime;
                    BLog.d(TAG, "Downloading " + file + ", " + percent + " Percent");
                }
            }

            public void onVoiceCue(String msg) {
                //voice.speak(msg, TextToSpeech.QUEUE_ADD, null,"Download Message");
            }
        };
    }

    private void initialCheck(){
        currentFailedChecks++;

        if (!downloadSuccessDisplayMap && currentFailedChecks < maxFailedChecks)
            downloadSuccessDisplayMap = GetNewDisplayMapJSON();
    }

    private void periodicCheck(){
        downloadSuccessDisplayMap = GetNewDisplayMapJSON();
    }

    private void LoadInitialDisplayMap() {
        try {
            File[] flist = new File(filesDir).listFiles();
            if (flist != null) {

                // if files are no longer referenced in the Data Directory, delete them.
                String origDir = FileHelpers.LoadTextFile(this.displauMapJSONFilename, filesDir);
                if (origDir != null) {
                    JSONArray dir = new JSONArray(origDir);
                    dataDisplayMap = dir;
                    CreateDisplayMapFromJSON();
                    BLog.d(TAG, "Dir " + origDir.substring(0,1000));
                }
            }

        } catch (Throwable er) {
            BLog.e(TAG, er.getMessage());
        }
    }

    private boolean GetNewDisplayMapJSON() {

        try {

            boolean returnValue = true;

            BLog.d(TAG, DIRECTORY_URL);

            long ddsz = -1;
            ddsz = FileHelpers.DownloadURL(DIRECTORY_URL + this.displauMapJSONFilename, DISPLAY_MAP_TEMP2_FILENAME, "Display Map JSON", onProgressCallback, this.filesDir);

            if (ddsz < 0) {
                BLog.d(TAG, "Unable to Download Display Map JSON.  Likely because of no internet.  Sleeping for 5 seconds. ");
                returnValue = false;
            } else {
                new File(filesDir, DISPLAY_MAP_TEMP2_FILENAME).renameTo(new File(filesDir, DISPLAY_MAP_TMP_FILENAME));

                String dirTxt = FileHelpers.LoadTextFile(DISPLAY_MAP_TMP_FILENAME, filesDir);
                JSONArray dir = new JSONArray(dirTxt);

                BLog.d(TAG, "Downloaded Display Map JSON: " + dirTxt.substring(0,10));

                if (onProgressCallback != null) {
                    if (dataDisplayMap == null || dir.length() != dataDisplayMap.length()) {
                        BLog.d(TAG, "A new Display Map was discovered.");
                        onProgressCallback.onVoiceCue("New Display Map available for syncing.");
                    } else
                        BLog.d(TAG, "A minor change was discovered in Display Map JSON.");
                }

                // got new display map.  Update!
                dataDisplayMap = dir;
                CreateDisplayMapFromJSON();

                // Update the directory so the board can use it.
                new File(filesDir, DISPLAY_MAP_TMP_FILENAME).renameTo(new File(filesDir, this.displauMapJSONFilename));

                if (dataDisplayMap != null)
                    if (dir.toString().length() == dataDisplayMap.toString().length()) {
                        BLog.d(TAG, "No Changes to Display Map JSON.");
                        returnValue = true;
                    }
            }


            return returnValue;
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
            return false;
        }
    }

    public TranslationMap[] GetDisplayMap(){
        TranslationMap a[] = new TranslationMap[displayMap.size()];
        a = displayMap.toArray(a);
        return a;
    }

    private void CreateDisplayMapFromJSON(){

        JSONObject displayMapTemp;

        try {

            if (dataDisplayMap == null) {
                BLog.d(TAG, "Could not find display map");
            } else {
                for (int i = 0; i < dataDisplayMap.length(); i++) {
                    displayMapTemp = dataDisplayMap.getJSONObject(i);
                    displayMap.add(
                            new TranslationMap(
                                    displayMapTemp.getInt("xy"),
                                    displayMapTemp.getInt("startXY"),
                                    displayMapTemp.getInt("endXY"),
                                    displayMapTemp.getInt("stripDirection"),
                                    displayMapTemp.getInt("stripNumber"),
                                    displayMapTemp.getInt("stripOffset")
                            )
                    );
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    }
}
