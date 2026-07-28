package org.net.stocks;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.net.settings.AppSettings;

public final class StockWatchlist {
    public static final String SETTINGS_KEY = "stocks.watchlist";
    public static final int MAX_STOCKS = 12;

    private StockWatchlist() {}

    public static List<String> load() {
        String value = AppSettings.getInstance().get(SETTINGS_KEY, "");
        if (value.isBlank()) return List.of();
        Set<String> unique = new LinkedHashSet<>();
        for (String symbol : value.split(",")) {
            String normalized = symbol.strip().toUpperCase();
            if (!normalized.isBlank() && normalized.endsWith(".ST")) {
                unique.add(normalized);
            }
        }
        return new ArrayList<>(unique);
    }

    public static boolean add(String symbol) {
        List<String> symbols = new ArrayList<>(load());
        String normalized = symbol.strip().toUpperCase();
        if (symbols.contains(normalized)) return true;
        if (symbols.size() >= MAX_STOCKS) return false;
        symbols.add(normalized);
        save(symbols);
        return true;
    }

    public static void remove(String symbol) {
        List<String> symbols = new ArrayList<>(load());
        symbols.remove(symbol.strip().toUpperCase());
        save(symbols);
    }

    private static void save(List<String> symbols) {
        AppSettings.getInstance().set(
                SETTINGS_KEY,
                symbols.stream().collect(Collectors.joining(","))
        );
    }
}
