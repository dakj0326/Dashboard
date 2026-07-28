package org.net.ui.pages.settings;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class SettingsAppearancePage extends ScrollPane {
    private final VBox content = new VBox();

    private final SettingsSection general = new SettingsSection("General");
    private final SettingsSection theme = new SettingsSection("Theme");

    public SettingsAppearancePage() {
        getStyleClass().add("settings-scroll");
        setFitToWidth(true);

        loadGeneral();
        loadTheme();
        loadContent();
    }

    private void loadContent() {
        javafx.scene.control.Label heading = new javafx.scene.control.Label("Appearance");
        heading.getStyleClass().add("settings-page-title");
        javafx.scene.control.Label intro = new javafx.scene.control.Label("Choose how your workspace looks and feels.");
        intro.getStyleClass().add("settings-page-subtitle");
        content.getChildren().addAll(heading, intro);
        content.getChildren().add(general);
        content.getChildren().add(theme);
        content.setFillWidth(true);
        content.setPadding(new Insets(38));
        content.setSpacing(18);
        setContent(content);
    }

    private void loadGeneral() {
        CheckBox checkBox = new CheckBox("Use rounded corners throughout the interface");
        checkBox.setSelected(CornerStyleManager.usesRoundedCorners());
        checkBox.setOnAction(e -> CornerStyleManager.setRounded(checkBox.isSelected()));
        general.add(checkBox);
    }

    private void loadTheme() {
        ComboBox<Theme> themeBox = new ComboBox<>();
        themeBox.getItems().addAll(Theme.values());
        themeBox.setValue(ThemeManager.getTheme());
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setOnAction(e -> {
            ThemeManager.setTheme(themeBox.getValue());
        });

        theme.add(themeBox);
    }
}
