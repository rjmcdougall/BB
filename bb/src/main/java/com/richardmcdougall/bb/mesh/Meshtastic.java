package com.richardmcdougall.bb.mesh;

import com.google.protobuf.ByteString;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.BoardLocations;
import com.richardmcdougall.bbcommon.BLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.geeksville.mesh.*;

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

    ByteString sessionPasskey = null;

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
            sch.scheduleWithFixedDelay(meshSendLoop, 30, 300, TimeUnit.SECONDS);

        } catch (Exception e) {

        }
        nodeDB = new NodeDB(service);
        initMyNode();
    }

    public List<Node> getNodes() {
        List<Node> nodes = nodeDB.getNodes();
        return nodes;
    }

    private void initMyNode() {
        myNode.setMyNodeNum(nodeDB.getMyNodeNum());
    }


    private void initMeshtasticDevice() {
        try {
            Thread.sleep(1000);
            packetProcessor.connect();
            /*
            Thread.sleep(1000);
            Thread.sleep(10000);
            while (sessionPasskey == null) {
                resetDevice();
                Thread.sleep(10000);
                requestKey();
                Thread.sleep(60000);
            }
            Thread.sleep(5000);
            setDeviceDefaults();
            Thread.sleep(5000);
            setRadioDefaults();
            Thread.sleep(5000);
            */

            requestConfig();
            requestKey();


            /*
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



             */

            /*


             */

        } catch (Exception e) {
            BLog.d(TAG, "Error sending meshtastic setup" + e.getMessage());
        }
    }

    private void resetDevice() {
        // TODO: check passkey
        MeshProtos.ToRadio.Builder packet;
        AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
        admin.setRebootSeconds(1);
        DataPacket data = new DataPacket(DataPacket.ID_LOCAL, admin.build());
        packet = MeshProtos.ToRadio.newBuilder();
        packet.setPacket(data.toProto(nodeDB));
        packet.build();
        BLog.d(TAG, "sending data: \n" + packet.toString());
        sendToRadio(packet);
    }

    private void requestKey() {
        MeshProtos.ToRadio.Builder packet;
        AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
        admin.setGetConfigRequest(AdminProtos.AdminMessage.ConfigType.SESSIONKEY_CONFIG);
        admin.setSessionPasskey(AdminProtos.AdminMessage.newBuilder().getSessionPasskey());
        //BLog.d(TAG, admin.build().toString());
        DataPacket data = new DataPacket(DataPacket.ID_LOCAL, admin.build());
        packet = MeshProtos.ToRadio.newBuilder();
        packet.setPacket(data.toProto(nodeDB));
        packet.build();
        BLog.d(TAG, "sending data: \n" + packet.toString());
        sendToRadio(packet);
    }

    private void requestConfig() {
        MeshProtos.ToRadio.Builder packet;
        packet = MeshProtos.ToRadio.newBuilder();
        packet.setWantConfigId(123456);
        sendToRadio(packet);
    }


    private void setDeviceDefaults() {
        // TODO: check passkey
        MeshProtos.ToRadio.Builder packet;
        AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
        ConfigProtos.Config.DeviceConfig.Builder device = ConfigProtos.Config.DeviceConfig.newBuilder();
        device.setRole(ConfigProtos.Config.DeviceConfig.Role.CLIENT);
        device.setRebroadcastMode(ConfigProtos.Config.DeviceConfig.RebroadcastMode.ALL);
        device.setNodeInfoBroadcastSecs(300);
        device.build();
        ConfigProtos.Config.Builder config = ConfigProtos.Config.newBuilder();
        config.setDevice(device);
        admin.setSetConfig(config.build());
        admin.setSessionPasskey(sessionPasskey);
        DataPacket data = new DataPacket(DataPacket.ID_LOCAL, admin.build());
        packet = MeshProtos.ToRadio.newBuilder();
        packet.setPacket(data.toProto(nodeDB));
        packet.build();
        BLog.d(TAG, "sending data: \n" + packet.toString());
        sendToRadio(packet);
    }

    private void setRadioDefaults() {
        // TODO: check passkey
        MeshProtos.ToRadio.Builder packet;
        AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
        ConfigProtos.Config.LoRaConfig.Builder lora = ConfigProtos.Config.LoRaConfig.newBuilder();
        lora.setRegion(ConfigProtos.Config.LoRaConfig.RegionCode.US);
        lora.setModemPreset(ConfigProtos.Config.LoRaConfig.ModemPreset.LONG_FAST);
        lora.setUsePreset(true);
        lora.setTxEnabled(true);
        lora.setChannelNum(20);
        ConfigProtos.Config.Builder config = ConfigProtos.Config.newBuilder();
        config.setLora(lora.build());
        admin.setSetConfig(config.build());
        admin.setSessionPasskey(sessionPasskey);
        DataPacket data = new DataPacket(DataPacket.ID_LOCAL, admin.build());
        packet = MeshProtos.ToRadio.newBuilder();
        packet.setPacket(data.toProto(nodeDB));
        packet.build();
        BLog.d(TAG, "sending data: \n" + packet.toString());
        sendToRadio(packet);
    }

    private void meshSendLoop() {
        try {
            //sendPosition();
            //TimeUnit.SECONDS.sleep(1800);
            sendTelemetry();
        } catch (Exception e) {

        }
    }

    void sendPosition() {
        MeshProtos.Position position = MeshProtos.Position.newBuilder()
                .setLatitudeI((int) (1.0e7 * 37.472604))
                .setLongitudeI((int) (1.0e7 * -122.155005))
                .build();
        DataPacket data = new DataPacket(DataPacket.ID_BROADCAST, 0, position);
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

        TelemetryProtos.DeviceMetrics metrics = TelemetryProtos.DeviceMetrics.newBuilder()
                .setBatteryLevel(batteryLevel)
                .setVoltage(voltage)
                .setChannelUtilization(channelUtilization)
                .setUptimeSeconds(uptimeSeconds)
                .setAirUtilTx(airUtilTx)
                .build();
        TelemetryProtos.Telemetry.Builder telemetry = TelemetryProtos.Telemetry.newBuilder();
        telemetry.setTime((int) (System.currentTimeMillis() / 1000));
        telemetry.setDeviceMetrics(metrics);
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
                    acknowledgePacket(fromRadio.getPacket());
                    handleReceivedMeshPacket(fromRadio.getPacket());
                    break;
                case MeshProtos.FromRadio.CONFIG_COMPLETE_ID_FIELD_NUMBER:
                    //handleConfigComplete(fromRadio.getConfigCompleteId());
                    break;
                case MeshProtos.FromRadio.MY_INFO_FIELD_NUMBER:
                    handleMyInfo(fromRadio.getMyInfo());
                    break;
                case MeshProtos.FromRadio.NODE_INFO_FIELD_NUMBER:
                    MeshProtos.NodeInfo node = fromRadio.getNodeInfo();
                    BLog.d(TAG, "Node from radio: " + node.getNum());
                    handleNode(node);
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
                case MeshProtos.FromRadio.FILEINFO_FIELD_NUMBER:
                    //handleClientNotification(fromRadio.getFileInfo());
                    break;
                default:
                    BLog.d(TAG, "Unexpected FromRadio variant: " + fromRadio.toString());
            }
        } catch (Exception ex) {
            BLog.d(TAG, "Invalid Protobuf from radio, len=" + bytes.length + " :" + ex.getMessage());
        }
    }

    void handleMyInfo(MeshProtos.MyNodeInfo info) {
        BLog.d(TAG, info.toString());
    }

    void handleNodeInfo(MeshProtos.NodeInfo node) {
        BLog.d(TAG, "nodeinfo packet: " + node.toString());
        nodeDB.addNode(node);
    }

    void handleReceivedMeshPacket(MeshProtos.MeshPacket packet) {

        BLog.d(TAG, "Meshpacket...");
        DataPacket data = null;

        if (packet.hasDecoded()) {
            data = toDataPacket(packet);
        }

        if (data != null) {
            if (data.pkiEncrypted) {
                BLog.d(TAG, "packet is encrypted");
            }
            switch (data.dataType) {
                case Portnums.PortNum.TEXT_MESSAGE_APP_VALUE:
                    BLog.d(TAG, "Meshpacket Text Message");
                    String message = new String(data.bytes);
                    handleText(packet, message);
                    break;
                case Portnums.PortNum.ADMIN_APP_VALUE:
                    BLog.d(TAG, "Meshpacket Admin Message");
                    try {
                        AdminProtos.AdminMessage admin = AdminProtos.AdminMessage.parseFrom(data.bytes);
                        BLog.d(TAG, "Admin Message: " + admin.toString());
                        handleReceivedAdmin(packet.getFrom(), admin);
                    } catch (Exception e) {
                        BLog.e(TAG, "Admin Message parse failed: " + e.getMessage());
                    }
                    break;
                case Portnums.PortNum.NODEINFO_APP_VALUE:
                    BLog.d(TAG, "Meshpacket User");
                    try {
                        //MeshProtos.NodeInfo node = MeshProtos.NodeInfo.parseFrom(data.bytes);
                        MeshProtos.User user = MeshProtos.User.parseFrom(data.bytes);
                        handleUser(packet, user);
                    } catch (Exception e) {
                        BLog.e(TAG, "Node info parse failed: " + e.getMessage());
                    }
                    break;
                case Portnums.PortNum.POSITION_APP_VALUE:
                    BLog.d(TAG, "Meshpacket Position");
                    try {
                        MeshProtos.Position position = MeshProtos.Position.parseFrom(data.bytes);
                        handlePosition(packet, position);
                    } catch (Exception e) {
                        BLog.e(TAG, "Position parse failed: " + e.getMessage());
                    }
                    break;
                case Portnums.PortNum.ROUTING_APP_VALUE:
                    BLog.d(TAG, "Routing");
                    break;
                case Portnums.PortNum.TELEMETRY_APP_VALUE:
                    BLog.d(TAG, "Meshpacket Telemetry");
                    try {
                        TelemetryProtos.Telemetry telemetry = TelemetryProtos.Telemetry.parseFrom(data.bytes);
                        handleTelemetry(packet, telemetry);
                    } catch (Exception e) {
                        BLog.e(TAG, "Telemetry parse failed: " + e.getMessage());
                    }
                    break;
                default:
                    BLog.d(TAG, "other datatype " + data.dataType);
                    BLog.d(TAG, packet.toString());

                    break;
            }
        } else {
            BLog.d(TAG, "empty packet"); //: " + packet.toString());
        }
    }

    // 05-26 04:34:41.676 13231 13341 D BB.Meshtastic: packet: node_info {
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:   num: 1227682206
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:   user {
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     id: "!492cf19e"
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     long_name: "Meshtastic f19e"
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     short_name: "f19e"
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     macaddr: "\342>I,\361\236"
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     hw_model: TRACKER_T1000_E
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     role: CLIENT_MUTE
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     public_key: "\316\234\3347/\270*\313\337\350\220\230\036L\360#\027C3\324\250GUTZ\220\344G\3331\fx"
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:   }
    //05-26 06:34:06.103 26946 27059 D BB.Meshtastic:   position {
    //05-26 06:34:06.103 26946 27059 D BB.Meshtastic:     latitude_i: 376700928
    //05-26 06:34:06.103 26946 27059 D BB.Meshtastic:     longitude_i: -1210318848
    //05-26 06:34:06.103 26946 27059 D BB.Meshtastic:     altitude: 40
    //05-26 06:34:06.103 26946 27059 D BB.Meshtastic:     location_source: LOC_MANUAL
    //05-26 06:34:06.103 26946 27059 D BB.Meshtastic:   }
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:   snr: 8.75
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:   last_heard: 1748227721
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:   device_metrics {
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     battery_level: 101
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     voltage: 4.192
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     channel_utilization: 3.7233334
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     air_util_tx: 0.014611111
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:     uptime_seconds: 6840085
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:   }
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic:   hops_away: 3
    //05-26 04:34:41.676 13231 13341 D BB.Meshtastic: }
    private void handleNode(MeshProtos.NodeInfo node) {
        BLog.d(TAG, "New Node " + node);
        nodeDB.addNode(node);
    }

    //05-26 04:38:44.355 13231 13341 D BB.Meshtastic: Node user message from mesh: id: "!99a34093"
    //05-26 04:38:44.355 13231 13341 D BB.Meshtastic: long_name: "Router-01"
    //05-26 04:38:44.355 13231 13341 D BB.Meshtastic: short_name: "rtr1"
    //05-26 04:38:44.355 13231 13341 D BB.Meshtastic: macaddr: "\356\r\231\243@\223"
    //05-26 04:38:44.355 13231 13341 D BB.Meshtastic: hw_model: RAK4631
    //05-26 04:38:47.977 13231 13341 D BB.Meshtastic: new packet... length 85
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic: packet: packet {
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:   from: 1128212352
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:   to: 2109068568
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:   decoded {
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:     portnum: NODEINFO_APP
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:     payload: "\n\t!433f2780\022\bMTB Base\032\004MTBB\"\006H\312C?\'\200(+"
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:     want_response: true
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:   }
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:   id: 4081294526
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:   rx_time: 1748234329
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:   rx_snr: 8.0
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:   rx_rssi: -85
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic:   hop_start: 3
    //05-26 04:38:47.980 13231 13341 D BB.Meshtastic: }
    private void handleUser(MeshProtos.MeshPacket packet, MeshProtos.User user) {
        //BLog.d(TAG, "Node " + packet.getFrom() + " user message from mesh: " + user.toString());
        try {
            MeshProtos.NodeInfo node = nodeDB.findNode(packet.getFrom());
            if (node == null) {
                node = MeshProtos.NodeInfo.getDefaultInstance();
            }
            MeshProtos.NodeInfo newnode = node.toBuilder()
                    .setUser(user)
                    .build();
            nodeDB.addNode(newnode);
            BLog.d(TAG, "Updated node: " + newnode.getUser().getLongName());
        } catch (Exception e) {
            BLog.d(TAG, "cannot update node from user message" + e.getMessage());
        }
    }

    private void handlePosition(MeshProtos.MeshPacket packet, MeshProtos.Position position) {
        //BLog.d(TAG, "Position " + packet.getFrom() + " user message from mesh: " + position.toString());
        MeshProtos.NodeInfo newnode = null;
        try {
            MeshProtos.NodeInfo node = nodeDB.findNode(packet.getFrom());
            if (node == null) {
                node = MeshProtos.NodeInfo.getDefaultInstance();
            }
            newnode = node.toBuilder()
                    .setPosition(position)
                    .build();
            nodeDB.addNode(newnode);
            BLog.d(TAG, "Position, node: " + newnode.getUser().getLongName() + ", " +
                    position.getLatitudeI() / 10000000.0f + "," +
                    position.getLongitudeI() / 10000000.0f);
        } catch (Exception e) {
            BLog.d(TAG, "cannot update node from position message" + e.getMessage());
        }
        try {
            if (newnode != null && newnode.getUser().getLongName().length() > 0) {
                this.service.boardLocations.updateBoardLocations(newnode.getUser().getLongName(),
                        (int)packet.getRxSnr(),
                        newnode.getPosition().getLatitudeI() / 10000000.0,
                        newnode.getPosition().getLongitudeI() / 10000000.0,
                        (int)newnode.getDeviceMetrics().getBatteryLevel(),
                        null,
                        false);

            }
        } catch (Exception e) {
        }
    }

    private void handleTelemetry(MeshProtos.MeshPacket packet, TelemetryProtos.Telemetry telemetry) {
        //BLog.d(TAG, "Telemetry " + packet.getFrom() + " Telemetry message from mesh: " + telemetry.toString());
        try {
            MeshProtos.NodeInfo node = nodeDB.findNode(packet.getFrom());
            if (node == null) {
                node = MeshProtos.NodeInfo.getDefaultInstance();
            }
            MeshProtos.NodeInfo newnode = node.toBuilder()
                    .setDeviceMetrics(telemetry.getDeviceMetrics())
                    .build();
            nodeDB.addNode(newnode);
            BLog.d(TAG, "Telemetry, node: " + newnode.getUser().getLongName() +
                    ", battery: " + newnode.getDeviceMetrics().getBatteryLevel() +
                    ", voltage: " + newnode.getDeviceMetrics().getVoltage());
        } catch (Exception e) {
            BLog.d(TAG, "cannot update node from telemetry message" + e.getMessage());
        }
    }

    public void handleText(MeshProtos.MeshPacket packet, String message) {
        try {
            String username;

            MeshProtos.NodeInfo node = nodeDB.findNode(packet.getFrom());
            if (node == null) {
                username = "unknown";
            } else {
                username = node.getUser().getLongName();
            }
            BLog.d(TAG, username + ": " + message);
        } catch (Exception e) {
            BLog.d(TAG, "can't get text message" + e.getMessage());
        }
    }
    int myNodeNum = 0;

    private void handleReceivedAdmin(int fromNodeNum, AdminProtos.AdminMessage a) {

        if (fromNodeNum == myNodeNum) {
            switch (a.getPayloadVariantCase()) {


                case GET_CONFIG_RESPONSE:

                    ConfigProtos.Config response = a.getGetConfigResponse();
                    BLog.d(TAG, "Admin: received config " + response.getPayloadVariantCase());
                    //setLocalConfig(response);
                    break;

                case GET_CHANNEL_RESPONSE:
                    ChannelProtos.Channel ch = a.getGetChannelResponse();
                    BLog.d(TAG, "Admin: Received channel " + ch.getIndex());
                    /*
                    MyNodeInfo mi = myNodeInfo;
                    if (mi != null) {


                        if (ch.getIndex() + 1 < mi.getMaxChannels()) {
                            handleChannel(ch);
                        }
                    }

                     */
                    break;

                default:
                    BLog.d(TAG, "No special processing needed for " + a.getPayloadVariantCase());
                    break;
            }
        } else {
            BLog.d(TAG, "Admin: Received session_passkey from " + fromNodeNum);
            sessionPasskey = a.getSessionPasskey();
        }
    }


    private DataPacket toDataPacket(MeshProtos.MeshPacket packet) {
        if (!packet.hasDecoded()) {
            // We never convert packets that are not DataPackets
            return null;
        } else {
            MeshProtos.Data data = packet.getDecoded();

            return new DataPacket(
                    NodeDB.toNodeID(packet.getFrom()),
                    NodeDB.toNodeID(packet.getTo()),
                    packet.getRxTime() * 1000L,
                    packet.getId(),
                    data.getPortnumValue(),
                    data.getPayload().toByteArray(),
                    packet.getHopLimit()
                    //packet.getPkiEncrypted() ? DataPacket.PKC_CHANNEL_INDEX : packet.getChannel()
            );
        }
    }

    public void acknowledgePacket(MeshProtos.MeshPacket receivedPacket) {
        if (receivedPacket.getWantAck()) {  // Only ack if requested


            MeshProtos.Routing routing = MeshProtos.Routing.newBuilder()
                    .setErrorReason(MeshProtos.Routing.Error.NONE)  // Indicate success

                    .build();
            MeshProtos.MeshPacket ackPacket = MeshProtos.MeshPacket.newBuilder()
                    .setFrom(530602760) // Your node ID
                    // TODO: fix my nodenum
                    .setTo(receivedPacket.getFrom())   // Send back to the sender
                    .setChannel(receivedPacket.getChannel()) // Same channel as received packet
                    .setDecoded(MeshProtos.Data.newBuilder()
                            .setPortnum(Portnums.PortNum.ROUTING_APP) // Indicate a routing message
                            .setPayload(routing.toByteString())
                            .setRequestId(receivedPacket.getId()) // Link to original packet ID
                            .build())
                    .setWantAck(false) // ACKs themselves don't need ACKs
                    .setPriority(MeshProtos.MeshPacket.Priority.ACK)  // High priority
                    .build();

            // 3. Send the ACK packet
            MeshProtos.ToRadio.Builder packet = MeshProtos.ToRadio.newBuilder();
            packet.setPacket(ackPacket);
            packet.build();
            BLog.d(TAG, "sending ack");
            sendToRadio(packet);
        }
    }
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