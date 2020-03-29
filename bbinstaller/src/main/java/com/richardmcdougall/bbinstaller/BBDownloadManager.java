package com.richardmcdougall.bbinstaller;

import android.content.res.Resources;

import com.richardmcdougall.bbcommon.BLog;
import com.richardmcdougall.bbcommon.FileHelpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BBDownloadManager {
    public FileHelpers.OnDownloadProgressType onProgressCallback = null;
    protected String mFilesDir;
    protected JSONObject dataDirectory;
    int mVersion;
    String mLastDirectoryMd5 = "";
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    boolean downloadSuccess = false;
    private String TAG = this.getClass().getSimpleName();

    BBDownloadManager(String filesDir, int myVersion) {
        mVersion = myVersion;
        mFilesDir = filesDir;

        LoadInitialDataDirectory();  // get started right away using the data we have on the board already (if any)

        // the first download thread should check every 10 seconds. likely wifi issues.
        Runnable checkForAPKs1 = () -> downloadThread1();
        sch.scheduleWithFixedDelay(checkForAPKs1, 20, 10, TimeUnit.SECONDS);

        // the second download thread should check every 5 minutes.
        Runnable checkForAPKs2 = () -> downloadThread2();
        sch.scheduleWithFixedDelay(checkForAPKs2, 5, 5, TimeUnit.MINUTES);
    }

    int GetAPKByVersion(String prefix, int version) throws Exception {
        int highestVersion = 0;
        int indexOfHighest = 0;

        BLog.d(TAG, "Get APK Version " + version);
        if (dataDirectory == null) {
            throw new Resources.NotFoundException("download directory empty");
        }
        if (dataDirectory.has("application")) {
            try {
                JSONArray apks = dataDirectory.getJSONArray("application");
                // Look for specific prefix, version
                if (version > 0) {
                    for (int i = 0; i < apks.length(); i++) {
                        BLog.d(TAG, "Looking at APK " + i);
                        JSONObject a = GetAPK(i);
                        if (a != null) {
                            if (a.getString("localName").startsWith(prefix)) {
                                int v = a.getInt("Version");
                                BLog.d(TAG, "Found version " + v);
                                if (v == version) {
                                    return (i);
                                }
                            }
                        }
                    }
                } else {
                    // Look for latest version
                    for (int i = 0; i < apks.length(); i++) {
                        JSONObject a = GetAPK(i);
                        if (a != null) {
                            if (a.getString("localName").startsWith(prefix)) {
                                int v = a.getInt("Version");
                                if (v > highestVersion) {
                                    highestVersion = v;
                                    indexOfHighest = i;
                                }
                            }
                        }
                    }
                    if (highestVersion > 0) {
                        return indexOfHighest;
                    }
                }
            } catch (JSONException e) {
                BLog.e(TAG, e.getMessage());
                return 0;
            }
        } else {
            BLog.d(TAG, "No APKs in download dir");
            throw new Resources.NotFoundException("download index contains no apks");
        }
        BLog.d(TAG, "No APKs with requested version number");
        throw new Resources.NotFoundException("No APKs with requested version number");
    }

    String GetAPKFile(int index) {
        try {
            String fn = mFilesDir + "/" + GetAPK(index).getString("localName");
            return fn;
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return null;
        }
    }

    JSONObject GetAPK(int index) {
        if (dataDirectory == null)
            return null;
        if (dataDirectory.has("application")) {
            try {
                return dataDirectory.getJSONArray("application").getJSONObject(index);
            } catch (JSONException e) {
                BLog.e(TAG, e.getMessage());
                return null;
            }
        } else
            return null;
    }

    public long GetURLFileSize(String URLString) {
        try {
            URL url = new URL(URLString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            List values = urlConnection.getHeaderFields().get("content-Length");
            if (values != null && !values.isEmpty()) {
                String sLength = (String) values.get(0);

                if (sLength != null) {
                    return Integer.valueOf(sLength);
                }
            }
            urlConnection.disconnect();
            return -1;
        } catch (Throwable e) {
            BLog.e(TAG, e.getMessage());
            return -1;
        }
    }

    public void CleanupOldFiles() {
        try {
            ArrayList<String> refrerencedFiles = new ArrayList<String>();


            JSONObject dir = dataDirectory;

            String[] dTypes = new String[]{"audio", "video"};
            String[] extTypes = new String[]{"mp3", "mp4"};
            for (int i = 0; i < dTypes.length; i++) {
                JSONArray tList = dir.getJSONArray(dTypes[i]);
                for (int j = 0; j < tList.length(); j++) {
                    JSONObject elm = tList.getJSONObject(j);
                    if (elm.has("localName")) {
                        refrerencedFiles.add(elm.getString("localName"));
                    }
                }
            }

            String dataDir = mFilesDir;
            File[] flist = new File(dataDir).listFiles();
            for (int i = 0; i < flist.length; i++) {
                String fname = flist[i].getName();

                if (fname.endsWith(".mp4") || fname.endsWith(".mp3")) {
                    if (!refrerencedFiles.contains(fname)) {
                        flist[i].delete();
                    }
                }
            }

        } catch (Throwable er) {
        }
    }

    public void LoadInitialDataDirectory() {
        try {
            String dataDir = mFilesDir;
            File[] flist = new File(dataDir).listFiles();
            for (int i = 0; i < flist.length; i++) {
                String fname = flist[i].getName();
                if (fname.endsWith(".tmp")) {   // download finished, move it over
                    String dstName = flist[i].getName().substring(0, flist[i].getName().lastIndexOf('.'));
                    flist[i].renameTo(new File(dataDir, dstName));
                }
            }

            String origDir = FileHelpers.LoadTextFile("directory.json", mFilesDir);
            if (origDir != null) {
                JSONObject dir = new JSONObject(origDir);
                dataDirectory = dir;
                CleanupOldFiles();
            }
        } catch (Throwable er) {
            BLog.e(TAG, er.getMessage());
        }
    }

    public boolean GetNewDirectory() {

        try {

            String dataDir = mFilesDir;

            BLog.d(TAG, "Downloading app index");
            long ddsz = FileHelpers.DownloadURL("https://us-central1-burner-board.cloudfunctions.net/boards/apkVersions", "tmp", "Directory", onProgressCallback, mFilesDir);
            if (ddsz > 0) {
                new File(dataDir, "tmp").renameTo(new File(dataDir, "directory.json.tmp"));

                String dirTxt = FileHelpers.LoadTextFile("directory.json.tmp", mFilesDir);
                JSONObject dir = new JSONObject(dirTxt);

                int tFiles = 0;

                String[] dTypes = new String[]{"application"};
                String[] extTypes = new String[]{"apk"};
                for (int i = 0; i < dTypes.length; i++) {
                    JSONArray tList = dir.getJSONArray(dTypes[i]);
                    for (int j = 0; j < tList.length(); j++) {
                        long appVersion = 0;
                        JSONObject elm = tList.getJSONObject(j);
                        String url = elm.getString("URL");

                        String localName;
                        if (elm.has("localName"))
                            localName = elm.getString("localName");
                        else
                            localName = String.format("%s-%d.%s", dTypes[i], j, extTypes[i]);
                        BLog.d(TAG, "Download file is " + localName);

                        Boolean upTodate = false;
                        File dstFile = new File(dataDir, localName);
                        if (elm.has("Version")) {
                            appVersion = elm.getLong("Version");
                        }
                        if (elm.has("Size")) {
                            long sz = elm.getLong("Size");

                            if (dstFile.exists()) {
                                long curSze = dstFile.length();
                                if (dstFile.length() == sz) {
                                    upTodate = true;
                                }
                            }
                        } else {
                            if (dstFile.exists()) {
                                long remoteSz = GetURLFileSize(url);
                                long curSze = dstFile.length();
                                BLog.d(TAG, "Checking Downloaded file: " + localName + " remote URL size " + remoteSz + " == " + dstFile.length());

                                if (dstFile.length() == remoteSz) {
                                    upTodate = true;
                                }
                            }
                        }

                        File dstFile2;
                        if (!upTodate) {
                            BLog.d(TAG, "Downloading url " + localName);

                            FileHelpers.DownloadURL(url, "tmp", localName, onProgressCallback, mFilesDir);   // download to a "tmp" file
                            tFiles++;
                            if (appVersion == 0) { // .tmp files for all media except app packages
                                dstFile2 = new File(dataDir, localName + ".tmp");   // move to localname.tmp, we'll move the file the next time we reboot
                            } else {
                                dstFile2 = new File(dataDir, localName);   // move to localname so that we can install it
                            }
                            if (dstFile2.exists())
                                dstFile2.delete();
                            new File(dataDir, "tmp").renameTo(dstFile2);

                        } else {
                            BLog.d(TAG, "Up to date: " + dstFile);
                        }

                    }
                }
                dataDirectory = dir;

                if (onProgressCallback != null) {
                    if (tFiles != 0)
                        onProgressCallback.onVoiceCue("Finished downloading " + String.valueOf(tFiles) + " files");
                }

            }
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    public void downloadThread1() {
        try {
            // On boot it can take some time to get an internet connection.  Check every 5 seconds for 5 minutes
            if (!downloadSuccess) {
                downloadSuccess = GetNewDirectory();
            }
        } catch (Throwable th) {
            BLog.e(TAG, "Error downloading app index");
            if (onProgressCallback != null)
                onProgressCallback.onVoiceCue(th.getMessage());
        }
    }

    public void downloadThread2() {
        try {
            downloadSuccess = GetNewDirectory();
        } catch (Throwable th) {
            BLog.e(TAG, "Error downloading app index");
            if (onProgressCallback != null)
                onProgressCallback.onVoiceCue(th.getMessage());
        }
    }

}


