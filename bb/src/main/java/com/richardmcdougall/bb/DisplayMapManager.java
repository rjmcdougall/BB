package com.richardmcdougall.bb;

import java.util.Collections;
import java.util.Random;

import com.richardmcdougall.bb.board.TranslationMap;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.FileHelpers;
import com.richardmcdougall.bb.board.TranslationMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Hashtable;

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
    public ArrayList<TranslationMap> displayMap;
    public int boardHeight = 0;
    public int boardWidth = 0;
    public int numberOfStrips = 0;
    public boolean debug = false;
    private Random rand = new Random();
    String origDir = "";

    public DisplayMapManager(BBService service) {
        this.service = service;
        filesDir = this.service.context.getFilesDir().getAbsolutePath();

        this.displauMapJSONFilename = this.service.boardState.BOARD_ID + ".json";
        LoadInitialDisplayMap();

        // wait 5 seconds to hopefully get wifi before starting the download.
        Runnable initialCheckForDisplayMap = () -> initialCheck();
        Runnable periodicCheckForDisplayMap = () -> periodicCheck();
        Runnable debugCheckForDisplayMap = () -> frequentCheck();
        Runnable checkForDebugStatus = () -> checkForDebug();

        sch.scheduleWithFixedDelay(initialCheckForDisplayMap, 3, 5, TimeUnit.SECONDS);
        sch.scheduleWithFixedDelay(periodicCheckForDisplayMap, 1, 2, TimeUnit.MINUTES);
        sch.scheduleWithFixedDelay(debugCheckForDisplayMap, 1, 5, TimeUnit.SECONDS);
        sch.scheduleWithFixedDelay(checkForDebugStatus, 1, 1, TimeUnit.SECONDS);

        this.onProgressCallback = new FileHelpers.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    service.speak("Downloading " + file + "", "downloading display");
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

    private void checkForDebug(){
        boolean currentDebug = this.debug;
        boolean newDebug = this.service.boardState.displayDebug;

        if(!currentDebug && newDebug)
            this.service.speak("Display Debug On", "debug status");

        if(currentDebug && !newDebug)
            this.service.speak("Display Debug Off", "debug status");

        this.debug = newDebug;
    }

    private void periodicCheck(){
        if(!debug)
            downloadSuccessDisplayMap = GetNewDisplayMapJSON();
    }
    private void frequentCheck(){
        if(debug)
            downloadSuccessDisplayMap = GetNewDisplayMapJSON();
    }

    private void LoadInitialDisplayMap() {
        try {
            File[] flist = new File(filesDir).listFiles();
            if (flist != null) {

                origDir = FileHelpers.LoadTextFile(this.displauMapJSONFilename, filesDir);
                if (origDir != null) {
                    JSONArray dir = new JSONArray(origDir);
                    dataDisplayMap = dir;
                    CreateDisplayMapFromJSON();
                    BLog.d(TAG, "Dir " + origDir.substring(0,100));
                }
            }

        } catch (Throwable er) {
            BLog.e(TAG, er.getMessage());
        }
    }

    String dirTxt;

    private boolean GetNewDisplayMapJSON() {

        try {

            String cacheBuster = "?" + String.valueOf(rand.nextInt());

            boolean returnValue = true;

            BLog.d(TAG, DIRECTORY_URL);

            long ddsz = -1;
            ddsz = FileHelpers.DownloadURL(DIRECTORY_URL + this.displauMapJSONFilename + cacheBuster, DISPLAY_MAP_TEMP2_FILENAME, "Display Map JSON", onProgressCallback, this.filesDir);

            if (ddsz < 0) {
                BLog.d(TAG, "Unable to Download Display Map JSON.  Likely because of no internet.  Sleeping for 5 seconds. ");
                returnValue = false;
            } else {
                new File(filesDir, DISPLAY_MAP_TEMP2_FILENAME).renameTo(new File(filesDir, DISPLAY_MAP_TMP_FILENAME));

                dirTxt = FileHelpers.LoadTextFile(DISPLAY_MAP_TMP_FILENAME, filesDir);
                JSONArray dir = new JSONArray(dirTxt);

                BLog.d(TAG, "Downloaded Display Map JSON: " + dirTxt.substring(0,10));

                if (onProgressCallback != null) {
                    if (!(dirTxt.length() == origDir.length())) {
                        BLog.d(TAG, "New Display Map Synced.");

                        // got new display map.  Update!
                        dataDisplayMap = dir;
                        CreateDisplayMapFromJSON();
                        service.burnerBoard.initpixelMap2Board();

                        onProgressCallback.onVoiceCue("New Display Map Synced.");
                    } else
                        BLog.d(TAG, "no changes discovered in Display Map JSON.");
                }

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


    Hashtable<Integer, Integer> offsets = new Hashtable<Integer, Integer> ();
    JSONObject displayMapTemp;
    int stripNumber;
    int  startXY;
    int endXY;
    int rowLength;

    private void interperetOffset(){

        try{
            // find the length of used pixels
            for (int i = 0; i< dataDisplayMap.length(); i++) {
                displayMapTemp = dataDisplayMap.getJSONObject(i);
                startXY = displayMapTemp.getInt("startXY");
                endXY = displayMapTemp.getInt("endXY");
                stripNumber = displayMapTemp.getInt("stripNumber");
                rowLength = startXY - endXY + 1;

                if(!offsets.containsKey(stripNumber))
                    offsets.put(stripNumber,0);

                offsets.put(stripNumber,offsets.get(stripNumber) + rowLength);
            }
        }
        catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }

    }


    private void CreateDisplayMapFromJSON(){

        int numberOfStripsTemp = 0;
        int boardHeightTemp = 0;
        int boardWidthTemp = 0;

        interperetOffset();

        try {

            if (dataDisplayMap == null) {
                BLog.d(TAG, "Could not find display map");
            } else {
                displayMap = new ArrayList<>();
                for (int i = dataDisplayMap.length() - 1; i >= 0 ; i--) {

                    displayMapTemp = dataDisplayMap.getJSONObject(i);
                    startXY = displayMapTemp.getInt("startXY");
                    endXY = displayMapTemp.getInt("endXY");
                    stripNumber = displayMapTemp.getInt("stripNumber");
                    rowLength = startXY - endXY + 1;

                    offsets.put( stripNumber,offsets.get(stripNumber) - rowLength);

                    TranslationMap m = new TranslationMap(
                            displayMapTemp.getInt("xy"),
                            startXY,
                            endXY,
                            displayMapTemp.getInt("stripDirection"),
                            stripNumber,
                            offsets.get(stripNumber)
                    );

                    displayMap.add(m);

                    //decrement offsets


                    BLog.d(TAG, "Display Map" + m);

                    if(displayMapTemp.getInt("stripNumber") > numberOfStripsTemp)
                        numberOfStripsTemp = displayMapTemp.getInt("stripNumber");
                    if(displayMapTemp.getInt("xy") > boardHeightTemp)
                        boardHeightTemp = displayMapTemp.getInt("xy");
                    if(Math.abs(rowLength) > boardWidthTemp)
                        boardWidthTemp = rowLength;
                }

                Collections.reverse(displayMap);

                boardHeight =  boardHeightTemp;
                boardWidth = boardWidthTemp;
                numberOfStrips = numberOfStripsTemp;

            }
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    }
}
