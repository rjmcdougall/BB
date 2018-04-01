package com.richardmcdougall.bb;

import android.content.pm.PackageInfo;
import android.text.TextUtils;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.net.URLEncoder;

/**
 * Created by Jonathan on 8/6/2017.
 */

public class DownloadManager {
    private static final String TAG = "BB.BBDownloadManger";
    protected String mFilesDir;
    protected JSONObject dataDirectory;
    int mVersion;
    boolean mIsServer = false;
    String mBoardId;

    JSONObject GetDataDirectory() { return dataDirectory; }


    interface OnDownloadProgressType {
        public void onProgress(String file, long fileSize, long bytesDownloaded);
        public void onVoiceCue(String err);
    }



    public OnDownloadProgressType onProgressCallback = null;

    DownloadManager(String filesDir, String boardId, boolean isServer, int myVersion) {
        mVersion = myVersion;
        mFilesDir = filesDir;
        PackageInfo pinfo;
        mIsServer = isServer;
        mBoardId = boardId;
    }

    void StartDownloads() {

        Log.d(TAG, "Starting download manager");
        BackgroundThread bThread = new BackgroundThread(this);
        Thread th = new Thread(bThread, "BB DownloadManager background thread");
        th.start();
    }

    String GetAudioFile(int index) {
        try {
            String fn = mFilesDir + "/" + GetAudio(index).getString("localName");
            return fn;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    String GetAudioFileLocalName(int index) {
        if (index >= 0 && index < GetTotalAudio()) {
            try {
                String fn = GetAudio(index).getString("localName");
                return fn;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    String GetVideoFileLocalName(int index) {
        if (index >= 0 && index < GetTotalVideo()) {
            try {
                String fn = "";
                if(GetVideo(index).has("algorithm"))
                    fn = GetVideo(index).getString("algorithm");
                else
                    fn = GetVideo(index).getString("localName");
                return fn;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    String GetVideoFile(int index) {
        try {
            String fn = mFilesDir + "/" + GetVideo(index).getString("localName");
            return fn;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    int GetTotalAudio() {
        if (dataDirectory==null)
            return 0;
        else {
            try {
                //Log.d(TAG, dataDirectory.getJSONArray("audio").length() + " audio files");
                return dataDirectory.getJSONArray("audio").length();
            } catch (JSONException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }

    int GetTotalVideo() {
        if (dataDirectory==null)
            return 0;
        else {
            try {
                //Log.d(TAG, dataDirectory.getJSONArray("audio").length() + " video files");
                return dataDirectory.getJSONArray("video").length();
            } catch (JSONException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }


    JSONObject GetVideo(int index) {
        if (dataDirectory==null)
            return null;
        if (dataDirectory.has("video")) {
            try {
                return dataDirectory.getJSONArray("video").getJSONObject(index);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        else
            return null;
    }

    String GetAlgorithm(int index) {
        if (dataDirectory==null)
            return null;
        if (dataDirectory.has("video")) {
            try {
                return dataDirectory.getJSONArray("video").getJSONObject(index).getString("algorithm");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        else
            return null;
    }

    JSONObject GetAudio(int index) {
        if (dataDirectory==null)
            return null;
        if (dataDirectory.has("audio")) {
            try {
                return dataDirectory.getJSONArray("audio").getJSONObject(index);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        else
            return null;
    }

    long GetAudioLength(int index) {

        try {
            return GetAudio(index).getLong("Length");
        } catch (JSONException e) {
            e.printStackTrace();
            return 1000;   // return a dummy value
        }
    }



    private class BackgroundThread implements Runnable {

        DownloadManager mDM;

        private BackgroundThread(DownloadManager dm) {
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

                Log.d(TAG, "Downloading" + URLString);
                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                    downloadSize += bufferLength;

                    if (mDM.onProgressCallback!=null) {
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
                for (int i=0; i<flist.length; i++) {
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
                if(flist != null){
                    for (int i=0; i<flist.length; i++) {
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
                }

            } catch (Throwable er) {
                er.printStackTrace();
                mDM.onProgressCallback.onVoiceCue("Error loading media error due to jason error");
            }
        }


        @Override
        public void run() {
            LoadInitialDataDirectory();  // get started right away using the data we have on the board already (if any)

            // keep trying to connect to the Internet to look for updates until we are able to connect
            try {
                boolean downloadSuccess = false;

                while (!downloadSuccess) {

                    String dataDir = mDM.mFilesDir;

                    String DirectoryURL =
                            "https://burnerboard.com/boards/" +
                                    mBoardId +  "/DownloadDirectoryJSON";

                    Log.d(TAG, "Reading Directory from " + DirectoryURL);

                    long ddsz = DownloadURL(DirectoryURL, "tmp", "Directory");

                    if (ddsz < 0) {
                        Log.d(TAG, "Sleeping for 5 seconds. ");
                        Thread.sleep(5000);   // no internet, wait 5 seconds before we try again
                    } else {
                        new File(dataDir, "tmp").renameTo(new File(dataDir,
                                "directory.json.tmp"));

                        String dirTxt = LoadTextFile("directory.json.tmp");
                        JSONObject dir = new JSONObject(dirTxt);

                        int tFiles = 0;

                        String[] dTypes = new String[]{"audio", "video"};
                        String[] extTypes = new String[]{"mp3", "mp4"};
                        for (int i = 0; i < dTypes.length; i++) {
                            JSONArray tList = dir.getJSONArray(dTypes[i]);
                            for (int j = 0; j < tList.length(); j++) {
                                long appVersion = 0;
                                JSONObject elm = tList.getJSONObject(j);

                                // if there is no URL, it is an algorithm and should be skipped for download.
                                if(elm.has("URL")) {

                                    String localName;
                                    if (elm.has("localName"))
                                        localName = elm.getString("localName");
                                    else
                                        localName = String.format("%s-%d.%s", dTypes[i], j, extTypes[i]);

                                    String url = elm.getString("URL");
                                    // Spaces were breaking the download
                                    String[] urlArray = url.split("/");
                                    urlArray[urlArray.length-1] = URLEncoder.encode(urlArray[urlArray.length-1], "UTF-8").replaceAll("\\+", "%20");
                                    urlArray[urlArray.length-2] = URLEncoder.encode(urlArray[urlArray.length-2], "UTF-8").replaceAll("\\+", "%20");
                                    url = TextUtils.join("/", urlArray);

                                    Boolean upTodate = false;
                                    File dstFile = new File(dataDir, localName);
                                    if (elm.has("Size")) {
                                        long sz = elm.getLong("Size");

                                        if (dstFile.exists()) {
                                            if (dstFile.length() == sz) {
                                                upTodate = true;
                                            }
                                        }
                                    } else {
                                        if (dstFile.exists()) {
                                            long remoteSz = GetURLFileSize(url);

                                            if (dstFile.length() == remoteSz) {
                                                upTodate = true;
                                            }
                                        }
                                    }

                                    if (mIsServer) {
                                        //downloadSuccess = true;
                                    } else {
                                        File dstFile2;
                                        if (!upTodate) {
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

                                        }
                                    }
                                }


                            }
                        }
                        mDM.dataDirectory = dir;

                        if (mDM.onProgressCallback != null) {
                            if (tFiles == 0)
                                mDM.onProgressCallback.onVoiceCue("Media ready");
                            else
                                mDM.onProgressCallback.onVoiceCue("Finished downloading " + String.valueOf(tFiles) + " files");
                        }
                        downloadSuccess = true;

                    }
                }

            } catch (Throwable th) {
                if (mDM.onProgressCallback != null)
                    mDM.onProgressCallback.onVoiceCue(th.getMessage());
                try {
                    Thread.sleep(10000);   // wait 10 second if unexpected error
                } catch (Throwable er) {

                }
            }
        }
    }


}


