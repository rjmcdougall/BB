package com.richardmcdougall.bb.mesh;

import com.richardmcdougall.bbcommon.BLog;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Messages class manages a buffered list of single-line text messages received from Meshtastic.
 * Messages are inserted when "Text message" is received in Meshtastic.java, 
 * and are polled by the 'getmessages' Bluetooth command.
 */
public class Messages {
    private String TAG = this.getClass().getSimpleName();
    
    // Thread-safe list to store incoming messages
    private final LinkedList<String> messageBuffer = new LinkedList<>();
    
    // Thread-safe list to track messages that haven't been retrieved via Bluetooth yet
    private final LinkedList<String> newMessages = new LinkedList<>();
    
    // Lock for thread-safe operations
    private final ReentrantLock lock = new ReentrantLock();
    
    // Maximum number of messages to keep in buffer to prevent memory issues
    private static final int MAX_BUFFER_SIZE = 100;
    
    // Maximum number of new messages to keep before oldest are discarded
    private static final int MAX_NEW_MESSAGES = 50;
    
    public Messages() {
        BLog.d(TAG, "Messages buffer initialized");
    }
    
    /**
     * Adds a new text message to the buffer. Called when a text message is received from Meshtastic.
     * 
     * @param message The text message content (single line)
     * @param sender The sender's name/identifier (optional, can be null)
     */
    public void addMessage(String message, String sender) {
        if (message == null || message.trim().isEmpty()) {
            BLog.w(TAG, "Ignoring empty or null message");
            return;
        }
        
        lock.lock();
        try {
            // Format message with sender if provided
            String formattedMessage;
            if (sender != null && !sender.trim().isEmpty()) {
                formattedMessage = sender + ": " + message.trim();
            } else {
                formattedMessage = message.trim();
            }
            
            // Add to both buffers
            messageBuffer.add(formattedMessage);
            newMessages.add(formattedMessage);
            
            // Maintain maximum buffer sizes
            while (messageBuffer.size() > MAX_BUFFER_SIZE) {
                String removed = messageBuffer.removeFirst();
                BLog.d(TAG, "Removed old message from buffer: " + removed);
            }
            
            while (newMessages.size() > MAX_NEW_MESSAGES) {
                String removed = newMessages.removeFirst();
                BLog.d(TAG, "Removed old new message: " + removed);
            }
            
            BLog.d(TAG, "Added message to buffer: " + formattedMessage);
            BLog.d(TAG, "Buffer size: " + messageBuffer.size() + ", New messages: " + newMessages.size());
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Retrieves all new messages since the last time this method was called.
     * This method clears the new messages list after returning them.
     * 
     * @return List of new messages, or empty list if no new messages
     */
    public List<String> getNewMessages() {
        lock.lock();
        try {
            // Create a copy of new messages and clear the original list
            List<String> messages = new ArrayList<>(newMessages);
            newMessages.clear();
            
            BLog.d(TAG, "Retrieved " + messages.size() + " new messages via Bluetooth");
            
            return messages;
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Gets all messages in the buffer (for debugging or full history retrieval)
     * 
     * @return List of all messages in buffer
     */
    public List<String> getAllMessages() {
        lock.lock();
        try {
            List<String> messages = new ArrayList<>(messageBuffer);
            BLog.d(TAG, "Retrieved all " + messages.size() + " messages from buffer");
            return messages;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Gets the current count of new messages waiting to be retrieved
     * 
     * @return Number of new messages
     */
    public int getNewMessageCount() {
        lock.lock();
        try {
            return newMessages.size();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Gets the total buffer size
     * 
     * @return Total number of messages in buffer
     */
    public int getTotalMessageCount() {
        lock.lock();
        try {
            return messageBuffer.size();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Converts new messages to JSON Array format for Bluetooth response
     * 
     * @return JSONArray of new messages
     */
    public JSONArray getNewMessagesJSON() {
        List<String> messages = getNewMessages();
        JSONArray jsonArray = new JSONArray();
        
        for (String message : messages) {
            jsonArray.put(message);
        }
        
        BLog.d(TAG, "Created JSON array with " + messages.size() + " messages");
        return jsonArray;
    }
    
    /**
     * Clears all messages from the buffer (for testing or reset purposes)
     */
    public void clearAll() {
        lock.lock();
        try {
            int totalCleared = messageBuffer.size();
            int newCleared = newMessages.size();
            
            messageBuffer.clear();
            newMessages.clear();
            
            BLog.d(TAG, "Cleared all messages - Total: " + totalCleared + ", New: " + newCleared);
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Gets buffer statistics for debugging
     * 
     * @return String containing buffer statistics
     */
    public String getStats() {
        lock.lock();
        try {
            return String.format("Messages Buffer Stats - Total: %d/%d, New: %d/%d", 
                    messageBuffer.size(), MAX_BUFFER_SIZE,
                    newMessages.size(), MAX_NEW_MESSAGES);
        } finally {
            lock.unlock();
        }
    }
}
