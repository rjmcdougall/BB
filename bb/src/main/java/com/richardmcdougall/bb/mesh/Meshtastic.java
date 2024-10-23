package com.richardmcdougall.bb.mesh;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.google.protobuf.ByteString;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.hardware.Canable;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.geeksville.mesh.*;
import com.richardmcdougall.bbcommon.BoardState;

/*
[  482.550531] usb 5-1.2: new full-speed USB device number 3 using xhci-hcd
[  482.683615] usb 5-1.2: New USB device found, idVendor=239a, idProduct=8029
[  482.683674] usb 5-1.2: New USB device strings: Mfr=1, Product=2, SerialNumber=3
[  482.683700] usb 5-1.2: Product: WisCore RAK4631 Board
[  482.683732] usb 5-1.2: Manufacturer: RAKwireless
[  482.683806] usb 5-1.2: SerialNumber: 1D771791614FF502

[ 8288.108235] usb 5-1.2: USB disconnect, device number 17
[ 8294.488913] usb 5-1.2: new full-speed USB device number 18 using xhci-hcd
[ 8294.620755] usb 5-1.2: New USB device found, idVendor=1a86, idProduct=55d4
[ 8294.620818] usb 5-1.2: New USB device strings: Mfr=0, Product=2, SerialNumber=3
[ 8294.620848] usb 5-1.2: Product: USB Single Serial
[ 8294.620890] usb 5-1.2: SerialNumber: 576D026358
 */

public
class Meshtastic {
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;

    private NodeDB nodeDB;

    private MeshProtos.MyNodeInfo.Builder myNode = MeshProtos.MyNodeInfo.newBuilder();

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    UsbWrapper.UsbWrapperCallback mUsbCallback = new UsbWrapper.UsbWrapperCallback() {
        @Override
        public void onConnect() {
            initMeshtasticDevice();
        }

        @Override
        public void onNewData(byte[] data) {
            if (data.length > 0) {
                //BLog.d(TAG, "Received " + data.length);// + "bytes: " + new String(data));
                for (byte b : data) {
                    packetProcessor.processByte(b);
                }
            }
        }

        @Override
        public boolean checkDevice(int vid, int pid) {
            if ((pid == 0x55d4) && (vid == 0x1a86)) {
                BLog.d(TAG, "Found device");
                return true;
            }
            return false;
        }
    };

    // TODO: check service
    private UsbWrapper mUsb;

    PacketProcessor.PacketInterface packetCallback = new PacketProcessor.PacketInterface() {
        @Override
        public void onReceivePacket(byte[] bytes) {
            newPacket(bytes);
        }

        @Override
        public void onConnect() {

        }

        @Override
        public void flushBytes() {

        }

        @Override
        public void sendBytes(byte[] bytes) {
            mUsb.writeAsync(bytes);
        }
    };


    PacketProcessor packetProcessor = new PacketProcessor(packetCallback);

    MeshtasticThreadFactory meshtasticThreadFactory = new MeshtasticThreadFactory();
    private ScheduledThreadPoolExecutor sch = (java.util.concurrent.ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, meshtasticThreadFactory);
    Runnable meshSendLoop = () -> meshSendLoop();

    static class MeshtasticThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("meshtastic");
            return t;
        }
    }


    public Meshtastic(BBService service) {
        BLog.e(TAG, "creating service");
        try {
            this.service = service;
            mUsb = new UsbWrapper(service, "meshtastic", mUsbCallback);

        } catch (Exception e) {

        }
        try {
            this.service = service;
            sch.scheduleWithFixedDelay(meshSendLoop, 1, 300, TimeUnit.SECONDS);

        } catch (Exception e) {

        }
        nodeDB = new NodeDB(service);
        initMyNode();
    }

    private void initMyNode() {
        myNode.setMyNodeNum(nodeDB.getMyNodeNum());
    }


    private void initMeshtasticDevice() {
        try {
            Thread.sleep(1000);
            packetProcessor.connect();
            MeshProtos.ToRadio.Builder packet = MeshProtos.ToRadio.newBuilder();
            packet.setWantConfigId(123456);
            sendToRadio(packet);

            packet = MeshProtos.ToRadio.newBuilder();
            AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
            MeshProtos.User.Builder user = MeshProtos.User.newBuilder();
            user.setId("123456");
            user.setLongName("Burner Board Test");
            user.setShortName("BB01");
            user.build();
            admin.setSetOwner(user);
            admin.build();



            MeshProtos.MeshPacket.Builder builder = MeshProtos.MeshPacket.newBuilder();
            //builder.setTo(0);
            builder.setId(PacketIdGenerator.generatePacketId());
            builder.setWantAck(true);
            //builder.setPay

            //sendToRadio(meshpacket);

            admin = AdminProtos.AdminMessage.newBuilder();
            ConfigProtos.Config.Builder config = ConfigProtos.Config.newBuilder();
            ConfigProtos.Config.DeviceConfig.Builder device = ConfigProtos.Config.DeviceConfig.newBuilder();
            device.setRole(ConfigProtos.Config.DeviceConfig.Role.CLIENT);
            device.setRebroadcastMode(ConfigProtos.Config.DeviceConfig.RebroadcastMode.ALL);
            device.setNodeInfoBroadcastSecs(300);
            device.build();
            config.setDevice(device);
            config.build();
            admin.setSetConfig(config);
            admin.setGetConfigRequest(AdminProtos.AdminMessage.ConfigType.DEVICE_CONFIG);
            sendToRadio(admin);

            admin = AdminProtos.AdminMessage.newBuilder();
            config = ConfigProtos.Config.newBuilder();
            ConfigProtos.Config.LoRaConfig.Builder lora = ConfigProtos.Config.LoRaConfig.newBuilder();
            lora.setRegion(ConfigProtos.Config.LoRaConfig.RegionCode.US);
            lora.setModemPreset(ConfigProtos.Config.LoRaConfig.ModemPreset.LONG_FAST);
            lora.setUsePreset(true);
            lora.setTxEnabled(true);
            lora.setChannelNum(20);
            lora.build();
            config.setLora(lora);
            config.build();
            admin.setSetConfig(config);
            admin.setGetConfigRequest(AdminProtos.AdminMessage.ConfigType.LORA_CONFIG);
            sendToRadio(admin);

        } catch (Exception e) {
            BLog.d(TAG, "Error sending meshtastic setup" + e.getMessage());
        }
    }


    private void meshSendLoop() {
        try {
            sendPosition();
            TimeUnit.SECONDS.sleep(2);
            sendTelemetry();
        } catch (Exception e) {

        }
    }

    void sendPosition() {
        Position position = new Position(37.472604, -122.155005, 0);
        DataPacket data = new DataPacket(DataPacket.ID_BROADCAST, 0, position.toProto());
        data.hopLimit = 2;
        MeshProtos.MeshPacket meshpacket = data.toProto(nodeDB);
        MeshProtos.ToRadio.Builder packet = MeshProtos.ToRadio.newBuilder();
        packet.setPacket(meshpacket);
        packet.build();
        BLog.d(TAG, "sending data: \n" + data.toString());
        sendToRadio(packet);
    }

    void sendTelemetry() {
        int batteryLevel = 91;
        float voltage = 54f;
        float channelUtilization = 10;
        float airUtilTx = 1;
        int uptimeSeconds = 1000;

        DeviceMetrics metrics = new DeviceMetrics(batteryLevel, voltage,
                channelUtilization, airUtilTx, uptimeSeconds);
        TelemetryProtos.Telemetry.Builder telemetry = TelemetryProtos.Telemetry.newBuilder();
        telemetry.setTime((int) (System.currentTimeMillis() / 1000));
        telemetry.setDeviceMetrics(metrics.toProto());
        DataPacket data = new DataPacket(DataPacket.ID_BROADCAST, 0, telemetry.build());
        data.hopLimit = 2;
        MeshProtos.MeshPacket meshpacket = data.toProto(nodeDB);
        MeshProtos.ToRadio.Builder packet = MeshProtos.ToRadio.newBuilder();
        packet.setPacket(meshpacket);
        packet.build();
        BLog.d(TAG, "sending data: \n" + data.toString());
        sendToRadio(packet);
    }

    public void sendToRadio(MeshProtos.ToRadio.Builder p) {
        MeshProtos.ToRadio packet = p.build();
        BLog.d(TAG, "Sending packet to radio:\n" + packet.toString());
        packetProcessor.sendToRadio(packet.toByteArray());
    }

    public void sendToRadio(AdminProtos.AdminMessage.Builder a) {
        AdminProtos.AdminMessage admin = a.build();
        BLog.d(TAG, "Sending admin to radio:\n" + admin.toString());
        packetProcessor.sendToRadio(admin.toByteArray());
    }


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & (byte) 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void newPacket(byte[] bytes) {
        BLog.d(TAG, "new packet... length " + bytes.length);// + bytesToHex(bytes));
        MeshProtos.FromRadio fromRadio = null;
        try {
            fromRadio = MeshProtos.FromRadio.parseFrom(Arrays.copyOfRange(bytes, 0, bytes.length));
        } catch (Exception e) {
            BLog.d(TAG, "could not decode new packet proto: " + e.getMessage());
        }

        if (fromRadio != null) {
            //BLog.d(TAG, "packet: " + fromRadio);
        }

        try {
            switch (fromRadio.getPayloadVariantCase().getNumber()) {
                case MeshProtos.FromRadio.PACKET_FIELD_NUMBER:
                    handleReceivedMeshPacket(fromRadio.getPacket());
                    break;
                case MeshProtos.FromRadio.CONFIG_COMPLETE_ID_FIELD_NUMBER:
                    //handleConfigComplete(fromRadio.getConfigCompleteId());
                    break;
                case MeshProtos.FromRadio.MY_INFO_FIELD_NUMBER:
                    handleMyInfo(fromRadio.getMyInfo());
                    break;
                case MeshProtos.FromRadio.NODE_INFO_FIELD_NUMBER:
                    handleNodeInfo(fromRadio.getNodeInfo());
                    break;
                case MeshProtos.FromRadio.CHANNEL_FIELD_NUMBER:
                    //handleChannel(fromRadio.getChannel());
                    break;
                case MeshProtos.FromRadio.CONFIG_FIELD_NUMBER:
                    //handleDeviceConfig(fromRadio.getConfig());
                    break;
                case MeshProtos.FromRadio.MODULECONFIG_FIELD_NUMBER:
                    //handleModuleConfig(fromRadio.getModuleConfig());
                    break;
                case MeshProtos.FromRadio.QUEUESTATUS_FIELD_NUMBER:
                    //handleQueueStatus(fromRadio.getQueueStatus());
                    break;
                case MeshProtos.FromRadio.METADATA_FIELD_NUMBER:
                    //handleMetadata(fromRadio.getMetadata());
                    break;
                case MeshProtos.FromRadio.MQTTCLIENTPROXYMESSAGE_FIELD_NUMBER:
                    //handleMqttProxyMessage(fromRadio.getMqttClientProxyMessage());
                    break;
                case MeshProtos.FromRadio.CLIENTNOTIFICATION_FIELD_NUMBER:
                    //handleClientNotification(fromRadio.getClientNotification());
                    break;
                default:
                    BLog.d(TAG, "Unexpected FromRadio variant");
            }
        } catch (Exception ex) {
            BLog.d(TAG, "Invalid Protobuf from radio, len=" + bytes.length + " :" + ex.getMessage());
        }
    }

    void handleMyInfo(MeshProtos.MyNodeInfo info) {
        BLog.d(TAG, info.toString());
    }

    void handleNodeInfo(MeshProtos.NodeInfo node) {
        BLog.d(TAG, node.toString());
    }

    void handleReceivedMeshPacket(MeshProtos.MeshPacket packet) {
        BLog.d(TAG, packet.toString());
    }

    private void handleReceivedAdmin(int fromNodeNum, AdminProtos.AdminMessage a) {
    }
    /*
        if (fromNodeNum == myNodeNum) {
            switch (a.getPayloadVariantCase()) {
                case GET_CONFIG_RESPONSE:
                    AdminProtos.GetConfigResponse response = a.getGetConfigResponse();
                    debug("Admin: received config " + response.getPayloadVariantCase());
                    setLocalConfig(response);
                    break;

                case GET_CHANNEL_RESPONSE:
                    MyNodeInfo mi = myNodeInfo;
                    if (mi != null) {
                        AdminProtos.GetChannelResponse ch = a.getGetChannelResponse();
                        debug("Admin: Received channel " + ch.getIndex());

                        if (ch.getIndex() + 1 < mi.getMaxChannels()) {
                            handleChannel(ch);
                        }
                    }
                    break;

                default:
                    warn("No special processing needed for " + a.getPayloadVariantCase());
                    break;
            }
        } else {
            debug("Admin: Received session_passkey from " + fromNodeNum);
            sessionPasskey = a.getSessionPasskey();
        }
    }
*/
}


    /*

    private String byteArrayToString(Byte[] array) {
        byte[] bytes = new byte[array.length];
        int i = 0;
        for (byte b : array) {
            bytes[i++] = b;
        }

        return new String(bytes);
    }


    /*
       TelemetryProtos.DeviceMetricsOrBuilder metrics = TelemetryProtos.DeviceMetrics.newBuilder();
       metrics.
       MeshProtos.NodeInfo.Builder node = MeshProtos.NodeInfo.newBuilder();
       node.setChannel(0);
       node.setDeviceMetrics(metrics);
       DataPacket d = new DataPacket(DataPacket.ID_BROADCAST, );
       d.dataType =



       //
       // public DeviceMetrics(int time, int batteryLevel, float voltage,
       // float channelUtilization, float airUtilTx, int uptimeSeconds) {
       int nodeNum = 12356789;
       String id = "id";
       String longName = "longname";
       String shortName = "shortname";
       //NodeInfo node = new NodeInfo();
       /*

       MeshProtos.HardwareModel  hwModel = MeshProtos.HardwareModel.PRIVATE_HW;
       MeshUser user = new MeshUser(id, longName, shortName, hwModel);
       Position position = new Position(0,0,0);
       DeviceMetrics metrics = new DeviceMetrics(DeviceMetrics.currentTime(), 91, 54f, 0f, 0f, 100 );
       NodeInfo node = new NodeInfo(nodeNum, user, position, metrics);

       MeshProtos.NodeInfo.Builder ni = MeshProtos.NodeInfo.newBuilder();

       ni.setNum(node.num);
       ni.setUser(node.user.toProto());
       ni.setPosition(node.position.toProto());
       ni.snr = 1;
       ni.last_heard = ;
       ni.setDeviceMetrics(node.deviceMetrics.toProto());
       ni.channel = ;
       ni.via_mqtt = ;
       ni.hops_away = ;
       ni.is_favorite = ;
       ni.build();

        */




/*
    private MeshProtos.MeshPacket toMeshPacket(DataPacket p, boolean wantAck, int hopLimit, int channel, int priority) {
        MeshProtos.MeshPacket.Builder builder = newMeshPacketTo(toNodeNum(p.to));

        builder.setWantAck(wantAck);
        builder.setId();
        builder.setHopLimit(hopLimit);
        builder.setChannel(channel);
        builder.setPriority(priority);

        Data.Builder dataBuilder = MeshProtos.Data.newBuilder();
        initDataFunction.apply(dataBuilder); // Call the function to initialize Data.Builder
        builder.setDecoded(dataBuilder.build());


        return meshPacketBuilder.build(
                p.id,
                true,  // wantAck
                p.hopLimit,
                p.channel,
                MeshProtos.MeshPacket.Priority.UNSET, // Assuming default priority
                dataBuilder -> {
                    dataBuilder.setPayload(ByteString.copyFrom(p.bytes));
                    return null; // No need to return anything from the lambda
                }
        );
    }

}

 */

/*

    private int toNodeNum(String id) {
        if (id.equals(DataPacket.ID_BROADCAST)) {
            return DataPacket.NODENUM_BROADCAST;
        } else if (id.equals(DataPacket.ID_LOCAL)) {
            return myNode.getMyNodeNum();  // Assuming 'myNodeNum' is a field in your class
        } else {
            //return toNodeInfo(id).num;  // Assuming 'toNodeInfo(id)' returns a NodeInfo object
            return 123456789;
        }
    }

    private MeshProtos.MeshPacket.Builder newMeshPacketTo(int idNum) {
        //if (myNodeInfo == null) {
        //    throw new RadioNotConnectedException();
        //}

        MeshProtos.MeshPacket.Builder builder = MeshProtos.MeshPacket.newBuilder();
        builder.setFrom(0); // Don't add myNodeNum
        builder.setTo(idNum);
        return builder;
    }



 */