package com.richardmcdougall.bb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;



public class FileHelpers {
    private static String TAG = "FileHelpers";

    public static String LoadTextFile(String filename, String fileDir) {
        try {
            File f = new File(fileDir, filename);
            if (!f.exists())
                return null;

            InputStream is = null;
            try {
                is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                BLog.e(TAG,e.getMessage());
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
            BLog.e(TAG,e.getMessage());
            return null;
        }
    }

    public static long DownloadURL(String URLString, String filename, String progressName, MediaManager.OnDownloadProgressType onProgressCallback, String filesDir) {
        try {
            URL url = new URL(URLString);

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            File file = new File(filesDir, filename);
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

            BLog.d(TAG,"Downloading " + URLString);
            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadSize += bufferLength;

                if (onProgressCallback != null && progressName != "Boards JSON" && progressName != "Directory") {
                    onProgressCallback.onProgress(progressName, fileSize, downloadSize);

                }
            }
            fileOutput.close();
            urlConnection.disconnect();

            return downloadSize;

        } catch (FileNotFoundException e) {
            BLog.e(TAG,"An exception occured access the file from burnerboard.com. This is likely the result of having an unregistered board.");
            return -1;
        } catch (UnknownHostException e) {
            BLog.e(TAG,"An exception occured access the boards file from burnerboard.com. This is likely the result of not having an internet connection.");
            return -1;
        } catch (ConnectException e) {
            BLog.e(TAG,"An exception occured access the boards file from burnerboard.com. This is likely the result of not having an internet connection.");
            return -1;
        } catch (Exception e) {
            BLog.e(TAG,"An exception occured access the boards file from burnerboard.com. " + e.getMessage());
            return -1;
        }
    }

}
