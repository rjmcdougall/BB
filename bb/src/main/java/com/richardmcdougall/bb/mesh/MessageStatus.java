package com.richardmcdougall.bb.mesh;

import android.os.Parcel;
import android.os.Parcelable;

public enum MessageStatus implements Parcelable {
    UNKNOWN, // Not set for this message
    RECEIVED, // Came in from the mesh
    QUEUED, // Waiting to send to the mesh as soon as we connect to the device
    ENROUTE, // Delivered to the radio, but no ACK or NAK received
    DELIVERED, // We received an ack
    ERROR; // We received back a nak, message not delivered

    // Parcelable Implementation

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    public static final Parcelable.Creator<MessageStatus> CREATOR = new Parcelable.Creator<MessageStatus>() {
        public MessageStatus createFromParcel(Parcel in) {
            return MessageStatus.values()[in.readInt()];
        }

        public MessageStatus[] newArray(int size) {
            return new MessageStatus[size];
        }
    };
}