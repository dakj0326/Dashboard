package org.net.ui.pages.settings;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.net.ui.widgets.WidgetEntry;
import org.net.ui.widgets.WidgetManager;

public class SettingsDashboardPage extends ScrollPane {
    private final WidgetManager widgetManager;
    private final SettingsSection widgetSection = new SettingsSection("Visible widgets");
    private final VBox widgetChoices = new VBox(10);

    public SettingsDashboardPage(WidgetManager widgetManager) {
        this.widgetManager = widgetManager;
        getStyleClass().add("settings-scroll");
        setFitToWidth(true);
        setContent(createContent());
    }

    private VBox createContent() {
        Label heading = new Label("Dashboard");
        heading.getStyleClass().add("settings-page-title");
        Label intro = new Label("Choose which widgets appear on your dashboard.");
        intro.getStyleClass().add("settings-page-subtitle");

        Label help = new Label(
                "Changes are applied automatically and remembered the next time the app starts."
        );
        help.getStyleClass().add("settings-help");
        help.setWrapText(true);
        widgetSection.add(help);
        widgetSection.add(widgetChoices);

        VBox content = new VBox(18, heading, intro, widgetSection);
        content.setPadding(new Insets(38));
        return content;
    }

    public void refresh() {
        widgetChoices.getChildren().clear();

        for (var widget : widgetManager.getWidgets().entrySet()) {
            WidgetEntry entry = widget.getValue();
            CheckBox visible = new CheckBox(entry.getWidget().getDisplayName());
            visible.getStyleClass().add("widget-visibility-check");
            visible.setSelected(entry.isVisible());
            visible.setOnAction(e -> {
                if (visible.isSelected()) {
                    widgetManager.showWidget(widget.getKey());
                } else {
                    widgetManager.hideWidget(widget.getKey());
                }
            });
            widgetChoices.getChildren().add(visible);
        }
    }
}
