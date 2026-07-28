package org.net.spotify;

public record SpotifyPlayback(
        String title,
        String artist,
        String imageUrl,
        String spotifyUrl,
        long progressMs,
        long durationMs,
        boolean playing
) {
    public static SpotifyPlayback empty() {
        return new SpotifyPlayback("", "", "", "", 0, 0, false);
    }
}
