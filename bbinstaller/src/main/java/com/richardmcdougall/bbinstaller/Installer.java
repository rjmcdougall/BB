package com.richardmcdougall.bbinstaller;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;

import com.richardmcdougall.bbcommon.AllBoards;
import com.richardmcdougall.bbcommon.BBWifi;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bbcommon.FileHelpers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Installer extends Service {

    public BBDownloadManager dlManager;
    int mVersion = 0;
    TextToSpeech voice;
    List<ApplicationInfo> packages = null;
    private String TAG = this.getClass().getSimpleName();
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private Context context = null;
    private AllBoards allBoards = null;
    private BoardState boardState = null;
    private BBWifi wifi = null;

    public Installer() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {

        super.onCreate();

        BLog.i(TAG,"Starting BB Installer");

        BLog.i(TAG, "onCreate");

        voice = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // check for successful instantiation
                if (status == TextToSpeech.SUCCESS) {
                    if (voice.isLanguageAvailable(Locale.UK) == TextToSpeech.LANG_AVAILABLE)
                        voice.setLanguage(Locale.US);
                }
            }
        });

        context = getApplicationContext();

        BLog.i(TAG, "Build Manufacturer " + Build.MANUFACTURER);
        BLog.i(TAG, "Build Model " + Build.MODEL);
        BLog.i(TAG, "Build Serial " + Build.SERIAL);

        allBoards = new AllBoards(context, voice);

        try {
            while (allBoards.dataBoards == null) {
                BLog.i(TAG, "Boards file is required to be downloaded before proceeding.  Please hold.");
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }

        boardState = new BoardState(this.context, this.allBoards);

        BLog.i(TAG, "State Version " + boardState.version);
        BLog.i(TAG, "State APK Updated Date " + boardState.apkUpdatedDate);
        BLog.i(TAG, "State Address " + boardState.address);
        BLog.i(TAG, "State SSID " + boardState.SSID);
        BLog.i(TAG, "State Password " + boardState.password);
        BLog.i(TAG, "State Mode " + boardState.currentVideoMode);
        BLog.i(TAG, "State BOARD_ID " + boardState.BOARD_ID);
        BLog.i(TAG, "State Tyoe " + boardState.boardType);
        BLog.i(TAG, "Display Teensy " + boardState.displayTeensy);

        wifi = new BBWifi(context, boardState);

        getPackageVersions();

        dlManager = new BBDownloadManager(getApplicationContext().getFilesDir().getAbsolutePath(), mVersion);
        dlManager.onProgressCallback = new FileHelpers.OnDownloadProgressType(){
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    if (fileSize > 1048576) {
                        voice.speak("Downloading New Software " + file + ", " + String.valueOf(percent) + " Percent", TextToSpeech.QUEUE_ADD, null, "downloading");
                    }
                    lastTextTime = curTime;
                    BLog.d(TAG,String.format("Downloading %02x%% %s", bytesDownloaded * 100 / fileSize, file));
                }
            }

            public void onVoiceCue(String msg) {
                voice.speak(msg, TextToSpeech.QUEUE_ADD, null, "Download Message");
            }

        };

        Runnable checkForInstall = () -> installerThread();

        sch.scheduleWithFixedDelay(checkForInstall, 5, 60, TimeUnit.SECONDS);

    }


    public void installerThread() {

        try {

            int currentBBVersion = getBBversion();

            if (boardState.targetAPKVersion > 0 && boardState.targetAPKVersion != currentBBVersion) {
                String apkFile = null;
                int thisBFileId = 0;
                try {
                    thisBFileId = dlManager.GetAPKByVersion("bb", boardState.targetAPKVersion);
                    apkFile = dlManager.GetAPKFile(thisBFileId);
                } catch (Exception e) {
                    BLog.e(TAG,"Version not yet downloaded");
                }

                if (apkFile != null) {
                    if (installApk(apkFile)) {
                        BLog.d(TAG,"Installed BB version " + boardState.targetAPKVersion);
                        voice.speak("Installed Software version " + boardState.targetAPKVersion, TextToSpeech.QUEUE_ADD, null, "swvers");
                        Thread.sleep(5000);

                        voice.speak("Re booting", TextToSpeech.QUEUE_ADD, null, "swvers");
                        Thread.sleep(3000);
                        doReboot("Upgrade");

                    } else {
                        BLog.e(TAG,"Failed installing BB version " + boardState.targetAPKVersion);
                        voice.speak("Failed upgrade of software version " + boardState.targetAPKVersion, TextToSpeech.QUEUE_ADD, null, "swversfail");
                    }
                }
            }

        } catch (Exception e) {
            BLog.e(TAG,"Unknown exception: " + e.getMessage());
        }
    }

    public void doReboot(String reason) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        pm.reboot(reason);
    }

    public boolean installApk(String path) {
        BLog.i(TAG, "Installing software update " + path);

        final String libs = "LD_LIBRARY_PATH=/vendor/lib64:/system/lib64 ";


        if (Build.MODEL.contains("NanoPC-T4")) {
            final String[] commands = {
                    "pm install -i com.richardmcdougall.bbinstaller --user 0 " + path
            };
            return execute_as_root(commands);
        } else {
            final String[] commands = {
//                libs + "cp  " + path + " /data/local/tmp 2>&1 >>/data/local/tmp/bb.log" ,
//                libs + "chmod 666 /data/local/tmp/bb.apk 2>&1 >>/data/local/tmp/bb.log",
//                libs + "pm install -r /data/local/tmp/bb.apk",
                    libs + "chmod 666 " + path,
                    libs + "pm install -rg " + path
            };

            return execute_as_root(commands);
        }
    }

    private boolean execute_as_root(String[] commands) {
        try {
            // Do the magic
            Process p = Runtime.getRuntime().exec("sh");
            InputStream es = p.getErrorStream();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            for (String command : commands) {
                BLog.i(TAG, command);
                os.writeBytes(command + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            os.close();

            int read;
            byte[] buffer = new byte[4096];
            String output = new String();
            while ((read = es.read(buffer)) > 0) {
                output += new String(buffer, 0, read);
            }

            p.waitFor();
            if(output.contains("Error:"))
                throw new Exception(output);

            BLog.i(TAG, output + " (" + p.exitValue() + ")");
            return true;
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
            return false;
        }
    }

    public int getBBversion() {
        final PackageManager pm = getPackageManager();
        int versionCode = 0;
        try {
            PackageInfo info = pm.getPackageInfo("com.richardmcdougall.bb", 0);
            versionCode = info.versionCode;
            BLog.d(TAG,"Version: " + versionCode);
        } catch (Exception e) {
        }
        return versionCode;
    }

    //get a list of installed apps.
    public void getPackageVersions() {
        final PackageManager pm = getPackageManager();

        packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            try {
                PackageInfo info = pm.getPackageInfo(packageInfo.packageName, 0);
                //l("Installed package :" + packageInfo.packageName + ", version: " + info.versionCode);
            } catch (Exception e) {
            }

        }
    }
}



