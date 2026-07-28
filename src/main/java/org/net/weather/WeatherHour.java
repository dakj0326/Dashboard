package org.net.weather;

import java.time.LocalDateTime;

public record WeatherHour(
        LocalDateTime time,
        double temperature,
        int weatherCode,
        int precipitationProbability
) {}
