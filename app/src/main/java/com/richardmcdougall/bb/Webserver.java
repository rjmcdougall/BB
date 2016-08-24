package com.richardmcdougall.bb;

import android.content.res.AssetManager;

import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.*;
import java.net.*;
import java.util.*;


import java.net.ServerSocket;

/**
 * Created by Jonathan on 8/19/2016.
 */
public class Webserver implements Runnable {
    /*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


    /**
     * Implementation of a very basic HTTP server. The contents are loaded from the assets folder. This
     * server handles one request at a time. It only supports GET method.
     */
    /**
     * Get IP address from first non-localhost interface
     * @param ipv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    MainActivity mMain;
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }


    private static final String TAG = "SimpleWebServer";

    /**
     * The port number we listen to
     */
    private final int mPort;

    /**
     * {@link android.content.res.AssetManager} for loading files to serve.
     */
    private final AssetManager mAssets;

    /**
     * True if the server is running.
     */
    private boolean mIsRunning;

    /**
     * The {@link java.net.ServerSocket} that we listen to.
     */
    private ServerSocket mServerSocket;

    /**
     * WebServer constructor.
     */
    public Webserver(int port, AssetManager assets, MainActivity mainActivity) {
        mPort = port;
        mAssets = assets;
        mMain = mainActivity;
    }

    /**
     * This method starts the web server listening to the specified port.
     */
    public void start() {
        mIsRunning = true;
        Log.i(TAG, "IP ADDRESS: " + getIPAddress(true));
        new Thread(this).start();
    }

    /**
     * This method stops the web server
     */
    public void stop() {
        try {
            mIsRunning = false;
            if (null != mServerSocket) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing the server socket.", e);
        }
    }

    @Override
    public void run() {
        try {
            mServerSocket = new ServerSocket(mPort);
            while (mIsRunning) {
                final Socket socket = mServerSocket.accept();

                Thread t = new Thread(new Runnable() {
                    public void run()
                    {
                        handle(socket);
                    }
                });
                t.start();
            }
        } catch (SocketException e) {
            Log.e(TAG, e.toString());
            // The server was stopped; ignore.
        } catch (IOException e) {
            Log.e(TAG, "Web server error.", e);
        } catch (Throwable e) {
            Log.e(TAG, e.toString());
        }

    }

    /**
     * Respond to a request from a client.
     *
     * @param socket The client socket.
     * @throws IOException
     */
    private void handle(Socket socket) {
        BufferedReader reader = null;
        PrintStream output = null;

        try {
            String route = null;

            // Read HTTP headers and parse out the route.
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (!TextUtils.isEmpty(line = reader.readLine())) {
                if (line.startsWith("GET /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    route = line.substring(start, end);
                    break;
                }
            }


            OutputStream out = socket.getOutputStream();
            socket.setKeepAlive(false);
            String o = "";


            o += "HTTP/1.0 200 OK\r\n";
            o += "Content-Type: text/html\r\n";
            o += "\r\n";

            while (true) {
                o += Long.toString(mMain.GetCurrentClock()) + "\r\n";
                out.write(o.getBytes());
                out.flush();
                o = "";
                try {
                    Thread.sleep(1000);
                } catch (Throwable er) {
                    Log.e(TAG, er.toString());
                }
                //Log.i(TAG, "socket status" + socket.isConnected() + ": " + socket.isOutputShutdown() + " : " + socket.isBound() + ": " + socket.isClosed());
            }

        }
        catch (Throwable er) {
            Log.e(TAG, er.toString());
            try {
                reader.close();
                socket.close();
            } catch (Throwable e) {

            }
        }
    }

    /**
     * Writes a server error response (HTTP/1.0 500) to the given output stream.
     *
     * @param output The output stream.
     */
    private void writeServerError(PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
    }

    /**
     * Loads all the content of {@code fileName}.
     *
     * @param fileName The name of the file.
     * @return The content of the file.
     * @throws IOException
     */
    private byte[] loadContent(String fileName) throws IOException {
        if (fileName.equals("getTime")) {
            return (Long.toString(mMain.GetCurrentClock())+"\r\n").getBytes();
        }
        else
            return "Time server ready!\r\n".getBytes();
        /*
        InputStream input = null;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input = mAssets.open(fileName);
            byte[] buffer = new byte[1024];
            int size;
            while (-1 != (size = input.read(buffer))) {
                output.write(buffer, 0, size);
            }
            output.flush();
            return output.toByteArray();
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (null != input) {
                input.close();
            }
        } */
    }

    /**
     * Detects the MIME type from the {@code fileName}.
     *
     * @param fileName The name of the file.
     * @return A MIME type.
     */
    private String detectMimeType(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        } else if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else {
            return "text/html";
        }
    }

}
