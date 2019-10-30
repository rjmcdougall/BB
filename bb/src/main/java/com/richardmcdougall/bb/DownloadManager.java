package com.richardmcdougall.bb;

import android.content.pm.PackageInfo;
import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.net.URLEncoder;

/**
 * Created by Jonathan on 8/6/2017.
 */

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    protected String mFilesDir;
    protected JSONObject dataDirectory;
    int mVersion;
    String mBoardId;

    JSONObject GetDataDirectory() {
        return dataDirectory;
    }

    interface OnDownloadProgressType {
        public void onProgress(String file, long fileSize, long bytesDownloaded);

        public void onVoiceCue(String err);
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

    DownloadManager(String filesDir, String boardId, int myVersion) {
        Log.d(TAG, "Downloading files to: " + filesDir);
        mVersion = myVersion;
        mFilesDir = filesDir;
        PackageInfo pinfo;
        mBoardId = boardId;
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

    String GetAudioFile(int index) {
        try {
            String fn = mFilesDir + "/" + GetAudio(index).getString("localName");
            return fn;
        } catch (JSONException e) {
            e(e.getMessage());
            return null;
        }
    }

    String GetAudioFileLocalName(int index) {
        if (index >= 0 && index < GetTotalAudio()) {
            try {
                String fn = GetAudio(index).getString("localName");
                return fn;
            } catch (JSONException e) {
                e(e.getMessage());
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
                if (GetVideo(index).has("friendlyName")) {
                    fn = GetVideo(index).getString("friendlyName");
                } else {
                    if (GetVideo(index).has("algorithm"))
                        fn = GetVideo(index).getString("algorithm");
                    else
                        fn = GetVideo(index).getString("localName");
                }
                return fn;
            } catch (JSONException e) {
                e(e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String GetVideoFile(int index) {
        try {
            String fn = mFilesDir + "/" + GetVideo(index).getString("localName");
            return fn;
        } catch (JSONException e) {
            e(e.getMessage());
            return null;
        }
    }

    int GetTotalAudio() {
        if (dataDirectory == null)
            return 0;
        else {
            try {
                //Log.d(TAG, dataDirectory.getJSONArray("audio").length() + " audio files");
                return dataDirectory.getJSONArray("audio").length();
            } catch (JSONException e) {
                e(e.getMessage());
                return 0;
            }
        }
    }

    public int GetTotalVideo() {
        if (dataDirectory == null)
            return 0;
        else {
            try {
                //Log.d(TAG, dataDirectory.getJSONArray("audio").length() + " video files");
                return dataDirectory.getJSONArray("video").length();
            } catch (JSONException e) {
                e(e.getMessage());
                return 0;
            }
        }
    }


    JSONObject GetVideo(int index) {
        if (dataDirectory == null)
            return null;
        if (dataDirectory.has("video")) {
            try {
                return dataDirectory.getJSONArray("video").getJSONObject(index);
            } catch (JSONException e) {
                e(e.getMessage());
                return null;
            }
        } else
            return null;
    }

    String GetAlgorithm(int index) {
        if (dataDirectory == null)
            return null;
        if (dataDirectory.has("video")) {
            try {
                return dataDirectory.getJSONArray("video").getJSONObject(index).getString("algorithm");
            } catch (JSONException e) {
                e(e.getMessage());
                return null;
            }
        } else
            return null;
    }

    JSONObject GetAudio(int index) {
        if (dataDirectory == null)
            return null;
        if (dataDirectory.has("audio")) {
            try {
                return dataDirectory.getJSONArray("audio").getJSONObject(index);
            } catch (JSONException e) {
                e(e.getMessage());
                return null;
            }
        } else
            return null;
    }

    long GetAudioLength(int index) {
        try {
            return GetAudio(index).getLong("Length");
        } catch (JSONException e) {
            e(e.getMessage());
            return 1000;   // return a dummy value
        }
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

            String dataDir = mFilesDir;
            File[] flist = new File(dataDir).listFiles();
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
            String dataDir = mFilesDir;
            File[] flist = new File(dataDir).listFiles();
            if (flist != null) {

                // if files are no longer referenced in the Data Directory, delete them.
                String origDir = FileHelpers.LoadTextFile("directory.json", mFilesDir);
                if (origDir != null) {
                    JSONObject dir = new JSONObject(origDir);
                    dataDirectory = dir;
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
            File dstFile = new File(mFilesDir, localName);

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
            FileHelpers.DownloadURL(url, "tmp", localName, onProgressCallback,mFilesDir);   // download to a "tmp" file
            File dstFile2 = new File(mFilesDir, localName);   // move to localname so that we can install it

            if (dstFile2.exists())
                dstFile2.delete();
            new File(mFilesDir, "tmp").renameTo(dstFile2);
            return true;

        } catch (JSONException jse) {
            e("Error " + jse.getMessage());
            return false;
        } catch (Throwable th) {
            e("Error " + th.getMessage());
            return false;
        }

    }

    public boolean GetNewDirectory() {

        try {

            String DirectoryURL = "https://us-central1-burner-board.cloudfunctions.net/boards/" + mBoardId;
            DirectoryURL = encodeURL(DirectoryURL) + "/DownloadDirectoryJSON?APKVersion=" + mVersion ;
            boolean returnValue = true;

            long ddsz = FileHelpers.DownloadURL(DirectoryURL, "tmp", "Directory", onProgressCallback,mFilesDir);
            if (ddsz < 0) {
                d("Unable to Download DirectoryJSON.  Sleeping for 5 seconds. ");
                returnValue  = false;
            }

            if(returnValue) {
                d("Reading Directory from " + DirectoryURL);

                new File(mFilesDir, "tmp").renameTo(new File(mFilesDir, "directory.json.tmp"));

                String dirTxt = FileHelpers.LoadTextFile("directory.json.tmp", mFilesDir);
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

                if(!returnValue) {
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
                    dataDirectory = dir;
                    new File(mFilesDir, "directory.json.tmp").renameTo(new File(mFilesDir, "directory.json"));

                    returnValue = true;
                }
            }

            return returnValue;

        } catch (JSONException jse) {
            e("Error " + jse.getMessage());
            return false;
        } catch (Throwable th) {
            e("Error " + th.getMessage());
            return false;
        }
    }

    public void StartDownloadManager() {

        LoadInitialDataDirectory();  // get started right away using the data we have on the board already (if any)

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


