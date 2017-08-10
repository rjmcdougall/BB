package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
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

/**
 * Created by Jonathan on 8/6/2017.
 */

public class BBDownloadManager {
    protected String mFilesDir;
    protected JSONObject dataDirectory;
    JSONObject GetDataDirectory() { return dataDirectory; }


    interface OnDownloadProgressType {
        public void onProgress(String file, long fileSize, long bytesDownloaded);
        public void onVoiceCue(String err);
    }



    public OnDownloadProgressType onProgressCallback = null;

    BBDownloadManager(String filesDir) {
        mFilesDir = filesDir;
    }

    void StartDownloads() {

        BackgroundThread bThread = new BackgroundThread(this);
        Thread th = new Thread(bThread, "DownloadManager background thread");
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
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(true);
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

        @Override
        public void run() {
            try {
                String dataDir = mDM.mFilesDir;

                long ddsz = DownloadURL("https://dl.dropboxusercontent.com/s/1gh7pupx6ygm3wu/DownloadDirectory.json?dl=0", "directory.tmp", "Directory");
                if (ddsz<0) {
                    // if we can't get the latest directory information we probably aren't connected to the internet
                    // let's give up on downloading until the next reboot


                    if (mDM.onProgressCallback!=null)
                        mDM.onProgressCallback.onVoiceCue("No Internet");

                    // if we have a locally cached data directory, use that
                    if (new File(mDM.mFilesDir, "directory.json").exists()) {
                        String dText = LoadTextFile("directory.json");
                        mDM.dataDirectory = new JSONObject(dText);
                        return ;
                    }
                }
                
                String origDir = LoadTextFile("directory.json");
                if (origDir == null || !origDir.equals(LoadTextFile("directory.tmp"))) {
                    File orig = new File(dataDir, "directory.json");
                    if (orig.exists())
                        orig.delete();
                    new File(dataDir, "directory.tmp").renameTo(orig);
                    origDir = LoadTextFile("directory.json");

                }

                JSONObject dir = new JSONObject(origDir);
                int tFiles = 0;

                String[] dTypes = new String[]{"audio", "video"};
                String[] extTypes = new String[]{"mp3", "mp4"};
                for (int i = 0; i < dTypes.length; i++) {
                    JSONArray tList = dir.getJSONArray(dTypes[i]);
                    for (int j = 0; j < tList.length(); j++) {
                        JSONObject elm = tList.getJSONObject(j);
                        String url = elm.getString("URL");

                        String localName;
                        if (elm.has("localName"))
                            localName = elm.getString("localName");
                        else
                            localName = String.format("%s-%d.%s", dTypes[i], j, extTypes[i]);

                        Boolean upTodate = false;
                        File dstFile = new File(dataDir, localName);
                        if (elm.has("Size")) {
                            long sz = elm.getLong("Size");

                            if (dstFile.exists()) {
                                long curSze = dstFile.length();
                                if (dstFile.length() ==sz) {
                                    upTodate = true;
                                }
                            }
                        } else {
                            if (dstFile.exists()) {
                                long remoteSz = GetURLFileSize(url);
                                long curSze = dstFile.length();

                                if (dstFile.length() == remoteSz) {
                                    upTodate = true;
                                }
                            }
                        }

                        if (!upTodate) {
                            DownloadURL(url, "tmp", localName);
                            tFiles++;
                            if (dstFile.exists())
                                dstFile.delete();
                            new File(dataDir, "tmp").renameTo(dstFile);
                        }
                    }
                }
                mDM.dataDirectory = dir;

                if (mDM.onProgressCallback!=null) {
                    if (tFiles == 0 )
                        mDM.onProgressCallback.onVoiceCue("Media ready");
                    else
                        mDM.onProgressCallback.onVoiceCue("Finished downloading " + String.valueOf(tFiles) + " files");
                }


            } catch (Throwable th) {
                if (mDM.onProgressCallback!=null)
                    mDM.onProgressCallback.onVoiceCue(th.getMessage());
            }
        }
    }
}