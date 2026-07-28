package org.net.spotify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class SpotifyApiClient {
    private static final URI API_BASE = URI.create("https://api.spotify.com/v1/");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SpotifyApiClient INSTANCE = new SpotifyApiClient();

    private final SpotifyAuthService auth = SpotifyAuthService.getInstance();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private SpotifyApiClient() {}

    public static SpotifyApiClient getInstance() {
        return INSTANCE;
    }

    public SpotifyPlayback getCurrentPlayback() throws IOException, InterruptedException {
        HttpResponse<String> response = send("me/player/currently-playing", "GET");
        if (response.statusCode() == 204) {
            return SpotifyPlayback.empty();
        }
        requireSuccess(response);

        JsonNode root = JSON.readTree(response.body());
        JsonNode item = root.path("item");
        if (item.isMissingNode() || item.isNull()) {
            return SpotifyPlayback.empty();
        }

        String artist = "";
        JsonNode artists = item.path("artists");
        if (artists.isArray() && !artists.isEmpty()) {
            artist = artists.get(0).path("name").asText("");
        }

        String imageUrl = "";
        JsonNode images = item.path("album").path("images");
        if (images.isArray() && !images.isEmpty()) {
            // Spotify returns album images from largest to smallest.
            imageUrl = images.get(0).path("url").asText("");
        }

        return new SpotifyPlayback(
                item.path("name").asText("Unknown title"),
                artist,
                imageUrl,
                item.path("external_urls").path("spotify").asText(""),
                root.path("progress_ms").asLong(0),
                item.path("duration_ms").asLong(0),
                root.path("is_playing").asBoolean(false)
        );
    }

    public CompletableFuture<Void> previous() {
        return control("me/player/previous", "POST");
    }

    public CompletableFuture<Void> next() {
        return control("me/player/next", "POST");
    }

    public CompletableFuture<Void> play() {
        return control("me/player/play", "PUT");
    }

    public CompletableFuture<Void> pause() {
        return control("me/player/pause", "PUT");
    }

    public CompletableFuture<List<SpotifyDevice>> getDevices() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = send("me/player/devices", "GET");
                requireSuccess(response);
                JsonNode devices = JSON.readTree(response.body()).path("devices");
                List<SpotifyDevice> result = new ArrayList<>();
                if (devices.isArray()) {
                    for (JsonNode device : devices) {
                        result.add(new SpotifyDevice(
                                device.path("id").asText(""),
                                device.path("name").asText("Unknown device"),
                                device.path("type").asText("Device"),
                                device.path("is_active").asBoolean(false),
                                device.path("is_restricted").asBoolean(false)
                        ));
                    }
                }
                return result;
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException(exception);
            }
        });
    }

    public CompletableFuture<Void> transferPlayback(String deviceId, boolean continuePlaying) {
        return CompletableFuture.runAsync(() -> {
            try {
                String token = auth.getValidAccessToken();
                String body = JSON.writeValueAsString(Map.of(
                        "device_ids", List.of(deviceId),
                        "play", continuePlaying
                ));
                HttpRequest request = HttpRequest.newBuilder(API_BASE.resolve("me/player"))
                        .timeout(Duration.ofSeconds(12))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                requireSuccess(http.send(request, HttpResponse.BodyHandlers.ofString()));
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException(exception);
            }
        });
    }

    private CompletableFuture<Void> control(String path, String method) {
        return CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<String> response = send(path, method);
                requireSuccess(response);
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException(exception);
            }
        });
    }

    private HttpResponse<String> send(String path, String method)
            throws IOException, InterruptedException {
        String token = auth.getValidAccessToken();
        HttpRequest.Builder builder = HttpRequest.newBuilder(API_BASE.resolve(path))
                .timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + token);
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else if ("PUT".equals(method)) {
            builder.PUT(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.GET();
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void requireSuccess(HttpResponse<String> response) throws IOException {
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Spotify request failed (" + response.statusCode() + ").");
        }
    }
}
