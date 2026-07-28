package org.net.spotify;

public record SpotifyDevice(
        String id,
        String name,
        String type,
        boolean active,
        boolean restricted
) {}
