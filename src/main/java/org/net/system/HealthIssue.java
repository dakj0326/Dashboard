package org.net.system;

public record HealthIssue(
        String source,
        HealthSeverity severity
) {}
