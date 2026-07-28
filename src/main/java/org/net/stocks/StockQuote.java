package org.net.stocks;

import java.time.Instant;

public record StockQuote(
        String symbol,
        String name,
        String currency,
        double price,
        double previousClose,
        double percentChange,
        Instant updatedAt,
        boolean cached
) {
    public StockQuote asCached() {
        return new StockQuote(
                symbol, name, currency, price, previousClose,
                percentChange, updatedAt, true
        );
    }
}
