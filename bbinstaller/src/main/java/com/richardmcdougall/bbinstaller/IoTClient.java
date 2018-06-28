package com.richardmcdougall.bbinstaller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.os.Build;

import org.eclipse.paho.android.service.MqttAndroidClient;

import org.eclipse.paho.android.service.MqttTraceHandler;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.util.Debug;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.InputStream;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.joda.time.DateTime;

import static android.R.id.message;
import static android.content.ContentValues.TAG;

import org.eclipse.paho.android.service.MqttTraceHandler;

/**
 * Created by rmc on 9/16/17.
 */

public class IoTClient {

    private ConnectivityManager mConnMan;
    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    private static final String TAG = "BBI.IoTClient";
    private volatile MqttAndroidClient mqttClient = null;
    private Context mContext;
    private String deviceId;
    IMqttToken token = null;
    byte[] keyBytes = new byte[16384];
    String jwtKey = null;
    boolean haveConnected = false;
    boolean haveEverConnected = false;
    WifiManager mWiFiManager = null;
    String subscriptionTopic = new String("/devices/installer-" + Build.MODEL.replaceAll("\\s", "") + "/config");
    public IoTAction mIoTActionCallback = null;

    interface IoTAction {
        public void onAction(String action);
    }

    public IoTClient(Context context, IoTAction callback) {
        mContext = context;
        mIoTActionCallback = callback;

        Log.d(TAG, "Creating MQTT Client");
        IntentFilter intentf = new IntentFilter();
        setClientID();
        InputStream keyfile = context.getResources().openRawResource(context.
                getResources().getIdentifier("rsa_private_pkcs8", "raw", context.getPackageName()));
        try {
            keyfile.read(keyBytes);
        } catch (Exception e) {
            Log.d(TAG, "Unable to open keyfile");
        }

        mWiFiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        Log.d(TAG, "connect(google, " + deviceId + ")");
        String filesDir = mContext.getFilesDir().getAbsolutePath();
        mqttClient = new MqttAndroidClient(context, "ssl://mqtt.googleapis.com:8883",
                deviceId, new MqttDefaultFilePersistence(filesDir));

        doConnect();


    }

    public void doConnect() {
        Thread connectThread = new Thread(new Runnable() {
            public void run() {

                Thread.currentThread().setName("BB MQTT Monitor");

                while (true) {

                    if (haveConnected == false) {
                        try {
                            // Disconnect if we were connected
                            // Required to recalculate jwtkey
                            //if ((haveEverConnected == true) && (mqttClient != null)) {
                            if ((mqttClient != null)) {
                                Log.d(TAG, "Disconnecting MQTT Client");
                                mqttClient.disconnect();
                            }

                        } catch (Exception e) {
                        }

                        try {

                            Log.d(TAG, "Connecting MQTT Client");

                            jwtKey = createJwtRsa("burner-board");
                            Log.d(TAG, "Created key " + jwtKey.toString());


                            MqttConnectOptions options = new MqttConnectOptions();
                            options.setCleanSession(false);
                            options.setAutomaticReconnect(false);
                            options.setUserName("burnerboard");
                            options.setPassword(jwtKey.toCharArray());
                            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
                            mqttClient.setCallback(new MqttEventCallback());
                            mqttClient.setTraceCallback(new MqttTraceHandlerCallback());
                            mqttClient.setTraceEnabled(true);

                            mqttClient.connect(options, null, new IMqttActionListener() {

                                @Override
                                public void onFailure(IMqttToken asyncActionToken,
                                                      Throwable exception) {
                                    Log.d(TAG, "connect.onFailure: " + exception.getMessage());
                                    exception.printStackTrace();
                                }

                                @Override
                                public void onSuccess(IMqttToken iMqttToken) {
                                    Log.d(TAG, "connect.onSuccess");
                                    haveConnected = true;
                                    haveEverConnected = true;
                                    DisconnectedBufferOptions disconnectedBufferOptions =
                                            new DisconnectedBufferOptions();
                                    disconnectedBufferOptions.setBufferEnabled(true);
                                    disconnectedBufferOptions.setBufferSize(100000);
                                    disconnectedBufferOptions.setPersistBuffer(true);
                                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                                    mqttClient.setBufferOpts(disconnectedBufferOptions);


                                    try {
                                        mqttClient.subscribe(subscriptionTopic, 1, null, new IMqttActionListener() {
                                            @Override
                                            public void onSuccess(IMqttToken asyncActionToken) {
                                                Log.d(TAG, "Subscribed!");
                                            }

                                            @Override
                                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                                Log.d(TAG, "Failed to subscribe");
                                            }
                                        });
                                    } catch (Exception e) {
                                        Log.d(TAG, "Failed to subscribe");
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Log.d(TAG, "MQTT Thread Exeception" + e.toString());
                        }
                    } else {
                        try {
                            // We get failed reconnects because JWT key expires in Google IoT
                            // Force a disconnect then reconnect
                            android.net.wifi.SupplicantState s = mWiFiManager.getConnectionInfo().getSupplicantState();
                            NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);
                            if (state == NetworkInfo.DetailedState.CONNECTED) {
                                if (mqttClient.isConnected() == false) {
                                    try {
                                        Log.d(TAG, "MQTT disconnected but Wifi connected, forcing disconnect");
                                        haveConnected = false;
                                    } catch (Exception e) {
                                    }
                                }
                            }

                        } catch (Exception e) {
                        }

                    }
                    try {
                        Intent intent = new Intent("com.android.server.NetworkTimeUpdateService.action.POLL", null);
                        mContext.sendBroadcast(intent);
                    } catch (Exception e) {
                    }

                    // Every  minute
                    try {
                        Thread.sleep(60000);
                    } catch (Exception e2) {
                    }
                }

            }
        });
        connectThread.start();
    }

    private class MqttEventCallback implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (reconnect) {
                Log.i(TAG, "Reconnection complete, " +
                        mqttClient.getBufferedMessageCount() + " messages ready to send");
            } else {
                Log.i(TAG, "Connection complete");
            }
        }

        @Override
        public void connectionLost(Throwable arg0) {
            Log.i(TAG, "Connection Lost");
            try {
                //mqttClient.disconnect();
                //mqttClient.close();
            } catch (Exception e) {
            }
            haveConnected = false;
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            Log.i(TAG, "Message delivered, " +
                    mqttClient.getBufferedMessageCount() + " messages outstanding");
        }

        @Override
        @SuppressLint("NewApi")
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {
            Log.i(TAG, "Message arrived from topic " + topic + ": " + new String(msg.getPayload()));
            try {
                mIoTActionCallback.onAction(new String(msg.getPayload()));
            } catch (Exception e) {
            }
        }

    }

    private class MqttTraceHandlerCallback implements MqttTraceHandler {
        @Override
        public void traceDebug(String tag, String message) {
            Log.d(TAG, "trace: " + tag + " : " + message);
        }

        @Override
        public void traceError(String tag, String message) {
            Log.d(TAG, "trace error: " + tag + " : " + message);

        }

        @Override
        public void traceException(String tag, String message, Exception e) {
            Log.d(TAG, "trace exception: " + tag + " : " + message);
        }
    }

    public void sendUpdate(String content) {
        //int qos             = 2;
        //String broker       = "tcp://m11.cloudmqtt.com:15488";
        int qos = 1;
        IMqttDeliveryToken deliveryToken;

        if (haveConnected == true) {
            Log.d(TAG, "sendUpdate(), " +
                    mqttClient.getBufferedMessageCount() + " messages ready to send");
        } else {
            Log.d(TAG, "sendUpdate()");
        }

        if (mqttClient == null) {
            Log.d(TAG, "null mqttClient");
            return;
        }

        String format = "yyyy-MM-dd'T'HH:mm:ss";
        final SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(new Date());

        String fullMessage = utcTime + "," + content;

        MqttMessage message = new MqttMessage(fullMessage.getBytes());
        message.setQos(qos);
        message.setRetained(true);

        try {
            String t = new String("/devices/installer-" + Build.MODEL.replaceAll("\\s", "") + "/state");
            //String t = new String("/devices/bb-test/events/" + topic);
            Log.d(TAG, "mqttClient(" + t + ", " + fullMessage + ")");
            mqttClient.publish(t, message);
        } catch (Exception e) {
            Log.d(TAG, "Failed to send:" + e.toString());
        }

        if (mqttClient.isConnected() == false && haveConnected) {
            Log.d(TAG, mqttClient.getBufferedMessageCount() + " messages in buffer");
        }
    }

    private void setClientID() {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        deviceId = wInfo.getMacAddress();
        if (deviceId == null) {
            deviceId = MqttAsyncClient.generateClientId();
        }
        deviceId = new String("projects/burner-board/locations/us-central1/registries/bb-registry/devices/installer-" + Build.MODEL.replaceAll("\\s", ""));
        //deviceId = new String("projects/burner-board/locations/us-central1/registries/bb-registry/devices/bb-test");
    }

    /**
     * Create a Cloud IoT Core JWT for the given project id, signed with the given private key.
     */
    private String createJwtRsa(String projectId) throws Exception {
        DateTime now = new DateTime();
        // Create a JWT to authenticate this device. The device will be disconnected after the token
        // expires, and will have to reconnect with a new token. The audience field should always be set
        // to the GCP project id.
        JwtBuilder jwtBuilder =
                Jwts.builder()
                        .setIssuedAt(now.toDate())
                        .setExpiration(now.plusMinutes(20).toDate())
                        //.setExpiration(now.plusSeconds(20).toDate())
                        .setAudience(projectId);

        //byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyFile));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return jwtBuilder.signWith(SignatureAlgorithm.RS256, kf.generatePrivate(spec)).compact();
    }



}


