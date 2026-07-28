package org.net.ui.pages.settings;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.net.settings.AppSettings;
import org.net.spotify.SpotifyAuthService;

public class SettingsWidgetsPage extends ScrollPane {
    private final SpotifyAuthService spotify = SpotifyAuthService.getInstance();
    private final TextField clientId = new TextField();
    private final Label connectionStatus = new Label();
    private final Button connectButton = new Button();

    public SettingsWidgetsPage() {
        getStyleClass().add("settings-scroll");
        setFitToWidth(true);
        setContent(createContent());
    }

    private VBox createContent() {
        Label heading = new Label("Widgets");
        heading.getStyleClass().add("settings-page-title");
        Label intro = new Label("Connect services and configure widget integrations.");
        intro.getStyleClass().add("settings-page-subtitle");

        SettingsSection clockSection = createClockSection();
        SettingsSection spotifySection = new SettingsSection("Spotify");
        Label explanation = new Label(
                "Connect through Spotify’s secure sign-in page. Your Spotify password "
                        + "is never entered into or stored by this app."
        );
        explanation.getStyleClass().add("settings-help");
        explanation.setWrapText(true);

        Label clientIdLabel = new Label("Spotify Client ID");
        clientIdLabel.getStyleClass().add("settings-field-label");
        clientId.setPromptText("Paste your Client ID");
        clientId.setText(AppSettings.getInstance().get("spotify.clientId", ""));
        clientId.textProperty().addListener((observable, oldValue, newValue) ->
                AppSettings.getInstance().set("spotify.clientId", newValue.trim()));

        Label callbackLabel = new Label("Redirect URI to add in Spotify Developer Dashboard");
        callbackLabel.getStyleClass().add("settings-field-label");
        TextField callback = new TextField(SpotifyAuthService.REDIRECT_URI);
        callback.setEditable(false);
        callback.getStyleClass().add("readonly-field");

        connectionStatus.getStyleClass().add("spotify-connection-status");
        connectButton.getStyleClass().add("settings-primary-button");
        connectButton.setOnAction(e -> {
            if (spotify.isConnected()) {
                spotify.disconnect();
                refreshConnectionState();
            } else {
                connect();
            }
        });
        refreshConnectionState();

        HBox connectionRow = new HBox(12, connectButton, connectionStatus);
        connectionRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        spotifySection.add(explanation);
        spotifySection.add(clientIdLabel);
        spotifySection.add(clientId);
        spotifySection.add(callbackLabel);
        spotifySection.add(callback);
        spotifySection.add(connectionRow);

        SettingsSection weatherSection = createWeatherSection();
        VBox content = new VBox(18, heading, intro, clockSection, spotifySection, weatherSection);
        content.setPadding(new Insets(38));
        return content;
    }

    private SettingsSection createClockSection() {
        AppSettings settings = AppSettings.getInstance();
        SettingsSection section = new SettingsSection("Clock");
        CheckBox digital = new CheckBox("Use a digital clock");
        digital.setSelected(settings.getBoolean("clock.digital", false));
        digital.setOnAction(e -> settings.setBoolean("clock.digital", digital.isSelected()));

        CheckBox use24Hour = new CheckBox("Use 24-hour time in digital mode");
        use24Hour.setSelected(settings.getBoolean("clock.use24Hour", true));
        use24Hour.setOnAction(e ->
                settings.setBoolean("clock.use24Hour", use24Hour.isSelected()));
        section.add(digital);
        section.add(use24Hour);
        return section;
    }

    private SettingsSection createWeatherSection() {
        AppSettings settings = AppSettings.getInstance();
        SettingsSection section = new SettingsSection("Weather");
        Label help = new Label(
                "Enter the city or locality whose current weather should appear on the dashboard."
        );
        help.getStyleClass().add("settings-help");
        help.setWrapText(true);
        TextField location = new TextField(settings.get("weather.location", "Stockholm"));
        location.setPromptText("City or locality");
        location.textProperty().addListener((observable, oldValue, newValue) ->
                settings.set("weather.location", newValue.trim()));
        section.add(help);
        section.add(location);
        return section;
    }

    private void connect() {
        String configuredClientId = clientId.getText().trim();
        if (configuredClientId.isBlank()) {
            connectionStatus.setText("Enter a Client ID first.");
            return;
        }

        connectButton.setDisable(true);
        connectionStatus.setText("Preparing sign-in…");
        spotify.connect(
                configuredClientId,
                message -> Platform.runLater(() -> connectionStatus.setText(message)),
                () -> Platform.runLater(this::refreshConnectionState)
        ).whenComplete((ignored, error) -> Platform.runLater(() -> {
            connectButton.setDisable(false);
            if (error != null) {
                connectionStatus.setText(messageOf(error));
            }
        }));
    }

    private void refreshConnectionState() {
        boolean connected = spotify.isConnected();
        connectButton.setText(connected ? "Disconnect Spotify" : "Connect Spotify");
        connectionStatus.setText(connected ? "Connected" : "Not connected");
    }

    private static String messageOf(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? "Spotify connection failed." : cause.getMessage();
    }
}
