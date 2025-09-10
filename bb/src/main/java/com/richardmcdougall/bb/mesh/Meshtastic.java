package com.richardmcdougall.bb.mesh;

import android.os.SystemClock;
import android.os.health.HealthStats;

import com.google.protobuf.ByteString;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bb.BoardLocations;
import com.richardmcdougall.bbcommon.BLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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


   1.949558] usb 5-1.2: new high-speed USB device number 3 using xhci-hcd
[    2.139977] usb 5-1.2: New USB device found, idVendor=16c0, idProduct=0483, bcdDevice= 2.80
[    2.139996] usb 5-1.2: New USB device strings: Mfr=1, Product=2, SerialNumber=3
[    2.140001] usb 5-1.2: Product: USB Serial
[    2.140006] usb 5-1.2: Manufacturer: Teensyduino
[    2.140011] usb 5-1.2: SerialNumber: 11964340
[    2.372808] usb 5-1.3: new full-speed USB device number 4 using xhci-hcd
[    2.584125] usb 5-1.3: New USB device found, idVendor=303a, idProduct=1001, bcdDevice= 1.00
[    2.584143] usb 5-1.3: New USB device strings: Mfr=1, Product=2, SerialNumber=3
[    2.584148] usb 5-1.3: Product: Heltec Wireless Tracker
[    2.584153] usb 5-1.3: Manufacturer: Espressif Systems

 */

public
class Meshtastic {
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;

    ByteString sessionPasskey = null;

    private NodeDB nodeDB;

    // Messages buffer for text messages received from mesh
    private Messages messages;

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
            if ((pid == 0x1001) && (vid == 0x303a)) {
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
        messages = new Messages();
        initMyNode();
    }

    // Predefined whitelist of node names (exact match on Node.name)
    private final Set<String> whitelistedNodeNames = new HashSet<>(Arrays.asList(
            "Burnulance",
            "Proto 1",
            "Proto 2",
            "Proto 3",
            "Proto 4",
            "Blinky Things Hospital",
            "Blinky Things",
            "Three Emus"
            // Add more whitelisted node names as needed
    ));

    // include any nodes with the meshtastic shortname in the form BBnn
    // Matches shortnames like BB01, BB99, etc.
    private final Pattern shortnamePattern = Pattern.compile("^BB[0-9a-fA-F][0-9a-fA-F˘]$");

    public List<Node> getNodes() {
        List<Node> nodes = nodeDB.getNodes();

        List<Node> filteredNodes = new ArrayList<>();
        for (Node node : nodes) {
            boolean isWhitelisted = whitelistedNodeNames.contains(node.name);
            boolean matchesRegex = node.shortname != null && shortnamePattern.matcher(node.shortname).matches();

            if (isWhitelisted || matchesRegex) {
                filteredNodes.add(node);
                BLog.d(TAG, "Including node: " + node.name + " (" + node.shortname + ") - " +
                        (isWhitelisted ? "whitelisted" : "regex match"));
            } else {
                BLog.d(TAG, "Filtering out node: " + node.name + " (" + node.shortname + ")");
            }
        }

        return filteredNodes;
    }

    private void initMyNode() {
        myNode.setMyNodeNum(nodeDB.getMyNodeNum());
    }

    private boolean sentDeviceConfig = false;
    private boolean sentLoraConfig = false;
    private boolean sentChannelConfig = false;
    private boolean sentUserConfig = false;

    private boolean sentReboot = false;


    private void initMeshtasticDevice() {
        try {
            Thread.sleep(1000);
            packetProcessor.connect();

            requestConfig();
            Thread.sleep(5000);
            requestKey();
            Thread.sleep(10000);

            if (sentUserConfig == false) {
                setUserDefaults();
                Thread.sleep(2000);
            }

            if(sentLoraConfig == false) {
                setLoraDefaults();
                Thread.sleep(2000);
            }

            if (sentChannelConfig == false) {
                setChannelDefaults();
                Thread.sleep(2000);
            }

            if (sentReboot == false) {
                resetDevice();
            }

        } catch (Exception e) {
            BLog.d(TAG, "Error sending meshtastic setup" + e.getMessage());
        }
    }

    private void resetDevice() {
        BLog.d(TAG, "resetDevice");
        // TODO: check passkey
        MeshProtos.ToRadio.Builder packet;
        AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
        admin.setRebootSeconds(1);
        DataPacket data = new DataPacket(radioNodeNum, admin.build());
        packet = MeshProtos.ToRadio.newBuilder();
        packet.setPacket(data.toProto(nodeDB));
        packet.build();
        BLog.d(TAG, "sending reset device: \n" + packet.toString());
        sendToRadio(packet);
        sentReboot = true;
    }

    private void requestKey() {
        BLog.d(TAG, "requestKey");
        MeshProtos.ToRadio.Builder packet;
        AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
        admin.setGetConfigRequest(AdminProtos.AdminMessage.ConfigType.SESSIONKEY_CONFIG);
        //admin.setSessionPasskey(AdminProtos.AdminMessage.newBuilder().getSessionPasskey());
        //BLog.d(TAG, admin.build().toString());
        DataPacket data = new DataPacket(radioNodeNum, admin.build());
        packet = MeshProtos.ToRadio.newBuilder();
        packet.setPacket(data.toProto(nodeDB));
        packet.build();
        BLog.d(TAG, "sending request key: \n" + packet.toString());
        sendToRadio(packet);
    }

    private void requestConfig() {
        BLog.d(TAG, "requestConfig");
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
        //admin.setSessionPasskey(sessionPasskey);
        DataPacket data = new DataPacket(radioNodeNum, admin.build());
        packet = MeshProtos.ToRadio.newBuilder();
        packet.setPacket(data.toProto(nodeDB));
        packet.build();
        BLog.d(TAG, "sending data: \n" + packet.toString());
        sendToRadio(packet);
        sentDeviceConfig = true;
    }

    private void setUserDefaults() {
        BLog.d(TAG, "setUserDefaults");
        try {
            // Get board name from service.boardState.BOARD_ID
            String boardName = service.boardState.BOARD_ID;
            
            // Get serial number and take the last two digits mod 100
            String serialNumber = android.os.Build.SERIAL;
            int serialNumeric = 0;
            try {
                // Try to extract numeric part from serial number
                String numericPart = serialNumber.replaceAll("[^0-9]", "");
                if (!numericPart.isEmpty()) {
                    serialNumeric = Integer.parseInt(numericPart.substring(Math.max(0, numericPart.length() - 2)));
                } else {
                    // If no numeric part, use hashCode mod 100
                    serialNumeric = Math.abs(serialNumber.hashCode()) % 100;
                }
            } catch (Exception e) {
                // Fallback to hashCode mod 100 if parsing fails
                serialNumeric = Math.abs(serialNumber.hashCode()) % 100;
            }
            String shortName = String.format("BB%02d", serialNumeric % 100);
            
            BLog.d(TAG, "Setting user: longName=" + boardName + ", shortName=" + shortName);
            
            // Create User object
            MeshProtos.ToRadio.Builder packet;
            AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
            MeshProtos.User.Builder user = MeshProtos.User.newBuilder();
            user.setLongName(boardName);
            user.setShortName(shortName);
            // Set a default ID based on the board name if needed
            user.setId("!" + boardName.toLowerCase().replaceAll("[^a-z0-9]", "").substring(0, Math.min(8, boardName.length())));
            
            admin.setSetOwner(user.build());
            admin.setSessionPasskey(sessionPasskey);
            DataPacket data = new DataPacket(radioNodeNum, admin.build());
            packet = MeshProtos.ToRadio.newBuilder();
            packet.setPacket(data.toProto(nodeDB));
            packet.build();
            BLog.d(TAG, "sending user config to radio: \n" + packet.toString());
            sendToRadio(packet);
            sentUserConfig = true;
        } catch (Exception e) {
            BLog.d(TAG, "setup exception: " + e.getMessage());
        }
    }
    private void setLoraDefaults() {
        BLog.d(TAG, "setRadioDefaults");
        // TODO: check passkey
        try {
            MeshProtos.ToRadio.Builder packet;
            AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
            ConfigProtos.Config.LoRaConfig.Builder lora = ConfigProtos.Config.LoRaConfig.newBuilder();
            lora.setRegion(ConfigProtos.Config.LoRaConfig.RegionCode.US);
            lora.setModemPreset(ConfigProtos.Config.LoRaConfig.ModemPreset.MEDIUM_SLOW);
            lora.setChannelNum(52);
            //lora.setModemPreset(ConfigProtos.Config.LoRaConfig.ModemPreset.SHORT_TURBO);
            //lora.setChannelNum(31);
            lora.setUsePreset(true);
            lora.setTxEnabled(true);
            lora.setHopLimit(7);
            ConfigProtos.Config.DeviceConfig.Builder deviceConfig = ConfigProtos.Config.DeviceConfig.newBuilder();
            //deviceConfig.setNodeInfoBroadcastSecs(3601);
            ConfigProtos.Config.Builder config = ConfigProtos.Config.newBuilder();
            config.setLora(lora.build());
            //config.setDevice(deviceConfig);
            admin.setSetConfig(config.build());
            admin.setSessionPasskey(sessionPasskey);
            DataPacket data = new DataPacket(radioNodeNum, admin.build());
            packet = MeshProtos.ToRadio.newBuilder();
            packet.setPacket(data.toProto(nodeDB));
            packet.build();
            BLog.d(TAG, "sending config to radio: \n" + packet.toString());
            sendToRadio(packet);
            sentLoraConfig = true;
        } catch (Exception e) {
            BLog.d(TAG, "setup exception: " + e.getMessage());
        }
    }

    private void setChannelDefaults() {
        BLog.d(TAG, "setChannelDefaults");
        try {
            // Configure Channel 0 (default channel) with passkey "AQ==" and position enabled
            MeshProtos.ToRadio.Builder packet;
            AdminProtos.AdminMessage.Builder admin = AdminProtos.AdminMessage.newBuilder();
            ChannelProtos.Channel.Builder channel0 = ChannelProtos.Channel.newBuilder();
            channel0.setIndex(0);
            channel0.setRole(ChannelProtos.Channel.Role.PRIMARY);
            ChannelProtos.ChannelSettings.Builder settings0 = ChannelProtos.ChannelSettings.newBuilder();
            settings0.setName("");
            settings0.setUplinkEnabled(true);
            settings0.setPsk(ByteString.copyFrom(java.util.Base64.getDecoder().decode("AQ==")));
            
            // Enable position sharing with high precision for Channel 0
            ChannelProtos.ModuleSettings.Builder moduleSettings0 = ChannelProtos.ModuleSettings.newBuilder();
            moduleSettings0.setPositionPrecision(32); // High precision (32 bits)
            settings0.setModuleSettings(moduleSettings0.build());
            
            channel0.setSettings(settings0.build());
            admin.setSetChannel(channel0.build());
            admin.setSessionPasskey(sessionPasskey);
            DataPacket data = new DataPacket(radioNodeNum, admin.build());
            packet = MeshProtos.ToRadio.newBuilder();
            packet.setPacket(data.toProto(nodeDB));
            packet.build();
            BLog.d(TAG, "sending channel 0 to radio: \n" + packet.toString());
            sendToRadio(packet);
            Thread.sleep(1000);

            // Configure Channel 1 named "BlinkThing" with passkey and position enabled
            admin = AdminProtos.AdminMessage.newBuilder();
            ChannelProtos.Channel.Builder channel1 = ChannelProtos.Channel.newBuilder();
            channel1.setIndex(1);
            channel1.setRole(ChannelProtos.Channel.Role.SECONDARY);
            ChannelProtos.ChannelSettings.Builder settings1 = ChannelProtos.ChannelSettings.newBuilder();
            settings1.setName("BlinkyThing");
            settings1.setUplinkEnabled(true);
            settings1.setPsk(ByteString.copyFrom(java.util.Base64.getDecoder().decode("MgkxoOxSr8pwXSkjvXrjt8pH8eStGHEIwKACN3TavNQ=")));
            
            // Enable position sharing with high precision for Channel 1
            ChannelProtos.ModuleSettings.Builder moduleSettings1 = ChannelProtos.ModuleSettings.newBuilder();
            moduleSettings1.setPositionPrecision(32); // High precision (32 bits)
            settings1.setModuleSettings(moduleSettings1.build());
            
            channel1.setSettings(settings1.build());
            admin.setSetChannel(channel1.build());
            admin.setSessionPasskey(sessionPasskey);
            data = new DataPacket(radioNodeNum, admin.build());
            packet = MeshProtos.ToRadio.newBuilder();
            packet.setPacket(data.toProto(nodeDB));
            packet.build();
            BLog.d(TAG, "sending channel 1 to radio: \n" + packet.toString());
            sendToRadio(packet);
            sentChannelConfig = true;
        } catch (Exception e) {
            BLog.d(TAG, "setup exception: " + e.getMessage());
        }
    }

    private void meshSendLoop() {
        try {
            //sendPosition();
            //TimeUnit.SECONDS.sleep(1800);
            // Send on both default and private channel
            sendTelemetry(0);
            sendTelemetry(1);
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

    void sendTelemetry(int channel) {
        float batteryLevel = 0;
        float voltage = 0;
        float ledCurrent = 0;
        float motorCurrent = 0;
        float channelUtilization = 10;
        float airUtilTx = 1;
        int uptimeSeconds = service.boardState.inCrisis ? 999999999 : (int) (SystemClock.uptimeMillis() / 1000);
        try {
            voltage = service.bms.getVoltage();
            ledCurrent = service.bms.getCurrentInstant();
            batteryLevel = service.bms.getLevel() / 100.0f;
        } catch (IOException e) {
        }
        try {
            motorCurrent = service.vesc.getMotorCurrent();
        } catch (Exception e) {
        }

        TelemetryProtos.PowerMetrics powerMetrics = TelemetryProtos.PowerMetrics.newBuilder()
                .setCh1Voltage(voltage)
                .setCh1Current(motorCurrent)
                .setCh2Voltage(batteryLevel)
                .setCh2Current(ledCurrent)
                .build();

        BLog.d(TAG, "Telemetry voltage: " + voltage + ", motorcurrent " + motorCurrent + ", batterylevel " + batteryLevel + ", ledcurrent " + ledCurrent);

/*
        TelemetryProtos.DeviceMetrics metrics = TelemetryProtos.DeviceMetrics.newBuilder()
                .setBatteryLevel(batteryLevel)
                .setVoltage(voltage)
                .setChannelUtilization(channelUtilization)
                .setUptimeSeconds(uptimeSeconds)
                .setAirUtilTx(airUtilTx)
                .build();

 */
        TelemetryProtos.Telemetry.Builder telemetry = TelemetryProtos.Telemetry.newBuilder();
        telemetry.setTime((int) (System.currentTimeMillis() / 1000));
        //telemetry.setDeviceMetrics(metrics);
        telemetry.setPowerMetrics(powerMetrics);
        DataPacket data = new DataPacket(DataPacket.ID_BROADCAST, channel, telemetry.build());
        data.hopLimit = 7;
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
                    BLog.d(TAG, "ModuleConfig FromRadio variant: " + fromRadio.toString());
                    //handleModuleConfig(fromRadio.getModuleConfig());
                    break;
                case MeshProtos.FromRadio.QUEUESTATUS_FIELD_NUMBER:
                    BLog.d(TAG, "QueueStatus FromRadio variant: " + fromRadio.toString());
                    //handleQueueStatus(fromRadio.getQueueStatus());
                    break;
                case MeshProtos.FromRadio.METADATA_FIELD_NUMBER:
                    BLog.d(TAG, "MetaData FromRadio variant: " + fromRadio.toString());
                    //handleMetadata(fromRadio.getMetadata());
                    break;
                case MeshProtos.FromRadio.MQTTCLIENTPROXYMESSAGE_FIELD_NUMBER:
                    BLog.d(TAG, "MQTT FromRadio variant: " + fromRadio.toString());
                    //handleMqttProxyMessage(fromRadio.getMqttClientProxyMessage());
                    break;
                case MeshProtos.FromRadio.CLIENTNOTIFICATION_FIELD_NUMBER:
                    BLog.d(TAG, "ClientNotification FromRadio variant: " + fromRadio.toString());
                    //handleClientNotification(fromRadio.getClientNotification());
                    break;
                case MeshProtos.FromRadio.FILEINFO_FIELD_NUMBER:
                    BLog.d(TAG, "FileInfo FromRadio variant: " + fromRadio.toString());
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
        radioNodeNum = info.getMyNodeNum();
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
        //BLog.d(TAG, "New Node " + node);
        nodeDB.addNode(node);
        pushLocation(node);
    }

    private void pushLocation(MeshProtos.NodeInfo node) {
        try {
            if ((node != null) && (node.getUser().getShortName().length() > 0) &&
                    (node.getPosition().getLatitudeI() > 0)) {

                // Apply the same filtering logic as getNodes()
                String nodeName = node.getUser().getLongName();
                String nodeShortName = node.getUser().getShortName();

                boolean isWhitelisted = whitelistedNodeNames.contains(nodeName);
                boolean matchesRegex = nodeShortName != null && shortnamePattern.matcher(nodeShortName).matches();

                int boardAddress = 0;
                if (isWhitelisted || matchesRegex) {
                    try {
                        boardAddress = service.allBoards.getBoardAddress(nodeName);
                    } catch (Exception e) {}
                    this.service.boardLocations.updateBoardLocations(nodeName,
                            boardAddress,
                            999,
                            node.getPosition().getLatitudeI() / 10000000.0,
                            node.getPosition().getLongitudeI() / 10000000.0,
                            (int) node.getDeviceMetrics().getBatteryLevel(),
                            null,
                            false);

                    BLog.d(TAG, "Pushed location for node: " + nodeName + " (" + nodeShortName + ") - " +
                            (isWhitelisted ? "whitelisted" : "regex match"));
                } else {
                    BLog.d(TAG, "Filtered out location for node: " + nodeName + " (" + nodeShortName + ")");
                }
            }
        } catch (Exception e) {
            BLog.w(TAG, "Error in pushLocation: " + e.getMessage());
        }
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
        pushLocation(newnode);
    }

    private void handleTelemetry(MeshProtos.MeshPacket packet, TelemetryProtos.Telemetry telemetry) {
        //BLog.d(TAG, "Telemetry " + packet.getFrom() + " Telemetry message from mesh: " + telemetry.toString());
        try {
            MeshProtos.NodeInfo node = nodeDB.findNode(packet.getFrom());
            if (node == null) {
                node = MeshProtos.NodeInfo.getDefaultInstance();
            }
            // Convert tunneled board voltage and battery level into node
            TelemetryProtos.PowerMetrics powerMetrics = telemetry.getPowerMetrics();
            int batteryLevel = telemetry.getDeviceMetrics().getBatteryLevel();
            float voltage = telemetry.getDeviceMetrics().getVoltage();
            float boardVoltage = powerMetrics.getCh1Voltage();
            int boardBatteryLevel = (int)(powerMetrics.getCh2Voltage() * 100.0);
            TelemetryProtos.DeviceMetrics deviceMetrics = telemetry.getDeviceMetrics();
            if (boardVoltage > 20) {
                voltage = boardVoltage;
                batteryLevel = boardBatteryLevel;
            }
            TelemetryProtos.DeviceMetrics devicemetrics = TelemetryProtos.DeviceMetrics.newBuilder()
                    .setBatteryLevel(batteryLevel)
                    .setVoltage(voltage)
                    .build();

            MeshProtos.NodeInfo newnode = node.toBuilder()
                    .setDeviceMetrics(deviceMetrics)
                    .build();
            nodeDB.addNode(newnode);
            BLog.d(TAG, "Telemetry, node: " + newnode.getUser().getLongName() +
                    ", battery: " + batteryLevel +
                    ", voltage: " + voltage);
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
            BLog.d(TAG, "ch:" + packet.getChannel()  + " username: " + message);

            // Add message to buffer for Bluetooth retrieval
            messages.addMessage(message, username);

        } catch (Exception e) {
            BLog.d(TAG, "can't get text message" + e.getMessage());
        }
    }

    int radioNodeNum = 0;

    private void handleReceivedAdmin(int fromNodeNum, AdminProtos.AdminMessage a) {

        try {
            sessionPasskey = a.getSessionPasskey();
            BLog.d(TAG, "Admin: Received session_passkey from " + fromNodeNum);
        } catch (Exception e) {

        }
        if (fromNodeNum == radioNodeNum) {
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


    /**
     * Send a text message through Meshtastic network
     *
     * @param message The text message to send
     * @param to      The destination node ID (use ID_BROADCAST for broadcast)
     * @return true if message was sent successfully, false otherwise
     */
    public boolean sendTextMessage(String message, String to) {
        try {
            if (message == null || message.trim().isEmpty()) {
                BLog.w(TAG, "Cannot send empty message");
                return false;
            }

            // Create text message DataPacket
            // We use ch1 per the mesh burn setup with camp channel in #1
            DataPacket data = new DataPacket(to, 1, message.trim());
            data.hopLimit = 2; // Set hop limit for mesh forwarding

            // Convert to MeshPacket and send
            MeshProtos.MeshPacket meshpacket = data.toProto(nodeDB);
            MeshProtos.ToRadio.Builder packet = MeshProtos.ToRadio.newBuilder();
            packet.setPacket(meshpacket);
            packet.build();

            BLog.d(TAG, "Sending text message: " + message + " to: " + to);
            sendToRadio(packet);

            return true;

        } catch (Exception e) {
            BLog.e(TAG, "Error sending text message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send a broadcast text message to all nodes
     *
     * @param message The text message to send
     * @return true if message was sent successfully, false otherwise
     */
    public boolean sendBroadcastMessage(String message) {
        return sendTextMessage(message, DataPacket.ID_BROADCAST);
    }

    /**
     * Get the Messages buffer instance for Bluetooth command access
     *
     * @return Messages instance
     */
    public Messages getMessages() {
        return messages;
    }
}
