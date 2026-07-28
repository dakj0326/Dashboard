package org.net.ui.widgets.widget;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.net.settings.AppSettings;
import org.net.system.HealthSeverity;
import org.net.system.SystemHealthService;
import org.net.ui.widgets.BaseWidget;
import org.net.ui.widgets.WidgetID;
import org.net.weather.WeatherForecast;
import org.net.weather.WeatherHour;
import org.net.weather.WeatherService;

public class WeatherWidget extends BaseWidget {
    private static final double PREFERRED_WIDTH = 350;
    private static final long REFRESH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15);
    private final WeatherService weatherService = new WeatherService();
    private final SystemHealthService health = SystemHealthService.getInstance();
    private final VBox content = new VBox(8);
    private final WeatherAnimation animation = new WeatherAnimation();
    private final ScheduledExecutorService poller;
    private volatile boolean active = true;
    private volatile boolean dashboardVisible;
    private volatile long lastFetch;
    private volatile String lastLocation = "";

    public WeatherWidget() {
        super("Weather", new VBox(), WidgetID.WEATHER, PREFERRED_WIDTH);
        VBox root = (VBox) getCenter();
        animation.widthProperty().bind(widthProperty());
        animation.heightProperty().bind(heightProperty());
        installBackgroundLayer(animation);
        root.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        showLoading("Loading local forecast…");

        poller = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "weather-widget-poller");
            thread.setDaemon(true);
            return thread;
        });
        poller.scheduleWithFixedDelay(this::refreshIfNeeded, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        active = visible;
        animation.setActive(visible && dashboardVisible);
        if (!visible) {
            health.clear("api.weather");
        }
        if (visible && poller != null) {
            poller.execute(this::refreshIfNeeded);
        }
    }

    @Override
    public void onPageVisibilityChanged(boolean visible) {
        dashboardVisible = visible;
        animation.setActive(active && dashboardVisible);
    }

    private void refreshIfNeeded() {
        if (!active) return;
        String location = AppSettings.getInstance().get("weather.location", "Stockholm").trim();
        if (location.isBlank()) {
            health.report("api.weather", HealthSeverity.WARNING);
            Platform.runLater(() -> showLoading("Choose a location in Preferences → Widgets."));
            return;
        }
        long now = System.currentTimeMillis();
        if (location.equals(lastLocation) && now - lastFetch < REFRESH_INTERVAL_MS) {
            return;
        }

        lastLocation = location;
        lastFetch = now;
        try {
            WeatherForecast forecast = weatherService.getForecast(location);
            health.clear("api.weather");
            Platform.runLater(() -> display(forecast));
        } catch (Exception exception) {
            if (active) {
                health.report("api.weather", HealthSeverity.WARNING);
            }
            Platform.runLater(() -> showLoading(messageOf(exception)));
        }
    }

    private void display(WeatherForecast forecast) {
        content.getChildren().clear();
        WeatherHour current = forecast.hours().get(0);
        animation.setWeatherCode(current.weatherCode());

        Label location = new Label(forecast.location());
        location.getStyleClass().add("weather-location");
        Label temperature = new Label(Math.round(current.temperature()) + "°");
        temperature.getStyleClass().add("weather-temperature");
        Label currentIcon = new Label(icon(current.weatherCode()));
        currentIcon.getStyleClass().add("weather-current-icon");
        HBox currentWeather = new HBox(9, temperature, currentIcon);
        currentWeather.setAlignment(Pos.CENTER_LEFT);
        VBox currentConditions = new VBox(3, location, currentWeather);
        currentConditions.setAlignment(Pos.CENTER_LEFT);
        currentConditions.setMinWidth(90);

        HBox upcoming = new HBox(5);
        upcoming.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i < forecast.hours().size(); i++) {
            upcoming.getChildren().add(hourCard(forecast.hours().get(i)));
        }
        HBox weatherRow = new HBox(6, currentConditions, upcoming);
        weatherRow.setAlignment(Pos.CENTER_LEFT);
        Label attribution = new Label("Data: Open-Meteo");
        attribution.getStyleClass().add("weather-attribution");
        content.getChildren().addAll(weatherRow, attribution);
    }

    private VBox hourCard(WeatherHour hour) {
        Label time = new Label(hour.time().format(DateTimeFormatter.ofPattern("HH:mm")));
        Label symbol = new Label(icon(hour.weatherCode()));
        Label temperature = new Label(Math.round(hour.temperature()) + "°");
        Label rain = new Label(hour.precipitationProbability() + "%");
        time.getStyleClass().add("weather-hour-time");
        symbol.getStyleClass().add("weather-hour-icon");
        temperature.getStyleClass().add("weather-hour-temperature");
        rain.getStyleClass().add("weather-hour-rain");
        VBox card = new VBox(4, time, symbol, temperature, rain);
        card.getStyleClass().add("weather-hour");
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private void showLoading(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("weather-message");
        label.setWrapText(true);
        content.getChildren().setAll(label);
    }

    private static String icon(int code) {
        if (code == 0) return "☀";
        if (code <= 2) return "◐";
        if (code == 3) return "☁";
        if (code == 45 || code == 48) return "≋";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "☂";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "❄";
        if (code >= 95) return "⚡";
        return "·";
    }

    private static String messageOf(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? "Weather is unavailable." : cause.getMessage();
    }
}
