package com.richardmcdougall.bb;

import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.FileHelpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DisplayMapManager2 {

    private String TAG = this.getClass().getSimpleName();
    private String displauMapXLSX;
    private BBService service;
    public ArrayList<DisplayMapRow> displayMap = new ArrayList<>();
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private String filesDir;
    public FileHelpers.OnDownloadProgressType onProgressCallback = null;

    public int boardHeight = 0;
    public int boardWidth = 0;
    public int numberOfStrips = 0;
    String displayMapText = "";
    long lastDisplayMapModified = 0;
    public String displayDebugPattern = "";

    public ArrayList<Integer> stripOffsets = new ArrayList<>();
    public ArrayList<Integer> stripMaxPixels = new ArrayList<>();
    public ArrayList<Integer> stripPixelsPer = new ArrayList<>();
    public ArrayList<ArrayList> stripSectionMap = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> twoDimensionalSectionMap = new ArrayList<>();
    public ArrayList<Integer> oneDimensionalSectionMap = new ArrayList<>();

    public DisplayMapManager2(BBService service) {
        this.service = service;
        filesDir = this.service.context.getFilesDir().getAbsolutePath();

        this.displauMapXLSX = this.service.boardState.BOARD_ID + ".csv";

        Runnable periodicCheckForDisplayMap = () -> LoadDisplayMap();

        sch.scheduleWithFixedDelay(periodicCheckForDisplayMap, 1, 1, TimeUnit.SECONDS);

    }

    private boolean LoadDisplayMap() {
        try {

            File file = new File(filesDir, this.displauMapXLSX);

            if (!file.exists())
                throw(new Exception("Display Map XLSX file not found."));

            long lastModified = file.lastModified();

            if (lastModified > lastDisplayMapModified) {

                importCSV(file);
                analyzeDisplayMap();
                calculateStripOffsets();
                createStripSectionMap();
                createOneDimensionalSectionMap();
                createTwoDimesionalSectionMap();

                try{
                    service.burnerBoard.initpixelMap2Board();
                }
                catch (Throwable er) {
                    BLog.e(TAG, "Failed to load display map. This is normal during startup.  " + er.getMessage());
                    return false;
                }

                //BLog.d(TAG, "new display map loaded " + this.boardWidth + " " + this.boardWidth + " " + this.numberOfStrips);
                lastDisplayMapModified = lastModified;
            }

        } catch (Throwable er) {
            BLog.e(TAG, er.getMessage());
            return false;
        }
        return true;
    }

    private void analyzeDisplayMap() {
        int maxStripNumber = 0;
        int maxRowNumber = 0;
        int maxWidth = 0;

        for (DisplayMapRow row : this.displayMap) {
            maxStripNumber = Math.max(maxStripNumber, row.stripNumber);
            maxRowNumber = Math.max(maxRowNumber, row.row);

            int rowSum = row.rail1 + row.under1 + row.side1 + row.top
                    + row.side2 + row.under2 + row.rail2;
            maxWidth = Math.max(maxWidth, rowSum);
        }

        this.boardHeight = maxRowNumber+1; // file will be 0 based.
        this.boardWidth = maxWidth; // file will be 0 based.
        this.numberOfStrips = maxStripNumber+1; // file will be 0 based.

    }

    private void calculateStripOffsets(){
        int runningOffset = 0;
        stripOffsets = new ArrayList<>();
        stripMaxPixels = new ArrayList<>();
        stripPixelsPer = new ArrayList<>();

        for (int i = 0; i < this.numberOfStrips; i++){
            for (DisplayMapRow row : this.displayMapForStrip(i)){
                stripOffsets.add(i,runningOffset);
                runningOffset += row.rowLength();
                stripMaxPixels.add(i,runningOffset);
                stripPixelsPer.add(i,row.rowLength());
            }
        }
    }

    private void createStripSectionMap(){
        stripSectionMap = new ArrayList<>();
        for (int i = 0; i < this.numberOfStrips; i++) {
            ArrayList<Integer> m = new ArrayList<Integer>();
            for (DisplayMapRow row : this.displayMapForStrip(i)) {
                m.addAll(row.rowPixels);
            }
            stripSectionMap.add(i,m);
        }
    }

    private void createOneDimensionalSectionMap(){

        //build an array that can be used to override and black out sections (like the underrail).
        // the map created here should be the same height / width as the OutputScreen.
        // because each of our rows are different lengths, we need to be centering our rows as we build this map.
        // there may be an issue here with non-even out-of-bounds.
        oneDimensionalSectionMap = new ArrayList<Integer>();

        for(int y = 0; y<boardHeight;y++ ){
            ArrayList<Integer> xyOverrideRow = new ArrayList<>();

            DisplayMapRow d = displayMap.get(y);
            int paddingPixels = (boardWidth - d.rowLength()) / 2;
            for(int i=0;i<paddingPixels;i++)
                xyOverrideRow.add(d.OUTOFBOUNDS);

            xyOverrideRow.addAll(d.rowPixels);
            for(int i=0;i<paddingPixels;i++)
                xyOverrideRow.add(d.OUTOFBOUNDS);

            oneDimensionalSectionMap.addAll(xyOverrideRow);
        }
    }

    private void createTwoDimesionalSectionMap(){

        //build an array that can be used to override and black out sections (like the underrail).
        // the map created here should be the same height / width as the OutputScreen.
        // because each of our rows are different lengths, we need to be centering our rows as we build this map.
        // there may be an issue here with non-even out-of-bounds.
        twoDimensionalSectionMap = new ArrayList<>();

        for(int y = 0; y<boardHeight;y++ ){
            ArrayList<Integer> xyOverrideRow = new ArrayList<>();

            DisplayMapRow d = displayMap.get(y);
            int paddingPixels = (boardWidth - d.rowLength()) / 2;
            for(int i=0;i<paddingPixels;i++)
                xyOverrideRow.add(d.OUTOFBOUNDS);

            xyOverrideRow.addAll(d.rowPixels);
            for(int i=0;i<paddingPixels;i++)
                xyOverrideRow.add(d.OUTOFBOUNDS);

            twoDimensionalSectionMap.add(xyOverrideRow);
        }
    }

    public int[] pixelsPerStrip(){
        int[] array = new int[stripPixelsPer.size()];
        for(int i = 0; i < stripPixelsPer.size(); i++) {
            array[i] = stripPixelsPer.get(i);
        }
        return array;
    }

    public int whichStripAmI(int pixel){
        int strip = 0;
        for(int i = 0; i< stripMaxPixels.size();i++){
            if(pixel >= stripOffsets.get(i).intValue() && pixel < stripMaxPixels.get(i).intValue()) {
                strip = i;
            }
        }
        return strip;
    }

    public ArrayList<DisplayMapRow> displayMapForStrip(int stripNumber){
        ArrayList<DisplayMapRow> map = new ArrayList<>();

        for (DisplayMapRow row : this.displayMap) {
            if(row.stripNumber == stripNumber)
                map.add(row);
        }
        return map;
    }

    private void importCSV(File file) {
        ArrayList<DisplayMapRow> displayMapRows = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {

                String[] parts = line.split(",");
                if(parts.length>0){
                    if (parts[0].equals("displayDebug"))
                        displayDebugPattern = parts[1];

                    if(parts[0].equals("stripNumber"))
                        break;
                }
            }

            while ((line = reader.readLine()) != null) {
                BLog.d(TAG, line);
                String[] parts = line.split(",");

                int stripNumber = Integer.parseInt(parts[0]);
                int row = Integer.parseInt(parts[1]);
                int rail1 = Integer.parseInt(parts[2]);
                int under1 = Integer.parseInt(parts[3]);
                int side1 = Integer.parseInt(parts[4]);
                int top = Integer.parseInt(parts[5]);
                int side2 = Integer.parseInt(parts[6]);
                int under2 = Integer.parseInt(parts[7]);
                int rail2 = Integer.parseInt(parts[8]);

                DisplayMapRow displayMapRow = new DisplayMapRow(stripNumber, row, rail1, under1, side1, top, side2, under2, rail2);
                displayMapRows.add(displayMapRow);

            }

            displayMap = displayMapRows;

            reader.close();
        } catch (Exception e) {
            BLog.e(TAG, "Error importing CSV: " + e.getMessage());
        }

    }

    public class DisplayMapRow {

        public int OUTOFBOUNDS = 0;
        public int RAIL = 1;
        public int UNDER = 2;
        public int SIDE = 3;
        public int TOP = 4;
        public int stripNumber;
        public int row;
        public int rail1;
        public int under1;
        public int side1;
        public int top;
        public int side2;
        public int under2;
        public int rail2;
        private ArrayList<Integer> rowPixels = new ArrayList<>();

        public DisplayMapRow(int stripNumber, int row, int rail1, int under1, int side1, int top, int side2, int under2, int rail2) {
            this.stripNumber = stripNumber;
            this.row = row;
            this.rail1 = rail1;
            this.under1 = under1;
            this.side1 = side1;
            this.top = top;
            this.side2 = side2;
            this.under2 = under2;
            this.rail2 = rail2;

            for(int i=0; i<rail1; i++)
                rowPixels.add(RAIL);
            for(int i=0; i<under1; i++)
                rowPixels.add(UNDER);
            for(int i=0; i<side1; i++)
                rowPixels.add(SIDE);
            for(int i=0; i<top; i++)
                rowPixels.add(TOP);
            for(int i=0; i<side2; i++)
                rowPixels.add(SIDE);
            for(int i=0; i<under2; i++)
                rowPixels.add(UNDER);
            for(int i=0; i<rail2; i++)
                rowPixels.add(RAIL);
        }

        public int rowLength(){
            return rowPixels.size();
        }
    }
}