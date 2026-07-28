package org.net.ui.widgets.widget;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.net.stocks.StockQuote;
import org.net.stocks.StockQuoteCache;
import org.net.stocks.StockWatchlist;
import org.net.stocks.YahooStockService;
import org.net.system.HealthSeverity;
import org.net.system.SystemHealthService;
import org.net.ui.widgets.BaseWidget;
import org.net.ui.widgets.WidgetID;

public final class StockWidget extends BaseWidget {
    private static final double PREFERRED_WIDTH = 410;
    private static final long REFRESH_MINUTES = 5;
    private final YahooStockService service = new YahooStockService();
    private final StockQuoteCache cache = new StockQuoteCache();
    private final SystemHealthService health = SystemHealthService.getInstance();
    private final VBox content = new VBox(7);
    private final StockBackground background = new StockBackground();
    private final AtomicBoolean fetching = new AtomicBoolean();
    private final ScheduledExecutorService poller;
    private boolean enabled = true;
    private boolean dashboardVisible;

    public StockWidget() {
        super("Stocks", new VBox(), WidgetID.STOCKS, PREFERRED_WIDTH);
        VBox root = (VBox) getCenter();
        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());
        installBackgroundLayer(background);
        root.getChildren().add(content);
        showMessage("Open Dashboard to load market data.");

        poller = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "stock-widget-poller");
            thread.setDaemon(true);
            return thread;
        });
        poller.scheduleWithFixedDelay(
                this::refreshIfActive,
                0,
                REFRESH_MINUTES,
                TimeUnit.MINUTES
        );
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        enabled = visible;
        updateActivity();
    }

    @Override
    public void onPageVisibilityChanged(boolean visible) {
        dashboardVisible = visible;
        updateActivity();
    }

    private void updateActivity() {
        boolean active = enabled && dashboardVisible;
        background.setActive(active);
        if (active) refreshIfActive();
    }

    private void refreshIfActive() {
        if (!enabled || !dashboardVisible || !fetching.compareAndSet(false, true)) return;
        try {
            StockQuote index = fetchWithCache(YahooStockService.MARKET_INDEX_SYMBOL);
            List<String> watchlist = StockWatchlist.load();
            List<StockQuote> watched = new ArrayList<>();
            for (String symbol : watchlist) {
                StockQuote quote = fetchWithCache(symbol);
                if (quote != null) watched.add(quote);
            }

            boolean usable = index != null && (watchlist.isEmpty() || !watched.isEmpty());
            if (usable) health.clear("api.stocks");
            else health.report("api.stocks", HealthSeverity.WARNING);
            Platform.runLater(() -> display(index, watched, watchlist.isEmpty()));
        } finally {
            fetching.set(false);
        }
    }

    private StockQuote fetchWithCache(String symbol) {
        try {
            StockQuote quote = service.fetchQuote(symbol);
            cache.save(quote);
            return quote;
        } catch (Exception exception) {
            return cache.load(symbol).map(StockQuote::asCached).orElse(null);
        }
    }

    private void display(StockQuote index, List<StockQuote> watched, boolean emptyWatchlist) {
        content.getChildren().clear();
        if (index != null) {
            content.getChildren().add(quoteRow("OMXSPI", index, changeClass(index)));
        }
        if (emptyWatchlist) {
            Label help = new Label("Add Stockholm shares in Preferences \u2192 Widgets.");
            help.getStyleClass().add("stock-message");
            content.getChildren().add(help);
        } else if (watched.size() == 1) {
            StockQuote only = watched.get(0);
            content.getChildren().add(quoteRow("WATCHED", only, changeClass(only)));
        } else if (!watched.isEmpty()) {
            StockQuote best = watched.stream()
                    .max(Comparator.comparingDouble(StockQuote::percentChange))
                    .orElseThrow();
            StockQuote worst = watched.stream()
                    .min(Comparator.comparingDouble(StockQuote::percentChange))
                    .orElseThrow();
            content.getChildren().add(quoteRow("BEST", best, changeClass(best)));
            content.getChildren().add(quoteRow("WORST", worst, changeClass(worst)));
        } else {
            showMessage("Stock data are temporarily unavailable.");
            return;
        }

        StockQuote newest = watched.stream()
                .max(Comparator.comparing(StockQuote::updatedAt))
                .orElse(index);
        if (newest != null) {
            Label updated = new Label(
                    (newest.cached() ? "Cached \u00b7 " : "Delayed \u00b7 ")
                            + newest.updatedAt().atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
            );
            updated.getStyleClass().add("stock-updated");
            content.getChildren().add(updated);
        }
    }

    private HBox quoteRow(String role, StockQuote quote, String changeClass) {
        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("stock-role");
        roleLabel.setMinWidth(54);

        VBox identity = new VBox(0);
        Label symbol = new Label(quote.symbol().replace(".ST", ""));
        symbol.getStyleClass().add("stock-symbol");
        Label name = new Label(quote.name());
        name.getStyleClass().add("stock-name");
        name.setMaxWidth(150);
        identity.getChildren().addAll(symbol, name);
        HBox.setHgrow(identity, Priority.ALWAYS);

        Label price = new Label(String.format(
                Locale.US, "%,.2f %s", quote.price(), quote.currency()
        ));
        price.getStyleClass().add("stock-price");
        Label change = new Label(String.format(
                Locale.US, "%+.2f%%", quote.percentChange()
        ));
        change.getStyleClass().addAll("stock-change", changeClass);
        VBox values = new VBox(0, price, change);
        values.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(9, roleLabel, identity, values);
        row.getStyleClass().add("stock-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String changeClass(StockQuote quote) {
        if (quote.percentChange() > 0.0001) return "stock-positive";
        if (quote.percentChange() < -0.0001) return "stock-negative";
        return "stock-neutral";
    }

    private void showMessage(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("stock-message");
        label.setWrapText(true);
        content.getChildren().setAll(label);
    }
}
