package com.richardmcdougall.bb.board;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.richardmcdougall.bb.ACTION;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.DebugConfigs;

import static com.richardmcdougall.bb.board.BurnerBoard.PIXEL_RED;
import static com.richardmcdougall.bb.board.BurnerBoard.PIXEL_GREEN;
import static com.richardmcdougall.bb.board.BurnerBoard.PIXEL_BLUE;

public class AppDisplay {

    private BurnerBoard board = null;
    private BBService service = null;

    public AppDisplay(BBService service, BurnerBoard board){
        this.board = board;
        this.service = service;
    }

    public void send(int[] displayMatrix){
        int[] rowPixels = new int[this.board.boardWidth * 3];
        for (int y = 0; y < this.board.boardHeight; y++) {
            //for (int y = 30; y < 31; y++) {
            for (int x = 0; x <  this.board.boardWidth; x++) {
                if (y <  this.board.boardHeight) {
                    rowPixels[( this.board.boardWidth - 1 - x) * 3 + 0] = displayMatrix[this.board.pixelOffset.Map(x, y, PIXEL_RED)];
                    rowPixels[( this.board.boardWidth - 1 - x) * 3 + 1] = displayMatrix[this.board.pixelOffset.Map(x, y, PIXEL_GREEN)];
                    rowPixels[( this.board.boardWidth - 1 - x) * 3 + 2] = displayMatrix[this.board.pixelOffset.Map(x, y, PIXEL_BLUE)];
                }
            }
            sendVisual(14, y, rowPixels);
        }
    }

    public void sendVisual(int visualId) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        Intent in = new Intent(ACTION.GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
    }

    public void sendVisual(int visualId, int arg) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        Intent in = new Intent(ACTION.GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg", arg);
        LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
    }

    public void sendVisual(int visualId, int arg1, int arg2, int arg3) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        Intent in = new Intent(ACTION.GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg1", arg1);
        in.putExtra("arg2", arg2);
        in.putExtra("arg3", arg3);
        LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
    }

    public void sendVisual(int visualId, int arg1, int[] arg2) {
        if (!DebugConfigs.DISPLAY_VIDEO_IN_APP) {
            return;
        }
        final byte[] pixels = new byte[arg2.length];
        for (int i = 0; i < arg2.length; i++) {
            pixels[i] = (byte) arg2[i];
        }
        Intent in = new Intent(ACTION.GRAPHICS);
        in.putExtra("resultCode", Activity.RESULT_OK);
        // Put extras into the intent as usual
        in.putExtra("visualId", visualId);
        in.putExtra("arg1", arg1);
        //java.util.Arrays.fill(arg2, (byte) 128);
        in.putExtra("arg2", pixels);
        LocalBroadcastManager.getInstance(service.context).sendBroadcast(in);
    }

}
