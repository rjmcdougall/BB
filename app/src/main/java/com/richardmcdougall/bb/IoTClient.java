package com.richardmcdougall.bb;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;


import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.InputStream;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import org.joda.time.DateTime;

/**
 * Created by rmc on 9/16/17.
 */

public class    IoTClient {

    private ConnectivityManager mConnMan;
    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    private static final String TAG = "IoTClient";
    private volatile IMqttAsyncClient mqttClient = null;
    private Context mContext;
    private String deviceId;
    IMqttToken token = null;
    byte[] keyBytes = new byte[16384];

    public IoTClient(Context context) {
        mContext = context;
        IntentFilter intentf = new IntentFilter();
        setClientID();
        intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(new MQTTBroadcastReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mConnMan = (ConnectivityManager) mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);

        InputStream keyfile = context.getResources().openRawResource(context.getResources().getIdentifier("rsa_private_pkcs8", "raw", context.getPackageName()));
        try {
            keyfile.read(keyBytes);
        } catch (Exception e) {
            Log.d(TAG, "Unable to open keyfile");
        }
    }


    public void sendUpdate(String topic, String content) {
        //int qos             = 2;
        //String broker       = "tcp://m11.cloudmqtt.com:15488";
        int qos             = 1;
        Log.d(TAG, "sendUpdate()");

        if (mqttClient == null || mqttClient.isConnected() == false) {
            doConnect();
        }

        try {
            mqttClient.publish("/devices/bb-candy/events/" + topic, content.getBytes(), qos, false);
        } catch (Exception e) {
            Log.d(TAG, "Failed to send:"  + e.toString());
        }
        /*
        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName("vokskcax");
            connOpts.setPassword("Lni2rRwd8dzw".toCharArray());
            connOpts.setCleanSession(true);
            Log.d(TAG, "Connecting to broker: "+broker);
            sampleClient.connect(connOpts);
            Log.d(TAG, "Connected");
            Log.d(TAG, "Publishing message: "+content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
            Log.d(TAG, "Message published");
            sampleClient.disconnect();
            Log.d(TAG, "Disconnected");
        } catch(MqttException me) {
            Log.d(TAG, "reason "+me.getReasonCode());
            Log.d(TAG, "msg "+me.getMessage());
            Log.d(TAG, "loc "+me.getLocalizedMessage());
            Log.d(TAG, "cause "+me.getCause());
            Log.d(TAG, "excep "+me);
            me.printStackTrace();
        }
        */


    }


    class MQTTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            IMqttToken token;
            boolean hasConnectivity = false;
            boolean hasChanged = false;
            NetworkInfo infos[] = mConnMan.getAllNetworkInfo();

            for (int i = 0; i < infos.length; i++){
                if (infos[i].getTypeName().equalsIgnoreCase("MOBILE")){
                    if((infos[i].isConnected() != hasMmobile)){
                        hasChanged = true;
                        hasMmobile = infos[i].isConnected();
                    }
                    Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
                } else if ( infos[i].getTypeName().equalsIgnoreCase("WIFI") ){
                    if((infos[i].isConnected() != hasWifi)){
                        hasChanged = true;
                        hasWifi = infos[i].isConnected();
                    }
                    Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
                }
            }

            hasConnectivity = hasMmobile || hasWifi;
            Log.v(TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - "+(mqttClient == null || !mqttClient.isConnected()));
            if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
                doConnect();
            } else if (!hasConnectivity && mqttClient != null && mqttClient.isConnected()) {
                Log.d(TAG, "doDisconnect()");
                try {
                    token = mqttClient.disconnect();
                    token.waitForCompletion(1000);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    };



    private void setClientID(){
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        deviceId = wInfo.getMacAddress();
        if(deviceId == null){
            deviceId = MqttAsyncClient.generateClientId();
        }
        deviceId = "projects/burner-board/locations/us-central1/registries/bb-registry/devices/bb-candy";
    }

    /** Create a Cloud IoT Core JWT for the given project id, signed with the given private key. */
    private String createJwtRsa(String projectId) throws Exception {
        DateTime now = new DateTime();
        // Create a JWT to authenticate this device. The device will be disconnected after the token
        // expires, and will have to reconnect with a new token. The audience field should always be set
        // to the GCP project id.
        JwtBuilder jwtBuilder =
                Jwts.builder()
                        .setIssuedAt(now.toDate())
                        .setExpiration(now.plusMinutes(20).toDate())
                        .setAudience(projectId);

        //byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyFile));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return jwtBuilder.signWith(SignatureAlgorithm.RS256, kf.generatePrivate(spec)).compact();
    }

    private void doConnect(){

        String jwtKey = null;
        Log.d(TAG, "doConnect()");
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);

        try {
            jwtKey = createJwtRsa("burner-board");
            Log.d(TAG, "Created key " + jwtKey.toString());
        } catch (Exception e) {
            Log.d(TAG, "Error creating key");
        }
        try {
            mqttClient = new MqttAsyncClient("ssl://mqtt.googleapis.com:8883", deviceId, new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName("vokskcax");
            connOpts.setPassword(jwtKey.toCharArray());
            token = mqttClient.connect(connOpts);
            token.waitForCompletion(15000);
            Log.d(TAG, "doConnect(): connected");
            mqttClient.setCallback(new MqttEventCallback());
            token = mqttClient.subscribe("bbcontrol", 0);
            token.waitForCompletion(15000);
        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                case MqttException.REASON_CODE_CONNECTION_LOST:
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    Log.v(TAG, "c" +e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    Intent i = new Intent("RAISEALLARM");
                    i.putExtra("ALLARM", e);
                    Log.e(TAG, "b"+  e.getMessage().toString());
                    break;
                default:
                    Log.e(TAG, "a" + e.getMessage().toString());
            }
        }
    }


    private class MqttEventCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable arg0) {
            Log.i(TAG, "Connection Lost");
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            Log.i(TAG, "Message delivered");
        }

        @Override
        @SuppressLint("NewApi")
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {
            Log.i(TAG, "Message arrived from topic " + topic + ": " + msg.getPayload().toString());
            Handler h = new Handler(mContext.getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    //Intent launchA = new Intent(MQTTService.this, FullscreenActivityTest.class);
                    //launchA.putExtra("message", msg.getPayload());

                }
            });
        }
    }


}
