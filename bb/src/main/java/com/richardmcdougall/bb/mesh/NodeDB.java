package com.richardmcdougall.bb.mesh;

import com.geeksville.mesh.MeshProtos;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeDB {

    private String TAG = this.getClass().getSimpleName();

    private BBService service;

    public static final String ID_BROADCAST = "^all";
    public static final String ID_LOCAL = "^local";
    public static final int NODENUM_BROADCAST = (0xffffffff);

    // The database of active nodes, indexed by node number
    private final ConcurrentHashMap<Integer, MeshProtos.NodeInfo> nodeDBbyNodeNum = new ConcurrentHashMap<>();
    // ... Other methods and fields related to NodeDB ...

    public NodeDB(BBService service) {
        this.service = service;
        // Add a new node:
        //MeshUser user;
        //user.id =
        //NodeInfo newNode = new NodeInfo(NODENUM_BROADCAST, );
        //nodeDB.nodeDBbyNodeNum.put(newNode.num, newNode);
    }


    public int getMyNodeNum() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(service.boardState.GetDeviceID().getBytes(StandardCharsets.UTF_8));

            // Convert the first 4 bytes of the hash to an int
            return ByteBuffer.wrap(encodedHash, 0, 4).getInt();
        } catch (Exception e) {
            BLog.e(TAG, "Error creating hash: " + e.getMessage());
            return 0; // Return 0 on error
        }

    }

    public int toNodeNum(String id) {
        if (id.equals(DataPacket.ID_BROADCAST)) {
            return DataPacket.NODENUM_BROADCAST;
        } else if (id.equals(DataPacket.ID_LOCAL)) {
            BLog.d(TAG, "toNodeNum: ID_LOCAL");
            return getMyNodeNum();
        } else {
            BLog.d(TAG, "toNodeNum: fallback");
            return toNodeInfo(id).getNum();
        }
    }


    public static String toNodeID(int nodeNum) {
        return String.format("0x%08X", nodeNum);
    }
    private MeshProtos.NodeInfo toNodeInfo(String idm) {
        // Implement logic to retrieve NodeEntity based on ID.
        // Throw appropriate exceptions if not found.
        throw new UnsupportedOperationException("toNodeInfo needs to be implemented.");
    }


}