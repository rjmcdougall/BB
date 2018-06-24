package com.richardmcdougall.bbinstaller;

import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.data;


/**
 * Created by Jonathan on 8/6/2017.
 */

public class BBDownloadManager {
    private static final String TAG = "BBI.DlManager";
    protected String mFilesDir;
    protected JSONObject dataDirectory;
    int mVersion;
    Thread bThread = null;
    String mLastDirectoryMd5 = "";


    public static void l(String s) {
        Log.v(TAG, s);
    }

    JSONObject GetDataDirectory() { return dataDirectory; }

    interface OnDownloadProgressType {
        public void onProgress(String file, long fileSize, long bytesDownloaded);
        public void onVoiceCue(String err);
    }

    public OnDownloadProgressType onProgressCallback = null;

    BBDownloadManager(String filesDir, int myVersion) {
        mVersion = myVersion;
        mFilesDir = filesDir;
        PackageInfo pinfo;
    }

    void StartDownloads() {

        if (bThread != null) {
            if (bThread.isAlive()) {
                l("Background thread already running");
                return;
            }
        }

        BackgroundThread b = new BackgroundThread(this);
        bThread = new Thread(b, "BB BBDownloadManager background thread");
        bThread.start();
    }

    int GetAPKByVersion(String prefix, int version) throws Exception {
        int highestVersion = 0;
        int indexOfHighest = 0;

        l("Get APK Version " + version);
        if (dataDirectory == null) {
            throw new Resources.NotFoundException("download directory empty");
        }
        if (dataDirectory.has("application")) {
            try {
                JSONArray apks = dataDirectory.getJSONArray("application");
                // Look for specific prefix, version
                if (version > 0) {
                    for (int i = 0; i < apks.length(); i++) {
                        l("Looking at APK " + i);
                        JSONObject a = GetAPK(i);
                        if (a != null) {
                            if (a.getString("localName").startsWith(prefix)) {
                                int v = a.getInt("Version");
                                l("Found version " + v);
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
                e.printStackTrace();
                return 0;
            }
        } else {
            l("No APKs in download dir");
            throw new Resources.NotFoundException("download index contains no apks");
        }
        l("No APKs with requested version number");
        throw new Resources.NotFoundException("No APKs with requested version number");
    }

    int GetAPKVersion(int index) {
        l("Get APK Version");
        try {
            JSONObject a = GetAPK(index);
            if (a != null) {
                int version = a.getInt("Version");
                return version;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }


    String GetAPKFile(int index) {
        try {
            String fn = mFilesDir + "/" + GetAPK(index).getString("localName");
            return fn;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    JSONObject GetAPK(int index) {
        if (dataDirectory==null)
            return null;
        if (dataDirectory.has("application")) {
            try {
                return dataDirectory.getJSONArray("application").getJSONObject(index);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        else
            return null;
    }

    private static class BackgroundThread implements Runnable {

        BBDownloadManager mDM;


        private BackgroundThread(BBDownloadManager dm) {
            mDM = dm;
        }

        public String LoadTextFile(String filename) {
            try {
                File f = new File(mDM.mFilesDir, filename);
                if (!f.exists())
                    return null;

                InputStream is = null;
                try {
                    is = new FileInputStream(f);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                BufferedReader buf = new BufferedReader(new InputStreamReader(is));
                String line = buf.readLine();
                StringBuilder sb = new StringBuilder();

                while (line != null) {
                    sb.append(line).append("\n");
                    line = buf.readLine();
                }

                return sb.toString();

            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            }
        }


        public long DownloadURL(String URLString, String filename, String progressName) {
            try {
                URL url = new URL(URLString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
                urlConnection.setRequestProperty("Accept", "*/*");
                urlConnection.setRequestMethod("GET");
                //urlConnection.setDoOutput(true);
                urlConnection.connect();

                File file = new File(mDM.mFilesDir, filename);
                FileOutputStream fileOutput = new FileOutputStream(file);
                InputStream inputStream = urlConnection.getInputStream();

                byte[] buffer = new byte[4096];
                int bufferLength = 0;

                long fileSize = -1;
                long downloadSize = 0;

                List values = urlConnection.getHeaderFields().get("content-Length");
                if (values != null && !values.isEmpty()) {
                    String sLength = (String) values.get(0);

                    if (sLength != null) {
                        fileSize = Long.valueOf(sLength);
                    }
                }


                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                    downloadSize += bufferLength;

                    if (mDM.onProgressCallback != null) {
                        mDM.onProgressCallback.onProgress(progressName, fileSize, downloadSize);

                    }
                }
                fileOutput.close();
                urlConnection.disconnect();

                return downloadSize;
            } catch (Throwable e) {
                e.printStackTrace();
                return -1;
            }
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
                e.printStackTrace();
                return -1;
            }
        }

        public long GetURLFileSizeFromGCS(String URLString) {

            Log.d(TAG, "Getting Length via Google REST Size API: " + URLString);

            try {
                StringBuilder result = new StringBuilder();
                URL url = new URL(URLString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    result.append(inputLine);
                in.close();

                JSONObject jsonObj = new JSONObject(result.toString());
                if (jsonObj.has("size"))
                    return jsonObj.getLong("size");
                else
                    return -1;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return -1;

        }

        public String GetURLMD5FromGCS(String URLString) {

            Log.d(TAG, "Getting MD5 via Google REST Size API: " + URLString);

            try {
                StringBuilder result = new StringBuilder();
                URL url = new URL(URLString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    result.append(inputLine);
                in.close();

                JSONObject jsonObj = new JSONObject(result.toString());
                if (jsonObj.has("md5Hash"))
                    return jsonObj.getString("md5Hash");
                else
                    return "";

            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";

        }

        public void CleanupOldFiles() {
            try {
                ArrayList<String> refrerencedFiles = new ArrayList<String>();


                JSONObject dir = mDM.dataDirectory;

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

                String dataDir = mDM.mFilesDir;
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
                String dataDir = mDM.mFilesDir;
                File[] flist = new File(dataDir).listFiles();
                for (int i = 0; i < flist.length; i++) {
                    String fname = flist[i].getName();
                    if (fname.endsWith(".tmp")) {   // download finished, move it over
                        String dstName = flist[i].getName().substring(0, flist[i].getName().lastIndexOf('.'));
                        flist[i].renameTo(new File(dataDir, dstName));
                    }
                }

                String origDir = LoadTextFile("directory.json");
                if (origDir != null) {
                    JSONObject dir = new JSONObject(origDir);
                    mDM.dataDirectory = dir;
                    CleanupOldFiles();
                }
            } catch (Throwable er) {
                er.printStackTrace();
            }
        }


        public boolean GetNewDirectory() {

            try {

                String dataDir = mDM.mFilesDir;

                // Check if the directory has changed
                String currentMD5 = GetURLMD5FromGCS("https://www.googleapis.com/storage/v1/b/burner-board/o/BurnerBoardApps%2FDownloadApp.json");
                l("current md5 " + currentMD5 + " == " + mDM.mLastDirectoryMd5 + "?");
                if (currentMD5.equals(mDM.mLastDirectoryMd5)) {
                    l("No Download directory changes");
                    //downloadSuccess = true;
                    return true;
                }

                mDM.mLastDirectoryMd5 = currentMD5;

                l("Downloading app index");
                long ddsz = DownloadURL("https://storage.googleapis.com/burner-board/BurnerBoardApps/DownloadApp.json", "tmp", "Directory");
                if (ddsz > 0) {
                    new File(dataDir, "tmp").renameTo(new File(dataDir, "directory.json.tmp"));

                    String dirTxt = LoadTextFile("directory.json.tmp");
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
                            l("Download file is " + localName);

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
                                    l("Checking Downloaded file: " + localName + " remote URL size " + remoteSz + " == " + dstFile.length());

                                    if (dstFile.length() == remoteSz) {
                                        upTodate = true;
                                    }
                                }
                            }

                            File dstFile2;
                            if (!upTodate) {
                                l("Downloading url " + localName);
                                DownloadURL(url, "tmp", localName);   // download to a "tmp" file
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
                                l("Up to date: " + dstFile);
                            }

                        }
                    }
                    mDM.dataDirectory = dir;

                    if (mDM.onProgressCallback != null) {
                        if (tFiles != 0)
                            mDM.onProgressCallback.onVoiceCue("Finished downloading " + String.valueOf(tFiles) + " files");
                    }

                }
                return true;
            } catch (Throwable th) {
                return false;
            }
        }

        @Override
        public void run() {
            int downloadTimeout = 5;

            LoadInitialDataDirectory();  // get started right away using the data we have on the board already (if any)

            try {
                boolean downloadSuccess = false;

                // On boot it can take some time to get an internet connection.  Check every 5 seconds for 5 minutes
                while (!downloadSuccess) {
                    downloadSuccess = GetNewDirectory();
                    l("Error downloading app index, retrying in " + downloadTimeout + " seconds");
                    Thread.sleep(downloadTimeout * 1000);   // no internet, wait 5 seconds before we try again
                    if (downloadTimeout < 256) {
                        downloadTimeout = downloadTimeout * 2;
                    }
                }

                // After the first boot check periodically in case the profile has changed.
                while (true) {
                    GetNewDirectory();
                    Thread.sleep(downloadTimeout * 1000);
                    if (downloadTimeout < 256) {
                        downloadTimeout = downloadTimeout * 2;
                    }
                }
            } catch (Throwable th) {
                if (mDM.onProgressCallback != null)
                    mDM.onProgressCallback.onVoiceCue(th.getMessage());
                if (downloadTimeout < 256) {
                    downloadTimeout = downloadTimeout * 2;
                }
                try {
                    Thread.sleep(downloadTimeout * 1000);   // wait if unexpected error
                } catch (Throwable er) {

                }
            }
        }
    }
}


