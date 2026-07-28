package org.net.ui.pages.settings;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.net.ui.pages.Page;
import org.net.ui.pages.PageButton;
import org.net.ui.widgets.WidgetManager;

public class SettingsPage extends BorderPane {
    private final SettingsGeneralPage generalPage = new SettingsGeneralPage();
    private final SettingsAppearancePage appearancePage = new SettingsAppearancePage();
    private final SettingsWidgetsPage widgetsPage = new SettingsWidgetsPage();
    private final SettingsDashboardPage dashboardPage;
    private PageButton generalButton;
    private PageButton appearanceButton;
    private PageButton widgetsButton;
    private PageButton dashboardButton;

    public SettingsPage(WidgetManager widgetManager) {
        dashboardPage = new SettingsDashboardPage(widgetManager);
        createLayout();
    }

    private void createLayout() {
        getStyleClass().add("settings-page");
        loadLeftSideBar();

        showSettingsPage(Page.SETTINGS_GENERAL);
    }

    private void showSettingsPage(Page p) {
        switch (p) {
            case SETTINGS_GENERAL -> {
                setCenter(generalPage);
                updateSelectedButton(generalButton);
            }
            case SETTINGS_APPEARANCE -> {
                setCenter(appearancePage);
                updateSelectedButton(appearanceButton);
            }
            case SETTINGS_WIDGETS -> {
                setCenter(widgetsPage);
                updateSelectedButton(widgetsButton);
            }
            case SETTINGS_DASHBOARD -> {
                dashboardPage.refresh();
                setCenter(dashboardPage);
                updateSelectedButton(dashboardButton);
            }
        }
    }

    private void loadLeftSideBar() {
        VBox content = new VBox(20);
        content.getStyleClass().add("settings-content");

        generalButton = new PageButton("General", "settings-left-section-button");
        appearanceButton = new PageButton("Appearance", "settings-left-section-button");
        widgetsButton = new PageButton("Widgets", "settings-left-section-button");
        dashboardButton = new PageButton("Dashboard", "settings-left-section-button");
        generalButton.getStyleClass().add("selected");

        generalButton.setOnAction(e -> showSettingsPage(Page.SETTINGS_GENERAL));
        appearanceButton.setOnAction(e -> showSettingsPage(Page.SETTINGS_APPEARANCE));
        widgetsButton.setOnAction(e -> showSettingsPage(Page.SETTINGS_WIDGETS));
        dashboardButton.setOnAction(e -> showSettingsPage(Page.SETTINGS_DASHBOARD));

        content.getChildren().addAll(
                generalButton,
                appearanceButton,
                widgetsButton,
                dashboardButton
        );

        setLeft(content);
    }

    private void updateSelectedButton(PageButton selected) {
        generalButton.getStyleClass().remove("selected");
        appearanceButton.getStyleClass().remove("selected");
        widgetsButton.getStyleClass().remove("selected");
        dashboardButton.getStyleClass().remove("selected");
        selected.getStyleClass().add("selected");
    }
}
