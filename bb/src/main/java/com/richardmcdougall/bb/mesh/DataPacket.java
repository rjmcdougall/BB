package com.richardmcdougall.bb.mesh;

import android.os.Parcel;
import android.os.Parcelable;
import com.geeksville.mesh.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;


public class DataPacket implements Parcelable {
    // Special node IDs
    public static final String ID_BROADCAST = "^all";
    public static final String ID_LOCAL = "^local";
    public static final int NODENUM_BROADCAST = (0xffffffff);

    public String to; // a nodeID string, or ID_BROADCAST for broadcast
    public byte[] bytes;
    public int dataType;  // A port number (see portnums.proto)
    public String from; // a nodeID string, or ID_LOCAL for localhost
    public long time; // msecs since 1970
    public int id; // 0 means unassigned
    public MessageStatus status;
    public int hopLimit;
    public int channel; // channel index

    public String errorMessage; // If there was an error with this message

    // Constructors

    public DataPacket(String to, int channel, String text) {
        this.id = PacketIdGenerator.generatePacketId();
        this.to = to;
        this.bytes = text.getBytes();
        this.dataType = Portnums.PortNum.TEXT_MESSAGE_APP_VALUE;
        this.channel = channel;
        this.from = ID_LOCAL; // Default to local sender
        this.time = System.currentTimeMillis();
    }

    public DataPacket(String to, int channel, MeshProtos.NodeInfo nodeinfo) {
        this.id = PacketIdGenerator.generatePacketId();
        this.to = to;
        this.bytes = nodeinfo.toByteArray();
        this.dataType = Portnums.PortNum.NODEINFO_APP_VALUE;
        this.channel = channel;
        this.from = ID_LOCAL; // Default to local sender
        this.time = System.currentTimeMillis();
    }


    public DataPacket(String to, int channel, TelemetryProtos.Telemetry metrics) {
        this.id = PacketIdGenerator.generatePacketId();
        this.to = to;
        this.bytes = metrics.toByteArray();
        this.dataType = Portnums.PortNum.TELEMETRY_APP_VALUE;
        this.channel = channel;
        this.from = ID_LOCAL; // Default to local sender
        this.time = System.currentTimeMillis();
    }

    public DataPacket(String to, int channel, MeshProtos.Position position) {
        this.id = PacketIdGenerator.generatePacketId();
        this.to = to;
        this.bytes = position.toByteArray();
        this.dataType = Portnums.PortNum.POSITION_APP_VALUE;
        this.channel = channel;
        this.from = ID_LOCAL; // Default to local sender
        this.time = System.currentTimeMillis();
    }

    public DataPacket(String to, int channel, MeshProtos.Waypoint waypoint) {
        this.to = to;
        this.bytes = waypoint.toByteArray();
        this.dataType = Portnums.PortNum.WAYPOINT_APP_VALUE;
        this.channel = channel;
        this.from = ID_LOCAL; // Default to local sender
        this.time = System.currentTimeMillis();
    }

    // Parcelable Implementation

    public DataPacket(Parcel in) {
        to = in.readString();
        bytes = in.createByteArray();
        dataType = in.readInt();
        from = in.readString();
        time = in.readLong();
        id = in.readInt();
        status = in.readParcelable(MessageStatus.class.getClassLoader());
        hopLimit = in.readInt();
        channel = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(to);
        dest.writeByteArray(bytes);
        dest.writeInt(dataType);
        dest.writeString(from);
        dest.writeLong(time);
        dest.writeInt(id);
        dest.writeParcelable(status, flags);
        dest.writeInt(hopLimit);
        dest.writeInt(channel);
    }

    public void readFromParcel(Parcel in) {
        to = in.readString();
        in.createByteArray(); // Skip reading bytes
        in.readInt(); // Skip reading dataType
        from = in.readString();
        time = in.readLong();
        id = in.readInt();
        status = in.readParcelable(MessageStatus.class.getClassLoader());
        hopLimit = in.readInt();
        channel = in.readInt();
    }

    public static final Parcelable.Creator<DataPacket> CREATOR = new Parcelable.Creator<DataPacket>() {
        public DataPacket createFromParcel(Parcel in) {
            return new DataPacket(in);
        }

        public DataPacket[] newArray(int size) {
            return new DataPacket[size];
        }
    };

    // Getters (add setters if needed)

    public String getText() {
        if (dataType == Portnums.PortNum.TEXT_MESSAGE_APP_VALUE) {
            return new String(bytes);
        } else {
            return null;
        }
    }

    public MeshProtos.Waypoint getWaypoint() {
        if (dataType == Portnums.PortNum.WAYPOINT_APP_VALUE) {
            try {
                return MeshProtos.Waypoint.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                // Handle exception
                return null;
            }
        } else {
            return null;
        }
    }


    @Override
    public String toString() {
        return "DataPacket{" +
                "to='" + to + '\'' +
                ", bytes=" + Arrays.toString(bytes) +
                ", dataType=" + dataType +
                ", from='" + from + '\'' +
                ", time=" + time +
                ", id=" + id +
                ", status=" + status +
                ", hopLimit=" + hopLimit +
                ", channel=" + channel +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    public MeshProtos.MeshPacket toProto(NodeDB nodeDB) {
        DataPacket p = this;
        int toNum = nodeDB.toNodeNum(p.to);
        int fromNum = nodeDB.toNodeNum(p.from);

        MeshProtos.MeshPacket.Builder builder = MeshProtos.MeshPacket.newBuilder();
        builder.setFrom(fromNum); // Assuming the sender node number is always 0
        builder.setTo(toNum);
        builder.setId(p.id);
        builder.setWantAck(false);
        builder.setHopLimit(p.hopLimit);
        builder.setChannel(p.channel);

        MeshProtos.Data.Builder dataBuilder = MeshProtos.Data.newBuilder();
        dataBuilder.setPortnumValue(p.dataType);
        dataBuilder.setPayload(ByteString.copyFrom(p.bytes));


        // Add PKI encryption logic if necessary based on your application:
        /* if (dataBuilder.getPortnumValue() == Portnums.PortNum.TEXT_MESSAGE_APP_VALUE ||
               dataBuilder.getPortnumValue() == Portnums.PortNum.ADMIN_APP_VALUE) {
            NodeEntity destNode = nodeDBbyNodeNum.get(toNum);
            if (destNode != null && destNode.getUser().hasPublicKey()) {
                ByteString publicKey = destNode.getUser().getPublicKey();
                if (!publicKey.isEmpty()) {
                    builder.setPkiEncrypted(true);
                    builder.setPublicKey(publicKey);
                }
            }
        } */


        builder.setDecoded(dataBuilder.build());
        return builder.build();
    }
    // Other methods (equals, hashCode, toString)
}