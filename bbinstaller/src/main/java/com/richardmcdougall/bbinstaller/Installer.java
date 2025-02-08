package com.richardmcdougall.bbinstaller;

import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;

import com.richardmcdougall.bbcommon.AllBoards;
import com.richardmcdougall.bbcommon.BBWifi;
import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.BoardState;
import com.richardmcdougall.bbcommon.FileHelpers;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Installer extends JobService {
    //public class Installer extends Service {

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
    private boolean textToSpeechReady = false;

    public Installer() {
    }

    /*
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

     */

    @Override
    public boolean onStopJob(JobParameters param) {
        return true;
    }

    @Override
    //public void onCreate() {
    public boolean onStartJob(JobParameters param) {

        super.onCreate();

        BLog.i(TAG, "Starting BB Installer");

        BLog.i(TAG, "onCreate");



        // Stop the Android popup for verifying apps
        BLog.i(TAG, " BB Installer set package_verifier_enable 0");
        disablePackageVerify();

        // Disables a dialog shown on adb install execution.
        //Settings.Global.putInt(getContentResolver(), "verifier_verify_adb_installs", 0);

        // enable install from non market
        //Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);

        voice = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // check for successful instantiation
                if (status == TextToSpeech.SUCCESS) {
                    if (voice.isLanguageAvailable(Locale.UK) == TextToSpeech.LANG_AVAILABLE)
                        voice.setLanguage(Locale.US);
                    textToSpeechReady = true;
                }
                else {
                    BLog.e(TAG, "TTS Is Disabled or Failed");
                    textToSpeechReady = false;
                }
            }
        });

        context = getApplicationContext();

        BLog.i(TAG, "Build Manufacturer " + Build.MANUFACTURER);
        BLog.i(TAG, "Build Model " + Build.MODEL);
        BLog.i(TAG, "Build Serial " + Build.SERIAL);

        BLog.i(TAG, "Starting Wifi");

        allBoards = new AllBoards(context, voice);
        boardState = new BoardState(this.context, this.allBoards);
        wifi = new BBWifi(context, boardState);

        try {
            while (allBoards.dataBoards == null) {
                BLog.i(TAG, "Boards file is required to be downloaded before proceeding.  Please hold.");
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }

        // look to see if the board exists in allboards. if not create it and wait for sync
        String deviceID = boardState.GetDeviceID();
        JSONObject board = allBoards.getBoardByDeviceID(deviceID);
        if (board == null) {
            allBoards.createBoard(deviceID);
        }

        try {
            while (allBoards.getBoardByDeviceID(deviceID) == null) {
                BLog.i(TAG, "Booard must register before proceeding.  Please hold.");
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            BLog.e(TAG, "getDeviceByID Failure: " + e.getMessage());
        }

        BLog.i(TAG, "State Device ID " + boardState.DEVICE_ID);
        BLog.i(TAG, "State Version " + boardState.version);
        BLog.i(TAG, "State APK Updated Date " + boardState.apkUpdatedDate);
        BLog.i(TAG, "State Address " + boardState.address);
        BLog.i(TAG, "State SSID " + boardState.SSID);
        BLog.i(TAG, "State Password " + boardState.password);
        BLog.i(TAG, "State Mode " + boardState.currentVideoMode);
        BLog.i(TAG, "State BOARD_ID " + boardState.BOARD_ID);
        BLog.i(TAG, "State Tyoe " + boardState.GetBoardType());
        BLog.i(TAG, "Display Teensy " + boardState.displayTeensy);

        getPackageVersions();

        dlManager = new BBDownloadManager(getApplicationContext().getFilesDir().getAbsolutePath(), mVersion);
        dlManager.onProgressCallback = new FileHelpers.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    if (fileSize > 1048576) {
                        speak("Downloading New Software " + file + ", " + String.valueOf(percent) + " Percent", "downloading");
                    }
                    lastTextTime = curTime;
                    BLog.d(TAG, String.format("Downloading %02x%% %s", bytesDownloaded * 100 / fileSize, file));
                }
            }

            public void onVoiceCue(String msg) {
                try {
                    if (voice != null)
                        speak(msg, "Download Message");
                } catch (Exception e) {
                    BLog.e(TAG, e.getMessage());
                }
            }

        };

        Runnable checkForInstall = () -> installerThread();

        sch.scheduleWithFixedDelay(checkForInstall, 20, 60, TimeUnit.SECONDS);

        return false;
    }

    public void speak(String txt, String id) {
        try {
            if (voice != null && textToSpeechReady) {
                voice.speak(txt, TextToSpeech.QUEUE_ADD, null, id);
            } else {
                throw new Exception("Text to Speech Not Available");
            }
        } catch (Exception e) {
            BLog.e(TAG, e.getMessage());
        }
    }

    public void installerThread() {

        try {

            int currentBBVersion = getBBversion();
            BLog.i(TAG, "Running Installer Check targetAPK:" + boardState.targetAPKVersion + ", currentBBVersion: " + currentBBVersion);

            if (boardState.targetAPKVersion > 0 && boardState.targetAPKVersion > currentBBVersion) {
                String apkFile = null;
                int thisBFileId = 0;
                try {
                    thisBFileId = dlManager.GetAPKByVersion("bb", boardState.targetAPKVersion);
                    apkFile = dlManager.GetAPKFile(thisBFileId);
                } catch (Exception e) {
                    BLog.e(TAG, "Version not yet downloaded");
                }

                if (apkFile != null) {
                    if (installApkinstallApkAndroid11andHigher(apkFile)) {
                        BLog.d(TAG, "Installed BB version " + boardState.targetAPKVersion);
                        speak("Installed Software version " + boardState.targetAPKVersion, "swvers");
                        Thread.sleep(10000);



                    } else {
                        BLog.e(TAG, "Failed installing BB version " + boardState.targetAPKVersion);
                        speak("Failed upgrade of software version " + boardState.targetAPKVersion, "swversfail");
                    }
                }
            }

        } catch (Exception e) {
            BLog.e(TAG, "Unknown exception: " + e.getMessage());
        }
    }

    public void doReboot(String reason) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        pm.reboot(reason);
    }

    public boolean disablePackageVerify() {
        BLog.i(TAG, "Disabling Package Verifier");

        final String libs = "LD_LIBRARY_PATH=/vendor/lib64:/system/lib64 ";

        try {
            if (Build.MODEL.contains("NanoPC-T4")) {
                final String[] commands = {
                        "settings put global verifier_verify_adb_installs 0",
                        "settings put global package_verifier_enable 0",

                };
                return execute_as_root(commands);
            } else {
                final String[] commands = {
                        libs + "settings put global verifier_verify_adb_installs 0",
                        libs + "settings put global package_verifier_enable 0"
                };

                return execute_as_root(commands);
            }
        } catch (Exception e) {
            BLog.e(TAG, "settings command failed: " + e.getMessage());
            return false;
        }
    }


    private boolean installApkinstallApkAndroid11andHigher(String apkPath) {
        PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName("com.richardmcdougall.bb");

        //params.setInstallFlags(params.getInstallFlags() | PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS);



        try {
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            OutputStream out = session.openWrite("bb", 0, -1);
            FileInputStream fis = new FileInputStream(new File(apkPath));
            byte [] buffer = new byte[65536];
            int n;
            while ((n = fis.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            session.fsync(out);
            fis.close();
            out.close();

            Intent intent = new Intent(this, InstallerReceiver.class);
            intent.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            session.commit(pendingIntent.getIntentSender());

        } catch (Exception e) {
            BLog.e(TAG, "Error installing APK: " + e.getMessage());
        }
        return true;
    }

    public class InstallerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1);
            if (sessionId!= -1) {
                int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
                String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                switch (status) {
                    case PackageInstaller.STATUS_PENDING_USER_ACTION:
                        BLog.d(TAG, "Installation user action required");
// User action required, launch the installer UI
                        Intent installerIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                        if (installerIntent!= null) {
                            installerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(installerIntent);
                        }
                        break;
                    case PackageInstaller.STATUS_SUCCESS:
                        BLog.d(TAG, "Installation succeeded");
                        grantPerms();
                        // Installation succeeded
                        try {
                            speak("Rebooting", "swvers");

                            Thread.sleep(10000);
                            doReboot("Upgrade");
                        } catch (Exception e) {

                        }
                        break;
                    case PackageInstaller.STATUS_FAILURE:
                        BLog.d(TAG, "Installation failed");
// Installation failed
                        break;
                    default:
                        // Unknown status
                }
            }
        }
    }


    public boolean grantPerms() {
        BLog.i(TAG, "Granting perms");

        final String libs = "LD_LIBRARY_PATH=/vendor/lib64:/system/lib64 ";

        try {
            if (Build.MODEL.contains("NanoPC-T4")) {
                final String[] commands = {
                        "pm grant com.richardmcdougall.bb android.permission.ACCESS_FINE_LOCATION",
                        "pm grant com.richardmcdougall.bb android.permission.RECORD_AUDIO"
                };
                return execute_as_root(commands);
            } else {
                return false;
            }
        } catch (Exception e) {
            BLog.e(TAG, "installAPK Failure: " + e.getMessage());
            return false;
        }
    }


    public boolean installApkAndroid10andLower(String path) {
        BLog.i(TAG, "Installing software update " + path);

        final String libs = "LD_LIBRARY_PATH=/vendor/lib64:/system/lib64 ";

        try {
            if (Build.MODEL.contains("NanoPC-T4")) {
                final String[] commands = {
                        "settings put global package_verifier_enable    0",
                        "pm install -i com.richardmcdougall.bbinstaller --user 0 -g " + path,
                        "am start com.richardmcdougall.bb/.MainActivity"
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
        } catch (Exception e) {
            BLog.e(TAG, "installAPK Failure: " + e.getMessage());
            return false;
        }
    }

    private boolean execute_as_root(String[] commands) {
        try {
            // Do the magic
            Process p = Runtime.getRuntime().exec("sh");
            InputStream es = p.getErrorStream();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            for (String command : commands) {
                BLog.i(TAG, "executing command: " + command);
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
            if (output.contains("Error:") || output.contains("Failure"))
                throw new Exception("Failed Executing Returned: " + output + " (" + p.exitValue() + ")");

            BLog.i(TAG, "Executed command: " + output + " (" + p.exitValue() + ")");
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
            BLog.d(TAG, "Version: " + versionCode);
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



