package org.net.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class WeatherService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WeatherForecast getForecast(String searchLocation)
            throws IOException, InterruptedException {
        JsonNode location = geocode(searchLocation);
        double latitude = location.path("latitude").asDouble();
        double longitude = location.path("longitude").asDouble();
        String name = location.path("name").asText(searchLocation);
        String country = location.path("country").asText("");
        String displayLocation = country.isBlank() ? name : name + ", " + country;

        String forecastUrl = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&hourly=temperature_2m,weather_code,precipitation_probability"
                + "&forecast_days=2"
                + "&timezone=auto";
        JsonNode root = getJson(forecastUrl);
        JsonNode hourly = root.path("hourly");
        JsonNode times = hourly.path("time");
        JsonNode temperatures = hourly.path("temperature_2m");
        JsonNode codes = hourly.path("weather_code");
        JsonNode precipitation = hourly.path("precipitation_probability");

        ZoneId zone;
        try {
            zone = ZoneId.of(root.path("timezone").asText(ZoneId.systemDefault().getId()));
        } catch (Exception exception) {
            zone = ZoneId.systemDefault();
        }
        LocalDateTime currentHour = LocalDateTime.now(zone).truncatedTo(ChronoUnit.HOURS);
        int start = 0;
        for (int i = 0; i < times.size(); i++) {
            LocalDateTime time = LocalDateTime.parse(times.get(i).asText());
            if (!time.isBefore(currentHour)) {
                start = i;
                break;
            }
        }

        List<WeatherHour> result = new ArrayList<>();
        int end = Math.min(times.size(), start + 5);
        for (int i = start; i < end; i++) {
            result.add(new WeatherHour(
                    LocalDateTime.parse(times.get(i).asText()),
                    temperatures.get(i).asDouble(),
                    codes.get(i).asInt(),
                    precipitation.get(i).asInt(0)
            ));
        }
        if (result.isEmpty()) {
            throw new IOException("No hourly forecast was returned.");
        }
        return new WeatherForecast(displayLocation, List.copyOf(result));
    }

    private JsonNode geocode(String location) throws IOException, InterruptedException {
        String url = "https://geocoding-api.open-meteo.com/v1/search"
                + "?name=" + URLEncoder.encode(location, StandardCharsets.UTF_8)
                + "&count=1&language=en&format=json";
        JsonNode results = getJson(url).path("results");
        if (!results.isArray() || results.isEmpty()) {
            throw new IOException("Location not found: " + location);
        }
        return results.get(0);
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "Kjellberius-Dashboard/1.0")
                .GET()
                .build();
        HttpResponse<String> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Weather service returned " + response.statusCode() + ".");
        }
        return JSON.readTree(response.body());
    }
}
