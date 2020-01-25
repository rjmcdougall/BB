package com.richardmcdougall.bb;

import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MediaManager {
    private String TAG = this.getClass().getSimpleName();

    private static final String DIRECTORY_JSON_FILENAME = "directory.json";
    private static final String DIRECTORY_JSON_TMP_FILENAME = "directory.json.tmp";
    private static final String DIRECTORY_URL = "https://us-central1-burner-board.cloudfunctions.net/boards/";
    private static final String DOWNLOAD_DIRECTORY_URL_PATH = "/DownloadDirectoryJSON?APKVersion=";

    private BBService service;
    private JSONObject dataDirectory;
    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    private JSONArray audio() {
        JSONArray audio = null;
        try {
            audio = new JSONArray(service.mediaManager.dataDirectory.getJSONArray("audio").toString());
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
        }
        return audio;
    }

    private JSONArray video() {
        JSONArray video = null;
        try {
            video = new JSONArray(service.mediaManager.dataDirectory.getJSONArray("video").toString());
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
        }
        return video;
    }

    public JSONArray MinimizedAudio() {

        // Add audio + video media lists. remove unecessary attributes to reduce ble message length.
        JSONArray audio = audio();
        if (audio() == null) {
            BLog.d(TAG, "Could not get audio directory (null)");
        } else {
            try {
                for (int i = 0; i < audio.length(); i++) {
                    JSONObject a = audio.getJSONObject(i);
                    if (a.has("URL")) a.remove("URL");
                    if (a.has("ordinal")) a.remove("ordinal");
                    if (a.has("Size")) a.remove("Size");
                    if (a.has("Length")) a.remove("Length");
                }

            } catch (Exception e) {
                BLog.e(TAG, "Could not get audio directory: " + e.getMessage());
            }
        }
        return audio;
    }

    public JSONArray MinimizedVideo() {

        // Add audio + video media lists. remove unecessary attributes to reduce ble message length.
        JSONArray video = video();

        if (video == null) {
            BLog.d(TAG, "Could not get video directory (null)");
        } else {
            try {
                for (int i = 0; i < video.length(); i++) {
                    JSONObject v = video.getJSONObject(i);
                    if (v.has("URL")) v.remove("URL");
                    if (v.has("ordinal")) v.remove("ordinal");
                    if (v.has("Size")) v.remove("Size");
                    if (v.has("SpeachCue")) v.remove("SpeachCue");
                    if (v.has("Length")) v.remove("Length");
                }

            } catch (Exception e) {
                BLog.e(TAG, "Could not get video directory: " + e.getMessage());
            }
        }
        return video;
    }

    interface OnDownloadProgressType {
        void onProgress(String file, long fileSize, long bytesDownloaded);

        void onVoiceCue(String err);
    }

    public OnDownloadProgressType onProgressCallback = null;

    MediaManager(BBService service) {
        this.service = service;

        this.onProgressCallback = new MediaManager.OnDownloadProgressType() {
            long lastTextTime = 0;

            public void onProgress(String file, long fileSize, long bytesDownloaded) {
                if (fileSize <= 0)
                    return;

                long curTime = System.currentTimeMillis();
                if (curTime - lastTextTime > 30000) {
                    lastTextTime = curTime;
                    long percent = bytesDownloaded * 100 / fileSize;

                    service.speak("Downloading " + file + ", " + percent + " Percent", "downloading");
                    lastTextTime = curTime;
                    BLog.d(TAG, "Downloading " + file + ", " + percent + " Percent");
                }
            }

            public void onVoiceCue(String msg) {
                service.speak(msg, "Download Message");
            }
        };

        LoadInitialDataDirectory();  // get started right away using the data we have on the board already (if any)

        // wait 8 seconds to hopefully get wifi before starting the download.
        Runnable checkForMedia = () ->  StartDownloadManager();
        sch.schedule(checkForMedia, 8, TimeUnit.SECONDS);

        BLog.d(TAG, "Downloading files to: " + service.filesDir);
    }

    public static long hashTrackName(String name) {
        byte[] encoded = {0, 0, 0, 0};
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            encoded = digest.digest(name.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return -1;
        }
        return (encoded[0] << 24) + (encoded[1] << 16) + (encoded[2] << 8) + encoded[0];
    }

    public void CleanupOldFiles() {
        try {
            ArrayList<String> refrerencedFiles = new ArrayList<String>();

            JSONObject dir = dataDirectory;

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
                String origDir = FileHelpers.LoadTextFile(DIRECTORY_JSON_FILENAME, service.filesDir);
                if (origDir != null) {
                    JSONObject dir = new JSONObject(origDir);
                    dataDirectory = dir;
                    CleanupOldFiles();
                }
            }

        } catch (Throwable e) {
            BLog.e(TAG, e.getMessage());
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
            BLog.e(TAG, "Error " + jse.getMessage());
            return false;
        }
    }

    private boolean replaceFile(JSONObject elm) {

        try {
            String localName = elm.getString("localName");
            String url = encodeURL(elm.getString("URL"));
            FileHelpers.DownloadURL(url, "tmp", localName, onProgressCallback, service.filesDir);   // download to a "tmp" file
            File dstFile2 = new File(service.filesDir, localName);   // move to localname so that we can install it

            if (dstFile2.exists())
                dstFile2.delete();
            new File(service.filesDir, "tmp").renameTo(dstFile2);
            return true;

        } catch (JSONException jse) {
            BLog.e(TAG, jse.getMessage());
            return false;
        } catch (Throwable th) {
            BLog.e(TAG, th.getMessage());
            return false;
        }

    }

    public boolean GetNewDirectory() {

        try {
            String DirectoryURL = DIRECTORY_URL + service.boardState.BOARD_ID;

            DirectoryURL = encodeURL(DirectoryURL) + DOWNLOAD_DIRECTORY_URL_PATH + service.boardState.version;
            boolean returnValue = true;

            long ddsz = FileHelpers.DownloadURL(DirectoryURL, "tmp", "Directory", onProgressCallback, service.filesDir);
            if (ddsz < 0) {
                BLog.d(TAG, "Unable to Download DirectoryJSON.  Sleeping for 5 seconds. ");
                returnValue = false;
            } else {
                BLog.d(TAG, "Reading Directory from " + DirectoryURL);

                new File(service.filesDir, "tmp").renameTo(new File(service.filesDir, DIRECTORY_JSON_TMP_FILENAME));

                String dirTxt = FileHelpers.LoadTextFile(DIRECTORY_JSON_TMP_FILENAME, service.filesDir);
                JSONObject dir = new JSONObject(dirTxt);
                String[] dTypes = new String[]{"audio", "video"};
                JSONArray changedFiles = new JSONArray();

                BLog.d(TAG, "Downloaded JSON: " + dirTxt);

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
                    BLog.d(TAG, "No Changes to Directory JSON.");
                    returnValue = true;
                }

                if (returnValue) {
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
                            BLog.d(TAG, diag);
                            onProgressCallback.onVoiceCue(diag);
                        }
                    }

                    // Replace the directory object.
                    dataDirectory = dir;
                    new File(service.filesDir, DIRECTORY_JSON_TMP_FILENAME).renameTo(new File(service.filesDir, DIRECTORY_JSON_FILENAME));

                }
            }

            return returnValue;

        } catch (JSONException jse) {
            BLog.e(TAG, jse.getMessage());
            return false;
        } catch (Throwable th) {
            BLog.e(TAG, th.getMessage());
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

    String GetAudioFile() {
        try {
            String fn = service.filesDir + "/" + GetAudio().getString("localName");
            return fn;
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return null;
        }
    }

    String GetAudioFileLocalName(int index) {
        if (index >= 0 && index < GetTotalAudio()) {
            try {
                String fn = GetAudio().getString("localName");
                return fn;
            } catch (JSONException e) {
                BLog.e(TAG, e.getMessage());
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
                if (GetVideo().has("friendlyName")) {
                    fn = GetVideo().getString("friendlyName");
                } else {
                    if (GetVideo().has("algorithm"))
                        fn = GetVideo().getString("algorithm");
                    else
                        fn = GetVideo().getString("localName");
                }
                return fn;
            } catch (JSONException e) {
                BLog.e(TAG, e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String GetVideoFile(int index) {
        try {
            String fn = service.filesDir + "/" + GetVideo().getString("localName");
            return fn;
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return null;
        }
    }

    int GetTotalAudio() {

        try {
            return dataDirectory.getJSONArray("audio").length();
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return 0;
        }

    }

    int GetMapMode() {
        if (dataDirectory.has("video")) {
            try {
                int map = -1;
                JSONArray j = dataDirectory.getJSONArray("video");

                for (int i = 0; i < j.length(); i++) {
                    JSONObject v = j.getJSONObject(i);
                    if(v.has("algorithm")){
                        String s = v.getString("algorithm");
                        if(s.equals("modePlayaMap()")){
                            map = i;
                        }
                    }
                }

                return map;
            } catch (JSONException e) {
                BLog.e(TAG, e.getMessage());
                return -1;
            }
        } else
            return -1;
    }

    public int GetTotalVideo() {
        try {
            return dataDirectory.getJSONArray("video").length();
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return 0;
        }
    }

    JSONObject GetVideo() {
        if (dataDirectory.has("video")) {
            try {
                return dataDirectory.getJSONArray("video").getJSONObject(service.boardState.currentVideoMode);
            } catch (JSONException e) {
                BLog.e(TAG, e.getMessage());
                return null;
            }
        } else
            return null;
    }

    String GetAlgorithm() {
        if (dataDirectory.has("video")) {
            try {
                return dataDirectory.getJSONArray("video").getJSONObject(service.boardState.currentVideoMode).getString("algorithm");
            } catch (JSONException e) {
                BLog.e(TAG, e.getMessage());
                return null;
            }
        } else
            return null;
    }

    JSONObject GetAudio() {
        if (dataDirectory.has("audio")) {
            try {
                return dataDirectory.getJSONArray("audio").getJSONObject(service.boardState.currentRadioChannel);
            } catch (JSONException e) {
                BLog.e(TAG, e.getMessage());
                return null;
            }
        } else
            return null;
    }

    long GetAudioLength() {
        try {
            return GetAudio().getLong("Length");
        } catch (JSONException e) {
            BLog.e(TAG, e.getMessage());
            return 1000;   // return a dummy value
        }
    }
}