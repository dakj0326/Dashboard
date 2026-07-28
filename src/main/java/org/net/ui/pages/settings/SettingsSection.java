package org.net.ui.pages.settings;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
public class SettingsSection extends VBox {

    private final VBox content = new VBox(10);

    public SettingsSection(String title) {
        createLayout(title);
    }

    private void createLayout(String t) {
        getStyleClass().add("settings-section");

        Label title = new Label(t);
        title.getStyleClass().add("settings-section-title");
        getChildren().add(title);

        content.getStyleClass().add("settings-section-content");
        getChildren().add(content);
    }

    public void add(Node node) {
        content.getChildren().add(node);
    }
}
