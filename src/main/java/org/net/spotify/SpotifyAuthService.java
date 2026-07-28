package org.net.spotify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.net.settings.AppSettings;

public final class SpotifyAuthService {
    public static final String REDIRECT_URI = "http://127.0.0.1:43821/callback";
    private static final String SCOPES = String.join(" ",
            "user-read-currently-playing",
            "user-read-playback-state",
            "user-modify-playback-state"
    );
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final SpotifyAuthService INSTANCE = new SpotifyAuthService();

    private volatile String accessToken;
    private volatile long accessTokenExpiresAt;

    private SpotifyAuthService() {}

    public static SpotifyAuthService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<Void> connect(
            String clientId,
            Consumer<String> status,
            Runnable onConnected
    ) {
        return CompletableFuture.runAsync(() -> {
            HttpServer server = null;
            try {
                status.accept("Opening Spotify sign-in…");
                String verifier = createVerifier();
                String challenge = createChallenge(verifier);
                String state = UUID.randomUUID().toString();

                CompletableFuture<String> authorizationCode = new CompletableFuture<>();
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 43821), 0);
                HttpServer callbackServer = server;
                server.createContext("/callback", exchange -> {
                    String query = exchange.getRequestURI().getRawQuery();
                    String returnedState = queryParameter(query, "state");
                    String code = queryParameter(query, "code");
                    String error = queryParameter(query, "error");
                    String response;
                    if (!state.equals(returnedState)) {
                        response = "Spotify connection failed: invalid state.";
                        authorizationCode.completeExceptionally(
                                new IllegalStateException("Spotify returned an invalid state value.")
                        );
                    } else if (error != null) {
                        response = "Spotify connection was cancelled. You can close this tab.";
                        authorizationCode.completeExceptionally(
                                new IllegalStateException("Spotify authorization was denied.")
                        );
                    } else {
                        response = "Spotify is connected. You can close this tab and return to the dashboard.";
                        authorizationCode.complete(code);
                    }
                    byte[] body = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                    callbackServer.stop(1);
                });
                server.start();

                URI authorizationUri = URI.create(
                        "https://accounts.spotify.com/authorize"
                                + "?response_type=code"
                                + "&client_id=" + encode(clientId)
                                + "&scope=" + encode(SCOPES)
                                + "&redirect_uri=" + encode(REDIRECT_URI)
                                + "&state=" + encode(state)
                                + "&code_challenge_method=S256"
                                + "&code_challenge=" + encode(challenge)
                );
                if (!Desktop.isDesktopSupported()
                        || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    throw new IllegalStateException("The system browser could not be opened.");
                }
                Desktop.getDesktop().browse(authorizationUri);

                String code = authorizationCode.get(3, TimeUnit.MINUTES);
                exchangeAuthorizationCode(clientId, code, verifier);
                AppSettings.getInstance().set("spotify.clientId", clientId);
                status.accept("Connected");
                onConnected.run();
            } catch (Exception exception) {
                if (server != null) {
                    server.stop(0);
                }
                throw new RuntimeException(messageOf(exception), exception);
            }
        });
    }

    public synchronized String getValidAccessToken() throws IOException, InterruptedException {
        if (accessToken != null && System.currentTimeMillis() < accessTokenExpiresAt - 30_000) {
            return accessToken;
        }
        refreshAccessToken();
        return accessToken;
    }

    public boolean isConnected() {
        return !AppSettings.getInstance().get("spotify.refreshToken", "").isBlank();
    }

    public synchronized void disconnect() {
        accessToken = null;
        accessTokenExpiresAt = 0;
        AppSettings.getInstance().remove("spotify.refreshToken");
    }

    private void exchangeAuthorizationCode(String clientId, String code, String verifier)
            throws IOException, InterruptedException {
        String body = form(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", REDIRECT_URI,
                "client_id", clientId,
                "code_verifier", verifier
        );
        storeTokenResponse(sendTokenRequest(body), true);
    }

    private void refreshAccessToken() throws IOException, InterruptedException {
        AppSettings settings = AppSettings.getInstance();
        String refreshToken = settings.get("spotify.refreshToken", "");
        String clientId = settings.get("spotify.clientId", "");
        if (refreshToken.isBlank() || clientId.isBlank()) {
            throw new IllegalStateException("Spotify is not connected.");
        }
        String body = form(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", clientId
        );
        storeTokenResponse(sendTokenRequest(body), false);
    }

    private JsonNode sendTokenRequest(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Spotify token request failed (" + response.statusCode() + ").");
        }
        return JSON.readTree(response.body());
    }

    private synchronized void storeTokenResponse(JsonNode response, boolean requireRefreshToken) {
        accessToken = response.path("access_token").asText();
        accessTokenExpiresAt = System.currentTimeMillis()
                + response.path("expires_in").asLong(3600) * 1000;
        String refreshToken = response.path("refresh_token").asText("");
        if (!refreshToken.isBlank()) {
            AppSettings.getInstance().set("spotify.refreshToken", refreshToken);
        } else if (requireRefreshToken) {
            throw new IllegalStateException("Spotify did not provide a refresh token.");
        }
    }

    private static String createVerifier() {
        byte[] random = new byte[64];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private static String createChallenge(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static String queryParameter(String query, String name) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(name)) {
                return parts.length == 2
                        ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                        : "";
            }
        }
        return null;
    }

    private static String form(String... values) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i += 2) {
            if (!result.isEmpty()) result.append('&');
            result.append(encode(values[i])).append('=').append(encode(values[i + 1]));
        }
        return result.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String messageOf(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? "Spotify connection failed." : cause.getMessage();
    }
}
