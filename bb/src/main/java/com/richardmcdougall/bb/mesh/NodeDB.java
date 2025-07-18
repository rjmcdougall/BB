package com.richardmcdougall.bb.mesh;

import com.geeksville.mesh.MeshProtos;
import com.richardmcdougall.bb.BBService;
import com.richardmcdougall.bbcommon.BLog;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public NodeDB(BBService service) {
        this.service = service;
        // Add a new node:
        //MeshUser user;
        //user.id =
        //NodeInfo newNode = new NodeInfo(NODENUM_BROADCAST, );
        //nodeDB.nodeDBbyNodeNum.put(newNode.num, newNode);
    }

    public List<Node> getNodes() {
        List<Node> nodelist = new ArrayList<>();
        for (Map.Entry<Integer, MeshProtos.NodeInfo> entry : nodeDBbyNodeNum.entrySet()) {
            Integer key = entry.getKey();
            MeshProtos.NodeInfo nodeinfo = entry.getValue();
            try {
                Node node = new Node();
                node.name = nodeinfo.getUser().getLongName();
                node.shortname = nodeinfo.getUser().getShortName();
                node.latitude = nodeinfo.getPosition().getLatitudeI() / 10000000.0;
                node.longitude = nodeinfo.getPosition().getLongitudeI() / 10000000.0;
                node.battery_pct = 1.0f * nodeinfo.getDeviceMetrics().getBatteryLevel();
                node.lastheard_epoch_ms = nodeinfo.getLastHeard();
                nodelist.add(node);
            } catch (Exception e) {
            }

        }
        return nodelist;
    }

    /**
     * Adds or updates a node in the database.
     * The node number obtained from {@code nodeInfo.getNodeNum()} is used as the key.
     * If a node with the same node number already exists, its information will be updated.
     *
     * @param nodeInfo The {@link MeshProtos.NodeInfo} object to add or update. Must not be null.
     * The {@code nodeInfo.getNodeNum()} will be used as the key.
     * @throws IllegalArgumentException if {@code nodeInfo} is null.
     */
    public void addNode(MeshProtos.NodeInfo nodeInfo) {
        if (nodeInfo == null) {
            // ConcurrentHashMap does not permit null values.
            throw new IllegalArgumentException("NodeInfo cannot be null.");
        }
        // nodeInfo.getNodeNum() returns int, which is autoboxed to Integer for the key.
        // ConcurrentHashMap does not permit null keys.
        nodeDBbyNodeNum.put(nodeInfo.getNum(), nodeInfo);
        BLog.d(TAG, "Added/Updated node: " + nodeInfo.getNum());
    }

    /**
     * Finds a node in the database by its node number.
     *
     * @param nodeNum The node number (Integer) to search for.
     * @return The {@link MeshProtos.NodeInfo} object if found, or {@code null} if no node
     * with the given number exists or if {@code nodeNum} is null.
     */
    public MeshProtos.NodeInfo findNode(Integer nodeNum) {
        if (nodeNum == null) {
            // ConcurrentHashMap does not permit null keys for get(), it would throw NullPointerException.
            // So, we handle it explicitly by returning null or logging.
            // System.err.println("Attempted to find node with null nodeNum.");
            return null;
        }
        return nodeDBbyNodeNum.get(nodeNum);
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