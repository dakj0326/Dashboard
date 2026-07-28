package org.net.ui.widgets.widget;

import java.awt.Desktop;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.net.news.NewsArticle;
import org.net.news.NewsService;
import org.net.system.HealthSeverity;
import org.net.system.SystemHealthService;
import org.net.ui.widgets.BaseWidget;
import org.net.ui.widgets.WidgetID;

public final class NewsWidget extends BaseWidget {
    private static final double PREFERRED_WIDTH = 430;
    private static final long REFRESH_MINUTES = 5;
    private final NewsService newsService = new NewsService();
    private final SystemHealthService health = SystemHealthService.getInstance();
    private final NewsBackground background = new NewsBackground();
    private final StackPane viewport = new StackPane();
    private final VBox articleView = new VBox(8);
    private final PauseTransition rotationDelay = new PauseTransition(javafx.util.Duration.seconds(16));
    private final ScheduledExecutorService poller;
    private final AtomicBoolean fetching = new AtomicBoolean();
    private List<NewsArticle> articles = List.of();
    private int articleIndex;
    private boolean enabled = true;
    private boolean dashboardVisible;

    public NewsWidget() {
        super("Latest news", new StackPane(), WidgetID.NEWS, PREFERRED_WIDTH);
        StackPane root = (StackPane) getCenter();
        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());
        installBackgroundLayer(background);
        viewport.getStyleClass().add("news-viewport");
        articleView.getStyleClass().add("news-article");
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
        viewport.getChildren().add(articleView);
        root.getChildren().add(viewport);
        showMessage("Open Dashboard to load the latest news.");

        rotationDelay.setOnFinished(event -> rotate());
        poller = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "news-widget-poller");
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
        if (active) {
            refreshIfActive();
            scheduleRotation();
        } else {
            rotationDelay.stop();
        }
    }

    private void refreshIfActive() {
        if (!enabled || !dashboardVisible || !fetching.compareAndSet(false, true)) return;
        try {
            NewsService.FetchResult result = newsService.fetchLatest();
            if (result.successfulSources() == 0) {
                health.report("api.news", HealthSeverity.WARNING);
                Platform.runLater(() -> {
                    if (articles.isEmpty()) showMessage("News are temporarily unavailable.");
                });
                return;
            }
            health.clear("api.news");
            Platform.runLater(() -> applyArticles(result.articles()));
        } finally {
            fetching.set(false);
        }
    }

    private void applyArticles(List<NewsArticle> latest) {
        String currentTitle = articles.isEmpty() ? null : articles.get(articleIndex).title();
        articles = latest;
        if (articles.isEmpty()) {
            articleIndex = 0;
            showMessage("No current news from the last 24 hours.");
            rotationDelay.stop();
            return;
        }
        articleIndex = findArticle(currentTitle);
        showArticle(articles.get(articleIndex));
        scheduleRotation();
    }

    private int findArticle(String title) {
        if (title != null) {
            for (int i = 0; i < articles.size(); i++) {
                if (articles.get(i).title().equals(title)) return i;
            }
        }
        return 0;
    }

    private void rotate() {
        if (!enabled || !dashboardVisible || articles.size() < 2) return;
        double distance = Math.max(getWidth(), PREFERRED_WIDTH);
        TranslateTransition slideOut = new TranslateTransition(
                javafx.util.Duration.millis(330),
                articleView
        );
        slideOut.setToX(-distance);
        slideOut.setInterpolator(Interpolator.EASE_BOTH);
        slideOut.setOnFinished(event -> {
            articleIndex = (articleIndex + 1) % articles.size();
            showArticle(articles.get(articleIndex));
            articleView.setTranslateX(distance);
        });

        TranslateTransition slideIn = new TranslateTransition(
                javafx.util.Duration.millis(380),
                articleView
        );
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_BOTH);
        SequentialTransition transition = new SequentialTransition(slideOut, slideIn);
        transition.setOnFinished(event -> scheduleRotation());
        transition.play();
    }

    private void scheduleRotation() {
        rotationDelay.stop();
        if (enabled && dashboardVisible && articles.size() > 1) {
            rotationDelay.playFromStart();
        }
    }

    private void showArticle(NewsArticle article) {
        Label source = new Label(article.source().toUpperCase());
        source.getStyleClass().add("news-source");
        Label age = new Label(age(article.publishedAt()));
        age.getStyleClass().add("news-age");
        HBox metadata = new HBox(8, source, age);
        metadata.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(article.title());
        title.getStyleClass().add("news-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);

        Label summary = new Label(article.summary());
        summary.getStyleClass().add("news-summary");
        summary.setWrapText(true);
        summary.setMaxWidth(Double.MAX_VALUE);
        summary.setVisible(!article.summary().isBlank());
        summary.setManaged(!article.summary().isBlank());

        Label open = new Label("Open article  \u2197");
        open.getStyleClass().add("news-open");
        HBox footer = new HBox(open);
        footer.setAlignment(Pos.CENTER_RIGHT);
        VBox.setVgrow(summary, Priority.ALWAYS);

        articleView.getChildren().setAll(metadata, title, summary, footer);
        articleView.setOnMouseClicked(event -> openArticle(event, article));
    }

    private void showMessage(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("news-message");
        label.setWrapText(true);
        articleView.getChildren().setAll(label);
        articleView.setOnMouseClicked(null);
    }

    private static String age(Instant publishedAt) {
        long minutes = Math.max(0, Duration.between(publishedAt, Instant.now()).toMinutes());
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        return (minutes / 60) + " h ago";
    }

    private static void openArticle(MouseEvent event, NewsArticle article) {
        if (!Desktop.isDesktopSupported()) return;
        Thread thread = new Thread(() -> {
            try {
                Desktop.getDesktop().browse(article.link());
            } catch (Exception ignored) {
                // The article remains visible if the operating system cannot open links.
            }
        }, "news-link-opener");
        thread.setDaemon(true);
        thread.start();
    }
}
