package org.net.ui.pages.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.net.ui.widgets.WidgetEntry;
import org.net.ui.widgets.WidgetManager;

public class SettingsDashboardPage extends ScrollPane {
    private final WidgetManager widgetManager;
    private final SettingsSection widgetSection = new SettingsSection("Widgets and order");
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
                "Choose visible widgets and use the buttons to set their dashboard order. "
                        + "Changes are saved automatically."
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

            Button moveUp = orderButton("\u2191", "Move widget up");
            Button moveDown = orderButton("\u2193", "Move widget down");
            moveUp.setDisable(!widgetManager.canMoveWidget(widget.getKey(), -1));
            moveDown.setDisable(!widgetManager.canMoveWidget(widget.getKey(), 1));
            moveUp.setOnAction(e -> {
                widgetManager.moveWidget(widget.getKey(), -1);
                refresh();
            });
            moveDown.setOnAction(e -> {
                widgetManager.moveWidget(widget.getKey(), 1);
                refresh();
            });

            HBox row = new HBox(8, visible, moveUp, moveDown);
            row.getStyleClass().add("widget-order-row");
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(visible, Priority.ALWAYS);
            visible.setMaxWidth(Double.MAX_VALUE);
            widgetChoices.getChildren().add(row);
        }
    }

    private static Button orderButton(String symbol, String description) {
        Button button = new Button(symbol);
        button.getStyleClass().add("widget-order-button");
        button.setAccessibleText(description);
        button.setTooltip(new Tooltip(description));
        return button;
    }
}
