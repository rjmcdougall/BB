package com.richardmcdougall.bb.mesh;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class PacketIdGenerator {

    private static final long NUM_PACKET_IDS = (1L << 32) - 1; // Mask for valid packet ID bits
    private static AtomicLong currentPacketId = new AtomicLong(new Random(System.currentTimeMillis()).nextLong());

    /**
     * Generate a unique packet ID, ensuring it stays within the 32-bit range.
     *
     * @return The generated packet ID.
     */
    public static synchronized int generatePacketId() {
        currentPacketId.getAndIncrement();

        currentPacketId.set(currentPacketId.get() & 0xffffffffL); // Keep within 32 bits

        // Use modulus and +1 to skip 0 and ensure the ID is within the valid range.
        return (int) ((currentPacketId.get() % NUM_PACKET_IDS) + 1L);
    }
}