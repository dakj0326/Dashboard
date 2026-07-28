package org.net.ui.pages.settings;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.net.settings.AppSettings;

public class SettingsGeneralPage extends ScrollPane {
    public SettingsGeneralPage() {
        getStyleClass().add("settings-scroll");
        setFitToWidth(true);
        setContent(createContent());
    }

    private VBox createContent() {
        Label heading = new Label("General");
        heading.getStyleClass().add("settings-page-title");
        Label intro = new Label("Personalize the dashboard for its primary user.");
        intro.getStyleClass().add("settings-page-subtitle");

        SettingsSection profile = new SettingsSection("Profile");
        Label nameLabel = new Label("Display name");
        nameLabel.getStyleClass().add("settings-field-label");
        TextField name = new TextField(
                AppSettings.getInstance().get("profile.name", "David")
        );
        name.setPromptText("Enter your name");
        name.textProperty().addListener((observable, oldValue, newValue) ->
                AppSettings.getInstance().set("profile.name", newValue.trim()));
        Label help = new Label("This name is used in the greeting on Overview.");
        help.getStyleClass().add("settings-help");
        profile.add(nameLabel);
        profile.add(name);
        profile.add(help);

        VBox content = new VBox(18, heading, intro, profile);
        content.setPadding(new Insets(38));
        return content;
    }
}
