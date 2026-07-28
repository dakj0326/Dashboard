package org.net.ui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import org.net.ui.widgets.WidgetEntry;
import org.net.ui.widgets.WidgetManager;

public class WidgetsPage extends BorderPane {
    private final FlowPane widgetContainer = new FlowPane();

    public WidgetsPage() {
        getStyleClass().add("widgets-page");
        widgetContainer.getStyleClass().add("widgets-only-grid");
        widgetContainer.setAlignment(Pos.TOP_LEFT);
        widgetContainer.setHgap(14);
        widgetContainer.setVgap(14);
        widgetContainer.setPadding(new Insets(22));

        ScrollPane scrollPane = new ScrollPane(widgetContainer);
        scrollPane.getStyleClass().add("widgets-scroll");
        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);
    }

    public void update(WidgetManager manager) {
        widgetContainer.getChildren().clear();
        boolean hasVisibleWidgets = false;
        for (WidgetEntry entry : manager.getWidgets().values()) {
            entry.getWidget().update(manager);
            if (entry.isVisible()) {
                widgetContainer.getChildren().add(entry.getWidget());
                hasVisibleWidgets = true;
            }
        }
        if (!hasVisibleWidgets) {
            Label emptyState = new Label(
                    "No widgets are enabled. Choose widgets in Preferences → Dashboard."
            );
            emptyState.getStyleClass().add("widgets-empty-state");
            widgetContainer.getChildren().add(emptyState);
        }
    }

    public void setPageActive(WidgetManager manager, boolean active) {
        for (WidgetEntry entry : manager.getWidgets().values()) {
            entry.getWidget().onPageVisibilityChanged(active && entry.isVisible());
        }
    }
}
