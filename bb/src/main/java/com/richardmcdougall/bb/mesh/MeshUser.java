package com.richardmcdougall.bb.mesh;

import android.os.Parcel;
import android.os.Parcelable;

import com.geeksville.mesh.MeshProtos;

public class MeshUser implements Parcelable {

    public String id;
    public String longName;
    public String shortName;
    public MeshProtos.HardwareModel hwModel;
    public boolean isLicensed = false;
    public int role = 0;

    // Constructors

    public MeshUser(String id, String longName, String shortName, MeshProtos.HardwareModel hwModel) {
        this.id = id;
        this.longName = longName;
        this.shortName = shortName;
        this.hwModel = hwModel;
    }

    // Parcelable Implementation

    public MeshUser(Parcel in) {
        id = in.readString();
        longName = in.readString();
        shortName = in.readString();
        // Note: hwModel needs to be handled based on how it's serialized in the Parcel
        // For example, if you're sending its ordinal value:
        hwModel = MeshProtos.HardwareModel.forNumber(in.readInt());
        isLicensed = in.readByte() != 0;
        role = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(longName);
        dest.writeString(shortName);
        // Serialize hwModel as needed (e.g., send its ordinal)
        dest.writeInt(hwModel.getNumber());
        dest.writeByte((byte) (isLicensed ? 1 : 0)); // Boolean to byte
        dest.writeInt(role);
    }

    public final Creator<MeshUser> CREATOR = new Creator<MeshUser>() {
        public MeshUser createFromParcel(Parcel in) {
            return new MeshUser(in);
        }

        public MeshUser[] newArray(int size) {
            return new MeshUser[size];
        }
    };

    // Other methods (toProto, hwModelString, equals, hashCode, toString)

    public MeshProtos.User toProto() {
        return MeshProtos.User.newBuilder()
                .setId(id)
                .setLongName(longName)
                .setShortName(shortName)
                .setHwModel(hwModel)
                .setIsLicensed(isLicensed)
                .setRoleValue(role)
                .build();
    }

    public String getHwModelString() {
        if (hwModel == MeshProtos.HardwareModel.UNSET) {
            return null;
        } else {
            return hwModel.name().replace('_', '-').replace('p', '.').toLowerCase();
        }
    }

}
