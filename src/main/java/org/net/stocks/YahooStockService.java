package org.net.stocks;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class YahooStockService {
    public static final String MARKET_INDEX_SYMBOL = "^OMXSPI";
    private static final URI BASE = URI.create("https://query1.finance.yahoo.com/");
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<StockSearchResult> searchSwedishStocks(String query) throws IOException {
        if (query == null || query.isBlank()) return List.of();
        String path = "v1/finance/search?q=" + encode(query.strip())
                + "&quotesCount=10&newsCount=0";
        JsonNode root = getJson(path);
        List<StockSearchResult> results = new ArrayList<>();
        for (JsonNode item : root.path("quotes")) {
            String symbol = item.path("symbol").asText("");
            String exchange = item.path("exchange").asText("");
            String quoteType = item.path("quoteType").asText("");
            if (!"STO".equalsIgnoreCase(exchange)
                    || !"EQUITY".equalsIgnoreCase(quoteType)
                    || !symbol.endsWith(".ST")) {
                continue;
            }
            String name = item.path("shortname").asText(
                    item.path("longname").asText(symbol)
            );
            results.add(new StockSearchResult(symbol, name, "Stockholm"));
        }
        return results;
    }

    public StockQuote fetchQuote(String symbol) throws IOException {
        String path = "v8/finance/chart/" + encode(symbol)
                + "?range=1d&interval=1m";
        JsonNode result = getJson(path).path("chart").path("result").path(0);
        if (result.isMissingNode()) {
            throw new IOException("No quote returned for " + symbol);
        }
        JsonNode meta = result.path("meta");
        double price = requiredDouble(meta, "regularMarketPrice");
        double previousClose = meta.path("previousClose").isNumber()
                ? meta.path("previousClose").asDouble()
                : requiredDouble(meta, "chartPreviousClose");
        double percentChange = previousClose == 0
                ? 0
                : (price - previousClose) / previousClose * 100;
        long timestamp = meta.path("regularMarketTime").asLong(Instant.now().getEpochSecond());
        return new StockQuote(
                meta.path("symbol").asText(symbol),
                meta.path("shortName").asText(meta.path("longName").asText(symbol)),
                meta.path("currency").asText("SEK"),
                price,
                previousClose,
                percentChange,
                Instant.ofEpochSecond(timestamp),
                false
        );
    }

    private JsonNode getJson(String relativePath) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(BASE.resolve(relativePath))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 Kjellberius-Dashboard/1.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Stock service returned " + response.statusCode());
            }
            return mapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Stock request interrupted", exception);
        }
    }

    private static double requiredDouble(JsonNode node, String field) throws IOException {
        JsonNode value = node.path(field);
        if (!value.isNumber()) throw new IOException("Missing " + field);
        return value.asDouble();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
