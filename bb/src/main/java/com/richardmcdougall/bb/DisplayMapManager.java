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

                    BLog.d(TAG, "new display map loaded " + displayMapText.substring(0,100));
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