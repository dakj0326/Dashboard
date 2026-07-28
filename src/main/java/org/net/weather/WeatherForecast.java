package org.net.weather;

import java.util.List;

public record WeatherForecast(
        String location,
        List<WeatherHour> hours
) {}
