package com.richardmcdougall.bb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttTraceHandler;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.joda.time.DateTime;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Created by rmc on 9/16/17.
 */

public class IoTClient {
    private String TAG = this.getClass().getSimpleName();

    private static final String MQTT_SERVER_URI = "ssl://mqtt.googleapis.com:8883";
    private static final String DEVICEY_REGISTRY = "projects/burner-board/locations/us-central1/registries/bb-registry/devices/bb-";
    private volatile MqttAndroidClient mqttClient = null;
    private String deviceId;
    private IMqttToken token = null;
    private byte[] keyBytes = new byte[16384];
    private String jwtKey = null;
    private boolean haveConnected = false;
    private WifiManager mWiFiManager = null;
    private BBService service = null;

    public IoTClient(BBService service) {
        this.service = service;

        BLog.d(TAG, "Creating MQTT Client");
        IntentFilter intentf = new IntentFilter();
        setClientID();
        InputStream keyfile = service.context.getResources().openRawResource(service.context.
                getResources().getIdentifier("rsa_private_pkcs8", "raw", service.context.getPackageName()));
        int kfSize = 0;
        try {
            kfSize = keyfile.read(keyBytes);
        } catch (Exception e) {
            BLog.d(TAG, "Unable to open keyfile");
        }
        byte[] compactKey = new byte[kfSize];
        System.arraycopy(keyBytes, 0, compactKey, 0, kfSize);
        keyBytes = compactKey;

        mWiFiManager = (WifiManager) this.service.context.getSystemService(Context.WIFI_SERVICE);

        BLog.d(TAG, "connect(google, " + deviceId + ")");
        String filesDir = this.service.context.getFilesDir().getAbsolutePath();
        mqttClient = new MqttAndroidClient(service.context, MQTT_SERVER_URI,
                deviceId, new MemoryPersistence());

    }

    void Run() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("BB Board Display");
                MQTTThread();
            }
        });
        t.start();
    }

    public void MQTTThread() {

        Thread.currentThread().setName("BB MQTT Monitor");

        while (true) {

            if (haveConnected == false) {
                try {
                    // Disconnect if we were connected
                    // Required to recalculate jwtkey
                    if (mqttClient != null) {
                        BLog.d(TAG, "Disconnecting MQTT Client");
                        if (mqttClient.isConnected()) {
                            mqttClient.disconnect();
                        }
                        //mqttClient.close();
                    }
                } catch (Exception e) {
                }
                try {

                    BLog.d(TAG, "Connecting MQTT Client");

                    jwtKey = createJwtRsa("burner-board");
                    BLog.d(TAG, "Created key " + jwtKey.toString());

                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setCleanSession(false);
                    options.setAutomaticReconnect(false);
                    options.setUserName("burnerboard");
                    options.setPassword(jwtKey.toCharArray());
                    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
                    mqttClient.setCallback(new MqttEventCallback());
                    mqttClient.setTraceCallback(new MqttTraceHandlerCallback());
                    mqttClient.setTraceEnabled(false);

                    mqttClient.connect(options, null, new IMqttActionListener() {

                        @Override
                        public void onFailure(IMqttToken asyncActionToken,
                                              Throwable exception) {
                            BLog.e(TAG, "onFailure: " + exception.getMessage());
                        }

                        @Override
                        public void onSuccess(IMqttToken iMqttToken) {
                            BLog.i(TAG, "onSuccess");
                            haveConnected = true;
                        }
                    });
                } catch (Exception e) {
                    BLog.e(TAG, "connect: " + e.getMessage());
                }
            } else {
                // We get failed reconnects because JWT key expires in Google IoT
                // Force a disconnect then reconnect
                android.net.wifi.SupplicantState s = mWiFiManager.getConnectionInfo().getSupplicantState();
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(s);
                if (state == NetworkInfo.DetailedState.CONNECTED) {
                    if (mqttClient.isConnected() == false) {
                        try {
                            BLog.d(TAG, "MQTT disconnected but Wifi connected, forcing disconnect");
                            haveConnected = false;
                        } catch (Exception e) {
                        }
                    }
                }

                // Sent fake wake-up event to android MQTT Client
                        /*
                        try {
                            Intent in = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);
                        } catch (Exception e) {
                            Log.d(TAG, "could not send CONNECTIVITY_ACTION");
                        }
                        */

            }
            try {
                Intent intent = new Intent("com.android.server.NetworkTimeUpdateService.action.POLL", null);
                service.context.sendBroadcast(intent);
            } catch (Exception e) {
                BLog.e(TAG, "Cannot send force-time-sync");
            }

            // Every  minute
            try {
                Thread.sleep(300000);
            } catch (Exception e) {
            }

        }
    }

    private class MqttEventCallback implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (reconnect) {
                BLog.i(TAG, "Reconnection complete, " +
                        mqttClient.getBufferedMessageCount() + " messages ready to send");
            } else {
                BLog.i(TAG, "Connection complete");
            }
        }

        @Override
        public void connectionLost(Throwable arg0) {
            BLog.i(TAG, "Connection Lost");
            try {
                //mqttClient.disconnect();
                //mqttClient.close();
            } catch (Exception e) {
            }
            haveConnected = false;
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            //Log.i(TAG, "Message delivered, " +
            //mqttClient.getBufferedMessageCount() + " messages outstanding");
        }

        @Override
        @SuppressLint("NewApi")
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {
            BLog.i(TAG, "Message arrived from topic " + topic + ": " + msg.getPayload().toString());
            Handler h = new Handler(service.context.getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    //Intent launchA = new Intent(MQTTService.this, FullscreenActivityTest.class);
                    //launchA.putExtra("message", msg.getPayload());

                }
            });
        }
    }

    private class MqttTraceHandlerCallback implements MqttTraceHandler {
        @Override
        public void traceDebug(String tag, String message) {
            BLog.d(TAG, "trace: " + tag + " : " + message);
        }

        @Override
        public void traceError(String tag, String message) {
            BLog.e(TAG, "trace error: " + tag + " : " + message);

        }

        @Override
        public void traceException(String tag, String message, Exception e) {
            BLog.e(TAG, "trace exception: " + tag + " : " + message);
        }
    }

    public void sendUpdate(String topic, String content) {
        int qos = 1;
        IMqttDeliveryToken deliveryToken;

        if (haveConnected == true) {
            //Log.d(TAG, "sendUpdate(), " +
            //        mqttClient.getBufferedMessageCount() + " messages ready to send");
        } else {
            //Log.d(TAG, "sendUpdate()");
        }

        if (mqttClient == null) {
            //Log.d(TAG, "null mqttClient");
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
            String t = new String("/devices/bb-" +
                    service.boardState.BOARD_ID.replaceAll("\\s", "") + "" +
                    "/events/" + topic);
            //String t = new String("/devices/bb-test/events/" + topic);
            //Log.d(TAG, "mqttClient(" + t + ", " + fullMessage + ")");
            mqttClient.publish(t, message);
        } catch (Exception e) {
            BLog.e(TAG, "Failed to send:" + e.toString());
        }

        if (mqttClient.isConnected() == false && haveConnected) {
            BLog.d(TAG, mqttClient.getBufferedMessageCount() + " messages in buffer");
        }
    }

    private void setClientID() {
        deviceId = new String(
                DEVICEY_REGISTRY + service.boardState.BOARD_ID.replaceAll("\\s", ""));
    }

    /**
     * Create a Cloud IoT Core JWT for the given project id, signed with the given private key.
     */
    private String createJwtRsa(String projectId) throws Exception {

        DateTime now = new DateTime();

        JwtBuilder jwtBuilder =
                Jwts.builder()
                        .setIssuedAt(now.toDate())
                        .setExpiration(now.plusMinutes(20).toDate())
                        //.setExpiration(now.plusSeconds(20).toDate())
                        .setAudience(projectId);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        Key key = null;
        try {
            key = kf.generatePrivate(spec);
        } catch (Exception e1) {
            BLog.e(TAG, e1.toString());
        }

        String s = "none";
        try {
            s = jwtBuilder.signWith(SignatureAlgorithm.RS256, key).compact();
        } catch (Exception e) {
            BLog.e(TAG, e.toString());
        }
        return s;
    }

}