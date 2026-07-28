package org.net.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.net.ui.pages.DashboardPage;
import org.net.ui.pages.Page;
import org.net.ui.pages.PageButton;
import org.net.ui.pages.WidgetsPage;
import org.net.ui.pages.settings.SettingsPage;
import org.net.ui.pages.settings.ThemeManager;
import org.net.ui.pages.settings.CornerStyleManager;
import org.net.ui.widgets.WidgetEntry;
import org.net.ui.widgets.WidgetID;
import org.net.ui.widgets.WidgetManager;
import org.net.ui.widgets.widget.ClockWidget;
import org.net.ui.widgets.widget.SpotifyWidget;
import org.net.ui.widgets.widget.WeatherWidget;
import org.net.ui.widgets.widget.NewsWidget;
import org.net.ui.widgets.widget.StockWidget;
import org.net.ui.widgets.widget.TimerWidget;
import org.net.settings.AppSettings;
import org.net.system.HealthSeverity;
import org.net.system.SystemHealthService;
import org.net.AppVersion;
import org.net.update.UpdateService;

public class MainWindow {

    private final Stage stage;
    private final WidgetManager widgetManager = new WidgetManager();
    private final UpdateService updateService = new UpdateService();

    private final DashboardPage overview = new DashboardPage();
    private final WidgetsPage widgetsPage = new WidgetsPage();
    private final SettingsPage settings = new SettingsPage(widgetManager);

    private final Label pageTitle = new Label();
    private final Label pageSubtitle = new Label();
    private final HBox topBar = new HBox();
    private final Button updateButton = new Button("UPDATE AVAILABLE");
    private final Button restartButton = new Button("RESTART");
    private PageButton overviewButton;
    private PageButton dashboardButton;
    private PageButton settingsButton;
    private final VBox sidebar = new VBox();
    private final VBox sidebarNavigation = new VBox();
    private final HBox sidebarBrand = new HBox();
    private final Label sidebarLabel = new Label("WORKSPACE");
    private final Button sidebarToggle = new Button("‹");
    private boolean sidebarCollapsed;
    private Timeline sidebarAnimation;

    private static final double SIDEBAR_EXPANDED_WIDTH = 238;
    private static final double SIDEBAR_COLLAPSED_WIDTH = 68;

    public MainWindow(Stage stage) {
        this.stage = stage;

        widgetManager.registerWidget(WidgetID.CLOCK, new WidgetEntry(new ClockWidget()));
        widgetManager.registerWidget(WidgetID.SPOTIFY, new WidgetEntry(new SpotifyWidget()));
        widgetManager.registerWidget(WidgetID.WEATHER, new WidgetEntry(new WeatherWidget()));
        widgetManager.registerWidget(WidgetID.NEWS, new WidgetEntry(new NewsWidget()));
        widgetManager.registerWidget(WidgetID.STOCKS, new WidgetEntry(new StockWidget()));
        widgetManager.registerWidget(WidgetID.TIMER, new WidgetEntry(new TimerWidget()));

        createWindow();
    }

    private void createWindow() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        Scene scene = new Scene(root, 1280, 720);

        loadCSS(scene);

        loadLeftSideBar(root);
        loadTopBar(root);

        showPage(Page.OVERVIEW, root);

        stage.setTitle("Kjellberius Dashboard");
        stage.setScene(scene);

        update();
    }

    public void update() {
        overview.update(widgetManager);
    }

    public void show() {
        stage.show();
    }

    public void showPage(Page p, BorderPane root) {
        switch (p) {
            case OVERVIEW -> {
                widgetsPage.setPageActive(widgetManager, false);
                overview.update(widgetManager);
                root.setCenter(overview);
                pageTitle.setText("Overview");
                pageSubtitle.setText("Your workspace at a glance");
            }
            case DASHBOARD -> {
                widgetsPage.update(widgetManager);
                widgetsPage.setPageActive(widgetManager, true);
                root.setCenter(widgetsPage);
                pageTitle.setText("Dashboard");
                pageSubtitle.setText("All your active widgets");
            }
            case SETTINGS -> {
                widgetsPage.setPageActive(widgetManager, false);
                root.setCenter(settings);
                pageTitle.setText("Settings");
                pageSubtitle.setText("Customize your experience");
            }
        }
        updateSelectedButton(p);
    }

    private void updateSelectedButton(Page selectedPage) {
        if (overviewButton != null) overviewButton.getStyleClass().remove("selected");
        if (dashboardButton != null) dashboardButton.getStyleClass().remove("selected");
        if (settingsButton != null) {
            settingsButton.getStyleClass().remove("selected");
        }

        switch (selectedPage) {
            case OVERVIEW -> overviewButton.getStyleClass().add("selected");
            case DASHBOARD -> dashboardButton.getStyleClass().add("selected");
            case SETTINGS -> settingsButton.getStyleClass().add("selected");
        }
    }

    private void loadTopBar(BorderPane root) {
        HBox topBarContent = new HBox();
        topBarContent.setAlignment(Pos.CENTER_LEFT);
        topBarContent.setMaxWidth(Double.MAX_VALUE);
        topBarContent.setSpacing(10);

        VBox titleContainer = new VBox(2);
        titleContainer.getStyleClass().add("top-section-content");
        titleContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleContainer, javafx.scene.layout.Priority.ALWAYS);

        topBar.getStyleClass().add("top-section");
        topBar.setAlignment(Pos.CENTER_LEFT);

        pageTitle.getStyleClass().add("top-section-title");
        pageSubtitle.getStyleClass().add("top-section-subtitle");
        Label version = new Label("v" + AppVersion.VERSION);
        version.getStyleClass().add("version-badge");
        HBox titleRow = new HBox(9, pageTitle, version);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label status = new Label();
        status.getStyleClass().add("status-pill");
        SystemHealthService health = SystemHealthService.getInstance();
        health.severityProperty().addListener((observable, oldValue, newValue) ->
                updateHealthStatus(status, newValue));
        updateHealthStatus(status, health.getSeverity());
        configureUpdateButton();
        configureRestartButton();

        titleContainer.getChildren().addAll(titleRow, pageSubtitle);
        topBarContent.getChildren().addAll(
                titleContainer,
                updateButton,
                restartButton,
                status
        );
        topBar.getChildren().add(topBarContent);
        HBox.setHgrow(topBarContent, javafx.scene.layout.Priority.ALWAYS);

        root.setTop(topBar);
    }

    private void configureUpdateButton() {
        updateButton.getStyleClass().add("update-button");
        updateButton.visibleProperty().bind(updateService.updateAvailableProperty());
        updateButton.managedProperty().bind(updateButton.visibleProperty());
        updateButton.setOnAction(event -> requestUpdate());
        updateService.start();
    }

    private void configureRestartButton() {
        restartButton.getStyleClass().add("restart-button");
        restartButton.setOnAction(event -> restartApplication());
    }

    private void restartApplication() {
        if (updateService.launchRestart()) {
            Platform.exit();
            return;
        }
        Alert failed = new Alert(Alert.AlertType.ERROR);
        failed.setTitle("Restart failed");
        failed.setHeaderText("The application could not be restarted");
        failed.setContentText(
                "Make sure the restart script for this operating system exists "
                        + "in the project folder."
        );
        styleUpdateDialog(failed);
        failed.showAndWait();
    }

    private void requestUpdate() {
        if (!updateService.isWorkingTreeClean()) {
            Alert blocked = new Alert(Alert.AlertType.WARNING);
            blocked.setTitle("Update unavailable");
            blocked.setHeaderText("Local project changes must be committed first");
            blocked.setContentText(
                    "The updater will not overwrite uncommitted or untracked files."
            );
            styleUpdateDialog(blocked);
            blocked.showAndWait();
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "The app will close, download origin/main, compile it and restart.",
                ButtonType.CANCEL,
                ButtonType.OK
        );
        confirmation.setTitle("Update Dashboard");
        confirmation.setHeaderText("Install the available update?");
        styleUpdateDialog(confirmation);
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        if (updateService.launchUpdater()) {
            Platform.exit();
        } else {
            Alert failed = new Alert(Alert.AlertType.ERROR);
            failed.setTitle("Update failed");
            failed.setHeaderText("The updater could not be started");
            failed.setContentText(
                    "Make sure the updater script for this operating system exists "
                            + "in the project folder."
            );
            styleUpdateDialog(failed);
            failed.showAndWait();
        }
    }

    private void styleUpdateDialog(Alert alert) {
        alert.initOwner(stage);
        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add("app-dialog");
        pane.getStylesheets().setAll(stage.getScene().getStylesheets());
    }

    private void updateHealthStatus(Label status, HealthSeverity severity) {
        status.getStyleClass().removeAll(
                "status-ok",
                "status-warning",
                "status-critical"
        );
        if (severity == HealthSeverity.OK) {
            status.setText("●  ALL SYSTEMS OPERATIONAL");
            status.getStyleClass().add("status-ok");
        } else {
            status.setText("●  SYSTEM ISSUE DETECTED");
            status.getStyleClass().add(
                    severity == HealthSeverity.CRITICAL
                            ? "status-critical"
                            : "status-warning"
            );
        }
    }

    private void loadLeftSideBar(BorderPane root) {
        sidebar.getStyleClass().add("left-section");
        sidebar.setMinWidth(SIDEBAR_COLLAPSED_WIDTH);
        sidebar.setPrefWidth(SIDEBAR_EXPANDED_WIDTH);
        sidebar.setMaxWidth(SIDEBAR_EXPANDED_WIDTH);

        Label brandMark = new Label("K");
        brandMark.getStyleClass().add("brand-mark");
        VBox brandCopy = new VBox(0);
        Label brandName = new Label("KJELLBERIUS");
        Label brandTagline = new Label("COMMAND CENTER");
        brandName.getStyleClass().add("brand-name");
        brandTagline.getStyleClass().add("brand-tagline");
        brandCopy.getChildren().addAll(brandName, brandTagline);
        sidebarBrand.getChildren().addAll(brandMark, brandCopy);
        sidebarBrand.setSpacing(12);
        sidebarBrand.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sidebarBrand, Priority.ALWAYS);

        sidebarToggle.getStyleClass().add("sidebar-toggle");
        sidebarToggle.setOnAction(e -> toggleSidebar());
        HBox sidebarHeader = new HBox(8, sidebarBrand, sidebarToggle);
        sidebarHeader.getStyleClass().add("sidebar-header");
        sidebarHeader.setAlignment(Pos.CENTER_LEFT);

        sidebarLabel.getStyleClass().add("sidebar-label");

        overviewButton = new PageButton("  Overview", "left-section-button");
        dashboardButton = new PageButton("  Dashboard", "left-section-button");
        settingsButton = new PageButton("  Preferences", "left-section-button");

        overviewButton.setOnAction(e -> showPage(Page.OVERVIEW, root));
        dashboardButton.setOnAction(e -> showPage(Page.DASHBOARD, root));
        settingsButton.setOnAction(e -> showPage(Page.SETTINGS, root));

        sidebarNavigation.setSpacing(8);
        sidebarNavigation.getChildren().addAll(overviewButton, dashboardButton, settingsButton);
        VBox.setVgrow(sidebarNavigation, Priority.ALWAYS);

        sidebar.getChildren().addAll(
                sidebarHeader,
                sidebarLabel,
                sidebarNavigation
        );

        root.setLeft(sidebar);

        if (AppSettings.getInstance().getBoolean("navigation.sidebarCollapsed", false)) {
            sidebarCollapsed = true;
            sidebarToggle.setText("›");
            sidebar.setPrefWidth(SIDEBAR_COLLAPSED_WIDTH);
            sidebar.setMaxWidth(SIDEBAR_COLLAPSED_WIDTH);
            for (Node node : new Node[]{sidebarBrand, sidebarLabel, sidebarNavigation}) {
                node.setOpacity(0);
                node.setVisible(false);
                node.setManaged(false);
            }
        }
    }

    private void toggleSidebar() {
        if (sidebarAnimation != null) {
            sidebarAnimation.stop();
        }

        boolean collapse = !sidebarCollapsed;
        sidebarCollapsed = collapse;
        AppSettings.getInstance().setBoolean("navigation.sidebarCollapsed", collapse);
        sidebarToggle.setText(collapse ? "›" : "‹");
        Node[] fadingContent = {sidebarBrand, sidebarLabel, sidebarNavigation};

        if (!collapse) {
            for (Node node : fadingContent) {
                node.setManaged(true);
                node.setVisible(true);
            }
        }

        double targetWidth = collapse ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;
        double targetOpacity = collapse ? 0 : 1;

        sidebarAnimation = new Timeline(
                new KeyFrame(
                        Duration.millis(280),
                        e -> {
                            if (collapse) {
                                for (Node node : fadingContent) {
                                    node.setVisible(false);
                                    node.setManaged(false);
                                }
                            }
                        },
                        new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(sidebar.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(sidebarBrand.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH),
                        new KeyValue(sidebarLabel.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH),
                        new KeyValue(sidebarNavigation.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH)
                )
        );
        sidebarAnimation.play();
    }

    private void loadCSS(Scene scene) {
        ThemeManager.initialize(scene);
        scene.getStylesheets().addAll(
                getClass().getResource("/css/widget.css").toExternalForm(),
                getClass().getResource("/css/dashboard.css").toExternalForm(),
                getClass().getResource("/css/mainWindow.css").toExternalForm(),
                getClass().getResource("/css/settings.css").toExternalForm()
        );
        // Keep corner overrides last so they win over component defaults.
        CornerStyleManager.initialize(scene);
    }
}
