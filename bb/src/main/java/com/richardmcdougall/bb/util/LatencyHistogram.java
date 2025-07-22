package com.richardmcdougall.bb.util;

import com.richardmcdougall.bbcommon.BLog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LatencyHistogram {
    private final String tag;
    private final Map<String, ConcurrentHashMap<Long, AtomicLong>> latencyHistograms = new HashMap<>();
    private ScheduledExecutorService statsScheduler;
    private boolean autoLogging = false;

    public LatencyHistogram(String tag) {
        this.tag = tag;
    }

    /**
     * Register a method to track latency for
     */
    public void registerMethod(String methodName) {
        latencyHistograms.put(methodName, new ConcurrentHashMap<>());
    }

    /**
     * Record a latency measurement for a specific method
     */
    public void recordLatency(String method, long latency) {
        ConcurrentHashMap<Long, AtomicLong> histogram = latencyHistograms.get(method);
        if (histogram != null) {
            histogram.computeIfAbsent(latency, k -> new AtomicLong()).incrementAndGet();
        }
    }

    /**
     * Get the raw latency histograms data
     */
    public Map<String, ConcurrentHashMap<Long, AtomicLong>> getLatencyHistograms() {
        return latencyHistograms;
    }

    /**
     * Enable automatic logging of stats at specified intervals
     */
    public void enableAutoLogging(int intervalSeconds) {
        if (!autoLogging) {
            statsScheduler = Executors.newScheduledThreadPool(1);
            statsScheduler.scheduleWithFixedDelay(this::logLatencyStats, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
            autoLogging = true;
        }
    }

    /**
     * Disable automatic logging
     */
    public void disableAutoLogging() {
        if (autoLogging && statsScheduler != null) {
            shutdown();
            autoLogging = false;
        }
    }

    /**
     * Log latency statistics for all registered methods
     */
    public void logLatencyStats() {
        for (String method : latencyHistograms.keySet()) {
            ConcurrentHashMap<Long, AtomicLong> histogram = latencyHistograms.get(method);
            if (!histogram.isEmpty()) {
                long totalCalls = histogram.values().stream().mapToLong(AtomicLong::get).sum();

                // Create power of 2 buckets: [0], [1], [2-3], [4-7], [8-15], [16-31], [32-63], [64-127], [128+]
                long[] buckets = new long[9];
                String[] bucketLabels = {"0ms", "1ms", "2-3ms", "4-7ms", "8-15ms", "16-31ms", "32-63ms", "64-127ms", "128+ms"};

                for (Map.Entry<Long, AtomicLong> entry : histogram.entrySet()) {
                    long latency = entry.getKey();
                    long count = entry.getValue().get();

                    int bucketIndex = getBucketIndex(latency);
                    buckets[bucketIndex] += count;
                }

                // Build the output string with non-zero buckets only
                StringBuilder bucketStats = new StringBuilder();
                for (int i = 0; i < buckets.length; i++) {
                    if (buckets[i] > 0) {
                        if (bucketStats.length() > 0) {
                            bucketStats.append(", ");
                        }
                        bucketStats.append(bucketLabels[i]).append(":").append(buckets[i]);
                    }
                }

                BLog.i(tag, method + " latency buckets (total: " + totalCalls + ") - " + bucketStats.toString());
            }
        }
    }

    /**
     * Get the bucket index for a given latency value
     */
    private int getBucketIndex(long latency) {
        if (latency == 0) {
            return 0;
        } else if (latency == 1) {
            return 1;
        } else if (latency <= 3) {
            return 2;
        } else if (latency <= 7) {
            return 3;
        } else if (latency <= 15) {
            return 4;
        } else if (latency <= 31) {
            return 5;
        } else if (latency <= 63) {
            return 6;
        } else if (latency <= 127) {
            return 7;
        } else {
            return 8;
        }
    }

    /**
     * Clear all histogram data
     */
    public void clear() {
        for (ConcurrentHashMap<Long, AtomicLong> histogram : latencyHistograms.values()) {
            histogram.clear();
        }
    }

    /**
     * Clear histogram data for a specific method
     */
    public void clear(String method) {
        ConcurrentHashMap<Long, AtomicLong> histogram = latencyHistograms.get(method);
        if (histogram != null) {
            histogram.clear();
        }
    }

    /**
     * Shutdown the scheduler and clean up resources
     */
    public void shutdown() {
        if (statsScheduler != null && !statsScheduler.isShutdown()) {
            statsScheduler.shutdown();
            try {
                if (!statsScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    statsScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                statsScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            statsScheduler = null;
        }
    }

    /**
     * Helper method to time and record the execution of a Runnable
     */
    public void timeExecution(String method, Runnable task) {
        long start = System.currentTimeMillis();
        try {
            task.run();
        } finally {
            long end = System.currentTimeMillis();
            recordLatency(method, end - start);
        }
    }

    /**
     * Helper method to time and record the execution of a task that returns a value
     */
    public <T> T timeExecution(String method, java.util.concurrent.Callable<T> task) throws Exception {
        long start = System.currentTimeMillis();
        try {
            return task.call();
        } finally {
            long end = System.currentTimeMillis();
            recordLatency(method, end - start);
        }
    }
}
