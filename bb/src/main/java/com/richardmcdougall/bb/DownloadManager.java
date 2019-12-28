package com.richardmcdougall.bb;

import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.net.URLEncoder;

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private BBService service;

    interface OnDownloadProgressType {
        void onProgress(String file, long fileSize, long bytesDownloaded);

        void onVoiceCue(String err);
    }

    public void d(String logMsg) {
        if (DebugConfigs.DEBUG_DOWNLOAD_MANAGER) {
            Log.d(TAG, logMsg);
        }
    }

    public void e(String logMsg) {
            Log.e(TAG, logMsg);
    }
    public OnDownloadProgressType onProgressCallback = null;

    DownloadManager(BBService service ) {
        this.service = service;

        this.onProgressCallback = new DownloadManager.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    service.voice.speak("Downloading " + file + ", " + percent + " Percent", TextToSpeech.QUEUE_ADD, null, "downloading");
                    lastTextTime = curTime;
                    d("Downloading " + file + ", " + percent + " Percent");
                }
            }

            public void onVoiceCue(String msg) {
                service.voice.speak(msg, TextToSpeech.QUEUE_ADD, null, "Download Message");
            }
        };

        LoadInitialDataDirectory();  // get started right away using the data we have on the board already (if any)

        Log.d(TAG, "Downloading files to: " + service.filesDir);
    }

    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run()
            {
                Thread.currentThread().setName("DownloadManager");
                StartDownloadManager();
            }
        });
        t.start();
    }


    public void CleanupOldFiles() {
        try {
            ArrayList<String> refrerencedFiles = new ArrayList<String>();

            JSONObject dir = service.boardState.dataDirectory;

            String[] dTypes = new String[]{"audio", "video"};
            String[] extTypes = new String[]{"mp3", "mp4", "m4a"};
            for (int i = 0; i < dTypes.length; i++) {
                JSONArray tList = dir.getJSONArray(dTypes[i]);
                for (int j = 0; j < tList.length(); j++) {
                    JSONObject elm = tList.getJSONObject(j);
                    if (elm.has("localName")) {
                        refrerencedFiles.add(elm.getString("localName"));
                    }
                }
            }

            File[] flist = new File(service.filesDir).listFiles();
            for (int i = 0; i < flist.length; i++) {
                String fname = flist[i].getName();

                if (fname.endsWith(".mp4") || fname.endsWith(".mp3") || fname.endsWith(".m4a")) {
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
            File[] flist = new File(service.filesDir).listFiles();
            if (flist != null) {

                // if files are no longer referenced in the Data Directory, delete them.
                String origDir = FileHelpers.LoadTextFile("directory.json", service.filesDir);
                if (origDir != null) {
                    JSONObject dir = new JSONObject(origDir);
                    service.boardState.dataDirectory = dir;
                    CleanupOldFiles();
                }
            }

        } catch (Throwable e) {
            e(e.getMessage());
            onProgressCallback.onVoiceCue("Error loading media error due to jason error");
        }
    }

    // Spaces were breaking the download
    public String encodeURL(String url) {
        try {
            String[] urlArray = url.split("/");
            urlArray[urlArray.length - 1] = URLEncoder.encode(urlArray[urlArray.length - 1], "UTF-8").replaceAll("\\+", "%20");
            urlArray[urlArray.length - 2] = URLEncoder.encode(urlArray[urlArray.length - 2], "UTF-8").replaceAll("\\+", "%20");
            urlArray[urlArray.length - 3] = URLEncoder.encode(urlArray[urlArray.length - 3], "UTF-8").replaceAll("\\+", "%20");
            return TextUtils.join("/", urlArray);
        } catch (Throwable th) {
            return "";
        }
    }

    // XXX this only works for file, not algorithms!!! -jib
    // See: https://github.com/rjmcdougall/BB/issues/171
    private boolean isUpToDate(JSONObject elm) {
        try {

            String localName = elm.getString("localName");
            boolean upToDate = false;
            File dstFile = new File(service.filesDir, localName);

            if (elm.has("Size")) {
                long sz = elm.getLong("Size");
                if (dstFile.exists())
                    if (dstFile.length() == sz)
                        upToDate = true;
            }
            return upToDate;

        } catch (JSONException jse) {
            e("Error " + jse.getMessage());
            return false;
        }
    }

    private boolean replaceFile(JSONObject elm) {

        try {
            String localName = elm.getString("localName");
            String url = encodeURL(elm.getString("URL"));
            FileHelpers.DownloadURL(url, "tmp", localName, onProgressCallback,service.filesDir);   // download to a "tmp" file
            File dstFile2 = new File(service.filesDir, localName);   // move to localname so that we can install it

            if (dstFile2.exists())
                dstFile2.delete();
            new File(service.filesDir, "tmp").renameTo(dstFile2);
            return true;

        } catch (JSONException jse) {
            e(jse.getMessage());
            return false;
        } catch (Throwable th) {
            e(th.getMessage());
            return false;
        }

    }

    public boolean GetNewDirectory() {

        try {
            String DirectoryURL = "";
            DirectoryURL = "https://us-central1-burner-board.cloudfunctions.net/boards/" + service.boardState.BOARD_ID;


            DirectoryURL = encodeURL(DirectoryURL) + "/DownloadDirectoryJSON?APKVersion=" + service.version ;
            boolean returnValue = true;

            long ddsz = FileHelpers.DownloadURL(DirectoryURL, "tmp", "Directory", onProgressCallback,service.filesDir);
            if (ddsz < 0) {
                d("Unable to Download DirectoryJSON.  Sleeping for 5 seconds. ");
                returnValue  = false;
            }
            else {
                d("Reading Directory from " + DirectoryURL);

                new File(service.filesDir, "tmp").renameTo(new File(service.filesDir, "directory.json.tmp"));

                String dirTxt = FileHelpers.LoadTextFile("directory.json.tmp", service.filesDir);
                JSONObject dir = new JSONObject(dirTxt);
                String[] dTypes = new String[]{"audio", "video"};
                JSONArray changedFiles = new JSONArray();

                d("Downloaded JSON: " + dirTxt);

                // determine changes
                for (int i = 0; i < dTypes.length; i++) {
                    JSONArray tList = dir.getJSONArray(dTypes[i]);
                    for (int j = 0; j < tList.length(); j++) {

                        JSONObject elm = tList.getJSONObject(j);

                        // if there is no URL, it is an algorithm and should be skipped for download.
                        // Note, NEW entries in the DB always have a 'URL' field, so we have to check the contents --jib
                        if (elm.has("URL") &&
                                !elm.isNull("URL") &&
                                elm.getString("URL").length() > 0
                        ) {
                            if (!isUpToDate(elm))
                                changedFiles.put(elm);
                        }
                    }
                }

                // announce changes
                if (changedFiles.length() > 0) {
                    if (onProgressCallback != null)
                        onProgressCallback.onVoiceCue(changedFiles.length() + " Media Changes Detected. Downloading.");
                } else {
                    d("No Changes to Directory JSON.");
                    returnValue = true;
                }

                if(returnValue) {
                    // download changes
                    for (int j = 0; j < changedFiles.length(); j++) {
                        JSONObject elm = changedFiles.getJSONObject(j);
                        replaceFile(elm);
                    }

                    // announce completion and set the media object to the new profile
                    if (changedFiles.length() > 0) {
                        CleanupOldFiles();
                        if (onProgressCallback != null) {
                            String diag = "Finished downloading " + String.valueOf(changedFiles.length()) + " files. Media ready.";
                            d(diag);
                            onProgressCallback.onVoiceCue(diag);
                        }
                    }

                    // Replace the directory object.
                    service.boardState.dataDirectory = dir;
                    new File(service.filesDir, "directory.json.tmp").renameTo(new File(service.filesDir, "directory.json"));

                }
            }

            return returnValue;

        } catch (JSONException jse) {
            e( jse.getMessage());
            return false;
        } catch (Throwable th) {
            e( th.getMessage());
            return false;
        }
    }

    public void StartDownloadManager() {

        try {
            boolean downloadSuccessDirectory = false;
            int i = 0;

            // On boot it can take some time to get an internet connection.  Check every 5 seconds for 5 minutes
            while (!(downloadSuccessDirectory)
                    && i < 60) { // try this for 5 minutes.
                i++;

                if (!downloadSuccessDirectory)
                    downloadSuccessDirectory = GetNewDirectory();

                Thread.sleep(5000);   // no internet, wait 5 seconds before we try again
            }

            // After the first boot check periodically in case the profile has changed.
            while (true) {
                GetNewDirectory();
                Thread.sleep(120000);   // no internet, wait 2 minutes before we try again
            }
        } catch (Throwable th) {
            if (onProgressCallback != null)
                onProgressCallback.onVoiceCue(th.getMessage());
            try {
                Thread.sleep(10000);   // wait 10 second if unexpected error
            } catch (Throwable er) {

            }
        }
    }


}


