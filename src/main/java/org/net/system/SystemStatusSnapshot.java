package org.net.system;

public record SystemStatusSnapshot(
        double cpuUsage,
        long memoryUsed,
        long memoryTotal,
        boolean networkAvailable
) {}
