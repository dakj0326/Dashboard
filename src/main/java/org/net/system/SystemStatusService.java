package org.net.system;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public final class SystemStatusService {
    private final OperatingSystemMXBean operatingSystem =
            ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    public SystemStatusSnapshot read() {
        double cpu = operatingSystem == null ? -1 : operatingSystem.getCpuLoad();
        long memoryTotal = operatingSystem == null ? 0
                : operatingSystem.getTotalMemorySize();
        long memoryFree = operatingSystem == null ? 0
                : operatingSystem.getFreeMemorySize();

        return new SystemStatusSnapshot(
                clamp(cpu),
                Math.max(0, memoryTotal - memoryFree),
                Math.max(0, memoryTotal),
                hasNetwork()
        );
    }

    private boolean hasNetwork() {
        try {
            for (NetworkInterface network : Collections.list(
                    NetworkInterface.getNetworkInterfaces())) {
                if (network.isUp() && !network.isLoopback() && !network.isVirtual()) {
                    return true;
                }
            }
        } catch (SocketException ignored) {
            // If interfaces cannot be inspected, show the network as unavailable.
        }
        return false;
    }

    private static double clamp(double value) {
        if (value < 0) return 0;
        return Math.min(1, value);
    }
}
