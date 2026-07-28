package org.net.ui.pages;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.net.system.SystemStatusService;
import org.net.system.SystemStatusSnapshot;
import org.net.system.HealthSeverity;
import org.net.system.HealthIssue;
import org.net.system.SystemHealthService;
import org.net.ui.widgets.WidgetManager;
import org.net.settings.AppSettings;

public class DashboardPage extends BorderPane {
    private final Label activeWidgetValue = new Label("0");
    private final Label registeredWidgetValue = new Label("0");
    private final Label hiddenWidgetValue = new Label("0");
    private final Label greeting = new Label();
    private final SystemStatusService systemStatus = new SystemStatusService();
    private final SystemHealthService health = SystemHealthService.getInstance();
    private final Label cpuValue = new Label("—");
    private final Label memoryValue = new Label("—");
    private final Label networkValue = new Label("—");
    private final DoubleProperty cpuProgress = new SimpleDoubleProperty();
    private final DoubleProperty memoryProgress = new SimpleDoubleProperty();
    private final VBox issueRows = new VBox(7);

    public DashboardPage() {
        getStyleClass().add("dashboard-page");
        createLayout();
    }

    private void createLayout() {
        VBox hero = new VBox(6);
        hero.getStyleClass().add("dashboard-hero");
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
        Label eyebrow = new Label(LocalDate.now().format(dateFormat).toUpperCase(Locale.ENGLISH));
        eyebrow.getStyleClass().add("hero-eyebrow");
        greeting.getStyleClass().add("hero-title");
        updateGreeting();
        Label copy = new Label("Your workspace and active widgets at a glance.");
        copy.getStyleClass().add("hero-copy");
        hero.getChildren().addAll(eyebrow, greeting, copy);

        Timeline greetingTimer = new Timeline(
                new KeyFrame(Duration.ZERO, e -> updateGreeting()),
                new KeyFrame(Duration.minutes(1))
        );
        greetingTimer.setCycleCount(Timeline.INDEFINITE);
        greetingTimer.play();

        HBox stats = new HBox(14,
                createStat("ACTIVE WIDGETS", activeWidgetValue, "Visible now"),
                createStat("REGISTERED", registeredWidgetValue, "In your workspace"),
                createStat("HIDDEN", hiddenWidgetValue, "Currently inactive")
        );
        for (Node node : stats.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        Label systemTitle = new Label("System status");
        systemTitle.getStyleClass().add("section-title");
        Label systemSubtitle = new Label("Live performance of this device");
        systemSubtitle.getStyleClass().add("section-subtitle");
        VBox systemHeading = new VBox(2, systemTitle, systemSubtitle);

        HBox systemCards = new HBox(12,
                createSystemCard("CPU", cpuValue, cpuProgress),
                createSystemCard("MEMORY", memoryValue, memoryProgress),
                createSystemCard("NETWORK", networkValue, null)
        );
        for (Node node : systemCards.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        VBox issuesPanel = createIssuesPanel();
        VBox content = new VBox(20, hero, stats, systemHeading, systemCards, issuesPanel);
        content.getStyleClass().add("dashboard-content");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("overview-scroll");
        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);

        health.getIssues().addListener(
                (javafx.collections.ListChangeListener<HealthIssue>) change ->
                        updateIssueRows()
        );
        updateIssueRows();

        Timeline systemTimer = new Timeline(
                new KeyFrame(Duration.ZERO, e -> updateSystemStatus()),
                new KeyFrame(Duration.seconds(2))
        );
        systemTimer.setCycleCount(Timeline.INDEFINITE);
        systemTimer.play();
    }

    private void updateGreeting() {
        int hour = LocalTime.now().getHour();
        String salutation;
        if (hour >= 5 && hour < 12) {
            salutation = "Good morning";
        } else if (hour < 18) {
            salutation = "Good afternoon";
        } else if (hour < 22) {
            salutation = "Good evening";
        } else {
            salutation = "Good night";
        }
        String name = AppSettings.getInstance().get("profile.name", "David").trim();
        greeting.setText(name.isBlank()
                ? salutation + "."
                : salutation + ", " + name + ".");
    }

    private VBox createStat(String label, Label valueNode, String detail) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("stat-label");
        valueNode.getStyleClass().add("stat-value");
        Label detailNode = new Label(detail);
        detailNode.getStyleClass().add("stat-detail");
        VBox card = new VBox(7, labelNode, valueNode, detailNode);
        card.getStyleClass().add("stat-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox createSystemCard(
            String label,
            Label value,
            DoubleProperty progressValue
    ) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("system-card-label");
        value.getStyleClass().add("system-card-value");
        VBox card = new VBox(7, labelNode, value);
        if (progressValue != null) {
            StackPane track = new StackPane();
            Region fill = new Region();
            track.getStyleClass().add("system-meter-track");
            fill.getStyleClass().add("system-meter-fill");
            track.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            track.setMinHeight(5);
            track.setMaxHeight(5);
            fill.setMinHeight(5);
            fill.setMaxHeight(5);
            fill.prefWidthProperty().bind(track.widthProperty().multiply(progressValue));
            fill.maxWidthProperty().bind(track.widthProperty().multiply(progressValue));
            track.getChildren().add(fill);
            card.getChildren().add(track);
        }
        card.getStyleClass().add("system-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void updateSystemStatus() {
        SystemStatusSnapshot snapshot = systemStatus.read();
        cpuProgress.set(snapshot.cpuUsage());
        memoryProgress.set(ratio(snapshot.memoryUsed(), snapshot.memoryTotal()));
        cpuValue.setText(Math.round(snapshot.cpuUsage() * 100) + "%");
        memoryValue.setText(formatBytes(snapshot.memoryUsed()) + " / "
                + formatBytes(snapshot.memoryTotal()));
        networkValue.setText(snapshot.networkAvailable() ? "Connected" : "Offline");
        networkValue.getStyleClass().removeAll("system-online", "system-offline");
        networkValue.getStyleClass().add(
                snapshot.networkAvailable() ? "system-online" : "system-offline"
        );

        if (snapshot.cpuUsage() >= 0.98) {
            health.report("system.cpu", HealthSeverity.CRITICAL);
        } else if (snapshot.cpuUsage() >= 0.90) {
            health.report("system.cpu", HealthSeverity.WARNING);
        } else {
            health.clear("system.cpu");
        }

        double memoryUsage = ratio(snapshot.memoryUsed(), snapshot.memoryTotal());
        if (memoryUsage >= 0.97) {
            health.report("system.memory", HealthSeverity.CRITICAL);
        } else if (memoryUsage >= 0.90) {
            health.report("system.memory", HealthSeverity.WARNING);
        } else {
            health.clear("system.memory");
        }

        if (snapshot.networkAvailable()) {
            health.clear("system.network");
        } else {
            health.report("system.network", HealthSeverity.CRITICAL);
        }
    }

    private VBox createIssuesPanel() {
        Label title = new Label("Current issues");
        title.getStyleClass().add("issues-title");
        Label subtitle = new Label("Components that currently require attention");
        subtitle.getStyleClass().add("issues-subtitle");
        VBox heading = new VBox(2, title, subtitle);
        VBox panel = new VBox(11, heading, issueRows);
        panel.getStyleClass().add("issues-panel");
        return panel;
    }

    private void updateIssueRows() {
        issueRows.getChildren().clear();
        if (health.getIssues().isEmpty()) {
            Label clear = new Label("●  No active issues");
            clear.getStyleClass().add("issues-clear");
            issueRows.getChildren().add(clear);
            return;
        }

        for (HealthIssue issue : health.getIssues()) {
            Label indicator = new Label("●");
            indicator.getStyleClass().add(
                    issue.severity() == HealthSeverity.CRITICAL
                            ? "issue-critical"
                            : "issue-warning"
            );
            Label message = new Label(issueMessage(issue));
            message.getStyleClass().add("issue-message");
            HBox row = new HBox(9, indicator, message);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.getStyleClass().add("issue-row");
            issueRows.getChildren().add(row);
        }
    }

    private static String issueMessage(HealthIssue issue) {
        return switch (issue.source()) {
            case "system.cpu" -> issue.severity() == HealthSeverity.CRITICAL
                    ? "Very high CPU usage"
                    : "High CPU usage";
            case "system.memory" -> issue.severity() == HealthSeverity.CRITICAL
                    ? "Critically high memory usage"
                    : "High memory usage";
            case "system.network" -> "Network connection unavailable";
            case "api.spotify" -> "Spotify service unavailable";
            case "api.weather" -> "Weather service unavailable";
            case "api.news" -> "News services unavailable";
            case "api.stocks" -> "Stock data unavailable";
            default -> "A system component requires attention";
        };
    }

    private static double ratio(long used, long total) {
        return total <= 0 ? 0 : Math.min(1, (double) used / total);
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 GB";
        return String.format(Locale.ENGLISH, "%.1f GB", bytes / 1_073_741_824.0);
    }

    public void update(WidgetManager manager) {
        updateGreeting();
        long active = manager.getVisibleWidgetCount();
        int registered = manager.getRegisteredWidgetCount();
        activeWidgetValue.setText(Long.toString(active));
        registeredWidgetValue.setText(Integer.toString(registered));
        hiddenWidgetValue.setText(Long.toString(registered - active));
    }
}
