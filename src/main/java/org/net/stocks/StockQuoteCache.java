package org.net.stocks;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.net.settings.AppSettings;

public final class StockQuoteCache {
    private static final String PREFIX = "stocks.cache.v2.";

    public void save(StockQuote quote) {
        String key = key(quote.symbol());
        AppSettings settings = AppSettings.getInstance();
        Map<String, String> values = new LinkedHashMap<>();
        values.put(key + ".symbol", quote.symbol());
        values.put(key + ".name", quote.name());
        values.put(key + ".currency", quote.currency());
        values.put(key + ".price", Double.toString(quote.price()));
        values.put(key + ".previousClose", Double.toString(quote.previousClose()));
        values.put(key + ".percentChange", Double.toString(quote.percentChange()));
        values.put(key + ".updatedAt", quote.updatedAt().toString());
        settings.setAll(values);
    }

    public Optional<StockQuote> load(String symbol) {
        String key = key(symbol);
        AppSettings settings = AppSettings.getInstance();
        String storedSymbol = settings.get(key + ".symbol", "");
        if (storedSymbol.isBlank()) return Optional.empty();
        try {
            return Optional.of(new StockQuote(
                    storedSymbol,
                    settings.get(key + ".name", storedSymbol),
                    settings.get(key + ".currency", "SEK"),
                    Double.parseDouble(settings.get(key + ".price", "")),
                    Double.parseDouble(settings.get(key + ".previousClose", "")),
                    Double.parseDouble(settings.get(key + ".percentChange", "")),
                    Instant.parse(settings.get(key + ".updatedAt", "")),
                    true
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static String key(String symbol) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(symbol.getBytes(StandardCharsets.UTF_8));
        return PREFIX + encoded;
    }
}
