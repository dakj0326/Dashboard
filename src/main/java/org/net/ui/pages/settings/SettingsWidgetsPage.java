package org.net.ui.pages.settings;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.net.settings.AppSettings;
import org.net.spotify.SpotifyAuthService;
import org.net.stocks.StockSearchResult;
import org.net.stocks.StockWatchlist;
import org.net.stocks.YahooStockService;

public class SettingsWidgetsPage extends ScrollPane {
    private final SpotifyAuthService spotify = SpotifyAuthService.getInstance();
    private final TextField clientId = new TextField();
    private final Label connectionStatus = new Label();
    private final Button connectButton = new Button();
    private final YahooStockService stocks = new YahooStockService();
    private final TextField stockSearch = new TextField();
    private final VBox stockSearchResults = new VBox(7);
    private final VBox stockWatchlist = new VBox(7);
    private final Label stockStatus = new Label();

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
        SettingsSection newsSection = createNewsSection();
        SettingsSection spotifySection = createSpotifySection();
        SettingsSection stocksSection = createStocksSection();
        SettingsSection weatherSection = createWeatherSection();
        VBox content = new VBox(
                18,
                heading,
                intro,
                clockSection,
                newsSection,
                spotifySection,
                stocksSection,
                weatherSection
        );
        content.setPadding(new Insets(38));
        return content;
    }

    private SettingsSection createNewsSection() {
        AppSettings settings = AppSettings.getInstance();
        SettingsSection section = new SettingsSection("News");
        Label help = new Label(
                "Current Swedish and world news rotate automatically and are refreshed in "
                        + "the background."
        );
        help.getStyleClass().add("settings-help");
        help.setWrapText(true);

        Label rotationLabel = new Label("Time per news item");
        rotationLabel.getStyleClass().add("settings-field-label");
        int savedSeconds = readIntSetting("news.rotationSeconds", 16, 5, 60);
        Slider rotation = new Slider(5, 60, savedSeconds);
        rotation.setBlockIncrement(1);
        rotation.setMajorTickUnit(5);
        rotation.setMinorTickCount(4);
        rotation.setSnapToTicks(true);
        rotation.setShowTickMarks(true);
        rotation.getStyleClass().add("news-rotation-slider");
        HBox.setHgrow(rotation, Priority.ALWAYS);
        Label seconds = new Label(savedSeconds + " s");
        seconds.getStyleClass().add("news-rotation-value");
        seconds.setMinWidth(38);
        rotation.valueProperty().addListener((observable, oldValue, newValue) -> {
            int value = (int) Math.round(newValue.doubleValue());
            seconds.setText(value + " s");
            settings.set("news.rotationSeconds", Integer.toString(value));
        });
        HBox rotationRow = new HBox(12, rotation, seconds);
        rotationRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox staticBackground = new CheckBox("Use a static news background");
        staticBackground.setSelected(settings.getBoolean("news.staticBackground", false));
        staticBackground.setOnAction(event -> settings.setBoolean(
                "news.staticBackground",
                staticBackground.isSelected()
        ));

        section.add(help);
        section.add(rotationLabel);
        section.add(rotationRow);
        section.add(staticBackground);
        return section;
    }

    private static int readIntSetting(String key, int defaultValue, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(AppSettings.getInstance().get(
                    key,
                    Integer.toString(defaultValue)
            ));
            return Math.max(minimum, Math.min(maximum, value));
        } catch (NumberFormatException exception) {
            AppSettings.getInstance().set(key, Integer.toString(defaultValue));
            return defaultValue;
        }
    }

    private SettingsSection createSpotifySection() {
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
        return spotifySection;
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

    private SettingsSection createStocksSection() {
        SettingsSection section = new SettingsSection("Stocks");
        Label help = new Label(
                "Search for Nasdaq Stockholm shares to monitor. The widget always includes "
                        + "OMXSPI and displays the best and worst watched share by today's "
                        + "percentage change. Quotes are delayed."
        );
        help.getStyleClass().add("settings-help");
        help.setWrapText(true);

        stockSearch.setPromptText("Search by company or ticker");
        HBox.setHgrow(stockSearch, Priority.ALWAYS);
        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("settings-primary-button");
        searchButton.setOnAction(event -> searchStocks(searchButton));
        stockSearch.setOnAction(event -> searchStocks(searchButton));
        HBox searchRow = new HBox(9, stockSearch, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        Label watchlistLabel = new Label("WATCHLIST");
        watchlistLabel.getStyleClass().add("settings-field-label");
        stockStatus.getStyleClass().add("settings-help");
        refreshStockWatchlist();

        section.add(help);
        section.add(searchRow);
        section.add(stockSearchResults);
        section.add(watchlistLabel);
        section.add(stockWatchlist);
        section.add(stockStatus);
        return section;
    }

    private void searchStocks(Button searchButton) {
        String query = stockSearch.getText().trim();
        if (query.isBlank()) {
            stockStatus.setText("Enter a company name or ticker.");
            return;
        }
        searchButton.setDisable(true);
        stockStatus.setText("Searching Stockholm shares\u2026");
        stockSearchResults.getChildren().clear();

        java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return stocks.searchSwedishStocks(query);
                    } catch (Exception exception) {
                        throw new java.util.concurrent.CompletionException(exception);
                    }
                })
                .whenComplete((results, error) -> Platform.runLater(() -> {
                    searchButton.setDisable(false);
                    if (error != null) {
                        stockStatus.setText("Stock search is temporarily unavailable.");
                        return;
                    }
                    displayStockSearchResults(results);
                }));
    }

    private void displayStockSearchResults(java.util.List<StockSearchResult> results) {
        stockSearchResults.getChildren().clear();
        if (results.isEmpty()) {
            stockStatus.setText("No Nasdaq Stockholm shares found.");
            return;
        }
        stockStatus.setText("");
        for (StockSearchResult result : results) {
            Label identity = new Label(result.symbol() + "  \u00b7  " + result.name());
            identity.getStyleClass().add("stock-setting-identity");
            identity.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(identity, Priority.ALWAYS);
            Button add = new Button(
                    StockWatchlist.load().contains(result.symbol()) ? "Added" : "Add"
            );
            add.getStyleClass().add("stock-setting-action");
            add.setDisable(StockWatchlist.load().contains(result.symbol()));
            add.setOnAction(event -> {
                if (StockWatchlist.add(result.symbol())) {
                    add.setText("Added");
                    add.setDisable(true);
                    stockStatus.setText("");
                    refreshStockWatchlist();
                } else {
                    stockStatus.setText(
                            "The watchlist supports up to " + StockWatchlist.MAX_STOCKS
                                    + " shares."
                    );
                }
            });
            HBox row = new HBox(9, identity, add);
            row.getStyleClass().add("stock-setting-row");
            row.setAlignment(Pos.CENTER_LEFT);
            stockSearchResults.getChildren().add(row);
        }
    }

    private void refreshStockWatchlist() {
        stockWatchlist.getChildren().clear();
        java.util.List<String> symbols = StockWatchlist.load();
        if (symbols.isEmpty()) {
            Label empty = new Label("No shares added yet.");
            empty.getStyleClass().add("settings-help");
            stockWatchlist.getChildren().add(empty);
            return;
        }
        for (String symbol : symbols) {
            Label symbolLabel = new Label(symbol);
            symbolLabel.getStyleClass().add("stock-setting-identity");
            symbolLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(symbolLabel, Priority.ALWAYS);
            Button remove = new Button("Remove");
            remove.getStyleClass().add("stock-setting-action");
            remove.setOnAction(event -> {
                StockWatchlist.remove(symbol);
                refreshStockWatchlist();
                if (!stockSearchResults.getChildren().isEmpty()) {
                    searchStocksFromCurrentResults();
                }
            });
            HBox row = new HBox(9, symbolLabel, remove);
            row.getStyleClass().add("stock-setting-row");
            row.setAlignment(Pos.CENTER_LEFT);
            stockWatchlist.getChildren().add(row);
        }
    }

    private void searchStocksFromCurrentResults() {
        stockSearchResults.getChildren().clear();
        stockStatus.setText("Search again to refresh the results.");
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
