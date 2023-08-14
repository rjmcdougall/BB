package com.richardmcdougall.bb;

import java.util.Collections;
import java.util.Random;

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
import java.util.Hashtable;

public class DisplayMapManager {

    private String TAG = this.getClass().getSimpleName();
    private String displauMapJSONFilename;
    private BBService service;
    public JSONArray dataDisplayMap;
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private String filesDir;
    public String displayDebugPattern;

    public FileHelpers.OnDownloadProgressType onProgressCallback = null;
    public ArrayList<TranslationMap> displayMap;
    public int boardHeight = 0;
    public int boardWidth = 0;
    public int numberOfStrips = 0;
    String displayMapText = "";
    long lastDisplayMapModified = 0;


    public DisplayMapManager(BBService service) {
        this.service = service;
        filesDir = this.service.context.getFilesDir().getAbsolutePath();

        this.displauMapJSONFilename = this.service.boardState.BOARD_ID + ".json";

        Runnable periodicCheckForDisplayMap = () -> LoadDisplayMap();

        sch.scheduleWithFixedDelay(periodicCheckForDisplayMap, 1, 1, TimeUnit.SECONDS);

    }

    private boolean LoadDisplayMap() {
        try {

            File file = new File(filesDir, this.displauMapJSONFilename);

            if (file.exists()) {
                long lastModified = file.lastModified();

                if (lastModified > lastDisplayMapModified) {

                    displayMapText = FileHelpers.LoadTextFile(this.displauMapJSONFilename, filesDir);

                    JSONArray dir = new JSONArray(displayMapText);

                    dataDisplayMap = dir;
                    CreateDisplayMapFromJSON();
                    service.burnerBoard.initpixelMap2Board();

                    BLog.d(TAG, "new display map loaded " + displayMapText.substring(0,10));
                    lastDisplayMapModified = lastModified;
                }
            }

        } catch (Throwable er) {
            BLog.e(TAG, "Failed to load display map. This is normal during startup.  " + er.getMessage());
            return false;
        }
        return true;
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

    private void CreateDisplayMapFromJSON(){

        boardHeight = dataDisplayMap.length();
        boardWidth = 70;
        int boardCenterXCoordinate = 35;
        numberOfStrips = 0;

        try {

            if (dataDisplayMap == null) {
                BLog.d(TAG, "Could not find display map");
            } else {

                displayMap = new ArrayList<>();
                displayDebugPattern = "";

                for (int i =0; i < dataDisplayMap.length() ; i++) {

                    displayMapTemp = dataDisplayMap.getJSONObject(i);

                    if(displayMapTemp.has("displayDebug")){
                        displayDebugPattern = displayMapTemp.getString("displayDebug");
                    }
                    else {
                        int rowLength = 0;
                        int centerPoint = 0;
                        int stripOffset = 0;

                        if(displayMapTemp.has("centerPoint"))
                            centerPoint = displayMapTemp.getInt("centerPoint");

                        if(displayMapTemp.has("rowLength"))
                            rowLength = displayMapTemp.getInt("rowLength");

                        if(displayMapTemp.has("startXY"))
                            startXY = displayMapTemp.getInt("startXY");
                        else
                            startXY = boardCenterXCoordinate + rowLength / 2;

                        if(displayMapTemp.has("endXY"))
                            endXY = displayMapTemp.getInt("endXY");
                        else
                            endXY = boardCenterXCoordinate - rowLength / 2;
                        if(displayMapTemp.has("stripOffset"))
                            stripOffset = displayMapTemp.getInt("stripOffset");
                        else
                            stripOffset = centerPoint - rowLength / 2;

                        stripNumber = displayMapTemp.getInt("stripNumber");

                        TranslationMap m = new TranslationMap(
                                displayMapTemp.getInt("xy"),
                                startXY,
                                0,
                                0,
                                endXY,
                                0,// not used
                                stripNumber,
                                stripOffset
                        );

                        displayMap.add(m);

                        BLog.d(TAG, "Display Map" + m);

                        if(numberOfStrips<stripNumber)
                            numberOfStrips = stripNumber;
                    }
                }
            }
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    }
}