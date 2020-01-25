package com.richardmcdougall.bb;

import android.content.Context;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Created by rmc on 9/16/17.
 */

public class IoTClient {
    private static final String MQTT_SERVER_URI = "ssl://mqtt.googleapis.com:8883";
    private static final String DEVICEY_REGISTRY = "projects/burner-board/locations/us-central1/registries/bb-registry/devices/bb-";
    private String TAG = this.getClass().getSimpleName();
    private volatile MqttAndroidClient mqttClient = null;
    private String deviceId;
    private byte[] keyBytes = new byte[16384];
    private String jwtKey = null;
    private boolean haveConnected = false;
    private WifiManager mWiFiManager;
    private BBService service;

    ScheduledThreadPoolExecutor sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    Runnable iotRunner = () -> MQTTThread();

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


        sch.scheduleWithFixedDelay(iotRunner, 5, 30, TimeUnit.SECONDS);
        //sch.scheduleWithFixedDelay(iotRunner, 5, 300, TimeUnit.SECONDS);
    }

    public void MQTTThread() {

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
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
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
        }
    }

    public void sendUpdate(String topic, String content) {
        int qos = 1;

        String format = "yyyy-MM-dd'T'HH:mm:ss";
        final SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(new Date());

        String fullMessage = utcTime + "," + content;

        MqttMessage message = new MqttMessage(fullMessage.getBytes());
        message.setQos(qos);
        message.setRetained(true);

        try {
            String t = "/devices/bb-" + service.boardState.BOARD_ID.replaceAll("\\s", "") + "" + "/events/" + topic;

            mqttClient.publish(t, message);
        } catch (Exception e) {
            BLog.e(TAG, "Failed to send:" + e.toString());
        }

        if (mqttClient.isConnected() == false && haveConnected) {
            BLog.d(TAG, mqttClient.getBufferedMessageCount() + " messages in buffer");
        }
    }

    private void setClientID() {
        deviceId = DEVICEY_REGISTRY + service.boardState.BOARD_ID.replaceAll("\\s", "");
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
            } catch (Exception e) {
            }
            haveConnected = false;
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            BLog.d(TAG, "delivery complete: ");
        }

        @Override
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {
            BLog.i(TAG, "Message arrived from topic " + topic + ": " + msg.getPayload().toString());
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

}