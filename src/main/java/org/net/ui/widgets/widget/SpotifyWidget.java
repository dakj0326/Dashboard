package org.net.ui.widgets.widget;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.net.spotify.SpotifyApiClient;
import org.net.spotify.SpotifyAuthService;
import org.net.spotify.SpotifyDevice;
import org.net.spotify.SpotifyPlayback;
import org.net.system.HealthSeverity;
import org.net.system.SystemHealthService;
import org.net.ui.widgets.BaseWidget;
import org.net.ui.widgets.WidgetID;

public class SpotifyWidget extends BaseWidget {
    private static final double PREFERRED_WIDTH = 440;
    private final SpotifyAuthService auth = SpotifyAuthService.getInstance();
    private final SpotifyApiClient api = SpotifyApiClient.getInstance();
    private final SystemHealthService health = SystemHealthService.getInstance();
    private final ImageView artwork = new ImageView();
    private final Label title = new Label("Connect Spotify in Preferences");
    private final Label artist = new Label("Spotify is not connected");
    private final Label elapsed = new Label("0:00");
    private final Label duration = new Label("0:00");
    private final DoubleProperty progressFraction = new SimpleDoubleProperty(0);
    private final StackPane progressTrack = new StackPane();
    private final Region progressFill = new Region();
    private final Button previous = controlButton("⏮", "Previous track");
    private final Button playPause = controlButton("▶", "Play");
    private final Button next = controlButton("⏭", "Next track");
    private final MenuButton deviceMenu = new MenuButton("Output");
    private final ScheduledExecutorService poller;
    private volatile SpotifyPlayback playback = SpotifyPlayback.empty();
    private volatile long playbackReceivedAt;
    private volatile boolean active = true;
    private String loadedImageUrl = "";

    public SpotifyWidget() {
        super("Spotify", new HBox(), WidgetID.SPOTIFY, PREFERRED_WIDTH);
        createContent();
        poller = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "spotify-widget-poller");
            thread.setDaemon(true);
            return thread;
        });
        poller.scheduleWithFixedDelay(this::pollPlayback, 0, 3, TimeUnit.SECONDS);
        poller.scheduleAtFixedRate(() -> Platform.runLater(this::updateProgress),
                1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        active = visible;
        if (!visible) {
            health.clear("api.spotify");
        }
        if (visible && poller != null) {
            poller.execute(this::pollPlayback);
        }
    }

    private void createContent() {
        HBox root = (HBox) getCenter();
        root.setSpacing(16);
        root.setAlignment(Pos.CENTER_LEFT);

        artwork.setFitWidth(126);
        artwork.setFitHeight(126);
        artwork.setPreserveRatio(true);
        artwork.setSmooth(true);
        StackPane artworkFrame = new StackPane(artwork);
        artworkFrame.getStyleClass().add("spotify-artwork");
        artworkFrame.setMinSize(126, 126);
        artworkFrame.setMaxSize(126, 126);
        artworkFrame.setOnMouseClicked(e -> openCurrentTrack());

        title.getStyleClass().add("spotify-title");
        title.setMaxWidth(Double.MAX_VALUE);
        artist.getStyleClass().add("spotify-artist");

        HBox times = new HBox(elapsed, new Label(" / "), duration);
        times.getStyleClass().add("spotify-time");
        progressTrack.getStyleClass().add("spotify-progress-track");
        progressFill.getStyleClass().add("spotify-progress-fill");
        progressTrack.setAlignment(Pos.CENTER_LEFT);
        progressTrack.setMinHeight(7);
        progressTrack.setPrefHeight(7);
        progressTrack.setMaxHeight(7);
        progressTrack.setMaxWidth(Double.MAX_VALUE);
        progressFill.setMinHeight(7);
        progressFill.setPrefHeight(7);
        progressFill.setMaxHeight(7);
        progressFill.prefWidthProperty().bind(
                progressTrack.widthProperty().multiply(progressFraction)
        );
        progressFill.maxWidthProperty().bind(
                progressTrack.widthProperty().multiply(progressFraction)
        );
        progressTrack.getChildren().add(progressFill);

        previous.setOnAction(e -> runControl(api.previous()));
        playPause.setOnAction(e -> runControl(
                playback.playing() ? api.pause() : api.play()
        ));
        next.setOnAction(e -> runControl(api.next()));
        deviceMenu.getStyleClass().add("spotify-device-menu");
        deviceMenu.setTooltip(new Tooltip("Choose playback device"));
        deviceMenu.setMaxWidth(105);
        deviceMenu.setOnShowing(e -> loadDevices());
        Region controlSpacer = new Region();
        HBox.setHgrow(controlSpacer, Priority.ALWAYS);
        HBox controls = new HBox(10, previous, playPause, next, controlSpacer, deviceMenu);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setMaxWidth(Double.MAX_VALUE);

        VBox details = new VBox(5, title, artist, progressTrack, times, controls);
        details.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(details, Priority.ALWAYS);
        root.getChildren().addAll(artworkFrame, details);
        setControlsDisabled(!auth.isConnected());
    }

    private void pollPlayback() {
        if (!active) {
            return;
        }
        if (!auth.isConnected()) {
            health.clear("api.spotify");
            Platform.runLater(this::showDisconnected);
            return;
        }
        try {
            SpotifyPlayback latest = api.getCurrentPlayback();
            health.clear("api.spotify");
            playback = latest;
            playbackReceivedAt = System.currentTimeMillis();
            Platform.runLater(() -> display(latest));
        } catch (Exception exception) {
            if (active) {
                health.report("api.spotify", HealthSeverity.WARNING);
            }
            Platform.runLater(() -> {
                artist.setText(messageOf(exception));
                setControlsDisabled(true);
            });
        }
    }

    private void display(SpotifyPlayback latest) {
        setControlsDisabled(false);
        if (latest.title().isBlank()) {
            title.setText("Nothing playing");
            artist.setText("Start playback in Spotify");
            progressFraction.set(0);
            elapsed.setText("0:00");
            duration.setText("0:00");
            playPause.setText("▶");
            return;
        }

        title.setText(latest.title());
        artist.setText(latest.artist());
        duration.setText(formatTime(latest.durationMs()));
        playPause.setText(latest.playing() ? "⏸" : "▶");
        playPause.setTooltip(new Tooltip(latest.playing() ? "Pause" : "Play"));
        if (!latest.imageUrl().equals(loadedImageUrl)) {
            loadedImageUrl = latest.imageUrl();
            if (latest.imageUrl().isBlank()) {
                artwork.setImage(null);
                clearAdaptiveBackground();
            } else {
                Image image = new Image(latest.imageUrl(), true);
                artwork.setImage(image);
                image.progressProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() >= 1 && !image.isError()) {
                        setAdaptiveBackground(image, 0.78);
                    }
                });
                if (image.getProgress() >= 1 && !image.isError()) {
                    setAdaptiveBackground(image, 0.78);
                }
            }
        }
        updateProgress();
    }

    private void updateProgress() {
        if (!active) {
            return;
        }
        long current = playback.progressMs();
        if (playback.playing()) {
            current += System.currentTimeMillis() - playbackReceivedAt;
        }
        current = Math.min(current, playback.durationMs());
        elapsed.setText(formatTime(current));
        progressFraction.set(playback.durationMs() == 0
                ? 0
                : (double) current / playback.durationMs());
    }

    private void showDisconnected() {
        playback = SpotifyPlayback.empty();
        title.setText("Connect Spotify in Preferences");
        artist.setText("Spotify is not connected");
        artwork.setImage(null);
        clearAdaptiveBackground();
        loadedImageUrl = "";
        updateProgress();
        setControlsDisabled(true);
    }

    private void runControl(java.util.concurrent.CompletableFuture<Void> action) {
        setControlsDisabled(true);
        action.whenComplete((ignored, error) -> Platform.runLater(() -> {
            setControlsDisabled(false);
            if (error != null) {
                health.report("api.spotify", HealthSeverity.WARNING);
                artist.setText(messageOf(error));
            } else {
                health.clear("api.spotify");
                poller.schedule(this::pollPlayback, 350, TimeUnit.MILLISECONDS);
            }
        }));
    }

    private void setControlsDisabled(boolean disabled) {
        previous.setDisable(disabled);
        playPause.setDisable(disabled);
        next.setDisable(disabled);
        deviceMenu.setDisable(disabled);
    }

    private void loadDevices() {
        deviceMenu.getItems().setAll(disabledMenuItem("Loading devices…"));
        api.getDevices().whenComplete((devices, error) -> Platform.runLater(() -> {
            deviceMenu.getItems().clear();
            if (error != null) {
                health.report("api.spotify", HealthSeverity.WARNING);
                deviceMenu.getItems().add(disabledMenuItem(messageOf(error)));
                return;
            }
            if (devices.isEmpty()) {
                deviceMenu.getItems().add(disabledMenuItem("No Spotify devices found"));
                return;
            }

            ToggleGroup activeDevice = new ToggleGroup();
            for (SpotifyDevice device : devices) {
                String type = device.type().isBlank() ? "" : " · " + device.type();
                RadioMenuItem item = new RadioMenuItem(device.name() + type);
                item.setToggleGroup(activeDevice);
                item.setSelected(device.active());
                item.setDisable(device.restricted() || device.id().isBlank());
                item.setOnAction(e -> transferTo(device));
                deviceMenu.getItems().add(item);
            }
            deviceMenu.getItems().add(new SeparatorMenuItem());
            deviceMenu.getItems().add(disabledMenuItem(
                    "Bluetooth outputs appear through their Spotify device"
            ));
        }));
    }

    private void transferTo(SpotifyDevice device) {
        deviceMenu.setText(device.name());
        deviceMenu.setDisable(true);
        api.transferPlayback(device.id(), playback.playing())
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    deviceMenu.setDisable(false);
                    if (error != null) {
                        health.report("api.spotify", HealthSeverity.WARNING);
                        artist.setText(messageOf(error));
                        deviceMenu.setText("Output");
                    } else {
                        health.clear("api.spotify");
                        poller.schedule(this::pollPlayback, 500, TimeUnit.MILLISECONDS);
                    }
                }));
    }

    private static MenuItem disabledMenuItem(String text) {
        MenuItem item = new MenuItem(text);
        item.setDisable(true);
        return item;
    }

    private void openCurrentTrack() {
        if (playback.spotifyUrl().isBlank()) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(playback.spotifyUrl()));
            }
        } catch (Exception ignored) {
            // Opening Spotify is a convenience; playback remains usable if it fails.
        }
    }

    private static Button controlButton(String text, String tooltip) {
        Button button = new Button(text);
        button.getStyleClass().add("spotify-control");
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private static String formatTime(long milliseconds) {
        long seconds = Math.max(0, milliseconds / 1000);
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private static String messageOf(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? "Spotify is unavailable" : cause.getMessage();
    }
}
